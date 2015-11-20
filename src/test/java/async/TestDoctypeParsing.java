package async;

import javax.xml.stream.XMLStreamException;

//import async.AsyncTestBase.AsyncReaderWrapper;


import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestDoctypeParsing extends AsyncTestBase
{
    public void testSimplest() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testSimplest(SPC, 1);
            _testSimplest(SPC, 2);
            _testSimplest(SPC, 3);
            _testSimplest(SPC, 5);
            _testSimplest(SPC, 11);
            _testSimplest(SPC, 1000);
        }
    }

    public void testWithSystemId() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);            
            _testWithIds(SPC, 1);
            _testWithIds(SPC, 2);
            _testWithIds(SPC, 3);
            _testWithIds(SPC, 6);
            _testWithIds(SPC, 900);
        }
    }

    public void testParseFull() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testFull(SPC, true, 1);
            _testFull(SPC, true, 2);
            _testFull(SPC, true, 3);
            _testFull(SPC, true, 6);
            _testFull(SPC, true, 900);
        }
    }

    public void testSkipFull() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testFull(SPC, false, 1);
            _testFull(SPC, false, 2);
            _testFull(SPC, false, 3);
            _testFull(SPC, false, 6);
            _testFull(SPC, false, 900);
        }
    }
    
    public void testInvalidDup() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testInvalidDup(SPC, 1);
            _testInvalidDup(SPC, 2);
            _testInvalidDup(SPC, 3);
            _testInvalidDup(SPC, 6);
            _testInvalidDup(SPC, 900);
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */
    
    private void _testSimplest(final String spaces, final int chunkSize) throws Exception
    {
        final String XML = spaces + "<!DOCTYPE root>  <root />";

        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testSimplest(sr_array, reader_array);
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
            _testSimplest(sr_buffer, reader_buffer);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testSimplest(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        int t = verifyStart(reader);
        assertTokenType(DTD, t);
        // as per Stax API, can't call getLocalName (ugh), but Stax2 gives us this:
        assertEquals("root", sr.getPrefixedName());
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
    }

    private void _testWithIds(final String spaces, final int chunkSize) throws Exception
    {
        final String PUBLIC_ID = "-//OASIS//DTD DITA Topic//EN";
        final String SYSTEM_ID = "file:/topic.dtd";
        final String XML = spaces + "<!DOCTYPE root PUBLIC '" + PUBLIC_ID + "' \"" + SYSTEM_ID + "\"><root/>";

        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testWithIds(sr_array, reader_array, PUBLIC_ID, SYSTEM_ID);
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
            _testWithIds(sr_buffer, reader_buffer, PUBLIC_ID, SYSTEM_ID);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testWithIds(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader, final String PUBLIC_ID, final String SYSTEM_ID) throws Exception
    {
        int t = verifyStart(reader);
        assertTokenType(DTD, t);
        assertTokenType(DTD, sr.getEventType());
        assertEquals("root", sr.getPrefixedName());
        assertEquals(PUBLIC_ID, sr.getDTDInfo().getDTDPublicId());
        assertEquals(SYSTEM_ID, sr.getDTDInfo().getDTDSystemId());

        assertTokenType(START_ELEMENT, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
    }

    private void _testFull(final String spaces, final boolean checkValue, final int chunkSize) throws Exception
    {
        final String SYSTEM_ID = "file:/something";
        final String INTERNAL_SUBSET = "<!--My dtd-->\n"
                + "<!ELEMENT html (head, body)>"
                + "<!ATTLIST head title CDATA #IMPLIED>";
        String XML = spaces + "<!DOCTYPE root SYSTEM '" + SYSTEM_ID + "' [" + INTERNAL_SUBSET + "]>\n<root/>";

        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testFull(sr_array, reader_array, checkValue, SYSTEM_ID, INTERNAL_SUBSET);
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
            _testFull(sr_buffer, reader_buffer, checkValue, SYSTEM_ID, INTERNAL_SUBSET);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testFull(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader, final boolean checkValue, final String SYSTEM_ID, final String INTERNAL_SUBSET) throws Exception
    {
        int t = verifyStart(reader);
        assertTokenType(DTD, t);
        if (checkValue) {
            assertNull(sr.getDTDInfo().getDTDPublicId());
            assertEquals(SYSTEM_ID, sr.getDTDInfo().getDTDSystemId());
            assertEquals("root", sr.getPrefixedName());
            final String subset = sr.getText();
            assertEquals(INTERNAL_SUBSET, subset);
        }
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertTokenType(END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }

    private void _testInvalidDup(final String spaces, final int chunkSize) throws Exception
    {
        final String XML = spaces + "<!DOCTYPE root> <!DOCTYPE root> <root />";

        final AsyncXMLInputFactory f = new InputFactoryImpl();

        //test for byte array
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr_array = null;
        try {
            sr_array = f.createAsyncForByteArray();
            final AsyncReaderWrapperForByteArray reader_array = new AsyncReaderWrapperForByteArray(sr_array, chunkSize, XML);
            _testInvalidDup(sr_array, reader_array);
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
            _testInvalidDup(sr_buffer, reader_buffer);
        } finally {
            if (sr_buffer != null) {
                sr_buffer.close();
            }
        }
    }

    private void _testInvalidDup(final AsyncXMLStreamReader<?> sr, final AsyncReaderWrapper reader) throws Exception
    {
        int t = verifyStart(reader);
        assertTokenType(DTD, t);
        assertEquals("root", sr.getPrefixedName());

        // so far so good, but not any more:
        try {
            reader.nextToken();
        } catch (XMLStreamException e) {
            verifyException(e, "Duplicate DOCTYPE declaration");
        }
    }
}
