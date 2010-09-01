package com.fasterxml.aalto.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Simple read-only iterator that iterators over one specific item, passed
 * in as constructor argument.
 */
public final class SingletonIterator
    implements Iterator<String>
{
    private final String mValue;

    private boolean mDone = false;

    public SingletonIterator(String value) {
        mValue = value;
    }

    public boolean hasNext() {
        return !mDone;
    }

    public String next() {
        if (mDone) {
            throw new NoSuchElementException();
        }
        mDone = true;
        return mValue;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
