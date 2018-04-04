package stax2.vstream;

import java.io.StringReader;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.*;

import com.ctc.wstx.dtd.DTDSchemaFactory;
import com.ctc.wstx.msv.RelaxNGSchemaFactory;
import com.ctc.wstx.msv.W3CSchemaFactory;

import stax2.BaseStax2Test;

public abstract class BaseStax2ValidationTest
    extends BaseStax2Test
{
    // 02-Apr-2018, tatu: not the cleanest thing ever but has to do for now; needed
    //    since Aalto does not support non-ns mode
    protected final static boolean HAS_NON_NS_MODE = false;

    protected XMLValidationSchemaFactory newW3CSchemaValidatorFactory() {
        return new W3CSchemaFactory();
    }

    protected XMLValidationSchemaFactory newRelaxNGValidatorFactory() {
        return new RelaxNGSchemaFactory();
    }

    protected XMLValidationSchemaFactory newDTDValidatorFactory() {
        return new DTDSchemaFactory();
    }

    protected XMLValidationSchema parseRngSchema(String contents)
        throws XMLStreamException
    {
        return newRelaxNGValidatorFactory()
                .createSchema(new StringReader(contents));
    }

    protected XMLValidationSchema parseDTDSchema(String contents)
        throws XMLStreamException
    {
        return newDTDValidatorFactory()
                .createSchema(new StringReader(contents));
    }

    protected XMLValidationSchema parseW3CSchema(String contents)
        throws XMLStreamException
    {
        return newW3CSchemaValidatorFactory()
                .createSchema(new StringReader(contents));
    }

    protected void verifyFailure(String xml, XMLValidationSchema schema, String failMsg,
                                 String failPhrase) throws XMLStreamException
    {
        // default to strict handling:
        verifyFailure(xml, schema, failMsg, failPhrase, true);
    }
    
    protected void verifyFailure(String xml, XMLValidationSchema schema, String failMsg,
                                 String failPhrase, boolean strict) throws XMLStreamException
    {
        XMLStreamReader2 sr = constructStreamReader(getInputFactory(), xml);
        sr.validateAgainst(schema);
        try {
            while (sr.hasNext()) {
                /* int type = */sr.next();
            }
            fail("Expected validity exception for " + failMsg);
        } catch (XMLValidationException vex) {
            String origMsg = vex.getMessage();
            String msg = (origMsg == null) ? "" : origMsg.toLowerCase();
            if (msg.indexOf(failPhrase.toLowerCase()) < 0) {
                String actualMsg = "Expected validation exception for "
                    + failMsg + ", containing phrase '" + failPhrase
                    + "': got '" + origMsg + "'";
                if (strict) {
                    fail(actualMsg);
                }
                warn("suppressing failure due to MSV bug, failure: '"
                     + actualMsg + "'");
            }
            // should get this specific type; not basic stream exception
        } catch (XMLStreamException sex) {
            fail("Expected XMLValidationException for " + failMsg
                 + "; instead got " + sex.getMessage());
        }
    }
}
