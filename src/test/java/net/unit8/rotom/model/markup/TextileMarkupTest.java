package net.unit8.rotom.model.markup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextileMarkupTest {

    private final TextileMarkup markup = new TextileMarkup();

    @Test
    void rendersHeading() {
        String html = markup.render("h1. Hello");
        assertTrue(html.contains("<h1"), html);
        assertTrue(html.contains("Hello"), html);
    }

    @Test
    void rendersBold() {
        String html = markup.render("*bold*");
        assertTrue(html.contains("<strong>bold</strong>") || html.contains("<b>bold</b>"), html);
    }

    @Test
    void rendersItalic() {
        String html = markup.render("_italic_");
        assertTrue(html.contains("<em>italic</em>") || html.contains("<i>italic</i>"), html);
    }

    @Test
    void rendersList() {
        String html = markup.render("* item1\n* item2");
        assertTrue(html.contains("<li>"), html);
    }

    @Test
    void rendersLink() {
        String html = markup.render("\"Example\":http://example.com");
        assertTrue(html.contains("http://example.com"), html);
        assertTrue(html.contains("Example"), html);
    }

    @Test
    void emptyStringReturnsEmpty() {
        String html = markup.render("");
        assertNotNull(html);
    }
}
