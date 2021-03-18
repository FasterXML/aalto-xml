package com.fasterxml.aalto.sax;

import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class TestSAXParserFactoryImpl extends base.BaseTestCase {
    
    public void testSetGetFeatureExternalGeneralEntities() throws SAXNotRecognizedException, SAXNotSupportedException {
        SAXParserFactoryImpl saxParserFactory = new SAXParserFactoryImpl();
        saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        assertFalse(saxParserFactory.getFeature("http://xml.org/sax/features/external-general-entities"));

        saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", true);
        assertTrue(saxParserFactory.getFeature("http://xml.org/sax/features/external-general-entities"));
    }
    
}
