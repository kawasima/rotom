package net.unit8.rotom.middleware.backend;

import enkan.security.bouncr.UserPermissionPrincipal;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;

class AnonymousBackendTest {

    private final AnonymousBackend backend = new AnonymousBackend();

    @Test
    void parseReturnsAnonymous() {
        assertEquals("anonymous", backend.parse(null));
    }

    @Test
    void authenticateReturnsPrincipalWithAllPermissions() {
        Principal principal = backend.authenticate(null, "anonymous");
        assertInstanceOf(UserPermissionPrincipal.class, principal);

        UserPermissionPrincipal userPrincipal = (UserPermissionPrincipal) principal;
        assertEquals("anonymous", userPrincipal.getName());
        assertTrue(userPrincipal.getPermissions().contains("page:read"));
        assertTrue(userPrincipal.getPermissions().contains("page:create"));
        assertTrue(userPrincipal.getPermissions().contains("page:edit"));
        assertTrue(userPrincipal.getPermissions().contains("page:delete"));
    }
}
