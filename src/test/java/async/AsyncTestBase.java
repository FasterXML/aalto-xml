package async;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncInputFeeder;
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
    
    /**
     * Helper class used with async parser
     */
    public static class AsyncReaderWrapper
    {
        private final AsyncXMLStreamReader _streamReader;
        private final byte[] _xml;
        private final int _bytesPerFeed;
        private int _offset = 0;

        public AsyncReaderWrapper(AsyncXMLStreamReader sr, String xmlString) {
            this(sr, 1, xmlString);
        }
        
        public AsyncReaderWrapper(AsyncXMLStreamReader sr, int bytesPerCall, String xmlString)
        {
            _streamReader = sr;
            _bytesPerFeed = bytesPerCall;
            try {
                _xml = xmlString.getBytes("UTF-8");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String currentText() throws XMLStreamException
        {
            return _streamReader.getText();
        }
        
        public int currentToken() throws XMLStreamException
        {
            return _streamReader.getEventType();
        }
        
        public int nextToken() throws XMLStreamException
        {
            int token;
            
            while ((token = _streamReader.next()) == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
                AsyncInputFeeder feeder = _streamReader.getInputFeeder();
                if (!feeder.needMoreInput()) {
                    fail("Got EVENT_INCOMPLETE, could not feed more input");
                }
                if (_offset >= _xml.length) { // end-of-input?
                    feeder.endOfInput();
                } else {
                    int amount = Math.min(_bytesPerFeed, _xml.length - _offset);
                    feeder.feedInput(_xml, _offset, amount);
                    _offset += amount;
                }
            }
            return token;
        }
    }
}
