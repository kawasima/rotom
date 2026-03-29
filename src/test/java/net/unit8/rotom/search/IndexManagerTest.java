package net.unit8.rotom.search;

import enkan.system.EnkanSystem;
import enkan.util.BeanBuilder;
import net.unit8.rotom.model.Commit;
import net.unit8.rotom.model.Wiki;
import net.unit8.rotom.model.Page;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class IndexManagerTest {

    private Wiki wiki;
    private IndexManager indexManager;
    private EnkanSystem system;

    @BeforeEach
    void setUp() throws IOException {
        for (String dir : new String[]{"target/test-wiki", "target/test-index"}) {
            Path p = Paths.get(dir);
            if (Files.exists(p)) {
                Files.walk(p).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }

        system = EnkanSystem.of(
                "wiki", BeanBuilder.builder(new Wiki())
                        .set(Wiki::setRepository, FileRepositoryBuilder.create(new File("target/test-wiki")))
                        .build(),
                "index", BeanBuilder.builder(new IndexManager())
                        .set(IndexManager::setIndexPath, Paths.get("target/test-index"))
                        .build()
        );
        system.start();
        wiki = system.getComponent("wiki");
        indexManager = system.getComponent("index");
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.stop();
        }
    }

    private void awaitIndex() throws InterruptedException {
        indexManager.awaitPendingOperations();
    }

    @Test
    void searchEmptyQueryReturnsEmpty() {
        Pagination<FoundPage> result = indexManager.search("", 0, 10);
        assertNotNull(result);
        assertTrue(result.getResults().isEmpty());
        assertEquals(0, result.getTotalHits());
        assertTrue(result.isExact());
    }

    @Test
    void searchNullQueryReturnsEmpty() {
        Pagination<FoundPage> result = indexManager.search(null, 0, 10);
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    void saveAndSearchFindsDocument() throws Exception {
        wiki.writePage("test", "markdown", "# Hello World\nSome content here".getBytes(StandardCharsets.UTF_8),
                null, new Commit("tester", "test@example.com", "init"));
        Page page = wiki.getPage("test");
        indexManager.save(page);
        awaitIndex();

        Pagination<FoundPage> result = indexManager.search("Hello", 0, 10);
        assertFalse(result.getResults().isEmpty(), "Should find the indexed document");
        assertEquals(1, result.getResults().size());
    }

    @Test
    void searchRespectsLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            wiki.writePage("page" + i, "markdown",
                    ("# Page " + i + "\nSearchable content").getBytes(StandardCharsets.UTF_8),
                    null, new Commit("tester", "test@example.com", "create page " + i));
            indexManager.save(wiki.getPage("page" + i));
        }
        awaitIndex();

        Pagination<FoundPage> limited = indexManager.search("Searchable", 0, 2);
        assertTrue(limited.getResults().size() <= 2, "Should respect limit");
    }

    @Test
    void deleteAllClearsIndex() throws Exception {
        wiki.writePage("test", "markdown", "# Deletable content".getBytes(StandardCharsets.UTF_8),
                null, new Commit("tester", "test@example.com", "init"));
        indexManager.save(wiki.getPage("test"));
        awaitIndex();

        indexManager.deleteAll();
        awaitIndex();

        Pagination<FoundPage> result = indexManager.search("Deletable", 0, 10);
        assertTrue(result.getResults().isEmpty(), "Index should be empty after deleteAll");
    }

    @Test
    void searchEscapesSpecialCharactersWithoutThrowing() {
        // BUG: Without QueryParser.escape(), Lucene special chars would throw ParseException
        assertDoesNotThrow(() -> indexManager.search("hello AND world OR (foo)", 0, 10));
        assertDoesNotThrow(() -> indexManager.search("test:query", 0, 10));
        assertDoesNotThrow(() -> indexManager.search("[brackets]", 0, 10));
        assertDoesNotThrow(() -> indexManager.search("a+b-c", 0, 10));
    }

    @Test
    void paginationMetadataIsCorrect() throws Exception {
        wiki.writePage("meta", "markdown", "# Metadata test content".getBytes(StandardCharsets.UTF_8),
                null, new Commit("tester", "test@example.com", "init"));
        indexManager.save(wiki.getPage("meta"));
        awaitIndex();

        Pagination<FoundPage> result = indexManager.search("Metadata", 0, 10);
        assertEquals(0, result.getOffset());
        assertEquals(10, result.getLimit());
        assertEquals(1, result.getTotalHits());
        assertTrue(result.isExact());
    }

    @Test
    void foundPageHasCorrectFields() throws Exception {
        wiki.writePage("detail", "markdown", "# Detail page content".getBytes(StandardCharsets.UTF_8),
                null, new Commit("tester", "test@example.com", "init"));
        indexManager.save(wiki.getPage("detail"));
        awaitIndex();

        Pagination<FoundPage> result = indexManager.search("Detail", 0, 10);
        assertFalse(result.getResults().isEmpty());
        FoundPage found = result.getResults().get(0);
        assertNotNull(found.getPath());
        assertNotNull(found.getName());
        assertTrue(found.getScore() > 0);
    }

    @Test
    void updateExistingDocumentReplacesIt() throws Exception {
        wiki.writePage("evolve", "markdown", "# Version 1 original".getBytes(StandardCharsets.UTF_8),
                null, new Commit("tester", "test@example.com", "v1"));
        indexManager.save(wiki.getPage("evolve"));
        awaitIndex();

        // Update the page
        Page page = wiki.getPage("evolve");
        wiki.updatePage(page, null, null, "# Version 2 replacement".getBytes(StandardCharsets.UTF_8),
                new Commit("tester", "test@example.com", "v2"));
        indexManager.save(wiki.getPage("evolve"));
        awaitIndex();

        // Old content should not be found
        Pagination<FoundPage> oldResult = indexManager.search("original", 0, 10);
        assertTrue(oldResult.getResults().isEmpty(), "Old content should be replaced");

        // New content should be found
        Pagination<FoundPage> newResult = indexManager.search("replacement", 0, 10);
        assertFalse(newResult.getResults().isEmpty(), "New content should be indexed");
    }
}
