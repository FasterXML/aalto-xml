/* Aalto XML processor
 *
 * Copyright (c) 2006- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fasterxml.aalto.async;

import java.io.IOException;
import javax.xml.stream.XMLStreamException;


import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.impl.ErrorConsts;
import com.fasterxml.aalto.in.*;
import com.fasterxml.aalto.util.CharsetNames;
import com.fasterxml.aalto.util.DataUtil;
//import com.fasterxml.aalto.util.XmlConsts;

/**
 * This is the base class for asynchronous (non-blocking) XML
 * scanners. Due to basic complexity of async approach, character-based
 * doesn't make much sense, so only byte-based input is supported.
 */
public abstract class AsyncByteScanner
    extends ByteBasedScanner
{
    private final static int EVENT_INCOMPLETE = AsyncXMLStreamReader.EVENT_INCOMPLETE;

    /*
    /**********************************************************************
    /* State consts
    /**********************************************************************
     */

    /**
     * Default starting state for many events/contexts -- nothing has been
     * seen so far, no  event incomplete. Not used for all event types.
     */
    final static int STATE_DEFAULT = 0;

    // // // States for prolog/epilog major state:

    /**
     * State in which a less-than sign has been seen
     */
    final static int STATE_PROLOG_INITIAL = 1; // State before document when we may get xml declaration
    final static int STATE_PROLOG_SEEN_LT = 2; // "<" seen after xml declaration
    final static int STATE_PROLOG_DECL = 3; // "<!" seen after xml declaration

    // // // States for in-tree major state:

    final static int STATE_TREE_SEEN_LT = 1; // "<" seen
    final static int STATE_TREE_SEEN_AMP = 2; // "&" seen
    final static int STATE_TREE_SEEN_EXCL = 3; // "<!" seen
    final static int STATE_TREE_SEEN_SLASH = 4; // "</" seen
    final static int STATE_TREE_NUMERIC_ENTITY_START = 5; // "&#" and part of value
    final static int STATE_TREE_NAMED_ENTITY_START = 6; // "&" and part of name

    // // // States within event types (STATE_DEFAULT is shared):

    // XML declaration parsing
    private final static int STATE_XMLDECL_AFTER_XML = 1; // "<?xml", need space
    private final static int STATE_XMLDECL_BEFORE_VERSION = 2; // "<?xml ", can have more spaces
    private final static int STATE_XMLDECL_VERSION = 3; // "<?xml ", part of "version"
    private final static int STATE_XMLDECL_AFTER_VERSION = 4; // "<?xml version", need space or '='
    private final static int STATE_XMLDECL_VERSION_EQ = 5; // "<?xml version=", need space or quote
    private final static int STATE_XMLDECL_VERSION_VALUE = 6; // parsing version value
    private final static int STATE_XMLDECL_AFTER_VERSION_VALUE = 7; // version got; need space or '?'
    private final static int STATE_XMLDECL_BEFORE_ENCODING = 8; // version, value, space got, need '?' or 'e'
    private final static int STATE_XMLDECL_ENCODING = 9; // parsing "encoding"
    private final static int STATE_XMLDECL_AFTER_ENCODING = 10; // 'encoding' got, need space or '='
    private final static int STATE_XMLDECL_ENCODING_EQ = 11; // "encoding="
    private final static int STATE_XMLDECL_ENCODING_VALUE = 12; // parsing encoding value
    private final static int STATE_XMLDECL_AFTER_ENCODING_VALUE = 13; // encoding+value gotten; need space or '?'
    private final static int STATE_XMLDECL_BEFORE_STANDALONE = 14; // after encoding+value+space; get '?' or 's'
    private final static int STATE_XMLDECL_STANDALONE = 15; // parsing "standalone"
    private final static int STATE_XMLDECL_AFTER_STANDALONE = 16; // 'standalone' got, need space or '='
    private final static int STATE_XMLDECL_STANDALONE_EQ = 17; // "standalone="
    private final static int STATE_XMLDECL_STANDALONE_VALUE = 18; // encoding+value gotten; need space or '?'
    private final static int STATE_XMLDECL_AFTER_STANDALONE_VALUE = 19; // encoding+value gotten; need space or '?'
    private final static int STATE_XMLDECL_ENDQ = 20; // "?" at the end of declaration

    // DOCTYPE declaration parsing
    private final static int STATE_DTD_DOCTYPE = 1; // part of "DOCTYPE"
    private final static int STATE_DTD_AFTER_DOCTYPE = 2; // "DOCTYPE", need space
    private final static int STATE_DTD_BEFORE_ROOT_NAME = 3; // optional space before root name
    private final static int STATE_DTD_ROOT_NAME = 4; // part of root name
    private final static int STATE_DTD_AFTER_ROOT_NAME = 5; // root name gotten; need a space or '>'
    private final static int STATE_DTD_BEFORE_IDS = 6; // before "PUBLIC" or "SYSTEM" token
    private final static int STATE_DTD_PUBLIC_OR_SYSTEM = 7; // parsing "PUBLIC" or "SYSTEM"
    private final static int STATE_DTD_AFTER_PUBLIC = 8; // "PUBLIC" found, need space
    private final static int STATE_DTD_AFTER_SYSTEM = 9; // "SYSTEM" found, need space
    private final static int STATE_DTD_BEFORE_PUBLIC_ID = 10; // after "PUBLIC", space, need quoted public id
    private final static int STATE_DTD_PUBLIC_ID = 11; // parsing public ID
    private final static int STATE_DTD_AFTER_PUBLIC_ID = 12; // public ID parsed, need space
    private final static int STATE_DTD_BEFORE_SYSTEM_ID = 13; // about to parse quoted system id
    private final static int STATE_DTD_SYSTEM_ID = 14; // parsing system ID
    private final static int STATE_DTD_AFTER_SYSTEM_ID = 15; // after system ID, optional space, '>' or int subset
    private final static int STATE_DTD_INT_SUBSET = 16; // parsing internal subset

    private final static int STATE_DTD_EXPECT_CLOSING_GT = 50; // ']' gotten that should be followed by '>'
    
    // For CHARACTERS, default is the basic (and only)

    // just seen "&"
    final static int STATE_TEXT_AMP = 4;
    // just seen "&#"
//    final static int STATE_TEXT_AMP_AND_HASH = 5;
    // seen '&' and partial name:
    final static int STATE_TEXT_AMP_NAME = 6;

    // For comments, STATE_DEFAULT means "<!-" has been seen
    final static int STATE_COMMENT_CONTENT = 1; // "<!--"
    final static int STATE_COMMENT_HYPHEN = 2; // content, and one '-'
    final static int STATE_COMMENT_HYPHEN2 = 3; // content, "--"

    // For cdata, STATE_DEFAULT means that just "<![" has been seen
    final static int STATE_CDATA_CONTENT = 1; // start marker seen, maybe some content
    final static int STATE_CDATA_C = 2; // "<![C"
    final static int STATE_CDATA_CD = 3; // "<![CD"
    final static int STATE_CDATA_CDA = 4; // "<![CDA"
    final static int STATE_CDATA_CDAT = 5; // "<![CDAT"
    final static int STATE_CDATA_CDATA = 6; // "<![CDATA"
    
    // For PIs, default means that '<?' has been seen, nothing else

    // (note: funny ordering, starting with "quick path" entries)
    final static int STATE_PI_AFTER_TARGET = 1; // "<?", target ?>
    final static int STATE_PI_AFTER_TARGET_WS = 2; // "<?", target, ws
    final static int STATE_PI_AFTER_TARGET_QMARK = 3; // "<?", target, "?"
    final static int STATE_PI_IN_TARGET = 4; // "<?", part of target
    final static int STATE_PI_IN_DATA = 5; // "<?", target, ws, part of data

    // For start element, DEFAULT means that only '<' has been seen
    final static int STATE_SE_ELEM_NAME = 1; // "<" and part of name
    final static int STATE_SE_SPACE_OR_END = 2; // after elem name or attr, but need space
    final static int STATE_SE_SPACE_OR_ATTRNAME = 3; // after elem/attr and space
    
    final static int STATE_SE_ATTR_NAME = 4; // in attribute name
    final static int STATE_SE_SPACE_OR_EQ = 5;
    final static int STATE_SE_SPACE_OR_ATTRVALUE = 6;
    final static int STATE_SE_ATTR_VALUE_NORMAL = 7;
    final static int STATE_SE_ATTR_VALUE_NSDECL = 8;
    final static int STATE_SE_SEEN_SLASH = 9;

    // For END_ELEMENT, default means we are parsing name
    final static int STATE_EE_NEED_GT = 1;
    
    /*
    /**********************************************************************
    /* Markers to use for 'pending' character, if
    /* not multi-byte UTF character
    /**********************************************************************
     */

    // Marker when dealing with general CR+LF pair
    final static int PENDING_STATE_CR = -1;

    // Parsing of possible XML declaration
    private final static int PENDING_STATE_XMLDECL_LT = -5; // "<" at start of doc
    private final static int PENDING_STATE_XMLDECL_LTQ = -6; // "<?" at start of doc
    private final static int PENDING_STATE_XMLDECL_TARGET = -7; // "<?" at start of doc, part of name
    
    // Processing Instruction parsing:
    final static int PENDING_STATE_PI_QMARK = -15;

    // Comment parsing
    final static int PENDING_STATE_COMMENT_HYPHEN1 = -20;
    final static int PENDING_STATE_COMMENT_HYPHEN2 = -21;

    // CData parsing
    final static int PENDING_STATE_CDATA_BRACKET1 = -30;
    final static int PENDING_STATE_CDATA_BRACKET2 = -31;

    final static int PENDING_STATE_ENT_SEEN_HASH = -70; // seen &#
    final static int PENDING_STATE_ENT_SEEN_HASH_X = -71; // seen &#x
    final static int PENDING_STATE_ENT_IN_DEC_DIGIT = -72; // seen &# and 1 or more decimals
    final static int PENDING_STATE_ENT_IN_HEX_DIGIT = -73; // seen &#x and 1 or more hex digits
//    final static int PENDING_STATE_ENT_IN_NAME = -; // seen & and part of the name
    
    /*
    /**********************************************************************
    /* Input buffer handling
    /**********************************************************************
     */

    /**
     * This buffer is actually provided by caller
     */
    protected byte[] _inputBuffer;

    /**
     * In addition to current buffer pointer, and end pointer,
     * we will also need to know number of bytes originally
     * contained. This is needed to correctly update location
     * information when the block has been completed.
     */
    protected int _origBufferLen;

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
    /* Instance construction
    /**********************************************************************
     */

    public AsyncByteScanner(ReaderConfig cfg)
    {
        super(cfg);
        // must start by checking if there's XML declaration...
        _state = STATE_PROLOG_INITIAL;
    }

    @Override
    public String toString()
    {
        return "asyncScanner; curr="+_currToken+" next="+_nextEvent+", state = "+_state;
    }

    /*
    /**********************************************************************
    /* Methods for subclasses to implement
    /**********************************************************************
     */
    
    protected abstract int parseCommentContents() throws XMLStreamException;

    protected abstract int parseCDataContents() throws XMLStreamException;

    protected abstract int parsePIData() throws XMLStreamException;

    /**
     * This method gets called, if the first character of a
     * CHARACTERS event could not be fully read (multi-byte,
     * split over buffer boundary). If so, there is some
     * pending data to be handled.
     */
    protected abstract int startCharactersPending() throws XMLStreamException;

    protected abstract int finishCharactersCoalescing() throws XMLStreamException;
    
    /*
    /**********************************************************************
    /* Async input, methods to feed (push) content to parse
    /**********************************************************************
     */

    public final boolean needMoreInput() {
        return (_inputPtr >=_inputEnd) && !_endOfInput;
    }

    public void feedInput(byte[] buf, int start, int len)
        throws XMLStreamException
    {
        // Must not have remaining input
        if (_inputPtr < _inputEnd) {
            throw new XMLStreamException("Still have "+(_inputEnd - _inputPtr)+" unread bytes");
        }
        // and shouldn't have been marked as end-of-input
        if (_endOfInput) {
            throw new XMLStreamException("Already closed, can not feed more input");
        }
        // Time to update pointers first
        _pastBytes += _origBufferLen;
        _rowStartOffset -= _origBufferLen;

        // And then update buffer settings
        _inputBuffer = buf;
        _inputPtr = start;
        _inputEnd = start+len;
        _origBufferLen = len;
    }

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
    protected void _closeSource()
        throws IOException
    {
        // nothing to do, we are done.
        _endOfInput = true;
    }

    /*
    /**********************************************************************
    /* Implementation of parsing API
    /**********************************************************************
     */

    public final int nextFromProlog(boolean isProlog)
        throws XMLStreamException
    {
        // Had fully complete event? Need to reset state etc:
        if (_currToken != EVENT_INCOMPLETE) {
            // yet one more special case: after START_DOCUMENT need to check things...
            if (_currToken == START_DOCUMENT) {
                _currToken = EVENT_INCOMPLETE;
                if (_tokenName != null) {
                    _nextEvent = PROCESSING_INSTRUCTION;
                    _state = STATE_PI_AFTER_TARGET;
                    checkPITargetName(_tokenName);
                    return handlePI();
                }
            } else {
                _currToken = _nextEvent = EVENT_INCOMPLETE;
                _state = STATE_DEFAULT;
            }
        }
        // Ok, do we know which event it will be?
        if (_nextEvent == EVENT_INCOMPLETE) { // nope
            // The very first thing: XML declaration handling
            if (_state == STATE_PROLOG_INITIAL) {
                if (_inputPtr >= _inputEnd) {
                    return _currToken;
                }
                // Ok: see if we have what looks like XML declaration; process:
                if (_pendingInput != 0) { // already parsing (potential) XML declaration
                    Boolean b = startXmlDeclaration(); // is or may be XML declaration, so:
                    if (b == null) { // not yet known; bail out
                        return EVENT_INCOMPLETE;
                    }
                    // no real XML declaration; syntesize one:
                    if (b == Boolean.FALSE) {
                        _currToken = START_DOCUMENT;
                        return START_DOCUMENT;
                    }
                    return handleXmlDeclaration();
                }
                if (_inputBuffer[_inputPtr] == BYTE_LT) { // first byte, see if it could be XML declaration
                    ++_inputPtr;
                    _pendingInput = PENDING_STATE_XMLDECL_LT;
                    Boolean b = startXmlDeclaration(); // is or may be XML declaration, so:
                    if (b == null) {
                        return EVENT_INCOMPLETE;
                    }
                    if (b == Boolean.FALSE) {
                        _currToken = START_DOCUMENT;
                        return START_DOCUMENT;
                    }
                    return handleXmlDeclaration();
                }
                // can't be XML declaration
                _state = STATE_DEFAULT;
                _currToken = START_DOCUMENT;
                return START_DOCUMENT;
            }

            // First: did we have a lone CR at the end of the buffer?
            if (_pendingInput != 0) { // yup
                if (!handlePartialCR()) {
                    return _currToken;
                }
            }
            while (_state == STATE_DEFAULT) {
                if (_inputPtr >= _inputEnd) { // no more input available
                    if (_endOfInput) { // for good? That may be fine
                        return TOKEN_EOI;
                    }
                    return _currToken;
                }
                byte b = _inputBuffer[_inputPtr++];

                /* Really should get white space or '<'... anything else is
                 * pretty much an error.
                 */
                if (b == BYTE_LT) { // root element, comment, proc instr?
                    _state = STATE_PROLOG_SEEN_LT;
                    break;
                }
                if (b == BYTE_SPACE || b == BYTE_CR
                    || b == BYTE_LF || b == BYTE_TAB) {
                    // Prolog/epilog ws is to be skipped, not part of Infoset
                    if (!asyncSkipSpace()) { // ran out of input?
                        if (_endOfInput) { // for good? That may be fine
                            return TOKEN_EOI;
                        }
                        return _currToken;
                    }
                } else {
                    reportPrologUnexpChar(isProlog, decodeCharForError(b), null);
                }
            }
            if (_state == STATE_PROLOG_SEEN_LT) {
                if (_inputPtr >= _inputEnd) {
                    return _currToken;
                }
                byte b = _inputBuffer[_inputPtr++];
                if (b == BYTE_EXCL) { // comment or DOCTYPE declaration?
                    _state = STATE_PROLOG_DECL;
                    return handlePrologDeclStart(isProlog);
                }
                if (b == BYTE_QMARK) { // PI
                    _nextEvent = PROCESSING_INSTRUCTION;
                    _state = STATE_DEFAULT;
                    return handlePI();
                }
                if (b == BYTE_SLASH || !isProlog) {
                    reportPrologUnexpChar(isProlog, decodeCharForError(b), " (unbalanced start/end tags?)");
                }
                return handleStartElementStart(b);
            }
            if (_state == STATE_PROLOG_DECL) {
                return handlePrologDeclStart(isProlog);
            }
            // should never have anything else...
            return throwInternal();
        }
        
        // At this point, we do know the event type
        switch (_nextEvent) {
        case START_ELEMENT:
            return handleStartElement();
        case START_DOCUMENT:
            return handleXmlDeclaration();
        case PROCESSING_INSTRUCTION:
            return handlePI();
        case COMMENT:
            return handleComment();
        case DTD:
            return handleDTD();
        }
        return throwInternal(); // should never get here
    }

    public int nextFromTree()
        throws XMLStreamException
    {
        // Had a fully complete event? Need to reset state:
        if (_currToken != EVENT_INCOMPLETE) {
            /* First, need to handle some complications arising from
             * empty elements, and namespace binding/unbinding:
             */
            if (_currToken == START_ELEMENT) {
                if (_isEmptyTag) {
                    --_depth;
                    return (_currToken = END_ELEMENT);
                }
            } else if (_currToken == END_ELEMENT) {
                _currElem = _currElem.getParent();
                // Any namespace declarations that need to be unbound?
                while (_lastNsDecl != null && _lastNsDecl.getLevel() >= _depth) {
                    _lastNsDecl = _lastNsDecl.unbind();
                }
            }

            /* Only CHARACTERS can remain incomplete: this happens if
             * first character is decoded, but coalescing mode is NOT
             * set. Skip can not therefore block, nor will add pending
             * input. Can also occur when we have run out of input
             */
            if (_tokenIncomplete) {
                if (!skipCharacters()) { // couldn't complete skipping
                    return EVENT_INCOMPLETE;
                }
                _tokenIncomplete = false;
            }
            _currToken = _nextEvent = EVENT_INCOMPLETE;
            _state = STATE_DEFAULT;
        }
        
        // Don't yet know the type?
        if (_nextEvent == EVENT_INCOMPLETE) {
            if (_state == STATE_DEFAULT) {
                /* We can only have pending input for (incomplete)
                 * CHARACTERS event.
                 */
                if (_pendingInput != 0) { // CR, or multi-byte?
                    _nextEvent = CHARACTERS;
                    return startCharactersPending();
                }
                if (_inputPtr >= _inputEnd) { // nothing we can do?
                    return _currToken; // i.e. EVENT_INCOMPLETE
                }
                byte b = _inputBuffer[_inputPtr++];
                if (b == BYTE_LT) { // root element, comment, proc instr?
                    _state = STATE_TREE_SEEN_LT;
                } else if (b == BYTE_AMP) {
                    _state = STATE_TREE_SEEN_AMP;
                } else {
                    _nextEvent = CHARACTERS;
                    return startCharacters(b);
                }
            }

            if (_inputPtr >= _inputEnd) {
                return _currToken; // i.e. EVENT_INCOMPLETE
            }
            if (_state == STATE_TREE_SEEN_LT) {
                // Ok, so we've just seen the less-than char...
                byte b = _inputBuffer[_inputPtr++];
                if (b == BYTE_EXCL) { // comment or CDATA
                    _state = STATE_TREE_SEEN_EXCL;
                } else if (b == BYTE_QMARK) {
                    _nextEvent = PROCESSING_INSTRUCTION;
                    _state = STATE_DEFAULT;
                    return handlePI();
                } else if (b == BYTE_SLASH) {
                    return handleEndElementStart();
                } else {
                    // Probably start element -- need to retain first char tho
                    return handleStartElementStart(b);
                }
            } else if (_state == STATE_TREE_SEEN_AMP) {
                return handleEntityStartingToken();
            } else if (_state == STATE_TREE_NAMED_ENTITY_START) {
                return handleNamedEntityStartingToken();
            } else if (_state == STATE_TREE_NUMERIC_ENTITY_START) {
                return handleNumericEntityStartingToken();
            }
                
            if (_state == STATE_TREE_SEEN_EXCL) {
                if (_inputPtr >= _inputEnd) {
                    return _currToken; // i.e. EVENT_INCOMPLETE
                }
                byte b = _inputBuffer[_inputPtr++];
                // Comment or CDATA?
                if (b == BYTE_HYPHEN) { // Comment
                    _nextEvent = COMMENT;
                    _state = STATE_DEFAULT;
                } else if (b == BYTE_LBRACKET) { // CDATA
                    _nextEvent = CDATA;
                    _state = STATE_DEFAULT;
                } else {
                    reportTreeUnexpChar(decodeCharForError(b), " (expected either '-' for COMMENT or '[CDATA[' for CDATA section)");
                }
            } else {
                throwInternal();
            }
        }
        
        /* We know the type; event is usually partially processed
         * and needs to be completely read.
         */
        switch (_nextEvent) {
        case START_ELEMENT:
            return handleStartElement();
        case END_ELEMENT:
            return handleEndElement();
        case PROCESSING_INSTRUCTION:
            return handlePI();
        case COMMENT:
            return handleComment();
        case CDATA:
            return handleCData();
        case CHARACTERS:
            if (!_cfgLazyParsing) {
                // !!! TBI: how would non-lazy mode work?
                if (_cfgCoalescing) {
                    return finishCharactersCoalescing();
                }
            }
            if (_pendingInput != 0) { // multi-byte, or CR without LF
                return startCharactersPending();
            }
            // Otherwise, should not get here
            throwInternal();
//        case ENTITY_REFERENCE:
        }
        return throwInternal(); // never gets here
    }

    /*
    /**********************************************************************
    /* Second-level parsing, prolog (XML declaration, DOCTYPE)
    /**********************************************************************
     */

    private final int handlePrologDeclStart(boolean isProlog)
        throws XMLStreamException
    {
        if (_inputPtr >= _inputEnd) { // nothing we can do?
            return EVENT_INCOMPLETE;
        }
        byte b = _inputBuffer[_inputPtr++];
        // So far, we have seen "<!", need to know if it's DTD or COMMENT 
        if (b == BYTE_HYPHEN) {
            _nextEvent = COMMENT;
            _state = STATE_DEFAULT;
            return handleComment();
        }
        if (b == BYTE_D) {
            _nextEvent = DTD;
            _state = STATE_DEFAULT;
            return handleDTD();
        }
        reportPrologUnexpChar(isProlog, decodeCharForError(b), " (expected '-' for COMMENT)");
        return EVENT_INCOMPLETE; // never gets here
    }
    
    /**
     * Method that deals with recognizing XML declaration, but not with parsing
     * its contents.
     * 
     * @return null if parsing is inconclusive (may or may not be XML declaration);
     *   Boolean.TRUE if complete XML declaration, and Boolean.FALSE if something
     *   else
     */
    private final Boolean startXmlDeclaration() throws XMLStreamException
    {
       if (_inputPtr >= _inputEnd) {
           return null;
       }
       if (_pendingInput == PENDING_STATE_XMLDECL_LT) { // "<" at start of doc
            if (_inputBuffer[_inputPtr] != BYTE_QMARK) { // some other 
                _pendingInput = 0;
                _state = STATE_PROLOG_SEEN_LT;
                return Boolean.FALSE;
            }
            ++_inputPtr;
            _pendingInput = PENDING_STATE_XMLDECL_LTQ;
            if (_inputPtr >= _inputEnd) {
                return null;
            }
       }
       if (_pendingInput == PENDING_STATE_XMLDECL_LTQ) { // "<?" at start of doc
            byte b = _inputBuffer[_inputPtr++];
            _tokenName = parseNewName(b);
            if (_tokenName == null) { // incomplete
                _pendingInput = PENDING_STATE_XMLDECL_TARGET;
                return null;
            }
            // xml or not?
            if (!"xml".equals(_tokenName.getPrefixedName())) { // nope: some other PI
                _pendingInput = 0;
                _state = STATE_PI_AFTER_TARGET;
                _nextEvent = PROCESSING_INSTRUCTION;
                checkPITargetName(_tokenName);
                return Boolean.FALSE;
            }
       } else if (_pendingInput == PENDING_STATE_XMLDECL_TARGET) { // "<?" at start of doc, part of name
            if ((_tokenName = parsePName()) == null) { // incomplete
                return null;
            }
            if (!"xml".equals(_tokenName.getPrefixedName())) {
                _pendingInput = 0;
                _state = STATE_PI_AFTER_TARGET;
                _nextEvent = PROCESSING_INSTRUCTION;
                checkPITargetName(_tokenName);
                return Boolean.FALSE;
            }
        } else {
           throwInternal();
        }
        _pendingInput = 0;
        _nextEvent = START_DOCUMENT;
        _state = STATE_XMLDECL_AFTER_XML;
        return Boolean.TRUE;
    }

    /**
     * Method called to complete parsing of XML declaration, once it has
     * been reliably detected.
     * 
     * @return Completed token (START_DOCUMENT), if fully parsed; incomplete (EVENT_INCOMPLETE)
     *   otherwise
     */
    private int handleXmlDeclaration() throws XMLStreamException
    {
        // First: left-over CRs?
        if (_pendingInput == PENDING_STATE_CR) {
            if (!handlePartialCR()) {
                return EVENT_INCOMPLETE;
            }
        }

        main_loop:
        while (_inputPtr < _inputEnd) {
            switch (_state) {
            case STATE_XMLDECL_AFTER_XML: // "<?xml", need space
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_SPACE || b == BYTE_CR || b == BYTE_LF || b == BYTE_TAB) {
                        _state = STATE_XMLDECL_BEFORE_VERSION;
                    } else {
                        reportPrologUnexpChar(true, decodeCharForError(b), " (expected space after 'xml' in xml declaration)");
                    }
                }
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            case STATE_XMLDECL_BEFORE_VERSION:
                if (!asyncSkipSpace()) { // not enough input
                    break;
                }
                if ((_tokenName = parseNewName(_inputBuffer[_inputPtr++])) == null) { // incomplete
                    _state = STATE_XMLDECL_VERSION;
                    break;
                }
                if (!_tokenName.hasPrefixedName("version")) {
                    reportInputProblem("Unexpected keyword '"+_tokenName.getPrefixedName()+"' in XML declaration: expected 'version'");
                }
                _state = STATE_XMLDECL_AFTER_VERSION;
                continue main_loop;
            case STATE_XMLDECL_VERSION: // "<?xml ", part of "version"
                if ((_tokenName = parsePName()) == null) { // incomplete
                    break;
                }
                if (!_tokenName.hasPrefixedName("version")) {
                    reportInputProblem("Unexpected keyword '"+_tokenName.getPrefixedName()+"' in XML declaration: expected 'version'");
                }
                _state = STATE_XMLDECL_AFTER_VERSION;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            case STATE_XMLDECL_AFTER_VERSION: // "<?xml version", need space or '='
                if (!asyncSkipSpace()) { // not enough input
                    break;
                }
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b != BYTE_EQ) {
                        reportPrologUnexpChar(true, decodeCharForError(b), " (expected '=' after 'version' in xml declaration)");
                    }
                }
                _state = STATE_XMLDECL_VERSION_EQ;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            case STATE_XMLDECL_VERSION_EQ: // "<?xml version=", need space or quote
                if (!asyncSkipSpace()) { // skip space, if any
                    break;
                }
                _elemAttrQuote = _inputBuffer[_inputPtr++];
                if (_elemAttrQuote != BYTE_QUOT && _elemAttrQuote != BYTE_APOS) {
                    reportPrologUnexpChar(true, decodeCharForError(_elemAttrQuote), " (expected '\"' or ''' in xml declaration for version value)");
                }
                {
                    char[] buf = _textBuilder.resetWithEmpty();
                    if (_inputPtr >= _inputEnd || !parseXmlDeclAttr(buf, 0)) {
                        _state = STATE_XMLDECL_VERSION_VALUE;
                        break;
                    }
                }
                verifyAndSetXmlVersion();
                _state = STATE_XMLDECL_AFTER_VERSION_VALUE;
                continue main_loop;
    
            case STATE_XMLDECL_VERSION_VALUE: // parsing version value
                if (!parseXmlDeclAttr(_textBuilder.getBufferWithoutReset(), _textBuilder.getCurrentLength())) {
                    _state = STATE_XMLDECL_VERSION_VALUE;
                    break;
                }
                verifyAndSetXmlVersion();
                _state = STATE_XMLDECL_AFTER_VERSION_VALUE;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
                
            case STATE_XMLDECL_AFTER_VERSION_VALUE: // version got; need space or '?'
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_QMARK) {
                        _state = STATE_XMLDECL_ENDQ;
                        continue main_loop;
                    }
                    if (b == BYTE_SPACE || b == BYTE_CR || b == BYTE_LF || b == BYTE_TAB) {
                        _state = STATE_XMLDECL_BEFORE_ENCODING;
                    } else {
                        reportPrologUnexpChar(true, decodeCharForError(b), " (expected space after version value in xml declaration)");
                    }
                }
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
                
            case STATE_XMLDECL_BEFORE_ENCODING: // version, value, space got, need '?' or 'e'
                if (!asyncSkipSpace()) { // not enough input
                    break;
                }
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_QMARK) {
                        _state = STATE_XMLDECL_ENDQ;
                        continue main_loop;
                    }
                    if ((_tokenName = parseNewName(b)) == null) { // incomplete
                        _state = STATE_XMLDECL_ENCODING;
                        break;
                    }
                    // Can actually also get "standalone" instead...
                    if (_tokenName.hasPrefixedName("encoding")) {
                        _state = STATE_XMLDECL_AFTER_ENCODING;
                    } else if (_tokenName.hasPrefixedName("standalone")) {
                        _state = STATE_XMLDECL_AFTER_STANDALONE;
                        continue main_loop;
                    } else {
                        reportInputProblem("Unexpected keyword '"+_tokenName.getPrefixedName()+"' in XML declaration: expected 'encoding'");
                    }
                }
                continue main_loop;
    
            case STATE_XMLDECL_ENCODING: // parsing "encoding"
                if ((_tokenName = parsePName()) == null) { // incomplete
                    break;
                }
                // Can actually also get "standalone" instead...
                if (_tokenName.hasPrefixedName("encoding")) {
                    _state = STATE_XMLDECL_AFTER_ENCODING;
                } else if (_tokenName.hasPrefixedName("standalone")) {
                    _state = STATE_XMLDECL_AFTER_STANDALONE;
                    continue main_loop;
                } else {
                    reportInputProblem("Unexpected keyword '"+_tokenName.getPrefixedName()+"' in XML declaration: expected 'encoding'");
                }
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            case STATE_XMLDECL_AFTER_ENCODING: // got "encoding"; must get ' ' or '='
                if (!asyncSkipSpace()) { // not enough input
                    break;
                }
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b != BYTE_EQ) {
                        reportPrologUnexpChar(true, decodeCharForError(b), " (expected '=' after 'encoding' in xml declaration)");
                    }
                }
                _state = STATE_XMLDECL_ENCODING_EQ;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            case STATE_XMLDECL_ENCODING_EQ: // "encoding="
                if (!asyncSkipSpace()) { // skip space, if any
                    break;
                }
                _elemAttrQuote = _inputBuffer[_inputPtr++];
                if (_elemAttrQuote != BYTE_QUOT && _elemAttrQuote != BYTE_APOS) {
                    reportPrologUnexpChar(true, decodeCharForError(_elemAttrQuote), " (expected '\"' or ''' in xml declaration for encoding value)");
                }
                _state = STATE_XMLDECL_ENCODING_VALUE;
                {
                    char[] buf = _textBuilder.resetWithEmpty();
                    if (_inputPtr >= _inputEnd || !parseXmlDeclAttr(buf, 0)) {
                        _state = STATE_XMLDECL_ENCODING_VALUE;
                        break;
                    }
                }
                verifyAndSetXmlEncoding();
                _state = STATE_XMLDECL_AFTER_ENCODING_VALUE;
                break;
    
            case STATE_XMLDECL_ENCODING_VALUE: // parsing encoding value
                if (!parseXmlDeclAttr(_textBuilder.getBufferWithoutReset(), _textBuilder.getCurrentLength())) {
                    _state = STATE_XMLDECL_ENCODING_VALUE;
                    break;
                }
                verifyAndSetXmlEncoding();
                _state = STATE_XMLDECL_AFTER_ENCODING_VALUE;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
                
            case STATE_XMLDECL_AFTER_ENCODING_VALUE: // encoding+value gotten; need space or '?'
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_QMARK) {
                        _state = STATE_XMLDECL_ENDQ;
                        continue main_loop;
                    }
                    if (b == BYTE_SPACE || b == BYTE_CR || b == BYTE_LF || b == BYTE_TAB) {
                        _state = STATE_XMLDECL_BEFORE_STANDALONE;
                    } else {
                        reportPrologUnexpChar(true, decodeCharForError(b), " (expected space after encoding value in xml declaration)");
                    }
                }
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            
            case STATE_XMLDECL_BEFORE_STANDALONE: // after encoding+value+space; get '?' or 's'
                if (!asyncSkipSpace()) { // not enough input
                    break;
                }
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_QMARK) {
                        _state = STATE_XMLDECL_ENDQ;
                        continue main_loop;
                    }
                    if ((_tokenName = parseNewName(b)) == null) { // incomplete
                        _state = STATE_XMLDECL_STANDALONE;
                        break;
                    }
                    if (!_tokenName.hasPrefixedName("standalone")) {
                        reportInputProblem("Unexpected keyword '"+_tokenName.getPrefixedName()+"' in XML declaration: expected 'standalone'");
                    }
                }
                _state = STATE_XMLDECL_AFTER_STANDALONE;
                continue main_loop;
    
            case STATE_XMLDECL_STANDALONE: // parsing "standalone"
                if ((_tokenName = parsePName()) == null) { // incomplete
                    break;
                }
                if (!_tokenName.hasPrefixedName("standalone")) {
                    reportInputProblem("Unexpected keyword 'encoding' in XML declaration: expected 'standalone'");
                }
                _state = STATE_XMLDECL_AFTER_STANDALONE;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            case STATE_XMLDECL_AFTER_STANDALONE: // got "standalone"; must get ' ' or '='
                if (!asyncSkipSpace()) { // not enough input
                    break;
                }
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b != BYTE_EQ) {
                        reportPrologUnexpChar(true, decodeCharForError(b), " (expected '=' after 'standalone' in xml declaration)");
                    }
                }
                _state = STATE_XMLDECL_STANDALONE_EQ;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            case STATE_XMLDECL_STANDALONE_EQ: // "standalone="
                if (!asyncSkipSpace()) { // skip space, if any
                    break;
                }
                _elemAttrQuote = _inputBuffer[_inputPtr++];
                if (_elemAttrQuote != BYTE_QUOT && _elemAttrQuote != BYTE_APOS) {
                    reportPrologUnexpChar(true, decodeCharForError(_elemAttrQuote), " (expected '\"' or ''' in xml declaration for standalone value)");
                }
                {
                    char[] buf = _textBuilder.resetWithEmpty();
                    if (_inputPtr >= _inputEnd || !parseXmlDeclAttr(buf, 0)) {
                        _state = STATE_XMLDECL_STANDALONE_VALUE;
                        break;
                    }
                }
                verifyAndSetXmlStandalone();
                _state = STATE_XMLDECL_AFTER_STANDALONE_VALUE;
                continue main_loop;
    
            case STATE_XMLDECL_STANDALONE_VALUE: // encoding+value gotten; need space or '?'
    
                if (!parseXmlDeclAttr(_textBuilder.getBufferWithoutReset(), _textBuilder.getCurrentLength())) {
                    _state = STATE_XMLDECL_STANDALONE_VALUE;
                    break;
                }
                verifyAndSetXmlStandalone();
                _state = STATE_XMLDECL_AFTER_STANDALONE_VALUE;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            case STATE_XMLDECL_AFTER_STANDALONE_VALUE: // encoding+value gotten; need space or '?'
                if (!asyncSkipSpace()) { // skip space, if any
                    break;
                }
                if (_inputBuffer[_inputPtr++] != BYTE_QMARK) {
                    reportPrologUnexpChar(true, decodeCharForError(_inputBuffer[_inputPtr-1]), " (expected '?>' to end xml declaration)");
                }
                _state = STATE_XMLDECL_ENDQ;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
    
            case STATE_XMLDECL_ENDQ:
                // Better clear up decoded name, to avoid later problems (would be taken as PI)
                _tokenName = null;
                _state = STATE_DEFAULT;
                _nextEvent = EVENT_INCOMPLETE;
                if (_inputBuffer[_inputPtr++] != BYTE_GT) {
                    reportPrologUnexpChar(true, decodeCharForError(_inputBuffer[_inputPtr-1]), " (expected '>' to end xml declaration)");
                }
                return START_DOCUMENT;
    
            default:
                throwInternal();
            }
        }

        return EVENT_INCOMPLETE;
    }
    
    private int handleDTD() throws XMLStreamException
    {
        // First: left-over CRs?
        if (_pendingInput == PENDING_STATE_CR) {
            if (!handlePartialCR()) {
                return EVENT_INCOMPLETE;
            }
        }
        if (_state == STATE_DTD_INT_SUBSET) {
            if (handleDTDInternalSubset(false)) { // got it!
                _state = STATE_DTD_EXPECT_CLOSING_GT;
            } else {
                return EVENT_INCOMPLETE;
            }
        }
        
        main_loop:
        while (_inputPtr < _inputEnd) {
            switch (_state) {
            case STATE_DEFAULT: // seen 'D'
                _tokenName = parseNewName(BYTE_D);
                if (_tokenName == null) {
                    _state = STATE_DTD_DOCTYPE;
                    return EVENT_INCOMPLETE;
                }
                if (!"DOCTYPE".equals(_tokenName.getPrefixedName())) {
                    reportPrologProblem(true, "expected 'DOCTYPE'");
                }
                _state = STATE_DTD_AFTER_DOCTYPE;
                continue main_loop;
            case STATE_DTD_DOCTYPE:
                _tokenName = parsePName();
                if (_tokenName == null) {
                    _state = STATE_DTD_DOCTYPE;
                    return EVENT_INCOMPLETE;
                }
                if (!"DOCTYPE".equals(_tokenName.getPrefixedName())) {
                    reportPrologProblem(true, "expected 'DOCTYPE'");
                }
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            case STATE_DTD_AFTER_DOCTYPE:
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_SPACE || b == BYTE_CR || b == BYTE_LF || b == BYTE_TAB) {
                        _state = STATE_DTD_BEFORE_ROOT_NAME;
                    } else {
                        reportPrologUnexpChar(true, decodeCharForError(b), " (expected space after 'DOCTYPE')");
                    }
                }
                // fall through (ok to skip bounds checks, async-skip does it)
            case STATE_DTD_BEFORE_ROOT_NAME:
                if (!asyncSkipSpace()) { // not enough input
                    break;
                }
                if ((_tokenName = parseNewName(_inputBuffer[_inputPtr++])) == null) { // incomplete
                    _state = STATE_DTD_ROOT_NAME;
                    break;
                }
                _state = STATE_DTD_ROOT_NAME;
                continue main_loop;
            case STATE_DTD_ROOT_NAME:
                if ((_tokenName = parsePName()) == null) { // incomplete
                    break;
                }
                _state = STATE_DTD_AFTER_ROOT_NAME;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            case STATE_DTD_AFTER_ROOT_NAME:
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_GT) {
                        _state = STATE_DEFAULT;
                        _nextEvent = EVENT_INCOMPLETE;
                        return DTD;
                    }
                    if (b == BYTE_SPACE || b == BYTE_CR || b == BYTE_LF || b == BYTE_TAB) {
                        _state = STATE_DTD_BEFORE_IDS;
                    } else {
                        reportPrologUnexpChar(true, decodeCharForError(b), " (expected space after root name in DOCTYPE declaration)");
                    }
                }
                // fall through (ok to skip bounds checks, async-skip does it)
            case STATE_DTD_BEFORE_IDS:
                if (!asyncSkipSpace()) { // not enough input
                    break;
                }
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_GT) {
                        _state = STATE_DEFAULT;
                        _nextEvent = EVENT_INCOMPLETE;
                        return DTD;
                    }
                    PName name;
                    if ((name = parseNewName(b)) == null) {
                        _state = STATE_DTD_PUBLIC_OR_SYSTEM;
                        break;
                    }
                    String str = name.getPrefixedName();
                    if ("PUBLIC".equals(str)) {
                        _state = STATE_DTD_AFTER_PUBLIC;
                    } else if ("SYSTEM".equals(str)) {
                        _state = STATE_DTD_AFTER_SYSTEM;
                    } else {
                        reportPrologProblem(true, "unexpected token '"+str+"': expected either PUBLIC or SYSTEM");
                    }
                }
                continue main_loop;
    
            case STATE_DTD_PUBLIC_OR_SYSTEM: 
                {
                    PName name;
                    if ((name = parsePName()) == null) {
                        _state = STATE_DTD_PUBLIC_OR_SYSTEM;
                        break;
                    }
                    String str = name.getPrefixedName();
                    if ("PUBLIC".equals(str)) {
                        _state = STATE_DTD_AFTER_PUBLIC;
                    } else if ("SYSTEM".equals(str)) {
                        _state = STATE_DTD_AFTER_SYSTEM;
                    } else {
                        reportPrologProblem(true, "unexpected token '"+str+"': expected either PUBLIC or SYSTEM");
                    }
                }
                continue main_loop;
                    
            case STATE_DTD_AFTER_PUBLIC: 
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_SPACE || b == BYTE_CR || b == BYTE_LF || b == BYTE_TAB) {
                        _state = STATE_DTD_BEFORE_PUBLIC_ID;
                    } else {
                        reportPrologUnexpChar(true, decodeCharForError(b), " (expected space after PUBLIC keyword)");
                    }
                }
                continue main_loop;
    
            case STATE_DTD_AFTER_SYSTEM: 
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_SPACE || b == BYTE_CR || b == BYTE_LF || b == BYTE_TAB) {
                        _state = STATE_DTD_BEFORE_SYSTEM_ID;
                    } else {
                        reportPrologUnexpChar(true, decodeCharForError(b), " (expected space after SYSTEM keyword)");
                    }
                }
                continue main_loop;
    
            case STATE_DTD_BEFORE_PUBLIC_ID: 
                if (!asyncSkipSpace()) {
                    break;
                }
                _elemAttrQuote = _inputBuffer[_inputPtr++];
                if (_elemAttrQuote != BYTE_QUOT && _elemAttrQuote != BYTE_APOS) {
                    reportPrologUnexpChar(true, decodeCharForError(_elemAttrQuote), " (expected '\"' or ''' for PUBLIC ID)");
                }
                {
                    char[] buf = _textBuilder.resetWithEmpty();
                    if (_inputPtr >= _inputEnd || !parseDtdId(buf, 0)) {
                        _state = STATE_DTD_PUBLIC_ID;
                        break;
                    }
                }
                verifyAndSetPublicId();
                _state = STATE_DTD_AFTER_PUBLIC_ID;
                continue main_loop;
    
            case STATE_DTD_PUBLIC_ID: 
                if (!parseDtdId(_textBuilder.getBufferWithoutReset(), _textBuilder.getCurrentLength())) {
                    break;
                }
                verifyAndSetPublicId();
                _state = STATE_DTD_AFTER_PUBLIC_ID;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
            case STATE_DTD_AFTER_PUBLIC_ID: 
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_SPACE || b == BYTE_CR || b == BYTE_LF || b == BYTE_TAB) {
                        _state = STATE_DTD_BEFORE_SYSTEM_ID;
                    } else {
                        reportPrologUnexpChar(true, decodeCharForError(b), " (expected space after PUBLIC ID)");
                    }
                }
                // fall through (ok to skip bounds checks, async-skip does it)
    
            case STATE_DTD_BEFORE_SYSTEM_ID: 
                if (!asyncSkipSpace()) {
                    break;
                }
                _elemAttrQuote = _inputBuffer[_inputPtr++];
                if (_elemAttrQuote != BYTE_QUOT && _elemAttrQuote != BYTE_APOS) {
                    reportPrologUnexpChar(true, decodeCharForError(_elemAttrQuote), " (expected '\"' or ''' for SYSTEM ID)");
                }
                {
                    char[] buf = _textBuilder.resetWithEmpty();
                    if (_inputPtr >= _inputEnd || !parseDtdId(buf, 0)) {
                        _state = STATE_DTD_SYSTEM_ID;
                        break;
                    }
                }
                verifyAndSetSystemId();
                _state = STATE_DTD_AFTER_SYSTEM_ID;
                continue main_loop;

            case STATE_DTD_SYSTEM_ID: 
                if (!parseDtdId(_textBuilder.getBufferWithoutReset(), _textBuilder.getCurrentLength())) {
                    break;
                }
                verifyAndSetSystemId();
                _state = STATE_DTD_AFTER_SYSTEM_ID;
                if (_inputPtr >= _inputEnd) {
                    break;
                }
                // fall through
    
            case STATE_DTD_AFTER_SYSTEM_ID:
                if (!asyncSkipSpace()) {
                    break;
                }
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_GT) {
                        _state = STATE_DEFAULT;
                        _nextEvent = EVENT_INCOMPLETE;
                        return DTD;
                    }
                    if (b != BYTE_LBRACKET) {
                        reportPrologUnexpChar(true, decodeCharForError(_elemAttrQuote), " (expected either '[' for internal subset, or '>' to end DOCTYPE)");
                    }
                }
                _state = STATE_DTD_INT_SUBSET;
                if (handleDTDInternalSubset(true)) {
                    _state = STATE_DTD_EXPECT_CLOSING_GT;
                } else {
                    return EVENT_INCOMPLETE;
                }
                // fall through
                
            case STATE_DTD_EXPECT_CLOSING_GT:
                if (!asyncSkipSpace()) {
                    break;
                }
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b != BYTE_GT) {
                        reportPrologUnexpChar(true, b, "expected '>' to end DTD");
                    }
                }
                _state = STATE_DEFAULT;
                _nextEvent = EVENT_INCOMPLETE;
                return DTD;
            default:
                throwInternal();
            }
        }
        return _currToken;
    }

    /**
     * @param init Whether this is the first call (and state needs to be initialized) or not
     *
     * @return True if parsing was completed; false if not.
     */
    protected abstract boolean handleDTDInternalSubset(boolean init) throws XMLStreamException;
    
    private final boolean parseDtdId(char[] buffer, int ptr) throws XMLStreamException
    {
        final int quote = (int) _elemAttrQuote;
        while (_inputPtr < _inputEnd) {
            int ch = _inputBuffer[_inputPtr++] & 0xFF;
            if (ch == quote) {
                _textBuilder.setCurrentLength(ptr);
                return true;
            }
            // this is not exact check; but does work for all legal (valid) characters:
            if (ch <= INT_SPACE || ch > 0x7E) {
                reportPrologUnexpChar(true, decodeCharForError((byte) ch), " (not valid in PUBLIC or SYSTEM ID)");
            }
            if (ptr >= buffer.length) {
                buffer = _textBuilder.finishCurrentSegment();
                ptr = 0;
            }
            buffer[ptr++] = (char) ch;
        }
        _textBuilder.setCurrentLength(ptr);
        return false;
    }
     
    /**
     * Method called to try to parse an XML pseudo-attribute value. This is relatively
     * simple, since we can't have linefeeds or entities; and although there are exact
     * rules for what is allowed, we can do coarse parsing and only later on verify
     * validity (for encoding could do stricter parsing in future?)
     * 
     * @return True if we managed to parse the whole pseudo-attribute
     */
    private boolean parseXmlDeclAttr(char[] buffer, int ptr) throws XMLStreamException
    {
        final int quote = (int) _elemAttrQuote;
        while (_inputPtr < _inputEnd) {
            int ch = _inputBuffer[_inputPtr++] & 0xFF;
            if (ch == quote) {
                _textBuilder.setCurrentLength(ptr);
                return true;
            }
            // this is not exact check; but does work for all legal (valid) characters:
            if (ch <= INT_SPACE || ch > INT_z) {
                reportPrologUnexpChar(true, decodeCharForError((byte) ch), " (not valid in XML pseudo-attribute values)");
            }
            if (ptr >= buffer.length) {
                buffer = _textBuilder.finishCurrentSegment();
                ptr = 0;
            }
            buffer[ptr++] = (char) ch;
        }
        _textBuilder.setCurrentLength(ptr);
        return false;
    }
    
    private final void verifyAndSetXmlVersion() throws XMLStreamException
    {
        if (_textBuilder.equalsString("1.0")) {
            _config.setXmlVersion("1.0");
        } else if (_textBuilder.equalsString("1.1")) {
            _config.setXmlVersion("1.1");
        } else {
            reportInputProblem("Unrecognized XML version '"+_textBuilder.contentsAsString()+"' (expected '1.0' or '1.1')");
        }
    }

    private final void verifyAndSetXmlEncoding() throws XMLStreamException
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

    private final void verifyAndSetXmlStandalone() throws XMLStreamException
    {
        if (_textBuilder.equalsString("yes")) {
            _config.setXmlStandalone(Boolean.TRUE);
        } else if (_textBuilder.equalsString("no")) {
            _config.setXmlStandalone(Boolean.FALSE);
        } else {
            reportInputProblem("Invalid standalone value '"+_textBuilder.contentsAsString()+"': can only use 'yes' and 'no'");
        }
    }

    private final void verifyAndSetPublicId() throws XMLStreamException
    {
        _publicId = _textBuilder.contentsAsString();
    }

    private final void verifyAndSetSystemId() throws XMLStreamException
    {
        _systemId = _textBuilder.contentsAsString();
    }

    /*
    /**********************************************************************
    /* Second-level parsing; character content (in tree)
    /**********************************************************************
     */

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
    protected abstract int startCharacters(byte b)
        throws XMLStreamException;

    private int handleCData() throws XMLStreamException
    {
        if (_state == STATE_CDATA_CONTENT) {
            return parseCDataContents();
        }
        if (_inputPtr >= _inputEnd) {
            return EVENT_INCOMPLETE;
        }
        return handleCDataStartMarker(_inputBuffer[_inputPtr++]);
    }
    
    private int handleCDataStartMarker(byte b)
        throws XMLStreamException
    {
        switch (_state) {
        case STATE_DEFAULT:
            if (b != BYTE_C) {
                reportTreeUnexpChar(decodeCharForError(b), " (expected 'C' for CDATA)");
            }
            _state = STATE_CDATA_C;
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }
            b = _inputBuffer[_inputPtr++];
            // fall through
        case STATE_CDATA_C:
            if (b != BYTE_D) {
                reportTreeUnexpChar(decodeCharForError(b), " (expected 'D' for CDATA)");
            }
            _state = STATE_CDATA_CD;
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }
            b = _inputBuffer[_inputPtr++];
            // fall through
        case STATE_CDATA_CD:
            if (b != BYTE_A) {
                reportTreeUnexpChar(decodeCharForError(b), " (expected 'A' for CDATA)");
            }
            _state = STATE_CDATA_CDA;
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }
            b = _inputBuffer[_inputPtr++];
            // fall through
        case STATE_CDATA_CDA:
            if (b != BYTE_T) {
                reportTreeUnexpChar(decodeCharForError(b), " (expected 'T' for CDATA)");
            }
            _state = STATE_CDATA_CDAT;
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }
            b = _inputBuffer[_inputPtr++];
            // fall through
        case STATE_CDATA_CDAT:
            if (b != BYTE_A) {
                reportTreeUnexpChar(decodeCharForError(b), " (expected 'A' for CDATA)");
            }
            _state = STATE_CDATA_CDATA;
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }
            b = _inputBuffer[_inputPtr++];
            // fall through
        case STATE_CDATA_CDATA:
            if (b != BYTE_LBRACKET) {
                reportTreeUnexpChar(decodeCharForError(b), " (expected '[' for CDATA)");
            }
            _textBuilder.resetWithEmpty();
            _state = STATE_CDATA_CONTENT;
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }
            return parseCDataContents();
        }
        return throwInternal();
    }
    
    /*
    /**********************************************************************
    /* Second-level parsing; other (PI, Comment)
    /**********************************************************************
     */
    
    private int handlePI()
        throws XMLStreamException
    {
        // Most common case first:
        if (_state == STATE_PI_IN_DATA) {
            return parsePIData();
        }

        main_loop:
        while (true) {
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }
            switch (_state) {
            case STATE_DEFAULT:
                _tokenName = parseNewName(_inputBuffer[_inputPtr++]);
                if (_tokenName == null) {
                    _state = STATE_PI_IN_TARGET;
                    return EVENT_INCOMPLETE;
                }
                _state = STATE_PI_AFTER_TARGET;
                checkPITargetName(_tokenName);
                if (_inputPtr >= _inputEnd) {
                    return EVENT_INCOMPLETE;
                }
                // fall through
            case STATE_PI_AFTER_TARGET:
                // Need ws or "?>"
                {
                    byte b = _inputBuffer[_inputPtr++];
                    if (b == BYTE_QMARK) {
                        // Quick check, can we see '>' as well? All done, if so
                        if (_inputPtr < _inputEnd && _inputBuffer[_inputPtr] == BYTE_GT) {
                            ++_inputPtr;
                            break main_loop; // means we are done
                        }
                        // If not (whatever reason), let's move to check state
                        _state = STATE_PI_AFTER_TARGET_QMARK;
                        break;
                    }
                    if (b == BYTE_SPACE || b == BYTE_CR
                               || b == BYTE_LF || b == BYTE_TAB) {
                        if (!asyncSkipSpace()) { // ran out of input?
                            _state = STATE_PI_AFTER_TARGET_WS;
                            return EVENT_INCOMPLETE;
                        }
                        _textBuilder.resetWithEmpty();
                        // Quick check, perhaps we'll see end marker?
                        if ((_inputPtr+1) < _inputEnd
                            && _inputBuffer[_inputPtr] == BYTE_QMARK
                            && _inputBuffer[_inputPtr+1] == BYTE_GT) {
                            _inputPtr += 2;
                            break main_loop; // means we are done
                        }
                        // If not, we'll move to 'data' portion of PI
                        _state = STATE_PI_IN_DATA;
                        return parsePIData();
                    }
                    // Otherwise, it's an error
                    reportMissingPISpace(decodeCharForError(b));
                }
                // fall through
            case STATE_PI_AFTER_TARGET_WS:
                if (!asyncSkipSpace()) { // ran out of input?
                    return EVENT_INCOMPLETE;
                }
                // Can just move to "data" portion right away
                _state = STATE_PI_IN_DATA;
                _textBuilder.resetWithEmpty();
                return parsePIData();
            case STATE_PI_AFTER_TARGET_QMARK:
                {
                    // Must get '>' following '?' we saw right after name
                    byte b = _inputBuffer[_inputPtr++];
                    // Otherwise, it's an error
                    if (b != BYTE_GT) {
                        reportMissingPISpace(decodeCharForError(b));
                    }
                }
                // but if it's ok, we are done
                break main_loop;
            case STATE_PI_IN_TARGET:
                _tokenName = parsePName();
                if (_tokenName == null) {
                    return EVENT_INCOMPLETE;
                }
                checkPITargetName(_tokenName);
                _state = STATE_PI_AFTER_TARGET;
                break;
                
            default:
                return throwInternal();
            }
        }
        
        _state = STATE_DEFAULT;
        _nextEvent = EVENT_INCOMPLETE;
        return PROCESSING_INSTRUCTION;
    }

    private final int handleComment() throws XMLStreamException
    {
        if (_state == STATE_COMMENT_CONTENT) {
            return parseCommentContents();
        }
        if (_inputPtr >= _inputEnd) {
            return EVENT_INCOMPLETE;
        }
        byte b = _inputBuffer[_inputPtr++];
        
        if (_state == STATE_DEFAULT) {
            if (b != BYTE_HYPHEN) {
                reportTreeUnexpChar(decodeCharForError(b), " (expected '-' for COMMENT)");
            }
            _state = STATE_COMMENT_CONTENT;
            _textBuilder.resetWithEmpty();
            return parseCommentContents();
        }
        if (_state == STATE_COMMENT_HYPHEN2) {
            // We are almost done, just need to get '>' at the end
            if (b != BYTE_GT) {
                reportDoubleHyphenInComments();
            }
            _state = STATE_DEFAULT;
            _nextEvent = EVENT_INCOMPLETE;
            return COMMENT;
        }
        return throwInternal();
    }
    
    /*
    /**********************************************************************
    /* Second-level parsing; helper methods
    /**********************************************************************
     */

    /**
     * Method to skip whatever space can be skipped.
     *<p>
     * NOTE: if available content ends with a CR, method will set
     * <code>_pendingInput</code> to <code>PENDING_STATE_CR</code>.
     * 
     * @return True, if was able to skip through the space and find
     *   a non-space byte; false if reached end-of-buffer
     */
    private boolean asyncSkipSpace() throws XMLStreamException
    {
        while (_inputPtr < _inputEnd) {
            byte b = _inputBuffer[_inputPtr];
            if ((b & 0xFF) > INT_SPACE) {
                // hmmmh. Shouldn't this be handled someplace else?
                if (_pendingInput == PENDING_STATE_CR) {
                    markLF();
                    _pendingInput = 0;
                }
                return true;
            }
            ++_inputPtr;
            if (b == BYTE_LF) {
                markLF();
            } else if (b == BYTE_CR) {
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = PENDING_STATE_CR;
                    break;
                }
                if (_inputBuffer[_inputPtr] == BYTE_LF) {
                    ++_inputPtr;
                }
                markLF();
            } else if (b != BYTE_SPACE && b != BYTE_TAB) {
                throwInvalidSpace(b);
            }
        }
        return false;
    }

    /**
     * Method called when a new token (within tree) starts with an
     * entity.
     *   
     * @return Type of event to return
     */
    protected int handleEntityStartingToken()
        throws XMLStreamException
    {
        _textBuilder.resetWithEmpty();
        byte b = _inputBuffer[_inputPtr++]; // we know one is available
        if (b == BYTE_HASH) { // numeric character entity
            _textBuilder.resetWithEmpty();
            _state = STATE_TREE_NUMERIC_ENTITY_START;
            _pendingInput = PENDING_STATE_ENT_SEEN_HASH;
            if (_inputPtr >= _inputEnd) { // but no more content to parse yet
                return EVENT_INCOMPLETE;
            }
            return handleNumericEntityStartingToken();
        }
        PName n = parseNewEntityName(b);
        // null if incomplete; non-null otherwise
        if (n == null) {
            // Not sure if it's a char entity or general one; so we don't yet know type
            _state = STATE_TREE_NAMED_ENTITY_START;
            return EVENT_INCOMPLETE;
        }
        int ch = decodeGeneralEntity(n);
        if (ch == 0) { // not a character entity
            _tokenName = n;
            return (_nextEvent = _currToken = ENTITY_REFERENCE);
        }
        // character entity; initialize buffer,
        _textBuilder.resetWithChar((char)ch);
        _nextEvent = 0;
        _currToken = CHARACTERS;
        if (_cfgLazyParsing) {
            _tokenIncomplete = true;
        } else {
            finishCharacters();
        }
        return _currToken;
    }

    /**
     * Method called when we see an entity that is starting a new token,
     * and part of its name has been decoded (but not all)
     */
    protected int handleNamedEntityStartingToken()
        throws XMLStreamException
    {
        PName n = parseEntityName();
        // null if incomplete; non-null otherwise
        if (n == null) {
            return _nextEvent; // i.e. EVENT_INCOMPLETE
        }
        int ch = decodeGeneralEntity(n);
        if (ch == 0) { // not a character entity
            _tokenName = n;
            return (_currToken = ENTITY_REFERENCE);
        }
        // character entity; initialize buffer,
        _textBuilder.resetWithChar((char)ch);
        _nextEvent = 0;
        _currToken = CHARACTERS;
        if (_cfgLazyParsing) {
            _tokenIncomplete = true;
        } else {
            finishCharacters();
        }
        return _currToken;
    }

    /**
     * Method called to handle cases where we find something other than
     * a character entity (or one of 4 pre-defined general entities that
     * act like character entities)
     */
    protected int handleNumericEntityStartingToken()
        throws XMLStreamException
    {
        if (_pendingInput == PENDING_STATE_ENT_SEEN_HASH) {
            byte b = _inputBuffer[_inputPtr]; // we know one is available
            _entityValue = 0;
            if (b == BYTE_x) { // 'x' marks hex
                _pendingInput = PENDING_STATE_ENT_IN_HEX_DIGIT;
                if (++_inputPtr >= _inputEnd) {
                    return EVENT_INCOMPLETE;
                }
            } else { // if not 'x', must be a digit
                _pendingInput = PENDING_STATE_ENT_IN_DEC_DIGIT;
                // let's just keep byte for calculation
            }
        }
        if (_pendingInput == PENDING_STATE_ENT_IN_HEX_DIGIT) {
            if (!decodeHexEntity()) {
                return EVENT_INCOMPLETE;
            }
        } else {
            if (!decodeDecEntity()) {
                return EVENT_INCOMPLETE;
            }
        }
        // and now we have the full value
        verifyAndAppendEntityCharacter(_entityValue);
        _currToken = CHARACTERS;
        if (_cfgLazyParsing) {
            _tokenIncomplete = true;
        } else {
            finishCharacters();
        }
        _pendingInput = 0;
        return _currToken;
    }

    /**
     * @return True if entity was decoded (and value assigned to <code>_entityValue</code>;
     *    false otherwise
     */
    protected final boolean decodeHexEntity() throws XMLStreamException
    {
        int value = _entityValue;
        while (_inputPtr < _inputEnd) {
            byte b = _inputBuffer[_inputPtr++];
            if (b == BYTE_SEMICOLON) {
                _entityValue = value;
                return true;
            }
            int ch = (int) b;
            if (ch <= INT_9 && ch >= INT_0) {
                ch -= INT_0;
            } else if (ch <= INT_F && ch >= INT_A) {
                ch = 10 + (ch - INT_A);
            } else  if (ch <= INT_f && ch >= INT_a) {
                ch = 10 + (ch - INT_a);
            } else {
                throwUnexpectedChar(decodeCharForError(b), " expected a hex digit (0-9a-fA-F) for character entity");
            }
            value = (value << 4) + ch;
            if (value > MAX_UNICODE_CHAR) { // Overflow?
                _entityValue = value;
                reportEntityOverflow();
            }
        }
        _entityValue = value;
        return false;
    }        

    /**
     * @return True if entity was decoded (and value assigned to <code>_entityValue</code>;
     *    false otherwise
     */
    protected final boolean decodeDecEntity() throws XMLStreamException
    {
        int value = _entityValue;
        while (_inputPtr < _inputEnd) {
            byte b = _inputBuffer[_inputPtr++];
            if (b == BYTE_SEMICOLON) {
                _entityValue = value;
                return true;
            }
            int ch = ((int) b) - INT_0;
            if (ch < 0 || ch > 9) { // invalid entity
                throwUnexpectedChar(decodeCharForError(b), " expected a digit (0 - 9) for character entity");
            }
            value = (value * 10) + ch;
            if (value > MAX_UNICODE_CHAR) { // Overflow?
                _entityValue = value;
                reportEntityOverflow();
            }
        }
        _entityValue = value;
        return false;
    }        

    /**
     * Method that verifies that given named entity is followed by 
     * a semi-colon (meaning next byte must be available for reading);
     * and if so, whether it is one of pre-defined general entities.
     * 
     * @return Character of the expanded pre-defined general entity
     *   (if name matches one); zero if not.
     */
    protected final int decodeGeneralEntity(PName entityName)
        throws XMLStreamException
    {
        // First things first: verify that we got semicolon afterwards
        byte b = _inputBuffer[_inputPtr++];
        if (b != BYTE_SEMICOLON) {
            throwUnexpectedChar(decodeCharForError(b), " expected ';' following entity name (\""+entityName.getPrefixedName()+"\")");
        }
        
        String name = entityName.getPrefixedName();
        if (name == "amp") {
            return INT_AMP;
        }
        if (name == "lt") {
            return INT_LT;
        }
        if (name == "apos") {
            return INT_APOS;
        }
        if (name == "quot") {
            return INT_QUOTE;
        }
        if (name == "gt") {
            return INT_GT;
        }
        return 0;
    }

    /**
     * Method called when '<' and (what appears to be) a name
     * start character have been seen.
     */
    protected int handleStartElementStart(byte b)
        throws XMLStreamException
    {
        PName elemName = parseNewName(b);
        _nextEvent = START_ELEMENT;
        if (elemName == null) {
            _state = STATE_SE_ELEM_NAME;
            return EVENT_INCOMPLETE;
        }
        initStartElement(elemName);
        return handleStartElement();
    }

    protected int handleStartElement()
        throws XMLStreamException
    {
        main_loop:
        while (true) {
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }

            byte b;
            int c;

            switch (_state) {
            case STATE_SE_ELEM_NAME:
                {
                    PName elemName = parsePName();
                    if (elemName == null) {
                        return EVENT_INCOMPLETE;
                    }
                    initStartElement(elemName);
                }
                if (_inputPtr >= _inputEnd) {
                    return EVENT_INCOMPLETE;
                }
                // Fall through to next state
                
            case STATE_SE_SPACE_OR_END: // obligatory space, or end
                if (_pendingInput != 0) {
                    if (!handlePartialCR()) {
                        return EVENT_INCOMPLETE;
                    }
                    // Ok, got a space, can move on
                } else {
                    b = _inputBuffer[_inputPtr++];
                    c = (int) b & 0xFF;
                    
                    if (c <= INT_SPACE) {
                        if (c == INT_LF) {
                            markLF();
                        } else if (c == INT_CR) {
                            if (_inputPtr >= _inputEnd) {
                                _pendingInput = PENDING_STATE_CR;
                                return EVENT_INCOMPLETE;
                            }
                            if (_inputBuffer[_inputPtr] == BYTE_LF) {
                                ++_inputPtr;
                            }
                            markLF();
                        } else if (c != INT_SPACE && c != INT_TAB) {
                            throwInvalidSpace(c);
                        }
                    } else if (c == INT_GT) { // must be '/' or '>'
                        return finishStartElement(false);
                    } else if (c == INT_SLASH) {
                        _state = STATE_SE_SEEN_SLASH;
                        continue main_loop;
                    } else {
                        throwUnexpectedChar(decodeCharForError(b), " expected space, or '>' or \"/>\"");
                    }
                }
                _state = STATE_SE_SPACE_OR_ATTRNAME;
                if (_inputPtr >= _inputEnd) {
                    return EVENT_INCOMPLETE;
                }
                // can fall through, again:

            case STATE_SE_SPACE_OR_ATTRNAME:
            case STATE_SE_SPACE_OR_EQ:
            case STATE_SE_SPACE_OR_ATTRVALUE:
                /* Common to these states is that there may be leading space(s),
                 * so let's see if any has to be skipped
                 */
                if (_pendingInput != 0) {
                    if (!handlePartialCR()) {
                        return EVENT_INCOMPLETE;
                    }
                    if (_inputPtr >= _inputEnd) {
                        return EVENT_INCOMPLETE;
                    }
                }
                b = _inputBuffer[_inputPtr++];
                c = (int) b & 0xFF;

                while (c <= INT_SPACE) {
                    if (c == INT_LF) {
                        markLF();
                    } else if (c == INT_CR) {
                        if (_inputPtr >= _inputEnd) {
                            _pendingInput = PENDING_STATE_CR;
                            return EVENT_INCOMPLETE;
                        }
                        if (_inputBuffer[_inputPtr] == BYTE_LF) {
                            ++_inputPtr;
                        }
                        markLF();
                    } else if (c != INT_SPACE && c != INT_TAB) {
                        throwInvalidSpace(c);
                    }
                    if (_inputPtr >= _inputEnd) {
                        return EVENT_INCOMPLETE;
                    }
                    b = _inputBuffer[_inputPtr++];
                    c = (int) b & 0xFF;
                }

                switch (_state) {
                case STATE_SE_SPACE_OR_ATTRNAME:
                    if (b == BYTE_SLASH) {
                        _state = STATE_SE_SEEN_SLASH;
                        continue main_loop;
                    }
                    if (b == BYTE_GT) {
                        return finishStartElement(false);
                    }
                    {
                        PName n = parseNewName(b);
                        if (n == null) {
                            _state = STATE_SE_ATTR_NAME;
                            return EVENT_INCOMPLETE;
                        }
                        _state = STATE_SE_SPACE_OR_EQ;
                        _elemAttrName = n;
                    }
                    continue main_loop;

                case STATE_SE_SPACE_OR_EQ:
                    if (b != BYTE_EQ) {
                        throwUnexpectedChar(decodeCharForError(b), " expected '='");
                    }
                    _state = STATE_SE_SPACE_OR_ATTRVALUE;
                    continue main_loop;

                case STATE_SE_SPACE_OR_ATTRVALUE:
                    if (b != BYTE_QUOT && b != BYTE_APOS) {
                        throwUnexpectedChar(decodeCharForError(b), " Expected a quote");
                    }
                    initAttribute(b);
                    continue main_loop;
                default:
                    throwInternal();
                }
                
            case STATE_SE_ATTR_NAME:
                {
                    PName n = parsePName();
                    if (n == null) {
                        return EVENT_INCOMPLETE;
                    }
                    _elemAttrName = n;
                    _state = STATE_SE_SPACE_OR_EQ;
                }
                break;

            case STATE_SE_ATTR_VALUE_NORMAL:
                if (!handleAttrValue()) {
                    return EVENT_INCOMPLETE;
                }
                _state = STATE_SE_SPACE_OR_END;
                break;

            case STATE_SE_ATTR_VALUE_NSDECL:
                if (!handleNsDecl()) {
                    return EVENT_INCOMPLETE;
                }
                _state = STATE_SE_SPACE_OR_END;
                break;

            case STATE_SE_SEEN_SLASH:
                {
                    b = _inputBuffer[_inputPtr++];
                    if (b != BYTE_GT) {
                        throwUnexpectedChar(decodeCharForError(b), " expected '>'");
                    }
                    return finishStartElement(true);
                }
            default:
                throwInternal();
            }
        }
    }

    private void initStartElement(PName elemName)
    {
        String prefix = elemName.getPrefix();
        if (prefix == null) { // element in default ns
            _elemAllNsBound = true; // which need not be bound
        } else {
            elemName = bindName(elemName, prefix);
            _elemAllNsBound = elemName.isBound();
        }
        _tokenName = elemName;
        _currElem = new ElementScope(elemName, _currElem);
        _attrCount = 0;
        _currNsCount = 0;
        _elemAttrPtr = 0;
        _state = STATE_SE_SPACE_OR_END;
    }

    private void initAttribute(byte quoteChar)
    {
        _elemAttrQuote = quoteChar;
        
        PName attrName = _elemAttrName;
        String prefix = attrName.getPrefix();
        boolean nsDecl;
 
        if (prefix == null) { // can be default ns decl:
            nsDecl = (attrName.getLocalName() == "xmlns");
        } else {
            // May be a namespace decl though?
            if (prefix == "xmlns") {
                nsDecl = true;
            } else {
                attrName = bindName(attrName, prefix);
                if (_elemAllNsBound) {
                    _elemAllNsBound = attrName.isBound();
                }
                nsDecl = false;
            }
        }

        if (nsDecl) {
            _state = STATE_SE_ATTR_VALUE_NSDECL;
            // Ns decls use name buffer transiently
            _elemNsPtr = 0;
            ++_currNsCount;
        } else {
            _state = STATE_SE_ATTR_VALUE_NORMAL;
            // Regular attributes are appended, shouldn't reset ptr
            _attrCollector.startNewValue(attrName, _elemAttrPtr);
        }
    }

    protected abstract boolean handleAttrValue()
        throws XMLStreamException;

    protected abstract boolean handleNsDecl()
        throws XMLStreamException;

    /**
     * Method called to wrap up settings when the whole start
     * (or empty) element has been parsed.
     */
    private int finishStartElement(boolean emptyTag)
        throws XMLStreamException
    {
        _isEmptyTag = emptyTag;

        // Note: this call also checks attribute uniqueness
        int act = _attrCollector.finishLastValue(_elemAttrPtr);
        if (act < 0) { // error, dup attr indicated by -1
            act = _attrCollector.getCount(); // let's get correct count
            reportInputProblem(_attrCollector.getErrorMsg());
        }
        _attrCount = act;
        ++_depth;

        /* Was there any prefix that wasn't bound prior to use?
         * That's legal, assuming declaration was found later on...
         * let's check
         */
        if (!_elemAllNsBound) {
            if (!_tokenName.isBound()) { // element itself unbound
                reportUnboundPrefix(_tokenName, false);
            }
            for (int i = 0, len = _attrCount; i < len; ++i) {
                PName attrName = _attrCollector.getName(i);
                if (!attrName.isBound()) {
                    reportUnboundPrefix(attrName, true);
                }
            }
        }

        return (_currToken = START_ELEMENT);
    }

    private int handleEndElementStart()
        throws XMLStreamException
    {
        --_depth;
        _tokenName = _currElem.getName();

        /* Ok, perhaps we can do this quickly? This works, if we
         * are expected to have the full name (plus one more byte
         * to indicate name end) in the current buffer:
         */
        int size = _tokenName.sizeInQuads();
        if ((_inputEnd - _inputPtr) < ((size << 2) + 1)) { // may need to load more
            _nextEvent = END_ELEMENT;
            _state = STATE_DEFAULT;
            _quadCount = _currQuad = _currQuadBytes = 0;
            /* No, need to take it slow. Can not yet give up, though,
             * without reading remainder of the buffer
             */
            return handleEndElement();
        }
        byte[] buf = _inputBuffer;
        
        // First all full chunks of 4 bytes (if any)
        --size;
        for (int qix = 0; qix < size; ++qix) {
            int ptr = _inputPtr;
            int q = (buf[ptr] << 24)
                | ((buf[ptr+1] & 0xFF) << 16)
                | ((buf[ptr+2] & 0xFF) << 8)
                | ((buf[ptr+3] & 0xFF))
                ;
            _inputPtr += 4;
            // match?
            if (q != _tokenName.getQuad(qix)) {
                reportUnexpectedEndTag(_tokenName.getPrefixedName());
            }
        }
        
        /* After which we can deal with the last entry: it's bit
         * tricky as we don't actually fully know byte length...
         */
        int lastQ = _tokenName.getQuad(size);
        int q = buf[_inputPtr++] & 0xFF;
        if (q != lastQ) { // need second byte?
            q = (q << 8) | (buf[_inputPtr++] & 0xFF);
            if (q != lastQ) { // need third byte?
                q = (q << 8) | (buf[_inputPtr++] & 0xFF);
                if (q != lastQ) { // need full 4 bytes?
                    q = (q << 8) | (buf[_inputPtr++] & 0xFF);
                    if (q != lastQ) { // still no match? failure!
                        reportUnexpectedEndTag(_tokenName.getPrefixedName());
                    }
                }
            }
        }
        // Trailing space?
        int i2 = _inputBuffer[_inputPtr++] & 0xFF;
        while (i2 <= INT_SPACE) {
            if (i2 == INT_LF) {
                markLF();
            } else if (i2 == INT_CR) {
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = PENDING_STATE_CR;
                    _nextEvent = END_ELEMENT;
                    _state = STATE_EE_NEED_GT;
                    return EVENT_INCOMPLETE;
                }
                if (_inputBuffer[_inputPtr] == BYTE_LF) {
                    ++_inputPtr;
                }
                markLF();
            } else if (i2 != INT_SPACE && i2 != INT_TAB) {
                throwInvalidSpace(i2);
            }
            if (_inputPtr >= _inputEnd) {
                _nextEvent = END_ELEMENT;
                _state = STATE_EE_NEED_GT;
                return EVENT_INCOMPLETE;
            }
            i2 = _inputBuffer[_inputPtr++] & 0xFF;
        }
        if (i2 != INT_GT) {
            throwUnexpectedChar(decodeCharForError((byte)i2), " expected space or closing '>'");
        }
        return (_currToken = END_ELEMENT);
    }

    /**
     * This method is "slow" version of above, used when name of
     * the end element can split input buffer boundary
     */
    private int handleEndElement()
        throws XMLStreamException
    {
        if (_state == STATE_DEFAULT) { // parsing name
            final PName elemName = _tokenName;
            final int quadSize = elemName.sizeInQuads() - 1; // need to ignore last for now
            for (; _quadCount < quadSize; ++_quadCount) { // first, full quads
                for (; _currQuadBytes < 4; ++_currQuadBytes) {
                    if (_inputPtr >= _inputEnd) {
                        return EVENT_INCOMPLETE;
                    }
                    _currQuad = (_currQuad << 8) | (_inputBuffer[_inputPtr++] & 0xFF);
                }
                // match?
                if (_currQuad != elemName.getQuad(_quadCount)) {
                    reportUnexpectedEndTag(elemName.getPrefixedName());
                }
                _currQuad = _currQuadBytes = 0;
            }
            // So far so good! Now need to check the last quad:
            int lastQ = elemName.getLastQuad();
        
            while (true) {
                if (_inputPtr >= _inputEnd) {
                    return EVENT_INCOMPLETE;
                }
                int q = (_currQuad << 8);
                q |= (_inputBuffer[_inputPtr++] & 0xFF);
                _currQuad = q;
                if (q == lastQ) { // match
                    break;
                }
                if (++_currQuadBytes > 3) { // no match, error
                    reportUnexpectedEndTag(elemName.getPrefixedName());
                    break; // never gets here
                }
            }
            // Bueno. How about optional space, '>'?
            _state = STATE_EE_NEED_GT;
        } else if (_state != STATE_EE_NEED_GT) {
            throwInternal();
        }

        if (_pendingInput != 0) {
            if (!handlePartialCR()) {
                return EVENT_INCOMPLETE;
            }
            // it's ignorable ws
        }
        
        // Trailing space?
        while (true) {
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }
            int i2 = _inputBuffer[_inputPtr++] & 0xFF;
            if (i2 <= INT_SPACE) {
                if (i2 == INT_LF) {
                    markLF();
                } else if (i2 == INT_CR) {
                    if (_inputPtr >= _inputEnd) {
                        _pendingInput = PENDING_STATE_CR;
                        return EVENT_INCOMPLETE;
                    }
                    if (_inputBuffer[_inputPtr] == BYTE_LF) {
                        ++_inputPtr;
                    }
                    markLF();
                } else if (i2 != INT_SPACE && i2 != INT_TAB) {
                    throwInvalidSpace(i2);
                }
                continue;
            }

            if (i2 != INT_GT) {
                throwUnexpectedChar(decodeCharForError((byte)i2), " expected space or closing '>'");
            }
            // Hah, done!
            return (_currToken = END_ELEMENT);
        }
    }

    /*
    /**********************************************************************
    /* Abstract methods from base class, parsing
    /**********************************************************************
     */

    protected abstract void finishCharacters()
        throws XMLStreamException;

    protected void finishCData()
        throws XMLStreamException
    {
        // N/A
        throwInternal();
    }

    protected void finishComment()
        throws XMLStreamException
    {
        // N/A
        throwInternal();
    }

    protected void finishDTD(boolean copyContents)
        throws XMLStreamException
    {
        // N/A
        throwInternal();
    }

    protected void finishPI()
        throws XMLStreamException
    {
        // N/A
        throwInternal();
    }

    protected void finishSpace()
        throws XMLStreamException
    {
        // N/A
        throwInternal();
    }

    // // token-skip methods

    /**
     * @return True if the whole characters segment was succesfully
     *   skipped; false if not
     */
    protected abstract boolean skipCharacters()
        throws XMLStreamException;

    protected void skipCData() throws XMLStreamException
    {
        // should never be called
        throwInternal();
    }

    protected void skipComment() throws XMLStreamException
    {
        // should never be called
        throwInternal();
    }

    protected void skipPI() throws XMLStreamException
    {
        // should never be called
        throwInternal();
    }

    protected void skipSpace() throws XMLStreamException
    {
        // should never be called
        throwInternal();
    }

    protected boolean loadMore() throws XMLStreamException
    {
        // should never get called
        throwInternal();
        return false; // never gets here
    }

    /*
    /**********************************************************************
    /* Common name/entity parsing
    /**********************************************************************
     */

    protected PName parseNewName(byte b)
        throws XMLStreamException
    {
        int q = b & 0xFF;

        /* Let's do just quick sanity check first; a thorough check will be
         * done later on if necessary, now we'll just do the very cheap
         * check to catch extra spaces etc.
         */
        if (q < INT_A) { // lowest acceptable start char, except for ':' that would be allowed in non-ns mode
            throwUnexpectedChar(q, "; expected a name start character");
        }
        _quadCount = 0;
        _currQuad = q;
        _currQuadBytes = 1;
        return parsePName();
    }

    /**
     * This method can (for now?) be shared between all Ascii-based
     * encodings, since it only does coarse validity checking -- real
     * checks are done in different method.
     *<p>
     * Some notes about assumption implementation makes:
     *<ul>
     * <li>Well-formed xml content can not end with a name: as such,
     *    end-of-input is an error and we can throw an exception
     *  </li>
     * </ul>
     */
    protected PName parsePName()
        throws XMLStreamException
    {
        int q = _currQuad;

        while (true) {
            int i;

            switch (_currQuadBytes) {
            case 0:
                if (_inputPtr >= _inputEnd) {
                    return null; // all pointers have been set
                }
                q = _inputBuffer[_inputPtr++] & 0xFF;
                /* Since name char validity is checked later on, we only
                 * need to be able to reliably see the end of the name...
                 * and those are simple enough so that we can just
                 * compare; lookup table won't speed things up (according
                 * to profiler)
                 */
                if (q < 65) { // 'A'
                    // Ok; "_" (45), "." (46) and "0"-"9"/":" (48 - 57/58) still name chars
                    if (q < 45 || q > 58 || q == 47) {
                        // End of name
                        return findPName(q, 0);
                    }
                }
                // fall through

            case 1:
                if (_inputPtr >= _inputEnd) { // need to store pointers
                    _currQuad = q;
                    _currQuadBytes = 1;
                    return null;
                }
                i = _inputBuffer[_inputPtr++] & 0xFF;
                if (i < 65) { // 'A'
                    if (i < 45 || i > 58 || i == 47) {
                        return findPName(q, 1);
                    }
                }
                q = (q << 8) | i;
                // fall through

            case 2:
                if (_inputPtr >= _inputEnd) { // need to store pointers
                    _currQuad = q;
                    _currQuadBytes = 2;
                    return null;
                }
                i = _inputBuffer[_inputPtr++] & 0xFF;
                if (i < 65) { // 'A'
                    if (i < 45 || i > 58 || i == 47) {
                        return findPName(q, 2);
                    }
                }
                q = (q << 8) | i;
                // fall through

            case 3:
                if (_inputPtr >= _inputEnd) { // need to store pointers
                    _currQuad = q;
                    _currQuadBytes = 3;
                    return null;
                }
                i = _inputBuffer[_inputPtr++] & 0xFF;
                if (i < 65) { // 'A'
                    if (i < 45 || i > 58 || i == 47) {
                        return findPName(q, 3);
                    }
                }
                q = (q << 8) | i;
            }

            /* If we get this far, need to add full quad into
             * result array and update state
             */
            if (_quadCount == 0) { // first quad
                _quadBuffer[0] = q;
                _quadCount = 1;
            } else {
                if (_quadCount >= _quadBuffer.length) { // let's just double?
                    _quadBuffer = DataUtil.growArrayBy(_quadBuffer, _quadBuffer.length);
                }
                _quadBuffer[_quadCount++] = q;
            }
            _currQuadBytes = 0;
        }
    }

    protected final PName parseNewEntityName(byte b)
        throws XMLStreamException
    {
        int q = b & 0xFF;
        if (q < INT_A) {
            throwUnexpectedChar(q, "; expected a name start character");
        }
        _quadCount = 0;
        _currQuad = q;
        _currQuadBytes = 1;
        return parseEntityName();
    }
    
    protected final PName parseEntityName()
        throws XMLStreamException
    {
        int q = _currQuad;

        while (true) {
            int i;

            switch (_currQuadBytes) {
            case 0:
                if (_inputPtr >= _inputEnd) {
                    return null; // all pointers have been set
                }
                q = _inputBuffer[_inputPtr++] & 0xFF;
                /* Since name char validity is checked later on, we only
                 * need to be able to reliably see the end of the name...
                 * and those are simple enough so that we can just
                 * compare; lookup table won't speed things up (according
                 * to profiler)
                 */
                if (q < 65) { // 'A'
                    // Ok; "_" (45), "." (46) and "0"-"9"/":" (48 - 57/58) still name chars
                    if (q < 45 || q > 58 || q == 47) {
                        // apos, quot?
                        if (_quadCount == 1) {
                            q = _quadBuffer[0];
                            if (q == EntityNames.ENTITY_APOS_QUAD) {
                                --_inputPtr;
                                return EntityNames.ENTITY_APOS;
                            }
                            if (q == EntityNames.ENTITY_QUOT_QUAD) {
                                --_inputPtr;
                                return EntityNames.ENTITY_QUOT;
                            }
                        }
                        // Nope, generic:
                        return findPName(q, 0);
                    }
                }
                // fall through

            case 1:
                if (_inputPtr >= _inputEnd) { // need to store pointers
                    _currQuad = q;
                    _currQuadBytes = 1;
                    return null;
                }
                i = _inputBuffer[_inputPtr++] & 0xFF;
                if (i < 65) { // 'A'
                    if (i < 45 || i > 58 || i == 47) {
                        return findPName(q, 1);
                    }
                }
                q = (q << 8) | i;
                // fall through

            case 2:
                if (_inputPtr >= _inputEnd) { // need to store pointers
                    _currQuad = q;
                    _currQuadBytes = 2;
                    return null;
                }
                i = _inputBuffer[_inputPtr++] & 0xFF;
                if (i < 65) { // 'A'
                    if (i < 45 || i > 58 || i == 47) {
                        // lt or gt?
                        if (_quadCount == 0) {
                            if (q == EntityNames.ENTITY_GT_QUAD) {
                                --_inputPtr;
                                return EntityNames.ENTITY_GT;
                            }
                            if (q == EntityNames.ENTITY_LT_QUAD) {
                                --_inputPtr;
                                return EntityNames.ENTITY_LT;
                            }
                        }
                        return findPName(q, 2);
                    }
                }
                q = (q << 8) | i;
                // fall through

            case 3:
                if (_inputPtr >= _inputEnd) { // need to store pointers
                    _currQuad = q;
                    _currQuadBytes = 3;
                    return null;
                }
                i = _inputBuffer[_inputPtr++] & 0xFF;
                if (i < 65) { // 'A'
                    if (i < 45 || i > 58 || i == 47) {
                        // amp?
                        if (_quadCount == 0) {
                            if (q == EntityNames.ENTITY_AMP_QUAD) {
                                --_inputPtr;
                                return EntityNames.ENTITY_AMP;
                            }
                        }
                        return findPName(q, 3);
                    }
                }
                q = (q << 8) | i;
            }

            /* If we get this far, need to add full quad into
             * result array and update state
             */
            if (_quadCount == 0) { // first quad
                _quadBuffer[0] = q;
                _quadCount = 1;
            } else {
                if (_quadCount >= _quadBuffer.length) { // let's just double?
                    _quadBuffer = DataUtil.growArrayBy(_quadBuffer, _quadBuffer.length);
                }
                _quadBuffer[_quadCount++] = q;
            }
            _currQuadBytes = 0;
        }
    }

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
    private final PName findPName(int lastQuad, int lastByteCount)
        throws XMLStreamException
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
    /* methods from base class, name handling
    /**********************************************************************
     */

    protected abstract PName addPName(int hash, int[] quads, int qlen, int lastQuadBytes)
        throws XMLStreamException;

    /**
     * Method called to verify validity of given character (from entity) and
     * append it to the text buffer
     */
    protected void verifyAndAppendEntityCharacter(int charFromEntity)
        throws XMLStreamException
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

    /*
    /**********************************************************************
    /* Internal methods, LF handling
    /**********************************************************************
     */

    /**
     * Method called when there is a pending \r (from past buffer),
     * and we need to see
     *
     * @return True if the linefeed was succesfully processed (had
     *   enough input data to do that); or false if there is no
     *   data available to check this
     */
    protected final boolean handlePartialCR()
    {
        // sanity check
        if (_pendingInput != PENDING_STATE_CR) {
            throwInternal();
        }
        if (_inputPtr >= _inputEnd) {
            return false;
        }
        _pendingInput = 0;
        if (_inputBuffer[_inputPtr] == BYTE_LF) {
            ++_inputPtr;
        }
        ++_currRow;
        _rowStartOffset = _inputPtr;
        return true;
    }

    /*
    /**********************************************************************
    /* Internal methods, error handling
    /**********************************************************************
     */

    protected int decodeCharForError(byte b)
        throws XMLStreamException
    {
        // !!! TBI
        return (int) b;
    }

    private void checkPITargetName(PName targetName)
        throws XMLStreamException
    {
        String ln = targetName.getLocalName();
        if (ln.length() == 3 && ln.equalsIgnoreCase("xml") &&
            !targetName.hasPrefix()) {
            reportInputProblem(ErrorConsts.ERR_WF_PI_XML_TARGET);
        }
    }

    protected int throwInternal()
    {
        throw new IllegalStateException("Internal error: should never execute this code path");
    }
}
