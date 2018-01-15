package net.unit8.rotom.model;

import net.unit8.rotom.model.filter.Render;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.nio.charset.StandardCharsets;
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

    public String toString() {
        return new String(blob.getData(), StandardCharsets.UTF_8);
    }

    public String toFormattedData() {
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
}
