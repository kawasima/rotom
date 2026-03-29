package net.unit8.rotom.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class MarkupTypeTest {

    @ParameterizedTest
    @CsvSource({
            "page.md, true",
            "page.markdown, true",
            "page.mdown, true",
            "page.mkdn, true",
            "page.mkd, true",
            "page.textile, false",
            "page.txt, false",
            "page.html, false"
    })
    void markdownMatchesCorrectExtensions(String filename, boolean expected) {
        assertEquals(expected, MarkupType.MARKDOWN.match(filename));
    }

    @Test
    void textileMatchesCorrectExtension() {
        assertTrue(MarkupType.TEXTILE.match("page.textile"));
        assertFalse(MarkupType.TEXTILE.match("page.md"));
    }

    @Test
    void txtMatchesCorrectExtension() {
        assertTrue(MarkupType.TXT.match("page.txt"));
        assertFalse(MarkupType.TXT.match("page.md"));
    }

    @Test
    void getNameReturnsDisplayName() {
        assertEquals("Markdown", MarkupType.MARKDOWN.getName());
        assertEquals("Textile", MarkupType.TEXTILE.getName());
        assertEquals("Plain Text", MarkupType.TXT.getName());
    }

    @Test
    void getExtensionReturnsFileExtension() {
        assertEquals("md", MarkupType.MARKDOWN.getExtension());
        assertEquals("textile", MarkupType.TEXTILE.getExtension());
        assertEquals("txt", MarkupType.TXT.getExtension());
    }

    @Test
    void renderDelegatesToMarkup() {
        String html = MarkupType.MARKDOWN.render("# Hello");
        assertTrue(html.contains("<h1"));
    }

    @Test
    void configureAllDoesNotThrow() {
        assertDoesNotThrow(() -> MarkupType.configureAll("/wiki"));
    }
}
