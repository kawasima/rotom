package net.unit8.rotom.model;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

public class Committer {
    private Repository repository;
    private DirCache index;

    public Committer(Repository repository) {
        this.repository = repository;
    }

    private DirCache createTemporaryIndex(final ObjectId headId, final String path, byte[] data) {

        final DirCache inCoreIndex = DirCache.newInCore();
        final DirCacheBuilder dcBuilder = inCoreIndex.builder();
        final ObjectInserter inserter = repository.newObjectInserter();

        try {
            if (data != null) {
                final DirCacheEntry dcEntry = new DirCacheEntry(path);
                dcEntry.setLength(data.length);
                dcEntry.setLastModified(System.currentTimeMillis());
                dcEntry.setFileMode(FileMode.REGULAR_FILE);
                dcEntry.setObjectId(inserter.insert(Constants.OBJ_BLOB, data));

                dcBuilder.add(dcEntry);
            }

            if (headId != null) {
                final TreeWalk treeWalk = new TreeWalk(repository);
                final int hIdx = treeWalk.addTree(new RevWalk(repository).parseTree(headId));
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
                treeWalk.close();
            }

            dcBuilder.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            inserter.close();
        }

        if (data == null) {
            final DirCacheEditor editor = inCoreIndex.editor();
            editor.add(new DirCacheEditor.DeleteTree(path));
            editor.finish();
        }

        return inCoreIndex;
    }

    public void addToIndex(String dir, String name, String format, byte[] data) throws GitAPIException, IOException {
        dir = dir.replace(' ', '-');
        name = name.replace(' ', '-');
        String path = name + "." + MarkupType.valueOf(format).getExtension();
        addToIndex(Paths.get(dir, path), data);
    }

    public void addToIndex(Path path, byte[] data) throws IOException {
        final ObjectId headId = repository.resolve("master^{commit}");
        index = createTemporaryIndex(headId, path.normalize().toString(), data);
    }

    public void add(String path, byte[] data) throws IOException {
        addToIndex(Paths.get(path), data);
    }

    public void updateWorkingDir(String dir, String name, String format) throws GitAPIException {
        try (Git git = new Git(repository)) {
            git.checkout()
                    .addPath(name)
                    .call();
        }
    }

    public ObjectId commit(Commit commitInfo) throws GitAPIException, IOException {
        final ObjectInserter odi = repository.newObjectInserter();
        try {
            final ObjectId headId = repository.resolve("master^{commit}");
            final ObjectId indexTreeId = index.writeTree(odi);
            final CommitBuilder commit = new CommitBuilder();
            commit.setAuthor(commitInfo.getPersonIdent());
            commit.setCommitter(commitInfo.getPersonIdent());
            commit.setEncoding(Constants.CHARACTER_ENCODING);
            commit.setMessage(commitInfo.getMessage());
            if (headId != null) {
                commit.setParentId(headId);
            }
            commit.setTreeId(indexTreeId);

            // Insert the commit into the repository
            final ObjectId commitId = odi.insert(commit);
            odi.flush();

            final RevWalk revWalk = new RevWalk(repository);
            try {
                final RevCommit revCommit = revWalk.parseCommit(commitId);
                final RefUpdate ru = repository.updateRef("refs/heads/" + "master");
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
            } finally {
                revWalk.close();
            }
        } finally {
            odi.close();
        }
    }
}
