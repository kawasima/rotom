package net.unit8.rotom.search;

import java.io.Serializable;

public class FoundPage implements Serializable {
    private final String urlPath;
    private final String name;
    private final String summary;
    private final float score;

    public FoundPage(String urlPath, String name, String summary, float score) {
        this.urlPath = urlPath;
        this.name = name;
        this.summary = summary;
        this.score = score;
    }

    public String getUrlPath() {
        return urlPath;
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

}
