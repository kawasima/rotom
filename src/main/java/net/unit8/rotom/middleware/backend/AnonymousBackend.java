package net.unit8.rotom.middleware.backend;

import enkan.data.HttpRequest;
import enkan.security.AuthBackend;
import enkan.security.bouncr.UserPermissionPrincipal;

import java.security.Principal;
import java.util.*;

public class AnonymousBackend implements AuthBackend<HttpRequest, String> {
    private static final Map<String, Object> PROFILES = new HashMap<String, Object>(){{
        put("emailAddress", "anonymous@example.com");
    }};

    private static final Set<String> PERMISSIONS = new HashSet<>(Arrays.asList(
        "page:read", "page:create", "page:edit", "page:delete"
    ));

    @Override
    public String parse(HttpRequest request) {
        return "anonymous";
    }

    @Override
    public Principal authenticate(HttpRequest request, String data) {
        return new UserPermissionPrincipal(data, PROFILES, PERMISSIONS);
    }
}
