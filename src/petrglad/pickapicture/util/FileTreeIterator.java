package petrglad.pickapicture.util;

import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.singletonIterator;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.google.common.collect.Iterators;

/**
 * Iterator that lists all nested files and directories in given root.
 */
public class FileTreeIterator implements Iterator<File> {

    final Queue<Iterator<File>> dirQueue = new LinkedList<Iterator<File>>();
    final Iterator<File> i = concat(new QueueIterator<Iterator<File>>(dirQueue));

    public FileTreeIterator(File root) {
        dirQueue.add(singletonIterator(root));
    }

    @Override
    public boolean hasNext() {
        return i.hasNext();
    }

    @Override
    public File next() {
        File f = i.next();
        handleDir(f);
        return f;
    }

    public void handleDir(File f) {
        if (f.isDirectory()) {
            File[] list = f.listFiles();
            if (list != null)
                dirQueue.offer(Iterators.forArray(list));
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
