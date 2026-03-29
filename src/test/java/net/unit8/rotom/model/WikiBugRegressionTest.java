package net.unit8.rotom.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for bugs found during code review.
 * Each test verifies a specific bug that previously existed.
 */
class WikiBugRegressionTest {

    @Test
    void fullpathWithNullNameThrowsInsteadOfProducingPathNull() {
        // BUG: Wiki.fullpath("docs", null) previously returned "docs/null" (string)
        assertThrows(IllegalArgumentException.class,
                () -> Wiki.fullpath("docs", null));
    }

    @Test
    void fullpathWithNullNameAndNullDirThrowsInsteadOfReturningNull() {
        // BUG: Wiki.fullpath(null, null) previously returned null silently
        assertThrows(IllegalArgumentException.class,
                () -> Wiki.fullpath(null, null));
    }
}
