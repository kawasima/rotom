package net.unit8.rotom.model;

public interface Filter {
    String extract(String data, Page page);
    String process(String data, Page page);
}
