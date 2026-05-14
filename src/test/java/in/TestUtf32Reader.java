package in;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for Utf32Reader: its constructor previously failed to
 * assign mIn/mBuffer/mPtr/mLength, so the very first read() returned -1
 * (mBuffer==null path) and any UTF-32 document parsed as empty.
 */
public class TestUtf32Reader
    extends base.BaseTestCase
{
    @Test
    public void testParseUtf32BE() throws Exception {
        _parseAndVerify("UTF-32BE");
    }

    @Test
    public void testParseUtf32LE() throws Exception {
        _parseAndVerify("UTF-32LE");
    }

    private void _parseAndVerify(String enc) throws Exception
    {
        String xml = "<?xml version='1.0' encoding='" + enc + "'?>"
                + "<root><child>hello</child></root>";
        byte[] bytes = xml.getBytes(Charset.forName(enc));

        XMLStreamReader sr = getInputFactory().createXMLStreamReader(new ByteArrayInputStream(bytes));

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("child", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("hello", sr.getText());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("child", sr.getLocalName());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /**
     * Exercise the surrogate-split branch by including a supplementary-plane
     * character (U+1F600, beyond the BMP) which must be emitted as a Java
     * surrogate pair across two read() iterations.
     */
    @Test
    public void testParseUtf32WithSupplementary() throws Exception
    {
        String enc = "UTF-32BE";
        String xml = "<?xml version='1.0' encoding='" + enc + "'?>"
                + "<root>a\uD83D\uDE00b</root>"; // 😀
        byte[] bytes = xml.getBytes(Charset.forName(enc));

        XMLStreamReader sr = getInputFactory().createXMLStreamReader(new ByteArrayInputStream(bytes));

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("a\uD83D\uDE00b", sr.getText());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }
}
