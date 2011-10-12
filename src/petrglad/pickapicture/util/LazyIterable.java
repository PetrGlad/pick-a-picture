package petrglad.pickapicture.util;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;

/**
 * Fetches data from source iterator at most once.
 */
public class LazyIterable<T> implements Iterable<T> {

    final Iterator<T> source;
    final List<T> data = Lists.newLinkedList();

    public LazyIterable(Iterator<T> source) {
        this.source = source;
    }

    @Override
    public Iterator<T> iterator() {
        return new AbstractIterator<T>() {
            int i = 0;

            @Override
            protected T computeNext() {
                if (data.size() > i)
                    return data.get(i++);
                else {
                    if (source.hasNext()) {
                        data.add(source.next());
                        assert data.size() > i;
                        return data.get(i++);
                    }
                    return endOfData();
                }
            }
        };
    }
}
