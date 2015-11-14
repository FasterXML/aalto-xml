package async;

import javax.xml.stream.XMLStreamConstants;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestCDataParsing extends AsyncTestBase
{
    public void testCDataParse() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testCData(1, SPC);
            _testCData(2, SPC);
            _testCData(3, SPC);
            _testCData(5, SPC);
            _testCData(11, SPC);
            _testCData(999, SPC);
        }
    }

    public void testCDataSkip() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testCDataSkip(1, SPC);
            _testCDataSkip(2, SPC);
            _testCDataSkip(3, SPC);
            _testCDataSkip(5, SPC);
            _testCDataSkip(11, SPC);
            _testCDataSkip(999, SPC);
        }
    }

    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */

    private final static String XML = "<root><![CDATA[cdata\r\n&] ]] stuff]]>...<![CDATA[this\r\r and Unicode: "+UNICODE_SEGMENT+"!]]></root>";

    private void _testCData(final int chunkSize, final String SPC) throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, SPC + XML);
            _testCData(sr_array, reader_array);
        } finally {
            if(sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = f.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, SPC + XML);
            _testCData(sr_buffer, reader_buffer);
        } finally {
            if(sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testCData(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(0, sr.getAttributeCount());

        // note: moved to next element by now, so:
        assertTokenType(CDATA, reader.nextToken());
        String str = collectAsyncText(reader, CDATA); // moves to end-element
        assertEquals("cdata\n&] ]] stuff", str);

        assertTokenType(XMLStreamConstants.CHARACTERS, reader.currentToken());
        str = collectAsyncText(reader, CHARACTERS);
        assertEquals("...", str);

        assertTokenType(XMLStreamConstants.CDATA, reader.currentToken());
        str = collectAsyncText(reader, CDATA);
        assertEquals("this\n\n and Unicode: "+UNICODE_SEGMENT+"!", str);
        
        assertTokenType(XMLStreamConstants.END_ELEMENT, reader.currentToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());

        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        
        assertFalse(sr.hasNext());
    }

    private void _testCDataSkip(final int chunkSize, final String SPC) throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, SPC + XML);
            _testCDataSkip(sr_array, reader_array);
        } finally {
            if(sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = f.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, SPC + XML);
            _testCDataSkip(sr_buffer, reader_buffer);
        } finally {
            if(sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testCDataSkip(AsyncXMLStreamReader<?> sr, AsyncReaderWrapper reader) throws Exception
    {
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(0, sr.getAttributeCount());

        // note: moved to next element by now, so:
        assertTokenType(CDATA, reader.nextToken());
        assertTokenType(XMLStreamConstants.CHARACTERS, reader.nextToken());
        assertTokenType(XMLStreamConstants.CDATA, reader.nextToken());
        assertTokenType(XMLStreamConstants.END_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        
        assertFalse(sr.hasNext());
    }
}
