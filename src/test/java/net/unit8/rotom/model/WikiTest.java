package net.unit8.rotom.model;

import enkan.collection.OptionMap;
import enkan.system.EnkanSystem;
import enkan.util.BeanBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WikiTest {

    private Wiki createWiki() throws IOException {
        Path wikiDir = Paths.get("target/wiki");
        if (Files.exists(wikiDir)) {
            Files.walk(wikiDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        EnkanSystem system = EnkanSystem.of(
                "wiki", BeanBuilder.builder(new Wiki())
                        .set(Wiki::setRepository, FileRepositoryBuilder.create(new File("target/wiki")))
                        .build()
        );
        system.start();
        return system.getComponent("wiki");
    }

    @Test
    void writeThenReadPage() throws IOException {
        Wiki wiki = createWiki();

        wiki.writePage("home", "markdown", "# Home page\n\n- a\n- b".getBytes(StandardCharsets.UTF_8), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        Page page = wiki.getPage("home");
        assertNotNull(page);
        assertEquals("home", page.getName());
        assertTrue(page.getFormattedData().contains("<h1"));
    }

    @Test
    void updatePageCreatesNewVersion() throws IOException {
        Wiki wiki = createWiki();

        wiki.writePage("home", "markdown", "# Home page\n\n- a\n- b".getBytes(StandardCharsets.UTF_8), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        Page page = wiki.getPage("home");
        wiki.updatePage(page, null, null, "# Updated\n\n- a\n- b\n- c".getBytes(StandardCharsets.UTF_8),
                new Commit("kawasima", "kawasima1016@gmail.com", "update"));

        Page updated = wiki.getPage("home");
        assertTrue(updated.getFormattedData().contains("Updated"));

        List<RevCommit> versions = wiki.getVersions(OptionMap.of("path", updated.getPath()));
        assertEquals(2, versions.size());
    }

    @Test
    void deletePageRemovesIt() throws IOException {
        Wiki wiki = createWiki();

        wiki.writePage("home", "markdown", "# Home\n".getBytes(StandardCharsets.UTF_8), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        Page page = wiki.getPage("home");
        assertNotNull(page);

        wiki.deletePage(page, new Commit("kawasima", "kawasima1016@gmail.com", "delete"));
        assertNull(wiki.getPage("home"));
    }

    @Test
    void getPagesListsFiles() throws IOException {
        Wiki wiki = createWiki();

        wiki.writePage("home", "markdown", "# Home".getBytes(StandardCharsets.UTF_8), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));
        wiki.writePage("test", "markdown", "# Test".getBytes(StandardCharsets.UTF_8), "a/b",
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        List<Page> rootPages = wiki.getPages("");
        assertFalse(rootPages.isEmpty());
        assertTrue(rootPages.stream().anyMatch(p -> p.getPath().contains("home")));
    }

    @Test
    void getDiffReturnsDiffText() throws IOException {
        Wiki wiki = createWiki();

        wiki.writePage("home", "markdown", "# Home page\n".getBytes(StandardCharsets.UTF_8), null,
                new Commit("kawasima", "kawasima1016@gmail.com", "init"));

        Page page = wiki.getPage("home");
        wiki.updatePage(page, null, null, "# Updated page\n".getBytes(StandardCharsets.UTF_8),
                new Commit("kawasima", "kawasima1016@gmail.com", "updated"));

        page = wiki.getPage("home");
        List<RevCommit> versions = wiki.getVersions(OptionMap.of("path", page.getPath()));
        String diff = wiki.getDiff(page,
                versions.get(0).getId().getName(),
                versions.get(1).getId().getName());
        assertNotNull(diff);
        assertFalse(diff.isEmpty());
    }

    @Test
    void getNonexistentPageReturnsNull() throws IOException {
        Wiki wiki = createWiki();
        assertNull(wiki.getPage("does-not-exist"));
    }

    @Test
    void emptyWikiGetPagesReturnsEmpty() throws IOException {
        Wiki wiki = createWiki();
        assertTrue(wiki.getPages("").isEmpty());
    }

    @Test
    void writePageWithNullDir() throws IOException {
        Wiki wiki = createWiki();
        wiki.writePage("test", "markdown", "content".getBytes(StandardCharsets.UTF_8), null,
                new Commit("user", "user@example.com", "init"));
        assertNotNull(wiki.getPage("test"));
    }

    @Test
    void writePageWithEmptyDir() throws IOException {
        Wiki wiki = createWiki();
        wiki.writePage("test", "markdown", "content".getBytes(StandardCharsets.UTF_8), "",
                new Commit("user", "user@example.com", "init"));
        assertNotNull(wiki.getPage("test"));
    }

    @Test
    void writePageWithSubdirectory() throws IOException {
        Wiki wiki = createWiki();
        wiki.writePage("guide", "markdown", "# Guide".getBytes(StandardCharsets.UTF_8), "docs",
                new Commit("user", "user@example.com", "init"));
        assertNotNull(wiki.getPage("docs/guide"));
    }

    @Test
    void spacesInNameConvertedToHyphens() throws IOException {
        Wiki wiki = createWiki();
        wiki.writePage("my page", "markdown", "content".getBytes(StandardCharsets.UTF_8), null,
                new Commit("user", "user@example.com", "init"));
        assertNotNull(wiki.getPage("my-page"));
    }

    @Test
    void fullpathWithNullDir() {
        assertEquals("name", Wiki.fullpath(null, "name"));
    }

    @Test
    void fullpathWithEmptyDir() {
        assertEquals("name", Wiki.fullpath("", "name"));
    }

    @Test
    void fullpathCombinesDirAndName() {
        assertEquals("docs/name", Wiki.fullpath("docs", "name"));
    }

    @Test
    void fullpathStripsTrailingSlash() {
        assertEquals("docs/name", Wiki.fullpath("docs/", "name"));
    }
}
