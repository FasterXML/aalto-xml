package wstream;

import java.io.*;

import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;

import org.junit.jupiter.api.Test;

import base.BaseTestCase;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for [aalto-xml#100]: a "]]" sequence inside CDATA content that is NOT
 * followed by '>' must be written through verbatim. Previously the first ']'
 * was dropped by the byte-based writer.
 */
public class CDataWithBrackets100Test extends BaseTestCase
{
    final XMLOutputFactory2 OUTPUT_FACTORY = newOutputFactory();
    final XMLInputFactory2 INPUT_FACTORY = newInputFactory();

    @Test
    public void testDoubleBracketSpaceGtUtf8() throws Exception {
        _roundtripCData("a ]] > b", ENC_UTF8);
    }

    @Test
    public void testDoubleBracketSpaceGtLatin1() throws Exception {
        _roundtripCData("a ]] > b", ENC_LATIN1);
    }

    @Test
    public void testDoubleBracketSpaceGtAscii() throws Exception {
        _roundtripCData("a ]] > b", ENC_ASCII);
    }

    @Test
    public void testDoubleBracketSpaceGtChars() throws Exception {
        _roundtripCData("a ]] > b", null);
    }

    @Test
    public void testTrailingDoubleBracketUtf8() throws Exception {
        _roundtripCData("trailing ]]", ENC_UTF8);
    }

    @Test
    public void testSingleBracketGtUtf8() throws Exception {
        _roundtripCData("one ] > bracket", ENC_UTF8);
    }

    /**
     * Triple right-bracket: the parser must still split for the literal "]]>"
     * triggered by the last two brackets, leaving a lone ']' in front.
     */
    @Test
    public void testTripleBracketGtUtf8() throws Exception {
        _roundtripCData("x]]]>y", ENC_UTF8);
    }

    /**
     * Drives writeCData(char[],int,int) rather than writeCData(String) to make
     * sure both code paths exercise the same fix.
     */
    @Test
    public void testDoubleBracketCharArrayUtf8() throws Exception {
        final String content = "a ]] > b";
        char[] cbuf = new char[content.length() + 4];
        content.getChars(0, content.length(), cbuf, 2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLStreamWriter2 sw = (XMLStreamWriter2) OUTPUT_FACTORY.createXMLStreamWriter(out, ENC_UTF8);
        sw.writeStartDocument();
        sw.writeStartElement("r");
        sw.writeCData(cbuf, 2, content.length());
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();

        assertEquals(content, _readSingleCData(new ByteArrayInputStream(out.toByteArray()), ENC_UTF8));
    }

    private void _roundtripCData(String content, String enc) throws Exception
    {
        if (enc == null) {
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 sw = (XMLStreamWriter2) OUTPUT_FACTORY.createXMLStreamWriter(strw);
            _writeDoc(sw, content);
            assertEquals(content, _readSingleCData(new StringReader(strw.toString())));
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            XMLStreamWriter2 sw = (XMLStreamWriter2) OUTPUT_FACTORY.createXMLStreamWriter(out, enc);
            _writeDoc(sw, content);
            assertEquals(content, _readSingleCData(new ByteArrayInputStream(out.toByteArray()), enc));
        }
    }

    private void _writeDoc(XMLStreamWriter2 sw, String content) throws Exception
    {
        sw.writeStartDocument();
        sw.writeStartElement("r");
        sw.writeCData(content);
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();
    }

    private String _readSingleCData(InputStream in, String enc) throws Exception {
        return _drainText(INPUT_FACTORY.createXMLStreamReader(in, enc));
    }

    private String _readSingleCData(Reader r) throws Exception {
        return _drainText(INPUT_FACTORY.createXMLStreamReader(r));
    }

    private String _drainText(XMLStreamReader sr) throws Exception
    {
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        StringBuilder sb = new StringBuilder();
        int tt;
        while ((tt = sr.next()) == CHARACTERS || tt == CDATA) {
            sb.append(sr.getText());
        }
        assertTokenType(END_ELEMENT, tt);
        sr.close();
        return sb.toString();
    }
}
