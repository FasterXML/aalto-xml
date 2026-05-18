package stream;

import java.io.ByteArrayInputStream;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// [aalto-xml#73] getLocation().getCharacterOffset() must include the XML
// declaration / prolog even when the document is in a non-UTF-8 encoding
// that routes through ReaderScanner.
public class Location73Test extends base.BaseTestCase
{
    private final XMLInputFactory2 F = newInputFactory();

    @Test
    public void testCharacterOffsetIncludesPrologUtf8() throws Exception
    {
        // UTF-8 already worked (Utf8Scanner path) — guard against regression.
        _verifyOffsetIncludesProlog("UTF-8");
    }

    @Test
    public void testCharacterOffsetIncludesPrologIso88592() throws Exception
    {
        // The originally reported failure: ISO-8859-2 routes through
        // ReaderScanner, which previously started its offset at 0.
        _verifyOffsetIncludesProlog("ISO-8859-2");
    }

    @Test
    public void testCharacterOffsetIncludesPrologIso88591() throws Exception
    {
        // Latin-1 has a fast-path Utf8Scanner branch too, so this one is
        // mostly for symmetry/coverage.
        _verifyOffsetIncludesProlog("ISO-8859-1");
    }

    private void _verifyOffsetIncludesProlog(String encoding) throws Exception
    {
        final String body = "<root/>";
        final String prolog =
                "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>";
        final String doc = prolog + body;

        XMLStreamReader2 sr = (XMLStreamReader2) F.createXMLStreamReader(
                new ByteArrayInputStream(doc.getBytes(encoding)));
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
}
