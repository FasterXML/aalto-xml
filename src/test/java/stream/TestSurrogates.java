package stream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

public class TestSurrogates extends base.BaseTestCase
{
    private final int HIGH_CODEPOINT = 0x1031c;
    private final String SURROGATE = new StringBuilder().appendCodePoint(HIGH_CODEPOINT).toString();
    private final String VALUE = "a/"+SURROGATE+"/b";
    private final String DOC = "<value>"+VALUE+"</value>";
    
    // for [#27]
    public void testSurrogateSkipping() throws Exception
    {
        _testSurrogate(true, false);
        _testSurrogate(true, true);
    }

    public void testSurrogateParsing() throws Exception
    {
        _testSurrogate(false, false);
        _testSurrogate(false, true);
    }

    private void _testSurrogate(boolean skip, boolean useBytes) throws Exception
    {
        XMLStreamReader2 sr = createReader(DOC, useBytes);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("value", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        if (!skip) {
            assertEquals(VALUE, sr.getText());
        }
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("value", sr.getLocalName());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private final XMLInputFactory2 F2 = getNewInputFactory();
    
    XMLStreamReader2 createReader(String content, boolean useBytes)
        throws IOException, XMLStreamException
    {
        if (useBytes) {
            byte[] data = content.getBytes("UTF-8");
            return (XMLStreamReader2) F2.createXMLStreamReader(new ByteArrayInputStream(data));
        }
        return (XMLStreamReader2) F2.createXMLStreamReader(new StringReader(content));
    }
}
