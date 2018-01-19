package net.unit8.rotom.model;

import enkan.system.EnkanSystem;
import enkan.util.BeanBuilder;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

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

        EnkanSystem system = EnkanSystem.of(
                "wiki", BeanBuilder.builder(new Wiki())
                        .set(Wiki::setRepositoryPath, Paths.get("target/wiki"))
                        .build()
        );
        system.start();
        Wiki wiki = system.getComponent("wiki");

        wiki.writePage("home", "markdown", "# Home page\n\n- a\n- b".getBytes(), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        wiki.writePage("fuga", "markdown", "# Fuga page\n\n- a\n- b\n- c".getBytes(), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        Page page = wiki.getPage("home");
        System.out.println(page.getFormattedData());

        wiki.updatePage(page, null, null, "# Home page2\n\n- a\n- b\n- c".getBytes(),
                new Commit("kawasima", "kawasima1016@gmail.com", "create 2"));

        page = wiki.getPage("home");
        System.out.println(page.getFormattedData());

        page.getVersions()
                .stream()
                .map(commit -> commit.getId() + " " + commit.getAuthorIdent() + " " + commit.getShortMessage())
                .forEach(System.out::println);
    }

    @Test
    public void getPages() throws IOException {
        Path wikiDir = Paths.get("target/wiki");
        if (Files.exists(wikiDir)) {
            Files.walk(Paths.get("target/wiki"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        EnkanSystem system = EnkanSystem.of(
                "wiki", BeanBuilder.builder(new Wiki())
                        .set(Wiki::setRepositoryPath, Paths.get("target/wiki"))
                        .build()
        );
        system.start();
        Wiki wiki = system.getComponent("wiki");

        wiki.writePage("home", "markdown", "# Home page\n\n- a\n- b".getBytes(), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        wiki.writePage("a/b/test", "markdown", "# Test page\n\n- a\n- b".getBytes(), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        wiki.getPages("").stream()
                .map(p -> p.getPath())
                .forEach(System.out::println);
        System.out.println(wiki.getPages("a/b/"));
    }

    @Test
    public void diff() throws Exception {
        Path wikiDir = Paths.get("target/wiki");
        if (Files.exists(wikiDir)) {
            Files.walk(Paths.get("target/wiki"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        EnkanSystem system = EnkanSystem.of(
                "wiki", BeanBuilder.builder(new Wiki())
                        .set(Wiki::setRepositoryPath, Paths.get("target/wiki"))
                        .build()
        );
        system.start();
        Wiki wiki = system.getComponent("wiki");

        wiki.writePage("home", "markdown", "# Home page\n\n- a\n- b".getBytes(), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        Page page = wiki.getPage("home");
        wiki.updatePage(page, null, null, "# Test page\n\n- a\n- b".getBytes(),
                new Commit("kawasima", "kawasima1016@gmail.com", "updated"));
        List<RevCommit> versions = page.getVersions();
        System.out.println(page.getDiff(versions.get(0).getId().getName(),
                versions.get(1).getId().getName()));
    }
}
