package com.fasterxml.aalto.impl;

import java.io.IOException;

/**
 * Simple wrapper for {@link IOException}s; needed when StAX does not expose
 * underlying I/O exceptions via its methods.
 */
@SuppressWarnings("serial")
public class IoStreamException
    extends StreamExceptionBase
{
    public IoStreamException(IOException ie) {
        super(ie);
    }

    public IoStreamException(String msg) {
        super(msg);
    }
}
