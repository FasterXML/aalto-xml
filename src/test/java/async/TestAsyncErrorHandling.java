package async;

import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;

// for [aalto-xml#52]: improve error reporting for multiple roots
public class TestAsyncErrorHandling extends AsyncTestBase
{
    public void testSimpleByteArray() throws Exception
    {
        final AsyncXMLInputFactory f = newAsyncInputFactory();

        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = f.createAsyncForByteArray();
        final AsyncReaderWrapperForByteArray asyncR = new AsyncReaderWrapperForByteArray(sr,
                1000, "<root>a</root><second>x>/second>");

        int t = verifyStart(asyncR);
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals(0, sr.getAttributeCount());

        assertTokenType(CHARACTERS, sr.next());
        assertTokenType(END_ELEMENT, sr.next());

        // and now expect problems
        try {
            sr.next();
            fail("Should not pass");
        } catch (XMLStreamException e) {
            verifyException(e, "Second root element in content");
        }
    }
}
