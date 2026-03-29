package net.unit8.rotom.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginationTest {

    @Test
    void constructorAndGetters() {
        Pagination<String> p = new Pagination<>(List.of("a", "b"), 100, 10, 20, true);
        assertEquals(List.of("a", "b"), p.getResults());
        assertEquals(100, p.getTotalHits());
        assertEquals(10, p.getOffset());
        assertEquals(20, p.getLimit());
        assertTrue(p.isExact());
    }

    @Test
    void inexactTotalHits() {
        Pagination<String> p = new Pagination<>(List.of(), 1000, 0, 10, false);
        assertFalse(p.isExact());
    }

    @Test
    void emptyResults() {
        Pagination<String> p = new Pagination<>(List.of(), 0, 0, 10, true);
        assertTrue(p.getResults().isEmpty());
        assertEquals(0, p.getTotalHits());
    }
}
