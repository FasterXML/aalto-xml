package wstream;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.codehaus.stax2.XMLStreamWriter2;
import org.junit.jupiter.api.Test;

import base.BaseTestCase;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WriteSpaceTest extends BaseTestCase
{
    @Test
    public void testStringSpaceWithByteWriter() throws Exception {
        _testSpace(true, false);
    }

    @Test
    public void testStringSpaceWithCharacterWriter() throws Exception {
        _testSpace(false, false);
    }

    @Test
    public void testArraySpaceWithByteWriter() throws Exception {
        _testSpace(true, true);
    }

    @Test
    public void testArraySpaceWithCharacterWriter() throws Exception {
        _testSpace(false, true);
    }

    private void _testSpace(boolean byteBased, boolean charArray) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringWriter chars = new StringWriter();
        XMLStreamWriter2 writer = (XMLStreamWriter2) (byteBased
                ? newOutputFactory().createXMLStreamWriter(bytes, "UTF-8")
                : newOutputFactory().createXMLStreamWriter(chars));
        _writeSpace(writer, "\n", charArray);
        writer.writeStartElement("root");
        _writeSpace(writer, "\n  ", charArray);
        writer.writeEmptyElement("leaf");
        _writeSpace(writer, "\n", charArray);
        writer.writeEndElement();
        _writeSpace(writer, "\n", charArray);
        writer.writeEndDocument();
        writer.close();

        String xml = byteBased ? bytes.toString("UTF-8") : chars.toString();
        assertEquals("\n<root>\n  <leaf/>\n</root>\n", xml);
    }

    private void _writeSpace(XMLStreamWriter2 writer, String space, boolean charArray)
            throws Exception
    {
        if (charArray) {
            char[] buffer = ("x" + space + "x").toCharArray();
            writer.writeSpace(buffer, 1, space.length());
        } else {
            writer.writeSpace(space);
        }
    }
}
