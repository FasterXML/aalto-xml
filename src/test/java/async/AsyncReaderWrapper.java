package async;

import javax.xml.stream.XMLStreamException;

public interface AsyncReaderWrapper {
    String currentText() throws XMLStreamException;
    int currentToken() throws XMLStreamException;
    int nextToken() throws XMLStreamException;
}
