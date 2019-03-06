module com.fasterxml.aalto {
    requires java.xml;
    requires org.codehaus.stax2;
    exports com.fasterxml.aalto;
    exports com.fasterxml.aalto.async;
    exports com.fasterxml.aalto.dom;
    exports com.fasterxml.aalto.evt;
//    exports com.fasterxml.aalto.impl;
    exports com.fasterxml.aalto.in;
    exports com.fasterxml.aalto.io;
    exports com.fasterxml.aalto.out;
    exports com.fasterxml.aalto.sax;
    exports com.fasterxml.aalto.stax;
    exports com.fasterxml.aalto.util;
    provides javax.xml.stream.XMLEventFactory with com.fasterxml.aalto.stax.EventFactoryImpl;
    provides javax.xml.stream.XMLInputFactory with com.fasterxml.aalto.stax.InputFactoryImpl;
    provides javax.xml.stream.XMLOutputFactory with com.fasterxml.aalto.stax.OutputFactoryImpl;
}
