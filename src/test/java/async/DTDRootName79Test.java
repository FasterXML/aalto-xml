package async;

import java.nio.charset.StandardCharsets;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamConstants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// [aalto-xml#79] DTD root name returned by AsyncXMLStreamReader is
// truncated when its byte length isn't a multiple of 4 (and parsing
// blows up entirely when a non-ASCII char crosses the 4-byte boundary).
public class DTDRootName79Test extends AsyncTestBase
{
    // ASCII names across a range of byte-lengths (mod-4 and non-mod-4)
    // exercising the truncation path.
    @Test
    public void testRootNameAsciiLengths() throws Exception {
        String[] names = {
                "Aa",            // 2
                "Aab",           // 3
                "Quad",          // 4  (mod-4)
                "fives",         // 5
                "sixsix",        // 6
                "sevenup",       // 7
                "eightupp",      // 8  (mod-4)
                "ShortRoot",     // 9
                "AnyRootName",   // 11 — from the issue
                "RootNameTwlv",  // 12 (mod-4)
                "ThirteenRoots", // 13
        };
        for (String n : names) {
            _testRootName(n);
        }
    }

    // Multi-byte UTF-8 chars. "AnyRootNameä" (13 bytes: 11 ASCII + 2-byte ä)
    // used to *crash* with "Unexpected end-of-input in name" because the ä
    // straddled the buggy 4-byte boundary; "AnyRootNäme" already worked but
    // is included for symmetry.
    @Test
    public void testRootNameMultibyte() throws Exception {
        _testRootName("AnyRootNameä");
        _testRootName("AnyRootNäme");
    }

    private void _testRootName(String rootName) throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                + "<!DOCTYPE " + rootName + " SYSTEM \"somedtd.dtd\">\r\n"
                + "<data>somedata</data>";
        final int total = xml.getBytes(StandardCharsets.UTF_8).length;

        // Whole-document single feed (the original reproducer) and a couple
        // of small chunk sizes to exercise EVENT_INCOMPLETE resume paths.
        for (int chunkSize : new int[] { total, 1, 3 }) {
            _verifyByteArray(xml, rootName, chunkSize);
            _verifyByteBuffer(xml, rootName, chunkSize);
        }
    }

    private void _verifyByteArray(String xml, String expectedRootName, int chunkSize) throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = null;
        try {
            sr = f.createAsyncForByteArray();
            AsyncReaderWrapperForByteArray reader = new AsyncReaderWrapperForByteArray(sr, chunkSize, xml);
            _walkToDtdAndVerify(sr, reader, expectedRootName,
                    "byte[]/'" + expectedRootName + "'/chunk=" + chunkSize);
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
            _walkToDtdAndVerify(sr, reader, expectedRootName,
                    "ByteBuffer/'" + expectedRootName + "'/chunk=" + chunkSize);
        } finally {
            if (sr != null) sr.close();
        }
    }

    private void _walkToDtdAndVerify(AsyncXMLStreamReader<?> sr, AsyncReaderWrapper reader,
                                     String expectedRootName, String label)
        throws Exception
    {
        int t = verifyStart(reader);
        if (t != XMLStreamConstants.DTD) {
            fail("[" + label + "] expected DTD event, got " + tokenTypeDesc(t));
        }
        assertEquals(expectedRootName, sr.getDTDInfo().getDTDRootName(),
                "[" + label + "] DTD root name mismatch");
    }
}
