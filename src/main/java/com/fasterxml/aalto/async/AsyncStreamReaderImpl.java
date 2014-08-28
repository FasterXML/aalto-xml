package com.fasterxml.aalto.async;

import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncInputFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.StreamReaderImpl;

import java.nio.ByteBuffer;

/**
 * Implementation of {@link AsyncXMLStreamReader}.
 */
public class AsyncStreamReaderImpl extends StreamReaderImpl
    implements AsyncXMLStreamReader, AsyncInputFeeder
{
    protected final AsyncUtfScanner _asyncScanner;
    
    public AsyncStreamReaderImpl(AsyncUtfScanner scanner)
    {
        super(scanner);
        _asyncScanner = scanner;
        _currToken = EVENT_INCOMPLETE;
    }

    /*
    /**********************************************************************
    /* AsyncXMLStreamReader implementation
    /**********************************************************************
     */
    
    @Override
    public AsyncInputFeeder getInputFeeder() {
        return this;
    }

    /*
    /**********************************************************************
    /* AsyncInputFeeder implementation
    /**********************************************************************
     */
    
    @Override
    public boolean needMoreInput() {
        return _asyncScanner.needMoreInput();
    }

    @Override
    public void endOfInput() {
        _asyncScanner.endOfInput();
    }

    @Override
    public void feedInput(byte[] data, int offset, int len) throws XMLStreamException {
        _asyncScanner.feedInput(data, offset, len);
    }

    @Override
    public void feedInput(ByteBuffer data) throws XMLStreamException {
        _asyncScanner.feedInput(data);
    }

    /*
    /**********************************************************************
    /* Overrides
    /**********************************************************************
     */

    @Override
    protected void _reportNonTextEvent(int type) throws XMLStreamException
    {
        // for Async parser
        if (type == EVENT_INCOMPLETE) {
            throwWfe("Can not use text-aggregating methods with non-blocking parser, as they (may) require blocking");
        }
        super._reportNonTextEvent(type);
    }
}
