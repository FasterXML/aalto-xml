package stream;

import java.io.StringReader;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLInputFactory2;

import com.fasterxml.aalto.AaltoInputProperties;

// Mostly for [aalto-xml#65]
public class TestGeneralEntityHandling extends base.BaseTestCase
{
    private final XMLInputFactory2 VANILLA_F = newInputFactory();

    private final XMLInputFactory2 RETAIN_ATTR_GE_F = newInputFactory();
    {
        RETAIN_ATTR_GE_F.setProperty(AaltoInputProperties.P_RETAIN_ATTRIBUTE_GENERAL_ENTITIES,
                true);
    }
    
    public void testAttributeGEHandling() throws Exception
    {
        final String DOC = "<root attr='Entity: &ent;'>Text</root>";

        // First: with Vanilla, should just fail
        XMLStreamReader sr = VANILLA_F.createXMLStreamReader(
                new StringReader(DOC));
        try {
            sr.next();
            fail("Should not pass");
        } catch (XMLStreamException e) {
            verifyException(e, "General entity reference (&ent;) encountered");
        }
        sr.close();

        // But with new (1.3) setting, can be tolerated
        sr = RETAIN_ATTR_GE_F.createXMLStreamReader(
                new StringReader(DOC));
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertEquals(1, sr.getAttributeCount());
        assertEquals("Entity: &ent;", sr.getAttributeValue(0));

        assertTokenType(CHARACTERS, sr.next());
        // Assume that value is not split (as per impl)
        assertEquals("Text", sr.getText());

        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }
}
