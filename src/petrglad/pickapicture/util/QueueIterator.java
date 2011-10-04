package petrglad.pickapicture.util;

import java.util.Iterator;
import java.util.Queue;

/**
 * Consuming iterator over queue.
 */
public class QueueIterator<E> implements Iterator<E> {

    final Queue<E> queue;

    public QueueIterator(Queue<E> queue) {
        this.queue = queue;
    }

    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public E next() {
        return queue.remove();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
