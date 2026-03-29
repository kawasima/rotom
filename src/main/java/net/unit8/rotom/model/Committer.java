package net.unit8.rotom.model;

import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.dircache.*;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.MessageFormat;
import java.util.Locale;

public class Committer {
    private Repository repository;
    private String ref;
    private DirCache index;

    public Committer(Repository repository, String ref) {
        this.repository = repository;
        this.ref = ref;
    }

    private DirCache createTemporaryIndex(final ObjectId headId, final String path, byte[] data) {
        final DirCache inCoreIndex = DirCache.newInCore();
        final DirCacheBuilder dcBuilder = inCoreIndex.builder();

        try (ObjectInserter inserter = repository.newObjectInserter()) {
            if (data != null) {
                final DirCacheEntry dcEntry = new DirCacheEntry(path);
                dcEntry.setLength(data.length);
                dcEntry.setLastModified(java.time.Instant.now());
                dcEntry.setFileMode(FileMode.REGULAR_FILE);
                dcEntry.setObjectId(inserter.insert(Constants.OBJ_BLOB, data));

                dcBuilder.add(dcEntry);
            }

            if (headId != null) {
                try (RevWalk revWalk = new RevWalk(repository);
                     TreeWalk treeWalk = new TreeWalk(repository)) {
                    final int hIdx = treeWalk.addTree(revWalk.parseTree(headId));
                    treeWalk.setRecursive(true);

                    while (treeWalk.next()) {
                        final String walkPath = treeWalk.getPathString();
                        final CanonicalTreeParser hTree = treeWalk.getTree(hIdx, CanonicalTreeParser.class);

                        if (!walkPath.equals(path)) {
                            final DirCacheEntry dcEntry = new DirCacheEntry(walkPath);
                            dcEntry.setObjectId(hTree.getEntryObjectId());
                            dcEntry.setFileMode(hTree.getEntryFileMode());
                            dcBuilder.add(dcEntry);
                        }
                    }
                }
            }

            dcBuilder.finish();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (data == null) {
            final DirCacheEditor editor = inCoreIndex.editor();
            editor.add(new DirCacheEditor.DeleteTree(path));
            editor.finish();
        }

        return inCoreIndex;
    }

    public void addToIndex(String dir, String name, String format, byte[] data) throws IOException {
        dir = dir.replace(' ', '-');
        name = name.replace(' ', '-');
        name = name + "." +
                MarkupType.valueOf(format.toUpperCase(Locale.US)).getExtension();
        addToIndex(Wiki.fullpath(dir, name), data);
    }

    public void addToIndex(String path, byte[] data) throws IOException {
        final ObjectId headId = repository.resolve(ref + "^{commit}");
        index = createTemporaryIndex(headId, path, data);
    }

    public void add(String path, byte[] data) throws IOException {
        addToIndex(path, data);
    }

    public void rm(String path) throws IOException {
        final ObjectId headId = repository.resolve(ref + "^{commit}");
        index = createTemporaryIndex(headId, path, null);
    }

    public ObjectId commit(Commit commitInfo) throws GitAPIException, IOException {
        try (ObjectInserter odi = repository.newObjectInserter()) {
            final ObjectId headId = repository.resolve(ref + "^{commit}");
            final ObjectId indexTreeId = index.writeTree(odi);
            final CommitBuilder commit = new CommitBuilder();
            commit.setAuthor(commitInfo.getPersonIdent());
            commit.setCommitter(commitInfo.getPersonIdent());
            commit.setEncoding(java.nio.charset.StandardCharsets.UTF_8);
            commit.setMessage(commitInfo.getMessage());
            if (headId != null) {
                commit.setParentId(headId);
            }
            commit.setTreeId(indexTreeId);

            final ObjectId commitId = odi.insert(commit);
            odi.flush();

            try (RevWalk revWalk = new RevWalk(repository)) {
                final RevCommit revCommit = revWalk.parseCommit(commitId);
                final RefUpdate ru = repository.updateRef("refs/heads/" + ref);
                if (headId == null) {
                    ru.setExpectedOldObjectId(ObjectId.zeroId());
                } else {
                    ru.setExpectedOldObjectId(headId);
                }
                ru.setNewObjectId(commitId);
                ru.setRefLogMessage("commit: " + revCommit.getShortMessage(), false);
                final RefUpdate.Result rc = ru.forceUpdate();
                switch (rc) {
                    case NEW:
                    case FORCED:
                    case FAST_FORWARD:
                        break;
                    case REJECTED:
                    case LOCK_FAILURE:
                        throw new ConcurrentRefUpdateException(JGitText.get().couldNotLockHEAD, ru.getRef(), rc);
                    default:
                        throw new JGitInternalException(MessageFormat.format(JGitText.get().updatingRefFailed, Constants.HEAD, commitId.toString(), rc));
                }
                return commitId;
            }
        }
    }
}
