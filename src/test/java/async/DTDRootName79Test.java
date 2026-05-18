package async;

import java.nio.charset.StandardCharsets;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// [aalto-xml#79] DTD root name returned by AsyncXMLStreamReader is
// truncated when its byte length isn't a multiple of 4 (and parsing
// blows up entirely when a non-ASCII char crosses the 4-byte boundary).
public class DTDRootName79Test extends AsyncTestBase
{
    @Test
    public void testRootNameAnyMultipleOf4() throws Exception {
        // 12 bytes — should keep working
        _testRootName("RootNameTwlv");
    }

    @Test
    public void testRootNameLen11() throws Exception {
        // From the issue: 11 bytes — should return full name
        _testRootName("AnyRootName");
    }

    @Test
    public void testRootNameLen9() throws Exception {
        _testRootName("ShortRoot");
    }

    @Test
    public void testRootNameLen5() throws Exception {
        _testRootName("fives");
    }

    @Test
    public void testRootNameLen6() throws Exception {
        _testRootName("sixsix");
    }

    @Test
    public void testRootNameLen7() throws Exception {
        _testRootName("sevenup");
    }

    // Tests covering many byte-lengths in one shot; useful to see the
    // pattern (failing on non-multiples of 4).
    @Test
    public void testRootNamesAcrossLengths() throws Exception {
        StringBuilder name = new StringBuilder("Aa");
        for (int len = 2; len <= 20; ++len) {
            _testRootName(name.toString());
            name.append((char) ('a' + (len % 26)));
        }
    }

    // The "blows up entirely" case from the issue: a non-ASCII char that
    // pushes a byte across the 4-byte boundary used to throw
    // "Unexpected end-of-input in name".
    @Test
    public void testRootNameWithMultibyteCharAtBoundary() throws Exception {
        // "AnyRootNameä" — 11 ASCII + 2-byte UTF-8 = 13 bytes; the ä lands
        // straddling a 4-byte word boundary.
        _testRootName("AnyRootNameä");
    }

    @Test
    public void testRootNameWithMultibyteCharMidName() throws Exception {
        // "AnyRootNäme" — works in the issue; included for symmetry
        _testRootName("AnyRootNäme");
    }

    private void _testRootName(String rootName) throws Exception {
        // Repro of the exact construction in the issue, then walked via
        // both feeder types and a couple of chunk sizes.
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                + "<!DOCTYPE " + rootName + " SYSTEM \"somedtd.dtd\">\r\n"
                + "<data>somedata</data>";

        // Original reproducer: single feed of the whole document.
        _verifyByteArray(xml, rootName, /*chunkSize*/ xml.getBytes(StandardCharsets.UTF_8).length);
        // Plus incremental feeding to exercise EVENT_INCOMPLETE paths.
        _verifyByteArray(xml, rootName, 1);
        _verifyByteArray(xml, rootName, 3);
        _verifyByteBuffer(xml, rootName, 1);
        _verifyByteBuffer(xml, rootName, 3);
    }

    private void _verifyByteArray(String xml, String expectedRootName, int chunkSize) throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = null;
        try {
            sr = f.createAsyncForByteArray();
            AsyncReaderWrapperForByteArray reader = new AsyncReaderWrapperForByteArray(sr, chunkSize, xml);
            _walkToDtdAndVerify(sr, reader, expectedRootName, "byte[]/chunk=" + chunkSize);
        } finally {
            if (sr != null) sr.close();
        }
    }

    private void _verifyByteBuffer(String xml, String expectedRootName, int chunkSize) throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr = null;
        try {
            sr = f.createAsyncForByteBuffer();
            AsyncReaderWrapperForByteBuffer reader = new AsyncReaderWrapperForByteBuffer(sr, chunkSize, xml);
            _walkToDtdAndVerify(sr, reader, expectedRootName, "ByteBuffer/chunk=" + chunkSize);
        } finally {
            if (sr != null) sr.close();
        }
    }

    private void _walkToDtdAndVerify(AsyncXMLStreamReader<?> sr, AsyncReaderWrapper reader,
                                     String expectedRootName, String label)
        throws Exception
    {
        // verifyStart already pulls START_DOCUMENT and one more event
        int t = verifyStart(reader);
        // Expect to land on DTD first.
        if (t != XMLStreamConstants.DTD) {
            fail("[" + label + "] expected DTD event, got " + tokenTypeDesc(t));
        }
        String actual = sr.getDTDInfo().getDTDRootName();
        assertEquals(expectedRootName, actual,
                "[" + label + "] DTD root name mismatch");
    }
}
