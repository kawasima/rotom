package net.unit8.rotom.model;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class WikiTest {
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
    public void test() throws IOException {
        Path wikiDir = Paths.get("target/wiki");
        if (Files.exists(wikiDir)) {
            Files.walk(Paths.get("target/wiki"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        Wiki wiki = new Wiki("target/wiki");
        wiki.writePage("home.md", "markdown", "# Home page\n\n- a\n- b".getBytes(), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        wiki.writePage("fuga.md", "markdown", "# Fuga page\n\n- a\n- b\n- c".getBytes(), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        Page page = wiki.getPage("home.md");
        System.out.println(page.toFormattedData());

        wiki.updatePage(page, null, null, "# Home page2\n\n- a\n- b\n- c".getBytes(),
                new Commit("kawasima", "kawasima1016@gmail.com", "create 2"));

        page = wiki.getPage("home.md");
        System.out.println(page.toFormattedData());

        page.getVersions()
                .stream()
                .map(commit -> commit.getId() + " " + commit.getAuthorIdent() + " " + commit.getShortMessage())
                .forEach(System.out::println);
    }
}
