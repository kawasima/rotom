package net.unit8.rotom;

import enkan.collection.OptionMap;
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
import net.unit8.rotom.search.FoundPage;
import net.unit8.rotom.search.IndexManager;
import net.unit8.rotom.search.Pagination;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.*;
import static enkan.util.ThreadingUtils.*;

public class WikiController {
    @Inject
    private BeansConverter beansConverter;

    @Inject
    private Wiki wiki;

    @Inject
    private IndexManager indexManager;

    @Inject
    private TemplateEngine templateEngine;

    @RolesAllowed("page:read")
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

    @RolesAllowed("page:read")
    public HttpResponse search(Parameters params) {
        String query = Optional.ofNullable(params.get("q")).orElse("");
        Pagination<FoundPage> pagination = indexManager.search(query, 0, 10);
        return templateEngine.render("search",
                "query", query,
                "pagination", pagination);
    }

    @RolesAllowed("page:create")
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

    @RolesAllowed("page:create")
    public HttpResponse create(Parameters params, UserPermissionPrincipal principal) {
        String name = params.get("page");
        String dir = params.get("dir");
        String format = params.get("format");
        PersonIdent committer;
        if (principal != null) {
            committer = new PersonIdent(principal.getName(), Objects.toString(principal.getProfiles().get("email")));
        } else {
            committer = new PersonIdent("anonymous", "anonymous@example.com");
        }
        wiki.writePage(name, format, params.get("content").getBytes(), dir,
                new Commit(committer.getName(), committer.getEmailAddress(), params.get("message")));
        Page page = wiki.getPage(Wiki.fullpath(dir, name));
        indexManager.save(page);
        return UrlRewriter.redirect(WikiController.class,
                "showPageOrFile?path=" + page.getUrlPath(),
                SEE_OTHER);
    }

    @RolesAllowed("page:edit")
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

    @RolesAllowed("page:edit")
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
        page = wiki.getPage(Wiki.fullpath(path, name));
        indexManager.save(page);
        return UrlRewriter.redirect(WikiController.class,
                "showPageOrFile?path=" + page.getUrlPath(),
                SEE_OTHER);
    }

    @RolesAllowed("page:delete")
    public HttpResponse delete(Parameters params, UserPermissionPrincipal principal) {
        Page page = some(params.get("path"), path -> wiki.getPage(path)).orElse(null);
        if (page != null) {
            PersonIdent committer;
            if (principal != null) {
                committer = new PersonIdent(principal.getName(), Objects.toString(principal.getProfiles().get("email")));
            } else {
                committer = new PersonIdent("anonymous", "anonymous@example.com");
            }
            wiki.deletePage(page, new Commit(committer.getName(), committer.getEmailAddress(),
                    "Destroyed " + page.getName() + " (" + page.getFormat() +")"));
        }
        return UrlRewriter.redirect(WikiController.class,
                "showPageOrFile?path=",
                SEE_OTHER);
    }

    @RolesAllowed("page:read")
    public HttpResponse history(Parameters params) {
        String path = params.get("path");
        Page page = wiki.getPage(path);
        if (page == null) {
            return UrlRewriter.redirect(WikiController.class,
                    "showPageOrFile?path=/",
                    SEE_OTHER);
        } else {
            List<RevCommit> versions = wiki.getVersions(OptionMap.of("path", page.getPath()));
            return templateEngine.render("history",
                    "page", page,
                    "versions", versions);
        }
    }

    @RolesAllowed("page:read")
    public HttpResponse latestChanges(Parameters params) {
        List<RevCommit> versions = wiki.getVersions(OptionMap.of());
        return templateEngine.render("latestChanges",
                "versions", versions);
    }

    @RolesAllowed("page:read")
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

    @RolesAllowed("page:read")
    public HttpResponse compare(Parameters params) {
        LinkedList<String> versions = Optional.ofNullable(params.getList("versions"))
                .map(vs -> vs
                        .stream()
                        .map(Objects::toString)
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

    @RolesAllowed("page:read")
    public HttpResponse doCompare(Parameters params) {
        Page page = wiki.getPage(params.get("path"));
        String diffEntries = wiki.getDiff(page, params.get("hash1"), params.get("hash2"));

        return templateEngine.render("compare",
                "page", page,
                "diffEntries", diffEntries);
    }
}
