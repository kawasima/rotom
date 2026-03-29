package net.unit8.rotom.model.markup;

import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import net.unit8.rotom.model.Markup;

import java.util.List;

public class MarkdownMarkup implements Markup {
    private Parser parser;
    private HtmlRenderer renderer;

    public MarkdownMarkup() {
        buildWithOptions("");
    }

    @Override
    public void configure(String basePath) {
        buildWithOptions(basePath);
    }

    private void buildWithOptions(String basePath) {
        String linkPrefix = basePath + "/";

        MutableDataSet options = new MutableDataSet();
        options.set(WikiLinkExtension.LINK_PREFIX, linkPrefix);
        options.set(WikiLinkExtension.LINK_FILE_EXTENSION, "");
        options.set(WikiLinkExtension.LINK_FIRST_SYNTAX, false);
        options.set(WikiLinkExtension.LINK_ESCAPE_CHARS, " ");
        options.set(WikiLinkExtension.LINK_REPLACE_CHARS, "-");
        options.set(WikiLinkExtension.IMAGE_LINKS, true);
        options.set(WikiLinkExtension.IMAGE_PREFIX, linkPrefix);
        options.set(WikiLinkExtension.IMAGE_FILE_EXTENSION, "");

        List<Extension> extensions = List.of(
                WikiLinkExtension.create(),
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create(),
                AutolinkExtension.create(),
                AnchorLinkExtension.create()
        );

        parser = Parser.builder(options)
                .extensions(extensions)
                .build();

        renderer = HtmlRenderer.builder(options)
                .extensions(extensions)
                .build();
    }

    @Override
    public String render(String source) {
        Document doc = parser.parse(source);
        return renderer.render(doc);
    }
}
