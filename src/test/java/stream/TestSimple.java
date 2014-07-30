package stream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

public class TestSimple extends base.BaseTestCase
{
    public void testNamespaces() throws Exception
    {
        // note: must specify encoding so parsers knows which decoder to use
        String DOC = "<root xmlns='abc' xmlns:a='b' xmlns:b='c'>\n</root>";
        XMLStreamReader2 sr = createReader(DOC, "UTF-8");
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertEquals(3, sr.getNamespaceCount());
        
        /* Although Stax does not mandate that ordering of namespace
         * declarations is preserved, ideally we would want to have them
         * that way...
         */
        assertEquals("", sr.getNamespacePrefix(0));
        assertEquals("abc", sr.getNamespaceURI(0));
        assertEquals("a", sr.getNamespacePrefix(1));
        assertEquals("b", sr.getNamespaceURI(1));
        assertEquals("b", sr.getNamespacePrefix(2));
        assertEquals("c", sr.getNamespaceURI(2));

        // Aalto follows Woodstox, and by default reports token start loc for "getLocation()"
        Location loc = sr.getLocation();
        assertEquals(1, loc.getLineNumber());
        assertEquals(1, loc.getColumnNumber());
        assertEquals(0, loc.getCharacterOffset());

        // and end should differ a bit
        loc = sr.getLocationInfo().getEndLocation();
        assertEquals(1, loc.getLineNumber());
        assertEquals(43,  loc.getColumnNumber());
        assertEquals(42, loc.getCharacterOffset());
        
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("\n", sr.getText());

        loc = sr.getLocation();
        assertEquals(1, loc.getLineNumber());
        assertEquals(43,  loc.getColumnNumber());
        assertEquals(42, loc.getCharacterOffset());
        loc = sr.getLocationInfo().getEndLocation();
        assertEquals(2, loc.getLineNumber());
        assertEquals(1,  loc.getColumnNumber());
        assertEquals(43, loc.getCharacterOffset());
        
        assertTokenType(END_ELEMENT, sr.next());
        loc = sr.getLocation();
        assertEquals(1, loc.getColumnNumber());
        assertEquals(2, loc.getLineNumber());
        assertEquals(43, loc.getCharacterOffset());

        assertTokenType(END_DOCUMENT, sr.next());
        assertEquals(2, loc.getLineNumber());
        assertEquals(8, loc.getColumnNumber());
        assertEquals(50, loc.getCharacterOffset());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */


    XMLStreamReader2 createReader(String content, String enc)
        throws IOException, XMLStreamException
    {
        // Let's ensure it's a new factory, to minimize caching probs
        XMLInputFactory2 f = getNewInputFactory();
        byte[] data = content.getBytes(enc);
        return (XMLStreamReader2) f.createXMLStreamReader(new ByteArrayInputStream(data));
    }

}
