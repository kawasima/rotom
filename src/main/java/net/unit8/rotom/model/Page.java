package net.unit8.rotom.model;

import net.unit8.rotom.model.filter.Render;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Page {
    private BlobEntry blob;
    private List<Filter> filterChain;

    public Page(BlobEntry blob) {
        this.blob = blob;
        filterChain = new ArrayList<>();
        filterChain.add(new Render());
    }

    public String getFileName() {
        return Optional.ofNullable(blob)
                .map(BlobEntry::getName)
                .orElse(null);
    }

    public String getName() {
        return Optional.ofNullable(getFileName())
                .map(name -> name
                        .replaceFirst("\\.\\w+$" , "")
                        .replace('-', ' '))
                .orElse(null);
    }

    public String toString() {
        return new String(blob.getData(), StandardCharsets.UTF_8);
    }

    public String toFormattedData() {
        String s = new String(blob.getData(), StandardCharsets.UTF_8);
        for (Filter filter : filterChain) {
            s = filter.extract(s, this);
        }

        for (Filter filter : filterChain) {
            s = filter.process(s, this);
        }

        Matcher m = Pattern.compile("<p></p>").matcher(s);
        return m.replaceAll("");
    }

    public String getFormat() {
        return Arrays.stream(MarkupType.values())
                .filter(mt -> mt.match(getFileName()))
                .map(mt -> mt.getName())
                .findAny()
                .orElse(null);
    }

}
