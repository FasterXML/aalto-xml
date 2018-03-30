package stax2.wstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

/**
 * Base class for all StaxTest unit tests that test basic
 * stream (cursor) writer API functionality.
 *
 * @author Tatu Saloranta
 */
public abstract class BaseWriterTest
    extends stax2.BaseStax2Test
{
    public XMLStreamWriter2 getRepairingWriter(Writer w)
        throws XMLStreamException
    {
        XMLOutputFactory f = getOutputFactory();
        f.setProperty(XMLStreamProperties.XSP_NAMESPACE_AWARE, Boolean.TRUE);
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.TRUE);
        return (XMLStreamWriter2) f.createXMLStreamWriter(w);
    }

    public XMLStreamWriter2 getRepairingWriter(Writer w, String enc)
        throws XMLStreamException
    {
        XMLOutputFactory2 f = getOutputFactory();
        f.setProperty(XMLStreamProperties.XSP_NAMESPACE_AWARE, Boolean.TRUE);
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.TRUE);
        return f.createXMLStreamWriter(w, enc);
    }

    public XMLStreamWriter2 getNonRepairingWriter(Writer w, boolean nsAware)
        throws XMLStreamException
    {
        XMLOutputFactory f = getOutputFactory();
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.FALSE);
        f.setProperty(XMLStreamProperties.XSP_NAMESPACE_AWARE,
                      Boolean.valueOf(nsAware));
        return (XMLStreamWriter2) f.createXMLStreamWriter(w);
    }

    public XMLStreamWriter2 getNonRepairingWriter(Writer w, String enc, boolean nsAware)
        throws XMLStreamException
    {
        XMLOutputFactory2 f = getOutputFactory();
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.FALSE);
        f.setProperty(XMLStreamProperties.XSP_NAMESPACE_AWARE,
                      Boolean.valueOf(nsAware));
        return f.createXMLStreamWriter(w, enc);
    }

    public XMLStreamWriter2 getNonRepairingWriter(OutputStream os, String enc, boolean nsAware)
        throws XMLStreamException
    {
        XMLOutputFactory2 f = getOutputFactory();
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.FALSE);
        f.setProperty(XMLStreamProperties.XSP_NAMESPACE_AWARE,
                      Boolean.valueOf(nsAware));
        return (XMLStreamWriter2) f.createXMLStreamWriter(os, enc);
    }
}
