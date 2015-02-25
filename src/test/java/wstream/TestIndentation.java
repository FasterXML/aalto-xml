package wstream;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;

import base.BaseTestCase;

// Simple test(s) to ensure that explicit indentation is fine, even outside
// root. Related to [#7]
public class TestIndentation extends BaseTestCase
{
    final XMLOutputFactory2 OUTPUT_FACTORY = getNewOutputFactory();

    public void testRootLevelIndentBytes() throws Exception
    {
        _testRootLevelIndent(true);
    }

    public void testRootLevelIndentChars() throws Exception
    {
        _testRootLevelIndent(false);
    }
    
    public void _testRootLevelIndent(boolean byteBased) throws Exception
    {
        final int COUNT = 199999;

        ByteArrayOutputStream bytes;
        StringWriter strw;
        XMLStreamWriter2 sw;

        if (byteBased) {
            bytes = new ByteArrayOutputStream(COUNT * 4);
            strw = null;
            sw = (XMLStreamWriter2) OUTPUT_FACTORY.createXMLStreamWriter(bytes, "UTF-8");
        } else {
            bytes = null;
            strw = new StringWriter(COUNT * 4);
            sw = (XMLStreamWriter2) OUTPUT_FACTORY.createXMLStreamWriter(strw);
        }

        sw.writeStartDocument("UTF-8", "1.0");
        sw.writeCharacters("\n");
        sw.writeStartElement("root");
        sw.writeCharacters("x");
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();

        String xml = byteBased ? bytes.toString("UTF-8") : strw.toString();
        if (xml.indexOf("<root>") < 0) {
            fail("No 'root' in: "+xml);
        }
        if (xml.indexOf('\n') < 0) {
            fail("Should have included linefeed in output: "+xml);
        }
    }
}
