package test;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamWriter2;

import com.fasterxml.aalto.*;
import com.fasterxml.aalto.stax.*;

/**
 * Simple helper test class for checking how stream reader handles xml
 * documents.
 */
public class TestAsyncReader
    implements XMLStreamConstants
{
    final static int BLOCK_SIZE = 24;
    
    protected int test(File file) throws Exception
    {
        int bytes = 0;

        @SuppressWarnings("resource")
        InputStream in = new FileInputStream(file);

        AsyncXMLStreamReader<AsyncByteArrayFeeder> asyncReader = new InputFactoryImpl().createAsyncForByteArray();
        final AsyncByteArrayFeeder feeder = asyncReader.getInputFeeder();
        
        final byte[] buf = new byte[BLOCK_SIZE];

        // Let's just copy input as is
        XMLStreamWriter2 sw = (XMLStreamWriter2) new OutputFactoryImpl().createXMLStreamWriter(System.out, "UTF-8");
        
        main_loop:
        while (true) {
            int type;

            // May need to feed multiple segments:
            while ((type = asyncReader.next()) == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
                if (!feeder.needMoreInput()) { // sanity check for this test (not needed for real code)
                    throw new IllegalStateException("Got EVENT_INCOMPLETE but not expecting more input");
                }
//                System.out.println("READ-MORE: reader == "+asyncReader.toString());
                int len = in.read(buf);
                if (len < 0) {
                    System.err.println("Error: Unexpected EOF");
                    break main_loop;
                }
                bytes += len;
                feeder.feedInput(buf, 0, len);
            }
            sw.copyEventFromReader(asyncReader, false);
            if (type == END_DOCUMENT) {
                break;
            }
        }
        feeder.endOfInput();
        sw.close();
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
            System.err.println();
            System.err.println("Bytes processed: "+total);
        } catch (Throwable t) {
          System.err.println("Error: "+t);
          t.printStackTrace();
        }
    }
}
