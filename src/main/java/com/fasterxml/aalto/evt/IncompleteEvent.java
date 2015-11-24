package com.fasterxml.aalto.evt;

import java.io.Writer;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.ri.evt.BaseEventImpl;

import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.impl.LocationImpl;

/**
 * Bogus {@link javax.xml.stream.events.XMLEvent} returned when the next event
 * is not yet available, in case of non-blocking (async) parsing.
 */
public class IncompleteEvent extends BaseEventImpl
{
    private final static IncompleteEvent INSTANCE = new IncompleteEvent();

    protected IncompleteEvent() {
        super(LocationImpl.getEmptyLocation());
    }

    public static IncompleteEvent instance() { return INSTANCE; }
    
    @Override
    public int getEventType() {
        return AsyncXMLStreamReader.EVENT_INCOMPLETE;
    }

    @Override
    public void writeAsEncodedUnicode(Writer w) throws XMLStreamException {
        ; // nothing to write
    }

    @Override
    public void writeUsing(XMLStreamWriter2 w) throws XMLStreamException {
        ; // nothing to write
    }

    @Override
    public boolean equals(Object o) {
        return (o == this);
    }

    @Override
    public int hashCode() {
        return 42;
    }
}
