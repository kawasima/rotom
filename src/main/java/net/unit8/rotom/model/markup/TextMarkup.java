package net.unit8.rotom.model.markup;

import net.unit8.rotom.model.Markup;

public class TextMarkup implements Markup {
    @Override
    public String render(String source) {
        return source;
    }
}
