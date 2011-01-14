package com.fasterxml.aalto.async;

import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncInputFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.StreamReaderImpl;

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
}
