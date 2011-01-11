package com.fasterxml.aalto;

import org.codehaus.stax2.XMLStreamReader2;

/**
 * Extension of {@link javax.xml.stream.XMLStreamReader2} used by non-blocking ("async")
 * stream readers. The main difference is addition of a token ({@link #EVENT_INCOMPLETE})
 * to indicate that there is not yet enough content to parse to tokenize next event;
 * and method to access {@link AsyncInputFeeder} that is used to provide input data
 * in non-blocking manner.
 */
public interface AsyncXMLStreamReader
    extends XMLStreamReader2
{
    /**
     * As per javadocs of {@link javax.xml.stream.XMLStreamConstants},
     * event codes 0 through 256 (inclusive?) are reserved by the Stax
     * specs, so we'll use the next available code.
     */
    public final static int EVENT_INCOMPLETE = 257;

    /**
     * Method used to access {@link AsyncInputFeeder} which is used to
     * provide XML content to parse in non-blocking manner (see
     * {@link AsyncInputFeeder} for more details).
     * 
     * @return Input feeder to use for "pushing" content to parse.
     */
    public AsyncInputFeeder getInputFeeder();
}
