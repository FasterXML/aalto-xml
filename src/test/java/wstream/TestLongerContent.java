package wstream;

import java.io.*;

import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;

import base.BaseTestCase;

public class TestLongerContent extends BaseTestCase
{
    final static String TEXT_UTF8_2BYTES = "\u00C0"+"c\u00C7ent! ";

    final XMLOutputFactory2 OUTPUT_FACTORY = getNewOutputFactory();

    final XMLInputFactory2 INPUT_FACTORY = getNewInputFactory();
    
    // To test [#26]

    public void testLongerWithMultiByteBytes() throws Exception {
        _testLongerWithMultiByte(true, false);
    }

    public void testLongerWithMultiByteBytesCData() throws Exception {
        _testLongerWithMultiByte(true, true);
    }

    public void testLongerWithMultiByteChars() throws Exception {
        _testLongerWithMultiByte(false, false);
    }

    public void testLongerWithMultiByteCharsCData() throws Exception {
        _testLongerWithMultiByte(false, true);
    }
    
    public void _testLongerWithMultiByte(boolean byteBased, boolean useCData) throws Exception
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

        sw.writeStartDocument();

        sw.writeStartElement("root");
        for (int i = 0; i < COUNT; ++i) {
            sw.writeStartElement("a");
            if (useCData) {
                sw.writeCData(TEXT_UTF8_2BYTES+(i % 1111));
            } else {
                sw.writeCharacters(TEXT_UTF8_2BYTES+(i % 1111));
            }
            sw.writeRaw("\n");
            sw.writeEndElement();
        }
        sw.writeEndElement();
        sw.writeEndDocument();

        XMLStreamReader sr = byteBased
                ? INPUT_FACTORY.createXMLStreamReader(new ByteArrayInputStream(bytes.toByteArray()), "UTF-8")
                : INPUT_FACTORY.createXMLStreamReader(new StringReader(strw.toString()));

        assertTokenType(START_DOCUMENT, sr.getEventType());

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        
        for (int i = 0; i < COUNT; ++i) {
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("a", sr.getLocalName());
            String EXP = TEXT_UTF8_2BYTES+(i % 1111)+"\n";
            String ACT = sr.getElementText();
            if (!EXP.equals(ACT)) {
                assertEquals("Strings differ at "+sr.getLocation(), EXP, ACT);
            }
            assertTokenType(END_ELEMENT, sr.getEventType());
            assertEquals("a", sr.getLocalName());
        }

        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        sr.close();
    }
}
