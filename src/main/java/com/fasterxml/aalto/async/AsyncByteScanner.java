package com.fasterxml.aalto.async;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncInputFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.impl.ErrorConsts;
import com.fasterxml.aalto.in.ByteBasedPNameTable;
import com.fasterxml.aalto.in.ByteBasedScanner;
import com.fasterxml.aalto.in.PName;
import com.fasterxml.aalto.in.ReaderConfig;
import com.fasterxml.aalto.util.CharsetNames;
import com.fasterxml.aalto.util.DataUtil;

public abstract class AsyncByteScanner
    extends ByteBasedScanner
    implements AsyncInputFeeder
{
    protected final static int EVENT_INCOMPLETE = AsyncXMLStreamReader.EVENT_INCOMPLETE;

    /*
    /**********************************************************************
    /* State consts
    /**********************************************************************
     */

    /**
     * Default starting state for many events/contexts -- nothing has been
     * seen so far, no  event incomplete. Not used for all event types.
     */
    protected final static int STATE_DEFAULT = 0;

    // // // States for prolog/epilog major state:

    /**
     * State in which a less-than sign has been seen
     */
    protected final static int STATE_PROLOG_INITIAL = 1; // State before document when we may get xml declaration
    protected final static int STATE_PROLOG_SEEN_LT = 2; // "<" seen after xml declaration
    protected final static int STATE_PROLOG_DECL = 3; // "<!" seen after xml declaration

    // // // States for in-tree major state:

    protected final static int STATE_TREE_SEEN_LT = 1; // "<" seen
    protected final static int STATE_TREE_SEEN_AMP = 2; // "&" seen
    protected final static int STATE_TREE_SEEN_EXCL = 3; // "<!" seen
    protected final static int STATE_TREE_SEEN_SLASH = 4; // "</" seen
    protected final static int STATE_TREE_NUMERIC_ENTITY_START = 5; // "&#" and part of value
    protected final static int STATE_TREE_NAMED_ENTITY_START = 6; // "&" and part of name

    // // // States within event types (STATE_DEFAULT is shared):

    // XML declaration parsing
    protected final static int STATE_XMLDECL_AFTER_XML = 1; // "<?xml", need space
    protected final static int STATE_XMLDECL_BEFORE_VERSION = 2; // "<?xml ", can have more spaces
    protected final static int STATE_XMLDECL_VERSION = 3; // "<?xml ", part of "version"
    protected final static int STATE_XMLDECL_AFTER_VERSION = 4; // "<?xml version", need space or '='
    protected final static int STATE_XMLDECL_VERSION_EQ = 5; // "<?xml version=", need space or quote
    protected final static int STATE_XMLDECL_VERSION_VALUE = 6; // parsing version value
    protected final static int STATE_XMLDECL_AFTER_VERSION_VALUE = 7; // version got; need space or '?'
    protected final static int STATE_XMLDECL_BEFORE_ENCODING = 8; // version, value, space got, need '?' or 'e'
    protected final static int STATE_XMLDECL_ENCODING = 9; // parsing "encoding"
    protected final static int STATE_XMLDECL_AFTER_ENCODING = 10; // 'encoding' got, need space or '='
    protected final static int STATE_XMLDECL_ENCODING_EQ = 11; // "encoding="
    protected final static int STATE_XMLDECL_ENCODING_VALUE = 12; // parsing encoding value
    protected final static int STATE_XMLDECL_AFTER_ENCODING_VALUE = 13; // encoding+value gotten; need space or '?'
    protected final static int STATE_XMLDECL_BEFORE_STANDALONE = 14; // after encoding+value+space; get '?' or 's'
    protected final static int STATE_XMLDECL_STANDALONE = 15; // parsing "standalone"
    protected final static int STATE_XMLDECL_AFTER_STANDALONE = 16; // 'standalone' got, need space or '='
    protected final static int STATE_XMLDECL_STANDALONE_EQ = 17; // "standalone="
    protected final static int STATE_XMLDECL_STANDALONE_VALUE = 18; // encoding+value gotten; need space or '?'
    protected final static int STATE_XMLDECL_AFTER_STANDALONE_VALUE = 19; // encoding+value gotten; need space or '?'
    protected final static int STATE_XMLDECL_ENDQ = 20; // "?" at the end of declaration

    // DOCTYPE declaration parsing
    protected final static int STATE_DTD_DOCTYPE = 1; // part of "DOCTYPE"
    protected final static int STATE_DTD_AFTER_DOCTYPE = 2; // "DOCTYPE", need space
    protected final static int STATE_DTD_BEFORE_ROOT_NAME = 3; // optional space before root name
    protected final static int STATE_DTD_ROOT_NAME = 4; // part of root name
    protected final static int STATE_DTD_AFTER_ROOT_NAME = 5; // root name gotten; need a space or '>'
    protected final static int STATE_DTD_BEFORE_IDS = 6; // before "PUBLIC" or "SYSTEM" token
    protected final static int STATE_DTD_PUBLIC_OR_SYSTEM = 7; // parsing "PUBLIC" or "SYSTEM"
    protected final static int STATE_DTD_AFTER_PUBLIC = 8; // "PUBLIC" found, need space
    protected final static int STATE_DTD_AFTER_SYSTEM = 9; // "SYSTEM" found, need space
    protected final static int STATE_DTD_BEFORE_PUBLIC_ID = 10; // after "PUBLIC", space, need quoted public id
    protected final static int STATE_DTD_PUBLIC_ID = 11; // parsing public ID
    protected final static int STATE_DTD_AFTER_PUBLIC_ID = 12; // public ID parsed, need space
    protected final static int STATE_DTD_BEFORE_SYSTEM_ID = 13; // about to parse quoted system id
    protected final static int STATE_DTD_SYSTEM_ID = 14; // parsing system ID
    protected final static int STATE_DTD_AFTER_SYSTEM_ID = 15; // after system ID, optional space, '>' or int subset
    protected final static int STATE_DTD_INT_SUBSET = 16; // parsing internal subset

    protected final static int STATE_DTD_EXPECT_CLOSING_GT = 50; // ']' gotten that should be followed by '>'
    
    // For CHARACTERS, default is the basic (and only)

    // just seen "&"
    protected final static int STATE_TEXT_AMP = 4;
    // just seen "&#"
//    protected final static int STATE_TEXT_AMP_AND_HASH = 5;
    // seen '&' and partial name:
    protected final static int STATE_TEXT_AMP_NAME = 6;

    // For comments, STATE_DEFAULT means "<!-" has been seen
    protected final static int STATE_COMMENT_CONTENT = 1; // "<!--"
    protected final static int STATE_COMMENT_HYPHEN = 2; // content, and one '-'
    protected final static int STATE_COMMENT_HYPHEN2 = 3; // content, "--"

    // For cdata, STATE_DEFAULT means that just "<![" has been seen
    protected final static int STATE_CDATA_CONTENT = 1; // start marker seen, maybe some content
    protected final static int STATE_CDATA_C = 2; // "<![C"
    protected final static int STATE_CDATA_CD = 3; // "<![CD"
    protected final static int STATE_CDATA_CDA = 4; // "<![CDA"
    protected final static int STATE_CDATA_CDAT = 5; // "<![CDAT"
    protected final static int STATE_CDATA_CDATA = 6; // "<![CDATA"
    
    // For PIs, default means that '<?' has been seen, nothing else

    // (note: funny ordering, starting with "quick path" entries)
    protected final static int STATE_PI_AFTER_TARGET = 1; // "<?", target ?>
    protected final static int STATE_PI_AFTER_TARGET_WS = 2; // "<?", target, ws
    protected final static int STATE_PI_AFTER_TARGET_QMARK = 3; // "<?", target, "?"
    protected final static int STATE_PI_IN_TARGET = 4; // "<?", part of target
    protected final static int STATE_PI_IN_DATA = 5; // "<?", target, ws, part of data

    // For start element, DEFAULT means that only '<' has been seen
    protected final static int STATE_SE_ELEM_NAME = 1; // "<" and part of name
    protected final static int STATE_SE_SPACE_OR_END = 2; // after elem name or attr, but need space
    protected final static int STATE_SE_SPACE_OR_ATTRNAME = 3; // after elem/attr and space
    
    protected final static int STATE_SE_ATTR_NAME = 4; // in attribute name
    protected final static int STATE_SE_SPACE_OR_EQ = 5;
    protected final static int STATE_SE_SPACE_OR_ATTRVALUE = 6;
    protected final static int STATE_SE_ATTR_VALUE_NORMAL = 7;
    protected final static int STATE_SE_ATTR_VALUE_NSDECL = 8;
    protected final static int STATE_SE_SEEN_SLASH = 9;

    // For END_ELEMENT, default means we are parsing name
    protected final static int STATE_EE_NEED_GT = 1;

    /*
    /**********************************************************************
    /* Markers to use for 'pending' character, if
    /* not multi-byte UTF character
    /**********************************************************************
     */

    // Marker when dealing with general CR+LF pair
    protected final static int PENDING_STATE_CR = -1;

    // Parsing of possible XML declaration
    protected final static int PENDING_STATE_XMLDECL_LT = -5; // "<" at start of doc
    protected final static int PENDING_STATE_XMLDECL_LTQ = -6; // "<?" at start of doc
    protected final static int PENDING_STATE_XMLDECL_TARGET = -7; // "<?" at start of doc, part of name
    
    // Processing Instruction parsing:
    protected final static int PENDING_STATE_PI_QMARK = -15;

    // Comment parsing
    protected final static int PENDING_STATE_COMMENT_HYPHEN1 = -20;
    protected final static int PENDING_STATE_COMMENT_HYPHEN2 = -21;

    // CData parsing
    protected final static int PENDING_STATE_CDATA_BRACKET1 = -30;
    protected final static int PENDING_STATE_CDATA_BRACKET2 = -31;

    protected final static int PENDING_STATE_ENT_SEEN_HASH = -70; // seen &#
    protected final static int PENDING_STATE_ENT_SEEN_HASH_X = -71; // seen &#x
    protected final static int PENDING_STATE_ENT_IN_DEC_DIGIT = -72; // seen &# and 1 or more decimals
    protected final static int PENDING_STATE_ENT_IN_HEX_DIGIT = -73; // seen &#x and 1 or more hex digits
//    final static int PENDING_STATE_ENT_IN_NAME = -; // seen & and part of the name

    // partially handled entities within attribute/ns values use pending state as well
    protected final static int PENDING_STATE_ATTR_VALUE_AMP = -60;
    protected final static int PENDING_STATE_ATTR_VALUE_AMP_HASH = -61;
    protected final static int PENDING_STATE_ATTR_VALUE_AMP_HASH_X = -62;
    protected final static int PENDING_STATE_ATTR_VALUE_ENTITY_NAME = -63;
    protected final static int PENDING_STATE_ATTR_VALUE_DEC_DIGIT = -64;
    protected final static int PENDING_STATE_ATTR_VALUE_HEX_DIGIT = -65;

    protected final static int PENDING_STATE_TEXT_AMP = -80; // seen &
    protected final static int PENDING_STATE_TEXT_AMP_HASH = -81; // seen &#
    protected final static int PENDING_STATE_TEXT_DEC_ENTITY = -82; // seen &# and 1 or more decimals
    protected final static int PENDING_STATE_TEXT_HEX_ENTITY = -83; // seen &#x and 1 or more hex digits
    protected final static int PENDING_STATE_TEXT_IN_ENTITY = -84; // seen & and part of entity name
    protected final static int PENDING_STATE_TEXT_BRACKET1 = -85; // seen ]
    protected final static int PENDING_STATE_TEXT_BRACKET2 = -86; // seen ]]

    /*
    /**********************************************************************
    /* General state tracking
    /**********************************************************************
     */

    /**
     * Due to asynchronous nature of parsing, we may know what
     * event we are trying to parse, even if it's not yet
     * complete. Type of that event is stored here.
     */
    protected int _nextEvent = EVENT_INCOMPLETE;

    /**
     * In addition to the event type, there is need for additional
     * state information
     */
    protected int _state;
    
    /**
     * For token/state combinations that are 'shared' between
     * events (or embedded in them), this is where the surrounding
     * event state is retained.
     */
    protected int _surroundingEvent = EVENT_INCOMPLETE;

    /**
     * There are some multi-byte combinations that must be handled
     * as a unit: CR+LF linefeeds, multi-byte UTF-8 characters, and
     * multi-character end markers for comments and PIs.
     * Since they can be split across input buffer
     * boundaries, first byte(s) may need to be temporarily stored.
     *<p>
     * If so, this int will store byte(s), in little-endian format
     * (that is, first pending byte is at 0x000000FF, second [if any]
     * at 0x0000FF00, and third at 0x00FF0000). This can be
     * (and is) used to figure out actual number of bytes pending,
     * for multi-byte (UTF-8) character decoding.
     *<p>
     * Note: it is assumed that if value is 0, there is no data.
     * Thus, if 0 needed to be added pending, it has to be masked.
     */
    protected int _pendingInput = 0;

    /**
     * Flag that is sent when calling application indicates that there will
     * be no more input to parse.
     */
    protected boolean _endOfInput = false;
    
    /*
    /**********************************************************************
    /* Name/entity parsing state
    /**********************************************************************
     */

    /**
     * Number of complete quads parsed for current name (quads
     * themselves are stored in {@link #_quadBuffer}).
     */
    protected int _quadCount;

    /**
     * Bytes parsed for the current, incomplete, quad
     */
    protected int _currQuad;

    /**
     * Number of bytes pending/buffered, stored in {@link #_currQuad}
     */
    protected int _currQuadBytes = 0;

    /**
     * Entity value accumulated so far
     */
    protected int _entityValue = 0;
    
    /*
    /**********************************************************************
    /* (Start) element parsing state
    /**********************************************************************
     */

    protected boolean _elemAllNsBound;

    protected boolean _elemAttrCount;

    protected byte _elemAttrQuote;

    protected PName _elemAttrName;

    /**
     * Pointer for the next character of currently being parsed value
     * within attribute value buffer
     */
    protected int _elemAttrPtr;

    /**
     * Pointer for the next character of currently being parsed namespace
     * URI for the current namespace declaration
     */
    protected int _elemNsPtr;

    /*
    /**********************************************************************
    /* Other state
    /**********************************************************************
     */

    /**
     * Flag that indicates whether we are inside a declaration during parsing
     * of internal DTD subset.
     */
    protected boolean _inDtdDeclaration;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */
    
    protected AsyncByteScanner(ReaderConfig cfg) {
        super(cfg);
    }

    @Override
    public void endOfInput() {
        _endOfInput = true;
    }
    
    /**
     * Since the async scanner has no access to whatever passes content,
     * there is no input source in same sense as with blocking scanner;
     * and there is nothing to close. But we can at least mark input
     * as having ended.
     */
    @Override
    protected void _closeSource() throws IOException
    {
        // nothing to do, we are done.
        _endOfInput = true;
    }

    /*
    /**********************************************************************
    /* Shared helper methods
    /**********************************************************************
     */

    protected void verifyAndSetXmlVersion() throws XMLStreamException
    {
        if (_textBuilder.equalsString("1.0")) {
            _config.setXmlVersion("1.0");
        } else if (_textBuilder.equalsString("1.1")) {
            _config.setXmlVersion("1.1");
        } else {
            reportInputProblem("Unrecognized XML version '"+_textBuilder.contentsAsString()+"' (expected '1.0' or '1.1')");
        }
    }

    protected void verifyAndSetXmlEncoding() throws XMLStreamException
    {
        String enc = CharsetNames.normalize(_textBuilder.contentsAsString());
        _config.setXmlEncoding(enc);
        /* 09-Feb-2011, tatu: For now, we will only accept UTF-8 and ASCII; could
         *   expand in future (Latin-1 should be doable)
         */
        if (CharsetNames.CS_UTF8 != enc && CharsetNames.CS_US_ASCII != enc) {
            reportInputProblem("Unsupported encoding '"+enc+"': only UTF-8 and US-ASCII support by async parser");
        }
    }

    protected void verifyAndSetXmlStandalone() throws XMLStreamException
    {
        if (_textBuilder.equalsString("yes")) {
            _config.setXmlStandalone(Boolean.TRUE);
        } else if (_textBuilder.equalsString("no")) {
            _config.setXmlStandalone(Boolean.FALSE);
        } else {
            reportInputProblem("Invalid standalone value '"+_textBuilder.contentsAsString()+"': can only use 'yes' and 'no'");
        }
    }

    protected void verifyAndSetPublicId() throws XMLStreamException {
        _publicId = _textBuilder.contentsAsString();
    }

    protected void verifyAndSetSystemId() throws XMLStreamException {
        _systemId = _textBuilder.contentsAsString();
    }

    /*
    /**********************************************************************
    /* Second-level parsing; character content (in tree)
    /**********************************************************************
     */

    @Override
    protected final void finishToken() throws XMLStreamException
    {
        _tokenIncomplete = false;
        switch (_currToken) {
        case PROCESSING_INSTRUCTION:
            finishPI();
            break;
        case CHARACTERS:
            finishCharacters();
            break;
        case COMMENT:
            finishComment();
            break;
        case SPACE:
            finishSpace();
            break;
        case DTD:
            finishDTD(true); // true -> get text
            break;
        case CDATA:
            finishCData();
            break;
        default:
            ErrorConsts.throwInternalError();
        }
    }
    
    /**
     * Method called to initialize state for CHARACTERS event, after
     * just a single byte has been seen. What needs to be done next
     * depends on whether coalescing mode is set or not: if it is not
     * set, just a single character needs to be decoded, after which
     * current event will be incomplete, but defined as CHARACTERS.
     * In coalescing mode, the whole content must be read before
     * current event can be defined. The reason for difference is
     * that when <code>XMLStreamReader.next()</code> returns, no
     * blocking can occur when calling other methods.
     *
     * @return Event type detected; either CHARACTERS, if at least
     *   one full character was decoded (and can be returned),
     *   EVENT_INCOMPLETE if not (part of a multi-byte character
     *   split across input buffer boundary)
     */
    protected abstract int startCharacters(byte b) throws XMLStreamException;

    protected abstract boolean handleAttrValue() throws XMLStreamException;

    protected abstract boolean handleNsDecl() throws XMLStreamException;

    /*
    /**********************************************************************
    /* Abstract methods from base class, parsing
    /**********************************************************************
     */


    @Override
    protected void finishCData() throws XMLStreamException
    {
        // N/A
        throwInternal();
    }

    @Override
    protected void finishComment() throws XMLStreamException
    {
        // N/A
        throwInternal();
    }

    @Override
    protected void finishDTD(boolean copyContents) throws XMLStreamException
    {
        // N/A
        throwInternal();
    }

    @Override
    protected void finishPI() throws XMLStreamException
    {
        // N/A
        throwInternal();
    }

    @Override
    protected void finishSpace() throws XMLStreamException
    {
        // N/A
        throwInternal();
    }

    // // token-skip methods

    /**
     * @return True if the whole characters segment was succesfully
     *   skipped; false if not
     */
    @Override
    protected abstract boolean skipCharacters()
        throws XMLStreamException;

    @Override
    protected void skipCData() throws XMLStreamException
    {
        // should never be called
        throwInternal();
    }

    @Override
    protected void skipComment() throws XMLStreamException
    {
        // should never be called
        throwInternal();
    }

    @Override
    protected void skipPI() throws XMLStreamException
    {
        // should never be called
        throwInternal();
    }

    @Override
    protected void skipSpace() throws XMLStreamException
    {
        // should never be called
        throwInternal();
    }

    @Override
    protected boolean loadMore() throws XMLStreamException
    {
        // should never get called
        throwInternal();
        return false; // never gets here
    }
    
    @Override
    protected abstract void finishCharacters() throws XMLStreamException;

    /*
    /**********************************************************************
    /* Internal methods, name decoding
    /**********************************************************************
     */

    /**
     * Method called to process a sequence of bytes that is likely to
     * be a PName. At this point we encountered an end marker, and
     * may either hit a formerly seen well-formed PName; an as-of-yet
     * unseen well-formed PName; or a non-well-formed sequence (containing
     * one or more non-name chars without any valid end markers).
     *
     * @param lastQuad Word with last 0 to 3 bytes of the PName; not included
     *   in the quad array
     * @param lastByteCount Number of bytes contained in lastQuad; 0 to 3.
     * @param firstQuad First 1 to 4 bytes of the PName (4 if length
     *    at least 4 bytes; less only if not). 
     * @param qlen Number of quads in the array, except if less than 2
     *    (in which case only firstQuad and lastQuad are used)
     * @param quads Array that contains all the quads, except for the
     *    last one, for names with more than 8 bytes (i.e. more than
     *    2 quads)
     */
    protected final PName findPName(int lastQuad, int lastByteCount) throws XMLStreamException
    {
        // First, need to push back the byte read but not used:
        --_inputPtr;
        int qlen = _quadCount;
        // Also: if last quad is empty, will need take last from qbuf.
        if (lastByteCount == 0) {
            lastQuad = _quadBuffer[--qlen];
            lastByteCount = 4;
        }
        // Separate handling for short names:
        if (qlen <= 1) { // short name?
            if (qlen == 0) { // 4-bytes or less; only has 'lastQuad' defined
                int hash = ByteBasedPNameTable.calcHash(lastQuad);
                PName name = _symbols.findSymbol(hash, lastQuad, 0);
                if (name == null) {
                    // Let's simplify things a bit, and just use array based one then:
                    _quadBuffer[0] = lastQuad;
                    name = addPName(hash, _quadBuffer, 1, lastByteCount);
                }
                return name;
            }
            int firstQuad = _quadBuffer[0];
            int hash = ByteBasedPNameTable.calcHash(firstQuad, lastQuad);
            PName name = _symbols.findSymbol(hash, firstQuad, lastQuad);
            if (name == null) {
                // As above, let's just use array, then
                _quadBuffer[1] = lastQuad;
                name = addPName(hash, _quadBuffer, 2, lastByteCount);
            }
            return name;
        }
        /* Nope, long (3 quads or more). At this point, the last quad is
         * not yet in the array, let's add:
         */
        if (qlen >= _quadBuffer.length) { // let's just double?
            _quadBuffer = DataUtil.growArrayBy(_quadBuffer, _quadBuffer.length);
        }
        _quadBuffer[qlen++] = lastQuad;
        int hash = ByteBasedPNameTable.calcHash(_quadBuffer, qlen);
        PName name = _symbols.findSymbol(hash, _quadBuffer, qlen);
        if (name == null) {
            name = addPName(hash, _quadBuffer, qlen, lastByteCount);
        }
        return name;
    }

    /*
    /**********************************************************************
    /* Internal methods, input validation
    /**********************************************************************
     */

    /**
     * Method called to verify validity of given character (from entity) and
     * append it to the text buffer
     */
    protected void verifyAndAppendEntityCharacter(int charFromEntity) throws XMLStreamException
    {
        verifyXmlChar(charFromEntity);
        // Ok; does it need a surrogate though? (over 16 bits)
        if ((charFromEntity >> 16) != 0) {
            charFromEntity -= 0x10000;
            _textBuilder.append((char) (0xD800 | (charFromEntity >> 10)));
            charFromEntity = 0xDC00 | (charFromEntity & 0x3FF);
        }
        _textBuilder.append((char) charFromEntity);
    }

    /**
     * Checks that a character for a PublicId
     *
     * @param c A character
     * @return true if the character is valid for use in the Public ID
     * of an XML doctype declaration
     *
     * @see "http://www.w3.org/TR/xml/#NT-PubidLiteral"
     */
    protected boolean validPublicIdChar(int c) {
        return
            c == 0xA ||                     //<LF>
            c == 0xD ||                     //<CR>
            c == 0x20 ||                    //<SPACE>
            (c >= '@' && c <= 'Z') ||       //@[A-Z]
            (c >= 'a' && c <= 'z') ||
            c == '!' ||
            (c >= 0x23 && c <= 0x25) ||     //#$%
            (c >= 0x27 && c <= 0x2F) ||     //'()*+,-./
            (c >= ':' && c <= ';') ||
            c == '=' ||
            c == '?' ||
            c == '_';
    }

    /*
    /**********************************************************************
    /* Internal methods, error handling
    /**********************************************************************
     */

    @Override
    protected int decodeCharForError(byte b) throws XMLStreamException {
        // !!! TBI
        return (int) b;
    }

    protected void checkPITargetName(PName targetName) throws XMLStreamException
    {
        String ln = targetName.getLocalName();
        if (ln.length() == 3 && ln.equalsIgnoreCase("xml") &&
            !targetName.hasPrefix()) {
            reportInputProblem(ErrorConsts.ERR_WF_PI_XML_TARGET);
        }
    }

    protected int throwInternal() {
        throw new IllegalStateException("Internal error: should never execute this code path");
    }

    protected void reportInvalidOther(int mask, int ptr) throws XMLStreamException
    {
        _inputPtr = ptr;
        reportInvalidOther(mask);
    }
}
