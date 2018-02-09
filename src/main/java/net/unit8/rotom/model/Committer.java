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
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;

import java.io.IOException;
import java.text.MessageFormat;

public class Committer {
    private Repository repository;
    private DirCache index;

    public Committer(Repository repository) {
        this.repository = repository;
    }

    private DirCacheBuilder createTemporaryIndex(final ObjectId headId, final String path) {
        index = DirCache.newInCore();
        final DirCacheBuilder dcBuilder = index.builder();

        try {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return dcBuilder;
    }

    private void addToIndex(DirCacheBuilder dcBuilder, final String path, byte[] data) {
        if (data != null) {
            final ObjectInserter inserter = repository.newObjectInserter();
            try {
                final DirCacheEntry dcEntry = new DirCacheEntry(path);
                dcEntry.setLength(data.length);
                dcEntry.setLastModified(System.currentTimeMillis());
                dcEntry.setFileMode(FileMode.REGULAR_FILE);
                dcEntry.setObjectId(inserter.insert(Constants.OBJ_BLOB, data));
                dcBuilder.add(dcEntry);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                inserter.close();
            }
        }
    }

    public void add(String path, byte[] data) throws IOException {
        final ObjectId headId = repository.resolve("master^{commit}");
        DirCacheBuilder dcBuilder = createTemporaryIndex(headId, path);
        addToIndex(dcBuilder, path, data);
        dcBuilder.finish();
    }

    public void rm(String path) throws IOException {
        final ObjectId headId = repository.resolve("master^{commit}");
        DirCacheBuilder dcBuilder = createTemporaryIndex(headId, path);
        dcBuilder.finish();
    }

    public void update(String oldPath, String newPath, byte[] data) throws IOException {
        final ObjectId headId = repository.resolve("master^{commit}");
        DirCacheBuilder dcBuilder = createTemporaryIndex(headId, oldPath);
        addToIndex(dcBuilder, newPath, data);
        dcBuilder.finish();
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
