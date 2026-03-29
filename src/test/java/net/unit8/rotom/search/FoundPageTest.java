package net.unit8.rotom.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FoundPageTest {

    @Test
    void constructorAndGetters() {
        FoundPage page = new FoundPage("docs", "guide", "<em>summary</em>", 0.95f);
        assertEquals("docs", page.getPath());
        assertEquals("guide", page.getName());
        assertEquals("<em>summary</em>", page.getSummary());
        assertEquals(0.95, page.getScore(), 0.01);
    }

    @Test
    void urlPathCombinesPathAndName() {
        FoundPage page = new FoundPage("docs", "guide", "", 1.0f);
        assertEquals("docs/guide", page.getUrlPath());
    }
}
