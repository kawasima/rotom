package net.unit8.rotom;

import enkan.Application;
import enkan.application.WebApplication;
import enkan.config.ApplicationFactory;
import enkan.endpoint.ResourceEndpoint;
import enkan.middleware.*;
import enkan.security.bouncr.BouncrBackend;
import enkan.system.inject.ComponentInjector;
import kotowari.middleware.*;
import kotowari.routing.Routes;
import net.unit8.rotom.model.Wiki;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.Predicates.NONE;

public class RotomApplicationFactory implements ApplicationFactory {
    @Inject
    private RotomConfiguration configuration;

    @Override
    public Application create(ComponentInjector injector) {
        WebApplication app = new WebApplication();

        injector.inject(this);

        Routes routes = Routes.define(root -> {
            root.scope(configuration.getBasePath(),  r -> {
                r.get("/").to(WikiController.class, "index");
                r.get("/pages/*path").to(WikiController.class, "pages");
                r.get("/files").to(WikiController.class, "files");
                r.get("latest_changes/*path").to(WikiController.class, "latestChanges");
                r.get("/create/*path").to(WikiController.class, "createForm");
                r.post("/create").to(WikiController.class, "create");
                r.get("/edit/*path").to(WikiController.class, "edit");
                r.post("/edit/*dummy").to(WikiController.class, "update");
                r.get("/history/*path").to(WikiController.class, "history");
                r.post("/compare/*path").to(WikiController.class, "compare");
                r.get("/compare/*path/:hash1..:hash2").to(WikiController.class, "doCompare");
                r.get("/*path/:sha1").requires("sha1", "[a-f0-9]{40}")
                        .to(WikiController.class, "showPageOrFile");
                r.get("/*path").to(WikiController.class, "showPageOrFile");
            });
        }).compile();

        app.use(new DefaultCharsetMiddleware());
        app.use(NONE, new ServiceUnavailableMiddleware<>(new ResourceEndpoint("/public/html/503.html")));
        app.use(new ContentTypeMiddleware());
        app.use(new ParamsMiddleware());
        app.use(new MultipartParamsMiddleware());
        app.use(new MethodOverrideMiddleware());
        app.use(new NormalizationMiddleware());
        app.use(new NestedParamsMiddleware());
        app.use(new CookiesMiddleware());

        app.use(builder(new ContentNegotiationMiddleware())
                .set(ContentNegotiationMiddleware::setAllowedLanguages,
                        new HashSet<>(Arrays.asList("en", "ja")))
                .build());
        app.use(builder(new CorsMiddleware())
                .set(CorsMiddleware::setHeaders,
                        new HashSet<>(Arrays.asList("X-Bouncr-Credential", "Content-Type")))
                .build());
        BouncrBackend bouncrBackend = new BouncrBackend();
        injector.inject(bouncrBackend);
        app.use(new AuthenticationMiddleware<>(Arrays.asList(bouncrBackend)));
        app.use(new ResourceMiddleware());
        app.use(new RenderTemplateMiddleware());
        app.use(new RoutingMiddleware(routes));
        app.use(new FormMiddleware());
        app.use(new SerDesMiddleware());
        app.use(new ValidateBodyMiddleware<>());
        app.use(new ControllerInvokerMiddleware<>(injector));

        return app;

    }
}
