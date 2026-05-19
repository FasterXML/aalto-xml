package stax2.wstream;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for <a href="https://github.com/FasterXML/aalto-xml/issues/56">#56</a>:
 * with {@code IS_REPAIRING_NAMESPACES=true}, copying events from an
 * {@link XMLEventReader} to an {@link XMLEventWriter} used to duplicate the
 * default {@code xmlns} attribute on the root element (it was emitted three
 * times).
 */
public class TestIssue56DefaultNamespaceRepair
    extends BaseWriterTest
{
    @Test
    public void testDefaultNamespaceNotDuplicatedOnCopy() throws Exception
    {
        // Minimal analogue of the xmldsig schema referenced in the issue:
        // root declares a default namespace plus a prefixed one, and uses
        // unprefixed child tags bound to the default namespace.
        final String SRC =
            "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\""
            + " xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\""
            + " targetNamespace=\"http://www.w3.org/2000/09/xmldsig#\""
            + " version=\"0.1\" elementFormDefault=\"qualified\">"
            + "<element name=\"Signature\" type=\"ds:SignatureType\"/>"
            + "</schema>";

        XMLInputFactory inF = getInputFactory();
        XMLOutputFactory outF = getOutputFactory();
        outF.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);

        StringWriter sw = new StringWriter();
        XMLEventReader er = inF.createXMLEventReader(new StringReader(SRC));
        XMLEventWriter ew = outF.createXMLEventWriter(sw);
        ew.add(er);
        ew.close();
        er.close();

        String out = sw.toString();

        int firstXmlns = out.indexOf("xmlns=");
        assertTrue(firstXmlns >= 0, "Expected an xmlns= in output: " + out);
        int secondXmlns = out.indexOf("xmlns=", firstXmlns + 1);
        assertEquals(-1, secondXmlns, "Default xmlns attribute written more than once on root: "
                + out);

        // And the prefixed namespace must still be present.
        assertTrue(out.indexOf("xmlns:ds=") >= 0, "Expected xmlns:ds= in output: " + out);
    }
}
