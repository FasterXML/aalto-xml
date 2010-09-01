package com.fasterxml.aalto.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Simple implementation of "null iterator", iterator that has nothing to
 * iterate over.
 */
public final class EmptyIterator<T>
    implements Iterator<T>
{
    final static EmptyIterator<Object> sInstance = new EmptyIterator<Object>();

    private EmptyIterator() { }

    /**
     * Since the actual type has no effect (as this iterator
     * never returns any value objects), we can just cast away
     * here: bit unclean, but safe.
     */
    @SuppressWarnings("unchecked")
	public static <T> EmptyIterator<T> getInstance() {
        return (EmptyIterator<T>) sInstance;
    }

    public boolean hasNext() { return false; }

    public T next() {
        throw new NoSuchElementException();
    }

    public void remove() {
        // could as well throw IllegalOperationException...
        throw new IllegalStateException();
    }
}
