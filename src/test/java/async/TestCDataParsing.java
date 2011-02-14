package async;

import javax.xml.stream.XMLStreamConstants;

import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestCDataParsing extends AsyncTestBase
{
    public void testCDataParse() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testCData(1, SPC);
            _testCData(2, SPC);
            _testCData(3, SPC);
            _testCData(5, SPC);
            _testCData(11, SPC);
            _testCData(999, SPC);
        }
    }

    public void testCDataSkip() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testCDataSkip(1, SPC);
            _testCDataSkip(2, SPC);
            _testCDataSkip(3, SPC);
            _testCDataSkip(5, SPC);
            _testCDataSkip(11, SPC);
            _testCDataSkip(999, SPC);
        }
    }
    
    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */

    private final static String XML = "<root><![CDATA[cdata\r\n&] ]] stuff]]>...<![CDATA[this\r\r and Unicode: "+UNICODE_SEGMENT+"!]]></root>";
    
    private void _testCData(int chunkSize, String SPC) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, SPC + XML);

        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(0, sr.getAttributeCount());

        // note: moved to next element by now, so:
        assertTokenType(CDATA, reader.nextToken());
        String str = collectAsyncText(reader, CDATA); // moves to end-element
        assertEquals("cdata\n&] ]] stuff", str);

        assertTokenType(XMLStreamConstants.CHARACTERS, reader.currentToken());
        str = collectAsyncText(reader, CHARACTERS);
        assertEquals("...", str);

        assertTokenType(XMLStreamConstants.CDATA, reader.currentToken());
        str = collectAsyncText(reader, CDATA);
        assertEquals("this\n\n and Unicode: "+UNICODE_SEGMENT+"!", str);
        
        assertTokenType(XMLStreamConstants.END_ELEMENT, reader.currentToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());

        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        
        assertFalse(sr.hasNext());
    }

    private void _testCDataSkip(int chunkSize, String SPC) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, SPC + XML);

        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(0, sr.getAttributeCount());

        // note: moved to next element by now, so:
        assertTokenType(CDATA, reader.nextToken());
        assertTokenType(XMLStreamConstants.CHARACTERS, reader.nextToken());
        assertTokenType(XMLStreamConstants.CDATA, reader.nextToken());
        assertTokenType(XMLStreamConstants.END_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        
        assertFalse(sr.hasNext());
    }
}
