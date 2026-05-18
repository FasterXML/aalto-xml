package stream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// [aalto-xml#66] When the namespace URI argument is null, the namespace must
// not be checked for equality (per Stax javadoc).
public class Issue66GetAttributeValueTest extends base.BaseTestCase
{
    private final XMLInputFactory2 F = newInputFactory();

    @Test
    public void testAttributeValueNullNsBytes() throws Exception {
        _testAttributeValueNullNs(true);
    }

    @Test
    public void testAttributeValueNullNsChars() throws Exception {
        _testAttributeValueNullNs(false);
    }

    private void _testAttributeValueNullNs(boolean useBytes) throws Exception
    {
        final String DOC =
                "<root xmlns:a='http://foo' xmlns:b='http://bar'"
                + " a:x='1' b:y='2' z='3'/>";
        XMLStreamReader2 sr = _createReader(DOC, useBytes);
        assertTokenType(START_ELEMENT, sr.next());

        // null nsURI: must ignore namespace and match by local name only
        assertEquals("1", sr.getAttributeValue(null, "x"));
        assertEquals("2", sr.getAttributeValue(null, "y"));
        assertEquals("3", sr.getAttributeValue(null, "z"));
        assertNull(sr.getAttributeValue(null, "no-such-attr"));

        // Explicit namespace URI: only matches that namespace
        assertEquals("1", sr.getAttributeValue("http://foo", "x"));
        assertNull(sr.getAttributeValue("http://bar", "x"));
        assertEquals("2", sr.getAttributeValue("http://bar", "y"));
        assertNull(sr.getAttributeValue("http://foo", "y"));

        // Empty-string nsURI: matches "no namespace" only (not a namespaced one)
        assertEquals("3", sr.getAttributeValue("", "z"));
        assertNull(sr.getAttributeValue("", "x"));
        assertNull(sr.getAttributeValue("", "y"));

        sr.close();
    }

    private XMLStreamReader2 _createReader(String content, boolean useBytes)
        throws IOException, XMLStreamException
    {
        if (useBytes) {
            byte[] data = content.getBytes("UTF-8");
            return (XMLStreamReader2) F.createXMLStreamReader(new ByteArrayInputStream(data));
        }
        return (XMLStreamReader2) F.createXMLStreamReader(new StringReader(content));
    }
}
