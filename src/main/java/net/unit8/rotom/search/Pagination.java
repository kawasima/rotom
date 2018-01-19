package net.unit8.rotom.search;

import java.util.List;

public class Pagination<T> {
    private long totalHits;
    private int offset;
    private int limit;
    private List<T> results;

    public Pagination(List<T> results, long totalHits, int offset, int limit) {
        this.results = results;
        this.offset = offset;
        this.limit = limit;
        this.totalHits = totalHits;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public List<T> getResults() {
        return results;
    }
}
