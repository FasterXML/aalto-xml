package test;

import java.io.*;
import java.util.List;

import javax.xml.stream.*;

import com.fasterxml.aalto.AsyncInputFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

/**
 * Simple helper test class for checking how stream reader handles xml
 * documents.
 */
public class TestAsyncReader
    implements XMLStreamConstants
{
    @SuppressWarnings("unchecked")
    protected int test(File file) throws Exception
    {
        int bytes = 0;

        InputStream in = new FileInputStream(file);

        AsyncXMLStreamReader asyncReader = new InputFactoryImpl().createAsyncXMLStreamReader();
        final AsyncInputFeeder feeder = asyncReader.getInputFeeder();
        
        final byte[] buf = new byte[3000];

        /*
        System.out.println("START: version = '"+sr.getVersion()
                           +"', xml-encoding = '"+sr.getCharacterEncodingScheme()
                           +"', input encoding = '"+sr.getEncoding()+"'");
        */

        main_loop:
        while (true) {
            int type;

            // May need to feed multiple segments:
            while ((type = asyncReader.next()) == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
                if (!feeder.needMoreInput()) { // sanity check for this test (not needed for real code)
                    throw new IllegalStateException("Got EVENT_INCOMPLETE but not expecting more input");
                }
//                System.out.println("READ-MORE: reader == "+asyncReader.toString());
                int len = in.read(buf, 1, 3);
                if (len < 0) {
                    System.err.println("Error: Unexpected EOF");
                    break main_loop;
                }
                feeder.feedInput(buf, 1, len);
                bytes += len;
            }

            if (type == END_DOCUMENT) {
                System.out.print("[END_DOCUMENT]");
                break;
            }
            System.out.print("["+type+"]");

            if (asyncReader.hasText()) {
                String text = asyncReader.getText();
                if (type == CHARACTERS || type == CDATA || type == COMMENT) {
                    System.out.println(" Text("+text.length()+") = '"+text+"'.");
                    if (text.length() >= 1) {
//                        System.out.println(" [first char code: 0x"+Integer.toHexString(text.charAt(0))+"]");
                    }
                } else if (type == SPACE) {
                    System.out.print(" Ws = '"+text+"'.");
                    char c = (text.length() == 0) ? ' ': text.charAt(text.length()-1);
                    if (c != '\r' && c != '\n') {
                        System.out.println();
                    }
                } else if (type == DTD) {
                    System.out.println(" DTD;");
                    List<Object> entities = (List<Object>) asyncReader.getProperty("javax.xml.stream.entities");
                    List<Object> notations = (List<Object>) asyncReader.getProperty("javax.xml.stream.notations");
                    int entCount = (entities == null) ? -1 : entities.size();
                    int notCount = (notations == null) ? -1 : notations.size();
                    System.out.print("  ("+entCount+" entities, "+notCount+" notations), sysid ");
                    System.out.print(", declaration = <<");
                    System.out.print(text);
                    System.out.println(">>");
                } else if (type == ENTITY_REFERENCE) {
                    // entity ref
                    System.out.println(" Entity ref: &"+asyncReader.getLocalName()+" -> '"+asyncReader.getText()+"'.");
                    //hasName = false; // to suppress further output
                } else { // PI?
                    ;
                }
            }

            if (type == PROCESSING_INSTRUCTION) {
                System.out.println(" PI target = '"+asyncReader.getPITarget()+"'.");
                System.out.println(" PI data = '"+asyncReader.getPIData()+"'.");
            } else if (type == START_ELEMENT) {
                String prefix = asyncReader.getPrefix();
                System.out.print('<');
                if (prefix != null && prefix.length() > 0) {
                    System.out.print(prefix);
                    System.out.print(':');
                }
                System.out.print(asyncReader.getLocalName());
    //System.out.println("[first char 0x"+Integer.toHexString(asyncReader.getLocalName().charAt(0))+"]");
                System.out.print(" {ns '");
                System.out.print(asyncReader.getNamespaceURI());
                System.out.print("'}> ");
                int count = asyncReader.getAttributeCount();
                int nsCount = asyncReader.getNamespaceCount();
                System.out.println(" ["+nsCount+" ns, "+count+" attrs]");
                // debugging:
                for (int i = 0; i < nsCount; ++i) {
                    System.out.println(" ns#"+i+": '"+asyncReader.getNamespacePrefix(i)
                                     +"' -> '"+asyncReader.getNamespaceURI(i)
                                     +"'");
                }
                for (int i = 0; i < count; ++i) {
                    String val = asyncReader.getAttributeValue(i);
                    System.out.print(" attr#"+i+": "+asyncReader.getAttributePrefix(i)
                                     +":"+asyncReader.getAttributeLocalName(i)
                                     +" ("+asyncReader.getAttributeNamespace(i)
                                     +") -> '"+val
                                     +"' ["+(val.length())+"]");
                    System.out.println(asyncReader.isAttributeSpecified(i) ?
                                       "[specified]" : "[Default]");
                }
            } else if (type == END_ELEMENT) {
                System.out.print("</");
                String prefix = asyncReader.getPrefix();
                if (prefix != null && prefix.length() > 0) {
                    System.out.print(prefix);
                    System.out.print(':');
                }
                System.out.print(asyncReader.getLocalName());
                System.out.print(" {ns '");
                System.out.print(asyncReader.getNamespaceURI());
                System.out.print("'}> ");
                int nsCount = asyncReader.getNamespaceCount();
                System.out.println(" ["+nsCount+" ns unbound]");
            } else if (type == START_DOCUMENT) { // only for multi-doc mode
                System.out.print("XML-DECL: version = '"+asyncReader.getVersion()
                                 +"', xml-decl-encoding = '"+asyncReader.getCharacterEncodingScheme()
                                 +"', app-encoding = '"+asyncReader.getEncoding()
                                 +"', stand-alone set: "+asyncReader.standaloneSet());
            }
        }
        return bytes;
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+TestAsyncReader.class+" [file]");
            System.exit(1);
        }

        try {
            int total = new TestAsyncReader().test(new File(args[0]));
            System.out.println();
            System.out.println("Bytes processed: "+total);
        } catch (Throwable t) {
          System.err.println("Error: "+t);
          t.printStackTrace();
        }
    }
}
