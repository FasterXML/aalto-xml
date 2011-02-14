package async;

import javax.xml.stream.XMLStreamConstants;

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
    
    private void _testLinefeeds(int chunkSize, boolean checkValues, String SPC) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        final String XML = SPC+"<root>\rFirst\r\nSecond\nThird: "+UNICODE_SEGMENT+"</root>";
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

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

    private void _testTextWithEntities(int chunkSize, boolean checkValues, String SPC) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        final String XML = SPC+"<root>a&lt;b\rMOT</root>";
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

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
            assertEquals("a<b\nMOT", str);
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

    private void _testTextWithNumericEntities(int chunkSize, boolean checkValues, String SPC) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        final String XML = SPC+"<root>&#60;tag&#x3e;!</root>";
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

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
