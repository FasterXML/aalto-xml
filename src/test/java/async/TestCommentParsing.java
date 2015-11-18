package async;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestCommentParsing extends AsyncTestBase
{
    public void testParseComments() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testComments(SPC, 1);
            _testComments(SPC, 2);
            _testComments(SPC, 3);
            _testComments(SPC, 5);
            _testComments(SPC, 9);
            _testComments(SPC, 999);
        }
    }

    public void testSkipComments() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testSkipComments(SPC, 1);
            _testSkipComments(SPC, 2);
            _testSkipComments(SPC, 3);
            _testSkipComments(SPC, 5);
            _testSkipComments(SPC, 9);
            _testSkipComments(SPC, 999);
        }
    }
    
    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */

    private final static String XML = "<!--comments&s\r\ntuf-fy>--><root><!----></root><!--\nHi - "+UNICODE_SEGMENT+" - ho->-->";
    
    private void _testComments(final String spaces, final int chunkSize) throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, spaces + XML);
            _testComments(sr_array, reader_array);
        } finally {
            if (sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = f.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, spaces + XML);
            _testComments(sr_buffer, reader_buffer);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testComments(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        int t = verifyStart(reader);
        assertTokenType(COMMENT, t);
        assertEquals("comments&s\ntuf-fy>", sr.getText());
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertTokenType(COMMENT, reader.nextToken());
        assertEquals("", sr.getText());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertTokenType(COMMENT, reader.nextToken());
        assertEquals("\nHi - "+UNICODE_SEGMENT+" - ho->", sr.getText());
        assertTokenType(END_DOCUMENT, reader.nextToken());
    }

    private void _testSkipComments(final String spaces, final int chunkSize) throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, spaces + XML);
            _testSkipComments(sr_array, reader_array);
        } finally {
            if (sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = f.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, spaces + XML);
            _testSkipComments(sr_buffer, reader_buffer);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testSkipComments(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        int t = verifyStart(reader);
        assertTokenType(COMMENT, t);
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertTokenType(COMMENT, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertTokenType(COMMENT, reader.nextToken());
        assertTokenType(END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
}
