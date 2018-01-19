package net.unit8.rotom.model;

import enkan.util.CodecUtils;
import net.unit8.rotom.model.filter.Render;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Page {
    private BlobEntry blob;
    private List<Filter> filterChain;
    private Repository repository;

    public Page(String path) {
        this.blob = new BlobEntry(path, null, new byte[0],
                path.endsWith("/") ? FileMode.TREE : FileMode.REGULAR_FILE);
    }

    public Page(Repository repository, BlobEntry blob) {
        this.blob = blob;
        this.repository = repository;
        filterChain = new ArrayList<>();
        filterChain.add(new Render());
    }

    public String getFileName() {
        return Optional.ofNullable(blob)
                .map(BlobEntry::getName)
                .orElse(null);
    }

    public String getName() {
        return Optional.ofNullable(getFileName())
                .map(name -> name
                        .replaceFirst("\\.\\w+$" , "")
                        .replace('-', ' '))
                .orElse(null);
    }

    public String getUrlPath() {
        return Optional.of(getPath())
                .map(CodecUtils::urlEncode)
                .map(u -> u.replaceAll("%2F", "/"))
                .orElse(null);
    }

    public String getPath() {
        return Optional.ofNullable(blob)
                .map(BlobEntry::getPath)
                .orElse(null);
    }

    public String toString() {
        return getName();
    }

    public String getTextData() {
        return new String(blob.getData(), StandardCharsets.UTF_8);
    }

    public String getFormattedData() {
        String s = new String(blob.getData(), StandardCharsets.UTF_8);
        for (Filter filter : filterChain) {
            s = filter.extract(s, this);
        }

        for (Filter filter : filterChain) {
            s = filter.process(s, this);
        }

        Matcher m = Pattern.compile("<p></p>").matcher(s);
        return m.replaceAll("");
    }

    public String getFormat() {
        return Arrays.stream(MarkupType.values())
                .filter(mt -> mt.match(getFileName()))
                .map(mt -> mt.getName())
                .findAny()
                .orElse(null);
    }

    public List<RevCommit> getVersions() {
        try(Git git = new Git(repository)) {
            List<RevCommit> commits = new ArrayList<>();
            git.log().addPath(getFileName()).call()
                    .forEach(commits::add);
            return commits;
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<DiffEntry> getDiff(String hash1, String hash2) {
        try(Git git = new Git(repository)) {
            AbstractTreeIterator oldTreeParser = prepareTreeParser(hash1);
            AbstractTreeIterator newTreeParser = prepareTreeParser(hash2);

            List<DiffEntry> diff = git.diff().
                    setOldTree(oldTreeParser).
                    setNewTree(newTreeParser).
                    setPathFilter(PathFilter.create(getFileName())).call();
            for (DiffEntry entry : diff) {
                System.out.println("Entry: " + entry + ", from: " + entry.getOldId() + ", to: " + entry.getNewId());
                try (DiffFormatter formatter = new DiffFormatter(System.out)) {
                    formatter.setRepository(repository);
                    formatter.format(entry);
                }
            }
            return diff;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    private AbstractTreeIterator prepareTreeParser(String objectId) throws IOException {
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
}
