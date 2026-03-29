package net.unit8.rotom.model.markup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownMarkupTest {
    private MarkdownMarkup markup;

    @BeforeEach
    void setUp() {
        markup = new MarkdownMarkup();
    }

    @Test
    void wikiLink() {
        String html = markup.render("[[PageName]]");
        assertTrue(html.contains("<a href=\"/PageName\""), html);
        assertTrue(html.contains(">PageName</a>"), html);
    }

    @Test
    void wikiLinkWithDisplayText() {
        String html = markup.render("[[Display Text|PageName]]");
        assertTrue(html.contains("<a href=\"/PageName\""), html);
        assertTrue(html.contains(">Display Text</a>"), html);
    }

    @Test
    void wikiLinkWithBasePath() {
        MarkdownMarkup configured = new MarkdownMarkup();
        configured.configure("/wiki");
        String html = configured.render("[[PageName]]");
        assertTrue(html.contains("<a href=\"/wiki/PageName\""), html);
    }

    @Test
    void wikiLinkSpaceToHyphen() {
        String html = markup.render("[[Page Name]]");
        assertTrue(html.contains("href=\"/Page-Name\""), html);
    }

    @Test
    void gfmTable() {
        String md = """
                | col1 | col2 |
                |------|------|
                | a    | b    |
                """;
        String html = markup.render(md);
        assertTrue(html.contains("<table>"), html);
        assertTrue(html.contains("<td>a</td>"), html);
    }

    @Test
    void strikethrough() {
        String html = markup.render("~~deleted~~");
        assertTrue(html.contains("<del>deleted</del>"), html);
    }

    @Test
    void taskList() {
        String html = markup.render("- [ ] todo\n- [x] done");
        assertTrue(html.contains("type=\"checkbox\""), html);
    }

    @Test
    void autolink() {
        String html = markup.render("Visit http://example.com for more.");
        assertTrue(html.contains("<a href=\"http://example.com\""), html);
    }

    @Test
    void headingAnchor() {
        String html = markup.render("## Section Title");
        assertTrue(html.contains("id=\"section-title\"") || html.contains("id=\"Section-Title\"")
                || (html.contains("<a") && html.contains("section-title")), html);
    }
}
