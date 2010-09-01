package wstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

/**
 * These tests try to specifically verify that name encoding and decoding
 * work as expected. Although basic Stax or Stax2 test should cover
 * some aspects, they may not force exact stream reader implementations
 * to use and thus might not uncover all problems Aalto may have.
 */
public class TestNameEncoding
    extends base.BaseTestCase
{
    // // // First, valid names for different classes of encodings

    final static String NAME_ASCII = "_a123";
    // "A" and second "c" of "Accents" with some decorations...
    final static String NAME_LATIN1 = "\u00C0"+"c\u00C7ents_";
    final static String NAME_UTF8 = "\u0c12\u114c_x";

    // // // And then similarly invalid ones
    // // // (note: ensured to be invalid in xml 1.1, too, to minimize
    // // // problems if and when parser supports 1.1)

    final static String INVALID_NAME_ASCII = "a b"; // no spaces
    final static String INVALID_NAME2_ASCII = "a:b"; // no no spaces
    final static String INVALID_NAME_LATIN1 = "abc\u00BEx"; // ctrl-char
    final static String INVALID_NAME_UTF8 = "+a\u2ff0";

    public void testValidElemNameEncodingUtf8()
        throws Exception
    {
        verifyValidElemName(NAME_ASCII, ENC_UTF8);
        verifyValidElemName(NAME_LATIN1, ENC_UTF8);
        verifyValidElemName(NAME_UTF8, ENC_UTF8);
    }

    public void testValidElemNameEncodingLatin1()
        throws Exception
    {
        verifyValidElemName(NAME_ASCII, ENC_LATIN1);
        verifyValidElemName(NAME_LATIN1, ENC_LATIN1);
    }

    public void testValidElemNameEncodingAscii()
        throws Exception
    {
        verifyValidElemName(NAME_ASCII, ENC_ASCII);
    }

    // // // Then tests for invalid name chars

    public void testInvalidElemNameEncodingUtf8()
        throws Exception
    {
        /* With UTF-8, only invalidity itself matters, as
         * all legal Unicode chars can be encoded
         */
        verifyInvalidElemName(INVALID_NAME_ASCII, ENC_UTF8);
        verifyInvalidElemName(INVALID_NAME_LATIN1, ENC_UTF8);
        verifyInvalidElemName(INVALID_NAME_UTF8, ENC_UTF8);
    }

    public void testInvalidElemNameEncodingLatin1()
        throws Exception
    {
        /* Validity checks should fail for Latin1/Ascii expressable Strings
         * that contain non-name chars:
         */
        verifyInvalidElemName(INVALID_NAME_ASCII, ENC_LATIN1);
        verifyInvalidElemName(INVALID_NAME_LATIN1, ENC_LATIN1);

        /* With Latin1, chars beyond 8-bit range can't be encoded;
         * hence otherwise valid UTF-8 name should fail
         */
        verifyInvalidElemName(NAME_UTF8, ENC_LATIN1);
    }

    public void testInvalidElemNameEncodingAscii()
        throws Exception
    {
        /* Validity checks should fail for Ascii expressable Strings
         * that contain non-name chars:
         */
        verifyInvalidElemName(INVALID_NAME_ASCII, ENC_ASCII);

        /* With Ascii, chars beyond 8-bit range can't be encoded;
         * hence otherwise valid Latin1/UTF-8 name should fail
         */
        verifyInvalidElemName(NAME_LATIN1, ENC_ASCII);
        verifyInvalidElemName(NAME_UTF8, ENC_ASCII);
    }

    /*
    ////////////////////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////////////////////
     */

    void verifyValidElemName(String name, String enc)
        throws IOException, XMLStreamException
    {
        verifyValidElemName(writeSimpleDoc(name, enc), name);
    }

    void verifyInvalidElemName(String name, String enc)
        throws IOException, XMLStreamException
    {
        try {
            writeSimpleDoc(name, enc);
            fail("Expected exception for invalid element name ("+name+"), encoding "+enc);
        } catch (XMLStreamException xse) {
            String msg = xse.getMessage();
            String exp1 = "invalid name";
            String exp2 = "illegal name";

            String lmsg = msg.toLowerCase();
            if (lmsg.indexOf(exp1) < 0 && lmsg.indexOf(exp2) < 0) {
                fail("Received exception for name \""+name+"\", encoding enc; but message did not contain fragment '"+exp1+"' or fragment '"+exp2+"'. Instead was: "+msg);
            }
        }
    }

    byte[] writeSimpleDoc(String name, String enc)
        throws IOException, XMLStreamException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter sw = getNewOutputFactory().createXMLStreamWriter(bos, enc);
        sw.writeStartDocument(enc, "1.0");
        sw.writeStartElement(name);
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();
        bos.close();
        return bos.toByteArray();
    }

    void verifyValidElemName(byte[] content, String name)
        throws IOException, XMLStreamException
    {
        // note: must specify encoding so parsers knows which decoder to use
        XMLStreamReader sr = createReader(content, name);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(name, sr.getLocalName());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals(name, sr.getLocalName());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    XMLStreamReader createReader(byte[] content, String enc)
        throws IOException, XMLStreamException
    {
        // Let's ensure it's a new factory, to minimize caching probs
        XMLInputFactory2 f = getNewInputFactory();
        return f.createXMLStreamReader(new ByteArrayInputStream(content));
    }
}

