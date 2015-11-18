package async;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;

/**
 * Helper class used with async parser
 */
public class AsyncReaderWrapperForByteArray implements AsyncReaderWrapper
{
    private final AsyncXMLStreamReader<AsyncByteArrayFeeder> _streamReader;
    private final byte[] _xml;
    private final int _bytesPerFeed;
    private int _offset = 0;

    public AsyncReaderWrapperForByteArray(AsyncXMLStreamReader<AsyncByteArrayFeeder> sr, String xmlString) {
        this(sr, 1, xmlString);
    }
    
    public AsyncReaderWrapperForByteArray(AsyncXMLStreamReader<AsyncByteArrayFeeder> sr, int bytesPerCall, String xmlString)
    {
        _streamReader = sr;
        _bytesPerFeed = bytesPerCall;
        try {
            _xml = xmlString.getBytes("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String currentText() throws XMLStreamException {
        return _streamReader.getText();
    }

    @Override
    public int currentToken() throws XMLStreamException {
        return _streamReader.getEventType();
    }

    @Override
    public int nextToken() throws XMLStreamException
    {
        int token;
        
        while ((token = _streamReader.next()) == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
            AsyncByteArrayFeeder feeder = _streamReader.getInputFeeder();
            if (!feeder.needMoreInput()) {
                AsyncTestBase.fail("Got EVENT_INCOMPLETE, could not feed more input");
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