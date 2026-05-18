package async;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// [aalto-xml#66] Verify the null-namespace lookup fix also applies to the
// async scanners (which share `AttributeCollector` / `PName` machinery with
// the blocking side).
public class Issue66AsyncTest extends AsyncTestBase
{
    private static final AsyncXMLInputFactory F = new InputFactoryImpl();

    private static final String DOC =
            "<root xmlns:a='http://foo' xmlns:b='http://bar'"
            + " a:x='1' b:y='2' z='3'/>";

    @Test
    public void testNullNsLookupByteArray() throws Exception
    {
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = F.createAsyncForByteArray();
        try {
            AsyncReaderWrapperForByteArray reader = new AsyncReaderWrapperForByteArray(
                    sr, 3, DOC);
            assertTokenType(START_ELEMENT, verifyStart(reader));
            _assertLookups(sr);
        } finally {
            sr.close();
        }
    }

    @Test
    public void testNullNsLookupByteBuffer() throws Exception
    {
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr = F.createAsyncForByteBuffer();
        try {
            AsyncReaderWrapperForByteBuffer reader = new AsyncReaderWrapperForByteBuffer(
                    sr, 3, DOC);
            assertTokenType(START_ELEMENT, verifyStart(reader));
            _assertLookups(sr);
        } finally {
            sr.close();
        }
    }

    // Direct feed path (no wrapper / no incremental refeed) to exercise the
    // simplest async flow as well.
    @Test
    public void testNullNsLookupDirectFeed() throws Exception
    {
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = F.createAsyncForByteArray();
        try {
            byte[] bytes = DOC.getBytes(StandardCharsets.UTF_8);
            sr.getInputFeeder().feedInput(bytes, 0, bytes.length);
            sr.getInputFeeder().endOfInput();
            assertEquals(START_DOCUMENT, sr.next());
            assertEquals(START_ELEMENT, sr.next());
            _assertLookups(sr);
        } finally {
            sr.close();
        }
    }

    @Test
    public void testNullNsLookupDirectFeedByteBuffer() throws Exception
    {
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr = F.createAsyncForByteBuffer();
        try {
            sr.getInputFeeder().feedInput(
                    ByteBuffer.wrap(DOC.getBytes(StandardCharsets.UTF_8)));
            sr.getInputFeeder().endOfInput();
            assertEquals(START_DOCUMENT, sr.next());
            assertEquals(START_ELEMENT, sr.next());
            _assertLookups(sr);
        } finally {
            sr.close();
        }
    }

    private void _assertLookups(AsyncXMLStreamReader<?> sr)
    {
        assertEquals(3, sr.getAttributeCount());

        // null nsURI: ignore namespace, match by local name only
        assertEquals("1", sr.getAttributeValue(null, "x"));
        assertEquals("2", sr.getAttributeValue(null, "y"));
        assertEquals("3", sr.getAttributeValue(null, "z"));
        assertNull(sr.getAttributeValue(null, "no-such-attr"));

        // explicit URI
        assertEquals("1", sr.getAttributeValue("http://foo", "x"));
        assertNull(sr.getAttributeValue("http://bar", "x"));

        // "" = no namespace
        assertEquals("3", sr.getAttributeValue("", "z"));
        assertNull(sr.getAttributeValue("", "x"));
    }
}
