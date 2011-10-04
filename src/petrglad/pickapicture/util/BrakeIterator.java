package petrglad.pickapicture.util;

import java.util.Iterator;

import com.google.common.base.Supplier;

public class BrakeIterator<E> implements Iterator<E> {

    final Iterator<E> wrapped;
    final Supplier<Boolean> allowContinue;

    public BrakeIterator(Iterator<E> wrapped, Supplier<Boolean> allowContinue) {
        this.wrapped = wrapped;
        this.allowContinue = allowContinue;
    }

    @Override
    public boolean hasNext() {
        if (!allowContinue.get())
            throw new StopIterationException();
        return wrapped.hasNext();
    }

    @Override
    public E next() {
        if (!allowContinue.get())
            throw new StopIterationException();
        return wrapped.next();
    }

    @Override
    public void remove() {
        if (!allowContinue.get())
            throw new StopIterationException();
        wrapped.remove();
    }
}
