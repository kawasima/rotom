package net.unit8.rotom.search;

import java.io.Serializable;

public class FoundPage implements Serializable {
    private final String path;
    private final String name;
    private final String summary;
    private final float score;

    public FoundPage(String path, String name, String summary, float score) {
        this.path = path;
        this.name = name;
        this.summary = summary;
        this.score = score;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getSummary() {
        return summary;
    }

    public double getScore() {
        return score;
    }

    public String getUrlPath() {
        if (path.startsWith("/")) {
            return path.substring(1);
        } else {
            return path;
        }
    }
}
