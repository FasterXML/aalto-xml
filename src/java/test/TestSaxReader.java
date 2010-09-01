package test;

import java.io.*;

import javax.xml.parsers.SAXParser;
import org.xml.sax.*;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.DefaultHandler2;

import com.fasterxml.aalto.sax.*;

/**
 * Simple helper test class for checking how the parser works when
 * used via SAX interface.
 */
public class TestSaxReader
{
    protected TestSaxReader() {
    }

    protected void test(File file)
        throws Exception
    {
        SAXParserFactoryImpl spf = new SAXParserFactoryImpl();
        SAXParser sp = spf.newSAXParser();
        MyContentHandler h = new MyContentHandler();
        sp.setProperty(SAXProperty.LEXICAL_HANDLER.toExternal(), (DeclHandler) h);
        InputStream in = new FileInputStream(file);
        sp.parse(new InputSource(in), h);
    }

    /*
    ////////////////////////////////////////////////
    // Helper class
    ////////////////////////////////////////////////
     */

    final static class MyContentHandler
        extends DefaultHandler2
                implements DeclHandler
    {
        public MyContentHandler() { }

        public void characters(char[] ch, int start, int length)
        {
            System.out.print("[CHARACTERS] (len "+length+"): '");
            printString(ch, start, length);
            System.out.println("'");
        }

        public void endDocument()
        {
            System.out.println("[END-DOCUMENT]");
        }

        public void endElement(String nsUri, String localName, String qName)
        {
            System.out.print("[END-ELEMENT] </");
            System.out.print(qName);
            if (nsUri != null) {
                System.out.print(" {");
                System.out.print(nsUri);
                System.out.print(" }");
            }
            System.out.println(">");
        }

        public void endPrefixMapping(String prefix)
        {
            System.out.println("[UNMAP-PREFIX '"+prefix+"']");
        }

        public void ignorableWhitespace(char[] ch, int start, int length)
        {
            System.out.println("[IGN-WS] (len "+length+")");
        }

        public void processingInstruction(String target, String data)
        {
            System.out.println("[PROC-INSTR '"+target+"' ...]");
        }

        public void setDocumentLocator(Locator locator) { }

        public void skippedEntity(String name)
        {
            System.out.println("[SKIPPED-entity '"+name+"']");
        }

        public void startDocument()
        {
            System.out.println("[START-DOC]");
        }

        public void startElement(String nsUri, String localName, String qName, Attributes attrs)
        {
            System.out.print("[START-ELEMENT] (");
            System.out.print(attrs.getLength());
            System.out.print(" attrs) <");
            System.out.print(qName);
            if (nsUri != null) {
                System.out.print(" {");
                System.out.print(nsUri);
                System.out.print(" }");
            }
            /*
            appendName(mText, localName, qName, namespaceURI);
            for (int i = 0, len = attrs.getLength(); i < len; ++i) {
                mText.append(' ');
                appendName(mText, attrs.getLocalName(i), attrs.getQName(i),
                           attrs.getURI(i));
                mText.append("='");
                mText.append(attrs.getValue(i));
                mText.append("'");
            }
            */
            System.out.println(">");
        }

        public void startPrefixMapping(String prefix, String uri)
        {
            System.out.println("[MAP-PREFIX '"+prefix+"'->'"+uri+"']");
        }

        public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName)
        {
            System.out.println("[UNPARSED-ENTITY-DECL '"+name+"']");
        }

        public void warning(SAXParseException e)
        {
            System.out.println("[WARNING: '"+e.getMessage()+"']");
        }

        // // // LexicalHandler:

        public void comment(char[] ch, int start, int length)
        {
            System.out.print("[COMMENT] '");
            printString(ch, start, length);
            System.out.println("'");
        }

        public void endCDATA()
        {
            System.out.println("[END-CDATA]");
        }

        public void endDTD()
        {
            System.out.println("[END-DTD]");
        }

        public void endEntity(String name)
        {
            System.out.println("[END-ENTITY '"+name+"']");
        }

        public void startCDATA()
        {
            System.out.println("[START-CDATA]");
        }

        public void startDTD(String name, String publicId, String systemId)
        {
            System.out.print("[START-DTD ");
            System.out.print(name);
            System.out.println("]");
        }

        public void startEntity(String name) 
        {
            System.out.println("[START-ENTITY '"+name+"']");
        }

        private void printString(char[] ch, int start, int length)
        {
            if (length < 60) {
                System.out.print(new String(ch, start, length));
            } else {
                StringBuffer sb = new StringBuffer(64);
                sb.append(ch, start, 28);
                sb.append("]..[");
                sb.append(ch, (start + length - 28), 28);
            }
        }
    }

    /*
    ////////////////////////////////////////////////
    // Main method
    ////////////////////////////////////////////////
     */

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+TestSaxReader.class+" [file]");
            System.exit(1);
        }

        try {
            new TestSaxReader().test(new File(args[0]));
            System.out.println("\nDone!");
        } catch (Throwable t) {
            System.err.println("Error: "+t);
            t.printStackTrace();
        }
    }
}
