package net.unit8.rotom;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.data.HttpResponse;
import enkan.security.UserPrincipal;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.rotom.model.Commit;
import net.unit8.rotom.model.Page;
import net.unit8.rotom.model.Wiki;
import org.eclipse.jgit.lib.PersonIdent;

import javax.inject.Inject;

import java.security.Principal;
import java.util.Objects;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;

public class WikiController {
    @Inject
    private BeansConverter beansConverter;

    @Inject
    private Wiki wiki;

    @Inject
    private TemplateEngine templateEngine;

    public HttpResponse index() {
        return UrlRewriter.redirect(WikiController.class,
                "showPageOrFile?path=" + wiki.getIndexPage(),
                SEE_OTHER);
    }

    public HttpResponse createForm(Parameters params) {
        String path = params.get("path");
        return templateEngine.render("create",
                "isCreatePage", true,
                "isEditPage", false,
                "content", "",
                "format", "markdown");
    }

    public HttpResponse create(Parameters params, UserPermissionPrincipal principal) {
        String name = params.get("page");
        String path = params.get("path");
        String format = params.get("format");
        PersonIdent committer;
        if (principal != null) {
            committer = new PersonIdent(principal.getName(), Objects.toString(principal.getProfiles().get("email")));
        } else {
            committer = new PersonIdent("anonymous", "anonymous@example.com");
        }
        wiki.writePage(name, format, params.get("content").getBytes(), path,
                new Commit(committer.getName(), committer.getEmailAddress(), params.get("message")));
        return UrlRewriter.redirect(WikiController.class,
                "showPageOrFile?path=",
                SEE_OTHER);
    }

    public HttpResponse showPageOrFile(Parameters params) {
        String path = params.get("path");
        Page page = wiki.getPage(path);
        if (page == null) {
            return UrlRewriter.redirect(WikiController.class,
                    "createForm?path=" + path,
                    SEE_OTHER);
        } else {
            return templateEngine.render("page",
                    "page", page);
        }
    }
}
