package net.unit8.rotom;


import enkan.Endpoint;
import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.security.AuthBackend;
import enkan.util.HttpResponseUtils;
import net.unit8.rotom.middleware.backend.AnonymousBackend;


public class RotomConfiguration extends SystemComponent<RotomConfiguration> {
    private String basePath = "";
    private AuthBackend<?, ?> authBackend = new AnonymousBackend();
    private Endpoint<HttpRequest, HttpResponse> unauthEndpoint =
            (Endpoint<HttpRequest, HttpResponse>) req -> {
                    String uri = req.getUri();
                    // Only allow relative paths to prevent open redirect
                    if (uri != null && !uri.startsWith("/")) {
                        uri = "/";
                    }
                    return HttpResponseUtils.redirect("/my/signIn?url=" + uri,
                            HttpResponseUtils.RedirectStatusCode.TEMPORARY_REDIRECT);
            };

    @Override
    protected ComponentLifecycle<RotomConfiguration> lifecycle() {
        return new ComponentLifecycle<RotomConfiguration>() {
            @Override
            public void start(RotomConfiguration config) {
            }

            @Override
            public void stop(RotomConfiguration config) {

            }
        };
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public AuthBackend<?, ?> getAuthBackend() {
        return authBackend;
    }

    public void setAuthBackend(AuthBackend<?, ?> authBackend) {
        this.authBackend = authBackend;
    }

    public Endpoint<HttpRequest, HttpResponse> getUnauthEndpoint() {
        return unauthEndpoint;
    }

    public void setUnauthEndpoint(Endpoint<HttpRequest, HttpResponse> unauthEndpoint) {
        this.unauthEndpoint = unauthEndpoint;
    }
}
