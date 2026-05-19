package stream;

import java.io.ByteArrayInputStream;

import javax.xml.stream.*;

import org.codehaus.stax2.LocationInfo;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamLocation2;
import org.codehaus.stax2.XMLStreamReader2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// [aalto-xml#73] getLocation().getCharacterOffset() must include the XML
// declaration / prolog even when the document is in a non-UTF-8 encoding
// that routes through ReaderScanner. Also exercises the Stax2 LocationInfo /
// XMLStreamLocation2 accessors to document which fields are populated on
// which scanner path (byte scanner reports byte offsets, ReaderScanner
// reports character offsets; the other field is N/A and returns -1).
public class Location73Test extends base.BaseTestCase
{
    private final XMLInputFactory2 F = newInputFactory();

    private static final String BODY = "<root/>";

    @Test
    public void testCharacterOffsetIncludesPrologUtf8() throws Exception {
        // UTF-8 already worked (Utf8Scanner path) — guard against regression.
        _verifyOffsetIncludesProlog("UTF-8");
    }

    @Test
    public void testCharacterOffsetIncludesPrologIso88592() throws Exception {
        // The originally reported failure: ISO-8859-2 routes through
        // ReaderScanner, which previously started its offset at 0.
        _verifyOffsetIncludesProlog("ISO-8859-2");
    }

    @Test
    public void testCharacterOffsetIncludesPrologIso88591() throws Exception {
        // Latin-1 has a fast-path Utf8Scanner branch too, so this one is
        // mostly for symmetry/coverage.
        _verifyOffsetIncludesProlog("ISO-8859-1");
    }

    // Byte-scanner path: byte offsets must include the prolog; char offsets
    // are documented N/A (-1) since this path doesn't track them.
    @Test
    public void testStax2LocationInfoByteScanner() throws Exception
    {
        final String prolog = _prolog("UTF-8");
        XMLStreamReader2 sr = _reader(prolog + BODY, "UTF-8");
        try {
            assertTokenType(START_ELEMENT, sr.next());
            LocationInfo info = sr.getLocationInfo();

            // Byte offsets: prolog must be counted.
            assertEquals(prolog.length(), info.getStartingByteOffset(),
                    "getStartingByteOffset must include the prolog");
            assertEquals(prolog.length() + BODY.length(), info.getEndingByteOffset(),
                    "getEndingByteOffset must include the prolog + element");

            // Char offsets: N/A for byte scanner path.
            assertEquals(-1L, info.getStartingCharOffset(),
                    "byte scanner should not report char offset");
            assertEquals(-1L, info.getEndingCharOffset(),
                    "byte scanner should not report char offset");

            // XMLStreamLocation2 wrappers carry the same offset via Location.
            XMLStreamLocation2 start = info.getStartLocation();
            XMLStreamLocation2 end = info.getEndLocation();
            assertEquals(prolog.length(), start.getCharacterOffset());
            assertEquals(prolog.length() + BODY.length(), end.getCharacterOffset());
        } finally {
            sr.close();
        }
    }

    // ReaderScanner path: char offsets must include the prolog; byte offsets
    // are documented N/A (-1) since the underlying Reader sees no bytes.
    @Test
    public void testStax2LocationInfoReaderScanner() throws Exception
    {
        final String prolog = _prolog("ISO-8859-2");
        XMLStreamReader2 sr = _reader(prolog + BODY, "ISO-8859-2");
        try {
            assertTokenType(START_ELEMENT, sr.next());
            LocationInfo info = sr.getLocationInfo();

            // Char offsets: prolog must be counted (this is the #73 fix).
            assertEquals(prolog.length(), info.getStartingCharOffset(),
                    "getStartingCharOffset must include the prolog");
            assertEquals(prolog.length() + BODY.length(), info.getEndingCharOffset(),
                    "getEndingCharOffset must include the prolog + element");

            // Byte offsets: N/A for reader-scanner path.
            assertEquals(-1L, info.getStartingByteOffset(),
                    "reader scanner should not report byte offset");
            assertEquals(-1L, info.getEndingByteOffset(),
                    "reader scanner should not report byte offset");

            XMLStreamLocation2 start = info.getStartLocation();
            XMLStreamLocation2 end = info.getEndLocation();
            assertEquals(prolog.length(), start.getCharacterOffset());
            assertEquals(prolog.length() + BODY.length(), end.getCharacterOffset());
        } finally {
            sr.close();
        }
    }

    // Walks through multiple events and asserts offsets march forward in
    // step with the source text — guards against any regression where the
    // prolog seed is added only at the first event.
    @Test
    public void testReaderScannerOffsetsAcrossEvents() throws Exception
    {
        final String prolog = _prolog("ISO-8859-2");
        final String body = "<root><a>x</a></root>";
        XMLStreamReader2 sr = _reader(prolog + body, "ISO-8859-2");
        try {
            // <root>
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("root", sr.getLocalName());
            assertEquals(prolog.length(), sr.getLocationInfo().getStartingCharOffset());

            // <a>
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("a", sr.getLocalName());
            assertEquals(prolog.length() + "<root>".length(),
                    sr.getLocationInfo().getStartingCharOffset());

            // "x"
            assertTokenType(CHARACTERS, sr.next());
            assertEquals("x", sr.getText());
            assertEquals(prolog.length() + "<root><a>".length(),
                    sr.getLocationInfo().getStartingCharOffset());
        } finally {
            sr.close();
        }
    }

    private void _verifyOffsetIncludesProlog(String encoding) throws Exception
    {
        final String prolog = _prolog(encoding);
        XMLStreamReader2 sr = _reader(prolog + BODY, encoding);
        try {
            assertTokenType(START_ELEMENT, sr.next());
            Location loc = sr.getLocation();
            // For UTF-8/Latin-1/ASCII this is byte offset, for ISO-8859-2 it
            // is character offset; for the ASCII prolog those happen to
            // coincide (one byte == one char).
            assertEquals(prolog.length(), loc.getCharacterOffset(),
                    "encoding=" + encoding
                            + ": getCharacterOffset() must include the XML prolog");
        } finally {
            sr.close();
        }
    }

    private static String _prolog(String encoding) {
        return "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>";
    }

    private XMLStreamReader2 _reader(String doc, String encoding) throws Exception {
        return (XMLStreamReader2) F.createXMLStreamReader(
                new ByteArrayInputStream(doc.getBytes(encoding)));
    }
}
