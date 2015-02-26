package stream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

public class TestSimple extends base.BaseTestCase
{
    private final XMLInputFactory2 F2 = getNewInputFactory();
    
    public void testNamespacesBytes() throws Exception {
        _testNamespaces(true);
    }

    public void testNamespacesChars() throws Exception {
        _testNamespaces(false);
    }
    
    public void _testNamespaces(boolean useBytes) throws Exception
    {
        // note: must specify encoding so parsers knows which decoder to use
        String DOC = "<root xmlns='abc' xmlns:a='b' xmlns:b='c'>\n</root>";
        XMLStreamReader2 sr = createReader(DOC, "UTF-8", useBytes);
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
        assertEquals("root", sr.getLocalName());
        assertEquals(useBytes ? -1L : 43L, sr.getLocationInfo().getStartingCharOffset());
        assertEquals(useBytes ? 43L : -1L, sr.getLocationInfo().getStartingByteOffset());
        assertEquals(useBytes ? -1L : 50L, sr.getLocationInfo().getEndingCharOffset());
        assertEquals(useBytes ? 50L : -1L, sr.getLocationInfo().getEndingByteOffset());
        
        loc = sr.getLocation();
        assertEquals(1, loc.getColumnNumber());
        assertEquals(2, loc.getLineNumber());
        assertEquals(43, loc.getCharacterOffset());

        assertTokenType(END_DOCUMENT, sr.next());
        loc = sr.getLocation();
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

    XMLStreamReader2 createReader(String content, String enc, boolean useBytes)
        throws IOException, XMLStreamException
    {
        if (useBytes) {
            byte[] data = content.getBytes(enc);
            return (XMLStreamReader2) F2.createXMLStreamReader(new ByteArrayInputStream(data));
        }
        return (XMLStreamReader2) F2.createXMLStreamReader(new StringReader(content));
    }
}
