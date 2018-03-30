package stax2.wstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.io.*;

/**
 * Unit test suite that tests additional StAX2 stream writer construction
 * methods.
 */
public class TestWriterConstruction
    extends BaseWriterTest
{
    public void testCreateWithFileSource()
        throws IOException, XMLStreamException
    {
        XMLOutputFactory2 outf = getOutputFactory();
        File f = createTempFile();
        XMLStreamWriter sw = outf.createXMLStreamWriter(new Stax2FileResult(f));
        writeAndVerify(sw, f, "withFileSource");
    }

    public void testCreateWithFileStreamReader()
        throws IOException, XMLStreamException
    {
        // Doesn't do much, yet... just constructs, for now
        StringWriter strw = new StringWriter();
        XMLStreamWriter sw = getNonRepairingWriter(strw, true);
        XMLEventWriter ew = getOutputFactory().createXMLEventWriter(sw);

        assertNotNull(ew);

        // TODO: try it out...
    }

    /*
    ////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////
     */

    File createTempFile()
        throws IOException
    {
        File f = File.createTempFile("stax2test", null);
        f.deleteOnExit();
        return f;
   }

    private void writeAndVerify(XMLStreamWriter sw, File f, String text)
        throws XMLStreamException
    {
        /* No need to write elaborate doc, just to ensure creation and
         * later access work ok.
         */
        sw.writeStartDocument("UTF-8", "1.0");
        sw.writeStartElement("write");
        sw.writeCharacters(text);
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();

        // And then reader
        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        XMLStreamReader sr = ifact.createXMLStreamReader(new Stax2FileSource(f));
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("write", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals(text, getAndVerifyText(sr));
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("write", sr.getLocalName());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }
}
