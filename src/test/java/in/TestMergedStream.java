package in;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.aalto.in.MergedStream;
import com.fasterxml.aalto.in.ReaderConfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMergedStream
    extends base.BaseTestCase
{
    @Test
    public void testReadSingleByteFromPushback() throws IOException
    {
        byte[] pushed = "abc".getBytes("US-ASCII");
        InputStream tail = new ByteArrayInputStream("DEF".getBytes("US-ASCII"));
        MergedStream ms = new MergedStream(null, tail, pushed, 0, pushed.length);

        assertEquals('a', ms.read());
        assertEquals('b', ms.read());
        assertEquals('c', ms.read()); // exhausts pushback
        // Now reading falls through to underlying stream:
        assertEquals('D', ms.read());
        assertEquals('E', ms.read());
        assertEquals('F', ms.read());
        assertEquals(-1, ms.read());
        ms.close();
    }

    @Test
    public void testReadSubrangeOfPushback() throws IOException
    {
        // Start/end window in the pushback array — only middle bytes are visible.
        byte[] pushed = "XXabcXX".getBytes("US-ASCII");
        InputStream tail = new ByteArrayInputStream(new byte[0]);
        MergedStream ms = new MergedStream(null, tail, pushed, 2, 5);

        assertEquals('a', ms.read());
        assertEquals('b', ms.read());
        assertEquals('c', ms.read());
        assertEquals(-1, ms.read());
        ms.close();
    }

    @Test
    public void testReadBulkSpansPushbackAndTail() throws IOException
    {
        byte[] pushed = "abc".getBytes("US-ASCII");
        InputStream tail = new ByteArrayInputStream("DEFG".getBytes("US-ASCII"));
        MergedStream ms = new MergedStream(null, tail, pushed, 0, pushed.length);

        byte[] buf = new byte[10];
        // First bulk read sees only what's left in pushback.
        int n = ms.read(buf, 0, buf.length);
        assertEquals(3, n);
        assertEquals("abc", new String(buf, 0, n, "US-ASCII"));

        // Second bulk read goes straight to underlying stream.
        n = ms.read(buf);
        assertEquals(4, n);
        assertEquals("DEFG", new String(buf, 0, n, "US-ASCII"));

        // EOF.
        assertEquals(-1, ms.read(buf));
        ms.close();
    }

    @Test
    public void testReadBulkPartialPushback() throws IOException
    {
        // Caller requests fewer bytes than pushback has — should clamp len but not free buffer.
        byte[] pushed = "abcdef".getBytes("US-ASCII");
        InputStream tail = new ByteArrayInputStream("Z".getBytes("US-ASCII"));
        MergedStream ms = new MergedStream(null, tail, pushed, 0, pushed.length);

        byte[] buf = new byte[3];
        assertEquals(3, ms.read(buf));
        assertEquals("abc", new String(buf, 0, 3, "US-ASCII"));
        // Still in pushback after partial read.
        assertEquals(3, ms.available());

        // Drain remaining pushback, then tail.
        assertEquals(3, ms.read(buf));
        assertEquals("def", new String(buf, 0, 3, "US-ASCII"));
        assertEquals('Z', ms.read());
        ms.close();
    }

    @Test
    public void testAvailableReportsPushbackThenDelegates() throws IOException
    {
        byte[] pushed = "abc".getBytes("US-ASCII");
        InputStream tail = new ByteArrayInputStream("XY".getBytes("US-ASCII"));
        MergedStream ms = new MergedStream(null, tail, pushed, 0, pushed.length);

        assertEquals(3, ms.available());
        ms.read();
        assertEquals(2, ms.available());

        // Drain remaining pushback so available() delegates to tail's available().
        ms.read();
        ms.read();
        assertEquals(2, ms.available());
        ms.close();
    }

    @Test
    public void testSkipInsidePushback() throws IOException
    {
        byte[] pushed = "abcdef".getBytes("US-ASCII");
        InputStream tail = new ByteArrayInputStream("Z".getBytes("US-ASCII"));
        MergedStream ms = new MergedStream(null, tail, pushed, 0, pushed.length);

        // Skip stays entirely inside pushback.
        assertEquals(3L, ms.skip(3));
        assertEquals('d', ms.read());
        ms.close();
    }

    @Test
    public void testSkipSpansPushbackAndTail() throws IOException
    {
        byte[] pushed = "abc".getBytes("US-ASCII");
        // SkippableInputStream returns full skip count (unlike ByteArrayInputStream which clamps).
        InputStream tail = new ByteArrayInputStream("DEFGH".getBytes("US-ASCII"));
        MergedStream ms = new MergedStream(null, tail, pushed, 0, pushed.length);

        long skipped = ms.skip(5);
        // 3 from pushback + however many tail skips — both ByteArrayInputStream backed.
        assertTrue(skipped >= 3, "should skip at least 3 (the pushback)");
        ms.close();
    }

    @Test
    public void testMarkSupportedFalseDuringPushback() throws IOException
    {
        byte[] pushed = "abc".getBytes("US-ASCII");
        // Even though ByteArrayInputStream supports mark, MergedStream must report false
        // while pushback is still active (mark would not cover the pushback region).
        InputStream tail = new ByteArrayInputStream("Z".getBytes("US-ASCII"));
        MergedStream ms = new MergedStream(null, tail, pushed, 0, pushed.length);

        assertFalse(ms.markSupported());
        ms.mark(10); // must be a no-op, not throw
        ms.reset();  // no-op while pushback active

        // Drain pushback.
        ms.read(); ms.read(); ms.read();
        // Now markSupported delegates to tail (true for ByteArrayInputStream).
        assertTrue(ms.markSupported());
        ms.mark(10);
        assertEquals('Z', ms.read());
        ms.reset();
        assertEquals('Z', ms.read());
        ms.close();
    }

    @Test
    public void testCloseAlsoClosesUnderlying() throws IOException
    {
        final boolean[] closed = { false };
        InputStream tail = new InputStream() {
            @Override public int read() { return -1; }
            @Override public void close() { closed[0] = true; }
        };
        MergedStream ms = new MergedStream(null, tail, new byte[] { (byte) 'a' }, 0, 1);
        ms.close();
        assertTrue(closed[0], "underlying stream must be closed via MergedStream.close()");
    }

    @Test
    public void testWithReaderConfigBufferFreed() throws IOException
    {
        // Using a real ReaderConfig exercises the freeFullBBuffer branch on pushback exhaustion.
        ReaderConfig cfg = new ReaderConfig();
        byte[] pushed = cfg.allocFullBBuffer(64);
        pushed[0] = (byte) 'a';
        pushed[1] = (byte) 'b';
        InputStream tail = new ByteArrayInputStream("Z".getBytes("US-ASCII"));
        MergedStream ms = new MergedStream(cfg, tail, pushed, 0, 2);

        assertEquals('a', ms.read());
        assertEquals('b', ms.read()); // triggers freeFullBBuffer
        assertEquals('Z', ms.read());
        ms.close();
    }
}
