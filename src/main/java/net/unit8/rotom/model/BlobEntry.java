package net.unit8.rotom.model;

import org.eclipse.jgit.lib.ObjectId;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class BlobEntry {
    private String path;
    private ObjectId sha;
    private byte[] data;

    public BlobEntry(String path, ObjectId sha, byte[] data) {
        this.path = path;
        this.sha = sha;
        this.data = data;
    }

    public String getPath() {
        return path;
    }

    public int getSize() {
        return (data == null) ? 0 : data.length;
    }

    public byte[] getData() {
        return data;
    }

    public String getName() {
        return Paths.get(path).getFileName().toString();
    }

    public String getDir() {
        return Optional.ofNullable(Paths.get(path).getParent())
                .map(Path::toString)
                .orElse("");
    }

    @Override
    public String toString() {
        return sha + " " + path;
    }
}
