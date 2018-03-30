package stax2.wstream;

import java.io.*;
import javax.xml.stream.*;
import javax.xml.transform.stream.StreamResult;

import org.codehaus.stax2.XMLInputFactory2;

import stax2.BaseStax2Test;

/**
 * This unit test suite verifies use of {@link StreamResult} as output
 * for {@link XMLOutputFactory}.
 *
 * @author Tatu Saloranta
 *
 * @since 3.0
 */
public class TestStreamResult
    extends BaseStax2Test
{
    /**
     * This test is related to problem reported as [WSTX-182], inability
     * to use SystemId alone as source.
     */
    public void testCreateUsingSystemId()
        throws IOException, XMLStreamException
    {
        File tmpF = File.createTempFile("staxtest", ".xml");
        tmpF.deleteOnExit();

        XMLOutputFactory f = getOutputFactory();
        StreamResult dst = new StreamResult();
        dst.setSystemId(tmpF);
        XMLStreamWriter sw = f.createXMLStreamWriter(dst);

        sw.writeStartDocument();
        sw.writeEmptyElement("root");
        sw.writeEndDocument();
        sw.close();

        // plus let's read and check it
        XMLInputFactory2 inf = getInputFactory();
        XMLStreamReader sr = inf.createXMLStreamReader(tmpF);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }
}
