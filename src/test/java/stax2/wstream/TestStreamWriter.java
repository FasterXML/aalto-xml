package stax2.wstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

/**
 * Unit test suite that focuses on testing additional methods that
 * StAX2 has for stream writers.
 */
public class TestStreamWriter
    extends BaseWriterTest
{
    /*
    //////////////////////////////////////////////////////////
    // First tests for simple accessors
    //////////////////////////////////////////////////////////
     */

    public void testGetEncoding()
        throws XMLStreamException
    {
        // Let's test with US-ASCII for fun
        final String ENC = "US-ASCII";

        for (int isWriter = 0; isWriter < 2; ++isWriter) {
            for (int i = 0; i < 3; ++i) {
                boolean ns = (i > 0);
                boolean repairing = (i == 2);
                XMLOutputFactory2 of = getFactory(ns, repairing);
                XMLStreamWriter2 w;

                if (isWriter > 0) {
                    StringWriter strw = new StringWriter();
                    w = of.createXMLStreamWriter(strw, ENC);
                } else {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    w = (XMLStreamWriter2)of.createXMLStreamWriter(bos, ENC);
                }
                assertEquals(ENC, w.getEncoding());
                // Need to output something, otherwise it'll be empty doc
                w.writeEmptyElement("root");
                w.close();

                /* Ok good, but how about the case where it's only
                 * passed for writeStartDocument()? Note: when wrapping
                 * a stream, factory has to use default (UTF-8).
                 */
                if (isWriter > 0) {
                    StringWriter strw = new StringWriter();
                    w = (XMLStreamWriter2)of.createXMLStreamWriter(strw);
                    w.writeStartDocument(ENC, "1.0");
                    assertEquals(ENC, w.getEncoding());
                } else {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    w = (XMLStreamWriter2)of.createXMLStreamWriter(bos);
                    w.writeStartDocument(ENC, "1.0");
                    assertEquals("UTF-8", w.getEncoding());
                }
                w.writeEmptyElement("root");
                w.close();
            }
        }
    }

    /**
     * Additional tests based on [WSTX-146]; JDK may report legacy
     * encoding names, we shouldn't report those but rather IANA
     * approved canonical equivalents.
     */
    public void testLegacyEncodings()
        throws Exception
    {
        String[] encs = new String[] { "UTF-8", "US-ASCII", "ISO-8859-1" };

        XMLOutputFactory2 outf = getFactory(true, false);
        XMLInputFactory2 inf = newInputFactory();

        for (int i = 0; i < encs.length; ++i) {
            String enc = encs[i];
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            XMLStreamWriter sw = outf.createXMLStreamWriter(new OutputStreamWriter(os, enc));
            sw.writeStartDocument("1.0");
            sw.writeEmptyElement("foo");
            sw.writeEndDocument();
            // Parse it and check the encoding
            XMLStreamReader sr = inf.createXMLStreamReader(new ByteArrayInputStream(os.toByteArray()));
            String act = sr.getCharacterEncodingScheme();
            if (!enc.equals(act)) {
                fail("Expected encoding to be returned correctly as \""+enc+"\", got \""+act+"\"");
            }
        }
    }

    /**
     * Since Woodstox doesn't yet actually implement the method, we'll
     * just call the method and do not expect and exception. Returned
     * object (or lack thereof) is not inspected
     */
    public void testGetLocation()
        throws XMLStreamException
    {
        for (int i = 0; i < 3; ++i) {
            boolean ns = (i > 0);
            boolean repairing = (i == 2);
            XMLOutputFactory2 of = getFactory(ns, repairing);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 w = (XMLStreamWriter2)of.createXMLStreamWriter(strw);
            XMLStreamLocation2 loc = w.getLocation();
            assertNotNull(loc);
            // Need to output something, otherwise it'll be empty doc
            w.writeEmptyElement("root");
            w.close();
        }
    }
        
    /*
    //////////////////////////////////////////////////////////
    // Then new output methods, or improved existing ones
    //////////////////////////////////////////////////////////
     */

    public void testCData()
        throws XMLStreamException
    {
        final String CDATA_TEXT = "Let's test it with some ] ]> data; <tag>s and && chars and all!";

        for (int i = 0; i < 2; ++i) {
            boolean ns = (i > 0);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 w = getNonRepairingWriter(strw, ns);
            
            w.writeStartDocument();
            w.writeStartElement("test");

            char[] cbuf = new char[CDATA_TEXT.length() + 10];
            CDATA_TEXT.getChars(0, CDATA_TEXT.length(), cbuf, 3);
            w.writeCData(cbuf, 3, CDATA_TEXT.length());
            w.writeEndElement();
            w.writeEndDocument();
            w.close();
            
            // And then let's parse and verify it all:
            
            XMLStreamReader sr = constructNsStreamReader(strw.toString(), true);
            assertTokenType(START_DOCUMENT, sr.getEventType());
            assertTokenType(START_ELEMENT, sr.next());
            
            // Now, parsers are allowed to report CHARACTERS or CDATA
            int tt = sr.next();
            if (tt != CHARACTERS && tt != CDATA) {
                assertTokenType(CDATA, tt); // to cause failure
            }
            assertFalse(sr.isWhiteSpace());
            assertEquals(CDATA_TEXT, getAndVerifyText(sr));
            assertTokenType(END_ELEMENT, sr.next());
            assertTokenType(END_DOCUMENT, sr.next());
        }
    }

    /**
     * This test was inspired by a failing regression test: it required
     * long enough COMMENT content to trigger buffar boundary problems
     */
    public void testLongerComment()
        throws XMLStreamException
    {
        doTestLonger(COMMENT, false, false, "UTF-8");
        doTestLonger(COMMENT, false, false, "ISO-8859-1");
        doTestLonger(COMMENT, false, false, "US-ASCII");
        doTestLonger(COMMENT, true, false, "UTF-8");
        doTestLonger(COMMENT, true, false, "ISO-8859-1");
        doTestLonger(COMMENT, true, false, "US-ASCII");
        doTestLonger(COMMENT, true, true, "UTF-8");
        doTestLonger(COMMENT, true, true, "ISO-8859-1");
        doTestLonger(COMMENT, true, true, "US-ASCII");
    }

    public void testLongerPI()
        throws XMLStreamException
    {
        doTestLonger(PROCESSING_INSTRUCTION, false, false, "UTF-8");
        doTestLonger(PROCESSING_INSTRUCTION, false, false, "ISO-8859-1");
        doTestLonger(PROCESSING_INSTRUCTION, false, false, "US-ASCII");
        doTestLonger(PROCESSING_INSTRUCTION, true, false, "UTF-8");
        doTestLonger(PROCESSING_INSTRUCTION, true, false, "ISO-8859-1");
        doTestLonger(PROCESSING_INSTRUCTION, true, false, "US-ASCII");
        doTestLonger(PROCESSING_INSTRUCTION, true, true, "UTF-8");
        doTestLonger(PROCESSING_INSTRUCTION, true, true, "ISO-8859-1");
        doTestLonger(PROCESSING_INSTRUCTION, true, true, "US-ASCII");
    }

    public void testCopy()
        throws XMLStreamException
    {
        final String XML =
            "<?xml version='1.0'?>\n"
            +"<!DOCTYPE root [  <!ENTITY foo 'value'> ]>\n"
            +"<root>\n"
            +"<!-- comment! --><?proc instr?>"
            +"Text: &amp; <leaf attr='xyz' xmlns:a='url:foo' a:xyz='1' />"
            +"<![CDATA[and <> there you have it!]]>"
            +"</root>"
            ;

        for (int i = 0; i < 2; ++i) {
            boolean ns = (i > 0);
            //boolean repairing = (i == 2);
            boolean repairing = (i == 1);
            XMLStreamReader2 sr = constructNsStreamReader(XML, ns);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 w;

            if (repairing) {
                w = getRepairingWriter(strw);
            } else {
                w = getNonRepairingWriter(strw, ns);
            }

            while (sr.hasNext()) {
                sr.next();
                w.copyEventFromReader(sr, false);
            }
            sr.close();
            w.close();
            String xmlOut = strw.toString();

            // And let's parse it to verify it's still well-formed...
            // (should also verify its accuracy...)
            sr = constructNsStreamReader(xmlOut, ns);
            streamThrough(sr);
        }
    }

    /**
     * Unit test for verifyin that writeRaw() works as expected.
     */
    public void testRaw()
        throws XMLStreamException
    {
        String RAW2 = "<elem>foo&amp;bar</elem>";

        for (int i = 0; i < 3; ++i) {
            boolean ns = (i > 0);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 w = (i == 2) ? getRepairingWriter(strw)
                : getNonRepairingWriter(strw, ns);
            w.writeStartDocument();
            w.writeStartElement("test");
            w.writeAttribute("attr", "value");
            w.writeRaw("this or &apos;that&apos;");
            char[] cbuf = new char[RAW2.length() + 10];
            RAW2.getChars(0, RAW2.length(), cbuf, 3);
            w.writeRaw(cbuf, 3, RAW2.length());
            w.writeEndElement();
            w.writeEndDocument();
            w.close();
            
            // And then let's parse and verify it all:
            XMLStreamReader sr = constructNsStreamReader(strw.toString(), true);
            assertTokenType(START_DOCUMENT, sr.getEventType());
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("test", sr.getLocalName());
            assertEquals(1, sr.getAttributeCount());
            assertEquals("attr", sr.getAttributeLocalName(0));
            assertEquals("value", sr.getAttributeValue(0));
            assertTokenType(CHARACTERS, sr.next());
            assertEquals("this or 'that'", getAndVerifyText(sr));
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("elem", sr.getLocalName());
            assertTokenType(CHARACTERS, sr.next());
            assertEquals("foo&bar", getAndVerifyText(sr));
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("elem", sr.getLocalName());
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("test", sr.getLocalName());
            assertTokenType(END_DOCUMENT, sr.next());
        }
    }

    /*
    //////////////////////////////////////////////////////////
    // Then custom quoting/escaping writers
    //////////////////////////////////////////////////////////
     */

    /**
     * First a simplish testing of how exotic characters are escaped
     * in attribute values.
     */
    public void testAttrValueWriterSimple()
        throws IOException, XMLStreamException
    {
        // Let's just ensure escaping is done for chars that need it
        //String IN = "Ok, lessee \u00A0; -- \t and this: \u0531.";
        String IN = "Ok, nbsp: \u00A0; and 'quotes' and \"doubles\" too; and multi-bytes too: [\u0531]";
        doTestAttrValueWriter("ISO-8859-1", IN);
        doTestAttrValueWriter("UTF-8", IN);
        doTestAttrValueWriter("US-ASCII", IN);
    }

    /**
     * And then bit more advanced test for things that need special
     * support for round-tripping
     */
    public void testAttrValueWriterTabsEtc()
        throws IOException, XMLStreamException
    {
        String IN = "How about tabs: [\t] or cr+lf [\r\n]";
        doTestAttrValueWriter("ISO-8859-1", IN);
        doTestAttrValueWriter("UTF-8", IN);
        doTestAttrValueWriter("US-ASCII", IN);
    }

    /*
    //////////////////////////////////////////////////////////
    // Non-test methods:
    //////////////////////////////////////////////////////////
     */

    public XMLOutputFactory2 getFactory(boolean nsAware, boolean repairing)
        throws XMLStreamException
    {
        XMLOutputFactory2 f = getOutputFactory();
        f.setProperty(XMLStreamProperties.XSP_NAMESPACE_AWARE,
                      Boolean.valueOf(nsAware));
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.valueOf(repairing));
        return f;
    }

    private void doTestAttrValueWriter(String enc, String IN)
        throws IOException, XMLStreamException
    {
        // First, let's explicitly pass the encoding...
        XMLOutputFactory of = getFactory(false, false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer w = new OutputStreamWriter(out, enc);
        /* 26-Mar-2008, tatus: Note: we may get legacy encoding
         *   names from here (like "ASCII" over "US-ASCII" etc).
         *  Additionally, should we count on output factory knowing
         *  how to find underlying encoding from OutputStreamWriter?
         *  Could (should?) explicitly pass encoding instead.
         */
        XMLStreamWriter sw = of.createXMLStreamWriter(w);

        // So shouldn't we do this?
        //XMLStreamWriter sw = of.createXMLStreamWriter(w, enc);
        
        sw.writeStartDocument(enc, "1.0");
        sw.writeStartElement("elem");
        sw.writeAttribute("attr", IN);
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();
        w.close();
        
        // Can we parse it ok?
        XMLInputFactory ifact = getInputFactory();
        XMLStreamReader sr = ifact.createXMLStreamReader(new ByteArrayInputStream(out.toByteArray()), enc);

        // First, let's ensure we see the encoding:
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertEquals(enc, sr.getCharacterEncodingScheme());

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(1, sr.getAttributeCount());
        String attrValue = sr.getAttributeValue(0);
        if (!IN.equals(attrValue)) {
            failStrings("Incorrect writing/reading of attribute value (encoding '"+enc+"')",
                        IN, attrValue);
        }
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    public void doTestLonger(int type, boolean ns, boolean repair, String enc)
        throws XMLStreamException
    {
        final String TEXT =
" Table of types of doubts\n"
+"doubt: specific error or issue with the test case\n"
+"extension: uses an extension feature\n"
+"gray-area: the spec does not give enough precision to distinguish correct behavior on the indicated detail\n"
+"processor-specific: processors are required to provide a unique value (should be marked as \"manual\" compare in catalog)\n"
+"serial: processor has options regarding serialization (This doubt only used for detail issues, not general discretion about encoding.)"
            ;

        for (int i = 0; i < 2; ++i) {
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 w;
            if (repair) {
                w = getRepairingWriter(strw, enc);
            } else {
                w = getNonRepairingWriter(strw, enc, ns);
            }
            w.writeStartDocument(enc, "1.0");
            if (type == COMMENT) {
                w.writeComment(TEXT);
            } else {
                w.writeProcessingInstruction("pi", TEXT);
            }
            w.writeEmptyElement("root");
            w.writeEndDocument();
            w.close();
            
            // And then let's parse and verify the contents:
            XMLStreamReader sr = constructNsStreamReader(strw.toString(), true);
            assertTokenType(START_DOCUMENT, sr.getEventType());
            
            if (type == COMMENT) {
                assertTokenType(COMMENT, sr.next());
                assertEquals(TEXT, getAndVerifyText(sr));
            } else {
                assertTokenType(PROCESSING_INSTRUCTION, sr.next());
                // PI data excludes leading space... need to trim
                assertEquals(TEXT.trim(), sr.getPIData().trim());
            }
            assertTokenType(START_ELEMENT, sr.next());
            assertTokenType(END_ELEMENT, sr.next());
            assertTokenType(END_DOCUMENT, sr.next());
            sr.close();
        }
    }
}
