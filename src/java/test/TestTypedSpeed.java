package test;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

/**
 * Simple typed information access stress test, useful for profiling, as well
 * as for
 * quickly checking high-level performance effects of changes (albeit
 * not very accurately, obviously -- need longer running composite
 * tests for such verifications).
 *<p>
 * Type of data is auto-detected, and is assumed to be homogenous. Basically,
 * data is either within attributes, or as element content, but not both.
 * In either case structure should be shallow, with the root and only
 * immediate leaf-level elements containing attribute or element data.
 * Type of this data is auto-detected from the first instance; data must
 * be in canonical format to be properly recognized (non-first values
 * can be non-canonical).
 */
public class TestTypedSpeed
    implements XMLStreamConstants
{
    /**
     * Number of repetitions to run per test. Dynamically variable,
     * based on observed runtime, to try to keep it high enough.
     */
    private int REPS;

    private final static int TEST_PER_GC = 7;

    final static int TYPE_BOOLEAN = 1;
    final static int TYPE_INT = 1;

    /**
     * Let's keep per-run times above 50 milliseconds
     */
    //final static int MIN_RUN_TIME = 50;
    final static int MIN_RUN_TIME = 5;

    /**
     * Let's keep per-run times below 300 milliseconds
     */
    //final static int MAX_RUN_TIME = 300;
    final static int MAX_RUN_TIME = 1000;

    final XMLInputFactory mInputFactory;

    final ByteArrayInputStream mIn;

    /**
     * Data in attributes? If true, yes; if no, in elements
     */
    boolean mUseAttr;

    int mType;

    private TestTypedSpeed(byte[] data)
    {
        mInputFactory = new com.fasterxml.aalto.stax.InputFactoryImpl();
        mInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        mInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        // Just in case:
        mInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        mIn = new ByteArrayInputStream(data);

        // Ok how should we guestimate speed... perhaps from data size?
        REPS = 100 + ((8 * 1000 * 1000) / data.length);
        System.out.println("Based on size, will use "+REPS+" repetitions");
    }

    protected int test()
        throws Exception
    {
        /* First things first: let's determine how data is stored,
         * as well as what's the first (canonical) value
         */
        mIn.reset();
        String firstValue = findFirstValue(mIn); // also sets/clear 'mUseAttr' flag
        if (mUseAttr) {
            REPS += REPS;
            System.out.println("(data stored as attributes: doubling REPS to "+REPS+")");
        } else {
            System.out.println("(data stored as element content)");
        }

        // But how about type?
        mType = 0;
        if ("true".equals(firstValue) || "false".equals(firstValue)) {
            mType = TYPE_BOOLEAN;
            System.out.println("Type detected as: BOOLEAN");
        } else {
            try {
                /*int nr =*/ Integer.parseInt(firstValue);
                // Ok, was a valid int
                mType = TYPE_BOOLEAN;
                System.out.println("Type detected as: INT");
            } catch (NumberFormatException nex) { }
        }

        if (mType == 0) {
            throw new IllegalArgumentException("Can not auto-detect type from value '"+firstValue+"'");
        }
        return test2();
    }

    private String findFirstValue(InputStream in)
        throws XMLStreamException
    {
        XMLStreamReader sr = mInputFactory.createXMLStreamReader(in);
        sr.nextTag();
        sr.nextTag();

        String value;

        if (sr.getAttributeCount() > 0) { // attributes
            mUseAttr = true;
            value = sr.getAttributeValue(0);
        } else {
            mUseAttr = false;
            value = sr.getElementText();
        }
        sr.close();
        return value;
    }

    private int test2()
        throws Exception
    {
        int i = 0;
        int total = 0;

        final int TEST_CASES = 2;

        while (true) {
            try {  Thread.sleep(150L); } catch (InterruptedException ie) { }
            int round = (i++ % TEST_CASES);
            long now = System.currentTimeMillis();
            String msg;
            int sum = 0;

            switch (round) {
            case 0:
                msg = "Access using Stax 1.0";
                switch (mType) {
                case TYPE_BOOLEAN:
                    sum = mUseAttr ? testUntypedBooleanAttr(REPS) : testUntypedBooleanElem(REPS);
                    break;

                    /*
                case TYPE_INT:
                    sum = mUseAttr ? testUntypedIntAttr(REPS) : testUntypedIntElem(REPS);
                    break;
                    */

                default:
                    throw new Error("Internal error");
                }
                break;

            case 1:
                msg = "Access using Stax2 Typed API";
                switch (mType) {
                case TYPE_BOOLEAN:
                    sum = mUseAttr ? testTypedBooleanAttr(REPS) : testTypedBooleanElem(REPS);
                    break;

                    /*
                case TYPE_INT:
                    sum = mUseAttr ? testTypedIntAttr(REPS) : testTypedIntElem(REPS);
                    break;
                    */

                default:
                    throw new Error("Internal error");
                }
                break;

            default:
                throw new Error("Internal error");
            }

            now = System.currentTimeMillis() - now;
            if (round == 0) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+now+" msecs ("
                               +sum+" -> "+(total & 0xFF)+").");

            total += sum;

            if ((i % TEST_PER_GC) == 0) {
                System.out.println("[GC]");
                try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
                System.gc();
                try {  Thread.sleep(200L); } catch (InterruptedException ie) { }

                /* One more tweak: let's add load if things start
                 * running too fast or slow, to try to get sweet range
                 * of 50 to 250 millisseconds
                 */
                if (now < MIN_RUN_TIME) {
                    REPS += (REPS / 5); // 20% up
                    System.out.println("[NOTE: increasing reps, now: "+REPS+"]");
                    try {  Thread.sleep(200L); } catch (InterruptedException ie) { }
                } else if (now > MAX_RUN_TIME && i > 20) {
                    /* Let's reduce load slower than increase; also,
                     * due to initial warmup, let's not adjust until
                     * we've gone through a few cycles
                     */
                    REPS -= (REPS / 10); // 10% down
                    System.out.println("[NOTE: decreasing reps, now: "+REPS+"]");
                    try {  Thread.sleep(200L); } catch (InterruptedException ie) { }
                }
            }
        }
    }

    /*
    /////////////////////////////////////////////////////////
    // Actual value type access, ones via Stax 1.0
    /////////////////////////////////////////////////////////
     */

    protected int testUntypedBooleanAttr(int reps)
        throws Exception
    {
        int total = 0;
        for (int i = 0; i < reps; ++i) {
            XMLStreamReader2 sr = constructAndFindRoot();
            while (sr.nextTag() == START_ELEMENT) {
                int c = sr.getAttributeCount();
                while (--c >= 0) {
                    String str = sr.getAttributeValue(c).trim();
                    if (str.equals("true")) {
                        ++total;
                    } else if (str.equals("false") || str.equals("0")) {
                        ;
                    } else if (str.equals("1")) {
                        ++total;
                    } else {
                        throw new XMLStreamException("Illegal value '"+str+"', not boolean");
                    }
                }
            }
            sr.close();
        }
        return total;
    }

    protected int testUntypedBooleanElem(int reps)
        throws Exception
    {
        int total = 0;
        for (int i = 0; i < reps; ++i) {
            XMLStreamReader2 sr = constructAndFindRoot();
            while (sr.nextTag() == START_ELEMENT) {
                String str = sr.getElementText().trim();
                if (str.equals("true")) {
                    ++total;
                } else if (str.equals("false") || str.equals("0")) {
                    ;
                } else if (str.equals("1")) {
                    ++total;
                } else {
                    throw new XMLStreamException("Illegal value '"+str+"', not boolean");
                }
            }
            sr.close();
        }
        return total;
    }

    /*
    /////////////////////////////////////////////////////////
    // Actual value type access, ones via Stax2 Typed API
    /////////////////////////////////////////////////////////
     */

    protected int testTypedBooleanAttr(int reps)
        throws Exception
    {
        int total = 0;
        for (int i = 0; i < reps; ++i) {
            XMLStreamReader2 sr = constructAndFindRoot();
            while (sr.nextTag() == START_ELEMENT) {
                int c = sr.getAttributeCount();
                while (--c >= 0) {
                    if (sr.getAttributeAsBoolean(c)) {
                        ++total;
                    }
                }
            }
            sr.close();
        }
        return total;
    }

    protected int testTypedBooleanElem(int reps)
        throws Exception
    {
        int total = 0;
        for (int i = 0; i < reps; ++i) {
            XMLStreamReader2 sr = constructAndFindRoot();
            while (sr.nextTag() == START_ELEMENT) {
                if (sr.getElementAsBoolean()) {
                    ++total;
                }
            }
            sr.close();
        }
        return total;
    }

    /*
    /////////////////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////////////////
     */
    
    XMLStreamReader2 constructAndFindRoot()
        throws XMLStreamException
    {
        mIn.reset();
        XMLStreamReader sr = mInputFactory.createXMLStreamReader(mIn);
        if (sr.nextTag() != START_ELEMENT) {
            throw new XMLStreamException("Couldn't locate root");
        }
        ; // points to root now
        return (XMLStreamReader2) sr;
    }

    static byte[] readData(File file)
        throws IOException
    {
        InputStream fin = new FileInputStream(file);
        byte[] buf = new byte[4000];
        ByteArrayOutputStream bos = new ByteArrayOutputStream(4000);
        int count;
        
        while ((count = fin.read(buf)) > 0) {
            bos.write(buf, 0, count);
        }
        fin.close();
        return bos.toByteArray();
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... <file>");
            System.exit(1);
        }
        byte[] data = readData(new File(args[0]));
        System.out.println(" -> "+data.length+" bytes read.");
        new TestTypedSpeed(data).test();
    }
}
