package com.fasterxml.aalto.async;

import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.in.XmlScanner;
import com.fasterxml.aalto.stax.StreamReaderImpl;

/**
 * Implementation of {@link AsyncXMLStreamReader}.
 */
public class AsyncStreamReaderImpl extends StreamReaderImpl
    implements AsyncXMLStreamReader
{
    public AsyncStreamReaderImpl(XmlScanner scanner) {
        super(scanner);
    }

}
