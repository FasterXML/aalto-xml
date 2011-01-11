package com.fasterxml.aalto;

import org.codehaus.stax2.XMLStreamReader2;

/**
 * Extension of {@link javax.xml.stream.XMLStreamReader2} used by non-blocking ("async")
 * stream readers.
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

}
