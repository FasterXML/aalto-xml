package async;

import javax.xml.stream.XMLStreamConstants;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestCharactersParsing extends AsyncTestBase
{
    public void testLinefeeds() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            _testLinefeeds(1, true, spaces(spaces));
            _testLinefeeds(2, true, spaces(spaces));
            _testLinefeeds(3, true, spaces(spaces));
            _testLinefeeds(5, true, spaces(spaces));
            _testLinefeeds(8, true, spaces(spaces));
            _testLinefeeds(99, true, spaces(spaces));
        }
    }

    public void testSkipLinefeeds() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            _testLinefeeds(1, false, spaces(spaces));
            _testLinefeeds(2, false, spaces(spaces));
            _testLinefeeds(3, false, spaces(spaces));
            _testLinefeeds(5, false, spaces(spaces));
            _testLinefeeds(8, false, spaces(spaces));
            _testLinefeeds(99, false, spaces(spaces));
        }
    }

    public void testTextWithEntities() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            _testTextWithEntities(1, true, spaces(spaces));
            _testTextWithEntities(2, true, spaces(spaces));
            _testTextWithEntities(3, true, spaces(spaces));
            _testTextWithEntities(5, true, spaces(spaces));
            _testTextWithEntities(11, true, spaces(spaces));
            _testTextWithEntities(999, true, spaces(spaces));
        }
    }

    public void testSkipTextWithEntities() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            _testTextWithEntities(1, false, spaces(spaces));
            _testTextWithEntities(2, false, spaces(spaces));
            _testTextWithEntities(3, false, spaces(spaces));
            _testTextWithEntities(5, false, spaces(spaces));
            _testTextWithEntities(11, false, spaces(spaces));
            _testTextWithEntities(999, false, spaces(spaces));
        }
    }
    
    public void testTextWithNumericEntities() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            _testTextWithNumericEntities(1, true, spaces(spaces));
            _testTextWithNumericEntities(2, true, spaces(spaces));
            _testTextWithNumericEntities(3, true, spaces(spaces));
            _testTextWithNumericEntities(5, true, spaces(spaces));
            _testTextWithNumericEntities(9, true, spaces(spaces));
            _testTextWithNumericEntities(99, true, spaces(spaces));
        }
    }
    
    public void testSkipTextWithNumericEntities() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            _testTextWithNumericEntities(1, false, spaces(spaces));
            _testTextWithNumericEntities(2, false, spaces(spaces));
            _testTextWithNumericEntities(3, false, spaces(spaces));
            _testTextWithNumericEntities(5, false, spaces(spaces));
            _testTextWithNumericEntities(9, false, spaces(spaces));
            _testTextWithNumericEntities(99, false, spaces(spaces));
        }
    }
    
    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */

    private void _testLinefeeds(final int chunkSize, final boolean checkValues, final String SPC) throws Exception
    {
        final String XML = SPC + "<root>\rFirst\r\nSecond\nThird: " + UNICODE_SEGMENT + "</root>";

        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = f.createAsyncForByteArray();
        final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
        _testLinefeeds(sr_array, reader_array, checkValues);
        sr_array.close();

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = f.createAsyncForByteBuffer();
        final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
        _testLinefeeds(sr_buffer, reader_buffer, checkValues);
        sr_buffer.close();
    }

    private void _testLinefeeds(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader,
            final boolean checkValues) throws Exception
    {
        assertTokenType(START_ELEMENT, verifyStart(reader));
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }

        assertTokenType(CHARACTERS, reader.nextToken());
        if (checkValues) {
            String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
            assertEquals("\nFirst\nSecond\nThird: "+UNICODE_SEGMENT, str);
        } else {
            reader.nextToken();
        }

        assertTokenType(END_ELEMENT, reader.currentToken());
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }

    private void _testTextWithEntities(final int chunkSize, final boolean checkValues, final String SPC) throws Exception
    {
        _testTextWithEntities(chunkSize, checkValues, SPC, "&lt", "<");
        _testTextWithEntities(chunkSize, checkValues, SPC, "&gt", ">");
        _testTextWithEntities(chunkSize, checkValues, SPC, "&apos", "'");
        // for [aalto-xml#78]
        _testTextWithEntities(chunkSize, checkValues, SPC, "&quot", "\"");
    }

    private void _testTextWithEntities(final int chunkSize, final boolean checkValues, final String SPC,
            final String entity, final String entityExpanded) throws Exception
    {
        final String XML = SPC + "<root>a"+entity+";b\rMOT</root>";

        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testTextWithEntities(sr_array, reader_array, checkValues, entityExpanded);
        } finally {
            if (sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = f.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
            _testTextWithEntities(sr_buffer, reader_buffer, checkValues, entityExpanded);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testTextWithEntities(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader,
            final boolean checkValues,
            final String entityExpanded) throws Exception
    {
        // should start with START_DOCUMENT, but for now skip
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(CHARACTERS, reader.nextToken());
        if (checkValues) {
            String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
            assertEquals("a"+entityExpanded+"b\nMOT", str);
        } else {
            reader.nextToken();
        }
        assertTokenType(END_ELEMENT, reader.currentToken());
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }

    private void _testTextWithNumericEntities(final int chunkSize, final boolean checkValues, final String SPC) throws Exception
    {
        final String XML = SPC + "<root>&#60;tag&#x3e;!</root>";

        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testTextWithNumericEntities(sr_array, reader_array, checkValues);
        } finally {
            if (sr_array != null) {
                sr_array.close();
            }
        }

        //test for byte buffer
        AsyncXMLStreamReader<AsyncByteBufferFeeder> sr_buffer = null;
        try {
            sr_buffer = f.createAsyncForByteBuffer();
            final AsyncReaderWrapperForByteBuffer reader_buffer = new AsyncReaderWrapperForByteBuffer(sr_buffer, chunkSize, XML);
            _testTextWithNumericEntities(sr_buffer, reader_buffer, checkValues);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testTextWithNumericEntities(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader, final boolean checkValues) throws Exception
    {
        // should start with START_DOCUMENT, but for now skip
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(CHARACTERS, reader.nextToken());
        if (checkValues) {
            String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
            assertEquals("<tag>!", str);
        } else {
            reader.nextToken();
        }
        assertTokenType(END_ELEMENT, reader.currentToken());
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
}
