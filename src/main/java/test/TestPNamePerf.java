package test;

import java.io.*;
import javax.xml.stream.*;


import com.fasterxml.aalto.in.*;
import com.fasterxml.aalto.util.*;

public final class TestPNamePerf
{
    final static int INT_A = 'A';

    final int mRepCount;

    int mTmpChar = 0;

    final byte[] mInputBuffer;

    final ByteBasedPNameTable mSymbols;

    final XmlCharTypes mCharTypes;

    int mInputPtr;

    int mInputLen;

    protected int[] mQuadBuffer = new int[64];

    protected char[] mNameBuffer = new char[100];

    public TestPNamePerf(byte[] data, int repCount)
    {
        mInputBuffer = data;
        mInputLen = data.length;
        mRepCount = repCount;
        ReaderConfig cfg = new ReaderConfig();
        cfg.setActualEncoding(CharsetNames.CS_UTF8);
        mSymbols = cfg.getBBSymbols();
        mCharTypes = cfg.getCharTypes();
    }

    public void test()
        throws IOException, XMLStreamException
    {
       int round = 0;

       for (; true; ++round) {
           String msg = "[null]";
           int total = 0;

           final int TYPES = 3;

           long now = System.currentTimeMillis();
           //switch (round % TYPES) {
           switch (0) {
           case 1:
               msg = "[Regular]";
               total = testRegularA();
               break;
           case 2:
               msg = "[New]";
               total = testNewA();
               break;
           case 0:
               msg = "[New/2]";
               total = testNew2A();
               break;
           default:
               throw new Error("Unexpected round, #"+round);
           }

           now = System.currentTimeMillis() - now;
           System.out.println(msg+" -> "+now+" msecs (total "+total+")");

           if ((round % TYPES) == 0) {
               System.out.println();
           }

           try { Thread.sleep(200L); } catch (Exception e) { }
           System.gc();
           try { Thread.sleep(200L); } catch (Exception e) { }
       }
    }

    private int testRegularA()
        throws IOException, XMLStreamException
    {
        int total = 0;
        for (int i = 0; i < mRepCount; ++i) {
            mInputPtr = 0;
            total += testRegular();
        }
        return total;
    }
    private int testNewA()
        throws IOException, XMLStreamException
    {
        int total = 0;
        for (int i = 0; i < mRepCount; ++i) {
            mInputPtr = 0;
            total += testNew();
        }
        return total;
    }
    private int testNew2A()
        throws IOException, XMLStreamException
    {
        int total = 0;
        for (int i = 0; i < mRepCount; ++i) {
            mInputPtr = 0;
            total += testNew2();
        }
        return total;
    }

    private int testRegular()
        throws IOException, XMLStreamException
    {
        ByteBasedPName name = null;
        int count = 0;

        while (mInputPtr < mInputLen) {
            byte b = mInputBuffer[mInputPtr++];
            int ch = (int) b & 0xFF;
            /* We'll skip all intervening chars that can't start a name,
             * including white space
             */
            if (ch >= INT_A) {
                name = parsePName(b);
                count += name.sizeInQuads();
            }
        }
        return count + name.sizeInQuads();
    }

    private int testNew()
        throws IOException, XMLStreamException
    {
        ByteBasedPName name = null;
        int count = 0;

        while (mInputPtr < mInputLen) {
            byte b = mInputBuffer[mInputPtr++];
            int ch = (int) b & 0xFF;
            /* We'll skip all intervening chars that can't start a name,
             * including white space
             */
            if (ch >= INT_A) {
                name = parsePNameNew(b);
                count += name.sizeInQuads();
            }
        }
        return count + name.sizeInQuads();
    }

    private int testNew2()
        throws IOException, XMLStreamException
    {
        ByteBasedPName name = null;
        int count = 0;

        while (mInputPtr < mInputLen) {
            byte b = mInputBuffer[mInputPtr++];
            int ch = (int) b & 0xFF;
            /* We'll skip all intervening chars that can't start a name,
             * including white space
             */
            if (ch >= INT_A) {
                name = parsePNameNew2(b);
                count += name.sizeInQuads();
            }
        }
        return count + name.sizeInQuads();
    }

    protected ByteBasedPName parsePName(byte b)
        throws XMLStreamException
    {
        int q = b & 0xFF;

        if (q < INT_A) { // lowest acceptable start char, except for ':' that would be allowed in non-ns mode
            reportError("; expected a name start character");
        }

        int[] quads = mQuadBuffer;
        int qix = 0;
        int firstQuad = 0;

        while (true) {
            // Second byte
            if (mInputPtr >= mInputLen) {
                loadMoreGuaranteed();
            }
            int i2 = mInputBuffer[mInputPtr++] & 0xFF;
            /* For other bytes beyond first we have to do bit more complicated
             * check, to reliably find out where name ends. Still can do quite
             * simple checks though
             */
            if (i2 < 65) {
                // Ok; "_" (45), "." (46) and "0"-"9"/":" (48 - 57/58) still name chars
                if (i2 < 45 || i2 > 58 || i2 == 47) {
                    // End of name, a single ascii char?
                    return findPName(q, 1, firstQuad, qix, quads);
                }
            }
            // 3rd byte:
            q = (q << 8) | i2;
            i2 = (int) ((mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++] : loadOne()) & 0xFF;
            if (i2 < 65) {
                if (i2 < 45 || i2 > 58 || i2 == 47) { // 2 (ascii) char name?
                    return findPName(q, 2, firstQuad, qix, quads);
                }
            }
            // 4th byte:
            q = (q << 8) | i2;
            i2 = (int) ((mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++] : loadOne()) & 0xFF;
            if (i2 < 65) {
                if (i2 < 45 || i2 > 58 || i2 == 47) { // 2 (ascii) char name?
                    return findPName(q, 3, firstQuad, qix, quads);
                }
            }
            q = (q << 8) | i2;
            i2 = (int) ((mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++] : loadOne()) & 0xFF;
            if (i2 < 65) {
                if (i2 < 45 || i2 > 58 || i2 == 47) { // 2 (ascii) char name?
                    return findPName(q, 4, firstQuad, qix, quads);
                }
            }
            if (qix == 0) { // not yet, was the first quad
                firstQuad = q;
            } else if (qix == 1) { // second quad, need to init buffer
                quads[0] = firstQuad;
                quads[1] = q;
            } else { // 3rd or after... need to make sure there's room
                if (qix >= quads.length) { // let's just double?
                    mQuadBuffer = quads = DataUtil.growArrayBy(quads, quads.length);
                }
                quads[qix] = q;
            }
            ++qix;
            q = i2;
        }
    }

    protected ByteBasedPName parsePNameNew(byte b)
        throws XMLStreamException
    {
        // First: can we optimize out bounds checks?
        if ((mInputLen - mInputPtr) < 8) { // got 1 byte, but need 7, plus one trailing
            return parsePName(b);
        }

        int q1 = b & 0xFF;

        if (q1 < INT_A) { // lowest acceptable start char, except for ':' that would be allowed in non-ns mode
            reportError("; expected a name start character");
        }

        // If so, can also unroll loops nicely
        int i2 = mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            // Ok; "_" (45), "." (46) and "0"-"9"/":" (48 - 57/58) still name chars
            if (i2 < 45 || i2 > 58 || i2 == 47) {
                return findPName(q1, 1);
            }
        }
        q1 = (q1 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 2 (ascii) char name?
                return findPName(q1, 2);
            }
        }
        q1 = (q1 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
       if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 3 (ascii) char name?
                return findPName(q1, 3);
            }
        }
        q1 = (q1 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 4 (ascii) char name?
                return findPName(q1, 4);
            }
        }

        // Ok, so far so good; one quad, one byte. Then the second
        int q2 = i2;
        i2 = mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            // Ok; "_" (45), "." (46) and "0"-"9"/":" (48 - 57/58) still name chars
            if (i2 < 45 || i2 > 58 || i2 == 47) {
                return findPName(q1, q2, 1);
            }
        }

        q2 = (q2 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 2 (ascii) char name?
                return findPName(q1, q2, 2);
            }
        }
        q2 = (q2 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 3 (ascii) char name?
                return findPName(q1, q2, 3);
            }
        }
        q2 = (q2 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 4 (ascii) char name?
                return findPName(q1, q2, 4);
            }
        }

        // Ok, no, longer loop. Let's offline
        int[] quads = mQuadBuffer;
        quads[0] = q1;
        quads[1] = q2;
        return parsePNameNewLong(i2, quads);
    }

    protected ByteBasedPName parsePNameNew2(byte b)
        throws XMLStreamException
    {
        // First: can we optimize out bounds checks?
        if ((mInputLen - mInputPtr) < 8) { // got 1 byte, but need 7, plus one trailing
            return parsePName(b);
        }

        int q1 = b & 0xFF;
        if (q1 < INT_A) { // lowest acceptable start char, except for ':' that would be allowed in non-ns mode
            reportError("; expected a name start character");
        }

        // If so, can also unroll loops nicely
        int i2 = mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            // Ok; "_" (45), "." (46) and "0"-"9"/":" (48 - 57/58) still name chars
            if (i2 < 45 || i2 > 58 || i2 == 47) {
                return findPName(q1, 1);
            }
        }
        q1 = (q1 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 2 (ascii) char name?
                return findPName(q1, 2);
            }
        }
        q1 = (q1 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
       if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 3 (ascii) char name?
                return findPName(q1, 3);
            }
        }
        q1 = (q1 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 4 (ascii) char name?
                return findPName(q1, 4);
            }
        }

        // Longer, let's offline:
        return parsePNameNewMedium(i2, q1);
    }

    protected ByteBasedPName parsePNameNewMedium(int i2, int q1)
        throws XMLStreamException
    {
        // Ok, so far so good; one quad, one byte. Then the second
        int q2 = i2;
        i2 = mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            // Ok; "_" (45), "." (46) and "0"-"9"/":" (48 - 57/58) still name chars
            if (i2 < 45 || i2 > 58 || i2 == 47) {
                return findPName(q1, q2, 1);
            }
        }

        q2 = (q2 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 2 (ascii) char name?
                return findPName(q1, q2, 2);
            }
        }
        q2 = (q2 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 3 (ascii) char name?
                return findPName(q1, q2, 3);
            }
        }
        q2 = (q2 << 8) | i2;
        i2 = (int) mInputBuffer[mInputPtr++] & 0xFF;
        if (i2 < 65) {
            if (i2 < 45 || i2 > 58 || i2 == 47) { // 4 (ascii) char name?
                return findPName(q1, q2, 4);
            }
        }

        // Ok, no, longer loop. Let's offline
        int[] quads = mQuadBuffer;
        quads[0] = q1;
        quads[1] = q2;
        return parsePNameNewLong(i2, quads);
    }

    protected ByteBasedPName parsePNameNewLong(int q, int[] quads)
        throws XMLStreamException
    {
        int qix = 2;
        while (true) {
            // Second byte of a new quad
            if (mInputPtr >= mInputLen) {
                loadMoreGuaranteed();
            }
            int i2 = mInputBuffer[mInputPtr++] & 0xFF;
            if (i2 < 65) {
                if (i2 < 45 || i2 > 58 || i2 == 47) {
                    // End of name, a single ascii char?
                    return findPName(q, quads, qix, 1);
                }
            }
            // 3rd byte:
            q = (q << 8) | i2;
            i2 = (int) ((mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++] : loadOne()) & 0xFF;
            if (i2 < 65) {
                if (i2 < 45 || i2 > 58 || i2 == 47) { // 2 (ascii) char name?
                    return findPName(q, quads, qix, 2);
                }
            }
            // 4th byte:
            q = (q << 8) | i2;
            i2 = (int) ((mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++] : loadOne()) & 0xFF;
            if (i2 < 65) {
                if (i2 < 45 || i2 > 58 || i2 == 47) { // 2 (ascii) char name?
                    return findPName(q, quads, qix, 3);
                }
            }
            q = (q << 8) | i2;
            i2 = (int) ((mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++] : loadOne()) & 0xFF;
            if (i2 < 65) {
                if (i2 < 45 || i2 > 58 || i2 == 47) { // 2 (ascii) char name?
                    return findPName(q, quads, qix, 4);
                }
            }
            if (qix >= quads.length) { // let's just double?
                mQuadBuffer = quads = DataUtil.growArrayBy(quads, quads.length);
            }
            quads[qix] = q;
            ++qix;
            q = i2;
        }
    }

    private final ByteBasedPName findPName(int onlyQuad, int lastByteCount)
        throws XMLStreamException
    {
        // First, need to push back the byte read but not used:
        --mInputPtr;
        int hash = ByteBasedPNameTable.calcHash(onlyQuad);
        ByteBasedPName name = mSymbols.findSymbol(hash, onlyQuad, 0);
        if (name == null) {
            // Let's simplify things a bit, and just use array based one then:
            mQuadBuffer[0] = onlyQuad;
            name = addPName(hash, mQuadBuffer, 1, lastByteCount);
        }
        return name;
    }

    private final ByteBasedPName findPName(int firstQuad, int secondQuad,
                                  int lastByteCount)
        throws XMLStreamException
    {
        // First, need to push back the byte read but not used:
        --mInputPtr;
        int hash = ByteBasedPNameTable.calcHash(firstQuad, secondQuad);
        ByteBasedPName name = mSymbols.findSymbol(hash, firstQuad, secondQuad);
        if (name == null) {
            // Let's just use array, then
            mQuadBuffer[0] = firstQuad;
            mQuadBuffer[1] = secondQuad;
            name = addPName(hash, mQuadBuffer, 2, lastByteCount);
        }
        return name;
    }

    private final ByteBasedPName findPName(int lastQuad, int[] quads, int qlen, int lastByteCount)
        throws XMLStreamException
    {
        // First, need to push back the byte read but not used:
        --mInputPtr;
        /* Nope, long (3 quads or more). At this point, the last quad is
         * not yet in the array, let's add:
         */
        if (qlen >= quads.length) { // let's just double?
            mQuadBuffer = quads = DataUtil.growArrayBy(quads, quads.length);
        }
        quads[qlen++] = lastQuad;
        int hash = ByteBasedPNameTable.calcHash(quads, qlen);
        ByteBasedPName name = mSymbols.findSymbol(hash, quads, qlen);
        if (name == null) {
            name = addPName(hash, quads, qlen, lastByteCount);
        }
        return name;
    }

    private final ByteBasedPName findPName(int lastQuad, int lastByteCount, int firstQuad,
                                  int qlen, int[] quads)
        throws XMLStreamException
    {
        // First, need to push back the byte read but not used:
        --mInputPtr;
        // Separate handling for short names:
        if (qlen <= 1) { // short name?
            if (qlen == 0) { // 4-bytes or less; only has 'lastQuad' defined
                int hash = ByteBasedPNameTable.calcHash(lastQuad, 0);
                ByteBasedPName name = mSymbols.findSymbol(hash, lastQuad, 0);
                if (name == null) {
                    // Let's simplify things a bit, and just use array based one then:
                    quads = mQuadBuffer;
                    quads[0] = lastQuad;
                    name = addPName(hash, quads, 1, lastByteCount);
                }
                return name;
            }

            int hash = ByteBasedPNameTable.calcHash(firstQuad, lastQuad);
            ByteBasedPName name = mSymbols.findSymbol(hash, firstQuad, lastQuad);
            if (name == null) {
                // As above, let's just use array, then
                quads = mQuadBuffer;
                quads[0] = firstQuad;
                quads[1] = lastQuad;
                name = addPName(hash, quads, 2, lastByteCount);
            }
            return name;
        }
        /* Nope, long (3 quads or more). At this point, the last quad is
         * not yet in the array, let's add:
         */
        if (qlen >= quads.length) { // let's just double?
            mQuadBuffer = quads = DataUtil.growArrayBy(quads, quads.length);
        }
        quads[qlen++] = lastQuad;
        int hash = ByteBasedPNameTable.calcHash(quads, qlen);
        ByteBasedPName name = mSymbols.findSymbol(hash, quads, qlen);
        if (name == null) {
            name = addPName(hash, quads, qlen, lastByteCount);
        }

        return name;
    }

    protected final ByteBasedPName addPName(int hash, int[] quads, int qlen, int lastQuadBytes)
        throws XMLStreamException
    {
        // 4 bytes per quad, except last one maybe less
        int byteLen = (qlen << 2) - 4 + lastQuadBytes;

        /* And last one is not correctly aligned (leading zero bytes instead
         * need to shift a bit, instead of trailing). Only need to shift it
         * for UTF-8 decoding; need revert for storage (since key will not
         * be aligned, to optimize lookup speed)
         */
        int lastQuad;

        if (lastQuadBytes < 4) {
            lastQuad = quads[qlen-1];
            // 8/16/24 bit left shift
            quads[qlen-1] = (lastQuad << ((4 - lastQuadBytes) << 3));
        } else {
            lastQuad = 0;
        }

        // Let's handle first char separately (different validation):
        int ch = (quads[0] >>> 24);
        boolean ok;
        int ix = 1;
        char[] cbuf = mNameBuffer;
        int cix  = 0;
        final int[] TYPES = mCharTypes.NAME_CHARS;

        switch (TYPES[ch]) {
        case XmlCharTypes.CT_NAME_NONE:
        case XmlCharTypes.CT_NAME_COLON: // not ok as first
        case XmlCharTypes.CT_NAME_NONFIRST:
        case InputCharTypes.CT_INPUT_NAME_MB_N:
            ok = false;
            break;
        case XmlCharTypes.CT_NAME_ANY:
            ok = true;
            break;
        default: // multi-byte (UTF-8) chars:
            {
                int needed;
                
                if ((ch & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
                    ch &= 0x1F;
                    needed = 1;
                } else if ((ch & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
                    ch &= 0x0F;
                    needed = 2;
                } else if ((ch & 0xF8) == 0xF0) { // 4 bytes; double-char with surrogates and all...
                    ch &= 0x07;
                    needed = 3;
                } else { // 5- and 6-byte chars not valid xml chars
                    reportError(ch);
                    needed = ch = 1; // never really gets this far
                }
                if ((ix + needed) > byteLen) {
                    reportError(ch);
                }
                ix += needed;
                
                int q = quads[0];
                // Always need at least one more right away:
                int ch2 = (q >> 16) & 0xFF;
                if ((ch2 & 0xC0) != 0x080) {
                    reportError(ch2);
                }
                ch = (ch << 6) | (ch2 & 0x3F);
                
                /* And then may need more. Note: here we do not do all the
                 * checks that UTF-8 text decoder might do. Reason is that
                 * name validity checking methods handle most of such checks
                 */
                if (needed > 1) {
                    ch2 = (q >> 8) & 0xFF;
                    if ((ch2 & 0xC0) != 0x080) {
                        reportError(ch2);
                    }
                    ch = (ch << 6) | (ch2 & 0x3F);
                    if (needed > 2) { // 4 bytes? (need surrogates on output)
                        ch2 = q & 0xFF;
                        if ((ch2 & 0xC0) != 0x080) {
                            reportError(ch2 & 0xFF);
                        }
                        ch = (ch << 6) | (ch2 & 0x3F);
                    }
                }
                ok = XmlChars.is10NameStartChar(ch);
                if (needed > 2) { // outside of basic 16-bit range? need surrogates
                    /* so, let's first output first char (high surrogate),
                     * let second be output by later code
                     */
                    ch -= 0x10000; // to normalize it starting with 0x0
                    cbuf[cix++] = (char) (0xD800 + (ch >> 10));
                    ch = (0xDC00 | (ch & 0x03FF));
                }
            }
        }

        if (!ok) { // 0 to indicate it's first char, even with surrogates
            reportError(ch);
        }

        cbuf[cix++] = (char) ch; // the only char, or second (low) surrogate

        /* Whoa! Tons of code for just the start char. But now we get to
         * decode the name proper, at last!
         */
        int last_colon = -1;

        for (; ix < byteLen; ) {
            ch = quads[ix >> 2]; // current quad, need to shift+mask
            int byteIx = (ix & 3);
            ch = (ch >> ((3 - byteIx) << 3)) & 0xFF;
            ++ix;

            // Ascii?
            switch (TYPES[ch]) {
            case XmlCharTypes.CT_NAME_NONE:
            case XmlCharTypes.CT_MULTIBYTE_N:
                ok = false;
                break;
            case XmlCharTypes.CT_NAME_COLON: // not ok as first
                if (last_colon >= 0) {
                    reportError(0);
                }
                last_colon = cix;
                ok = true;
                break;
            case XmlCharTypes.CT_NAME_NONFIRST:
            case XmlCharTypes.CT_NAME_ANY:
                ok = true;
                break;
            default:
                {
                    int needed;
                    if ((ch & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
                        ch &= 0x1F;
                        needed = 1;
                    } else if ((ch & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
                        ch &= 0x0F;
                        needed = 2;
                    } else if ((ch & 0xF8) == 0xF0) { // 4 bytes; double-char with surrogates and all...
                        ch &= 0x07;
                        needed = 3;
                    } else { // 5- and 6-byte chars not valid xml chars
                        reportError(ch);
                        needed = ch = 1; // never really gets this far
                    }
                    if ((ix + needed) > byteLen) {
                        reportError(cix);
                    }
                    
                    // Ok, always need at least one more:
                    int ch2 = quads[ix >> 2]; // current quad, need to shift+mask
                    byteIx = (ix & 3);
                    ch2 = (ch2 >> ((3 - byteIx) << 3));
                    ++ix;
                    
                    if ((ch2 & 0xC0) != 0x080) {
                        reportError(ch2);
                    }
                    ch = (ch << 6) | (ch2 & 0x3F);
                    
                    // Once again, some of validation deferred to name char validator
                    if (needed > 1) {
                        ch2 = quads[ix >> 2];
                        byteIx = (ix & 3);
                        ch2 = (ch2 >> ((3 - byteIx) << 3));
                        ++ix;
                        
                        if ((ch2 & 0xC0) != 0x080) {
                            reportError(ch2);
                        }
                        ch = (ch << 6) | (ch2 & 0x3F);
                        if (needed > 2) { // 4 bytes? (need surrogates on output)
                            ch2 = quads[ix >> 2];
                            byteIx = (ix & 3);
                            ch2 = (ch2 >> ((3 - byteIx) << 3));
                            ++ix;
                            if ((ch2 & 0xC0) != 0x080) {
                                reportError(ch2 & 0xFF);
                            }
                            ch = (ch << 6) | (ch2 & 0x3F);
                        }
                    }
                    ok = XmlChars.is10NameChar(ch);
                    if (needed > 2) { // surrogate pair? once again, let's output one here, one later on
                        ch -= 0x10000; // to normalize it starting with 0x0
                        if (cix >= cbuf.length) {
                            mNameBuffer = cbuf = DataUtil.growArrayBy(cbuf, cbuf.length);
                        }
                        cbuf[cix++] = (char) (0xD800 + (ch >> 10));
                        ch = 0xDC00 | (ch & 0x03FF);
                    }
                }
            }
            if (!ok) {
                reportError(cix);
            }
            if (cix >= cbuf.length) {
                mNameBuffer = cbuf = DataUtil.growArrayBy(cbuf, cbuf.length);
            }
            cbuf[cix++] = (char) ch;
        }

        /* Ok. Now we have the character array, and can construct the
         * String (as well as check proper composition of semicolons
         * for ns-aware mode...)
         */
        String baseName = new String(cbuf, 0, cix);
        // And finally, unalign if necessary
        if (lastQuadBytes < 4) {
            quads[qlen-1] = lastQuad;
        }
        return mSymbols.addSymbol(hash, baseName, last_colon, quads, qlen);
    }

    private void loadMoreGuaranteed()
    {
        throw new IllegalStateException();
    }

    private int loadOne()
    {
        throw new IllegalStateException();
    }

    private void reportError(int arg)
    {
        throw new IllegalStateException();
    }

    private void reportError(String msg)
    {
        throw new IllegalStateException(msg);
    }

    private static byte[] readData(File f)
        throws IOException
    {
        int len = (int) f.length();
        byte[] data = new byte[len];
        int offset = 0;
        FileInputStream fis = new FileInputStream(f);
        
        while (len > 0) {
            int count = fis.read(data, offset, len-offset);
            offset += count;
            len -= count;
        }
        fis.close();
        return data;
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... [input file]");
            System.exit(1);
        }
        byte[] data = readData(new File(args[0]));
        int len = data.length;
        int repCount = 1;

        int THRESHOLD = 10 * 1000 * 1000;

        if (len < THRESHOLD) {
            repCount = (THRESHOLD / len);
        }
        //if (repCount > 2) { repCount /= 2; }

        System.out.println("Ok, read in test data, "+len+" bytes; using "+repCount+" repetitions");
        new TestPNamePerf(data, repCount).test();
    }
}
