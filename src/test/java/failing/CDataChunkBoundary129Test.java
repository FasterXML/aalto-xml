package failing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.junit.jupiter.api.Test;

import base.BaseTestCase;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Manually runnable reproducer for the chunk-boundary variant of
 * [aalto-xml#100]. {@code writeCData(String)} chunks its input into 512-char
 * pieces (the {@code _copyBuffer}) and {@code writeCDataContents} only sees one
 * chunk at a time. When a "]]" or "]]>" sequence straddles that seam neither
 * chunk can detect it, so the output is either silently corrupted (one ']' lost)
 * or contains a literal "]]>" inside an unterminated CDATA section.
 *
 * <p>Proper fix requires tracking trailing-']' state across {@code
 * writeCDataContents} invocations, analogous to the existing {@code _surrogate}
 * field. Kept under {@code failing/} (and named without the {@code Test} prefix
 * so surefire skips it) until that work is done.
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
                    + "\n raw output (last 40 bytes): " + new String(bytes, Math.max(0, bytes.length-40), Math.min(40, bytes.length)));
        }
    }
}
