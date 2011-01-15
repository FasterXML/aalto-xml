package async;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class BasicParsingTest extends AsyncTestBase
{
    public void testEmptyRoot() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        _testEmptyRoot(1, false);
        _testEmptyRoot(2, false);
        _testEmptyRoot(3, false);
        _testEmptyRoot(5, false);

        _testEmptyRoot(1, true);
        _testEmptyRoot(2, true);
        _testEmptyRoot(3, true);
        _testEmptyRoot(5, true);
    }

    public void testRootNoContent() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        
        _testRootNoContent(1);
        _testRootNoContent(2);
        _testRootNoContent(3);
        _testRootNoContent(5);
    }

    public void testSimple() throws Exception
    {
        _testSimple(1);
        _testSimple(2);
        _testSimple(3);
        _testSimple(5);
    }

    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */

    private void _testEmptyRoot(int chunkSize, boolean addSpace) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        String XML = addSpace ? "<root />" : "<root/>";
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

        // should start with START_DOCUMENT, but for now skip
        int t = reader.nextToken();
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(0, sr.getAttributeCount());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }

    private void _testRootNoContent(int chunkSize) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, "<root  ></root>");

        // should start with START_DOCUMENT, but for now skip
        int t = reader.nextToken();
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(0, sr.getAttributeCount());
        assertTokenType(END_ELEMENT, reader.nextToken());

        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
    
    private void _testSimple(int chunkSize) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, 
            "<!--comment&s\r\ntuf-fy>--><root\r\nattr='1' >text<![CDATA[cdata\r\n&] ]] stuff]]></root><?pi\r\ndata? what\ndata??>");

        // minor deviation from Stax; START_DOCUMENT not available right away
//        assertTokenType(AsyncXMLStreamReader.EVENT_INCOMPLETE, reader.currentToken());
        int t = reader.nextToken();
//        assertTokenType(XMLStreamConstants.START_DOCUMENT, reader.nextToken());
        assertTokenType(XMLStreamConstants.COMMENT, t);
        assertEquals("comment&s\ntuf-fy>", sr.getText());
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(1, sr.getAttributeCount());
        assertEquals("attr", sr.getAttributeLocalName(0));
        assertEquals("", sr.getAttributeNamespace(0));
        assertEquals("1", sr.getAttributeValue(0));
        assertTokenType(CHARACTERS, reader.nextToken());
        String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
        assertEquals("text", str);
        // note: moved to next element by now, so:
        assertTokenType(CDATA, reader.currentToken());
        str = collectAsyncText(reader, CDATA); // moves to end-element
        assertEquals("cdata\n&] ]] stuff", str);
        assertTokenType(XMLStreamConstants.END_ELEMENT, reader.currentToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.PROCESSING_INSTRUCTION, reader.nextToken());
        assertEquals("pi", sr.getPITarget());
        assertEquals("data? what\ndata?", sr.getPIData());
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
}
