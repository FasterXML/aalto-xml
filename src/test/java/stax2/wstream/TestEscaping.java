package stax2.wstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

/**
 * This test checks to see that text/attribute value escaping is
 * working properly.
 */
public class TestEscaping
    extends BaseWriterTest
{
    /**
     * This test checks that even though it's 'wrong' to use non-URL/URI
     * namespace URIs, it's not a fatal error; and that the 'uri' value
     * should come back as it was written out.
     */
    public void testBrokenNsURLs()
        throws XMLStreamException
    {
        final String BROKEN_URL1 = "<tag>";
        final String BROKEN_URL2 = "\"";
        final String BROKEN_URL3 = "x&";

        StringWriter strw = new StringWriter();
        XMLStreamWriter2 w = getNonRepairingWriter(strw, true);
            
        w.writeStartDocument();
        w.writeStartElement("", "test", "");
        w.writeNamespace("ns", BROKEN_URL1);
        w.writeStartElement("", "test", "");
        w.writeNamespace("ns", BROKEN_URL2);
        w.writeStartElement("", "test", "");
        w.writeNamespace("ns", BROKEN_URL3);
        
        w.writeEndElement();
        w.writeEndElement();
        w.writeEndElement();
        
        w.writeEndDocument();
        w.close();
        
        // And then let's parse and verify it all:

        String input = strw.toString();

        XMLStreamReader sr = constructNsStreamReader(input, true);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("test", sr.getLocalName());
        assertEquals(1, sr.getNamespaceCount());
        assertEquals("ns", sr.getNamespacePrefix(0));
        assertEquals(BROKEN_URL1, sr.getNamespaceURI(0));
        
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("test", sr.getLocalName());
        assertEquals(1, sr.getNamespaceCount());
        assertEquals("ns", sr.getNamespacePrefix(0));
        assertEquals(BROKEN_URL2, sr.getNamespaceURI(0));

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("test", sr.getLocalName());
        assertEquals(1, sr.getNamespaceCount());
        assertEquals("ns", sr.getNamespacePrefix(0));
        assertEquals(BROKEN_URL3, sr.getNamespaceURI(0));
        
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        
        assertTokenType(END_DOCUMENT, sr.next());
        
        sr.close();
    }

    public void testLatin1Quoting()
        throws XMLStreamException
    {
        final String TEXT = "ab\u00A0cd\tef\u00D8gh\u3c00...";

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter2 w = getNonRepairingWriter(bos, "ISO-8859-1", true);

        w.writeStartDocument();
        w.writeStartElement("root");
        w.writeCharacters(TEXT);
        w.writeEndElement();
        w.writeEndDocument();
        w.close();

        InputStream in = new ByteArrayInputStream(bos.toByteArray());
        XMLStreamReader sr = constructNsStreamReader(in, true);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());

        assertEquals(TEXT, sr.getText());

        assertTokenType(END_ELEMENT, sr.next());
    }

    public void testAsciiQuoting()
        throws XMLStreamException
    {
        final String TEXT = "ab\u00A0cd\tef\u00D8gh\u3c00...";

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter2 w = getNonRepairingWriter(bos, "US-ASCII", true);

        w.writeStartDocument();
        w.writeStartElement("root");
        w.writeCharacters(TEXT);
        w.writeEndElement();
        w.writeEndDocument();
        w.close();

        InputStream in = new ByteArrayInputStream(bos.toByteArray());
        XMLStreamReader sr = constructNsStreamReader(in, true);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());

        assertEquals(TEXT, sr.getText());

        assertTokenType(END_ELEMENT, sr.next());
    }

    public void testLinefeedQuoting() throws Exception
    {
        final String EXP = "<root>a\nb&#xd;c</root>";

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter2 w = getNonRepairingWriter(bos, "US-ASCII", true);
        w.writeStartElement("root");
        w.writeCharacters("a\nb\rc");
        w.writeEndElement();
        w.writeEndDocument();
        w.close();

        assertEquals(EXP, bos.toString("UTF-8"));

        StringWriter strw = new StringWriter();
        w = getNonRepairingWriter(strw, "US-ASCII", true);
        w.writeStartElement("root");
        w.writeCharacters("a\nb\rc");
        w.writeEndElement();
        w.writeEndDocument();
        w.close();

        assertEquals(EXP, strw.toString());
    }
}
