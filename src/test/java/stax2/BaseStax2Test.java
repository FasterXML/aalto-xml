package stax2;

import java.io.*;
import java.util.HashMap;

import junit.framework.TestCase;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;

import org.codehaus.stax2.*;
import org.codehaus.stax2.evt.*;

import org.codehaus.stax2.ri.Stax2ReaderAdapter;

import com.fasterxml.aalto.stax.EventFactoryImpl;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.fasterxml.aalto.stax.OutputFactoryImpl;

/**
 * Base unit test class to be inherited by all unit tests that test
 * StAX2 API compatibility.
 */
public abstract class BaseStax2Test
    extends TestCase
    implements XMLStreamConstants
{
    /**
     * Value that should be reported by stax(2) impl to denote "no prefix"
     * for elements
     */
    final static String ELEM_NO_PREFIX = "";

    /**
     * Value that should be reported by stax(2) impl to denote "no prefix"
     * for attributes
     */
    final static String ATTR_NO_PREFIX = "";

    final static HashMap<Integer,String> mTokenTypes = new HashMap<Integer,String>();
    static {
        mTokenTypes.put(new Integer(START_ELEMENT), "START_ELEMENT");
        mTokenTypes.put(new Integer(END_ELEMENT), "END_ELEMENT");
        mTokenTypes.put(new Integer(START_DOCUMENT), "START_DOCUMENT");
        mTokenTypes.put(new Integer(END_DOCUMENT), "END_DOCUMENT");
        mTokenTypes.put(new Integer(CHARACTERS), "CHARACTERS");
        mTokenTypes.put(new Integer(CDATA), "CDATA");
        mTokenTypes.put(new Integer(COMMENT), "COMMENT");
        mTokenTypes.put(new Integer(PROCESSING_INSTRUCTION), "PROCESSING_INSTRUCTION");
        mTokenTypes.put(new Integer(DTD), "DTD");
        mTokenTypes.put(new Integer(SPACE), "SPACE");
        mTokenTypes.put(new Integer(ENTITY_REFERENCE), "ENTITY_REFERENCE");
    }

    /**
     * Switch that can be turned on to verify to display ALL exact Exceptions
     * thrown when Exceptions are expected. This is sometimes necessary
     * when debugging, since it's impossible to automatically verify
     * that Exception is exactly the right one, since there is no
     * strict Exception type hierarchy for StAX problems.
     *<p>
     * Note: Not made 'final static', so that compiler won't inline
     * it. Makes possible to do partial re-compilations.
     * Note: Since it's only used as the default value, sub-classes
     *  can separately turn it off as necessary
     */
    //protected static boolean DEF_PRINT_EXP_EXCEPTION = true;
    protected static boolean DEF_PRINT_EXP_EXCEPTION = false;

    protected boolean PRINT_EXP_EXCEPTION = DEF_PRINT_EXP_EXCEPTION;

    /*
    ///////////////////////////////////////////////////////////
    // Lazy-loaded thingies
    ///////////////////////////////////////////////////////////
     */

    XMLInputFactory2 mInputFactory = null;
    XMLOutputFactory2 mOutputFactory = null;
    XMLEventFactory2 mEventFactory = null;

    /*
    ///////////////////////////////////////////////////////////
    // "Overloads"
    ///////////////////////////////////////////////////////////
     */
    
    protected static XMLInputFactory2 newInputFactory() {
        return (XMLInputFactory2) InputFactoryImpl.newInstance();
    }

    protected static XMLOutputFactory2 newOutputFactory() {
        return (XMLOutputFactory2) OutputFactoryImpl.newInstance();
    }

    protected XMLEventFactory newEventFactory() {
        return EventFactoryImpl.newInstance();
    }

    /*
    ///////////////////////////////////////////////////////////
    // Factory methods
    ///////////////////////////////////////////////////////////
     */

    protected XMLInputFactory2 getInputFactory()
    {
        if (mInputFactory == null) {
            /* Shouldn't try to set these here, if these tests are
             * to be reusable. Rather, junit (ant) task should
             * define system properties if necessary.
             */
            //System.setProperty("javax.xml.stream.XMLInputFactory", "...");
            mInputFactory = newInputFactory();
        }
        return mInputFactory;
    }

    protected XMLStreamReader2 constructNsStreamReader(String content, boolean coal)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, true);
        setCoalescing(f, coal);
        return (XMLStreamReader2) f.createXMLStreamReader(new StringReader(content));
    }

    protected XMLStreamReader2 constructNsStreamReader(InputStream in, boolean coal)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, true);
        setCoalescing(f, coal);
        return (XMLStreamReader2) f.createXMLStreamReader(in);
    }

    protected XMLEventFactory2 getEventFactory()
    {
        if (mEventFactory == null) {
            /* Shouldn't try to set these here, if these tests are
             * to be reusable. Rather, junit (ant) task should
             * define system properties if necessary.
             */
            //System.setProperty("javax.xml.stream.XMLEventFactory", "...");
            mEventFactory = (XMLEventFactory2) XMLEventFactory.newInstance();
        }
        return mEventFactory;
    }


    protected XMLOutputFactory2 getOutputFactory()
    {
        if (mOutputFactory == null) {
            //System.setProperty("javax.xml.stream.XMLOutputFactory", "...");
            mOutputFactory = newOutputFactory();
        }
        return mOutputFactory;
    }

    protected static XMLStreamReader2 constructStreamReader(XMLInputFactory f, String content)
        throws XMLStreamException
    {
        return (XMLStreamReader2) f.createXMLStreamReader(new StringReader(content));
    }

    protected static XMLStreamReader2 constructStreamReader(XMLInputFactory f, byte[] data)
        throws XMLStreamException
    {
        return (XMLStreamReader2) f.createXMLStreamReader(new ByteArrayInputStream(data));
    }

    @SuppressWarnings({ "deprecation", "resource" })
	protected static XMLStreamReader2 constructStreamReaderForFile(XMLInputFactory f, String filename)
        throws IOException, XMLStreamException
    {
        File inf = new File(filename);
        XMLStreamReader sr = f.createXMLStreamReader(inf.toURL().toString(),
                                                     new FileReader(inf));
        assertEquals(sr.getEventType(), START_DOCUMENT);
        return (XMLStreamReader2) sr;
    }

    protected static XMLEventReader2 constructEventReader(XMLInputFactory f, String content)
        throws XMLStreamException
    {
        return (XMLEventReader2) f.createXMLEventReader(new StringReader(content));
    }

    protected XMLStreamReader2 constructNonNsStreamReader(String content, boolean coal)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, false);
        setCoalescing(f, coal);
        return (XMLStreamReader2) f.createXMLStreamReader(new StringReader(content));
    }

    /**
     * Method to force constructing a wrapper for given stream reader.
     * Have to use this method to work around natural resistance by
     * the wrapper to apply itself on what it considered "unnecessary"
     * target.
     */
    protected XMLStreamReader2 wrapWithAdapter(XMLStreamReader sr)
    {
        return new ForcedAdapter(sr);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Configuring input factory
    ///////////////////////////////////////////////////////////
     */

    protected static boolean setNamespaceAware(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        /* Let's not assert, but see if it sticks. Some implementations
         * might choose to silently ignore setting, at least for 'false'?
         */
        try {
            f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, state ? Boolean.TRUE : Boolean.FALSE);
            return (isNamespaceAware(f) == state);
        } catch (IllegalArgumentException e) {
            /* Let's assume, then, that the property (or specific value for it)
             * is NOT supported...
             */
            return false;
        }
    }

    protected static boolean isNamespaceAware(XMLInputFactory f)
        throws XMLStreamException
    {
        return ((Boolean) f.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE)).booleanValue();
    }

    protected static void setCoalescing(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.valueOf(state));
    }

    protected static void setValidating(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.valueOf(state));
    }

    protected static boolean setSupportDTD(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        try {
            f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.valueOf(state));
            return (willSupportDTD(f) == state);
        } catch (IllegalArgumentException e) {
            // Let's assume that the property (or specific value) is NOT supported...
            return false;
        }
    }

    protected static boolean willSupportDTD(XMLInputFactory f)
        throws XMLStreamException
    {
        return ((Boolean) f.getProperty(XMLInputFactory.SUPPORT_DTD)).booleanValue();
    }

    protected static void setReplaceEntities(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
                      state ? Boolean.TRUE : Boolean.FALSE);
    }

    protected static void setSupportExternalEntities(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
                      state ? Boolean.TRUE : Boolean.FALSE);
    }

    protected static void setLazyParsing(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(XMLInputFactory2.P_LAZY_PARSING,
                      state ? Boolean.TRUE : Boolean.FALSE);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Configuring output factory
    ///////////////////////////////////////////////////////////
     */

    protected static void setRepairing(XMLOutputFactory f, boolean state)
    {
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.valueOf(state));
    }

    protected static boolean setNamespaceAware(XMLOutputFactory f, boolean state)
        throws XMLStreamException
    {
        /* Let's not assert, but see if it sticks. Some implementations
         * might choose to silently ignore setting, at least for 'false'?
         */
        try {
            f.setProperty(XMLStreamProperties.XSP_NAMESPACE_AWARE, state ? Boolean.TRUE : Boolean.FALSE);
            return (isNamespaceAware(f) == state);
        } catch (IllegalArgumentException e) {
            /* Let's assume, then, that the property (or specific value for it)
             * is NOT supported...
             */
            return false;
        }
    }

    protected static boolean isNamespaceAware(XMLOutputFactory f)
        throws XMLStreamException
    {
        return ((Boolean) f.getProperty(XMLStreamProperties.XSP_NAMESPACE_AWARE)).booleanValue();
    }

    /*
    ///////////////////////////////////////////////////////////
    // Higher-level test methods
    ///////////////////////////////////////////////////////////
     */

    /**
     * Method that will iterate through contents of an XML document
     * using specified stream reader; will also access some of data
     * to make sure reader reads most of lazy-loadable data.
     * Method is usually called to try to get an exception for invalid
     * content.
     *
     * @return Dummy value calculated on contents; used to make sure
     *   no dead code is eliminated
     */
    protected int streamThrough(XMLStreamReader sr)
        throws XMLStreamException
    {
        int result = 0;

        while (sr.hasNext()) {
            int type = sr.next();
            result += type;
            if (sr.hasText()) {
                /* will also do basic verification for text content, to 
                 * see that all text accessor methods return same content
                 */
                result += getAndVerifyText(sr).hashCode();
            }
            if (sr.hasName()) {
                result += sr.getName().hashCode();
            }
        }

        return result;
    }

    protected int streamThroughFailing(XMLInputFactory f, String contents,
                                       String msg)
        throws XMLStreamException
    {
        int result = 0;
        try {
            XMLStreamReader sr = constructStreamReader(f, contents);
            result = streamThrough(sr);
        } catch (XMLStreamException ex) { // good
            if (PRINT_EXP_EXCEPTION) {
                System.out.println("Expected failure: '"+ex.getMessage()+"' "
                                   +"(matching message: '"+msg+"')");
            }
            return 0;
        } catch (RuntimeException ex2) { // ok
            if (PRINT_EXP_EXCEPTION) {
                System.out.println("Expected failure: '"+ex2.getMessage()+"' "
                                   +"(matching message: '"+msg+"')");
            }
            return 0;
        } catch (Throwable t) { // not so good
            fail("Expected an XMLStreamException or RuntimeException for "+msg
                 +", got: "+t);
        }

        fail("Expected an exception for "+msg);
        return result; // never gets here
    }

    protected int streamThroughFailing(XMLStreamReader sr, String msg)
        throws XMLStreamException
    {
        int result = 0;
        try {
            result = streamThrough(sr);
        } catch (XMLStreamException ex) { // good
            if (PRINT_EXP_EXCEPTION) {
                System.out.println("Expected failure: '"+ex.getMessage()+"' "
                                   +"(matching message: '"+msg+"')");
            }
            return 0;
        } catch (RuntimeException ex2) { // ok
            if (PRINT_EXP_EXCEPTION) {
                System.out.println("Expected failure: '"+ex2.getMessage()+"' "
                                   +"(matching message: '"+msg+"')");
            }
            return 0;
        } catch (Throwable t) { // not so good
            fail("Expected an XMLStreamException or RuntimeException for "+msg
                 +", got: "+t);
        }

        fail("Expected an exception for "+msg);
        return result; // never gets here
    }

    /*
    ///////////////////////////////////////////////////////////
    // Assertions
    ///////////////////////////////////////////////////////////
     */

    protected static String tokenTypeDesc(int tt)
    {
	String desc = mTokenTypes.get(new Integer(tt));
	return (desc == null) ? ("["+tt+"]") : desc;
    }

    protected static void assertTokenType(int expType, XMLEvent evt)
    {
        assertTokenType(expType, evt.getEventType());
    }

    protected static void assertTokenType(int expType, int actType)
    {
        if (expType != actType) {
            String expStr = tokenTypeDesc(expType);
            String actStr = tokenTypeDesc(actType);

            if (expStr == null) {
                expStr = ""+expType;
            }
            if (actStr == null) {
                actStr = ""+actType;
            }
            fail("Expected token "+expStr+"; got "+actStr+".");
        }
    }

    /**
     * Helper assertion that assert that the String is either null or
     * empty ("").
     */
    protected static void assertNullOrEmpty(String str)
    {
        if (str != null && str.length() > 0) {
            fail("Expected String to be empty or null; was '"+str+"' (length "
                 +str.length()+")");
        }
    }

    protected static void assertNotNullOrEmpty(String str)
    {
        if (str == null || str.length() == 0) {
            fail("Expected String to be non-empty; got "
                 +((str == null) ? "NULL" : "\"\""));
        }
    }

    /**
     * Method that can be used to verify that the current element
     * pointed to by the stream reader has no prefix.
     */
    protected static void assertNoElemPrefix(XMLStreamReader sr)
        throws XMLStreamException
    {
        String prefix = sr.getPrefix();
        if (prefix != ELEM_NO_PREFIX) {
            fail("Element that does not have a prefix should be indicated with <"+ELEM_NO_PREFIX+">, not <"+prefix+">");
        }
    }

    /**
     * Helper method for ensuring that the given return value for
     * attribute prefix accessor has returned a value that
     * represents "no prefix" value.
     *<p>
     * Current thinking (early 2008) is that empty string is the
     * expected value here.
     */
    protected static void assertNoAttrPrefix(String attrPrefix)
        throws XMLStreamException
    {
        if (attrPrefix != ATTR_NO_PREFIX) {
            fail("Attribute that does not have a prefix should be indicated with <"+ATTR_NO_PREFIX+">, not <"+attrPrefix+">");
        }
    }

    /**
     * Method that can be used to verify that the current element
     * pointed to by the stream reader does not belong to a namespace.
     */
    protected static void assertElemNotInNamespace(XMLStreamReader sr)
        throws XMLStreamException
    {
        String uri = sr.getNamespaceURI();
        if (uri == null) {
            fail("Excepted empty String to indicate \"no namespace\": got null");
        } else if (uri.length() != 0) {
            fail("Excepted no (null) namespace URI: got '"+uri+"'");
        }
    }

    protected static void assertNoAttrNamespace(String attrNsURI)
        throws XMLStreamException
    {
        if (attrNsURI == null) {
            fail("Expected empty String to indicate \"no namespace\" (for attribute): got null");
        } else if (attrNsURI.length() != 0) {
            fail("Expected empty String to indicate \"no namespace\" (for attribute): got '"+attrNsURI+"'");
        }
    }

    protected static void failStrings(String msg, String exp, String act)
    {
        // !!! TODO: Indicate position where Strings differ
        fail(msg+": expected "+quotedPrintable(exp)+", got "
             +quotedPrintable(act));
    }

    /**
     * Method that not only gets currently available text from the 
     * reader, but also checks that its consistenly accessible using
     * different (basic) StAX methods.
     */
    protected static String getAndVerifyText(XMLStreamReader sr)
        throws XMLStreamException
    {
        /* 05-Apr-2006, TSa: Although getText() is available for DTD
         *   and ENTITY_REFERENCE, getTextXxx() are not. Thus, can not
         *   do more checks for those types.
         */
        int type = sr.getEventType();
        if (type == ENTITY_REFERENCE || type == DTD) {
            return sr.getText();
        }

        int expLen = sr.getTextLength();
        /* Hmmh. It's only ok to return empty text for DTD event... well,
         * maybe also for CDATA, since empty CDATA blocks are legal?
         */
        /* !!! 01-Sep-2004, TSa:
         *  note: theoretically, in coalescing mode, it could be possible
         *  to have empty CDATA section(s) get converted to CHARACTERS,
         *  which would be empty... may need to enhance this to check that
         *  mode is not coalescing? Or something
         */
        if (type == CHARACTERS) {
            assertTrue("Stream reader should never return empty Strings.",  (expLen > 0));
        }
        String text = sr.getText();
        assertNotNull("getText() should never return null.", text);
        assertEquals("Expected text length of "+expLen+", got "+text.length(),
		     expLen, text.length());
        char[] textChars = sr.getTextCharacters();
        int start = sr.getTextStart();
        String text2 = new String(textChars, start, expLen);
        assertEquals(text, text2);
        return text;
    }

    protected void verifyException(Throwable e, String match)
    {
        String msg = e.getMessage();
        String lmsg = msg.toLowerCase();
        String lmatch = match.toLowerCase();
        if (lmsg.indexOf(lmatch) < 0) {
            fail("Expected an exception with sub-string \""+match+"\": got one with message \""+msg+"\"");
        }
    }

    /*
    ///////////////////////////////////////////////////////////
    // Debug/output helpers
    ///////////////////////////////////////////////////////////
     */

    public static void warn(String msg)
    {
	System.err.println("WARN: "+msg);
    }

    public static String printable(char ch)
    {
        if (ch == '\n') {
            return "\\n";
        }
        if (ch == '\r') {
            return "\\r";
        }
        if (ch == '\t') {
            return "\\t";
        }
        if (ch == ' ') {
            return "_";
        }
        if (ch > 127 || ch < 32) {
            StringBuffer sb = new StringBuffer(6);
            sb.append("\\u");
            String hex = Integer.toHexString(ch);
            for (int i = 0, len = 4 - hex.length(); i < len; i++) {
                sb.append('0');
            }
            sb.append(hex);
            return sb.toString();
        }
        return null;
    }

    public static String printableWithSpaces(char ch)
    {
        if (ch == '\n') {
            return "\\n";
        }
        if (ch == '\r') {
            return "\\r";
        }
        if (ch == '\t') {
            return "\\t";
        }
        if (ch > 127 || ch < 32) {
            StringBuffer sb = new StringBuffer(6);
            sb.append("\\u");
            String hex = Integer.toHexString(ch);
            for (int i = 0, len = 4 - hex.length(); i < len; i++) {
                sb.append('0');
            }
            sb.append(hex);
            return sb.toString();
        }
        return null;
    }

    public static String printable(String str)
    {
        if (str == null || str.length() == 0) {
            return str;
        }

        int len = str.length();
        StringBuffer sb = new StringBuffer(len + 64);
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            String res = printable(c);
            if (res == null) {
                sb.append(c);
            } else {
                sb.append(res);
            }
        }
        return sb.toString();
    }

    public static String printableWithSpaces(String str)
    {
        if (str == null || str.length() == 0) {
            return str;
        }

        int len = str.length();
        StringBuffer sb = new StringBuffer(len + 64);
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            String res = printableWithSpaces(c);
            if (res == null) {
                sb.append(c);
            } else {
                sb.append(res);
            }
        }
        return sb.toString();
    }

    protected static String quotedPrintable(String str)
    {
        if (str == null || str.length() == 0) {
            return "[0]''";
        }
        return "[len: "+str.length()+"] '"+printable(str)+"'";
    }

    /*
    ///////////////////////////////////////////////////////////
    // Helper classes
    ///////////////////////////////////////////////////////////
     */

    /**
     * Need a dummy base class to be able to access protected
     * constructor for testing purposes.
     */
    final static class ForcedAdapter
        extends Stax2ReaderAdapter
    {
        public ForcedAdapter(XMLStreamReader sr)
        {
            super(sr);
        }
    }
}

