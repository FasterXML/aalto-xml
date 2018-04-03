package stax2.vwstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamWriter2;
//import org.codehaus.stax2.validation.*;

/**
 * Unit tests for testing handling of attribute value validation, mostly
 * focusing on default value modifiers (#FIXED, #REQUIRED).
 * Validation for specific types are in type-specific additional tests.
 */
public class TestAttributeValidation
    extends BaseOutputTest
{
    final String NS_PREFIX = "ns";
    final String NS_PREFIX2 = "ns2";
    final String NS_URI = "http://ns";

    final String FIXED_DTD_STR = "<!ELEMENT root EMPTY>\n"
        +"<!ATTLIST root fixAttr CDATA #FIXED 'fixedValue'>\n";
    final String REQUIRED_DTD_STR = "<!ELEMENT root EMPTY>\n"
        +"<!ATTLIST root reqAttr CDATA #REQUIRED>\n";
    final String IMPLIED_NS_DTD_STR = "<!ELEMENT root EMPTY>\n"
        +"<!ATTLIST root "+NS_PREFIX+":attr CDATA #REQUIRED>\n";

    public void testValidFixedAttr() throws XMLStreamException
    {
        if (HAS_NON_NS_MODE) { // only test non-ns mode if supported
            _testValidFixedAttr(true, false);
        }
        _testValidFixedAttr(true, false);
        _testValidFixedAttr(true, true);
    }

    private void _testValidFixedAttr(boolean nsAware, boolean repairing) throws XMLStreamException
    {
        StringWriter strw = new StringWriter();

        // Ok either without being added:
        XMLStreamWriter2 sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
        sw.writeStartElement("root");
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();

        sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
        sw.writeEmptyElement("root");
        sw.writeEndDocument();
        sw.close();

        // or by using the exact same value
        sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
        sw.writeStartElement("root");
        sw.writeAttribute("fixAttr", "fixedValue");
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();
    }

/*    
    public void testInvalidFixedAttr() throws XMLStreamException
    {
        if (HAS_NON_NS_MODE) { // only test non-ns mode if supported
            _testInvalidFixedAttr(true, false);
        }
        _testInvalidFixedAttr(true, false);
        _testInvalidFixedAttr(true, true);
    }

    private void _testInvalidFixedAttr(boolean nsAware, boolean repairing)
            throws XMLStreamException
    {
        String modeDesc = String.format("[ns-aware? %s, repairing? %s]", nsAware, repairing);

        // Invalid case, trying to add some other value:

        // non-empty but not same
        StringWriter strw = new StringWriter();
        XMLStreamWriter2 sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
        sw.writeStartElement("root");
        try {
            sw.writeAttribute("fixAttr", "otherValue");
            fail(modeDesc+" Expected a validation exception when trying to add a #FIXED attribute with 'wrong' value");
        } catch (XMLValidationException vex) {
            // expected...
        }
        // Should not close, since stream is invalid now...

        // empty is not the same as leaving it out:
        strw = new StringWriter();
        sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
        sw.writeStartElement("root");
        try {
            sw.writeAttribute("fixAttr", "");
            fail(modeDesc+" Expected a validation exception when trying to add a #FIXED attribute with an empty value");
        } catch (XMLValidationException vex) {
            // expected...
        }

        // And finally, same for empty elem in case impl. is different
        strw = new StringWriter();
        sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
        sw.writeEmptyElement("root");
        try {
            sw.writeAttribute("fixAttr", "foobar");
            fail(modeDesc+" Expected a validation exception when trying to add a #FIXED attribute with an empty value");
        } catch (XMLValidationException vex) {
            // expected...
        }
    }

    public void testValidRequiredAttr() throws XMLStreamException
    {
        if (HAS_NON_NS_MODE) { // only test non-ns mode if supported
            _testValidRequiredAttr(true, false);
        }
        _testValidRequiredAttr(true, false);
        _testValidRequiredAttr(true, true);
    }

    private void _testValidRequiredAttr(boolean nsAware, boolean repairing)
        throws XMLStreamException
    {
        for (int i = 0; i < 3; ++i) {
            StringWriter strw = new StringWriter();

            // Ok if value is added:
            XMLStreamWriter2 sw = getDTDValidatingWriter(strw, REQUIRED_DTD_STR, nsAware, repairing);
            sw.writeStartElement("root");
            sw.writeAttribute("reqAttr", "value");
            sw.writeEndElement();
            sw.writeEndDocument();
            sw.close();

            // ... even if with empty value (for CDATA type, at least)
            sw = getDTDValidatingWriter(strw, REQUIRED_DTD_STR, nsAware, repairing);
            sw.writeStartElement("root");
            sw.writeAttribute("reqAttr", "");
            sw.writeEndElement();
            sw.writeEndDocument();
            sw.close();

            // and ditto for empty element:
            sw = getDTDValidatingWriter(strw, REQUIRED_DTD_STR, nsAware, repairing);
            sw.writeEmptyElement("root");
            sw.writeAttribute("reqAttr", "hii & haa");
            sw.writeEndDocument();
            sw.close();
        }
    }

    public void testInvalidRequiredAttr() throws XMLStreamException
    {
        if (HAS_NON_NS_MODE) { // only test non-ns mode if supported
            _testInvalidRequiredAttr(true, false);
        }
        _testInvalidRequiredAttr(true, false);
        _testInvalidRequiredAttr(true, true);
    }

    public void _testInvalidRequiredAttr(boolean nsAware, boolean repairing)
        throws XMLStreamException
    {
        String modeDesc = String.format("[ns-aware? %s, repairing? %s]", nsAware, repairing);

        // Invalid case: leaving the required attr out:
        StringWriter strw = new StringWriter();
        XMLStreamWriter2 sw = getDTDValidatingWriter(strw, REQUIRED_DTD_STR, nsAware, repairing);
        sw.writeStartElement("root");
        try {
            sw.writeEndElement();
            fail(modeDesc+" Expected a validation exception when omitting a #REQUIRED attribute");
        } catch (XMLValidationException vex) {
            // expected...
        }
        // Should not close, since stream is invalid now...
    }

    //Test to ensure that the namespace-prefix mapping works (to the degree
    // it can... wrt dtd-non-ns-awareness) with attributes.
    public void testValidNsAttr() throws XMLStreamException
    {
        _testValidNsAttr(false);
        _testValidNsAttr(true);
    }

    private void _testValidNsAttr(boolean repairing) throws XMLStreamException
    {
        StringWriter strw = new StringWriter();

        // Ok, as long as we use the right ns prefix... better also
        // output namespace declaration, in non-repairing mode.
        XMLStreamWriter2 sw = getDTDValidatingWriter(strw, IMPLIED_NS_DTD_STR, true, repairing);
        sw.writeStartElement("root");
        if (!repairing) {
            sw.writeNamespace(NS_PREFIX, NS_URI);
        }
        // prefix, uri, localname (for attrs!)
        sw.writeAttribute(NS_PREFIX, NS_URI, "attr", "value");
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();
    }

    public void testInvalidNsAttr() throws XMLStreamException
    {
        _testInvalidNsAttr(false);
        _testInvalidNsAttr(true);
    }

    private void _testInvalidNsAttr(boolean repairing) throws XMLStreamException
    {
        String modeDesc = "[namespace-aware, repairing? "+repairing+"]";

        // Invalid case, trying to use "wrong" prefix:

        StringWriter strw = new StringWriter();
        XMLStreamWriter2 sw = getDTDValidatingWriter(strw, IMPLIED_NS_DTD_STR, true, repairing);
        sw.writeStartElement("root");
        if (!repairing) {
            sw.writeNamespace(NS_PREFIX, NS_URI);
        }
        // prefix, uri, localname (for attrs!)
        try {
            sw.writeAttribute(NS_PREFIX2, NS_URI, "attr", "value");
            fail(modeDesc+" Expected a validation exception when trying to add an attribute with wrong ns prefix");
        } catch (XMLValidationException vex) {
            // expected...
        }
        // Should not close, since stream is invalid now...
    }
    */
}
