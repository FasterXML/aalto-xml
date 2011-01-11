package test;

import java.io.*;
import java.util.List;

import javax.xml.stream.*;

import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.async.*;
import com.fasterxml.aalto.in.*;
import com.fasterxml.aalto.stax.StreamReaderImpl;

/**
 * Simple helper test class for checking how stream reader handles xml
 * documents.
 */
public class TestAsyncReader
    implements XMLStreamConstants
{
    @SuppressWarnings("unchecked")
	protected int test(File file)
        throws Exception
    {
        int total = 0;

        InputStream in = new FileInputStream(file);
        ReaderConfig cfg = new ReaderConfig();
        cfg.setActualEncoding("UTF-8");

        AsyncUtfScanner asc = new AsyncUtfScanner(cfg);
        StreamReaderImpl sr = new StreamReaderImpl(asc);

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
            while ((type = sr.next()) == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
                System.out.println("DEBUG: scanner -> "+asc.toString());
                int len = in.read(buf, 1, 3);
                if (len < 0) {
                    System.err.println("Error: Unexpected EOF");
                    break main_loop;
                }
                asc.addInput(buf, 1, len);
            }

            if (type == END_DOCUMENT) {
                System.out.print("[END_DOCUMENT]");
                break;
            }
            total += type; // so it won't be optimized out...

            System.out.print("["+type+"]");

            if (sr.hasText()) {
                String text = sr.getText();
                total += text.length(); // to prevent dead code elimination
                if (type == CHARACTERS || type == CDATA || type == COMMENT) {
                    System.out.println(" Text("+text.length()+") = '"+text+"'.");
                    if (text.length() >= 1) {
                        System.out.println(" [first char code: 0x"+Integer.toHexString(text.charAt(0))+"]");
                    }
                } else if (type == SPACE) {
                    System.out.print(" Ws = '"+text+"'.");
                    char c = (text.length() == 0) ? ' ': text.charAt(text.length()-1);
                    if (c != '\r' && c != '\n') {
                        System.out.println();
                    }
                } else if (type == DTD) {
                    System.out.println(" DTD;");
                    List<Object> entities = (List<Object>) sr.getProperty("javax.xml.stream.entities");
                    List<Object> notations = (List<Object>) sr.getProperty("javax.xml.stream.notations");
                    int entCount = (entities == null) ? -1 : entities.size();
                    int notCount = (notations == null) ? -1 : notations.size();
                    System.out.print("  ("+entCount+" entities, "+notCount
                                       +" notations), sysid ");
                    System.out.print(", declaration = <<");
                    System.out.print(text);
                    System.out.println(">>");
                } else if (type == ENTITY_REFERENCE) {
                    // entity ref
                    System.out.println(" Entity ref: &"+sr.getLocalName()+" -> '"+sr.getText()+"'.");
                    //hasName = false; // to suppress further output
                } else { // PI?
                    ;
                }
            }

            if (type == PROCESSING_INSTRUCTION) {
                System.out.println(" PI target = '"+sr.getPITarget()+"'.");
                System.out.println(" PI data = '"+sr.getPIData()+"'.");
            } else if (type == START_ELEMENT) {
                String prefix = sr.getPrefix();
                System.out.print('<');
                if (prefix != null && prefix.length() > 0) {
                    System.out.print(prefix);
                    System.out.print(':');
                }
                System.out.print(sr.getLocalName());
                //System.out.println("[first char 0x"+Integer.toHexString(sr.getLocalName().charAt(0))+"]");
                System.out.print(" {ns '");
                System.out.print(sr.getNamespaceURI());
                System.out.print("'}> ");
                int count = sr.getAttributeCount();
                int nsCount = sr.getNamespaceCount();
                System.out.println(" ["+nsCount+" ns, "+count+" attrs]");
                // debugging:
                for (int i = 0; i < nsCount; ++i) {
                    System.out.println(" ns#"+i+": '"+sr.getNamespacePrefix(i)
                                     +"' -> '"+sr.getNamespaceURI(i)
                                     +"'");
                }
                for (int i = 0; i < count; ++i) {
                    String val = sr.getAttributeValue(i);
                    System.out.print(" attr#"+i+": "+sr.getAttributePrefix(i)
                                     +":"+sr.getAttributeLocalName(i)
                                     +" ("+sr.getAttributeNamespace(i)
                                     +") -> '"+val
                                     +"' ["+(val.length())+"]");
                    System.out.println(sr.isAttributeSpecified(i) ?
                                       "[specified]" : "[Default]");
                }
            } else if (type == END_ELEMENT) {
                System.out.print("</");
                String prefix = sr.getPrefix();
                if (prefix != null && prefix.length() > 0) {
                    System.out.print(prefix);
                    System.out.print(':');
                }
                System.out.print(sr.getLocalName());
                System.out.print(" {ns '");
                System.out.print(sr.getNamespaceURI());
                System.out.print("'}> ");
                int nsCount = sr.getNamespaceCount();
                System.out.println(" ["+nsCount+" ns unbound]");
            } else if (type == START_DOCUMENT) { // only for multi-doc mode
                System.out.print("XML-DECL: version = '"+sr.getVersion()
                                 +"', xml-decl-encoding = '"+sr.getCharacterEncodingScheme()
                                 +"', app-encoding = '"+sr.getEncoding()
                                 +"', stand-alone set: "+sr.standaloneSet());
            }
        }
        return total;
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
            System.out.println("Total: "+total);
        } catch (Throwable t) {
          System.err.println("Error: "+t);
          t.printStackTrace();
        }
    }
}
