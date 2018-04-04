package async;

import javax.xml.stream.XMLStreamConstants;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestTextEncoding extends AsyncTestBase
{
    final String SIMPLE_LATIN1_TEXT = "value:["+UNICODE_2BYTES+"/"+UNICODE_2BYTES+"]";

    public void testLatin1ByteArray() throws Exception {
        _testLatin1(false, 1);
        _testLatin1(false, 3);
        _testLatin1(false, 8);
    }

    public void testLatin1ByteBuffer() throws Exception {
        _testLatin1(true, 1);
        _testLatin1(true, 3);
        _testLatin1(true, 8);
    }
    
    public void _testLatin1(boolean byteBuffer, int chunkSize) throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        _testLatin1(chunkSize, byteBuffer, "");
        _testLatin1(chunkSize, byteBuffer, " ");
        _testLatin1(chunkSize, byteBuffer, "  ");
    }

    private void _testLatin1(final int chunkSize, boolean byteBuffer,
            final String SPC) throws Exception
    {
        final byte[] XML_BYTES;

        {
            // note: can NOT prefix with space, must be after initial <?xml
            final String XML = "<?xml version='1.0' "+SPC+" encoding='ISO-8859-1'?><root>"+SIMPLE_LATIN1_TEXT+"</root>";
            final byte[] XML_LATIN1 = XML.getBytes("iso-8859-1");
            final byte[] XML_UTF8 = XML.getBytes("utf-8");
    
            if (XML_LATIN1.length == XML_UTF8.length) {
                fail("Internal problem: latin-1 length same as UTF-8, should not be: "+XML_LATIN1.length);
            }
            XML_BYTES = XML_LATIN1;
        }

        final AsyncXMLInputFactory f = new InputFactoryImpl();

        if (byteBuffer) {
            AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = f.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer,
                    chunkSize, XML_BYTES);
            _testLatin1(sr_buffer, reader_buffer);
            sr_buffer.close();
        } else {
            AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array,
                    chunkSize, XML_BYTES);
            _testLatin1(sr_array, reader_array);
            sr_array.close();
        }
    }

    private void _testLatin1(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader)
        throws Exception
    {
        assertTokenType(START_ELEMENT, verifyStart(reader));
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());

        assertTokenType(CHARACTERS, reader.nextToken());
        String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
        assertEquals(SIMPLE_LATIN1_TEXT, str);

        assertTokenType(END_ELEMENT, reader.currentToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
}
