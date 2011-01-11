package stream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

/**
 * These tests try to specifically verify that name encoding and decoding
 * work as expected
 */
public class TestNameDecoding
    extends base.BaseTestCase
{
    final static String NAME_ASCII = "funnyAsciiName_123";
    // "A" and second "c" of "Accents" with some decorations...
    final static String NAME_LATIN1 = "some\u00C0"+"c\u00C7ents";
    final static String NAME_UTF8 = "\u1165_\ud7a2";

    public void testValidElemNameDecodingUtf8()
        throws Exception
    {
        verifyValidElemName(NAME_ASCII, ENC_UTF8);
        verifyValidElemName(NAME_LATIN1, ENC_UTF8);
        verifyValidElemName(NAME_UTF8, ENC_UTF8);
    }

    public void testValidElemNameDecodingLatin1()
        throws Exception
    {
        verifyValidElemName(NAME_ASCII, ENC_LATIN1);
        verifyValidElemName(NAME_LATIN1, ENC_LATIN1);
    }

    public void testValidElemNameDecodingAscii()
        throws Exception
    {
        verifyValidElemName(NAME_ASCII, ENC_ASCII);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    void verifyValidElemName(String name, String enc)
        throws IOException, XMLStreamException
    {
        // note: must specify encoding so parsers knows which decoder to use
        String DOC = "<?xml version='1.0' encoding='"+enc+"' ?><"+name+" />";
        XMLStreamReader sr = createReader(DOC, enc);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(name, sr.getLocalName());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals(name, sr.getLocalName());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    XMLStreamReader createReader(String content, String enc)
        throws IOException, XMLStreamException
    {
        // Let's ensure it's a new factory, to minimize caching probs
        XMLInputFactory2 f = getNewInputFactory();
        byte[] data = content.getBytes(enc);
        return f.createXMLStreamReader(new ByteArrayInputStream(data));
    }
}

