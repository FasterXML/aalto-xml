package wstream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.junit.jupiter.api.Test;

import base.BaseTestCase;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * [aalto-xml#129]: {@code writeCData(String)} chunks its input into 512-char
 * pieces (the {@code _copyBuffer}). When a "]]" or "]]>" sequence straddled
 * that chunk boundary, the per-chunk scanner in {@code writeCDataContents}
 * could not see the full pattern, so the output either lost a ']' or contained
 * a literal "]]>" inside an unterminated CDATA section. Fixed by backing the
 * chunk end off if it would land between the brackets and '>'.
 */
public class CDataChunkBoundary129Test extends BaseTestCase
{
    final XMLOutputFactory2 OUTPUT_FACTORY = newOutputFactory();
    final XMLInputFactory2 INPUT_FACTORY = newInputFactory();

    // _copyBuffer default is 512 chars
    @Test
    public void testBracketSplitAcrossChunk() throws Exception {
        // Place "]]" at the very end of the first 512-char chunk and ">" at
        // the start of the next chunk.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 510; ++i) sb.append('a');
        sb.append("]]>after");
        _roundtrip(sb.toString());
    }

    @Test
    public void testBracketsStraddlingOneSplit() throws Exception {
        // "]" at end of chunk1, "]>" at start of chunk2
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 511; ++i) sb.append('a');
        sb.append("]]>after");
        _roundtrip(sb.toString());
    }

    @Test
    public void testTwoBracketsStraddlingOnlyBrackets() throws Exception {
        // "]]" at end of chunk1, nothing-special at start of chunk2
        // (this is the bare-]] regression from #100, at the seam)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 510; ++i) sb.append('a');
        sb.append("]] x after");
        _roundtrip(sb.toString());
    }

    // Parallel coverage for the char-based writer (StringWriter destination).
    // CharXmlWriter previously had a broader bug: its "]]>" detection was
    // keyed off the '>' char-type slot which is CT_OK in the writer table, so
    // the case never fired and literal "]]>" was emitted regardless of chunk
    // boundaries. Verify both the basic and chunk-boundary cases here.

    @Test
    public void testBracketSplitAcrossChunkChars() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 510; ++i) sb.append('a');
        sb.append("]]>after");
        _roundtripChars(sb.toString());
    }

    @Test
    public void testBracketsStraddlingOneSplitChars() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 511; ++i) sb.append('a');
        sb.append("]]>after");
        _roundtripChars(sb.toString());
    }

    private void _roundtrip(String content) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLStreamWriter2 sw = (XMLStreamWriter2) OUTPUT_FACTORY.createXMLStreamWriter(out, ENC_UTF8);
        sw.writeStartDocument();
        sw.writeStartElement("r");
        sw.writeCData(content);
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();

        byte[] bytes = out.toByteArray();
        XMLStreamReader sr = INPUT_FACTORY.createXMLStreamReader(new ByteArrayInputStream(bytes), ENC_UTF8);
        _assertRoundtrip(content, sr,
                "raw output (last 40 bytes): " + new String(bytes, Math.max(0, bytes.length-40), Math.min(40, bytes.length)));
    }

    private void _roundtripChars(String content) throws Exception
    {
        StringWriter out = new StringWriter();
        XMLStreamWriter2 sw = (XMLStreamWriter2) OUTPUT_FACTORY.createXMLStreamWriter(out);
        sw.writeStartDocument();
        sw.writeStartElement("r");
        sw.writeCData(content);
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();

        String written = out.toString();
        XMLStreamReader sr = INPUT_FACTORY.createXMLStreamReader(new StringReader(written));
        _assertRoundtrip(content, sr,
                "raw output (last 40 chars): " + written.substring(Math.max(0, written.length()-40)));
    }

    private void _assertRoundtrip(String content, XMLStreamReader sr, String diagnostic) throws Exception
    {
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        StringBuilder got = new StringBuilder();
        int tt;
        while ((tt = sr.next()) == CHARACTERS || tt == CDATA) {
            got.append(sr.getText());
        }
        assertTokenType(END_ELEMENT, tt);
        sr.close();
        if (!content.equals(got.toString())) {
            fail("Content corrupted across chunk boundary.\n"
                    + " expected length=" + content.length() + ", got length=" + got.length()
                    + "\n " + diagnostic);
        }
    }
}
