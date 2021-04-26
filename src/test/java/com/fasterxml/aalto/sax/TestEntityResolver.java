package com.fasterxml.aalto.sax;

import java.io.*;
import java.util.concurrent.CountDownLatch;

import javax.xml.parsers.SAXParser;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.fasterxml.aalto.AaltoInputProperties;

/**
 * Simple unit tests to verify that most fundamental parsing functionality
 * works via Woodstox SAX implementation.
 */
public class TestEntityResolver
    extends base.BaseTestCase
{
    public void testWithDummyExtSubset()
        throws Exception
    {
        final String XML =
            "<!DOCTYPE root PUBLIC '//some//public//id' 'no-such-thing.dtd'>\n"
            +"<root />"
            ;

        SAXParserFactoryImpl spf = new SAXParserFactoryImpl();
        spf.setNamespaceAware(true);
        SAXParser sp = spf.newSAXParser();
        DefaultHandler h = new DefaultHandler();

        // First: let's verify that we get an exception for
        // unresolved reference...
        try {
            sp.parse(new InputSource(new StringReader(XML)), h);
        } catch (SAXException e) {
            verifyException(e, "No such file or directory");
        }

        // And then with dummy resolver; should work ok now
        sp = spf.newSAXParser();
        sp.getXMLReader().setEntityResolver(new MyResolver("   "));
        h = new DefaultHandler();
        try {
            sp.parse(new InputSource(new StringReader(XML)), h);
        } catch (SAXException e) {
            fail("Should not have failed with entity resolver, got ("+e.getClass()+"): "+e.getMessage());
        }
    }

    // [aalto-xml#65]: allow retaining GEs in attribute values
    public void testRetainAttributeEntityReference()
            throws Exception
    {
        final String XML =
                "<!DOCTYPE root PUBLIC '//some//public//id' 'no-such-thing.dtd'>\n"
                        +"<root b=\"&replace-me;\" />";

        SAXParserFactoryImpl spf = new SAXParserFactoryImpl();
        // should be disabled by default:
        assertEquals(false,
                spf.getFeature(AaltoInputProperties.P_RETAIN_ATTRIBUTE_GENERAL_ENTITIES));
        SAXParser sp = spf.newSAXParser();
        DefaultHandler h = new DefaultHandler();
        
        try {
            sp.parse(new InputSource(new StringReader(XML)), h);
            fail();
        } catch (SAXException e) {
            verifyException(e, "General entity reference (&replace-me;) encountered in entity expanding mode: operation not (yet) implemented\n at [row,col {unknown-source}]: [2,22]");
        }

        SAXParserFactoryImpl spfKeepEntityReferences = new SAXParserFactoryImpl();
        spfKeepEntityReferences.setFeature(AaltoInputProperties.P_RETAIN_ATTRIBUTE_GENERAL_ENTITIES, true);
        assertEquals(true,
                spfKeepEntityReferences.getFeature(AaltoInputProperties.P_RETAIN_ATTRIBUTE_GENERAL_ENTITIES));

        SAXParser spKeepEntityReferences = spfKeepEntityReferences.newSAXParser();

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        spKeepEntityReferences.parse(new InputSource(new StringReader(XML)), new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                assertEquals("root", localName);
                assertEquals("root", qName);
                assertEquals(1, attributes.getLength());
                assertEquals("&replace-me;", attributes.getValue(0));

                countDownLatch.countDown();
            }
        });
        
        assertEquals(0, countDownLatch.getCount());
    }
    
    static class MyResolver
        implements EntityResolver
    {
        final String mContents;

        public MyResolver(String c) {
            mContents = c;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId)
        {
            return new InputSource(new StringReader(mContents));
        }
    }
}
