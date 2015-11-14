package async;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.fasterxml.aalto.util.IllegalCharHandler;

import java.nio.ByteBuffer;

public class TestElementParsing extends AsyncTestBase
{
    private final AsyncXMLInputFactory ASYNC_F = new InputFactoryImpl();

    /**
     * Trivial test to verify basic operation with a full buffer.
     */
    public void testTrivial_array() throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = null;
        try {
            sr = f.createAsyncFor("<root>a</root>".getBytes("UTF-8"));

            assertTokenType(START_DOCUMENT, sr.next());
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("root", sr.getLocalName());
            assertTokenType(CHARACTERS, sr.next());
            assertEquals("a", sr.getText());
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("root", sr.getLocalName());
            // no input to see (could still get a PI, comment etc), so
            assertTokenType(AsyncXMLStreamReader.EVENT_INCOMPLETE, sr.next());
            sr.getInputFeeder().endOfInput();

            assertTokenType(END_DOCUMENT, sr.next());
        } finally {
            if(sr != null) {
                sr.close();
            }
        }
    }

    /**
     * Trivial test to verify basic operation with a full buffer.
     */
    public void testTrivial_buffer() throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr = null;
        try {
            sr = f.createAsyncFor(ByteBuffer.wrap("<root>a</root>".getBytes("UTF-8")));

            assertTokenType(START_DOCUMENT, sr.next());
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("root", sr.getLocalName());
            assertTokenType(CHARACTERS, sr.next());
            assertEquals("a", sr.getText());
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("root", sr.getLocalName());
            // no input to see (could still get a PI, comment etc), so
            assertTokenType(AsyncXMLStreamReader.EVENT_INCOMPLETE, sr.next());
            sr.getInputFeeder().endOfInput();

            assertTokenType(END_DOCUMENT, sr.next());
        } finally {
            if(sr != null) {
                sr.close();
            }
        }
    }
    
    public void testRootElement() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testEmptyRoot(1, SPC+"<root />");
            _testEmptyRoot(1, SPC+"<root/>");
            _testEmptyRoot(1, SPC+"<root></root>");
            _testEmptyRoot(2, SPC+"<root />");
            _testEmptyRoot(2, SPC+"<root/>");
            _testEmptyRoot(2, SPC+"<root></root>");
            _testEmptyRoot(3, SPC+"<root />");
            _testEmptyRoot(3, SPC+"<root/>");
            _testEmptyRoot(3, SPC+"<root></root>");
            _testEmptyRoot(5, SPC+"<root />");
            _testEmptyRoot(5, SPC+"<root/>");
            _testEmptyRoot(5, SPC+"<root></root>");
            _testEmptyRoot(8, SPC+"<root />");
            _testEmptyRoot(8, SPC+"<root/>");
            _testEmptyRoot(8, SPC+"<root></root>");
        }
    }

    public void testElements() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testElements(1, SPC);
            _testElements(2, SPC);
            _testElements(3, SPC);
            _testElements(5, SPC);
            _testElements(8, SPC);
            _testElements(15, SPC);
        }
    }

    // Bit more stuff with attributes
    public void testParseElementsWithAttrs() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testElementsWithAttrs(1, true, SPC);
            _testElementsWithAttrs(2, true, SPC);
            _testElementsWithAttrs(3, true, SPC);
            _testElementsWithAttrs(5, true, SPC);
            _testElementsWithAttrs(8, true, SPC);
            _testElementsWithAttrs(15, true, SPC);
            _testElementsWithAttrs(999, true, SPC);
        }
    }

    public void testSkipElementsWithAttrs() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testElementsWithAttrs(1, false, SPC);
            _testElementsWithAttrs(2, false, SPC);
            _testElementsWithAttrs(3, false, SPC);
            _testElementsWithAttrs(5, false, SPC);
            _testElementsWithAttrs(8, false, SPC);
            _testElementsWithAttrs(15, false, SPC);
            _testElementsWithAttrs(999, false, SPC);
        }
    }

    // [Issue-12], probs with attrs, multi-byte UTF-8 chars
    public void testParseElementsWithUTF8Attrs() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testElementsWithUTF8Attrs(1, true, SPC);
            _testElementsWithUTF8Attrs(2, true, SPC);
            _testElementsWithUTF8Attrs(5, true, SPC);
            _testElementsWithAttrs(999, true, SPC);
        }
    }

    // [Issue-12]
    public void testSkipElementsWithUTF8Attrs() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testElementsWithUTF8Attrs(1, false, SPC);
            _testElementsWithUTF8Attrs(2, false, SPC);
            _testElementsWithUTF8Attrs(5, false, SPC);
            _testElementsWithAttrs(999, false, SPC);
        }
    }
    
    public void testParseElementsWithIllegalChars() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testElementsWithIllegalChars(1, true, SPC);
            _testElementsWithIllegalChars(2, true, SPC);
            _testElementsWithIllegalChars(5, true, SPC);
            _testElementsWithAttrs(999, true, SPC);
        }
    }

    // [#8]: give useful exception for `getElementText()`
    public void testGetElementText_array() throws Exception
    {
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = null;
        try {
            sr = ASYNC_F.createAsyncFor("<root>foo</r".getBytes("UTF-8"));

            assertTokenType(START_DOCUMENT, sr.next());
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("root", sr.getLocalName());

            try {
                sr.getElementText();
            } catch (XMLStreamException e) {
                verifyException(e, "Can not use text-aggregating methods");
            }
        } finally {
            if(sr != null) {
                sr.close();
            }
        }
    }

    // [#8]: give useful exception for `getElementText()`
    public void testGetElementText_buffer() throws Exception
    {
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr = null;
        try {
            sr = ASYNC_F.createAsyncFor(ByteBuffer.wrap("<root>foo</r".getBytes("UTF-8")));

            assertTokenType(START_DOCUMENT, sr.next());
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("root", sr.getLocalName());

            try {
                sr.getElementText();
            } catch (XMLStreamException e) {
                verifyException(e, "Can not use text-aggregating methods");
            }
        } finally {
            if(sr != null) {
                sr.close();
            }
        }
    }
    
    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */

    private void _testEmptyRoot(final int chunkSize, final String XML) throws Exception
    {
        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = ASYNC_F.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testEmptyRoot(sr_array, reader_array);
        } finally {
            if(sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = ASYNC_F.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
            _testEmptyRoot(sr_buffer, reader_buffer);
        } finally {
            if(sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testEmptyRoot(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        // should start with START_DOCUMENT, but for now skip
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(0, sr.getAttributeCount());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }

    private void _testElements(final int chunkSize, final String SPC) throws Exception
    {
//        final String XML = SPC+"<root attr='1&amp;2'><leaf xmlns='abc' a   ='3'\rb=''  /></root>";
        final String XML = SPC + "<root attr='1&amp;2'><leaf xmlns='abc' a   ='3'\rxmlns:foo='bar'  b=''  /></root>";

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = ASYNC_F.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testElements(sr_array, reader_array);
        } finally {
            if (sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = ASYNC_F.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
            _testElements(sr_buffer, reader_buffer);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testElements(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        // should start with START_DOCUMENT, but for now skip
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(1, sr.getAttributeCount());
        assertEquals("1&2", sr.getAttributeValue(0));
        assertEquals("attr", sr.getAttributeLocalName(0));
        assertEquals("", sr.getAttributeNamespace(0));
        
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("leaf", sr.getLocalName());
        assertEquals("abc", sr.getNamespaceURI());
        assertEquals(2, sr.getAttributeCount());
        assertEquals(2, sr.getNamespaceCount());

        assertEquals("a", sr.getAttributeLocalName(0));
        assertEquals("", sr.getAttributeNamespace(0));
        assertEquals("3", sr.getAttributeValue(0));
        assertEquals("b", sr.getAttributeLocalName(1));
        assertEquals("", sr.getAttributeNamespace(1));
        assertEquals("", sr.getAttributeValue(1));

        assertEquals("", sr.getNamespacePrefix(0));
        assertEquals("abc", sr.getNamespaceURI(0));
        assertEquals("foo", sr.getNamespacePrefix(1));
        assertEquals("bar", sr.getNamespaceURI(1));
        
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("leaf", sr.getLocalName());
        assertEquals("abc", sr.getNamespaceURI());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }

    private void _testElementsWithAttrs(final int chunkSize, final boolean checkValues, final String SPC) throws Exception
    {
//        final String XML = SPC+"<root attr='1&amp;2'><leaf xmlns='abc' a   ='3'\rb=''  /></root>";
        final String XML = SPC + "<root attr='1&#62;2, 2&#x3C;1' />";

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = ASYNC_F.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testElementsWithAttrs(sr_array, reader_array, checkValues);
        } finally {
            if (sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = ASYNC_F.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
            _testElementsWithAttrs(sr_buffer, reader_buffer, checkValues);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testElementsWithAttrs(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader, final boolean checkValues) throws Exception
    {
        // should start with START_DOCUMENT, but for now skip
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
            assertEquals(1, sr.getAttributeCount());
            assertEquals("1>2, 2<1", sr.getAttributeValue(0));
            assertEquals("attr", sr.getAttributeLocalName(0));
            assertEquals("", sr.getAttributeNamespace(0));
        }
        assertTokenType(END_ELEMENT, reader.nextToken());
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }

    private void _testElementsWithUTF8Attrs(final int chunkSize, final boolean checkValues, final String SPC) throws Exception
    {
        final String VALUE = "Gr\u00e4";
        final String XML = SPC + "<root attr='" + VALUE + "' />";

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = ASYNC_F.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testElementsWithUTF8Attrs(sr_array, reader_array, checkValues, VALUE);
        } finally {
            if (sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = ASYNC_F.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
            _testElementsWithUTF8Attrs(sr_buffer, reader_buffer, checkValues, VALUE);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testElementsWithUTF8Attrs(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader, final boolean checkValues, final String VALUE) throws Exception
    {
        // should start with START_DOCUMENT, but for now skip
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
            assertEquals(1, sr.getAttributeCount());
            assertEquals(VALUE, sr.getAttributeValue(0));
            assertEquals("attr", sr.getAttributeLocalName(0));
            assertEquals("", sr.getAttributeNamespace(0));
        }
        assertTokenType(END_ELEMENT, reader.nextToken());
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
    
    private void _testElementsWithIllegalChars(final int chunkSize, final boolean checkValues, final String SPC) throws Exception
    {
        char replaced = ' ';
        char illegal = 22;
        final String VALUE = "Gr" + illegal;
        final String VALUE_REPL = "Gr" + replaced;
        final String XML = SPC + "<root attr='" + VALUE + "' />";

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = ASYNC_F.createAsyncForByteArray();
            sr_array.getConfig().setIllegalCharHandler(new IllegalCharHandler.ReplacingIllegalCharHandler(replaced));
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testElementsWithIllegalChars(sr_array, reader_array, checkValues, VALUE_REPL);
        } finally {
            if (sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = ASYNC_F.createAsyncForByteBuffer();
            sr_buffer.getConfig().setIllegalCharHandler(new IllegalCharHandler.ReplacingIllegalCharHandler(replaced));
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
            _testElementsWithIllegalChars(sr_buffer, reader_buffer, checkValues, VALUE_REPL);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testElementsWithIllegalChars(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader, final boolean checkValues, final String VALUE_REPL) throws Exception
    {
        // should start with START_DOCUMENT, but for now skip
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
            assertEquals(1, sr.getAttributeCount());
            assertEquals(VALUE_REPL, sr.getAttributeValue(0));
            assertEquals("attr", sr.getAttributeLocalName(0));
            assertEquals("", sr.getAttributeNamespace(0));
        }
        assertTokenType(END_ELEMENT, reader.nextToken());
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
}
