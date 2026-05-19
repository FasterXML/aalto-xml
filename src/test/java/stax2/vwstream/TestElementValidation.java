package stax2.vwstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.validation.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for element-level validation on the writer side: structural
 * checks driven by the {@code validateElementStart} /
 * {@code validateElementAndAttributes} / {@code validateElementEnd}
 * callbacks. Complements {@link TestAttributeValidation}.
 */
public class TestElementValidation
    extends BaseOutputTest
{
    final String ROOT_ONLY_DTD = "<!ELEMENT root EMPTY>\n";

    final String ROOT_CHILD_DTD =
            "<!ELEMENT root (child)>\n"
          + "<!ELEMENT child EMPTY>\n";

    @Test
    public void testValidRootElement() throws XMLStreamException
    {
        _testValidRootElement(true, false);
        _testValidRootElement(true, true);
    }

    private void _testValidRootElement(boolean nsAware, boolean repairing) throws XMLStreamException
    {
        StringWriter strw = new StringWriter();

        // <root/> via writeEmptyElement
        XMLStreamWriter2 sw = getDTDValidatingWriter(strw, ROOT_ONLY_DTD, nsAware, repairing);
        sw.writeEmptyElement("root");
        sw.writeEndDocument();
        sw.close();

        // <root></root> via writeStartElement + writeEndElement
        strw = new StringWriter();
        sw = getDTDValidatingWriter(strw, ROOT_ONLY_DTD, nsAware, repairing);
        sw.writeStartElement("root");
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();
    }

    @Test
    public void testInvalidRootElement() throws XMLStreamException
    {
        _testInvalidRootElement(true, false);
        _testInvalidRootElement(true, true);
    }

    private void _testInvalidRootElement(boolean nsAware, boolean repairing) throws XMLStreamException
    {
        String modeDesc = String.format("[ns-aware? %s, repairing? %s]", nsAware, repairing);

        // DTD only declares <root>; "wrong" is not declared.
        StringWriter strw = new StringWriter();
        XMLStreamWriter2 sw = getDTDValidatingWriter(strw, ROOT_ONLY_DTD, nsAware, repairing);
        try {
            sw.writeStartElement("wrong");
            fail(modeDesc + " Expected a validation exception writing an undeclared root element");
        } catch (XMLValidationException vex) {
            verifyException(vex, "Undefined element <wrong>");
        }
    }

    @Test
    public void testValidChildElement() throws XMLStreamException
    {
        _testValidChildElement(true, false);
        _testValidChildElement(true, true);
    }

    private void _testValidChildElement(boolean nsAware, boolean repairing) throws XMLStreamException
    {
        StringWriter strw = new StringWriter();
        XMLStreamWriter2 sw = getDTDValidatingWriter(strw, ROOT_CHILD_DTD, nsAware, repairing);
        sw.writeStartElement("root");
        sw.writeEmptyElement("child");
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();
    }

    @Test
    public void testInvalidChildElement() throws XMLStreamException
    {
        _testInvalidChildElement(true, false);
        _testInvalidChildElement(true, true);
    }

    private void _testInvalidChildElement(boolean nsAware, boolean repairing) throws XMLStreamException
    {
        String modeDesc = String.format("[ns-aware? %s, repairing? %s]", nsAware, repairing);

        StringWriter strw = new StringWriter();
        XMLStreamWriter2 sw = getDTDValidatingWriter(strw, ROOT_CHILD_DTD, nsAware, repairing);
        sw.writeStartElement("root");
        try {
            sw.writeEmptyElement("wrong");
            fail(modeDesc + " Expected a validation exception writing an undeclared child element");
        } catch (XMLValidationException vex) {
            verifyException(vex, "Undefined element <wrong>");
        }
    }

    @Test
    public void testMissingRequiredChild() throws XMLStreamException
    {
        _testMissingRequiredChild(true, false);
        _testMissingRequiredChild(true, true);
    }

    private void _testMissingRequiredChild(boolean nsAware, boolean repairing) throws XMLStreamException
    {
        String modeDesc = String.format("[ns-aware? %s, repairing? %s]", nsAware, repairing);

        // Open <root> then close it without writing the required <child>.
        StringWriter strw = new StringWriter();
        XMLStreamWriter2 sw = getDTDValidatingWriter(strw, ROOT_CHILD_DTD, nsAware, repairing);
        sw.writeStartElement("root");
        try {
            sw.writeEndElement();
            fail(modeDesc + " Expected a validation exception when closing element with missing required child");
        } catch (XMLValidationException vex) {
            // 18-May-2026, tatu: Extra space in there until Woodstox 7.2, so:
            verifyException(vex, "element </root>: Expected element");
            verifyException(vex, "<child>");
        }
    }

    @Test
    public void testTextInEmptyElement() throws XMLStreamException
    {
        _testTextInEmptyElement(true, false);
        _testTextInEmptyElement(true, true);
    }

    private void _testTextInEmptyElement(boolean nsAware, boolean repairing) throws XMLStreamException
    {
        String modeDesc = String.format("[ns-aware? %s, repairing? %s]", nsAware, repairing);

        // <root> is declared EMPTY; writing character content must fail.
        // This exercises validateElementAndAttributes returning CONTENT_ALLOW_NONE
        // when the start tag closes.
        StringWriter strw = new StringWriter();
        XMLStreamWriter2 sw = getDTDValidatingWriter(strw, ROOT_ONLY_DTD, nsAware, repairing);
        sw.writeStartElement("root");
        try {
            sw.writeCharacters("nope");
            fail(modeDesc + " Expected a validation exception writing text into EMPTY element");
        } catch (XMLValidationException vex) {
            verifyException(vex, "Element <root> has EMPTY");
            // expected
        } catch (XMLStreamException sex) {
            // Aalto's structural checker may surface this as a plain stream exception
            // before the validator gets a chance; accept either as long as it fails.
            assertTrue(sex.getMessage() != null && !sex.getMessage().isEmpty(),
                    modeDesc + " Expected non-empty error message");
        }
    }
}
