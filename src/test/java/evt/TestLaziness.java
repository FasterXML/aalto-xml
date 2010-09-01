package evt;

import java.io.*;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;

import org.codehaus.stax2.XMLInputFactory2;

import com.fasterxml.aalto.UncheckedStreamException;
import com.fasterxml.aalto.WFCException;

/**
 * Set of unit tests that verify that Aalto implementation of
 * {@link XMLEventReader} does obey additional constraints Aalto
 * guarantees. Specifically:
 *<ul>
 * <li>Event readers never read things in lazy manner: even if lazy parsing
 *   is enabled. (this restriction is added since lazy parsing does not
 *   significantly benefit Event API since there's no way to skip events,
 *   but it creates class of non-checked exceptions used to wrap real
 *   stream exceptions)
 *  </li>
 *</ul>
 */

public class TestLaziness
    extends base.BaseTestCase
{
    /**
     * This test verifies that lazy parsing is not enabled, by making
     * use of a specific side effect of lazy parsing. Specifically, parts
     * of Stax API do not expose XMLStreamException: such as getText()
     * method of the stream reader. Thus we can infer whether lazy mode
     * is enabled or not by whether we get a regular XMLStreamException
     * when trying to access the event, or not.
     *<p>
     * Note: whether this test reliably detecs "laziness" may depend
     * on the event reader implementation.
     */
    public void testEventReaderNonLaziness()
        throws XMLStreamException
    {
        final String XML =
            "<root>Some text and &amp; ...\n\n &error;</root>"
            ;
        XMLEventReader er = getReader(XML);

        // First things first: what does it say about mode?
        assertEquals("Event reader should have P_LAZY_PARSING == false", Boolean.FALSE, er.getProperty(XMLInputFactory2.P_LAZY_PARSING));

        XMLEvent evt = er.nextEvent(); // start document
        assertTrue(evt.isStartDocument());
        assertTrue(er.nextEvent().isStartElement());

        // Ok, and now...
        try {
            evt = er.nextEvent();
            // should NOT get this far...
            fail("Expected an exception for invalid content: something not working with event reader");
        } catch (WFCException wex) {
            // This is correct... parsing exc for entity, hopefully
            //System.err.println("GOOD: got "+wex.getClass()+": "+wex);
        } catch (UncheckedStreamException ue) {
            // Not good; lazy exception
            fail("Should not get a lazy exception via (default) event reader, but did get one: "+ue);
        } catch (Throwable t) {
            fail("Unexpected excpetion (class "+t.getClass()+") caught: "+t);
        }
    }


    /*
    //////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////
    */

    private XMLEventReader getReader(String contents)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        return f.createXMLEventReader(new StringReader(contents));
    }
}
