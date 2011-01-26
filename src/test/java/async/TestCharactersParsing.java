package async;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestCharactersParsing extends AsyncTestBase
{
    public void testLinefeeds() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            _testLinefeeds(1, spaces(spaces));
            _testLinefeeds(2, spaces(spaces));
            _testLinefeeds(3, spaces(spaces));
            _testLinefeeds(5, spaces(spaces));
        }
    }

    public void testTextWithEntities() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            _testTextWithEntities(1, spaces(spaces));
            _testTextWithEntities(2, spaces(spaces));
            _testTextWithEntities(3, spaces(spaces));
            _testTextWithEntities(5, spaces(spaces));
        }
    }

    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */
    
    private void _testLinefeeds(int chunkSize, String SPC) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        final String XML = SPC+"<root>\rFirst\r\nSecond\nThird: "+UNICODE_SEGMENT+"</root>";
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

        assertTokenType(START_ELEMENT, _verifyStart(reader));
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());

        assertTokenType(CHARACTERS, reader.nextToken());
        String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
        assertEquals("\nFirst\nSecond\nThird: "+UNICODE_SEGMENT, str);

        assertTokenType(END_ELEMENT, reader.currentToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }

    private void _testTextWithEntities(int chunkSize, String SPC) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        final String XML = SPC+"<root>a&lt;b\rMOT</root>";
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

        // should start with START_DOCUMENT, but for now skip
        int t = _verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(CHARACTERS, reader.nextToken());
        String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
        assertEquals("a<b\nMOT", str);
        assertTokenType(END_ELEMENT, reader.currentToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
    
    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected String collectAsyncText(AsyncReaderWrapper reader, int tt) throws XMLStreamException
    {
        StringBuilder sb = new StringBuilder();
        while (reader.currentToken() == tt) {
            sb.append(reader.currentText());
            reader.nextToken();
        }
        return sb.toString();
    }
    
    private int _verifyStart(AsyncReaderWrapper reader) throws Exception
    {
        // !!! TODO: should not start with START_DOCUMENT; but should get it right away
        int t = reader.nextToken();
        return t;
    }

}
