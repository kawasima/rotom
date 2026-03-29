package net.unit8.rotom.model;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageTest {

    @Nested
    class PathOnlyConstructor {

        @Test
        void regularFilePath() {
            Page page = new Page("docs/guide.md");
            assertEquals("docs/guide.md", page.getPath());
            assertTrue(page.isRegularFile());
        }

        @Test
        void directoryPath() {
            Page page = new Page("docs/");
            assertFalse(page.isRegularFile());
        }

        @Test
        void trailingSlashesStripped() {
            Page page = new Page("docs///");
            assertEquals("docs", page.getPath());
        }
    }

    @Nested
    class NameParsing {

        private Page pageWithBlob(String path) {
            BlobEntry blob = new BlobEntry(path, ObjectId.zeroId(),
                    new PersonIdent("test", "test@example.com"), 0,
                    () -> "content".getBytes());
            return new Page(path, blob);
        }

        @Test
        void nameStripsExtensionAndReplacesHyphens() {
            Page page = pageWithBlob("hello-world.md");
            assertEquals("hello world", page.getName());
        }

        @Test
        void fileNameIncludesExtension() {
            Page page = pageWithBlob("hello-world.md");
            assertEquals("hello-world.md", page.getFileName());
        }

        @Test
        void urlPathStripsExtension() {
            Page page = pageWithBlob("docs/hello-world.md");
            assertEquals("docs/hello-world", page.getUrlPath());
        }

        @Test
        void dirExtractedFromPath() {
            Page page = pageWithBlob("docs/sub/page.md");
            assertEquals("docs/sub", page.getDir());
        }

        @Test
        void rootLevelPageHasEmptyDir() {
            Page page = pageWithBlob("page.md");
            assertEquals("", page.getDir());
        }

        @Test
        void formatDetectedFromExtension() {
            assertEquals("Markdown", pageWithBlob("page.md").getFormat());
            assertEquals("Textile", pageWithBlob("page.textile").getFormat());
            assertEquals("Plain Text", pageWithBlob("page.txt").getFormat());
        }

        @Test
        void multipleDotsInName() {
            Page page = pageWithBlob("v1.2.3.md");
            assertEquals("v1.2.3", page.getName());
        }
    }

    @Nested
    class FormattedData {

        private Page pageWithContent(String filename, String content) {
            BlobEntry blob = new BlobEntry(filename, ObjectId.zeroId(),
                    new PersonIdent("test", "test@example.com"), 0,
                    () -> content.getBytes());
            return new Page(filename, blob);
        }

        @Test
        void markdownRenderedToHtml() {
            Page page = pageWithContent("test.md", "# Hello");
            String html = page.getFormattedData();
            assertTrue(html.contains("<h1"), html);
        }

        @Test
        void textReturnedAsIs() {
            Page page = pageWithContent("test.txt", "plain text");
            assertEquals("plain text", page.getFormattedData().trim());
        }

        @Test
        void emptyParagraphsRemoved() {
            Page page = pageWithContent("test.md", "");
            assertFalse(page.getFormattedData().contains("<p></p>"));
        }
    }
}
