package net.unit8.rotom.model;

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;

public class BlobEntry {
    private String path;
    private String name;
    private String dir;
    private ObjectId sha;
    private byte[] data;
    private Supplier<byte[]> dataSupplier;
    private FileMode mode;

    public BlobEntry(String path, ObjectId sha, Supplier<byte[]> dataSupplier) {
        this(path, sha, dataSupplier, FileMode.REGULAR_FILE);
    }

    public BlobEntry(String path, ObjectId sha, Supplier<byte[]> dataSupplier, FileMode mode) {
        this.path = path;
        this.sha = sha;
        this.dataSupplier = dataSupplier;
        this.mode = mode;
        this.name = path.replaceFirst("^.*/", "");
        this.dir  = path.contains("/") ? path.replaceFirst("/[^/]*$", "") : "";
    }

    public String getPath() {
        return path;
    }

    public int getSize() {
        return (data == null) ? 0 : data.length;
    }

    public byte[] getData() {
        if (data == null) {
            data = dataSupplier == null ? new byte[0] : dataSupplier.get();
        }
        return data;
    }

    public String getName() {
        return name;
    }

    public String getDir() {
        return dir;
    }

    public ObjectId getSHA() {
        return sha;
    }

    public FileMode getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return sha + " " + path;
    }
}
