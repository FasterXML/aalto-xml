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
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr,        
            "<!--comment--><root attr='1'>text<![CDATA[cdata]]> & stuff</root><?pi data?>");

        // minor deviation from Stax; START_DOCUMENT not available right away
        assertTokenType(AsyncXMLStreamReader.EVENT_INCOMPLETE, reader.currentToken());
        assertTokenType(XMLStreamConstants.START_DOCUMENT, reader.nextToken());
        assertTokenType(XMLStreamConstants.COMMENT, reader.nextToken());
        assertTokenType(XMLStreamConstants.START_ELEMENT, reader.nextToken());
        assertTokenType(XMLStreamConstants.CDATA, reader.nextToken());
        /* Ok; need to loop a bit here...
         */
        assertTokenType(XMLStreamConstants.CDATA, reader.nextToken());
        String str = collectAsyncText(reader); // moves to end-element
        assertEquals("", str);
        assertTokenType(XMLStreamConstants.END_ELEMENT, reader.currentToken());
        assertTokenType(XMLStreamConstants.PROCESSING_INSTRUCTION, reader.nextToken());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
    
    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected String collectAsyncText(AsyncReaderWrapper reader) throws XMLStreamException
    {
        StringBuilder sb = new StringBuilder();
        while (reader.currentToken() == XMLStreamConstants.CHARACTERS) {
            sb.append(reader.currentText());
            reader.nextToken();
        }
        return sb.toString();
    }
}
