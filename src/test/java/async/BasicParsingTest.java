package async;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class BasicParsingTest extends BaseAsyncTest
{
    public void testSimple() throws Exception
    {
        // let's try with different chunking to tease different edge cases...
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

    private void _testSimple(int chunkSize) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, 
            "<!--comment&stuff--><root attr='1'>text<![CDATA[cdata &] ]] stuff]]></root><?pi data? what data??>");

        // minor deviation from Stax; START_DOCUMENT not available right away
//        assertTokenType(AsyncXMLStreamReader.EVENT_INCOMPLETE, reader.currentToken());
        int t = reader.nextToken();
//        assertTokenType(XMLStreamConstants.START_DOCUMENT, reader.nextToken());
        assertTokenType(XMLStreamConstants.COMMENT, t);
        assertEquals("comment&stuff", sr.getText());
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
        assertEquals("cdata &] ]] stuff", str);
        assertTokenType(XMLStreamConstants.END_ELEMENT, reader.currentToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.PROCESSING_INSTRUCTION, reader.nextToken());
        assertEquals("pi", sr.getPITarget());
        assertEquals("data? what data?", sr.getPIData());
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
