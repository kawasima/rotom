package net.unit8.rotom.model.markup;

import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.builder.HtmlDocumentBuilder;
import net.java.textilej.parser.markup.Dialect;
import net.java.textilej.parser.markup.textile.TextileDialect;
import net.unit8.rotom.model.Markup;

import java.io.StringWriter;

public class TextileMarkup implements Markup {
    private Dialect dialect;

    public TextileMarkup() {
        this.dialect = new TextileDialect();
    }
    @Override
    public String render(String source) {
        StringWriter sw = new StringWriter();
        HtmlDocumentBuilder documentBuilder = new HtmlDocumentBuilder(sw);
        MarkupParser parser = new MarkupParser(dialect);
        parser.setBuilder(documentBuilder);
        parser.parse(source);

        return sw.toString();
    }
}
