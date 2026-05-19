package in;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage-oriented tests for {@code Utf8Scanner} (byte-based UTF-8 input)
 * and {@code ReaderScanner} (char-based input). Each scenario is exercised
 * via both input paths so a single document drives both scanners and the
 * shared assertion code stays small.
 *<p>
 * These tests target paths that were previously under-exercised: secondary
 * tokens (PI/CDATA/comment) in different document positions, content larger
 * than the default 4000-unit buffer, multi-byte UTF-8 / surrogate handling,
 * entity expansion, DOCTYPE with PUBLIC/SYSTEM ids, newline normalization,
 * "skip" paths (no getText), coalescing, and namespaced attributes.
 */
public class BasicScannerTest
    extends base.BaseTestCase
{
    // Default ReaderConfig char/byte buffer = 4000 units; pick > that to
    // force at least one buffer reload while finishing a token.
    private static final int BIG_LEN = 8500;

    // Supplementary-plane codepoint U+1D11E ("MUSICAL SYMBOL G CLEF"),
    // encoded as a Java surrogate pair so callers stay ASCII.
    private static final String SUPPL_CHAR = "\uD834\uDD1E";

    /*
    /**********************************************************************
    /* Comments
    /**********************************************************************
     */

    @Test
    public void testCommentsBytes() throws Exception { _testComments(true); }

    @Test
    public void testCommentsChars() throws Exception { _testComments(false); }

    private void _testComments(boolean useBytes) throws Exception
    {
        // Comments in prolog, in tree, in epilog
        String doc = "<!-- prolog comment --><root><!-- in tree -->text"
                + "</root><!-- epilog -->";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_DOCUMENT, sr.getEventType());

        assertTokenType(COMMENT, sr.next());
        assertEquals(" prolog comment ", sr.getText());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());

        assertTokenType(COMMENT, sr.next());
        assertEquals(" in tree ", sr.getText());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("text", sr.getText());
        assertTokenType(END_ELEMENT, sr.next());

        assertTokenType(COMMENT, sr.next());
        assertEquals(" epilog ", sr.getText());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    @Test
    public void testCommentSkipBytes() throws Exception { _testCommentSkip(true); }

    @Test
    public void testCommentSkipChars() throws Exception { _testCommentSkip(false); }

    // Exercise skipComment path (don't ask for text)
    private void _testCommentSkip(boolean useBytes) throws Exception
    {
        // Body contains a hyphen inside a comment so the "single -" path is hit
        String doc = "<!-- a - dash --><root/>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(COMMENT, sr.next());
        // intentionally not calling getText() -> skip path
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    @Test
    public void testLongCommentBytes() throws Exception { _testLongComment(true); }

    @Test
    public void testLongCommentChars() throws Exception { _testLongComment(false); }

    private void _testLongComment(boolean useBytes) throws Exception
    {
        StringBuilder sb = new StringBuilder(BIG_LEN);
        for (int i = 0; i < BIG_LEN; ++i) {
            sb.append((char) ('a' + (i % 26)));
        }
        String body = sb.toString();
        String doc = "<root><!--" + body + "--></root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);

        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(COMMENT, sr.next());
        assertEquals(body, sr.getText());
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Processing instructions
    /**********************************************************************
     */

    @Test
    public void testPIBytes() throws Exception { _testPI(true); }

    @Test
    public void testPIChars() throws Exception { _testPI(false); }

    private void _testPI(boolean useBytes) throws Exception
    {
        String doc = "<?pi1 target1?><?pi2?><root><?pi3 data with ? mark?></root><?pi4 epilog?>";
        XMLStreamReader2 sr = createReader(doc, useBytes);

        assertTokenType(START_DOCUMENT, sr.getEventType());

        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        assertEquals("pi1", sr.getPITarget());
        assertEquals("target1", sr.getPIData());

        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        assertEquals("pi2", sr.getPITarget());
        assertEquals("", sr.getPIData());

        assertTokenType(START_ELEMENT, sr.next());

        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        assertEquals("pi3", sr.getPITarget());
        assertEquals("data with ? mark", sr.getPIData());

        assertTokenType(END_ELEMENT, sr.next());

        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        assertEquals("pi4", sr.getPITarget());
        assertTokenType(END_DOCUMENT, sr.next());

        sr.close();
    }

    @Test
    public void testPISkipBytes() throws Exception { _testPISkip(true); }

    @Test
    public void testPISkipChars() throws Exception { _testPISkip(false); }

    // skipPI path
    private void _testPISkip(boolean useBytes) throws Exception
    {
        String doc = "<?keep this data?><root><?ignored data ? not yet end ??></root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);

        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        // don't fetch data -> skipPI
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        // don't fetch data again
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());

        sr.close();
    }

    /*
    /**********************************************************************
    /* CDATA
    /**********************************************************************
     */

    @Test
    public void testCDataBytes() throws Exception { _testCData(true); }

    @Test
    public void testCDataChars() throws Exception { _testCData(false); }

    private void _testCData(boolean useBytes) throws Exception
    {
        // CDATA body contains ']' which should not terminate (only ]]> does)
        String doc = "<root><![CDATA[plain]] not yet ]> still not done]]></root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CDATA, sr.next());
        assertEquals("plain]] not yet ]> still not done", sr.getText());
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    @Test
    public void testCDataSkipBytes() throws Exception { _testCDataSkip(true); }

    @Test
    public void testCDataSkipChars() throws Exception { _testCDataSkip(false); }

    private void _testCDataSkip(boolean useBytes) throws Exception
    {
        String doc = "<root><![CDATA[abc]]></root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CDATA, sr.next());
        // skip
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    @Test
    public void testLongCDataBytes() throws Exception { _testLongCData(true); }

    @Test
    public void testLongCDataChars() throws Exception { _testLongCData(false); }

    private void _testLongCData(boolean useBytes) throws Exception
    {
        StringBuilder sb = new StringBuilder(BIG_LEN);
        for (int i = 0; i < BIG_LEN; ++i) {
            sb.append((char) ('a' + (i % 26)));
        }
        String body = sb.toString();
        String doc = "<root><![CDATA[" + body + "]]></root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CDATA, sr.next());
        assertEquals(body, sr.getText());
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Coalesced text (CDATA + chars + entities)
    /**********************************************************************
     */

    @Test
    public void testCoalescedTextBytes() throws Exception { _testCoalesced(true); }

    @Test
    public void testCoalescedTextChars() throws Exception { _testCoalesced(false); }

    private void _testCoalesced(boolean useBytes) throws Exception
    {
        String doc = "<root>first<![CDATA[ <middle> ]]>after &amp; more"
                + "<![CDATA[final]]></root>";
        XMLStreamReader2 sr = createCoalescingReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("first <middle> after & morefinal", sr.getText());
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    @Test
    public void testCoalescedSkipBytes() throws Exception { _testCoalescedSkip(true); }

    @Test
    public void testCoalescedSkipChars() throws Exception { _testCoalescedSkip(false); }

    private void _testCoalescedSkip(boolean useBytes) throws Exception
    {
        // Skip mode through coalesced segment
        String doc = "<root>plain<![CDATA[piece]]>more</root>";
        XMLStreamReader2 sr = createCoalescingReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CHARACTERS, sr.next());
        // do not fetch text -> skipCoalescedText
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Character & numeric entities
    /**********************************************************************
     */

    @Test
    public void testEntitiesInTextBytes() throws Exception { _testEntitiesInText(true); }

    @Test
    public void testEntitiesInTextChars() throws Exception { _testEntitiesInText(false); }

    private void _testEntitiesInText(boolean useBytes) throws Exception
    {
        // All five XML predefined entities plus decimal/hex char refs
        String doc = "<root>&lt;a&gt;&amp;&apos;&quot; &#65;-&#x42;</root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        // First chunk is the leading &lt; (single resolved char)
        assertTokenType(CHARACTERS, sr.next());
        // The reader may split around entity boundaries; concatenate to verify
        StringBuilder concat = new StringBuilder(sr.getText());
        while (sr.next() == CHARACTERS) {
            concat.append(sr.getText());
        }
        // After loop we should be on END_ELEMENT
        assertTokenType(END_ELEMENT, sr.getEventType());
        assertEquals("<a>&'\" A-B", concat.toString());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    @Test
    public void testEntitiesInAttributesBytes() throws Exception { _testEntitiesInAttrs(true); }

    @Test
    public void testEntitiesInAttributesChars() throws Exception { _testEntitiesInAttrs(false); }

    private void _testEntitiesInAttrs(boolean useBytes) throws Exception
    {
        String doc = "<root a=\"&lt;&gt;&amp;&#65;\" b='&quot;&apos;&#x21;'/>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(2, sr.getAttributeCount());
        // Stax doesn't guarantee attribute order, so look up by name
        String a = null, b = null;
        for (int i = 0, n = sr.getAttributeCount(); i < n; ++i) {
            String name = sr.getAttributeLocalName(i);
            if (name.equals("a")) {
                a = sr.getAttributeValue(i);
            } else if (name.equals("b")) {
                b = sr.getAttributeValue(i);
            }
        }
        assertEquals("<>&A", a);
        assertEquals("\"'!", b);
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Multi-byte UTF-8 + surrogates
    /**********************************************************************
     */

    @Test
    public void testMultiByteUtf8InText() throws Exception
    {
        // Latin-1 supplement (2-byte), CJK (3-byte), supplementary plane (4-byte)
        // Use byte path to drive the UTF-8 decoder branches.
        String body = "two:\u00E9 three:\u4E2D four:" + SUPPL_CHAR;
        String doc = "<root>" + body + "</root>";
        XMLStreamReader2 sr = createReader(doc, true);
        assertTokenType(START_ELEMENT, sr.next());
        StringBuilder concat = new StringBuilder();
        int t;
        while ((t = sr.next()) == CHARACTERS) {
            concat.append(sr.getText());
        }
        assertTokenType(END_ELEMENT, t);
        assertEquals(body, concat.toString());
        sr.close();
    }

    @Test
    public void testMultiByteUtf8InAttribute() throws Exception
    {
        String value = "\u00E9-\u4E2D-" + SUPPL_CHAR;
        String doc = "<root attr=\"" + value + "\"/>";
        XMLStreamReader2 sr = createReader(doc, true);
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(1, sr.getAttributeCount());
        assertEquals(value, sr.getAttributeValue(0));
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    @Test
    public void testMultiByteUtf8InElementName() throws Exception
    {
        // Element name with non-ASCII chars: triggers PName multibyte path
        String name = "el\u00E9ment\u4E2D";
        String doc = "<" + name + ">x</" + name + ">";
        XMLStreamReader2 sr = createReader(doc, true);
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(name, sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("x", sr.getText());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals(name, sr.getLocalName());
        sr.close();
    }

    @Test
    public void testSurrogateInTextChars() throws Exception
    {
        // Char reader path: input contains actual surrogate pair
        String body = "x" + SUPPL_CHAR + "y";
        String doc = "<root>" + body + "</root>";
        XMLStreamReader2 sr = createReader(doc, false);
        assertTokenType(START_ELEMENT, sr.next());
        StringBuilder concat = new StringBuilder();
        int t;
        while ((t = sr.next()) == CHARACTERS) {
            concat.append(sr.getText());
        }
        assertTokenType(END_ELEMENT, t);
        assertEquals(body, concat.toString());
        sr.close();
    }

    @Test
    public void testSurrogateViaCharRefBytes() throws Exception { _testSurrogateViaCharRef(true); }

    @Test
    public void testSurrogateViaCharRefChars() throws Exception { _testSurrogateViaCharRef(false); }

    private void _testSurrogateViaCharRef(boolean useBytes) throws Exception
    {
        // Hex char ref above 0xFFFF must expand to a surrogate pair
        String doc = "<root>x&#x1D11E;y</root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        StringBuilder concat = new StringBuilder();
        int t;
        while ((t = sr.next()) == CHARACTERS) {
            concat.append(sr.getText());
        }
        assertTokenType(END_ELEMENT, t);
        assertEquals("x" + SUPPL_CHAR + "y", concat.toString());
        sr.close();
    }

    /*
    /**********************************************************************
    /* DOCTYPE with PUBLIC / SYSTEM identifiers
    /**********************************************************************
     */

    @Test
    public void testDoctypePublicBytes() throws Exception { _testDoctypePublic(true); }

    @Test
    public void testDoctypePublicChars() throws Exception { _testDoctypePublic(false); }

    private void _testDoctypePublic(boolean useBytes) throws Exception
    {
        // Both PUBLIC and SYSTEM ids; whitespace inside the public id is
        // collapsed to a single space.
        String doc = "<!DOCTYPE root PUBLIC '-//Test//DTD Foo//EN'"
                + " 'foo.dtd'><root/>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(DTD, sr.next());
        // exercise parsePublicId / parseSystemId
        sr.getText();
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    @Test
    public void testDoctypeSystemBytes() throws Exception { _testDoctypeSystem(true); }

    @Test
    public void testDoctypeSystemChars() throws Exception { _testDoctypeSystem(false); }

    private void _testDoctypeSystem(boolean useBytes) throws Exception
    {
        String doc = "<!DOCTYPE root SYSTEM 'foo.dtd'><root/>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(DTD, sr.next());
        sr.getText();
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Line endings normalization
    /**********************************************************************
     */

    @Test
    public void testLineEndingsBytes() throws Exception { _testLineEndings(true); }

    @Test
    public void testLineEndingsChars() throws Exception { _testLineEndings(false); }

    private void _testLineEndings(boolean useBytes) throws Exception
    {
        // Mix of CR, LF, CRLF inside an element body must normalize to "\n"
        String doc = "<root>a\rb\r\nc\nd</root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        StringBuilder concat = new StringBuilder();
        int t;
        while ((t = sr.next()) == CHARACTERS) {
            concat.append(sr.getText());
        }
        assertTokenType(END_ELEMENT, t);
        assertEquals("a\nb\nc\nd", concat.toString());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Whitespace events / SPACE token
    /**********************************************************************
     */

    @Test
    public void testWhitespaceInPrologBytes() throws Exception { _testWhitespaceInProlog(true); }

    @Test
    public void testWhitespaceInPrologChars() throws Exception { _testWhitespaceInProlog(false); }

    private void _testWhitespaceInProlog(boolean useBytes) throws Exception
    {
        // Whitespace before root and after root: tests prolog/epilog WS skipping
        String doc = "  \n<?pi t?> \r\n<root/> \n  ";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    @Test
    public void testWhitespaceOnlyTextBytes() throws Exception { _testWhitespaceOnlyText(true); }

    @Test
    public void testWhitespaceOnlyTextChars() throws Exception { _testWhitespaceOnlyText(false); }

    private void _testWhitespaceOnlyText(boolean useBytes) throws Exception
    {
        // Inside element: WS + non-WS text + WS only; concatenate
        String doc = "<root>\n  \t<child/>\n</root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        // Whitespace before <child/> -- reported as CHARACTERS for mixed content
        int t = sr.next();
        if (t == CHARACTERS) {
            sr.getText();
            t = sr.next();
        }
        assertTokenType(START_ELEMENT, t);
        assertEquals("child", sr.getLocalName());
        assertTokenType(END_ELEMENT, sr.next());
        t = sr.next();
        if (t == CHARACTERS) {
            sr.getText();
            t = sr.next();
        }
        assertTokenType(END_ELEMENT, t);
        sr.close();
    }

    /*
    /**********************************************************************
    /* Namespaced attributes / namespace declarations
    /**********************************************************************
     */

    @Test
    public void testNamespacedAttributesBytes() throws Exception { _testNsAttrs(true); }

    @Test
    public void testNamespacedAttributesChars() throws Exception { _testNsAttrs(false); }

    private void _testNsAttrs(boolean useBytes) throws Exception
    {
        String doc = "<root xmlns:a='urn:a' xmlns:b='urn:b' a:x='1' b:y='2' z='3'/>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(2, sr.getNamespaceCount());
        assertEquals(3, sr.getAttributeCount());
        // Sanity check the values
        String x = null, y = null, z = null;
        for (int i = 0, n = sr.getAttributeCount(); i < n; ++i) {
            String name = sr.getAttributeLocalName(i);
            if (name.equals("x")) {
                x = sr.getAttributeValue(i);
            } else if (name.equals("y")) {
                y = sr.getAttributeValue(i);
            } else if (name.equals("z")) {
                z = sr.getAttributeValue(i);
            }
        }
        assertEquals("1", x);
        assertEquals("2", y);
        assertEquals("3", z);
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Long text content (forces buffer reload)
    /**********************************************************************
     */

    @Test
    public void testLongTextBytes() throws Exception { _testLongText(true); }

    @Test
    public void testLongTextChars() throws Exception { _testLongText(false); }

    private void _testLongText(boolean useBytes) throws Exception
    {
        StringBuilder sb = new StringBuilder(BIG_LEN);
        for (int i = 0; i < BIG_LEN; ++i) {
            sb.append((char) ('a' + (i % 26)));
        }
        String body = sb.toString();
        String doc = "<root>" + body + "</root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        StringBuilder concat = new StringBuilder(BIG_LEN);
        int t;
        while ((t = sr.next()) == CHARACTERS) {
            concat.append(sr.getText());
        }
        assertTokenType(END_ELEMENT, t);
        assertEquals(body, concat.toString());
        sr.close();
    }

    @Test
    public void testLongTextSkipBytes() throws Exception { _testLongTextSkip(true); }

    @Test
    public void testLongTextSkipChars() throws Exception { _testLongTextSkip(false); }

    // skipCharacters across buffer reload
    private void _testLongTextSkip(boolean useBytes) throws Exception
    {
        StringBuilder sb = new StringBuilder(BIG_LEN);
        for (int i = 0; i < BIG_LEN; ++i) {
            sb.append('x');
        }
        String doc = "<root>" + sb + "</root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        // Drain without reading getText() (skip path)
        int t;
        while ((t = sr.next()) == CHARACTERS) {
            // no-op
        }
        assertTokenType(END_ELEMENT, t);
        sr.close();
    }

    /*
    /**********************************************************************
    /* Empty / self-closing elements + nesting
    /**********************************************************************
     */

    @Test
    public void testNestedAndEmptyBytes() throws Exception { _testNestedAndEmpty(true); }

    @Test
    public void testNestedAndEmptyChars() throws Exception { _testNestedAndEmpty(false); }

    private void _testNestedAndEmpty(boolean useBytes) throws Exception
    {
        String doc = "<a><b/><c></c><d><e/></d></a>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next()); // a
        assertEquals("a", sr.getLocalName());
        assertTokenType(START_ELEMENT, sr.next()); // b
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(START_ELEMENT, sr.next()); // c
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(START_ELEMENT, sr.next()); // d
        assertTokenType(START_ELEMENT, sr.next()); // e
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next()); // /d
        assertTokenType(END_ELEMENT, sr.next()); // /a
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Indentation-aware text handling
    /**********************************************************************
     */

    @Test
    public void testIndentationSpacesBytes() throws Exception { _testIndentationSpaces(true); }

    @Test
    public void testIndentationSpacesChars() throws Exception { _testIndentationSpaces(false); }

    // Typical "pretty-printed" XML: text segments start with \n + spaces
    // before each child, which triggers checkInTreeIndentation's fast path.
    private void _testIndentationSpaces(boolean useBytes) throws Exception
    {
        String doc = "<root>\n  <a/>\n  <b/>\n    <c/>\n</root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next()); // root
        // Walk and accumulate so we don't care whether SPACE vs CHARACTERS is reported
        StringBuilder text = new StringBuilder();
        int events = 0;
        int t;
        while ((t = sr.next()) != END_DOCUMENT) {
            if (t == CHARACTERS || t == SPACE) {
                text.append(sr.getText());
            } else {
                ++events;
            }
        }
        // 3 child elements -> 6 element events, plus closing root
        assertEquals(7, events);
        assertTrue(text.toString().contains("\n  "));
        sr.close();
    }

    @Test
    public void testIndentationTabsBytes() throws Exception { _testIndentationTabs(true); }

    @Test
    public void testIndentationTabsChars() throws Exception { _testIndentationTabs(false); }

    private void _testIndentationTabs(boolean useBytes) throws Exception
    {
        String doc = "<root>\n\t<a/>\n\t<b/>\n</root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        int t;
        while ((t = sr.next()) != END_DOCUMENT) {
            if (t == CHARACTERS || t == SPACE) {
                sr.getText();
            }
        }
        sr.close();
    }

    @Test
    public void testIndentationOverflowBytes() throws Exception { _testIndentationOverflow(true); }

    @Test
    public void testIndentationOverflowChars() throws Exception { _testIndentationOverflow(false); }

    // Many leading spaces: exceeds MAX_INDENT_SPACES so the "copy literally"
    // path is taken instead of the shared indentation token.
    private void _testIndentationOverflow(boolean useBytes) throws Exception
    {
        StringBuilder pad = new StringBuilder("\n");
        for (int i = 0; i < 200; ++i) {
            pad.append(' ');
        }
        String doc = "<root>" + pad + "<child/></root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        StringBuilder text = new StringBuilder();
        int t;
        while ((t = sr.next()) == CHARACTERS || t == SPACE) {
            text.append(sr.getText());
        }
        assertTokenType(START_ELEMENT, t);
        assertEquals(pad.toString(), text.toString());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Skip-mode with multi-byte content (UTF-8 only)
    /**********************************************************************
     */

    @Test
    public void testSkipMultiByteText() throws Exception
    {
        // Byte path only: forces skipUtf8_2/3/4 paths in skipCharacters
        String body = "two:\u00E9 three:\u20AC four:" + SUPPL_CHAR;
        String doc = "<root>" + body + "</root>";
        XMLStreamReader2 sr = createReader(doc, true);
        assertTokenType(START_ELEMENT, sr.next());
        int t;
        while ((t = sr.next()) == CHARACTERS) {
            // skip
        }
        assertTokenType(END_ELEMENT, t);
        sr.close();
    }

    @Test
    public void testSkipMultiByteCData() throws Exception
    {
        String body = "two:\u00E9 three:\u20AC four:" + SUPPL_CHAR;
        String doc = "<root><![CDATA[" + body + "]]></root>";
        XMLStreamReader2 sr = createReader(doc, true);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CDATA, sr.next());
        // not consuming text -> skipCData
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    @Test
    public void testSkipMultiByteComment() throws Exception
    {
        // skipComment with multi-byte content
        String doc = "<root><!-- a:\u00E9 b:\u20AC c:" + SUPPL_CHAR + " --></root>";
        XMLStreamReader2 sr = createReader(doc, true);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(COMMENT, sr.next());
        // skip
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    @Test
    public void testSkipMultiBytePI() throws Exception
    {
        String doc = "<root><?tgt body:\u00E9 body:\u20AC body:" + SUPPL_CHAR + "?></root>";
        XMLStreamReader2 sr = createReader(doc, true);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        // skip
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Namespace URI with multi-byte content (handleNsDeclaration)
    /**********************************************************************
     */

    @Test
    public void testNamespaceUriWithMultiByteBytes() throws Exception { _testNsUriMultiByte(true); }

    @Test
    public void testNamespaceUriWithMultiByteChars() throws Exception { _testNsUriMultiByte(false); }

    private void _testNsUriMultiByte(boolean useBytes) throws Exception
    {
        String uri = "urn:\u00E9-\u4E2D-" + SUPPL_CHAR;
        String doc = "<a:elem xmlns:a=\"" + uri + "\"/>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(1, sr.getNamespaceCount());
        assertEquals(uri, sr.getNamespaceURI(0));
        assertEquals(uri, sr.getNamespaceURI("a"));
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Whitespace variants in start-tag
    /**********************************************************************
     */

    @Test
    public void testWhitespaceInStartTagBytes() throws Exception { _testWhitespaceInStartTag(true); }

    @Test
    public void testWhitespaceInStartTagChars() throws Exception { _testWhitespaceInStartTag(false); }

    // Tab and CR/CRLF between attributes, and space before self-closing slash
    private void _testWhitespaceInStartTag(boolean useBytes) throws Exception
    {
        String doc = "<root\tx='1'\r\ny='2'\rz='3' />";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(3, sr.getAttributeCount());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* CR (no LF) inside content
    /**********************************************************************
     */

    @Test
    public void testBareCRInTextBytes() throws Exception { _testBareCRInText(true); }

    @Test
    public void testBareCRInTextChars() throws Exception { _testBareCRInText(false); }

    private void _testBareCRInText(boolean useBytes) throws Exception
    {
        // Bare \r (no \n) inside text, between attributes, in attr value
        String doc = "<root\rattr='v\ra\rb'>x\ry\rz</root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        // attr CR -> normalized to single space (per XML attr-value normalization)
        assertEquals("v a b", sr.getAttributeValue(0));
        StringBuilder text = new StringBuilder();
        int t;
        while ((t = sr.next()) == CHARACTERS) {
            text.append(sr.getText());
        }
        assertTokenType(END_ELEMENT, t);
        assertEquals("x\ny\nz", text.toString());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Empty CDATA / empty comment
    /**********************************************************************
     */

    @Test
    public void testEmptyCDataBytes() throws Exception { _testEmptyCData(true); }

    @Test
    public void testEmptyCDataChars() throws Exception { _testEmptyCData(false); }

    private void _testEmptyCData(boolean useBytes) throws Exception
    {
        String doc = "<root><![CDATA[]]></root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CDATA, sr.next());
        assertEquals("", sr.getText());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    @Test
    public void testEmptyCommentBytes() throws Exception { _testEmptyComment(true); }

    @Test
    public void testEmptyCommentChars() throws Exception { _testEmptyComment(false); }

    private void _testEmptyComment(boolean useBytes) throws Exception
    {
        String doc = "<!----><root/>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(COMMENT, sr.next());
        assertEquals("", sr.getText());
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Long attribute value (force buffer reload inside collectValue)
    /**********************************************************************
     */

    @Test
    public void testLongAttrValueBytes() throws Exception { _testLongAttrValue(true); }

    @Test
    public void testLongAttrValueChars() throws Exception { _testLongAttrValue(false); }

    private void _testLongAttrValue(boolean useBytes) throws Exception
    {
        StringBuilder sb = new StringBuilder(BIG_LEN);
        for (int i = 0; i < BIG_LEN; ++i) {
            sb.append((char) ('a' + (i % 26)));
        }
        String value = sb.toString();
        String doc = "<root attr='" + value + "'/>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(value, sr.getAttributeValue(0));
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* XML declaration variants
    /**********************************************************************
     */

    @Test
    public void testXmlDeclFullBytes() throws Exception { _testXmlDeclFull(true); }

    @Test
    public void testXmlDeclFullChars() throws Exception { _testXmlDeclFull(false); }

    private void _testXmlDeclFull(boolean useBytes) throws Exception
    {
        String doc = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><root/>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertEquals("1.0", sr.getVersion());
        assertEquals("UTF-8", sr.getCharacterEncodingScheme());
        assertTrue(sr.isStandalone());
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* General entity reference fall-through paths in handleEntityInText
    /**********************************************************************
     */

    @Test
    public void testGeneralEntityVariantsBytes() throws Exception { _testGeneralEntityVariants(true); }

    @Test
    public void testGeneralEntityVariantsChars() throws Exception { _testGeneralEntityVariants(false); }

    // Drive the partial-prefix fallback branches in handleEntityInText:
    // "a..", "am..", "ap..", "l..", "g..", "q..", plus a non-prefix start.
    // Aalto rejects unresolved general entities in expanding mode, so each
    // entity is fed via a fresh reader and the expected exception verified.
    private void _testGeneralEntityVariants(boolean useBytes) throws Exception
    {
        String[] names = new String[] { "a", "ab", "am", "amx", "ap", "apo",
                "apox", "l", "lz", "g", "gz", "q", "qu", "quo", "quoX", "zzz" };
        for (String n : names) {
            _expectGeneralEntityFailure(n, useBytes);
        }
    }

    private void _expectGeneralEntityFailure(String name, boolean useBytes) throws Exception
    {
        String doc = "<root>&" + name + ";</root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(START_ELEMENT, sr.next());
        try {
            sr.next();
            fail("Expected failure on unknown general entity &" + name + ";");
        } catch (XMLStreamException e) {
            verifyException(e, "entity");
        }
        sr.close();
    }

    /*
    /**********************************************************************
    /* Reader / stream close
    /**********************************************************************
     */

    @Test
    public void testCloseExplicitBytes() throws Exception { _testCloseExplicit(true); }

    @Test
    public void testCloseExplicitChars() throws Exception { _testCloseExplicit(false); }

    private void _testCloseExplicit(boolean useBytes) throws Exception
    {
        String doc = "<root>text</root>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        sr.next(); // START_ELEMENT
        sr.close(); // exercise _closeSource()
        // calling close again should be safe (no-op)
        sr.close();
    }

    /*
    /**********************************************************************
    /* DTD with internal subset that contains tokens
    /**********************************************************************
     */

    @Test
    public void testDoctypeInternalSubsetBytes() throws Exception { _testDoctypeInternal(true); }

    @Test
    public void testDoctypeInternalSubsetChars() throws Exception { _testDoctypeInternal(false); }

    // Internal subset includes a comment, a PI, an element decl, and an
    // entity decl: drives finishDTD's secondary token handling.
    private void _testDoctypeInternal(boolean useBytes) throws Exception
    {
        String subset = "<!-- comment -->"
                + "<?pi data?>"
                + "<!ELEMENT root (#PCDATA)>"
                + "<!ENTITY foo \"bar\">";
        String doc = "<!DOCTYPE root [" + subset + "]><root/>";
        XMLStreamReader2 sr = createReader(doc, useBytes);
        assertTokenType(DTD, sr.next());
        assertEquals(subset, sr.getText());
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private XMLStreamReader2 createReader(String content, boolean useBytes)
        throws Exception
    {
        XMLInputFactory2 f = newInputFactory();
        if (useBytes) {
            byte[] data = content.getBytes(ENC_UTF8);
            return (XMLStreamReader2) f.createXMLStreamReader(new ByteArrayInputStream(data));
        }
        return (XMLStreamReader2) f.createXMLStreamReader(new StringReader(content));
    }

    private XMLStreamReader2 createCoalescingReader(String content, boolean useBytes)
        throws Exception
    {
        XMLInputFactory2 f = newInputFactory();
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        if (useBytes) {
            byte[] data = content.getBytes(ENC_UTF8);
            return (XMLStreamReader2) f.createXMLStreamReader(new ByteArrayInputStream(data));
        }
        return (XMLStreamReader2) f.createXMLStreamReader(new StringReader(content));
    }
}
