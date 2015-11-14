package async;

import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncXMLStreamReader;

abstract class AsyncTestBase extends base.BaseTestCase
{
    final static String SPACES = "                ";

    protected final static char UNICODE_2BYTES = (char) 167; // law symbol
    protected final static char UNICODE_3BYTES = (char) 0x4567;

    protected final static String UNICODE_SEGMENT = "["+UNICODE_2BYTES+"/"+UNICODE_3BYTES+"]";
    
    public static String spaces(int count) 
    {
        return SPACES.substring(0, Math.min(SPACES.length(), count));
    }

    protected final int verifyStart(AsyncReaderWrapper reader) throws Exception
    {
        assertTokenType(AsyncXMLStreamReader.EVENT_INCOMPLETE, reader.currentToken());
        assertTokenType(START_DOCUMENT, reader.nextToken());
        return reader.nextToken();
    }

    protected final String collectAsyncText(AsyncReaderWrapper reader, int tt) throws XMLStreamException
    {
        StringBuilder sb = new StringBuilder();
        while (reader.currentToken() == tt) {
            sb.append(reader.currentText());
            reader.nextToken();
        }
        return sb.toString();
    }
}
