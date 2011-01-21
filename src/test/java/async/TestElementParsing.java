package async;

import javax.xml.stream.XMLStreamConstants;

import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestElementParsing extends AsyncTestBase
{
    public void testRootElement() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testEmptyRoot(1, SPC+"<root />");
            _testEmptyRoot(1, SPC+"<root/>");
            _testEmptyRoot(1, SPC+"<root></root>");
            _testEmptyRoot(2, SPC+"<root />");
            _testEmptyRoot(2, SPC+"<root/>");
            _testEmptyRoot(2, SPC+"<root></root>");
            _testEmptyRoot(3, SPC+"<root />");
            _testEmptyRoot(3, SPC+"<root/>");
            _testEmptyRoot(3, SPC+"<root></root>");
            _testEmptyRoot(5, SPC+"<root />");
            _testEmptyRoot(5, SPC+"<root/>");
            _testEmptyRoot(5, SPC+"<root></root>");
        }
    }

    public void testElements() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testElements(1, SPC);
            _testElements(2, SPC);
            _testElements(3, SPC);
            _testElements(5, SPC);
        }
    }
    
    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */

    private void _testEmptyRoot(int chunkSize, String XML) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

        // should start with START_DOCUMENT, but for now skip
        int t = this._verifyStart(reader);
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

    private void _testElements(int chunkSize, String SPC) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        final String XML = SPC+"<root attr='1&amp;2'><leaf xmlns='abc' a   ='3'\rb=''  /></root>";
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

        // should start with START_DOCUMENT, but for now skip
        int t = this._verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(1, sr.getAttributeCount());
        assertEquals("1&2", sr.getAttributeValue(0));
        assertEquals("attr", sr.getAttributeLocalName(0));
        assertEquals("", sr.getAttributeNamespace(0));
        
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("leaf", sr.getLocalName());
        assertEquals("abc", sr.getNamespaceURI());
        assertEquals(2, sr.getAttributeCount());

        assertEquals("a", sr.getAttributeLocalName(0));
        assertEquals("", sr.getAttributeNamespace(0));
        assertEquals("3", sr.getAttributeValue(0));
        assertEquals("b", sr.getAttributeLocalName(1));
        assertEquals("", sr.getAttributeNamespace(1));
        assertEquals("", sr.getAttributeValue(1));
        
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("leaf", sr.getLocalName());
        assertEquals("abc", sr.getNamespaceURI());
        assertTokenType(END_ELEMENT, reader.nextToken());
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

    private int _verifyStart(AsyncReaderWrapper reader) throws Exception
    {
        // !!! TODO: should not start with START_DOCUMENT; but should get it right away
        int t = reader.nextToken();
        return t;
    }
}
