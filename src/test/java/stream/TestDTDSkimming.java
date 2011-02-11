package stream;

import java.io.*;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

public class TestDTDSkimming extends base.BaseTestCase
{
    public void testSimple() throws Exception
    {
        _doTestSimple(null, false);
        _doTestSimple(null, true);
        _doTestSimple("UTF-8", false);
        _doTestSimple("UTF-8", true);
    }

    public void testInvalidDup() throws Exception
    {
        _doTestInvalidDup(null, false);
        _doTestInvalidDup(null, true);
        _doTestInvalidDup("UTF-8", false);
        _doTestInvalidDup("UTF-8", true);
    }
    
    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _doTestSimple(String encoding, boolean skip)
        throws IOException, XMLStreamException
    {
        final String INTERNAL_SUBSET = "<!--test-->";
        
        String DOC = "<?xml version='1.0'?><!DOCTYPE root ["+INTERNAL_SUBSET+"]> <root />";
        XMLStreamReader2 sr = createReader(DOC, encoding);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        int t = sr.next();
        assertTokenType(DTD, t);
        if (!skip) {
            assertEquals("root", sr.getPrefixedName());
            assertEquals(INTERNAL_SUBSET, sr.getText());
        }
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    private void _doTestInvalidDup(String encoding, boolean skip)
        throws IOException, XMLStreamException
    {
        String DOC = "<?xml version='1.0'?><!DOCTYPE root>  <!DOCTYPE root>";
        XMLStreamReader2 sr = createReader(DOC, encoding);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        int t = sr.next();
        assertTokenType(DTD, t);
        if (!skip) {
            assertEquals("root", sr.getPrefixedName());
            sr.getText(); // just to force its parsing
        }
        // But second one should fail
        try {
            t = sr.next(); // to get second DTD
            fail("Should fail on invalid DOCTYPE declaration: instead got "+t);
        } catch (XMLStreamException e) {
            verifyException(e, "Duplicate DOCTYPE declaration");
        }
        sr.close();
    }
    
    private XMLStreamReader2 createReader(String content, String enc)
        throws IOException, XMLStreamException
    {
        // Let's ensure it's a new factory, to minimize caching probs
        XMLInputFactory2 f = getNewInputFactory();
        if (enc == null) { // reader-based
            return (XMLStreamReader2) f.createXMLStreamReader(new StringReader(content));
        }
        // nope, byte based
        byte[] data = content.getBytes(enc);
        return (XMLStreamReader2) f.createXMLStreamReader(new ByteArrayInputStream(data));
    }
}
