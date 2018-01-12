package net.unit8.rotom;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import net.unit8.rotom.model.Page;
import net.unit8.rotom.model.Wiki;

import javax.inject.Inject;

public class WikiController {
    @Inject
    private BeansConverter beansConverter;

    @Inject
    private Wiki wiki;

    @Inject
    private TemplateEngine templateEngine;

    public HttpResponse index() {
        return templateEngine.render("");
    }

    public HttpResponse showPageOrFile(Parameters params) {
        String path = params.get("path");
        Page page = wiki.getPage(path);
        return templateEngine.render("page",
                "page", page);
    }
}
