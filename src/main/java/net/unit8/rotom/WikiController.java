package net.unit8.rotom;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.data.HttpResponse;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.CodecUtils;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.rotom.model.Commit;
import net.unit8.rotom.model.MarkupType;
import net.unit8.rotom.model.Page;
import net.unit8.rotom.model.Wiki;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.*;
import static enkan.util.ThreadingUtils.some;

public class WikiController {
    @Inject
    private BeansConverter beansConverter;

    @Inject
    private Wiki wiki;

    @Inject
    private TemplateEngine templateEngine;

    public HttpResponse pages(Parameters params) {
        String path = params.get("path");
        List<Page> pages = wiki.getPages(Objects.toString(path, ""));
        return templateEngine.render("pages",
                "path", path,
                "pages", pages,
                "wiki", wiki);
    }

    public HttpResponse index() {
        return UrlRewriter.redirect(WikiController.class,
                "showPageOrFile?path=" + wiki.getIndexPage(),
                SEE_OTHER);
    }

    public HttpResponse createForm(Parameters params) {
        String path = params.get("path");
        Page page = new Page(path);
        return templateEngine.render("create",
                "isCreatePage", true,
                "isEditPage", false,
                "page", page,
                "markupTypes", MarkupType.values(),
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
        Page page = wiki.getPage(Wiki.fullpath(path, name));
        return UrlRewriter.redirect(WikiController.class,
                "showPageOrFile?path=" + page.getUrlPath(),
                SEE_OTHER);
    }

    public HttpResponse edit(Parameters params) {
        String path = params.get("path");
        Page page = wiki.getPage(path);
        if (page == null) {
            return UrlRewriter.redirect(WikiController.class,
                    "createForm?path=" + CodecUtils.urlEncode(path),
                    SEE_OTHER);
        } else {
            return templateEngine.render("edit",
                    "isCreatePage", false,
                    "isEditPage", true,
                    "markupTypes", MarkupType.values(),
                    "format", page.getFormat(),
                    "page", page);
        }
    }

    public HttpResponse update(Parameters params, UserPermissionPrincipal principal) {
        String name = params.get("page");
        String path = params.get("path");
        String format = params.get("format");
        PersonIdent committer;
        if (principal != null) {
            committer = new PersonIdent(principal.getName(), Objects.toString(principal.getProfiles().get("email")));
        } else {
            committer = new PersonIdent("anonymous", "anonymous@example.com");
        }
        Page page = wiki.getPage(Wiki.fullpath(path, name));
        wiki.updatePage(page, null, null, params.get("content").getBytes(),
                new Commit(committer.getName(), committer.getEmailAddress(), params.get("message")));
        return UrlRewriter.redirect(WikiController.class,
                "showPageOrFile?path=" + page.getUrlPath(),
                SEE_OTHER);
    }

    public HttpResponse history(Parameters params) {
        String path = params.get("path");
        Page page = wiki.getPage(path);
        if (page == null) {
            return UrlRewriter.redirect(WikiController.class,
                    "showPageOrFile?path=/",
                    SEE_OTHER);
        } else {
            return templateEngine.render("history",
                    "page", page);
        }
    }

    public HttpResponse showPageOrFile(Parameters params) {
        String path = params.get("path");
        ObjectId sha1 = some(params.get("sha1"),
                ObjectId::fromString)
                .orElse(null);

        Page page = wiki.getPage(path, sha1);
        if (page == null) {
            return UrlRewriter.redirect(WikiController.class,
                    "createForm?path=" + CodecUtils.urlEncode(path),
                    SEE_OTHER);
        } else {
            return templateEngine.render("page",
                    "page", page);
        }
    }

    public HttpResponse compare(Parameters params) {
        LinkedList<String> versions = Optional.ofNullable(params.getList("versions[]"))
                .map(vs -> vs
                        .stream()
                        .map(v -> Objects.toString(v))
                        .collect(Collectors.toList()))
                .map(LinkedList::new)
                .orElseGet(LinkedList::new);
        if (versions.size() < 2) {
            return UrlRewriter.redirect(WikiController.class,
                    "history?path=" + params.get("path"),
                    SEE_OTHER);
        } else {
            return UrlRewriter.redirect(WikiController.class,
                    "doCompare?hash1=" + versions.getFirst() +
                            "&hash2=" + versions.getLast() +
                            "&path=" + params.get("path"),
                    SEE_OTHER);
        }
    }

    public HttpResponse doCompare(Parameters params) {
        Page page = wiki.getPage(params.get("path"));
        return templateEngine.render("compare");
    }
}
