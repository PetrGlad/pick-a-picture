package petrglad.pickapicture.util;

import java.util.Iterator;

import com.google.common.base.Supplier;

/**
 * Evals source iterator to on-demand. The class is not thread-safe.
 *
 * @param <E>
 */
public class LazyIterator<E> implements Iterator<E> {

    protected final Supplier<Iterator<E>> supplier;
    protected Iterator<E> source;

    public LazyIterator(Supplier<Iterator<E>> supplier) {
        this.supplier = supplier;
    }

    protected Iterator<E> fetch() {
        if (source == null)
            source = supplier.get();
        return source;
    }

    @Override
    public boolean hasNext() {
        return fetch().hasNext();
    }

    @Override
    public E next() {
        return fetch().next();
    }

    @Override
    public void remove() {
        fetch().remove();
    }
}
