package com.fasterxml.aalto;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;

/**
 * Extension of XMLInputFactory2
 * to provide factory methods for constructing non-blocking (asynchronous)
 * parsers (of type {@link AsyncXMLStreamReader}.
 */
public abstract class AsyncXMLInputFactory
    extends XMLInputFactory2
{
    /**
     * Method for constructing a non-blocking {@link XMLStreamReader2} instance
     * without any input data.
     * 
     * @return Non-blocking stream reader without any input
     */
    public abstract AsyncXMLStreamReader createAsyncXMLStreamReader();

    /**
     * Method for constructing a non-blocking {@link XMLStreamReader2} instance
     * with specified initial input data.
     * Input data will not be parsed at this point but merely fed to be parsed as
     * needed with Stax API calls.
     *<p>
     * Note that caller needs to ensure that given input buffer is available for
     * parser to use until it has been fully consumed; parser is not required to
     * make a copy of it, in order to minimize number of copies made. Caller can
     * choose to just make a copy to pass. After input has been parsed buffer can
     * be reused.
     * Implementations may return an optimized implementation for handling byte arrays.
     * 
     * @return Non-blocking stream reader initialized with given input
     */
    public abstract AsyncXMLStreamReader createAsyncXMLStreamReader(byte[] input);

    /**
     * Method for constructing a non-blocking {@link XMLStreamReader2} instance
     * with specified initial input data.
     * Input data will not be parsed at this point but merely fed to be parsed as
     * needed with Stax API calls.
     *<p>
     * Note that caller needs to ensure that given input buffer is available for
     * parser to use until it has been fully consumed; parser is not required to
     * make a copy of it, in order to minimize number of copies made. Caller can
     * choose to just make a copy to pass. After input has been parsed buffer can
     * be reused.
     * Implementations may return an optimized implementation for handling byte arrays.
     * 
     * @return Non-blocking stream reader initialized with given input
     */
    public abstract AsyncXMLStreamReader createAsyncXMLStreamReader(byte[] input, int offset, int length);

    /**
     * Method for constructing a non-blocking {@link XMLStreamReader2} instance
     * with specified initial input data.
     * Input data will not be parsed at this point but merely fed to be parsed as
     * needed with Stax API calls.
     *<p>
     * Note that caller needs to ensure that given input buffer is available for
     * parser to use until it has been fully consumed; parser is not required to
     * make a copy of it, in order to minimize number of copies made. Caller can
     * choose to just make a copy to pass. After input has been parsed buffer can
     * be reused.
     * Implementations may return an optimized implementation for handling {@link ByteBuffer}s.
     *
     * @return Non-blocking stream reader initialized with given input
     */
    public abstract AsyncXMLStreamReader createAsyncXMLStreamReader(ByteBuffer input) throws XMLStreamException;
}
