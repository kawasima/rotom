package net.unit8.rotom.model.markup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextMarkupTest {

    private final TextMarkup markup = new TextMarkup();

    @Test
    void returnsSourceUnchanged() {
        assertEquals("hello world", markup.render("hello world"));
    }

    @Test
    void htmlNotEscaped() {
        assertEquals("<b>bold</b>", markup.render("<b>bold</b>"));
    }

    @Test
    void emptyStringReturnsEmpty() {
        assertEquals("", markup.render(""));
    }
}
