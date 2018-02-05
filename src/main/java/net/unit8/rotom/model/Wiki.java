package net.unit8.rotom.model;

import enkan.collection.OptionMap;
import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

import static enkan.util.ThreadingUtils.some;

public class Wiki extends SystemComponent {
    private String indexPage = "Home";
    private String ref = "master";
    private Repository repository;

    private Git git;

    public static String fullpath(String dir, String name) {
        if (dir == null || dir.isEmpty()) {
            return name;
        } else {
            return dir.replaceFirst("/+$", "") + "/" + name;
        }
    }

    private TreeWalk buildTreeWalk(RevTree tree, final String path) throws IOException {
        TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), path, tree);
        if(treeWalk == null) {
            throw new FileNotFoundException("Did not find expected file '" + path + "' in tree '" + tree.getName() + "'");
        }
        return treeWalk;
    }

    public List<Page> getPages(String path) {
        try {
            String sanitizedPath = sanitize(path);
            List<Page> pages = new ArrayList<>();
            Ref head = git.getRepository().exactRef("refs/heads/master");
            if (head == null) return Collections.emptyList();
            RevTree tree;
            try (RevWalk revWalk = new RevWalk(git.getRepository())) {
                RevCommit commit = revWalk.parseCommit(head.getObjectId());
                tree = commit.getTree();
            }

            if (sanitizedPath.isEmpty()) {
                try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
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
                try (TreeWalk treeWalk = buildTreeWalk(tree, sanitizedPath)) {
                    if ((treeWalk.getFileMode(0).getBits() & FileMode.TYPE_TREE) == 0) {
                        throw new IllegalStateException(
                                "Tried to read the elements of a non-tree for commit '" + head.getObjectId() + "' and path '" + path + "', had filemode " + treeWalk.getFileMode(0).getBits());
                    }

                    try (TreeWalk dirWalk = new TreeWalk(git.getRepository())) {
                        dirWalk.addTree(treeWalk.getObjectId(0));
                        dirWalk.setRecursive(false);
                        while (dirWalk.next()) {
                            String p = dirWalk.getPathString();
                            if (dirWalk.getFileMode() == FileMode.TREE) {
                                p = p + "/";
                            }
                            pages.add(new Page(Wiki.fullpath(sanitizedPath, p)));
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
                Ref head = git.getRepository().exactRef("refs/heads/master");
                if (head == null) return null;
                commitId = head.getObjectId();
            }
            // a commit points to a tree
            try (RevWalk walk = new RevWalk(git.getRepository())) {
                RevCommit commit = walk.parseCommit(commitId);
                RevTree tree = walk.parseTree(commit.getTree().getId());
                try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(JGitPathPrefixFilter.create(sanitize(name)));
                    if (!treeWalk.next()) {
                        // Not found
                        return null;
                    }
                    String path = treeWalk.getPathString();
                    ObjectId objectId = treeWalk.getObjectId(0);
                    git.getRepository().hasObject(objectId);

                    BlobEntry blob = new BlobEntry(path, objectId,
                            commit.getCommitterIdent(), commit.getCommitTime(),
                            () -> {
                                try {
                                    ObjectLoader loader = git.getRepository().open(objectId);
                                    return loader.getCachedBytes();
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                    return new Page(path, blob);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writePage(String name, String format, byte[] data, String dir, Commit commit) {
        String sanitizedName = sanitize(name);
        String sanitizedDir  = sanitize(dir);

        Committer committer = new Committer(git.getRepository());
        try {
            committer.addToIndex(sanitizedDir, sanitizedName, format, data);
            committer.commit(commit);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void updatePage(Page page, String name, String format, byte[] data, Commit commit) {
        if (name == null) name = page.getName();
        if (format == null) format = page.getFormat();

        boolean rename = !Objects.equals(name, page.getName());
        Committer committer = new Committer(git.getRepository());

        try {
            committer.add(page.getPath(), data);
            committer.commit(commit);
        } catch (GitAPIException e) {

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void deletePage(Page page, Commit commit) {
        Committer committer = new Committer(git.getRepository());
        try {
            committer.rm(page.getPath());
            committer.commit(commit);
        } catch (GitAPIException e) {

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<RevCommit> getVersions(OptionMap options) {
        List<RevCommit> commits = new ArrayList<>();
        try {
            LogCommand log = git.log();
            if(options.containsKey("path")) {
                log.addPath(options.getString("path"));
            }
            log.setSkip(options.getInt("offset", 0));
            log.setMaxCount(options.getInt("limit", 10));
            log.call().forEach(commits::add);
            return commits;
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getDiff(Page page, String hash1, String hash2) {
        try {
            AbstractTreeIterator oldTreeParser = prepareTreeParser(git.getRepository(), hash1);
            AbstractTreeIterator newTreeParser = prepareTreeParser(git.getRepository(), hash2);

            List<DiffEntry> diff = git.diff().
                    setOldTree(oldTreeParser).
                    setNewTree(newTreeParser).
                    setPathFilter(PathFilter.create(page.getPath())).call();
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                for (DiffEntry entry : diff) {
                    try (DiffFormatter formatter = new DiffFormatter(baos)) {
                        formatter.setRepository(git.getRepository());
                        formatter.format(entry);
                    }
                }
                return new String(baos.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    private AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        //noinspection Duplicates
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(ObjectId.fromString(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }

    private String sanitize(String raw) {
        return some(raw, str -> str.replace(' ', '-')).orElse("");
    }

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<Wiki>() {
            @Override
            public void start(Wiki wiki) {
                try {
                    if (!some(repository.getDirectory(), File::exists).orElse(true)) {
                        repository.create(true);
                    }
                    wiki.git = Git.wrap(repository);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public void stop(Wiki wiki) {
                if (wiki.git != null) {
                    git.getRepository().close();
                    git.close();
                }
            }
        };
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
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

    public void setRef(String ref) {
        this.ref = ref;
    }
}
