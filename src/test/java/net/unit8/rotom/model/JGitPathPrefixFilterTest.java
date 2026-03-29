package net.unit8.rotom.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JGitPathPrefixFilterTest {

    @Test
    void createWithValidPath() {
        assertNotNull(JGitPathPrefixFilter.create("home"));
    }

    @Test
    void createWithEmptyPathThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> JGitPathPrefixFilter.create(""));
    }

    @Test
    void shouldBeRecursiveForPathWithSlash() {
        assertTrue(JGitPathPrefixFilter.create("docs/page").shouldBeRecursive());
    }

    @Test
    void shouldNotBeRecursiveForSimpleName() {
        assertFalse(JGitPathPrefixFilter.create("page").shouldBeRecursive());
    }

    @Test
    void cloneReturnsSameInstance() {
        JGitPathPrefixFilter filter = JGitPathPrefixFilter.create("test");
        assertSame(filter, filter.clone());
    }
}
