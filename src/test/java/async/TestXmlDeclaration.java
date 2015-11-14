package async;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import static com.fasterxml.aalto.AsyncXMLStreamReader.EVENT_INCOMPLETE;

public class TestXmlDeclaration extends AsyncTestBase
{
    private final int[] CHUNK_SIZES = new int[] { 1, 2, 3, 5, 9, 33 };

    public void testNoDeclaration() throws Exception
    {
        final AsyncXMLInputFactory f = new InputFactoryImpl();
        for (final String XML : new String[] { "   <root />", "<root/>" }) {
            for (final int chunkSize : CHUNK_SIZES) {
                //test for byte array
                AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
                try {
                    sr_array = f.createAsyncForByteArray();
                    final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
                    _testNoDeclaration(sr_array, reader_array);
                } finally {
                    if(sr_array != null) {
                        sr_array.close();
                    }
                }

                //test for byte buffer
                AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
                try {
                    sr_buffer = f.createAsyncForByteBuffer();
                    final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
                    _testNoDeclaration(sr_buffer, reader_buffer);
                } finally {
                    if(sr_buffer != null) {
                        sr_buffer.close();
                    }
                }
            }
        }
    }

    private void _testNoDeclaration(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        assertEquals(EVENT_INCOMPLETE, reader.currentToken());
        assertTokenType(START_DOCUMENT, reader.nextToken());
        // no info, however; except for encoding auto-detection
        assertNull(sr.getCharacterEncodingScheme());
        assertEquals("UTF-8", sr.getEncoding());
        assertNull(sr.getVersion());
        assertFalse(sr.standaloneSet());

        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
    }

    public void testVersionOnlyDeclaration() throws Exception
    {
        final String XML = "<?xml version='1.0' ?><root />";
        final AsyncXMLInputFactory f = new InputFactoryImpl();

        for (final int chunkSize : CHUNK_SIZES) {
            //test for byte array
            AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
            try {
                sr_array = f.createAsyncForByteArray();
                final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
                _testVersionOnlyDeclaration(sr_array, reader_array);
            } finally {
                if(sr_array != null) {
                    sr_array.close();
                }
            }

            //test for byte buffer
            AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
            try {
                sr_buffer = f.createAsyncForByteBuffer();
                final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
                _testVersionOnlyDeclaration(sr_buffer, reader_buffer);
            } finally {
                if(sr_buffer != null) {
                    sr_buffer.close();
                }
            }
        }
    }

    private void _testVersionOnlyDeclaration(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        assertEquals(EVENT_INCOMPLETE, reader.currentToken());
        assertTokenType(START_DOCUMENT, reader.nextToken());
        assertNull(sr.getCharacterEncodingScheme());
        assertEquals("UTF-8", sr.getEncoding());
        assertEquals("1.0", sr.getVersion());
        assertFalse(sr.standaloneSet());

        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
    }

    public void testEncodingDeclaration() throws Exception
    {
        final String XML = "<?xml version= \"1.0\"   encoding='UTF-8' ?><root/>";
        final AsyncXMLInputFactory f = new InputFactoryImpl();

        for (final int chunkSize : CHUNK_SIZES) {
            //test for byte array
            AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
            try {
                sr_array = f.createAsyncForByteArray();
                final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
                _testEncodingDeclaration(sr_array, reader_array);
            } finally {
                if(sr_array != null) {
                    sr_array.close();
                }
            }

            //test for byte buffer
            AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
            try {
                sr_buffer = f.createAsyncForByteBuffer();
                final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
                _testEncodingDeclaration(sr_buffer, reader_buffer);
            } finally {
                if(sr_buffer != null) {
                    sr_buffer.close();
                }
            }
        }
    }

    private void _testEncodingDeclaration(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        assertEquals(EVENT_INCOMPLETE, reader.currentToken());
        assertTokenType(START_DOCUMENT, reader.nextToken());
        assertEquals("UTF-8", sr.getEncoding());
        assertEquals("UTF-8", sr.getCharacterEncodingScheme());
        assertEquals("1.0", sr.getVersion());
        assertFalse(sr.standaloneSet());

        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
    }

    public void testStandAloneDeclaration() throws Exception
    {
        final String XML = "<?xml version  ='1.0' encoding=\"UTF-8\"  standalone='yes' ?>  <root />";
        final AsyncXMLInputFactory f = new InputFactoryImpl();

        for (final int chunkSize : CHUNK_SIZES) {
            //test for byte array
            AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
            try {
                sr_array = f.createAsyncForByteArray();
                final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
                _testStandAloneDeclaration(sr_array, reader_array);
            } finally {
                if(sr_array != null) {
                    sr_array.close();
                }
            }

            //test for byte buffer
            AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
            try {
                sr_buffer = f.createAsyncForByteBuffer();
                final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
                _testStandAloneDeclaration(sr_buffer, reader_buffer);
            } finally {
                if(sr_buffer != null) {
                    sr_buffer.close();
                }
            }
        }
    }

    private void _testStandAloneDeclaration(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        assertEquals(EVENT_INCOMPLETE, reader.currentToken());
        assertTokenType(START_DOCUMENT, reader.nextToken());
        assertEquals("UTF-8", sr.getEncoding());
        assertEquals("UTF-8", sr.getCharacterEncodingScheme());
        assertEquals("1.0", sr.getVersion());
        assertTrue(sr.standaloneSet());
        assertTrue(sr.isStandalone());

        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
    }

    public void testStandAloneDeclaration2() throws Exception
    {
        final String XML = "<?xml version=\"1.0\" standalone='yes'?>\n<root/>";
        final AsyncXMLInputFactory f = new InputFactoryImpl();

        for (final int chunkSize : CHUNK_SIZES) {
            //test for byte array
            AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
            try {
                sr_array = f.createAsyncForByteArray();
                final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
                _testStandAloneDeclaration2(sr_array, reader_array);
            } finally {
                if(sr_array != null) {
                    sr_array.close();
                }
            }

            //test for byte buffer
            AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
            try {
                sr_buffer = f.createAsyncForByteBuffer();
                final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
                _testStandAloneDeclaration2(sr_buffer, reader_buffer);
            } finally {
                if(sr_buffer != null) {
                    sr_buffer.close();
                }
            }
        }
    }

    private void _testStandAloneDeclaration2(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        assertEquals(EVENT_INCOMPLETE, reader.currentToken());
        assertTokenType(START_DOCUMENT, reader.nextToken());
        assertEquals("UTF-8", sr.getEncoding());
        assertNull(sr.getCharacterEncodingScheme());
        assertEquals("1.0", sr.getVersion());
        assertTrue(sr.standaloneSet());
        assertTrue(sr.isStandalone());

        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
    }
}
