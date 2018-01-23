package net.unit8.rotom;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.security.AuthBackend;
import net.unit8.rotom.middleware.backend.AnonymousBackend;

public class RotomConfiguration extends SystemComponent {
    private String basePath = "";
    private AuthBackend<?, ?> authBackend = new AnonymousBackend();

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
}
