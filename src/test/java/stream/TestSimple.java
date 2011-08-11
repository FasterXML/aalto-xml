package stream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLInputFactory2;

public class TestSimple extends base.BaseTestCase
{
    public void testNamespaces() throws Exception
    {
        // note: must specify encoding so parsers knows which decoder to use
        String DOC = "<root xmlns='abc' xmlns:a='b' xmlns:b='c' />";
        XMLStreamReader sr = createReader(DOC, "UTF-8");
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
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */


    XMLStreamReader createReader(String content, String enc)
        throws IOException, XMLStreamException
    {
        // Let's ensure it's a new factory, to minimize caching probs
        XMLInputFactory2 f = getNewInputFactory();
        byte[] data = content.getBytes(enc);
        return f.createXMLStreamReader(new ByteArrayInputStream(data));
    }

}
