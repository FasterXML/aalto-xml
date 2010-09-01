package test;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamWriter2;

/**
 * Simple non-automated manual test code for outputting namespace-aware XML
 * documents.
 */
public class RunStreamWriter
{
    private RunStreamWriter() { }

    protected XMLOutputFactory getFactory() throws Exception
    {
        return (XMLOutputFactory) Class.forName("com.fasterxml.aalto.stax.OutputFactoryImpl").newInstance();
    }

    final String ENCODING = "ISO-8859-1";
    //final String ENCODING = "UTF-8";

    protected void test() throws Exception
    {
        XMLOutputFactory f = getFactory();
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.TRUE);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter2 sw = (XMLStreamWriter2) f.createXMLStreamWriter(bos, ENCODING);
        //XMLStreamWriter sw = f.createXMLStreamWriter(bos);

        /*
        StringWriter w = new StringWriter();
        XMLStreamWriter sw = f.createXMLStreamWriter(w);
        */

        sw.writeStartDocument();
        sw.writeSpace("\n");
        writeContents(sw);
        sw.writeEndDocument();

        sw.flush();
        sw.close();

        System.err.println("DOC -> '"+new String(bos.toByteArray(), ENCODING)+"'");
        //System.err.println("DOC -> '"+w.toString()+"'");
    }

    protected void writeContents(XMLStreamWriter sw)
        throws XMLStreamException
    {
        final String URL_P1 = "http://p1.org";
        // Let's try to enforce using of the default ns by passing empty prefix
        // (writer is not required to honor that request though)
        sw.writeStartElement("", "test", URL_P1);
        sw.writeStartElement("", "leaf", URL_P1);
        sw.writeEndElement();
        sw.writeEndElement();

        /*
        sw.writeCharacters("\n");
        sw.writeStartElement("root");

        sw.writeCharacters("Need to quote this (\u0531) too: ]]>");

        sw.writeEmptyElement("alpha");
        sw.writeAttribute("attr", "(\u0531)");
        sw.writeNamespace("ns", "uri:foo");
        sw.writeAttribute("atpr", "http://attr-prefix", "attr", "a<b");

        sw.writeStartElement("bravo");

        sw.writeCharacters("Text: & \n");

        sw.writeCData("Test: ]]>x");
        sw.writeProcessingInstruction("p", "i");

        sw.writeEndElement();

        sw.writeStartElement("bravo"); // 2nd one, recycle
        sw.writeEmptyElement("root");
        sw.writeEndElement();

        sw.writeEmptyElement("root");
        sw.writeEmptyElement("root");
        sw.writeEmptyElement("root");
        sw.writeEmptyElement("root");
        sw.writeEmptyElement("bravo");

        sw.writeCharacters("\n"); // to get linefeed
        */
    }

    public static void main(String[] args) throws Exception
    {
        new RunStreamWriter().test();
    }
}
