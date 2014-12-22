package test;

import java.io.*;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

public class TestEventReader
{
    final XMLInputFactory mFactory;

    public TestEventReader()
    {
        super();
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.fasterxml.aalto.stax.InputFactoryImpl");
        XMLInputFactory f = XMLInputFactory.newInstance();
        mFactory = f;
        //f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        //f.setProperty(XMLInputFactory.REPORTER, new TestReporter());

        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);

        System.out.println("Factory instance: "+f.getClass());
        System.out.println("  coalescing: "+f.getProperty(XMLInputFactory.IS_COALESCING));
    }

	public void test(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+getClass().getName()+" [file]");
            System.exit(1);
        }
        String filename = args[0];
        File file = new File(filename);
        Reader fin = new java.io.FileReader(file);

        // Let's pass generated system id:
        @SuppressWarnings("deprecation")
        XMLEventReader er = mFactory.createXMLEventReader(file.toURL().toString(), fin);

        Writer out = new PrintWriter(System.out);

        //out.write("[START]\n");
        while (er.hasNext()) {
            XMLEvent evt = er.nextEvent();
// Uncomment for debugging:
//System.err.println("["+evt.getEventType()+"]: '");
            if (evt.isStartElement()) {
                StartElement elem = (StartElement) evt;
                QName n = elem.getName();
                System.err.println("QName = ["+n+", ns='"+n.getNamespaceURI()+"']");
                Iterator<?> it = elem.getNamespaces();
                @SuppressWarnings("unused")
                int count = 0;
                while (it.hasNext()) {
                    it.next();
                    ++count;
                }
//System.err.println("[Ns count: "+count+"]");
            } else if (evt.isCharacters()) {
                Characters chars = evt.asCharacters();
                int len = chars.getData().length();
                out.write("[CHARACTERS("+len+"), ws: "+chars.isWhiteSpace()+", iws: "+chars.isIgnorableWhiteSpace()+"]");
            } else if (evt instanceof EntityReference) {
		EntityReference eref = (EntityReference) evt;
		out.write("[ENTITY-REF '"+eref.getName()+"']");
            }
            //out.write("{ENC:");
            evt.writeAsEncodedUnicode(out);
            //out.write("}");
            //out.write("'\n");
            //out.write('\n');
            //out.flush();
        }
        //out.write("[END]\n");
        out.flush();
        fin.close();
    }

    public static void main(String[] args) throws Exception
    {
        // Uncomment for infinite looping (stress test)

        /*
        int count = 0;
        while (true) {
            ++count;
            System.err.println("#"+count);
        */
            new TestEventReader().test(args);
            /*
        }
            */
    }
}
