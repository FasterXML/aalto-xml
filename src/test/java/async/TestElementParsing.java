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
            _testEmptyRoot(8, SPC+"<root />");
            _testEmptyRoot(8, SPC+"<root/>");
            _testEmptyRoot(8, SPC+"<root></root>");
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
            _testElements(8, SPC);
            _testElements(15, SPC);
        }
    }

    // Bit more stuff with attributes
    public void testParseElementsWithAttrs() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testElementsWithAttrs(1, true, SPC);
            _testElementsWithAttrs(2, true, SPC);
            _testElementsWithAttrs(3, true, SPC);
            _testElementsWithAttrs(5, true, SPC);
            _testElementsWithAttrs(8, true, SPC);
            _testElementsWithAttrs(15, true, SPC);
            _testElementsWithAttrs(999, true, SPC);
        }
    }

    public void testSkipElementsWithAttrs() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testElementsWithAttrs(1, false, SPC);
            _testElementsWithAttrs(2, false, SPC);
            _testElementsWithAttrs(3, false, SPC);
            _testElementsWithAttrs(5, false, SPC);
            _testElementsWithAttrs(8, false, SPC);
            _testElementsWithAttrs(15, false, SPC);
            _testElementsWithAttrs(999, false, SPC);
        }
    }

    // [Issue-12], probs with attrs, multi-byte UTF-8 chars
    public void testParseElementsWithUTF8Attrs() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testElementsWithUTF8Attrs(1, true, SPC);
            _testElementsWithUTF8Attrs(2, true, SPC);
            _testElementsWithUTF8Attrs(5, true, SPC);
            _testElementsWithAttrs(999, true, SPC);
        }
    }

    // [Issue-12]
    public void testSkipElementsWithUTF8Attrs() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testElementsWithUTF8Attrs(1, false, SPC);
            _testElementsWithUTF8Attrs(2, false, SPC);
            _testElementsWithUTF8Attrs(5, false, SPC);
            _testElementsWithAttrs(999, false, SPC);
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
        int t = verifyStart(reader);
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
//        final String XML = SPC+"<root attr='1&amp;2'><leaf xmlns='abc' a   ='3'\rb=''  /></root>";
        final String XML = SPC+"<root attr='1&amp;2'><leaf xmlns='abc' a   ='3'\rxmlns:foo='bar'  b=''  /></root>";
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

        // should start with START_DOCUMENT, but for now skip
        int t = verifyStart(reader);
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
        assertEquals(2, sr.getNamespaceCount());

        assertEquals("a", sr.getAttributeLocalName(0));
        assertEquals("", sr.getAttributeNamespace(0));
        assertEquals("3", sr.getAttributeValue(0));
        assertEquals("b", sr.getAttributeLocalName(1));
        assertEquals("", sr.getAttributeNamespace(1));
        assertEquals("", sr.getAttributeValue(1));

        assertEquals("", sr.getNamespacePrefix(0));
        assertEquals("abc", sr.getNamespaceURI(0));
        assertEquals("foo", sr.getNamespacePrefix(1));
        assertEquals("bar", sr.getNamespaceURI(1));
        
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("leaf", sr.getLocalName());
        assertEquals("abc", sr.getNamespaceURI());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }

    private void _testElementsWithAttrs(int chunkSize, boolean checkValues, String SPC) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
//        final String XML = SPC+"<root attr='1&amp;2'><leaf xmlns='abc' a   ='3'\rb=''  /></root>";
        final String XML = SPC+"<root attr='1&#62;2, 2&#x3C;1' />";
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

        // should start with START_DOCUMENT, but for now skip
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
            assertEquals(1, sr.getAttributeCount());
            assertEquals("1>2, 2<1", sr.getAttributeValue(0));
            assertEquals("attr", sr.getAttributeLocalName(0));
            assertEquals("", sr.getAttributeNamespace(0));
        }
        assertTokenType(END_ELEMENT, reader.nextToken());
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }

    private void _testElementsWithUTF8Attrs(int chunkSize, boolean checkValues, String SPC) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        final String VALUE = "Gr\u00e4"; 
        final String XML = SPC+"<root attr='"+VALUE+"' />";
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

        // should start with START_DOCUMENT, but for now skip
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
            assertEquals(1, sr.getAttributeCount());
            assertEquals(VALUE, sr.getAttributeValue(0));
            assertEquals("attr", sr.getAttributeLocalName(0));
            assertEquals("", sr.getAttributeNamespace(0));
        }
        assertTokenType(END_ELEMENT, reader.nextToken());
        if (checkValues) {
            assertEquals("root", sr.getLocalName());
            assertEquals("", sr.getNamespaceURI());
        }
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
}
