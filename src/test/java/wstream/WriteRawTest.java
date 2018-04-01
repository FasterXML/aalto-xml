package wstream;

import java.io.*;

import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;

// for [aalto-xml#46]
public class WriteRawTest extends base.BaseTestCase
{
    public void testSerialization_failsWithUtf8() throws Exception
    {
        final String TEXT = "Left \u2265 Right";
        final String CONTENT = "<problem>"+TEXT+"</problem>";

        final XMLOutputFactory2 outputFactory = newOutputFactory();
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final XMLStreamWriter2 xmlStreamWriter =
                (XMLStreamWriter2) outputFactory.createXMLStreamWriter(byteArrayOutputStream, "utf-8");

        xmlStreamWriter.writeStartElement("example");
        xmlStreamWriter.writeRaw(CONTENT);
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.close();

        final String xml = byteArrayOutputStream.toString("utf-8");

        XMLStreamReader r = newInputFactory().createXMLStreamReader(new StringReader(xml));
        assertTokenType(START_ELEMENT, r.next());
        assertEquals("example", r.getLocalName());
        assertTokenType(START_ELEMENT, r.next());
        assertEquals("problem", r.getLocalName());
        assertTokenType(CHARACTERS, r.next());
        assertEquals(TEXT, r.getText());
        assertTokenType(END_ELEMENT, r.next());
        assertTokenType(END_ELEMENT, r.next());
        r.close();
    }
}
