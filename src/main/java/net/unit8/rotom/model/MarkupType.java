package net.unit8.rotom.model;

import net.unit8.rotom.model.markup.MarkdownMarkup;
import net.unit8.rotom.model.markup.TextMarkup;

import java.util.regex.Pattern;

public enum MarkupType {
    MARKDOWN("MarkdownMarkup", "md", "md|mkdn?|mdown|markdown", new MarkdownMarkup()),
    TXT("Plain Text", "txt", "txt", new TextMarkup());

    private Pattern extPattern;
    private String extension;
    private String name;
    private Markup markup;

    MarkupType(String name, String extension, String ptn, Markup markup) {
        this.name = name;
        this.extension = extension;
        extPattern = Pattern.compile("\\.(" + ptn + ")$");
        this.markup = markup;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public String render(String source) {
        return markup.render(source);
    }

    public boolean match(String filename) {
        return extPattern.matcher(filename).find();
    }
}