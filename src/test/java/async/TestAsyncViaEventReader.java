package async;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;

import com.fasterxml.aalto.*;
import com.fasterxml.aalto.stax.InputFactoryImpl;

/**
 * Set of tests to ensure that it is possible to use Stax {@link XMLEventReader} with
 * async parser.
 */
public class TestAsyncViaEventReader extends AsyncTestBase
{
    public void testSimple() throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = f.createAsyncFor("<root>a</r".getBytes("UTF-8"));

        assertTokenType(START_DOCUMENT, sr.next());
        
        XMLEventReader er = f.createXMLEventReader(sr);

        XMLEvent evt = er.nextEvent();
        assertTokenType(START_DOCUMENT, evt.getEventType());

        evt = er.nextEvent();
        assertTokenType(START_ELEMENT, evt.getEventType());
        assertEquals("root", sr.getLocalName());

        evt = er.nextEvent();
        assertTokenType(CHARACTERS, evt.getEventType());
        assertEquals("a", sr.getText());
        
        // then need more input
        evt = er.nextEvent();
        assertTokenType(AsyncXMLStreamReader.EVENT_INCOMPLETE, evt.getEventType());

        byte[] b = "oot>".getBytes("UTF-8");
        sr.getInputFeeder().feedInput(b, 0, b.length);

        evt = er.nextEvent();
        assertTokenType(END_ELEMENT, evt.getEventType());
        assertEquals("root", sr.getLocalName());

        evt = er.nextEvent();
        assertTokenType(AsyncXMLStreamReader.EVENT_INCOMPLETE, evt.getEventType());

        sr.getInputFeeder().endOfInput();
        evt = er.nextEvent();
        assertTokenType(END_DOCUMENT, evt.getEventType());
        
        assertFalse(er.hasNext());
        
        sr.close();
    }
}
