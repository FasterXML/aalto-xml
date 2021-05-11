package com.fasterxml.aalto.sax;

import com.fasterxml.aalto.AaltoInputProperties;

public class TestSAXParserFactoryImpl extends base.BaseTestCase
{
    // [aalto-xml#65]
    public void testSetGetFeatureExternalGeneralEntities() throws Exception
    {
        SAXParserFactoryImpl saxParserFactory = new SAXParserFactoryImpl();
        assertFalse(saxParserFactory.getFeature(AaltoInputProperties.P_RETAIN_ATTRIBUTE_GENERAL_ENTITIES));

        saxParserFactory.setFeature(AaltoInputProperties.P_RETAIN_ATTRIBUTE_GENERAL_ENTITIES, true);
        assertTrue(saxParserFactory.getFeature(AaltoInputProperties.P_RETAIN_ATTRIBUTE_GENERAL_ENTITIES));
    }
   
}
