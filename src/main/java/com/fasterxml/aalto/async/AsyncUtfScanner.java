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

import javax.xml.stream.XMLStreamException;


import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.in.*;
import com.fasterxml.aalto.util.DataUtil;
import com.fasterxml.aalto.util.XmlCharTypes;

/**
 * This class handles parsing of UTF-8 encoded XML streams, as well as
 * other UTF-8 compatible (subset) encodings (specifically, Latin1 and
 * US-ASCII).
 */
public class AsyncUtfScanner
    extends AsyncByteScanner
{
    private final static int EVENT_INCOMPLETE = AsyncXMLStreamReader.EVENT_INCOMPLETE;

    /*
    /**********************************************************************
    /* Local state constants only used in this class
    /**********************************************************************
     */
    
    // partially handled entities within attribute/ns values use pending state as well
    private final static int PENDING_STATE_ATTR_VALUE_AMP = -60;
    private final static int PENDING_STATE_ATTR_VALUE_AMP_HASH = -61;
   final static int PENDING_STATE_ATTR_VALUE_AMP_HASH_X = -62;
    private final static int PENDING_STATE_ATTR_VALUE_ENTITY_NAME = -63;
    private final static int PENDING_STATE_ATTR_VALUE_DEC_DIGIT = -64;
    private final static int PENDING_STATE_ATTR_VALUE_HEX_DIGIT = -65;

    private final static int PENDING_STATE_TEXT_AMP = -80; // seen &
    private final static int PENDING_STATE_TEXT_AMP_HASH = -81; // seen &#
    private final static int PENDING_STATE_TEXT_DEC_ENTITY = -82; // seen &# and 1 or more decimals
    private final static int PENDING_STATE_TEXT_HEX_ENTITY = -83; // seen &#x and 1 or more hex digits
    private final static int PENDING_STATE_TEXT_IN_ENTITY = -84; // seen & and part of entity name
    private final static int PENDING_STATE_TEXT_BRACKET1 = -85; // seen ]
    private final static int PENDING_STATE_TEXT_BRACKET2 = -86; // seen ]]

    /*
    /**********************************************************************
    /* Additional state
    /**********************************************************************
     */
    
    /**
     * Flag that indicates whether we are inside a declaration during parsing
     * of internal DTD subset.
     */
    protected boolean _inDtdDeclaration;
    
    /*
    /**********************************************************************
    /* Instance construction
    /**********************************************************************
     */

    public AsyncUtfScanner(ReaderConfig cfg)
    {
        super(cfg);
        _currToken = EVENT_INCOMPLETE;
    }

    /*
    /**********************************************************************
    /* Implementation of parsing API, character events
    /**********************************************************************
     */

    @Override
    protected final int startCharacters(byte b)
        throws XMLStreamException
    {
        dummy_loop:
        do { // dummy loop, to allow break
            int c = (int) b & 0xFF;
            switch (_charTypes.TEXT_CHARS[c]) {
            case XmlCharTypes.CT_INVALID:
                throwInvalidXmlChar(c);
            case XmlCharTypes.CT_WS_CR:
                /* Note: can not have pending input when this method
                 * is called. No need to check that (could assert)
                 */
                if (_inputPtr >= _inputEnd) { // no more input available
                    _pendingInput = PENDING_STATE_CR;
                    return EVENT_INCOMPLETE;
                }
                if (_inputBuffer[_inputPtr] == BYTE_LF) {
                    ++_inputPtr;
                }
                markLF();
                c = INT_LF;
                break;
            case XmlCharTypes.CT_WS_LF:
                markLF();
                break;
            case XmlCharTypes.CT_MULTIBYTE_2:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = c;
                    return EVENT_INCOMPLETE;
                }
                c = decodeUtf8_2(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_3:
                if ((_inputEnd - _inputPtr) < 2) {
                    if (_inputEnd > _inputPtr) { // 2 bytes available
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                    }
                    _pendingInput = c;
                    return EVENT_INCOMPLETE;
                }
                c = decodeUtf8_3(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_4:
                if ((_inputEnd - _inputPtr) < 3) {
                    if (_inputEnd > _inputPtr) { // at least 2 bytes?
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                        if (_inputEnd > _inputPtr) { // 3 bytes?
                            d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                            c |= (d << 16);
                        }
                    }
                    _pendingInput = c;
                    return EVENT_INCOMPLETE;
                }
                c = decodeUtf8_4(c);
                // Need a surrogate pair, have to call from here:
                _textBuilder.resetWithSurrogate(c);
                break dummy_loop;
                
            case XmlCharTypes.CT_MULTIBYTE_N:
                reportInvalidInitial(c);
                break;
            case XmlCharTypes.CT_LT: // should never get here
            case XmlCharTypes.CT_AMP: // - "" -
                throwInternal();
                break;
            case XmlCharTypes.CT_RBRACKET: // ']]>'?
                // !!! TBI: check for "]]>"

            default:
                break;
            }

            _textBuilder.resetWithChar((char) c);
        } while (false); // dummy loop, for break

        if (_cfgCoalescing && !_cfgLazyParsing) {
            // In eager coalescing mode, must read it all
            return finishCharactersCoalescing();
        }
        _currToken = CHARACTERS;
        if (_cfgLazyParsing) {
            _tokenIncomplete = true;
        } else {
            finishCharacters();
        }
        return _currToken;
    }

    protected int startCharactersPending()
        throws XMLStreamException
    {
        // First, need to have at least one more byte:
        if (_inputPtr >= _inputEnd) {
            return EVENT_INCOMPLETE;
        }

        // K. So what was the type again?
        int c = _pendingInput;
        _pendingInput = 0;

        // Possible \r\n linefeed?
        if (c == PENDING_STATE_CR) {
            if (_inputBuffer[_inputPtr] == BYTE_LF) {
                ++_inputPtr;
            }
            markLF();
            _textBuilder.resetWithChar(CHAR_LF);
        } else {
            // Nah, a multi-byte UTF-8 char:
            
            // Let's just retest the first pending byte (in LSB):
            switch (_charTypes.TEXT_CHARS[c & 0xFF]) {
            case XmlCharTypes.CT_MULTIBYTE_2:
                // Easy: must have just one byte, did get another one:
                _textBuilder.resetWithChar((char) decodeUtf8_2(c));
                break;
            case XmlCharTypes.CT_MULTIBYTE_3:
                {
                    // Ok... so do we have one or two pending bytes?
                    int next = _inputBuffer[_inputPtr++] & 0xFF;
                    int c2 = (c >> 8);
                    if (c2 == 0) { // just one; need two more
                        if (_inputPtr >= _inputEnd) { // but got only one
                            _pendingInput = c | (next << 8);
                            return EVENT_INCOMPLETE;
                        }
                        int c3 = _inputBuffer[_inputPtr++] & 0xFF;
                        c = decodeUtf8_3(c, next, c3);
                    } else { // had two, got one, bueno:
                        c = decodeUtf8_3((c & 0xFF), c2, next);
                    }
                    _textBuilder.resetWithChar((char) c);
                }
                break;
            case XmlCharTypes.CT_MULTIBYTE_4:
                {
                    int next = (int) _inputBuffer[_inputPtr++] & 0xFF;
                    // Only had one?
                    if ((c >> 8) == 0) { // ok, so need 3 more
                        if (_inputPtr >= _inputEnd) { // just have 1
                            _pendingInput = c | (next << 8);
                            return EVENT_INCOMPLETE;
                        }
                        int c2 = _inputBuffer[_inputPtr++] & 0xFF;
                        if (_inputPtr >= _inputEnd) { // almost, got 2
                            _pendingInput = c | (next << 8) | (c2 << 16);
                            return EVENT_INCOMPLETE;
                        }
                        int c3 = _inputBuffer[_inputPtr++] & 0xFF;
                        c = decodeUtf8_4(c, next, c2, c3);
                    } else { // had two or three
                        int c2 = (c >> 8) & 0xFF;
                        int c3 = (c >> 16);
                        
                        if (c3 == 0) { // just two
                            if (_inputPtr >= _inputEnd) { // one short
                                _pendingInput = c | (next << 16);
                                return EVENT_INCOMPLETE;
                            }
                            c3 = _inputBuffer[_inputPtr++] & 0xFF;
                            c = decodeUtf8_4((c & 0xFF), c2, next, c3);
                        } else { // had three, got last
                            c = decodeUtf8_4((c & 0xFF), c2, c3, next);
                        }
                    } 
                }
                // Need a surrogate pair, have to call from here:
                _textBuilder.resetWithSurrogate(c);
                return (_currToken = CHARACTERS);
            default: // should never occur:
                throwInternal();
            }
        }

        // Great, we got it. Is that enough?
        if (_cfgCoalescing && !_cfgLazyParsing) {
            // In eager coalescing mode, must read it all
            return finishCharactersCoalescing();
        }
        _currToken = CHARACTERS;
        if (_cfgLazyParsing) {
            _tokenIncomplete = true;
        } else {
            finishCharacters();
        }
        return _currToken;
    }

    /**
     * This method only gets called in non-coalescing mode; and if so,
     * needs to parse as many characters of the current text segment
     * from the current input block as possible.
     */
    protected final void finishCharacters()
        throws XMLStreamException
    {
        /* Now: there should not usually be any pending input (as it's
         * handled when CHARACTERS segment started, and this method
         * only gets called exactly once)... but we may want to
         * revisit this subject when (if) coalescing mode is to be
         * tackled.
         */
        if (_pendingInput != 0) {
            // !!! TBI: needs to be changed for coalescing mode
            throwInternal();
        }

        final int[] TYPES = _charTypes.TEXT_CHARS;
        final byte[] inputBuffer = _inputBuffer;
        char[] outputBuffer = _textBuilder.getBufferWithoutReset();
        // Should have just one code point (one or two chars). Assert?
        int outPtr = _textBuilder.getCurrentLength();

        main_loop:
        while (true) {
            int c;
            // Then the tight ASCII non-funny-char loop:
            ascii_loop:
            while (true) {
                int ptr = _inputPtr;
                if (ptr >= _inputEnd) {
                    break main_loop;
                }
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = _textBuilder.finishCurrentSegment();
                    outPtr = 0;
                }
                int max = _inputEnd;
                {
                    int max2 = ptr + (outputBuffer.length - outPtr);
                    if (max2 < max) {
                        max = max2;
                    }
                }
                while (ptr < max) {
                    c = (int) inputBuffer[ptr++] & 0xFF;
                    if (TYPES[c] != 0) {
                        _inputPtr = ptr;
                        break ascii_loop;
                    }
                    outputBuffer[outPtr++] = (char) c;
                }
                _inputPtr = ptr;
            }
            // And then fallback for funny chars / UTF-8 multibytes:
            switch (TYPES[c]) {
            case XmlCharTypes.CT_INVALID:
                throwInvalidXmlChar(c);
            case XmlCharTypes.CT_WS_CR:
                {
                    if (_inputPtr >= _inputEnd) {
                        _pendingInput = PENDING_STATE_CR;
                        break main_loop;
                    }
                    if (inputBuffer[_inputPtr] == BYTE_LF) {
                        ++_inputPtr;
                    }
                    markLF();
                }
                c = INT_LF;
                break;
            case XmlCharTypes.CT_WS_LF:
                markLF();
                break;
            case XmlCharTypes.CT_MULTIBYTE_2:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_2(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_3:
                if ((_inputEnd - _inputPtr) < 2) {
                    if (_inputEnd > _inputPtr) { // 2 bytes available
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_3(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_4:
                if ((_inputEnd - _inputPtr) < 3) {
                    if (_inputEnd > _inputPtr) { // at least 2 bytes?
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                        if (_inputEnd > _inputPtr) { // 3 bytes?
                            d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                            c |= (d << 16);
                        }
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_4(c);
                // Let's add first part right away:
                outputBuffer[outPtr++] = (char) (0xD800 | (c >> 10));
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = _textBuilder.finishCurrentSegment();
                    outPtr = 0;
                }
                c = 0xDC00 | (c & 0x3FF);
                // And let the other char output down below
                break;
            case XmlCharTypes.CT_MULTIBYTE_N:
                reportInvalidInitial(c);
            case XmlCharTypes.CT_LT:
                --_inputPtr;
                break main_loop;
            case XmlCharTypes.CT_AMP:
                c = handleEntityInCharacters();
                if (c == 0) { // not a succesfully expanded char entity
                    // _inputPtr set by entity expansion method
                    --_inputPtr;
                    break main_loop;
                }
                // Ok; does it need a surrogate though? (over 16 bits)
                if ((c >> 16) != 0) {
                    c -= 0x10000;
                    outputBuffer[outPtr++] = (char) (0xD800 | (c >> 10));
                    // Need to ensure room for one more char
                    if (outPtr >= outputBuffer.length) {
                        outputBuffer = _textBuilder.finishCurrentSegment();
                        outPtr = 0;
                    }
                    c = 0xDC00 | (c & 0x3FF);
                }
                break;
            case XmlCharTypes.CT_RBRACKET: // ']]>'?
                /* 09-Mar-2007, tatus: This will not give 100% coverage,
                 *  for it may be split across input buffer boundary.
                 *  For now this will have to suffice though.
                 */
                {
                    // Let's then just count number of brackets --
                    // in case they are not followed by '>'
                    int count = 1;
                    byte b = BYTE_NULL;
                    while (_inputPtr < _inputEnd) {
                        b = inputBuffer[_inputPtr];
                        if (b != BYTE_RBRACKET) {
                            break;
                        }
                        ++_inputPtr; // to skip past bracket
                        ++count;
                    }
                    if (b == BYTE_GT && count > 1) {
                        reportIllegalCDataEnd();
                    }
                    // Nope. Need to output all brackets, then; except
                    // for one that can be left for normal output
                    while (--count > 0) {
                        outputBuffer[outPtr++] = ']';
                        // Need to ensure room for one more char
                        if (outPtr >= outputBuffer.length) {
                            outputBuffer = _textBuilder.finishCurrentSegment();
                            outPtr = 0;
                        }
                    }
                }
                // Can just output the first ']' along normal output
                break;
                
            // default:
                // Other types are not important here...
            }
            // We know there's room for one more:
            outputBuffer[outPtr++] = (char) c;
        }
        _textBuilder.setCurrentLength(outPtr);
    }

    protected final int finishCharactersCoalescing()
        throws XMLStreamException
    {
        // First things first: any pending partial multi-bytes?
        if (_pendingInput != 0) {
            if (!handleAndAppendPending()) {
                return EVENT_INCOMPLETE;
            }
        }

        if (true) throw new UnsupportedOperationException();
        // !!! TBI
        
        return 0;
    }

    /**
     * Method called to handle entity encountered inside
     * CHARACTERS segment, when trying to complete a non-coalescing text segment.
     *<p>
     * NOTE: unlike with generic parsing of named entities, where trailing semicolon
     * needs to be left in place, here we should just process it right away.
     * 
     * @return Expanded (character) entity, if positive number; 0 if incomplete.
     */
    protected int handleEntityInCharacters() throws XMLStreamException
    {
        /* Thing that simplifies processing here is that handling
         * is pretty much optional: if there isn't enough data, we
         * just return 0 and are done with it.
         * 
         * Also: we need at least 3 more characters for any character entity
         */
        int ptr = _inputPtr;
        if ((ptr  + 3) <= _inputEnd) {
            byte b = _inputBuffer[ptr++];
            if (b == BYTE_HASH) { // numeric character entity
                if (_inputBuffer[ptr] == BYTE_x) {
                    return handleHexEntityInCharacters(ptr+1);
                }
                return handleDecEntityInCharacters(ptr);
            }
            // general entity; maybe one of pre-defined ones
            if (b == BYTE_a) { // amp or apos?
                b = _inputBuffer[ptr++];
                if (b == BYTE_m) {
                    if ((ptr + 1) < _inputPtr
                            && _inputBuffer[ptr] == BYTE_p
                            && _inputBuffer[ptr+1] == BYTE_SEMICOLON) {
                        _inputPtr = ptr + 2;
                        return INT_AMP;
                    }
                } else if (b == BYTE_p) {
                    if ((ptr + 2) < _inputPtr
                            && _inputBuffer[ptr] == BYTE_o
                            && _inputBuffer[ptr+1] == BYTE_s
                            && _inputBuffer[ptr+2] == BYTE_SEMICOLON) {
                        _inputPtr = ptr + 3;
                        return INT_APOS;
                    }
                }
            } else if (b == BYTE_g) { // gt?
                if (_inputBuffer[ptr] == BYTE_t
                        && _inputBuffer[ptr+1] == BYTE_SEMICOLON) {
                    _inputPtr = ptr + 2;
                    return INT_GT;
                }
            } else if (b == BYTE_l) { // lt?
                if (_inputBuffer[ptr] == BYTE_t
                        && _inputBuffer[ptr+1] == BYTE_SEMICOLON) {
                    _inputPtr = ptr + 2;
                    return INT_LT;
                }
            } else if (b == BYTE_q) { // quot?
                if ((ptr + 3) < _inputPtr
                        && _inputBuffer[ptr] == BYTE_u
                        && _inputBuffer[ptr+1] == BYTE_o
                        && _inputBuffer[ptr+2] == BYTE_t
                        && _inputBuffer[ptr+3] == BYTE_SEMICOLON) {
                    _inputPtr = ptr + 4;
                    return INT_APOS;
                }
            }
        }
        // couldn't handle:
        return 0;
    }

    protected int handleDecEntityInCharacters(int ptr) throws XMLStreamException
    {
        byte b = _inputBuffer[ptr++];
        final int end = _inputEnd;
        int value = 0;
        do {
            int ch = (int) b;
            if (ch > INT_9 || ch < INT_0) {
                throwUnexpectedChar(decodeCharForError(b), " expected a digit (0 - 9) for character entity");
            }
            value = (value * 10) + (ch - INT_0);
            if (value > MAX_UNICODE_CHAR) { // Overflow?
                reportEntityOverflow();
            }
            if (ptr >= end) {
                return 0;
            }
            b = _inputBuffer[ptr++];
        } while (b != BYTE_SEMICOLON);
        _inputPtr = ptr;
        verifyXmlChar(value);
        return value;
    }
    
    protected int handleHexEntityInCharacters(int ptr) throws XMLStreamException
    {
        byte b = _inputBuffer[ptr++];
        final int end = _inputEnd;
        int value = 0;
        do {
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
                reportEntityOverflow();
            }
            if (ptr >= end) {
                return 0;
            }
            b = _inputBuffer[ptr++];
        } while (b != BYTE_SEMICOLON);
        _inputPtr = ptr;
        verifyXmlChar(value);
        return value;
    }
    
    /**
     * Method called to handle split multi-byte character, by decoding
     * it and appending to the text buffer, if possible.
     *
     * @return True, if split character was completely handled; false
     *    if not
     */
    private final boolean handleAndAppendPending()
        throws XMLStreamException
    {
        // First, need to have at least one more byte:
        if (_inputPtr >= _inputEnd) {
            return false;
        }
        int c = _pendingInput;
        _pendingInput = 0;

        // Possible \r\n linefeed?
        if (c < 0) { // markers are all negative
            if (c == PENDING_STATE_CR) {
                if (_inputBuffer[_inputPtr] == BYTE_LF) {
                    ++_inputPtr;
                }
                markLF();
                _textBuilder.append(CHAR_LF);
                return true;
            }
            throwInternal();
        }
        
        // Nah, a multi-byte UTF-8 char:
        // Let's just re-test the first pending byte (in LSB):
        switch (_charTypes.TEXT_CHARS[c & 0xFF]) {
        case XmlCharTypes.CT_MULTIBYTE_2:
            // Easy: must have just one byte, did get another one:
            _textBuilder.append((char) decodeUtf8_2(c));
            break;

        case XmlCharTypes.CT_MULTIBYTE_3:
            {
                // Ok... so do we have one or two pending bytes?
                int next = _inputBuffer[_inputPtr++] & 0xFF;
                int c2 = (c >> 8);
                if (c2 == 0) { // just one; need two more
                    if (_inputPtr >= _inputEnd) { // but got only one
                        _pendingInput = c | (next << 8);
                        return false;
                    }
                    int c3 = _inputBuffer[_inputPtr++] & 0xFF;
                    c = decodeUtf8_3(c, next, c3);
                } else { // had two, got one, bueno:
                    c = decodeUtf8_3((c & 0xFF), c2, next);
                }
                _textBuilder.append((char) c);
            }
            break;
        case XmlCharTypes.CT_MULTIBYTE_4:
            {
                int next = (int) _inputBuffer[_inputPtr++] & 0xFF;
                // Only had one?
                if ((c >> 8) == 0) { // ok, so need 3 more
                    if (_inputPtr >= _inputEnd) { // just have 1
                        _pendingInput = c | (next << 8);
                        return false;
                    }
                    int c2 = _inputBuffer[_inputPtr++] & 0xFF;
                    if (_inputPtr >= _inputEnd) { // almost, got 2
                        _pendingInput = c | (next << 8) | (c2 << 16);
                        return false;
                    }
                    int c3 = _inputBuffer[_inputPtr++] & 0xFF;
                    c = decodeUtf8_4(c, next, c2, c3);
                } else { // had two or three
                    int c2 = (c >> 8) & 0xFF;
                    int c3 = (c >> 16);
                    
                    if (c3 == 0) { // just two
                        if (_inputPtr >= _inputEnd) { // one short
                            _pendingInput = c | (next << 16);
                            return false;
                        }
                        c3 = _inputBuffer[_inputPtr++] & 0xFF;
                        c = decodeUtf8_4((c & 0xFF), c2, next, c3);
                    } else { // had three, got last
                        c = decodeUtf8_4((c & 0xFF), c2, c3, next);
                    }
                } 
            }
            // Need a surrogate pair, have to call from here:
            _textBuilder.appendSurrogate(c);
            break;
        default: // should never occur:
            throwInternal();
        }
        return true;
    }

    /*
    /**********************************************************************
    /* Implementation of parsing API, skipping remainder CHARACTERS section
    /**********************************************************************
     */
    
    /**
     * Method that will be called to skip all possible characters
     * from the input buffer, but without blocking. Partial
     * characters are not to be handled (not pending input
     * is to be added).
     *
     * @return True, if skipping ending with an unexpanded
     *   entity; false if not
     */
    protected boolean skipCharacters()
        throws XMLStreamException
    {
        if (_pendingInput != 0) {
            if (!skipPending()) {
                return false;
            }
        }

        final int[] TYPES = _charTypes.TEXT_CHARS;
        final byte[] inputBuffer = _inputBuffer;

        main_loop:
        while (true) {
            int c;

            ascii_loop:
            while (true) {
                int ptr = _inputPtr;
                int max = _inputEnd;
                if (ptr >= max) {
                    break main_loop;
                }
                while (ptr < max) {
                    c = (int) inputBuffer[ptr++] & 0xFF;
                    if (TYPES[c] != 0) {
                        _inputPtr = ptr;
                        break ascii_loop;
                    }
                }
                _inputPtr = ptr;
            }
            // And then fallback for funny chars / UTF-8 multibytes:
            switch (TYPES[c]) {
            case XmlCharTypes.CT_INVALID:
                throwInvalidXmlChar(c);
            case XmlCharTypes.CT_WS_CR:
                {
                    if (_inputPtr >= _inputEnd) {
                        _pendingInput = PENDING_STATE_CR;
                        break main_loop;
                    }
                    if (inputBuffer[_inputPtr] == BYTE_LF) {
                        ++_inputPtr;
                    }
                    markLF();
                }
                break;
            case XmlCharTypes.CT_WS_LF:
                markLF();
                break;
            case XmlCharTypes.CT_MULTIBYTE_2:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = c;
                    break main_loop;
                }
                skipUtf8_2(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_3:
                if ((_inputEnd - _inputPtr) < 2) {
                    if (_inputEnd > _inputPtr) { // 2 bytes available
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                decodeUtf8_3(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_4:
                if ((_inputEnd - _inputPtr) < 3) {
                    if (_inputEnd > _inputPtr) { // at least 2 bytes?
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                        if (_inputEnd > _inputPtr) { // 3 bytes?
                            d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                            c |= (d << 16);
                        }
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                decodeUtf8_4(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_N:
                reportInvalidInitial(c);
            case XmlCharTypes.CT_LT:
                --_inputPtr;
                return true;
            case XmlCharTypes.CT_AMP:
                c = skipEntityInCharacters();
                if (c == 0) { // not a successfully expanded char entity
                    _pendingInput = PENDING_STATE_TEXT_AMP;
                    // but we may have input to skip nonetheless..
                    if (_inputPtr < _inputEnd) {
                        if (skipPending()) {
                            return true;
                        }
                    }
                    return false;
                }
                break;
            case XmlCharTypes.CT_RBRACKET: // ']]>'?
                /* !!! 09-Mar-2007, tatu: This will not give 100% coverage,
                 *  for it may be split across input buffer boundary.
                 *  For now this will have to suffice though.
                 */
                {
                    // Let's then just count number of brackets --
                    // in case they are not followed by '>'
                    int count = 1;
                    byte b = BYTE_NULL;
                    while (_inputPtr < _inputEnd) {
                        b = inputBuffer[_inputPtr];
                        if (b != BYTE_RBRACKET) {
                            break;
                        }
                        ++_inputPtr; // to skip past bracket
                        ++count;
                    }
                    if (b == BYTE_GT && count > 1) {
                        reportIllegalCDataEnd();
                    }
                }
                break;
                
            // default:
                // Other types are not important here...
            }
        }

        // Ran out of input, no entity encountered
        return false;
    }

    private final boolean skipPending() throws XMLStreamException
    {
        // First, need to have at least one more byte:
        if (_inputPtr >= _inputEnd) {
            return false;
        }
        
        // Possible \r\n linefeed?
        if (_pendingInput < 0) { // markers are all negative
            while (true) {
                switch (_pendingInput) {
                case PENDING_STATE_CR:
                    _pendingInput = 0;
                    if (_inputBuffer[_inputPtr] == BYTE_LF) {
                        ++_inputPtr;
                    }
                    markLF();
                    return true;
                case PENDING_STATE_TEXT_AMP:
                    {
                        byte b = _inputBuffer[_inputPtr++];
                        if (b == BYTE_HASH) {
                            _pendingInput = PENDING_STATE_TEXT_AMP_HASH;
                            break;
                        }
                        PName n = parseNewEntityName(b);
                        if (n == null) {
                            _pendingInput = PENDING_STATE_TEXT_IN_ENTITY;
                            return false;
                        }
                        int ch = decodeGeneralEntity(n);
                        if (ch == 0) {
                            _tokenName = n;
                            _nextEvent = ENTITY_REFERENCE;
                        }
                    }
                    _pendingInput = 0;
                    return true; // no matter what, we are done
                case PENDING_STATE_TEXT_AMP_HASH:
                    _entityValue = 0;
                    if (_inputBuffer[_inputPtr] == BYTE_x) {
                        ++_inputPtr;
                        if (decodeHexEntity()) {
                            _pendingInput = 0;
                            return true;
                        }
                        _pendingInput = PENDING_STATE_TEXT_HEX_ENTITY;
                        return false;
                    }
                    if (decodeDecEntity()) {
                        _pendingInput = 0;
                        return true;
                    }
                    _pendingInput = PENDING_STATE_TEXT_DEC_ENTITY;
                    return false;

                case PENDING_STATE_TEXT_DEC_ENTITY:
                    if (decodeDecEntity()) {
                        _pendingInput = 0;
                        return true;
                    }
                    return false;
                    
                case PENDING_STATE_TEXT_HEX_ENTITY:
                    if (decodeHexEntity()) {
                        _pendingInput = 0;
                        return true;
                    }
                    return false;

                case PENDING_STATE_TEXT_IN_ENTITY:
                    {
                        PName n = parseEntityName();
                        if (n == null) {
                            return false;
                        }
                        int ch = decodeGeneralEntity(n);
                        if (ch == 0) {
                            _tokenName = n;
                            _nextEvent = ENTITY_REFERENCE;
                        }
                    }
                    _pendingInput = 0;
                    return true;

                case PENDING_STATE_TEXT_BRACKET1:
                    if (_inputBuffer[_inputPtr] != BYTE_RBRACKET) {
                        _pendingInput = 0;
                        return true;
                    }
                    ++_inputPtr;
                    _pendingInput = PENDING_STATE_TEXT_BRACKET2;
                    break;

                case PENDING_STATE_TEXT_BRACKET2:
                    // may get sequence...
                    {
                        byte b = _inputBuffer[_inputPtr];
                        if (b == BYTE_RBRACKET) {
                            ++_inputPtr;
                            break;
                        }
                        if (b == BYTE_GT) { // problem!
                            ++_inputPtr;
                            reportInputProblem("Encountered ']]>' in text segment");
                        }
                    }
                    // nope, something else, reprocess
                    _pendingInput = 0;
                    return true;
                default:
                    throwInternal();
                }

                if (_inputPtr >= _inputEnd) {
                    return false;
                }
            }
        }
    
        // Nah, a multi-byte UTF-8 char:
        // Let's just re-test the first pending byte (in LSB):
        int c = _pendingInput;
        switch (_charTypes.TEXT_CHARS[c & 0xFF]) {
        case XmlCharTypes.CT_MULTIBYTE_2:
            // Easy: must have just one byte, did get another one:
            skipUtf8_2(c);
            break;
    
        case XmlCharTypes.CT_MULTIBYTE_3:
            {
                // Ok... so do we have one or two pending bytes?
                int next = _inputBuffer[_inputPtr++] & 0xFF;
                int c2 = (c >> 8);
                if (c2 == 0) { // just one; need two more
                    if (_inputPtr >= _inputEnd) { // but got only one
                        _pendingInput = c | (next << 8);
                        return false;
                    }
                    int c3 = _inputBuffer[_inputPtr++] & 0xFF;
                    decodeUtf8_3(c, next, c3);
                } else { // had two, got one, bueno:
                    decodeUtf8_3((c & 0xFF), c2, next);
                }
            }
            break;
        case XmlCharTypes.CT_MULTIBYTE_4:
            {
                int next = (int) _inputBuffer[_inputPtr++] & 0xFF;
                // Only had one?
                if ((c >> 8) == 0) { // ok, so need 3 more
                    if (_inputPtr >= _inputEnd) { // just have 1
                        _pendingInput = c | (next << 8);
                        return false;
                    }
                    int c2 = _inputBuffer[_inputPtr++] & 0xFF;
                    if (_inputPtr >= _inputEnd) { // almost, got 2
                        _pendingInput = c | (next << 8) | (c2 << 16);
                        return false;
                    }
                    int c3 = _inputBuffer[_inputPtr++] & 0xFF;
                    decodeUtf8_4(c, next, c2, c3);
                } else { // had two or three
                    int c2 = (c >> 8) & 0xFF;
                    int c3 = (c >> 16);
                    
                    if (c3 == 0) { // just two
                        if (_inputPtr >= _inputEnd) { // one short
                            _pendingInput = c | (next << 16);
                            return false;
                        }
                        c3 = _inputBuffer[_inputPtr++] & 0xFF;
                        decodeUtf8_4((c & 0xFF), c2, next, c3);
                    } else { // had three, got last
                        decodeUtf8_4((c & 0xFF), c2, c3, next);
                    }
                } 
            }
            break;
        default: // should never occur:
            throwInternal();
        }
        _pendingInput = 0;
        return true;
    }

    /**
     * Method called to handle entity encountered inside
     * CHARACTERS segment, when trying to complete a non-coalescing text segment.
     * 
     * @return Expanded (character) entity, if positive number; 0 if incomplete.
     */
    private int skipEntityInCharacters() throws XMLStreamException
    {
        /* Thing that simplifies processing here is that handling
         * is pretty much optional: if there isn't enough data, we
         * just return 0 and are done with it.
         * 
         * Also: we need at least 3 more characters for any character entity
         */
        int ptr = _inputPtr;
        if ((ptr  + 3) <= _inputEnd) {
            byte b = _inputBuffer[ptr++];
            if (b == BYTE_HASH) { // numeric character entity
                if (_inputBuffer[ptr] == BYTE_x) {
                    return handleHexEntityInCharacters(ptr+1);
                }
                return handleDecEntityInCharacters(ptr);
            }
            // general entity; maybe one of pre-defined ones
            if (b == BYTE_a) { // amp or apos?
                b = _inputBuffer[ptr++];
                if (b == BYTE_m) {
                    if ((ptr + 1) < _inputPtr
                            && _inputBuffer[ptr] == BYTE_p
                            && _inputBuffer[ptr+1] == BYTE_SEMICOLON) {
                        _inputPtr = ptr + 2; // NOTE: do skip semicolon as well
                        return INT_AMP;
                    }
                } else if (b == BYTE_p) {
                    if ((ptr + 2) < _inputPtr
                            && _inputBuffer[ptr] == BYTE_o
                            && _inputBuffer[ptr+1] == BYTE_s
                            && _inputBuffer[ptr+2] == BYTE_SEMICOLON) {
                        _inputPtr = ptr + 3;
                        return INT_APOS;
                    }
                }
            } else if (b == BYTE_g) { // gt?
                if (_inputBuffer[ptr] == BYTE_t
                        && _inputBuffer[ptr+1] == BYTE_SEMICOLON) {
                    _inputPtr = ptr + 2;
                    return INT_GT;
                }
            } else if (b == BYTE_l) { // lt?
                if (_inputBuffer[ptr] == BYTE_t
                        && _inputBuffer[ptr+1] == BYTE_SEMICOLON) {
                    _inputPtr = ptr + 2;
                    return INT_LT;
                }
            } else if (b == BYTE_q) { // quot?
                if ((ptr + 3) < _inputPtr
                        && _inputBuffer[ptr] == BYTE_u
                        && _inputBuffer[ptr+1] == BYTE_o
                        && _inputBuffer[ptr+2] == BYTE_t
                        && _inputBuffer[ptr+3] == BYTE_SEMICOLON) {
                    _inputPtr = ptr + 4;
                    return INT_APOS;
                }
            }
        }
        // couldn't handle:
        return 0;
    }
    
    /**
     * Coalescing mode is (and will) not be implemented for non-blocking
     * parsers, so this method should never get called.
     */
    protected boolean skipCoalescedText()
        throws XMLStreamException
    {
        throwInternal();
        return false;
    }

    /*
    /**********************************************************************
    /* Implementation of parsing API, element/attr events
    /**********************************************************************
     */

    /**
     * @return True, if the whole value was read; false if
     *   only part (due to buffer ending)
     */
    protected boolean handleAttrValue()
        throws XMLStreamException
    {
        // First; any pending input?
        if (_pendingInput != 0) {
            if (!handleAttrValuePending()) {
                return false;
            }
            _pendingInput = 0;
        }

        char[] attrBuffer = _attrCollector.continueValue();
        final int[] TYPES = _charTypes.ATTR_CHARS;
        final int quoteChar = (int) _elemAttrQuote;
        
        value_loop:
        while (true) {
            int c;

            ascii_loop:
            while (true) {
                if (_inputPtr >= _inputEnd) {
                    return false;
                }
                if (_elemAttrPtr >= attrBuffer.length) {
                    attrBuffer = _attrCollector.valueBufferFull();
                }
                int max = _inputEnd;
                {
                    int max2 = _inputPtr + (attrBuffer.length - _elemAttrPtr);
                    if (max2 < max) {
                        max = max2;
                    }
                }
                while (_inputPtr < max) {
                    c = (int) _inputBuffer[_inputPtr++] & 0xFF;
                    if (TYPES[c] != 0) {
                        break ascii_loop;
                    }
                    attrBuffer[_elemAttrPtr++] = (char) c;
                }
            }
            
            switch (TYPES[c]) {
            case XmlCharTypes.CT_INVALID:
                throwInvalidXmlChar(c);
            case XmlCharTypes.CT_WS_CR:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = PENDING_STATE_CR;
                    return false;
                }
                if (_inputBuffer[_inputPtr] == BYTE_LF) {
                    ++_inputPtr;
                }
                // fall through
            case XmlCharTypes.CT_WS_LF:
                markLF();
                // fall through
            case XmlCharTypes.CT_WS_TAB:
                // Plus, need to convert these all to simple space
                c = INT_SPACE;
                break;
            case XmlCharTypes.CT_MULTIBYTE_2:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = c;
                    return false;
                }
                c = decodeUtf8_2(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_3:
                if ((_inputEnd - _inputPtr) < 2) {
                    if (_inputEnd > _inputPtr) { // 2 bytes available
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                    }
                    _pendingInput = c;
                    return false;
                }
                c = decodeUtf8_3(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_4:
                if ((_inputEnd - _inputPtr) < 3) {
                    if (_inputEnd > _inputPtr) { // at least 2 bytes?
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                        if (_inputEnd > _inputPtr) { // 3 bytes?
                            d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                            c |= (d << 16);
                        }
                    }
                    _pendingInput = c;
                    return false;
                }
                c = decodeUtf8_4(c);
                // Let's add first part right away:
                attrBuffer[_elemAttrPtr++] = (char) (0xD800 | (c >> 10));
                c = 0xDC00 | (c & 0x3FF);
                if (_elemAttrPtr >= attrBuffer.length) {
                    attrBuffer = _attrCollector.valueBufferFull();
                }
                break;
            case XmlCharTypes.CT_MULTIBYTE_N:
                reportInvalidInitial(c);
            case XmlCharTypes.CT_LT:
                throwUnexpectedChar(c, "'<' not allowed in attribute value");
            case XmlCharTypes.CT_AMP:
                c = handleEntityInAttributeValue();
                if (c <= 0) { // general entity; should never happen
                    if (c < 0) { // end-of-input
                        return false;
                    }
                    reportUnexpandedEntityInAttr(_elemAttrName, false);
                }
                // Ok; does it need a surrogate though? (over 16 bits)
                if ((c >> 16) != 0) {
                    c -= 0x10000;
                    attrBuffer[_elemAttrPtr++] = (char) (0xD800 | (c >> 10));
                    c = 0xDC00 | (c & 0x3FF);
                    if (_elemAttrPtr >= attrBuffer.length) {
                        attrBuffer = _attrCollector.valueBufferFull();
                    }
                }
                break;
            case XmlCharTypes.CT_ATTR_QUOTE:
                if (c == quoteChar) {
                    break value_loop;
                }
                
                // default:
                // Other chars are not important here...
            }
            // We know there's room for at least one char without checking
            attrBuffer[_elemAttrPtr++] = (char) c;
        }

        return true; // yeah, we're done!
    }

    /**
     * @return True if the partial information was succesfully handled;
     *    false if not
     */
    private final boolean handleAttrValuePending() throws XMLStreamException
    {
        if (_pendingInput == PENDING_STATE_CR) {
            if (!handlePartialCR()) {
                return false;
            }
            char[] attrBuffer = _attrCollector.continueValue();
            if (_elemAttrPtr >= attrBuffer.length) {
                attrBuffer = _attrCollector.valueBufferFull();
            }
            // All LFs get converted to spaces, in attribute values
            attrBuffer[_elemAttrPtr++] = ' ';
            return true;
        }
        // otherwise must be related to entity handling within attribute value
        if (_inputPtr >= _inputEnd) {
            return false;
        }
        int ch;

        if (_pendingInput == PENDING_STATE_ATTR_VALUE_AMP) {
            byte b = _inputBuffer[_inputPtr++];
            if (b == BYTE_HASH) { // numeric character entity
                _pendingInput = PENDING_STATE_ATTR_VALUE_AMP_HASH;
                if (_inputPtr >= _inputEnd) {
                    return false;
                }
                if (_inputBuffer[_inputPtr] == BYTE_x) {
                    _pendingInput = PENDING_STATE_ATTR_VALUE_AMP_HASH_X;
                    ++_inputPtr;
                    if (_inputPtr >= _inputEnd) {
                        return false;
                    }
                    ch = handleHexEntityInAttribute(true);
                } else {
                    ch = handleDecEntityInAttribute(true);
                }
            } else {
                PName entityName = parseNewEntityName(b);
                if (entityName == null) {
                    _pendingInput = PENDING_STATE_ATTR_VALUE_ENTITY_NAME;
                    return false;
                }   
                ch = decodeGeneralEntity(entityName);
                if (ch == 0) { // can't have general entities within attribute values
                    _tokenName = entityName;
                    reportUnexpandedEntityInAttr(_elemAttrName, false);
                }
            }
        } else if (_pendingInput == PENDING_STATE_ATTR_VALUE_AMP_HASH) {
            if (_inputBuffer[_inputPtr] == BYTE_x) {
                _pendingInput = PENDING_STATE_ATTR_VALUE_AMP_HASH_X;
                ++_inputPtr;
                if (_inputPtr >= _inputEnd) {
                    return false;
                }
                ch = handleHexEntityInAttribute(true);
            } else {
                ch = handleDecEntityInAttribute(true);
            }
        } else if (_pendingInput == PENDING_STATE_ATTR_VALUE_AMP_HASH_X) {
            ch = handleHexEntityInAttribute(true);
        } else if (_pendingInput == PENDING_STATE_ATTR_VALUE_ENTITY_NAME) {
            PName entityName = parseEntityName();
            if (entityName == null) {
                return false;
            }   
            ch = decodeGeneralEntity(entityName);
            if (ch == 0) { // can't have general entities within attribute values
                _tokenName = entityName;
                reportUnexpandedEntityInAttr(_elemAttrName, false);
            }
        } else if (_pendingInput == PENDING_STATE_ATTR_VALUE_DEC_DIGIT) {
            ch = handleDecEntityInAttribute(false);
        } else if (_pendingInput == PENDING_STATE_ATTR_VALUE_HEX_DIGIT) {
            ch = handleHexEntityInAttribute(false);
        } else { // nope, split UTF-8 char
            // Nah, a multi-byte UTF-8 char. Alas, can't use shared method, as results
            // don't go in shared text buffer...
            ch = handleAttrValuePendingUTF8();
        }
        if (ch == 0) { // wasn't resolved
            return false;
        }

        char[] attrBuffer = _attrCollector.continueValue();
        // Ok; does it need a surrogate though? (over 16 bits)
        if ((ch >> 16) != 0) {
            ch -= 0x10000;
            if (_elemAttrPtr >= attrBuffer.length) {
                attrBuffer = _attrCollector.valueBufferFull();
            }
            attrBuffer[_elemAttrPtr++] = (char) (0xD800 | (ch >> 10));
            ch = 0xDC00 | (ch & 0x3FF);
        }
        if (_elemAttrPtr >= attrBuffer.length) {
            attrBuffer = _attrCollector.valueBufferFull();
        }
        attrBuffer[_elemAttrPtr++] = (char) ch;
        return true; // done it!
    }

    private final int handleAttrValuePendingUTF8() throws XMLStreamException
    {
        // note: we know there must be at least one byte available at this point
        int c = _pendingInput;
        _pendingInput = 0;
        
        // Let's just re-test the first pending byte (in LSB):
        switch (_charTypes.TEXT_CHARS[c & 0xFF]) {
        case XmlCharTypes.CT_MULTIBYTE_2:
            // Easy: must have just one byte, did get another one:
            return decodeUtf8_2(c);
        case XmlCharTypes.CT_MULTIBYTE_3:
            {
                // Ok... so do we have one or two pending bytes?
                int next = _inputBuffer[_inputPtr++] & 0xFF;
                int c2 = (c >> 8);
                if (c2 == 0) { // just one; need two more
                    if (_inputPtr >= _inputEnd) { // but got only one
                        _pendingInput = c | (next << 8);
                        return 0;
                    }
                    int c3 = _inputBuffer[_inputPtr++] & 0xFF;
                    c = decodeUtf8_3(c, next, c3);
                } else { // had two, got one, bueno:
                    c = decodeUtf8_3((c & 0xFF), c2, next);
                }
                return c;
            }
        case XmlCharTypes.CT_MULTIBYTE_4:
            {
                int next = (int) _inputBuffer[_inputPtr++] & 0xFF;
                // Only had one?
                if ((c >> 8) == 0) { // ok, so need 3 more
                    if (_inputPtr >= _inputEnd) { // just have 1
                        _pendingInput = c | (next << 8);
                        return 0;
                    }
                    int c2 = _inputBuffer[_inputPtr++] & 0xFF;
                    if (_inputPtr >= _inputEnd) { // almost, got 2
                        _pendingInput = c | (next << 8) | (c2 << 16);
                        return 0;
                    }
                    int c3 = _inputBuffer[_inputPtr++] & 0xFF;
                    c = decodeUtf8_4(c, next, c2, c3);
                } else { // had two or three
                    int c2 = (c >> 8) & 0xFF;
                    int c3 = (c >> 16);
                    
                    if (c3 == 0) { // just two
                        if (_inputPtr >= _inputEnd) { // one short
                            _pendingInput = c | (next << 16);
                            return 0;
                        }
                        c3 = _inputBuffer[_inputPtr++] & 0xFF;
                        c = decodeUtf8_4((c & 0xFF), c2, next, c3);
                    } else { // had three, got last
                        c = decodeUtf8_4((c & 0xFF), c2, c3, next);
                    }
                } 
                return c;
            }
        default: // should never occur:
            throwInternal();
            return 0; // never gets here
        }
    }    
    
    private final int handleDecEntityInAttribute(boolean starting)
        throws XMLStreamException
    {
        byte b = _inputBuffer[_inputPtr++]; // we know one is available
        if (starting) {
            int ch = (int) b;
            if (ch < INT_0 || ch > INT_9) { // invalid entity
                throwUnexpectedChar(decodeCharForError(b), " expected a digit (0 - 9) for character entity");
            }
            _pendingInput = PENDING_STATE_ATTR_VALUE_DEC_DIGIT;
            _entityValue = ch - INT_0;
            if (_inputPtr >= _inputEnd) {
                return 0;
            }
            b = _inputBuffer[_inputPtr++];
        }
        while (b != BYTE_SEMICOLON) {
            int ch = ((int) b) - INT_0;
            if (ch < 0 || ch > 9) { // invalid entity
                throwUnexpectedChar(decodeCharForError(b), " expected a digit (0 - 9) for character entity");
            }
            int value = (_entityValue * 10) + ch;
            _entityValue = value;
            if (value > MAX_UNICODE_CHAR) { // Overflow?
                reportEntityOverflow();
            }
            if (_inputPtr >= _inputEnd) {
                return 0;
            }
            b = _inputBuffer[_inputPtr++];
        }
        verifyXmlChar(_entityValue);
        _pendingInput = 0;
        return _entityValue;
    }

    private final int handleHexEntityInAttribute(boolean starting)
        throws XMLStreamException
    {
        byte b = _inputBuffer[_inputPtr++]; // we know one is available
        if (starting) {
            int ch = (int) b;
            if (ch < INT_0 || ch > INT_9) { // invalid entity
                throwUnexpectedChar(decodeCharForError(b), " expected a hex digit (0-9a-fA-F) for character entity");
            }
            _pendingInput = PENDING_STATE_ATTR_VALUE_HEX_DIGIT;
            _entityValue = ch - INT_0;
            if (_inputPtr >= _inputEnd) {
                return 0;
            }
            b = _inputBuffer[_inputPtr++];
        }
        while (b != BYTE_SEMICOLON) {
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
            int value = (_entityValue << 4) + ch;
            _entityValue = value;
            if (value > MAX_UNICODE_CHAR) { // Overflow?
                reportEntityOverflow();
            }
            if (_inputPtr >= _inputEnd) {
                return 0;
            }
            b = _inputBuffer[_inputPtr++];
        }
        verifyXmlChar(_entityValue);
        _pendingInput = 0;
        return _entityValue;
    }

    /**
     * Method called to handle entity encountered inside attribute value.
     * 
     * @return Value of expanded character entity, if processed (which must be
     *    1 or above); 0 for general entity, or -1 for "not enough input"
     */
    protected int handleEntityInAttributeValue()
        throws XMLStreamException
    {
        if (_inputPtr >= _inputEnd) {
            _pendingInput = PENDING_STATE_ATTR_VALUE_AMP;
            return -1;
        }
        byte b = _inputBuffer[_inputPtr++];
        if (b == BYTE_HASH) { // numeric character entity
            _pendingInput = PENDING_STATE_ATTR_VALUE_AMP_HASH;
            if (_inputPtr >= _inputEnd) {
                return -1;
            }
            int ch;
            if (_inputBuffer[_inputPtr] == BYTE_x) {
                _pendingInput = PENDING_STATE_ATTR_VALUE_AMP_HASH_X;
                ++_inputPtr;
                if (_inputPtr >= _inputEnd) {
                    return -1;
                }
                ch = handleHexEntityInAttribute(true);
            } else {
                ch = handleDecEntityInAttribute(true);
            }
            if (ch == 0) {
                return -1;
            }
            return ch;
        }
        PName entityName = parseNewEntityName(b);
        if (entityName == null) {
            _pendingInput = PENDING_STATE_ATTR_VALUE_ENTITY_NAME;
            return -1;
        }
        int ch = decodeGeneralEntity(entityName);
        if (ch != 0) {
            return ch;
        }
        _tokenName = entityName;
        return 0;
    }
    
    protected boolean handleNsDecl()
        throws XMLStreamException
    {
        final int[] TYPES = _charTypes.ATTR_CHARS;
        char[] attrBuffer = _nameBuffer;
        final int quoteChar = (int) _elemAttrQuote;

        // First; any pending input?
        if (_pendingInput != 0) {
            if (!handleNsValuePending()) {
                return false;
            }
            _pendingInput = 0;
        }
        
        value_loop:
        while (true) {
            int c;

            ascii_loop:
            while (true) {
                if (_inputPtr >= _inputEnd) {
                    return false;
                }
                if (_elemNsPtr >= attrBuffer.length) {
                    _nameBuffer = attrBuffer = DataUtil.growArrayBy(attrBuffer, attrBuffer.length);
                }
                int max = _inputEnd;
                {
                    int max2 = _inputPtr + (attrBuffer.length - _elemNsPtr);
                    if (max2 < max) {
                        max = max2;
                    }
                }
                while (_inputPtr < max) {
                    c = (int) _inputBuffer[_inputPtr++] & 0xFF;
                    if (TYPES[c] != 0) {
                        break ascii_loop;
                    }
                    attrBuffer[_elemNsPtr++] = (char) c;
                }
            }
            
            switch (TYPES[c]) {
            case XmlCharTypes.CT_INVALID:
                throwInvalidXmlChar(c);
            case XmlCharTypes.CT_WS_CR:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = PENDING_STATE_CR;
                    return false;
                }
                if (_inputBuffer[_inputPtr] == BYTE_LF) {
                    ++_inputPtr;
                }
                // fall through
            case XmlCharTypes.CT_WS_LF:
                markLF();
                // fall through
            case XmlCharTypes.CT_WS_TAB:
                // Plus, need to convert these all to simple space
                c = INT_SPACE;
                break;
            case XmlCharTypes.CT_MULTIBYTE_2:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = c;
                    return false;
                }
                c = decodeUtf8_2(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_3:
                if ((_inputEnd - _inputPtr) < 2) {
                    if (_inputEnd > _inputPtr) { // 2 bytes available
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                    }
                    _pendingInput = c;
                    return false;
                }
                c = decodeUtf8_3(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_4:
                if ((_inputEnd - _inputPtr) < 3) {
                    if (_inputEnd > _inputPtr) { // at least 2 bytes?
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                        if (_inputEnd > _inputPtr) { // 3 bytes?
                            d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                            c |= (d << 16);
                        }
                    }
                    _pendingInput = c;
                    return false;
                }
                c = decodeUtf8_4(c);
                // Let's add first part right away:
                attrBuffer[_elemNsPtr++] = (char) (0xD800 | (c >> 10));
                c = 0xDC00 | (c & 0x3FF);
                if (_elemNsPtr >= attrBuffer.length) {
                    _nameBuffer = attrBuffer = DataUtil.growArrayBy(attrBuffer, attrBuffer.length);
                }
                break;
            case XmlCharTypes.CT_MULTIBYTE_N:
                reportInvalidInitial(c);
            case XmlCharTypes.CT_LT:
                throwUnexpectedChar(c, "'<' not allowed in attribute value");
            case XmlCharTypes.CT_AMP:
                c = handleEntityInAttributeValue();
                if (c <= 0) { // general entity; should never happen
                    if (c < 0) { // end-of-input
                        return false;
                    }
                    reportUnexpandedEntityInAttr(_elemAttrName, true);
                }
                // Ok; does it need a surrogate though? (over 16 bits)
                if ((c >> 16) != 0) {
                    c -= 0x10000;
                    attrBuffer[_elemNsPtr++] = (char) (0xD800 | (c >> 10));
                    c = 0xDC00 | (c & 0x3FF);
                    if (_elemNsPtr >= attrBuffer.length) {
                        _nameBuffer = attrBuffer = DataUtil.growArrayBy(attrBuffer, attrBuffer.length);
                    }
                }
                break;
            case XmlCharTypes.CT_ATTR_QUOTE:
                if (c == quoteChar) {
                    break value_loop;
                }
                
                // default:
                // Other chars are not important here...
            }
            // We know there's room for at least one char without checking
            attrBuffer[_elemNsPtr++] = (char) c;
        }

        /* Simple optimization: for default ns removal (or, with
         * ns 1.1, any other as well), will use empty value... no
         * need to try to intern:
         */
        int attrPtr = _elemNsPtr;
        if (attrPtr == 0) {
            bindNs(_elemAttrName, "");
        } else {
            String uri = _config.canonicalizeURI(attrBuffer, attrPtr);
            bindNs(_elemAttrName, uri);
        }
        return true;
    }

    /**
     * @return True if the partial information was succesfully handled;
     *    false if not
     */
    private final boolean handleNsValuePending() throws XMLStreamException
    {
        if (_pendingInput == PENDING_STATE_CR) {
            if (!handlePartialCR()) {
                return false;
            }
            char[] attrBuffer = _nameBuffer;
            if (_elemNsPtr >= attrBuffer.length) {
                _nameBuffer = attrBuffer = DataUtil.growArrayBy(attrBuffer, attrBuffer.length);
            }
            // All lfs get converted to spaces, in attribute values
            attrBuffer[_elemNsPtr++] = ' ';
            return true;
        }

        // otherwise must be related to entity handling within attribute value
        if (_inputPtr >= _inputEnd) {
            return false;
        }

        int ch;

        if (_pendingInput == PENDING_STATE_ATTR_VALUE_AMP) {
            byte b = _inputBuffer[_inputPtr++];
            if (b == BYTE_HASH) { // numeric character entity
                _pendingInput = PENDING_STATE_ATTR_VALUE_AMP_HASH;
                if (_inputPtr >= _inputEnd) {
                    return false;
                }
                if (_inputBuffer[_inputPtr] == BYTE_x) {
                    _pendingInput = PENDING_STATE_ATTR_VALUE_AMP_HASH_X;
                    ++_inputPtr;
                    if (_inputPtr >= _inputEnd) {
                        return false;
                    }
                    ch = handleHexEntityInAttribute(true);
                } else {
                    ch = handleDecEntityInAttribute(true);
                }
            } else {
                PName entityName = parseNewEntityName(b);
                if (entityName == null) {
                    _pendingInput = PENDING_STATE_ATTR_VALUE_ENTITY_NAME;
                    return false;
                }   
                ch = decodeGeneralEntity(entityName);
                if (ch == 0) { // can't have general entities within attribute values
                    _tokenName = entityName;
                    reportUnexpandedEntityInAttr(_elemAttrName, false);
                }
            }
        } else if (_pendingInput == PENDING_STATE_ATTR_VALUE_AMP_HASH) {
            if (_inputBuffer[_inputPtr] == BYTE_x) {
                _pendingInput = PENDING_STATE_ATTR_VALUE_AMP_HASH_X;
                ++_inputPtr;
                if (_inputPtr >= _inputEnd) {
                    return false;
                }
                ch = handleHexEntityInAttribute(true);
            } else {
                ch = handleDecEntityInAttribute(true);
            }
        } else if (_pendingInput == PENDING_STATE_ATTR_VALUE_AMP_HASH_X) {
            ch = handleHexEntityInAttribute(true);
        } else if (_pendingInput == PENDING_STATE_ATTR_VALUE_ENTITY_NAME) {
            PName entityName = parseEntityName();
            if (entityName == null) {
                return false;
            }   
            ch = decodeGeneralEntity(entityName);
            if (ch == 0) { // can't have general entities within attribute values
                _tokenName = entityName;
                reportUnexpandedEntityInAttr(_elemAttrName, false);
            }
        } else if (_pendingInput == PENDING_STATE_ATTR_VALUE_DEC_DIGIT) {
            ch = handleDecEntityInAttribute(false);
        } else if (_pendingInput == PENDING_STATE_ATTR_VALUE_HEX_DIGIT) {
            ch = handleHexEntityInAttribute(false);
        } else {
	    // 05-Aug-2012, tatu: Apparently we can end up here too...
	    ch = handleAttrValuePendingUTF8();
        }
        if (ch == 0) { // wasn't resolved
            return false;
        }
        
        char[] attrBuffer = _nameBuffer; 
        // Ok; does it need a surrogate though? (over 16 bits)
        if ((ch >> 16) != 0) {
            ch -= 0x10000;
            if (_elemNsPtr >= attrBuffer.length) {
                _nameBuffer = attrBuffer = DataUtil.growArrayBy(attrBuffer, attrBuffer.length);
            }
            attrBuffer[_elemNsPtr++] = (char) (0xD800 | (ch >> 10));
            ch = 0xDC00 | (ch & 0x3FF);
        }
        if (_elemNsPtr >= attrBuffer.length) {
            _nameBuffer = attrBuffer = DataUtil.growArrayBy(attrBuffer, attrBuffer.length);
        }
        attrBuffer[_elemNsPtr++] = (char) ch;
        return true; // done it!
    }
    
    /*
    /**********************************************************************
    /* Implementation of parsing API, other events
    /**********************************************************************
     */
    
    protected final boolean handleDTDInternalSubset(boolean init) throws XMLStreamException
    {
        char[] outputBuffer;
        int outPtr;

        if (init) { // first time around
            outputBuffer = _textBuilder.resetWithEmpty();
            outPtr = 0;
            _elemAttrQuote = 0;
            _inDtdDeclaration = false;
        } else {
            if (_pendingInput != 0) {
                if (!handleAndAppendPending()) {
                    return false;
                }
            }        
            outputBuffer = _textBuilder.getBufferWithoutReset();
            outPtr = _textBuilder.getCurrentLength();
        }

        final int[] TYPES = _charTypes.DTD_CHARS;
        final byte[] inputBuffer = _inputBuffer;
        
        main_loop:
        while (true) {
            int c;
            // Then the tight ASCII non-funny-char loop:
            ascii_loop:
            while (true) {
                if (_inputPtr >= _inputEnd) {
                    break main_loop;
                }
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = _textBuilder.finishCurrentSegment();
                    outPtr = 0;
                }
                int max = _inputEnd;
                {
                    int max2 = _inputPtr + (outputBuffer.length - outPtr);
                    if (max2 < max) {
                        max = max2;
                    }
                }
                while (_inputPtr < max) {
                    c = (int) inputBuffer[_inputPtr++] & 0xFF;
                    if (TYPES[c] != 0) {
                        break ascii_loop;
                    }
                    outputBuffer[outPtr++] = (char) c;
                }
            }

            switch (TYPES[c]) {
            case XmlCharTypes.CT_INVALID:
                throwInvalidXmlChar(c);
            case XmlCharTypes.CT_WS_CR:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = PENDING_STATE_CR;
                    break main_loop;
                }
                if (inputBuffer[_inputPtr] == BYTE_LF) {
                    ++_inputPtr;
                }
                markLF();
                c = INT_LF;
                break;
            case XmlCharTypes.CT_WS_LF:
                markLF();
                break;
            case XmlCharTypes.CT_MULTIBYTE_2:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_2(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_3:
                if ((_inputEnd - _inputPtr) < 2) {
                    if (_inputEnd > _inputPtr) { // 2 bytes available
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_3(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_4:
                if ((_inputEnd - _inputPtr) < 3) {
                    if (_inputEnd > _inputPtr) { // at least 2 bytes?
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                        if (_inputEnd > _inputPtr) { // 3 bytes?
                            d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                            c |= (d << 16);
                        }
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_4(c);
                // Let's add first part right away:
                outputBuffer[outPtr++] = (char) (0xD800 | (c >> 10));
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = _textBuilder.finishCurrentSegment();
                    outPtr = 0;
                }
                c = 0xDC00 | (c & 0x3FF);
                // And let the other char output down below
                break;
            case XmlCharTypes.CT_MULTIBYTE_N:
                reportInvalidInitial(c);

            case XmlCharTypes.CT_DTD_QUOTE: // apos or quot
                if (_elemAttrQuote == 0) {
                    _elemAttrQuote = (byte) c;
                } else {
                    if (_elemAttrQuote == c) {
                        _elemAttrQuote = 0;
                    }
                }
                break;

            case XmlCharTypes.CT_DTD_LT:
                if (!_inDtdDeclaration) {
                    _inDtdDeclaration = true;
                }
                break;
            case XmlCharTypes.CT_DTD_GT:
                if (_elemAttrQuote == 0) {
                    _inDtdDeclaration = false;
                }
                break;
            case XmlCharTypes.CT_DTD_RBRACKET:
                if (!_inDtdDeclaration && _elemAttrQuote == 0) {
                    _textBuilder.setCurrentLength(outPtr);
                    return true;
                }
                break;
            // default:
                // Other types are not important here...
            }
            // Ok, can output the char (we know there's room for one more)
            outputBuffer[outPtr++] = (char) c;
        }
        _textBuilder.setCurrentLength(outPtr);
        return false;
    }
    
    protected final int parseCommentContents()
        throws XMLStreamException
    {
        // Left-overs from last input block?
        if (_pendingInput != 0) { // CR, multi-byte, or '-'?
            int result = handleCommentPending();
            // If there's not enough input, or if we completed, can leave
            if (result != 0) {
                return result;
            }
            // otherwise we should be good to continue
        }

        char[] outputBuffer = _textBuilder.getBufferWithoutReset();
        int outPtr = _textBuilder.getCurrentLength();

        final int[] TYPES = _charTypes.OTHER_CHARS;
        final byte[] inputBuffer = _inputBuffer;

        main_loop:
        while (true) {
            int c;
            // Then the tight ASCII non-funny-char loop:
            ascii_loop:
            while (true) {
                if (_inputPtr >= _inputEnd) {
                    break main_loop;
                }
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = _textBuilder.finishCurrentSegment();
                    outPtr = 0;
                }
                int max = _inputEnd;
                {
                    int max2 = _inputPtr + (outputBuffer.length - outPtr);
                    if (max2 < max) {
                        max = max2;
                    }
                }
                while (_inputPtr < max) {
                    c = (int) inputBuffer[_inputPtr++] & 0xFF;
                    if (TYPES[c] != 0) {
                        break ascii_loop;
                    }
                    outputBuffer[outPtr++] = (char) c;
                }
            }

            switch (TYPES[c]) {
            case XmlCharTypes.CT_INVALID:
                throwInvalidXmlChar(c);
            case XmlCharTypes.CT_WS_CR:
                {
                    if (_inputPtr >= _inputEnd) {
                        _pendingInput = PENDING_STATE_CR;
                        break main_loop;
                    }
                    if (inputBuffer[_inputPtr] == BYTE_LF) {
                        ++_inputPtr;
                    }
                    markLF();
                }
                c = INT_LF;
                break;
            case XmlCharTypes.CT_WS_LF:
                markLF();
                break;
            case XmlCharTypes.CT_MULTIBYTE_2:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_2(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_3:
                if ((_inputEnd - _inputPtr) < 2) {
                    if (_inputEnd > _inputPtr) { // 2 bytes available
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_3(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_4:
                if ((_inputEnd - _inputPtr) < 3) {
                    if (_inputEnd > _inputPtr) { // at least 2 bytes?
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                        if (_inputEnd > _inputPtr) { // 3 bytes?
                            d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                            c |= (d << 16);
                        }
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_4(c);
                // Let's add first part right away:
                outputBuffer[outPtr++] = (char) (0xD800 | (c >> 10));
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = _textBuilder.finishCurrentSegment();
                    outPtr = 0;
                }
                c = 0xDC00 | (c & 0x3FF);
                // And let the other char output down below
                break;
            case XmlCharTypes.CT_MULTIBYTE_N:
                reportInvalidInitial(c);
            case XmlCharTypes.CT_HYPHEN: // '-->'?
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = PENDING_STATE_COMMENT_HYPHEN1;
                    break main_loop;
                }
                if (_inputBuffer[_inputPtr] == BYTE_HYPHEN) { // ok, must be end then
                    ++_inputPtr;
                    if (_inputPtr >= _inputEnd) {
                        _pendingInput = PENDING_STATE_COMMENT_HYPHEN2;
                        break main_loop;
                    }
                    if (_inputBuffer[_inputPtr++] != BYTE_GT) {
                        reportDoubleHyphenInComments();
                    }
                    _textBuilder.setCurrentLength(outPtr);
                    _state = STATE_DEFAULT;
                    _nextEvent = EVENT_INCOMPLETE;
                    return COMMENT;
                }
                break;
            // default:
                // Other types are not important here...
            }

            // Ok, can output the char (we know there's room for one more)
            outputBuffer[outPtr++] = (char) c;
        }

        _textBuilder.setCurrentLength(outPtr);
        return EVENT_INCOMPLETE;
    }

    /**
     * @return EVENT_INCOMPLETE, if there's not enough input to
     *   handle pending char, COMMENT, if we handled complete
     *   "-->" end marker, or 0 to indicate something else
     *   was succesfully handled.
     */
    protected final int handleCommentPending()
        throws XMLStreamException
    {
        if (_inputPtr >= _inputEnd) {
            return EVENT_INCOMPLETE;
        }
        if (_pendingInput == PENDING_STATE_COMMENT_HYPHEN1) {
            if (_inputBuffer[_inputPtr] != BYTE_HYPHEN) {
                // can't be the end marker, just append '-' and go
                _pendingInput = 0;
                _textBuilder.append("-");
                return 0;
            }
            ++_inputPtr;
            _pendingInput = PENDING_STATE_COMMENT_HYPHEN2;
            if (_inputPtr >= _inputEnd) { // no more input?
                return EVENT_INCOMPLETE;
            }
            // continue
        }
        if (_pendingInput == PENDING_STATE_COMMENT_HYPHEN2) {
            _pendingInput = 0;
            byte b = _inputBuffer[_inputPtr++];
            if (b != BYTE_GT) {
                reportDoubleHyphenInComments();
            } 
            _state = STATE_DEFAULT;
            _nextEvent = EVENT_INCOMPLETE;
            return COMMENT;
        }
        // Otherwise can use default code
        return handleAndAppendPending() ? 0 : EVENT_INCOMPLETE;
    }

    protected final int parseCDataContents()
        throws XMLStreamException
    {
        // Left-overs from last input block?
        if (_pendingInput != 0) { // CR, multi-byte, or ']'?
            int result = handleCDataPending();
            // If there's not enough input, or if we completed, can leave
            if (result != 0) {
                return result;
            }
            // otherwise we should be good to continue
        }
        char[] outputBuffer = _textBuilder.getBufferWithoutReset();
        int outPtr = _textBuilder.getCurrentLength();
    
        final int[] TYPES = _charTypes.OTHER_CHARS;
        final byte[] inputBuffer = _inputBuffer;
    
        main_loop:
        while (true) {
            int c;
            // Then the tight ASCII non-funny-char loop:
            ascii_loop:
            while (true) {
                if (_inputPtr >= _inputEnd) {
                    break main_loop;
                }
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = _textBuilder.finishCurrentSegment();
                    outPtr = 0;
                }
                int max = _inputEnd;
                {
                    int max2 = _inputPtr + (outputBuffer.length - outPtr);
                    if (max2 < max) {
                        max = max2;
                    }
                }
                while (_inputPtr < max) {
                    c = (int) inputBuffer[_inputPtr++] & 0xFF;
                    if (TYPES[c] != 0) {
                        break ascii_loop;
                    }
                    outputBuffer[outPtr++] = (char) c;
                }
            }
    
            switch (TYPES[c]) {
            case XmlCharTypes.CT_INVALID:
                throwInvalidXmlChar(c);
            case XmlCharTypes.CT_WS_CR:
                {
                    if (_inputPtr >= _inputEnd) {
                        _pendingInput = PENDING_STATE_CR;
                        break main_loop;
                    }
                    if (inputBuffer[_inputPtr] == BYTE_LF) {
                        ++_inputPtr;
                    }
                    markLF();
                }
                c = INT_LF;
                break;
            case XmlCharTypes.CT_WS_LF:
                markLF();
                break;
            case XmlCharTypes.CT_MULTIBYTE_2:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_2(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_3:
                if ((_inputEnd - _inputPtr) < 2) {
                    if (_inputEnd > _inputPtr) { // 2 bytes available
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_3(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_4:
                if ((_inputEnd - _inputPtr) < 3) {
                    if (_inputEnd > _inputPtr) { // at least 2 bytes?
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                        if (_inputEnd > _inputPtr) { // 3 bytes?
                            d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                            c |= (d << 16);
                        }
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_4(c);
                // Let's add first part right away:
                outputBuffer[outPtr++] = (char) (0xD800 | (c >> 10));
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = _textBuilder.finishCurrentSegment();
                    outPtr = 0;
                }
                c = 0xDC00 | (c & 0x3FF);
                // And let the other char output down below
                break;
            case XmlCharTypes.CT_MULTIBYTE_N:
                reportInvalidInitial(c);
            case XmlCharTypes.CT_RBRACKET: // ']]>'?
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = PENDING_STATE_CDATA_BRACKET1;
                    break main_loop;
                }
                // Hmmh. This is more complex... so be it.
                if (_inputBuffer[_inputPtr] == BYTE_RBRACKET) { // end might be nigh...
                    ++_inputPtr;
                    while (true) {
                        if (_inputPtr >= _inputEnd) {
                            _pendingInput = PENDING_STATE_CDATA_BRACKET2;
                            break main_loop;
                        }
                        if (_inputBuffer[_inputPtr] == BYTE_GT) {
                            ++_inputPtr;
                            _textBuilder.setCurrentLength(outPtr);
                            _state = STATE_DEFAULT;
                            _nextEvent = EVENT_INCOMPLETE;
                            return CDATA;
                        }
                        if (_inputBuffer[_inputPtr] != BYTE_RBRACKET) { // neither '>' nor ']'; push "]]" back
                            outputBuffer[outPtr++] = ']';
                            if (outPtr >= outputBuffer.length) {
                                outputBuffer = _textBuilder.finishCurrentSegment();
                                outPtr = 0;
                            }
                            outputBuffer[outPtr++] = ']';
                            continue main_loop;
                        }
                        // Got third bracket; push one back, keep on checking
                        ++_inputPtr;
                        outputBuffer[outPtr++] = ']';
                        if (outPtr >= outputBuffer.length) {
                            outputBuffer = _textBuilder.finishCurrentSegment();
                            outPtr = 0;
                        }
                    }
                }
                break;
            // default:
                // Other types are not important here...
            }
    
            // Ok, can output the char (we know there's room for one more)
            outputBuffer[outPtr++] = (char) c;
        }
    
        _textBuilder.setCurrentLength(outPtr);
        return EVENT_INCOMPLETE;
    }

    /**
     * @return EVENT_INCOMPLETE, if there's not enough input to
     *   handle pending char, CDATA, if we handled complete
     *   "]]>" end marker, or 0 to indicate something else
     *   was succesfully handled.
     */
    protected final int handleCDataPending()
        throws XMLStreamException
    {
        if (_pendingInput == PENDING_STATE_CDATA_BRACKET1) {
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }
            if (_inputBuffer[_inputPtr] != BYTE_RBRACKET) {
                // can't be the end marker, just append ']' and go
                _textBuilder.append(']');
                return (_pendingInput = 0);
            }
            ++_inputPtr;
            _pendingInput = PENDING_STATE_CDATA_BRACKET2;
            if (_inputPtr >= _inputEnd) { // no more input?
                return EVENT_INCOMPLETE;
            }
            // continue
        }
        while (_pendingInput == PENDING_STATE_CDATA_BRACKET2) {
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }
            byte b = _inputBuffer[_inputPtr++];
            if (b == BYTE_GT) {
                _pendingInput = 0;
                _state = STATE_DEFAULT;
                _nextEvent = EVENT_INCOMPLETE;
                return CDATA;
            }
            if (b != BYTE_RBRACKET) {
                --_inputPtr;
                _textBuilder.append("]]");
                return (_pendingInput = 0);
            }
            _textBuilder.append(']');
        }
        // Otherwise can use default code
        return handleAndAppendPending() ? 0 : EVENT_INCOMPLETE;
    }
    
    protected final int parsePIData()
        throws XMLStreamException
    {
        // Left-overs from last input block?
        if (_pendingInput != 0) { // CR, multi-byte, '?'
            int result = handlePIPending();
            // If there's not enough input, or if we completed, can leave
            if (result != 0) {
                return result;
            }
            // otherwise we should be good to continue
        }
        
        char[] outputBuffer = _textBuilder.getBufferWithoutReset();
        int outPtr = _textBuilder.getCurrentLength();
        
        final int[] TYPES = _charTypes.OTHER_CHARS;
        final byte[] inputBuffer = _inputBuffer;
        
        main_loop:
        while (true) {
            int c;
            // Then the tight ASCII non-funny-char loop:
            ascii_loop:
            while (true) {
                if (_inputPtr >= _inputEnd) {
                    break main_loop;
                }
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = _textBuilder.finishCurrentSegment();
                    outPtr = 0;
                }
                int max = _inputEnd;
                {
                    int max2 = _inputPtr + (outputBuffer.length - outPtr);
                    if (max2 < max) {
                        max = max2;
                    }
                }
                while (_inputPtr < max) {
                    c = (int) inputBuffer[_inputPtr++] & 0xFF;
                    if (TYPES[c] != 0) {
                        break ascii_loop;
                    }
                    outputBuffer[outPtr++] = (char) c;
                }
            }

            switch (TYPES[c]) {
            case XmlCharTypes.CT_INVALID:
                throwInvalidXmlChar(c);
            case XmlCharTypes.CT_WS_CR:
                {
                    if (_inputPtr >= _inputEnd) {
                        _pendingInput = PENDING_STATE_CR;
                        break main_loop;
                    }
                    if (inputBuffer[_inputPtr] == BYTE_LF) {
                        ++_inputPtr;
                    }
                    markLF();
                }
                c = INT_LF;
                break;
            case XmlCharTypes.CT_WS_LF:
                markLF();
                break;
            case XmlCharTypes.CT_MULTIBYTE_2:
                if (_inputPtr >= _inputEnd) {
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_2(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_3:
                if ((_inputEnd - _inputPtr) < 2) {
                    if (_inputEnd > _inputPtr) { // 2 bytes available
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_3(c);
                break;
            case XmlCharTypes.CT_MULTIBYTE_4:
                if ((_inputEnd - _inputPtr) < 3) {
                    if (_inputEnd > _inputPtr) { // at least 2 bytes?
                        int d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                        c |= (d << 8);
                        if (_inputEnd > _inputPtr) { // 3 bytes?
                            d = (int) _inputBuffer[_inputPtr++] & 0xFF;
                            c |= (d << 16);
                        }
                    }
                    _pendingInput = c;
                    break main_loop;
                }
                c = decodeUtf8_4(c);
                // Let's add first part right away:
                outputBuffer[outPtr++] = (char) (0xD800 | (c >> 10));
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = _textBuilder.finishCurrentSegment();
                    outPtr = 0;
                }
                c = 0xDC00 | (c & 0x3FF);
                // And let the other char output down below
                break;
            case XmlCharTypes.CT_MULTIBYTE_N:
                reportInvalidInitial(c);
            case XmlCharTypes.CT_QMARK:

                if (_inputPtr >= _inputEnd) {
                    _pendingInput = PENDING_STATE_PI_QMARK;
                    break main_loop;
                }
                if (_inputBuffer[_inputPtr] == BYTE_GT) { // end
                    ++_inputPtr;
                    _textBuilder.setCurrentLength(outPtr);
                    _state = STATE_DEFAULT;
                    _nextEvent = EVENT_INCOMPLETE;
                    return PROCESSING_INSTRUCTION;
                }
                // Not end mark, just need to reprocess the second char
                break;
            // default:
                // Other types are not important here...
            }

            // Ok, can output the char (we know there's room for one more)
            outputBuffer[outPtr++] = (char) c;
        }
        _textBuilder.setCurrentLength(outPtr);
        return EVENT_INCOMPLETE;
    }

    /**
     * @return EVENT_INCOMPLETE, if there's not enough input to
     *   handle pending char, PROCESSING_INSTRUCTION, if we handled complete
     *   "?>" end marker, or 0 to indicate something else
     *   was succesfully handled.
     */
    protected final int handlePIPending()
        throws XMLStreamException
    {
        // First, the special case, end marker:
        if (_pendingInput == PENDING_STATE_PI_QMARK) {
            if (_inputPtr >= _inputEnd) {
                return EVENT_INCOMPLETE;
            }
            byte b = _inputBuffer[_inputPtr];
            _pendingInput = 0;
            if (b != BYTE_GT) {
                // can't be the end marker, just append '-' and go
                _textBuilder.append('?');
                return 0;
            }
            ++_inputPtr;
            _state = STATE_DEFAULT;
            _nextEvent = EVENT_INCOMPLETE;
            return PROCESSING_INSTRUCTION;
        }
        // Otherwise can use default code
        return handleAndAppendPending() ? 0 : EVENT_INCOMPLETE;
    }

    /*
    /**********************************************************************
    /* Multi-byte char decoding
    /**********************************************************************
     */

    /**
     *<p>
     * Note: caller must guarantee enough data is available before
     * calling the method
     */
    protected final int decodeUtf8_2(int c)
        throws XMLStreamException
    {
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            reportInvalidOther(d & 0xFF, _inputPtr);
        }
        return ((c & 0x1F) << 6) | (d & 0x3F);
    }

    protected final void skipUtf8_2(int c)
        throws XMLStreamException
    {
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            reportInvalidOther(d & 0xFF, _inputPtr);
        }
    }

    /**
     *<p>
     * Note: caller must guarantee enough data is available before
     * calling the method
     */
    protected final int decodeUtf8_3(int c1)
        throws XMLStreamException
    {
        c1 &= 0x0F;
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            reportInvalidOther(d & 0xFF, _inputPtr);
        }
        int c = (c1 << 6) | (d & 0x3F);
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            reportInvalidOther(d & 0xFF, _inputPtr);
        }
        c = (c << 6) | (d & 0x3F);
        if (c1 >= 0xD) { // 0xD800-0xDFFF, 0xFFFE-0xFFFF illegal
            if (c >= 0xD800) { // surrogates illegal, as well as 0xFFFE/0xFFFF
                if (c < 0xE000 || (c >= 0xFFFE && c <= 0xFFFF)) {
                    throwInvalidXmlChar(c);
                }
            }
        }
        return c;
    }

    protected final int decodeUtf8_3(int c1, int c2, int c3)
        throws XMLStreamException
    {
        // Note: first char is assumed to have been checked
        if ((c2 & 0xC0) != 0x080) {
            reportInvalidOther(c2 & 0xFF, _inputPtr-1);
        }
        if ((c3 & 0xC0) != 0x080) {
            reportInvalidOther(c3 & 0xFF, _inputPtr);
        }
        int c = ((c1 & 0x0F) << 12) | ((c2 & 0x3F) << 6) | (c3 & 0x3F);
        if (c1 >= 0xD) { // 0xD800-0xDFFF, 0xFFFE-0xFFFF illegal
            if (c >= 0xD800) { // surrogates illegal, as well as 0xFFFE/0xFFFF
                if (c < 0xE000 || (c >= 0xFFFE && c <= 0xFFFF)) {
                    throwInvalidXmlChar(c);
                }
            }
        }
        return c;
    }

    protected final int decodeUtf8_4(int c)
        throws XMLStreamException
    {
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            reportInvalidOther(d & 0xFF, _inputPtr);
        }
        c = ((c & 0x07) << 6) | (d & 0x3F);
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            reportInvalidOther(d & 0xFF, _inputPtr);
        }
        c = (c << 6) | (d & 0x3F);
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            reportInvalidOther(d & 0xFF, _inputPtr);
        }
        /* note: won't change it to negative here, since caller
         * already knows it'll need a surrogate
         */
        return ((c << 6) | (d & 0x3F)) - 0x10000;
    }

    /**
     * @return Character value <b>minus 0x10000</c>; this so that caller
     *    can readily expand it to actual surrogates
     */
    protected final int decodeUtf8_4(int c1, int c2, int c3, int c4)
        throws XMLStreamException
    {
        /* Note: first char is assumed to have been checked,
         * (but not yet masked)
         */
        if ((c2 & 0xC0) != 0x080) {
            reportInvalidOther(c2 & 0xFF, _inputPtr-2);
        }
        int c = ((c1 & 0x07) << 6) | (c2 & 0x3F);
        if ((c3 & 0xC0) != 0x080) {
            reportInvalidOther(c3 & 0xFF, _inputPtr-1);
        }
        c = (c << 6) | (c3 & 0x3F);
        if ((c4 & 0xC0) != 0x080) {
            reportInvalidOther(c4 & 0xFF, _inputPtr);
        }
        return ((c << 6) | (c4 & 0x3F)) - 0x10000;
    }

    /*
    /**********************************************************************
    /* Name handling
    /**********************************************************************
     */

    protected final PName addPName(int hash, int[] quads, int qlen, int lastQuadBytes)
        throws XMLStreamException
    {
        return addUtfPName(_charTypes, hash, quads, qlen, lastQuadBytes);
    }

    /*
    /**********************************************************************
    /* Error reporting
    /**********************************************************************
     */

    protected void reportInvalidInitial(int mask)
        throws XMLStreamException
    {
        reportInputProblem("Invalid UTF-8 start byte 0x"
                           +Integer.toHexString(mask));
    }

    protected void reportInvalidOther(int mask)
        throws XMLStreamException
    {
        reportInputProblem("Invalid UTF-8 middle byte 0x"
                           +Integer.toHexString(mask));
    }

    protected void reportInvalidOther(int mask, int ptr)
        throws XMLStreamException
    {
        _inputPtr = ptr;
        reportInvalidOther(mask);
    }
}
