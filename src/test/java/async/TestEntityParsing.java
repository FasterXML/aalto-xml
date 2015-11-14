package async;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestEntityParsing extends AsyncTestBase
{
    public void testEntityParsing() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testEntity(1, true, SPC);
            _testEntity(2, true, SPC);
            _testEntity(3, true, SPC);
            _testEntity(5, true, SPC);
            _testEntity(11, true, SPC);
            _testEntity(999, true, SPC);
        }
    }

    public void testEntitySkipping() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testEntity(1, true, SPC);
            _testEntity(2, true, SPC);
            _testEntity(3, true, SPC);
            _testEntity(5, true, SPC);
            _testEntity(11, true, SPC);
            _testEntity(999, true, SPC);
        }
    }
    
    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */

    private void _testEntity(final int chunkSize, final boolean checkValues, final String spaces) throws Exception
    {
        final String XML = spaces + "<root>&entity1;Some text&entity2;!<leaf>...&leafEntity;</leaf>&last;</root>";

        final AsyncXMLInputFactory f = new InputFactoryImpl();
        // important must not require expansion of general entities
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testEntity(sr_array, reader_array, checkValues);
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
            _testEntity(sr_buffer, reader_buffer, checkValues);
        } finally {
            if(sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    final void _testEntity(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader, final boolean checkValues) throws Exception
    {
        // should start with START_DOCUMENT, but for now skip
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
        }
        assertTokenType(ENTITY_REFERENCE, reader.nextToken());
        if (checkValues) {
            assertEquals("entity1", sr.getLocalName());
        }
        assertTokenType(CHARACTERS, reader.nextToken());
        if (checkValues) {
            String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
            assertEquals("Some text", str);
        } else {
            reader.nextToken();
        }
        assertTokenType(ENTITY_REFERENCE, reader.currentToken());
        // Here we better verify name, either way
        assertEquals("entity2", sr.getLocalName());

        assertTokenType(CHARACTERS, reader.nextToken());
        if (checkValues) {
            String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
            assertEquals("!", str);
        } else {
            reader.nextToken();
        }
        
        assertTokenType(START_ELEMENT, reader.currentToken());
        if (checkValues) {
            assertEquals("leaf", sr.getLocalName());
        }
        assertTokenType(CHARACTERS, reader.nextToken());
        if (checkValues) {
            String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
            assertEquals("...", str);
        } else {
            reader.nextToken();
        }
        assertTokenType(ENTITY_REFERENCE, reader.currentToken());
        assertEquals("leafEntity", sr.getLocalName());

        assertTokenType(END_ELEMENT, reader.nextToken());
        if (checkValues) {
            assertEquals("leaf", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(ENTITY_REFERENCE, reader.nextToken());
        assertEquals("last", sr.getLocalName());
        
        assertTokenType(END_ELEMENT, reader.nextToken());
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
}
