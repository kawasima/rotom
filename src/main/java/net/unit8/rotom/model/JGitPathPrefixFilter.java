package net.unit8.rotom.model;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;

public class JGitPathPrefixFilter extends TreeFilter {
    public static JGitPathPrefixFilter create(String path) {
        if (path.length() == 0)
            throw new IllegalArgumentException(JGitText.get().emptyPathNotPermitted);
        return new JGitPathPrefixFilter(path);
    }

    final String pathStr;
    final byte[] pathRaw;
    final int lastSlashIndex;

    private JGitPathPrefixFilter(final String s) {
        pathStr = s;
        pathRaw = Constants.encode(pathStr);
        int idx = pathRaw.length-1;
        for (; idx>=0; idx--) {
            if (pathRaw[idx] == 0x2f) break;
        }
        lastSlashIndex = idx;
    }

    @Override
    public TreeFilter clone() {
        return this;
    }

    @Override
    public boolean include(TreeWalk walker) throws MissingObjectException, IncorrectObjectTypeException, IOException {
        byte[] currentRow = walker.getRawPath();
        int idx = walker.getPathLength()-1;
        for (; idx>=0; idx--) {
            if (currentRow[idx] == '.') {
                break;
            }
        }

        // Directory match
        if (idx < 0) {
            if (lastSlashIndex < 0 || walker.getPathLength() > lastSlashIndex) return false;
            for (int i=0; i<walker.getPathLength(); i++) {
                if (currentRow[i] != pathRaw[i])
                    return false;
            }
            return true;
        }

        // File name match
        for (int i=0; i<idx; i++) {
            if (currentRow[i] != pathRaw[i])
                return false;
        }
        return true;
    }

    @Override
    public boolean shouldBeRecursive() {
        for (final byte b : pathRaw)
            if (b == '/')
                return true;
        return false;
    }
}
