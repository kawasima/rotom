package net.unit8.rotom.model.markup;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import net.unit8.rotom.model.Markup;

public class MarkdownMarkup implements Markup {
    private Parser parser;
    private HtmlRenderer renderer;

    public MarkdownMarkup() {
        parser = Parser.builder().build();
        renderer = HtmlRenderer.builder().build();
    }

    public String render(String source) {
        Document doc = parser.parse(source);
        return renderer.render(doc);
    }
}
