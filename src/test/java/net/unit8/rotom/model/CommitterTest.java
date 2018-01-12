package net.unit8.rotom.model;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Comparator;

public class CommitterTest {
    public Repository createNewRepository() throws IOException, GitAPIException {
        // prepare a new folder
        File localPath = File.createTempFile("TestGitRepository", "");
        if(!localPath.delete()) {
            throw new IOException("Could not delete temporary file " + localPath);
        }

        // create the directory
        Repository repository = FileRepositoryBuilder.create(new File(localPath, ".git"));
        repository.create();

        return repository;
    }

    @Test
    public void test() throws IOException, GitAPIException {
        try(Repository repository = createNewRepository()) {
            Committer committer = new Committer(repository);
            ObjectId id = committer.commit(new Commit("kawasima", "kawasima1016@gmail.com", "init"));
            System.out.println(id);
        }
    }

    @Test
    public void testBare() throws IOException, GitAPIException {
        DirCache inCoreIndex = DirCache.newInCore();
        DirCacheBuilder dcBuilder = inCoreIndex.builder();

        try(Repository repository = createNewRepository()) {
            ObjectId headId = repository.resolve("master^{commit}");

            DirCacheEntry entry = new DirCacheEntry("hoge");
            entry.setLength("hello".getBytes().length);
            entry.setFileMode(FileMode.REGULAR_FILE);
            entry.setLastModified(System.currentTimeMillis());
            ObjectInserter inserter = repository.newObjectInserter();
            try {
                entry.setObjectId(inserter.insert(Constants.OBJ_BLOB, "hello".getBytes()));
                dcBuilder.add(entry);
            } finally {
                inserter.close();
            }

            if (headId != null) {
                final TreeWalk treeWalk = new TreeWalk(repository);
                final int hIdx = treeWalk.addTree(new RevWalk(repository).parseTree(headId));
                treeWalk.setRecursive(true);

                while (treeWalk.next()) {
                    final String walkPath = treeWalk.getPathString();
                    final CanonicalTreeParser hTree = treeWalk.getTree(hIdx, CanonicalTreeParser.class);

                    if (!walkPath.equals("hoge")) {
                        // add entries from HEAD for all other paths
                        // create a new DirCacheEntry with data retrieved from HEAD
                        final DirCacheEntry dcEntry = new DirCacheEntry(walkPath);
                        dcEntry.setObjectId(hTree.getEntryObjectId());
                        dcEntry.setFileMode(hTree.getEntryFileMode());

                        // add to temporary in-core index
                        dcBuilder.add(dcEntry);
                    }
                }
                treeWalk.close();
            }

            dcBuilder.finish();
            ObjectInserter odi = repository.newObjectInserter();
            try {
                ObjectId indexTreeId = inCoreIndex.writeTree(odi);

                final CommitBuilder commit = new CommitBuilder();
                commit.setTreeId(indexTreeId);
                if (headId != null) {
                    commit.setParentId(headId);
                }
                commit.setAuthor(new PersonIdent("kawasima", "kawasima1016@gmail.com"));
                commit.setCommitter(new PersonIdent("kawasima", "kawasima1016@gmail.com"));
                commit.setMessage("init");

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

                } finally {
                    revWalk.close();
                }
            } finally {
                odi.close();
            }

            Files.walk(Paths.get("target/workspace"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            Git.cloneRepository()
                    .setBare(false)
                    .setURI(repository.getDirectory().toURI().toString())
                    .setDirectory(new File("target/workspace"))
                    .call();
        }
    }
}
