package stax2.wstream;

import java.io.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests that namespaces are written to the output stream in namespace
 * repairing mode. See [WSTX-193] for details.
 *
 * @author Christopher Paul Simmons
 */
public class TestNamespaceCopying
    extends BaseWriterTest
{
    XMLInputFactory _inputFactory;
    XMLOutputFactory _outputFactory;
    XMLEventFactory _eventFactory;

  @Override
  protected void setUp() throws Exception {
      _outputFactory = getOutputFactory();
      setRepairing(_outputFactory, true);
      _eventFactory = getEventFactory();
      _inputFactory = getInputFactory();
  }

  public void testStreamXMLNSDeclaration() throws Exception {
    final StringWriter stringWriter = new StringWriter();
    XMLEventWriter xmlWriter = _outputFactory.createXMLEventWriter(stringWriter);
    xmlWriter.add(_eventFactory.createStartDocument("UFT-8"));
    xmlWriter.add(_eventFactory.createStartElement("foo", "fooNS", "root", Collections.EMPTY_LIST.iterator(), Arrays.asList(_eventFactory.createNamespace("bar", "barNS")).iterator()));
    xmlWriter.add(_eventFactory.createNamespace("baz", "bazNS"));
    xmlWriter.add(_eventFactory.createCharacters("bar:qname"));
    xmlWriter.add(_eventFactory.createEndElement("foo", "fooNS", "root"));
    xmlWriter.add(_eventFactory.createEndDocument());

    // The document is just to inspect the result.
    final Document document = buildDocument(stringWriter.toString());

    Element documentElement = document.getDocumentElement();
    assertEquals("fooNS", getNamespaceForPrefix(documentElement, "foo"));
    // This line fails in 3.2.7
    assertEquals("barNS", getNamespaceForPrefix(documentElement, "bar"));
    assertEquals("bazNS", getNamespaceForPrefix(documentElement, "baz"));
  }

  private String getNamespaceForPrefix(final Element element, final String prefix) {
    return element.getAttributeNS("http://www.w3.org/2000/xmlns/", prefix);
  }

  private Document buildDocument(final String string) throws XMLStreamException, ParserConfigurationException {
    // Less painful to do this using XMLUnit if you use it.
    XMLEventReader reader = _inputFactory.createXMLEventReader(new StringReader(string));
    final DocumentBuilderFactory documentBuilder = DocumentBuilderFactory.newInstance();
    documentBuilder.setNamespaceAware(true);
    final Document document = documentBuilder.newDocumentBuilder().newDocument();
    XMLEventWriter writer = _outputFactory.createXMLEventWriter(new DOMResult(document));
    writer.add(reader);
    return document;
  }
}
