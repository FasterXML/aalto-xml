package stax2.wstream;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

/**
 * Set of unit tests that checks that configuring of
 * {@link XMLOutputFactory2} works ok.
 *<p>
 * Note: for now there isn't much meat in this unit test: it's mostly
 * used to do simple smoke testing for profile setters.
 */
public class TestConfig
    extends BaseWriterTest
{
    public void testProfiles() throws XMLStreamException
    {
        // configureForXmlConformance
        XMLOutputFactory2 ofact = newOutputFactory();
        ofact.configureForXmlConformance();

        // configureForRobustness
        ofact = newOutputFactory();
        ofact.configureForRobustness();

        // configureForSpeed
        ofact = newOutputFactory();
        ofact.configureForSpeed();
    }
}
