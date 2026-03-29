package net.unit8.rotom.model;

import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Committer, including regression tests for bugs found during review.
 */
class CommitterTest {

    @TempDir
    File tempDir;

    @Test
    void addToIndexWithNullFormatThrows() throws Exception {
        var repo = FileRepositoryBuilder.create(new File(tempDir, ".git"));
        repo.create();
        Committer committer = new Committer(repo, "master");

        // BUG: Previously threw NullPointerException on format.toUpperCase()
        assertThrows(IllegalArgumentException.class,
                () -> committer.addToIndex("", "page", null, "data".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void addToIndexWithBlankNameThrows() throws Exception {
        var repo = FileRepositoryBuilder.create(new File(tempDir, ".git"));
        repo.create();
        Committer committer = new Committer(repo, "master");

        assertThrows(IllegalArgumentException.class,
                () -> committer.addToIndex("", "", "markdown", "data".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void addToIndexWithNullNameThrows() throws Exception {
        var repo = FileRepositoryBuilder.create(new File(tempDir, ".git"));
        repo.create();
        Committer committer = new Committer(repo, "master");

        assertThrows(IllegalArgumentException.class,
                () -> committer.addToIndex("", null, "markdown", "data".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void addToIndexWithValidInputsSucceeds() throws Exception {
        var repo = FileRepositoryBuilder.create(new File(tempDir, ".git"));
        repo.create();
        Committer committer = new Committer(repo, "master");

        assertDoesNotThrow(
                () -> committer.addToIndex("docs", "page", "markdown", "# Hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void addToIndexWithNullDirSucceeds() throws Exception {
        var repo = FileRepositoryBuilder.create(new File(tempDir, ".git"));
        repo.create();
        Committer committer = new Committer(repo, "master");

        // BUG: Previously threw NPE on dir.replace(' ', '-') when dir was null
        assertDoesNotThrow(
                () -> committer.addToIndex(null, "page", "markdown", "content".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void commitAfterAddToIndex() throws Exception {
        var repo = FileRepositoryBuilder.create(new File(tempDir, ".git"));
        repo.create();
        Committer committer = new Committer(repo, "master");
        committer.addToIndex("", "test", "markdown", "# Test".getBytes(StandardCharsets.UTF_8));

        var commitId = committer.commit(new Commit("user", "user@example.com", "initial"));
        assertNotNull(commitId);
    }
}
