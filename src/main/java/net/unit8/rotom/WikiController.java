package net.unit8.rotom;

import enkan.component.BeansConverter;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;

import javax.inject.Inject;

public class WikiController {
    @Inject
    private BeansConverter beansConverter;

    @Inject
    private TemplateEngine templateEngine;

    public HttpResponse index() {
        return templateEngine.render("");
    }
}
