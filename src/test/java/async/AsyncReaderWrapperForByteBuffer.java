package async;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;

/**
 * Helper class used with async parser
 */
public class AsyncReaderWrapperForByteBuffer implements AsyncReaderWrapper
{
    private final AsyncXMLStreamReader<AsyncByteBufferFeeder> _streamReader;

    private final byte[] _xml;

    private final int _bytesPerFeed;
    private int _offset = 0;

    private final ByteBuffer _buf;

    public AsyncReaderWrapperForByteBuffer(AsyncXMLStreamReader<AsyncByteBufferFeeder> sr, String xmlString) {
        this(sr, 1, xmlString);
    }
    
    public AsyncReaderWrapperForByteBuffer(AsyncXMLStreamReader<AsyncByteBufferFeeder> sr, int bytesPerCall, String xmlString)
    {
        _streamReader = sr;
        _bytesPerFeed = bytesPerCall;
        try {
            _xml = xmlString.getBytes("UTF-8");
            _buf = ByteBuffer.allocate(_bytesPerFeed);
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
            _buf.clear();
            AsyncByteBufferFeeder feeder = _streamReader.getInputFeeder();
            if (!feeder.needMoreInput()) {
                AsyncTestBase.fail("Got EVENT_INCOMPLETE, could not feed more input");
            }

            if (_offset >= _xml.length) { // end-of-input?
                feeder.endOfInput();
            } else {
                int amount = Math.min(_bytesPerFeed, _xml.length - _offset);
                _buf.put(_xml, _offset, amount);
                _buf.flip();
                feeder.feedInput(_buf);
                _offset += amount;
            }
        }
        return token;
    }
}