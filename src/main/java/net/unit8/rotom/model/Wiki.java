package net.unit8.rotom.model;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Wiki extends SystemComponent {
    private String indexPage = "Home";
    private Path repositoryPath;
    private String ref = "master";

    private Repository repository;

    public static String fullpath(String dir, String name) {
        if (dir == null || dir.isEmpty()) {
            return name;
        } else {
            return dir.replaceFirst("/+$", "") + "/" + name;
        }
    }

    private TreeWalk buildTreeWalk(RevTree tree, final String path) throws IOException {
        TreeWalk treeWalk = TreeWalk.forPath(repository, path, tree);
        if(treeWalk == null) {
            throw new FileNotFoundException("Did not find expected file '" + path + "' in tree '" + tree.getName() + "'");
        }
        return treeWalk;
    }

    public List<Page> getPages(String path) {
        try {
            List<Page> pages = new ArrayList<>();
            Ref head = repository.exactRef("refs/heads/master");
            if (head == null) return Collections.emptyList();
            RevTree tree;
            try (RevWalk revWalk = new RevWalk(repository)) {
                tree = revWalk.parseCommit(head.getObjectId()).getTree();
            }

            if (path.isEmpty()) {
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(false);
                    treeWalk.setPostOrderTraversal(false);

                    while(treeWalk.next()) {
                        String p = treeWalk.getPathString();
                        if (treeWalk.getFileMode() == FileMode.TREE) {
                            p = p + "/";
                        }
                        pages.add(new Page(p));
                    }
                }
            } else {
                try (TreeWalk treeWalk = buildTreeWalk(tree, path)) {
                    if ((treeWalk.getFileMode(0).getBits() & FileMode.TYPE_TREE) == 0) {
                        throw new IllegalStateException(
                                "Tried to read the elements of a non-tree for commit '" + head.getObjectId() + "' and path '" + path + "', had filemode " + treeWalk.getFileMode(0).getBits());
                    }

                    try (TreeWalk dirWalk = new TreeWalk(repository)) {
                        dirWalk.addTree(treeWalk.getObjectId(0));
                        dirWalk.setRecursive(false);
                        while (dirWalk.next()) {
                            String p = dirWalk.getPathString();
                            if (dirWalk.getFileMode() == FileMode.TREE) {
                                p = p + "/";
                            }
                            pages.add(new Page(Wiki.fullpath(path, p)));
                        }
                    }
                }
            }
            return pages;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Page getPage(String name) {
        return getPage(name, null);
    }

    public Page getPage(String name, ObjectId commitId) {
        try {
            if (commitId == null) {
                Ref head = repository.exactRef("refs/heads/master");
                if (head == null) return null;
                commitId = head.getObjectId();
            }
            // a commit points to a tree
            try (RevWalk walk = new RevWalk(repository)) {
                RevCommit commit = walk.parseCommit(commitId);
                RevTree tree = walk.parseTree(commit.getTree().getId());
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(JGitPathPrefixFilter.create(name));
                    if (!treeWalk.next()) {
                        // Not found
                        return null;
                    }
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    BlobEntry blob = new BlobEntry(treeWalk.getPathString(), objectId, loader.getCachedBytes());
                    return new Page(repository, blob);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writePage(String name, String format, byte[] data, String dir, Commit commit) {
        if (dir == null) dir = "";
        String sanitizedName = name.replace(' ', '-');
        String sanitizedDir  = dir.replace(' ', '-');

        Committer committer = new Committer(repository);
        try {
            committer.addToIndex(sanitizedDir, sanitizedName, format, data);
            committer.commit(commit);
        } catch (GitAPIException e) {

        } catch (IOException e) {

        }
    }

    public void updatePage(Page page, String name, String format, byte[] data, Commit commit) {
        if (name == null) name = page.getName();
        if (format == null) format = page.getFormat();

        boolean rename = !Objects.equals(name, page.getName());
        Committer committer = new Committer(repository);

        try {
            committer.add(Wiki.fullpath(page.getPath(), page.getFileName()), data);
            committer.commit(commit);
        } catch (GitAPIException e) {

        } catch (IOException e) {

        }
    }

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<Wiki>() {
            @Override
            public void start(Wiki wiki) {
                try {
                    wiki.repository = FileRepositoryBuilder.create(repositoryPath.resolve(".git").toFile());
                    if (!wiki.repository.getDirectory().exists()) {
                        wiki.repository.create(true);
                    }
                } catch (IOException e) {

                }
            }

            @Override
            public void stop(Wiki wiki) {
                if (wiki.repository != null) {
                    wiki.repository.close();
                }
            }
        };
    }

    public void setRepositoryPath(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public String getIndexPage() {
        return indexPage;
    }

    public void setIndexPage(String indexPage) {
        this.indexPage = indexPage;
    }

    public String getRef() {
        return ref;
    }
}
