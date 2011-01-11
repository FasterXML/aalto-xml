package base;

import java.util.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.*;

public abstract class BaseTestCase
    extends junit.framework.TestCase
    implements XMLStreamConstants
{
    public final static String ENC_UTF8 = "UTF-8";
    public final static String ENC_LATIN1 = "ISO-8859-1";
    public final static String ENC_ASCII = "US-ASCII";

    /*
    /**********************************************************************
    /* Lazy-loaded thingies
    /**********************************************************************
     */

    XMLInputFactory2 mInputFactory = null;
    XMLOutputFactory2 mOutputFactory = null;

    /*
    /**********************************************************************
    /* Factory methods
    /**********************************************************************
     */

    protected XMLInputFactory2 getInputFactory()
    {
        if (mInputFactory == null) {
            mInputFactory = getNewInputFactory();
        }
        return mInputFactory;
    }

    protected XMLInputFactory2 getNewInputFactory()
    {
        // Can hard-code things here, being Aalto-specific tests.
        return new InputFactoryImpl();
    }

    protected XMLOutputFactory2 getOutputFactory()
    {
        if (mOutputFactory == null) {
            mOutputFactory = getNewOutputFactory();
        }
        return mOutputFactory;
    }

    protected XMLOutputFactory2 getNewOutputFactory()
    {
        // Can hard-code things here, being Aalto-specific tests.
        return new OutputFactoryImpl();
    }

    /*
    /**********************************************************************
    /* Additional assert methods
    /**********************************************************************
     */

    final static HashMap<Integer,String> mTokenTypes = new HashMap<Integer,String>();
    static {
        mTokenTypes.put(START_ELEMENT, "START_ELEMENT");
        mTokenTypes.put(END_ELEMENT, "END_ELEMENT");
        mTokenTypes.put(START_DOCUMENT, "START_DOCUMENT");
        mTokenTypes.put(END_DOCUMENT, "END_DOCUMENT");
        mTokenTypes.put(CHARACTERS, "CHARACTERS");
        mTokenTypes.put(CDATA, "CDATA");
        mTokenTypes.put(COMMENT, "COMMENT");
        mTokenTypes.put(PROCESSING_INSTRUCTION, "PROCESSING_INSTRUCTION");
        mTokenTypes.put(DTD, "DTD");
        mTokenTypes.put(SPACE, "SPACE");
        mTokenTypes.put(ENTITY_REFERENCE, "ENTITY_REFERENCE");

        // and Async addition(s)
        mTokenTypes.put(AsyncXMLStreamReader.EVENT_INCOMPLETE, "Async.EVENT_INCOMPLETE");
    }

    protected static String tokenTypeDesc(int tt)
    {
	String desc = (String) mTokenTypes.get(new Integer(tt));
	return (desc == null) ? ("["+tt+"]") : desc;
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

    protected void verifyException(Throwable e, String match)
    {
        String msg = e.getMessage();
        String lmsg = msg.toLowerCase();
        String lmatch = match.toLowerCase();
        if (lmsg.indexOf(lmatch) < 0) {
            fail("Expected an exception with sub-string \""+match+"\": got one with message \""+msg+"\"");
        }
    }
}
