package net.unit8.rotom;

import enkan.collection.Parameters;
import enkan.component.ComponentLifecycle;
import enkan.data.HttpResponse;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.system.EnkanSystem;
import enkan.util.BeanBuilder;
import kotowari.component.TemplateEngine;
import net.unit8.rotom.model.Commit;
import net.unit8.rotom.model.Wiki;
import net.unit8.rotom.search.IndexManager;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WikiController that invoke endpoint methods directly.
 * These tests verify the full request-handling logic including bug-fix regressions.
 */
class WikiControllerIntegrationTest {

    private Wiki wiki;
    private IndexManager indexManager;
    private WikiController controller;
    private EnkanSystem system;

    /** Minimal TemplateEngine stub that records the last rendered template name. */
    private static class StubTemplateEngine extends TemplateEngine<StubTemplateEngine> {
        String lastTemplate;

        @Override
        public HttpResponse render(String name, Object... keyOrVals) {
            lastTemplate = name;
            HttpResponse response = HttpResponse.of("<html>" + name + "</html>");
            response.setContentType("text/html; charset=utf-8");
            return response;
        }

        @Override
        public Object createFunction(java.util.function.Function<java.util.List<?>, Object> func) {
            return func;
        }

        @Override
        protected ComponentLifecycle<StubTemplateEngine> lifecycle() {
            return new ComponentLifecycle<>() {
                @Override public void start(StubTemplateEngine c) {}
                @Override public void stop(StubTemplateEngine c) {}
            };
        }
    }

    private static String locationOf(HttpResponse response) {
        Object header = response.getHeaders().get("Location");
        return header == null ? null : header.toString();
    }

    private static int statusOf(HttpResponse response) {
        return response.getStatus();
    }

    @BeforeEach
    void setUp() throws Exception {
        for (String dir : new String[]{"target/ctrl-test-wiki", "target/ctrl-test-index"}) {
            Path p = Paths.get(dir);
            if (Files.exists(p)) {
                Files.walk(p).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }

        StubTemplateEngine templateEngine = new StubTemplateEngine();

        system = EnkanSystem.of(
                "wiki", BeanBuilder.builder(new Wiki())
                        .set(Wiki::setRepository, FileRepositoryBuilder.create(new File("target/ctrl-test-wiki")))
                        .build(),
                "index", BeanBuilder.builder(new IndexManager())
                        .set(IndexManager::setIndexPath, Paths.get("target/ctrl-test-index"))
                        .build()
        );
        system.start();
        wiki = system.getComponent("wiki");
        indexManager = system.getComponent("index");

        controller = new WikiController();
        inject(controller, "wiki", wiki);
        inject(controller, "indexManager", indexManager);
        inject(controller, "templateEngine", templateEngine);
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.stop();
        }
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private StubTemplateEngine templateEngine() throws Exception {
        Field f = WikiController.class.getDeclaredField("templateEngine");
        f.setAccessible(true);
        return (StubTemplateEngine) f.get(controller);
    }

    private void awaitIndex() throws InterruptedException {
        indexManager.awaitPendingOperations();
    }

    private UserPermissionPrincipal principal(String name, String email) {
        return new UserPermissionPrincipal(1L, name,
                email == null ? Map.of() : Map.of("email", email),
                Set.of());
    }

    // ─── create() ────────────────────────────────────────────────────────────

    @Test
    void createWithNullContentRedirects() {
        // Regression: before fix, null content caused NPE in writePage
        Parameters params = Parameters.of("page", "mypage", "format", "markdown");
        // content is absent → null
        HttpResponse response = controller.create(params, null);
        assertEquals(303, statusOf(response));
        String location = locationOf(response);
        assertNotNull(location);
        assertTrue(location.contains("createForm"), "Should redirect to createForm, got: " + location);
    }

    @Test
    void createWithNullNameRedirects() {
        // name absent → null
        Parameters params = Parameters.of("content", "# Hello", "format", "markdown");
        HttpResponse response = controller.create(params, null);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("createForm"));
    }

    @Test
    void createWithBlankNameRedirects() {
        Parameters params = Parameters.of("page", "  ", "content", "# Hello", "format", "markdown");
        HttpResponse response = controller.create(params, null);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("createForm"));
    }

    @Test
    void createWithNullFormatRedirects() {
        // format absent → null
        Parameters params = Parameters.of("page", "mypage", "content", "# Hello");
        HttpResponse response = controller.create(params, null);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("createForm"));
    }

    @Test
    void createWithValidParamsCreatesPageAndRedirects() throws Exception {
        Parameters params = Parameters.of(
                "page", "newpage",
                "content", "# New Page",
                "format", "markdown");
        HttpResponse response = controller.create(params, principal("alice", "alice@example.com"));
        assertEquals(303, statusOf(response));
        String location = locationOf(response);
        assertTrue(location.contains("showPageOrFile"), "Should redirect to showPageOrFile, got: " + location);

        // Verify page was actually saved
        assertNotNull(wiki.getPage("newpage"), "Page should exist in wiki after create");
    }

    @Test
    void createWithDirCreatesPageUnderDir() throws Exception {
        Parameters params = Parameters.of(
                "page", "guide",
                "content", "# Guide",
                "format", "markdown",
                "dir", "docs");
        HttpResponse response = controller.create(params, null);
        assertEquals(303, statusOf(response));
        assertNotNull(wiki.getPage("docs/guide"), "Page should exist under docs/");
    }

    // ─── update() ────────────────────────────────────────────────────────────

    @Test
    void updateWithNullContentRedirects() throws Exception {
        wiki.writePage("existing", "markdown", "# Original".getBytes(StandardCharsets.UTF_8),
                null, new Commit("test", "test@example.com", "init"));

        Parameters params = Parameters.of("page", "existing", "path", "");
        // content absent → null
        HttpResponse response = controller.update(params, null);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("edit"), "Should redirect to edit, got: " + locationOf(response));
    }

    @Test
    void updateWithNullNameRedirects() throws Exception {
        wiki.writePage("existing", "markdown", "# Original".getBytes(StandardCharsets.UTF_8),
                null, new Commit("test", "test@example.com", "init"));

        Parameters params = Parameters.of("content", "# Updated", "path", "");
        // page (name) absent → null
        HttpResponse response = controller.update(params, null);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("edit"));
    }

    @Test
    void updateWithNonexistentPageRedirectsToCreateForm() throws Exception {
        Parameters params = Parameters.of(
                "page", "ghost",
                "content", "# Ghost",
                "path", "");
        HttpResponse response = controller.update(params, null);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("createForm"), "Missing page should redirect to createForm, got: " + locationOf(response));
    }

    @Test
    void updateValidPageSavesNewContent() throws Exception {
        wiki.writePage("updateme", "markdown", "# Version 1".getBytes(StandardCharsets.UTF_8),
                null, new Commit("test", "test@example.com", "init"));

        Parameters params = Parameters.of(
                "page", "updateme",
                "content", "# Version 2",
                "path", "");
        HttpResponse response = controller.update(params, principal("bob", "bob@example.com"));
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("showPageOrFile"));

        // Verify content was updated
        assertTrue(wiki.getPage("updateme").getFormattedData().contains("Version 2"));
    }

    // ─── delete() ────────────────────────────────────────────────────────────

    @Test
    void deleteExistingPageRemovesItAndRedirects() throws Exception {
        wiki.writePage("todelete", "markdown", "# Delete me".getBytes(StandardCharsets.UTF_8),
                null, new Commit("test", "test@example.com", "init"));
        assertNotNull(wiki.getPage("todelete"));

        Parameters params = Parameters.of("path", "todelete");
        HttpResponse response = controller.delete(params, null);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("showPageOrFile"));
        assertNull(wiki.getPage("todelete"), "Page should be deleted");
    }

    @Test
    void deleteNonexistentPageRedirectsWithoutError() {
        // Should not throw even if page does not exist
        Parameters params = Parameters.of("path", "no-such-page");
        HttpResponse response = controller.delete(params, null);
        assertEquals(303, statusOf(response));
    }

    @Test
    void deleteWithNullPathRedirectsWithoutError() {
        Parameters params = Parameters.empty();
        HttpResponse response = controller.delete(params, null);
        assertEquals(303, statusOf(response));
    }

    // ─── preview() ───────────────────────────────────────────────────────────

    @Test
    void previewMarkdownReturnsRenderedHtml() {
        Parameters params = Parameters.of("content", "# Preview", "format", "markdown");
        HttpResponse response = controller.preview(params);
        // Should be 200 with HTML body, not a redirect
        assertNull(locationOf(response), "Preview must not redirect");
        String body = response.getBodyAsString();
        assertTrue(body != null && (body.contains("<h1") || body.contains("Preview")),
                "Rendered HTML should contain h1 or text, got: " + body);
    }

    @Test
    void previewWithNullContentReturnsEmpty() {
        Parameters params = Parameters.empty();
        HttpResponse response = controller.preview(params);
        assertNull(locationOf(response));
    }

    @Test
    void previewWithUnknownFormatReturnsContentAsIs() {
        Parameters params = Parameters.of("content", "plain text", "format", "unknown");
        HttpResponse response = controller.preview(params);
        assertEquals("plain text", response.getBodyAsString());
    }

    // ─── restore() ───────────────────────────────────────────────────────────

    @Test
    void restoreWithInvalidSha1RedirectsToHistory() {
        Parameters params = Parameters.of(
                "path", "somepage",
                "sha1", "not-a-sha1");
        HttpResponse response = controller.restore(params, null);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("history"), "Invalid sha1 should redirect to history, got: " + locationOf(response));
    }

    @Test
    void restoreWithNullSha1RedirectsToHistory() {
        Parameters params = Parameters.of("path", "somepage");
        HttpResponse response = controller.restore(params, null);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("history"));
    }

    @Test
    void restoreWithNonexistentShaRedirectsToHistory() {
        Parameters params = Parameters.of(
                "path", "somepage",
                "sha1", "a".repeat(40));
        HttpResponse response = controller.restore(params, null);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("history"));
    }

    @Test
    void restoreValidShaRestoresPageContent() throws Exception {
        wiki.writePage("restore-target", "markdown", "# v1 content".getBytes(StandardCharsets.UTF_8),
                null, new Commit("test", "test@example.com", "v1"));
        var versions = wiki.getVersions(enkan.collection.OptionMap.of(
                "path", wiki.getPage("restore-target").getPath()));
        String v1sha = versions.get(0).getId().getName();

        // Update to v2
        wiki.updatePage(wiki.getPage("restore-target"), null, null,
                "# v2 content".getBytes(StandardCharsets.UTF_8),
                new Commit("test", "test@example.com", "v2"));
        assertTrue(wiki.getPage("restore-target").getFormattedData().contains("v2"));

        // Restore to v1
        Parameters params = Parameters.of(
                "path", "restore-target",
                "sha1", v1sha);
        HttpResponse response = controller.restore(params, null);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("showPageOrFile"));

        assertTrue(wiki.getPage("restore-target").getFormattedData().contains("v1"),
                "Content should be restored to v1");
    }

    // ─── search() ────────────────────────────────────────────────────────────

    @Test
    void searchWithQueryRendersSearchTemplate() throws Exception {
        wiki.writePage("searchable", "markdown", "# Findable content".getBytes(StandardCharsets.UTF_8),
                null, new Commit("test", "test@example.com", "init"));
        indexManager.save(wiki.getPage("searchable"));
        awaitIndex();

        Parameters params = Parameters.of("q", "Findable");
        HttpResponse response = controller.search(params);
        assertNull(locationOf(response), "Search should render, not redirect");
        assertEquals("search", templateEngine().lastTemplate);
    }

    @Test
    void searchWithEmptyQueryRendersSearchTemplate() throws Exception {
        Parameters params = Parameters.of("q", "");
        HttpResponse response = controller.search(params);
        assertNull(locationOf(response));
        assertEquals("search", templateEngine().lastTemplate);
    }

    @Test
    void searchWithSpecialCharsDoesNotThrow() {
        Parameters params = Parameters.of("q", "hello AND world OR (test)");
        assertDoesNotThrow(() -> controller.search(params));
    }

    // ─── doCompare() ─────────────────────────────────────────────────────────

    @Test
    void doCompareWithNonexistentPageRedirects() {
        // Regression: before fix, null page caused NPE
        Parameters params = Parameters.of("path", "nonexistent", "hash1", "abc", "hash2", "def");
        HttpResponse response = controller.doCompare(params);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("showPageOrFile"), "Missing page should redirect, got: " + locationOf(response));
    }

    @Test
    void doCompareWithValidPageRendersCompare() throws Exception {
        wiki.writePage("compare-page", "markdown", "# v1".getBytes(StandardCharsets.UTF_8),
                null, new Commit("test", "test@example.com", "v1"));
        wiki.updatePage(wiki.getPage("compare-page"), null, null,
                "# v2".getBytes(StandardCharsets.UTF_8),
                new Commit("test", "test@example.com", "v2"));

        var versions = wiki.getVersions(enkan.collection.OptionMap.of(
                "path", wiki.getPage("compare-page").getPath()));
        String hash1 = versions.get(1).getId().getName();
        String hash2 = versions.get(0).getId().getName();

        Parameters params = Parameters.of("path", "compare-page", "hash1", hash1, "hash2", hash2);
        HttpResponse response = controller.doCompare(params);
        assertNull(locationOf(response), "Should render compare, not redirect");
        assertEquals("compare", templateEngine().lastTemplate);
    }

    // ─── showPageOrFile() ────────────────────────────────────────────────────

    @Test
    void showNonexistentPageRedirectsToCreateForm() {
        Parameters params = Parameters.of("path", "doesnotexist");
        HttpResponse response = controller.showPageOrFile(params);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("createForm"));
    }

    @Test
    void showExistingPageRendersIt() throws Exception {
        wiki.writePage("showme", "markdown", "# Show me".getBytes(StandardCharsets.UTF_8),
                null, new Commit("test", "test@example.com", "init"));
        Parameters params = Parameters.of("path", "showme");
        HttpResponse response = controller.showPageOrFile(params);
        assertNull(locationOf(response), "Existing page should render, not redirect");
        assertEquals("page", templateEngine().lastTemplate);
    }

    // ─── edit() ──────────────────────────────────────────────────────────────

    @Test
    void editNonexistentPageRedirectsToCreateForm() {
        Parameters params = Parameters.of("path", "nosuchpage");
        HttpResponse response = controller.edit(params);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("createForm"));
    }

    @Test
    void editExistingPageRendersEditForm() throws Exception {
        wiki.writePage("editme", "markdown", "# Edit me".getBytes(StandardCharsets.UTF_8),
                null, new Commit("test", "test@example.com", "init"));
        Parameters params = Parameters.of("path", "editme");
        HttpResponse response = controller.edit(params);
        assertNull(locationOf(response));
        assertEquals("edit", templateEngine().lastTemplate);
    }

    // ─── history() ───────────────────────────────────────────────────────────

    @Test
    void historyNonexistentPageRedirects() {
        Parameters params = Parameters.of("path", "nosuchpage");
        HttpResponse response = controller.history(params);
        assertEquals(303, statusOf(response));
        assertTrue(locationOf(response).contains("showPageOrFile"));
    }

    @Test
    void historyExistingPageRendersHistory() throws Exception {
        wiki.writePage("histpage", "markdown", "# History".getBytes(StandardCharsets.UTF_8),
                null, new Commit("test", "test@example.com", "init"));
        Parameters params = Parameters.of("path", "histpage");
        HttpResponse response = controller.history(params);
        assertNull(locationOf(response));
        assertEquals("history", templateEngine().lastTemplate);
    }
}
