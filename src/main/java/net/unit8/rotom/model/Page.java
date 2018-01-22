package net.unit8.rotom.model;

import enkan.util.CodecUtils;
import net.unit8.rotom.model.filter.Render;
import org.eclipse.jgit.lib.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Page {
    private BlobEntry blob;
    private List<Filter> filterChain;

    public Page(String path) {
        this.blob = new BlobEntry(path.replaceFirst("/+$", ""),
                null, null, 0, null,
                path.endsWith("/") ? FileMode.TREE : FileMode.REGULAR_FILE);
    }

    public Page(String path, BlobEntry blob) {
        this.blob = blob;
        filterChain = new ArrayList<>();
        filterChain.add(new Render());
    }

    /**
     * Get a basename of file.
     *
     * /a/b/c.md --> c.md
     */
    public String getFileName() {
        return Optional.ofNullable(blob)
                .map(BlobEntry::getName)
                .orElse(null);
    }

    public String getDir() {
        return Optional.ofNullable(blob)
                .map(BlobEntry::getDir)
                .orElse(null);
    }

    /**
     * Get a decoded basename of file without extension.
     *
     * /a/b/h-e-l-l-o.md --> "h e l l o"
     *
     * @return name
     */
    public String getName() {
        return Optional.ofNullable(getFileName())
                .map(name -> name
                        .replaceFirst("\\.\\w+$" , "")
                        .replace('-', ' '))
                .orElse(null);
    }

    public String getUrlPath() {
        return Optional.of(blob.getPath())
                .map(p -> p.replaceFirst("\\.\\w+$", ""))
                .orElse(null);
    }

    /**
     * Get a dirname of a page.
     *
     * @return path
     */
    public String getPath() {
        return Optional.ofNullable(blob)
                .map(BlobEntry::getPath)
                .orElse(null);
    }

    public boolean isRegularFile() {
        return Optional.ofNullable(blob)
                .map(b -> b.getMode() == FileMode.REGULAR_FILE)
                .orElse(false);
    }

    public String toString() {
        return getName();
    }

    public String getTextData() {
        return new String(blob.getData(), StandardCharsets.UTF_8);
    }

    public String getFormattedData() {
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
                .map(MarkupType::getName)
                .findAny()
                .orElse(null);
    }

    public PersonIdent getCommitter() {
        return blob.getComitter();
    }

    public long getModifiedTime() {
        return blob.getCommitTime();
    }
}
