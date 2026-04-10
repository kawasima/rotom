package net.unit8.rotom;

import enkan.security.bouncr.UserPermissionPrincipal;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WikiControllerTest {

    @Nested
    class SanitizePathTest {

        @Test
        void nullReturnsEmpty() {
            assertEquals("", WikiController.sanitizePath(null));
        }

        @Test
        void emptyReturnsEmpty() {
            assertEquals("", WikiController.sanitizePath(""));
        }

        @Test
        void normalPathPassesThrough() {
            assertEquals("docs/guide", WikiController.sanitizePath("docs/guide"));
        }

        @Test
        void stripsLeadingSlashes() {
            assertEquals("page", WikiController.sanitizePath("/page"));
            assertEquals("page", WikiController.sanitizePath("///page"));
        }

        @Test
        void normalizesBackslashes() {
            assertEquals("a/b/c", WikiController.sanitizePath("a\\b\\c"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "../etc/passwd",
                "foo/../bar",
                "foo/bar/..",
                "..",
                "..\\etc\\passwd",
                "foo\\..\\bar"
        })
        void rejectsPathTraversal(String path) {
            assertThrows(IllegalArgumentException.class,
                    () -> WikiController.sanitizePath(path));
        }

        @Test
        void allowsDotsInPageName() {
            assertEquals("page.md", WikiController.sanitizePath("page.md"));
            assertEquals("v1.2.3", WikiController.sanitizePath("v1.2.3"));
        }

        @Test
        void allowsSingleDotInPath() {
            assertEquals("./page", WikiController.sanitizePath("./page"));
        }

        @Test
        void handlesDeepNestedPath() {
            assertEquals("a/b/c/d/e", WikiController.sanitizePath("a/b/c/d/e"));
        }

        @Test
        void handlesSpacesInPath() {
            assertEquals("my page", WikiController.sanitizePath("my page"));
        }

        @Test
        void handlesUnicodeInPath() {
            assertEquals("日本語ページ", WikiController.sanitizePath("日本語ページ"));
        }
    }

    @Nested
    class ToPersonIdentTest {

        @Test
        void nullPrincipalReturnsAnonymous() {
            PersonIdent ident = WikiController.toPersonIdent(null);
            assertEquals("anonymous", ident.getName());
            assertEquals("anonymous@example.com", ident.getEmailAddress());
        }

        @Test
        void principalWithEmail() {
            Map<String, Object> profiles = new HashMap<>();
            profiles.put("email", "user@example.com");
            var principal = new UserPermissionPrincipal(1L, "testuser", profiles, Set.of());
            PersonIdent ident = WikiController.toPersonIdent(principal);
            assertEquals("testuser", ident.getName());
            assertEquals("user@example.com", ident.getEmailAddress());
        }

        @Test
        void principalWithNullEmail() {
            Map<String, Object> profiles = new HashMap<>();
            var principal = new UserPermissionPrincipal(1L, "testuser", profiles, Set.of());
            PersonIdent ident = WikiController.toPersonIdent(principal);
            assertEquals("testuser", ident.getName());
            assertEquals("", ident.getEmailAddress());
        }
    }
}
