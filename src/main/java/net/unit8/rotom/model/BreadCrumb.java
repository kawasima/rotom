package net.unit8.rotom.model;

import java.io.Serializable;

public class BreadCrumb implements Serializable {
    private final String path;
    private final String title;

    public BreadCrumb(String title, String path) {
        this.path = path;
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }
}
