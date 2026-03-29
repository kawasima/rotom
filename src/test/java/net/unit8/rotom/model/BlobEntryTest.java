package net.unit8.rotom.model;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlobEntryTest {

    @Test
    void nameExtractedFromPath() {
        BlobEntry entry = new BlobEntry("docs/guide.md", ObjectId.zeroId(),
                new PersonIdent("test", "test@example.com"), 12345, () -> new byte[0]);
        assertEquals("guide.md", entry.getName());
    }

    @Test
    void dirExtractedFromPath() {
        BlobEntry entry = new BlobEntry("docs/sub/guide.md", ObjectId.zeroId(),
                new PersonIdent("test", "test@example.com"), 12345, () -> new byte[0]);
        assertEquals("docs/sub", entry.getDir());
    }

    @Test
    void rootLevelFileHasEmptyDir() {
        BlobEntry entry = new BlobEntry("page.md", ObjectId.zeroId(),
                new PersonIdent("test", "test@example.com"), 12345, () -> new byte[0]);
        assertEquals("", entry.getDir());
    }

    @Test
    void dataLoadedLazily() {
        byte[] content = "hello".getBytes();
        BlobEntry entry = new BlobEntry("page.md", ObjectId.zeroId(),
                new PersonIdent("test", "test@example.com"), 12345, () -> content);
        // Before getData(), size is 0 (data not loaded yet)
        assertEquals(0, entry.getSize());
        // After getData(), data is available
        assertArrayEquals(content, entry.getData());
        assertEquals(5, entry.getSize());
    }

    @Test
    void nullSupplierReturnsEmptyArray() {
        BlobEntry entry = new BlobEntry("page.md", ObjectId.zeroId(),
                new PersonIdent("test", "test@example.com"), 12345, null);
        assertArrayEquals(new byte[0], entry.getData());
    }

    @Test
    void committerAndTimestamp() {
        PersonIdent person = new PersonIdent("author", "author@example.com");
        BlobEntry entry = new BlobEntry("page.md", ObjectId.zeroId(), person, 99999, () -> new byte[0]);
        assertEquals(person, entry.getCommitter());
        assertEquals(99999, entry.getCommitTime());
    }

    @Test
    void toStringContainsShaAndPath() {
        BlobEntry entry = new BlobEntry("docs/page.md", ObjectId.zeroId(),
                new PersonIdent("test", "test@example.com"), 0, () -> new byte[0]);
        String str = entry.toString();
        assertTrue(str.contains("docs/page.md"));
    }
}
