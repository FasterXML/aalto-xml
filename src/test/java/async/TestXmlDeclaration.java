package async;

import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import static com.fasterxml.aalto.AsyncXMLStreamReader.EVENT_INCOMPLETE;

public class TestXmlDeclaration extends AsyncTestBase
{
    private final int[] CHUNK_SIZES = new int[] { 1, 2, 3, 5, 9, 33 };

    public void testNoDeclaration() throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        for (String XML : new String[] { "   <root />", "<root/>" }) {
            for (int chunkSize : CHUNK_SIZES) {
                AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
                AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);
                assertEquals(EVENT_INCOMPLETE, reader.currentToken());
                assertTokenType(START_DOCUMENT, reader.nextToken());
                // no info, however; except for encoding auto-detection
                assertNull(sr.getCharacterEncodingScheme());
                assertEquals("UTF-8", sr.getEncoding());
                assertNull(sr.getVersion());
                assertFalse(sr.standaloneSet());
    
                assertTokenType(START_ELEMENT, reader.nextToken());
                assertEquals("root", sr.getLocalName());
            }
        }
    }

    public void testVersionOnlyDeclaration() throws Exception
    {
        String XML = "<?xml version='1.0' ?><root />";
        AsyncXMLInputFactory f = new InputFactoryImpl();

        for (int chunkSize : CHUNK_SIZES) {
            AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
            AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);
            assertEquals(EVENT_INCOMPLETE, reader.currentToken());
            assertTokenType(START_DOCUMENT, reader.nextToken());
            assertNull(sr.getCharacterEncodingScheme());
            assertEquals("UTF-8", sr.getEncoding());
            assertEquals("1.0", sr.getVersion());
            assertFalse(sr.standaloneSet());

            assertTokenType(START_ELEMENT, reader.nextToken());
            assertEquals("root", sr.getLocalName());
        }
    }

    public void testEncodingDeclaration() throws Exception
    {
        String XML = "<?xml version= \"1.0\"   encoding='UTF-8' ?><root/>";
        AsyncXMLInputFactory f = new InputFactoryImpl();

        for (int chunkSize : CHUNK_SIZES) {
            AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
            AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);
            assertEquals(EVENT_INCOMPLETE, reader.currentToken());
            assertTokenType(START_DOCUMENT, reader.nextToken());
            assertEquals("UTF-8", sr.getEncoding());
            assertEquals("UTF-8", sr.getCharacterEncodingScheme());
            assertEquals("1.0", sr.getVersion());
            assertFalse(sr.standaloneSet());

            assertTokenType(START_ELEMENT, reader.nextToken());
            assertEquals("root", sr.getLocalName());
        }
    }

    public void testStandAloneDeclaration() throws Exception
    {
        String XML = "<?xml version  ='1.0' encoding=\"UTF-8\"  standalone='yes' ?>  <root />";
        AsyncXMLInputFactory f = new InputFactoryImpl();

        for (int chunkSize : CHUNK_SIZES) {
            AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
            AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);
            assertEquals(EVENT_INCOMPLETE, reader.currentToken());
            assertTokenType(START_DOCUMENT, reader.nextToken());
            assertEquals("UTF-8", sr.getEncoding());
            assertEquals("UTF-8", sr.getCharacterEncodingScheme());
            assertEquals("1.0", sr.getVersion());
            assertTrue(sr.standaloneSet());
            assertTrue(sr.isStandalone());

            assertTokenType(START_ELEMENT, reader.nextToken());
            assertEquals("root", sr.getLocalName());
        }
    }

    public void testStandAloneDeclaration2() throws Exception
    {
        String XML = "<?xml version=\"1.0\" standalone='yes'?>\n<root/>";
        AsyncXMLInputFactory f = new InputFactoryImpl();

        for (int chunkSize : CHUNK_SIZES) {
            AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
            AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);
            assertEquals(EVENT_INCOMPLETE, reader.currentToken());
            assertTokenType(START_DOCUMENT, reader.nextToken());
            assertEquals("UTF-8", sr.getEncoding());
            assertNull(sr.getCharacterEncodingScheme());
            assertEquals("1.0", sr.getVersion());
            assertTrue(sr.standaloneSet());
            assertTrue(sr.isStandalone());

            assertTokenType(START_ELEMENT, reader.nextToken());
            assertEquals("root", sr.getLocalName());
        }
    }
}
