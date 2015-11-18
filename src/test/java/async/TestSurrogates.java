package async;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestSurrogates extends AsyncTestBase
{
    private final int HIGH_CODEPOINT = 0x1031c;
    private final String SURROGATE = new StringBuilder().appendCodePoint(HIGH_CODEPOINT).toString();
    private final String VALUE = "a/"+SURROGATE+"/b";
    private final String DOC = "<value>"+VALUE+"</value>";

    public void testCdataWithSurrogate() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testWithSurrogate(SPC, 1);
            _testWithSurrogate(SPC, 2);
            _testWithSurrogate(SPC, 3);
            _testWithSurrogate(SPC, 5);
            _testWithSurrogate(SPC, 9);
            _testWithSurrogate(SPC, 999);
        }
    }

    public void testSkipWithSurrogate() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testSkipWithSurrogate(SPC, 1);
            _testSkipWithSurrogate(SPC, 2);
            _testSkipWithSurrogate(SPC, 3);
            _testSkipWithSurrogate(SPC, 5);
            _testSkipWithSurrogate(SPC, 9);
            _testSkipWithSurrogate(SPC, 999);
        }
    }
    
    private void _testWithSurrogate(final String spaces, final int chunkSize) throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, spaces + DOC);
            _testWithSurrogate(sr_array, reader_array);
        } finally {
            if (sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = f.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, spaces + DOC);
            _testWithSurrogate(sr_buffer, reader_buffer);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testWithSurrogate(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertEquals("value", sr.getLocalName());
        assertTokenType(CHARACTERS, reader.nextToken());
        String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
        assertEquals(VALUE, str);
        assertTokenType(END_ELEMENT, reader.currentToken());
        assertEquals("value", sr.getLocalName());
        assertTokenType(END_DOCUMENT, reader.nextToken());
    }

    private void _testSkipWithSurrogate(final String spaces, final int chunkSize) throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, spaces + DOC);
            _testSkipWithSurrogate(sr_array, reader_array);
        } finally {
            if (sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = f.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, spaces + DOC);
            _testSkipWithSurrogate(sr_buffer, reader_buffer);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testSkipWithSurrogate(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertTokenType(CHARACTERS, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertTokenType(END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
}
