package com.fasterxml.aalto.sax;

import com.fasterxml.aalto.out.Utf8XmlWriter;
import com.fasterxml.aalto.out.WriterConfig;

import java.io.ByteArrayOutputStream;

public class TestSaxWriter extends base.BaseTestCase
{
    private final String TEXT_WITH_SURROGATE;
    {
        StringBuilder testText = new StringBuilder(1025);
        for (int i = 0; i < 511; i++) {
            testText.append('x');
        }
        testText.append("\uD835\uDFCE");
        for (int i = 0; i < 512; i++) {
            testText.append('x');
        }
        TEXT_WITH_SURROGATE = testText.toString();
    }

    public void testSplitSurrogateWithAttributeValue() throws Exception
    {
        // This test aims to produce the
        // javax.xml.stream.XMLStreamException: Incomplete surrogate pair in content: first char 0xd835, second 0x78
        // error message. Before fixing the respective issue, it was provoked by a multi-byte character
        // where the first byte was exactly at the end of the internal reading buffer and enough further data
        // to also fill the next two internal reading buffers. Then, the code would try to fuse the first byte
        // of the original multi-byte character with the first character in the third buffer because
        // ByteXmlWriter#_surrogate was not set back to 0 after writing the original multi-byte character.
        WriterConfig writerConfig = new WriterConfig();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Utf8XmlWriter writer = new Utf8XmlWriter(writerConfig, byteArrayOutputStream);
        writer.writeStartTagStart(writer.constructName("testelement"));
        writer.writeAttribute(writer.constructName("testattr"), TEXT_WITH_SURROGATE);
        writer.writeStartTagEnd();
        writer.writeEndTag(writer.constructName("testelement"));
        writer.close(false);
    }

    public void testSplitSurrogateWithAttributeValue2() throws Exception
    {
        // This test aims to produce the
        // java.io.IOException: Unpaired surrogate character (0xd835)
        // error message. Before fixing the respective issue, it was provoked by a multi-byte character
        // where the first byte was exactly at the end of the internal reading buffer and the next
        // reading buffer was enough to write all the remaining data. Then, by the missing reset of
        // ByteXmlWriter#_surrogate, the code expected another multi-byte surrogate that never came.
        StringBuilder testText = new StringBuilder();
        for (int i = 0; i < 511; i++) {
            testText.append('x');
        }
        testText.append("\uD835\uDFCE");

        WriterConfig writerConfig = new WriterConfig();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Utf8XmlWriter writer = new Utf8XmlWriter(writerConfig, byteArrayOutputStream);
        writer.writeStartTagStart(writer.constructName("testelement"));
        writer.writeAttribute(writer.constructName("testattr"), testText.toString());
        writer.writeStartTagEnd();
        writer.writeEndTag(writer.constructName("testelement"));
        writer.close(false);
    }

    public void testSplitSurrogateWithCData() throws Exception
    {
        // Modification of "testSplitSurrogateWithAttributeValue()" but for CDATA
        WriterConfig writerConfig = new WriterConfig();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Utf8XmlWriter writer = new Utf8XmlWriter(writerConfig, byteArrayOutputStream);
        writer.writeStartTagStart(writer.constructName("testelement"));
        writer.writeCData(TEXT_WITH_SURROGATE);
        writer.writeStartTagEnd();
        writer.writeEndTag(writer.constructName("testelement"));
        writer.close(false);
    }

    public void testSplitSurrogateWithComment() throws Exception
    {
        // Modification of "testSplitSurrogateWithAttributeValue()" but for Comment
        WriterConfig writerConfig = new WriterConfig();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Utf8XmlWriter writer = new Utf8XmlWriter(writerConfig, byteArrayOutputStream);
        writer.writeStartTagStart(writer.constructName("testelement"));
        writer.writeComment(TEXT_WITH_SURROGATE);
        writer.writeStartTagEnd();
        writer.writeEndTag(writer.constructName("testelement"));
        writer.close(false);
    }

    public void testSplitSurrogateWithPI() throws Exception
    {
        // Modification of "testSplitSurrogateWithAttributeValue()" but for Processing instructions
        WriterConfig writerConfig = new WriterConfig();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Utf8XmlWriter writer = new Utf8XmlWriter(writerConfig, byteArrayOutputStream);
        writer.writeStartTagStart(writer.constructName("testelement"));
        writer.writePI(writer.constructName("target"), TEXT_WITH_SURROGATE);
        writer.writeStartTagEnd();
        writer.writeEndTag(writer.constructName("testelement"));
        writer.close(false);
    }
}
