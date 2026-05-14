package io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.aalto.io.UTF8Writer;
import com.fasterxml.aalto.out.WriterConfig;

public class TestUTF8Writer
    extends base.BaseTestCase
{
    public void testAsciiCharArray() throws IOException
    {
        // Single-char call dispatches to write(int) — different code path.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(new WriterConfig(), bytes, true);
        w.write(new char[] { 'x' });
        w.close();
        assertEquals("x", new String(bytes.toByteArray(), "UTF-8"));

        bytes = new ByteArrayOutputStream();
        w = new UTF8Writer(new WriterConfig(), bytes, true);
        w.write(new char[] { 'a', 'b', 'c' }, 0, 3);
        w.close();
        assertEquals("abc", new String(bytes.toByteArray(), "UTF-8"));

        // Zero-length is a no-op.
        bytes = new ByteArrayOutputStream();
        w = new UTF8Writer(new WriterConfig(), bytes, true);
        w.write(new char[] { 'a' }, 0, 0);
        w.close();
        assertEquals(0, bytes.size());
    }

    public void testWriteIntAscii() throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(new WriterConfig(), bytes, true);
        for (char c : "hello".toCharArray()) {
            w.write(c);
        }
        w.flush();
        assertEquals("hello", new String(bytes.toByteArray(), "UTF-8"));
        w.close();
    }

    public void testWriteIntMultiByte() throws IOException
    {
        // Cover 2-byte (Latin), 3-byte (BMP), and 4-byte (supplementary plane) paths via write(int).
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(new WriterConfig(), bytes, true);
        w.write(0x00E4);            // 2 bytes: ä
        w.write(0x4E2D);            // 3 bytes: 中
        // Supplementary plane (U+1F600 😀) requires writing the surrogate pair as ints.
        w.write(0xD83D);
        w.write(0xDE00);
        w.close();
        String s = new String(bytes.toByteArray(), "UTF-8");
        assertEquals("\u00E4\u4E2D\uD83D\uDE00", s);
    }

    public void testStringWriteAll() throws IOException
    {
        // Mix ascii, 2-byte, 3-byte and surrogate pair in a single String write.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(new WriterConfig(), bytes, true);
        String input = "Hi! \u00E4\u4E2D\uD83D\uDE00 end";
        w.write(input);
        w.close();
        assertEquals(input, new String(bytes.toByteArray(), "UTF-8"));
    }

    public void testStringWriteSingleChar() throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(new WriterConfig(), bytes, true);
        w.write("Q", 0, 1);
        w.close();
        assertEquals("Q", new String(bytes.toByteArray(), "UTF-8"));
    }

    public void testStringWriteOffsetLen() throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(new WriterConfig(), bytes, true);
        w.write("xxxabcyyy", 3, 3);
        w.close();
        assertEquals("abc", new String(bytes.toByteArray(), "UTF-8"));

        // Zero-length is a no-op.
        bytes = new ByteArrayOutputStream();
        w = new UTF8Writer(new WriterConfig(), bytes, true);
        w.write("ignored", 0, 0);
        w.close();
        assertEquals(0, bytes.size());
    }

    public void testCharArrayAllForms() throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(new WriterConfig(), bytes, true);
        char[] data = ("Hi! \u00E4\u4E2D\uD83D\uDE00 end").toCharArray();
        w.write(data, 0, data.length);
        w.close();
        assertEquals(new String(data), new String(bytes.toByteArray(), "UTF-8"));
    }

    public void testBufferFlush() throws IOException
    {
        // Buffer is sized 4000; force several buffer flushes to exercise the
        // wrap-around branch inside the main output loop.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(new WriterConfig(), bytes, true);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; ++i) {
            sb.append('A' + (i % 26));
            sb.append('\u00E4');
        }
        String s = sb.toString();
        w.write(s);
        w.close();
        assertEquals(s, new String(bytes.toByteArray(), "UTF-8"));
    }

    public void testSplitSurrogateAcrossCharArrayCalls() throws IOException
    {
        // First call ends on the high surrogate (it must be buffered);
        // second call supplies the low surrogate. Both write() variants need this exercised.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(new WriterConfig(), bytes, true);
        w.write(new char[] { 'a', '\uD83D' }, 0, 2);
        w.write(new char[] { '\uDE00', 'b' }, 0, 2);
        w.close();
        assertEquals("a\uD83D\uDE00b", new String(bytes.toByteArray(), "UTF-8"));
    }

    public void testSplitSurrogateAcrossStringCalls() throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(new WriterConfig(), bytes, true);
        w.write("a\uD83D");
        w.write("\uDE00b");
        w.close();
        assertEquals("a\uD83D\uDE00b", new String(bytes.toByteArray(), "UTF-8"));
    }

    public void testFailOnSecondHalfWithoutFirst() throws IOException
    {
        UTF8Writer w = new UTF8Writer(new WriterConfig(), new ByteArrayOutputStream(), true);
        try {
            w.write(0xDC00);
            fail("Expected IOException for unmatched second surrogate");
        } catch (IOException e) {
            // expected
        }
    }

    public void testFailOnDanglingHighSurrogateAtClose() throws IOException
    {
        UTF8Writer w = new UTF8Writer(new WriterConfig(), new ByteArrayOutputStream(), true);
        w.write(0xD800); // first half, buffered
        try {
            w.close();
            fail("Expected IOException for unmatched first surrogate at close");
        } catch (IOException e) {
            // expected
        }
    }

    public void testFailOnBrokenSurrogatePair() throws IOException
    {
        UTF8Writer w = new UTF8Writer(new WriterConfig(), new ByteArrayOutputStream(), true);
        w.write(0xD800); // first half
        try {
            w.write('A'); // not a low surrogate — convertSurrogate must reject
            fail("Expected IOException for broken surrogate pair");
        } catch (IOException e) {
            verifyMessage(e, "Broken surrogate pair");
        }
    }

    public void testDoesNotCloseUnderlyingWhenAutocloseDisabled() throws IOException
    {
        final boolean[] closed = { false };
        OutputStream out = new OutputStream() {
            @Override public void write(int b) {}
            @Override public void write(byte[] b, int off, int len) {}
            @Override public void close() { closed[0] = true; }
        };
        UTF8Writer w = new UTF8Writer(new WriterConfig(), out, false);
        w.write("x");
        w.close();
        assertFalse("underlying stream must not be closed when autoclose=false", closed[0]);
    }

    public void testClosesUnderlyingWhenAutocloseEnabled() throws IOException
    {
        final boolean[] closed = { false };
        OutputStream out = new OutputStream() {
            @Override public void write(int b) {}
            @Override public void write(byte[] b, int off, int len) {}
            @Override public void close() { closed[0] = true; }
        };
        UTF8Writer w = new UTF8Writer(new WriterConfig(), out, true);
        w.write("x");
        w.close();
        assertTrue("underlying stream must be closed when autoclose=true", closed[0]);
    }

    public void testDoubleCloseIsNoop() throws IOException
    {
        UTF8Writer w = new UTF8Writer(new WriterConfig(), new ByteArrayOutputStream(), true);
        w.write("data");
        w.close();
        w.close(); // must not throw
    }

    private void verifyMessage(Throwable t, String sub) {
        String msg = t.getMessage();
        if (msg == null || msg.indexOf(sub) < 0) {
            fail("Expected exception message containing '" + sub + "', got: " + msg);
        }
    }
}
