package async;

import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestCommentParsing extends AsyncTestBase
{
    public void testComments() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testComments(SPC, 1);
            _testComments(SPC, 2);
            _testComments(SPC, 3);
            _testComments(SPC, 5);
        }
    }

    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */
    
    private void _testComments(String spaces, int chunkSize) throws Exception
    {
        String XML = spaces+"<!--comments&s\r\ntuf-fy>--><root><!----></root><!--\nHi - "+UNICODE_SEGMENT+" - ho->-->";
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);
        int t = verifyStart(reader);
        assertTokenType(COMMENT, t);
        assertEquals("comments&s\ntuf-fy>", sr.getText());
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertTokenType(COMMENT, reader.nextToken());
        assertEquals("", sr.getText());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertTokenType(COMMENT, reader.nextToken());
        assertEquals("\nHi - "+UNICODE_SEGMENT+" - ho->", sr.getText());
        assertTokenType(END_DOCUMENT, reader.nextToken());
    }
}
