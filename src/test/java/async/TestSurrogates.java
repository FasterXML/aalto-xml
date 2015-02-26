package async;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestSurrogates extends AsyncTestBase
{
    private final int HIGH_CODEPOINT = 0x1031c;
    private final String SURROGATE = new StringBuilder().appendCodePoint(HIGH_CODEPOINT).toString();
    private final String VALUE = "a/"+SURROGATE+"/b";
    private final String DOC = "<value>"+VALUE+"</value>";

    public void testCdataWithSurrogate() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testWithSurrogate(SPC, 1);
            _testWithSurrogate(SPC, 2);
            _testWithSurrogate(SPC, 3);
            _testWithSurrogate(SPC, 5);
            _testWithSurrogate(SPC, 9);
            _testWithSurrogate(SPC, 999);
        }
    }

    public void testSkipWithSurrogate() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testSkipWithSurrogate(SPC, 1);
            _testSkipWithSurrogate(SPC, 2);
            _testSkipWithSurrogate(SPC, 3);
            _testSkipWithSurrogate(SPC, 5);
            _testSkipWithSurrogate(SPC, 9);
            _testSkipWithSurrogate(SPC, 999);
        }
    }
    
    private void _testWithSurrogate(String spaces, int chunkSize) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = f.createAsyncForByteArray();
        AsyncReaderWrapperForByteArray reader = new AsyncReaderWrapperForByteArray(sr, chunkSize, spaces+DOC);
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertEquals("value", sr.getLocalName());
        assertTokenType(CHARACTERS, reader.nextToken());
        String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
        assertEquals(VALUE, str);
        assertTokenType(END_ELEMENT, reader.currentToken());
        assertEquals("value", sr.getLocalName());
        assertTokenType(END_DOCUMENT, reader.nextToken());
        sr.close();
    }

    private void _testSkipWithSurrogate(String spaces, int chunkSize) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader<AsyncByteArrayFeeder> sr = f.createAsyncForByteArray();
        AsyncReaderWrapperForByteArray reader = new AsyncReaderWrapperForByteArray(sr, chunkSize, spaces+DOC);
        int t = verifyStart(reader);
        assertTokenType(START_ELEMENT, t);
        assertTokenType(CHARACTERS, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertTokenType(END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
        sr.close();
    }
}
