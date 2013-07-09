package test;

import java.io.*;

public final class TestScannerPerf
{
    final static int INT_AMP = '&';
    final static int INT_LT = '<';
    final static int INT_RBRACKET = ']';

    final static int INT_SPACE = ' ';
    final static int INT_TAB = '\t';
    final static int INT_CR = '\r';
    final static int INT_LF = '\n';

    final static byte BYTE_LF = (byte) '\n';

    final static byte BYTE_NULL = (byte)0;

    final int mRepCount;

    int mTmpChar = 0;

    final byte[] mData;

    final byte[] mInputBuffer = new byte[4000];

    final char[] mOutputBuffer = new char[2000];

    final static int MB_CODE_BASE = 5;

    final static int[] CHAR_TYPES = new int[256];
    static {
        int code;
        for (int i = 128; i < 256; ++i) {
            int c = i;
            if ((c & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
                code = MB_CODE_BASE + 1;
            } else if ((c & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
                code = MB_CODE_BASE + 2;
            } else if ((c & 0xF8) == 0xF0) {
                // 4 bytes; double-char with surrogates and all...
                code = MB_CODE_BASE + 3;
            } else {
                code = 1;
            }
            CHAR_TYPES[c] = code;
        }
        for (int i = 0; i < 32; ++i) {
            CHAR_TYPES[i] = 1; // invalid white space
        }
        CHAR_TYPES['\r'] = 2;
        CHAR_TYPES['\n'] = 2;
        CHAR_TYPES['\t'] = 0; // no processing needed
        CHAR_TYPES['<'] = 3;
        CHAR_TYPES['&'] = 4;
        CHAR_TYPES[']'] = 5;
    }

    InputStream mIn;

    int mLineNr;
    int mByteCount;
    int mTagCount;
    int mEntityCount;
    int mBracketCount;

    int mInputPtr;

    int mInputLen;

    int mTmpType = 0;

    public TestScannerPerf(byte[] data, int repCount)
    {
        mData = data;
        mRepCount = repCount;
    }

    public void test()
        throws IOException
    {
       int round = 0;
       mIn = new ByteArrayInputStream(mData);

       for (; true; ++round) {
           long now = System.currentTimeMillis();
           String msg = "[null]";
           int total = 0;

           final int TYPES = 3;

           if ((round % TYPES) == 0) {
               System.out.println();
           }

           for (int i = 0; i < mRepCount; ++i) {
               mIn.reset();

               mLineNr = 0;
               mTagCount = 0;
               mByteCount = 0;

               switch (round % TYPES) {
               case 0:
                   msg = "[Scanner-code]";
                   total += testScannerCode();
                   break;
               case 1:
                   msg = "[Scanner-int-arr]";
                   total += testScannerInts();
                   break;
               case 2:
                   msg = "[Scanner-int-arr2]";
                   total += testScannerInts2();
                   break;
               default:
                   throw new Error("Unexpected round, #"+i);
               }
           }

           now = System.currentTimeMillis() - now;
           System.out.println(msg+" -> "+now+" msecs (total "+total
                              +", byte count 0x"+Integer.toHexString(mByteCount)+")");

           try { Thread.sleep(200L); } catch (Exception e) { }
           System.gc();
           try { Thread.sleep(200L); } catch (Exception e) { }
       }
    }

    private int testScannerCode()
        throws IOException
    {
        final char[] outBuf = mOutputBuffer;
        int outPtr = 0;
        int c = 0;

        mInputLen = 0;
        mInputPtr = 0;

        main_loop:
        while (true) {
            // Next thing: let's get the first byte:
            int ptr = mInputPtr;

            ascii_loop:
            while (true) {
                if (ptr >= mInputLen) {
                    if (!loadMoreBytes()) {
                        break main_loop;
                    }
                    ptr = mInputPtr;
                }
                c = (int) mInputBuffer[ptr++];
                if (c <= INT_RBRACKET) {
                //if (c <= INT_LT) {
                    if (c < 0) {
                        break ascii_loop;
                    }
                    if (c < INT_SPACE) {
                        if (c == INT_CR) {
                            ++mLineNr;
                        } else if (c == INT_LF) {
                            ++mLineNr;
                        } else if (c != INT_TAB) {
                            throw new Error();
                        }
                    } else if (c == INT_LT) {
                        ++mTagCount;
                    } else if (c == INT_AMP) {
                        ++mEntityCount;
                    } else if (c == INT_RBRACKET) {
                        ++mBracketCount;
                    }
                }
                // !!! TODO: xml1.1, 0x7F?
                if (outPtr >= outBuf.length) {
                    outPtr = 0;
                }
                outBuf[outPtr++] = (char) c;
            }

            c = decodeMultiByteChar(c, ptr);
            if (c < 0) { // surrogate pair
                if (outPtr >= outBuf.length) {
                    outPtr = 0;
                }
                c = -c;
                // Let's add first part right away:
                outBuf[outPtr++] = (char) (0xD800 | (c >> 10));
                c = 0xDC00 | (c & 0x3FF);
                // And let the other char output in general loop
            }
            if (outPtr >= outBuf.length) {
                outPtr = 0;
            }
            outBuf[outPtr++] = (char) c;
        }
        return mByteCount;
    }

    private int testScannerInts()
        throws IOException
    {
        int outPtr = 0;
        int c = 0;
        final int[] TYPES = CHAR_TYPES;

        final byte[] inputBuffer = mInputBuffer;
        final char[] outputBuffer = mOutputBuffer;

        mInputLen = 0;
        mInputPtr = 0;

        main_loop:
        while (true) {
            // Next thing: let's get the first byte:
            int ptr = mInputPtr;

            ascii_loop:
            while (true) {
                if (ptr >= mInputLen) {
                    if (!loadMoreBytes()) {
                        break main_loop;
                    }
                    ptr = mInputPtr;
                }
                c = (int) inputBuffer[ptr++] & 0xFF;
                int type = TYPES[c];
                if (type != 0) {
                    switch (type) {                        
                    case 1:
                        throw new Error("Invalid white space");
                    case 2:
                        if (c == INT_CR) {
                            ++mLineNr;
                        } else if (c == INT_LF) {
                            ++mLineNr;
                        }
                        break;
                    case 3:
                        ++mTagCount;
                        break;
                    case 4:
                        ++mEntityCount;
                        break;
                    case 5:
                        ++mBracketCount;
                        break;
                    case 6: // 2 bytes
                    case 7: // 3 bytes
                    case 8: // 4 bytes
                        break ascii_loop;
                    default:
                        throw new Error();
                    }
                }
                if (outPtr >= outputBuffer.length) {
                    outPtr = 0;
                }
                outputBuffer[outPtr++] = (char) c;
            }

            c = decodeMultiByteChar(c, ptr);
            if (c < 0) { // surrogate pair
                if (outPtr >= outputBuffer.length) {
                    outPtr = 0;
                }
                c = -c;
                // Let's add first part right away:
                outputBuffer[outPtr++] = (char) (0xD800 | (c >> 10));
                c = 0xDC00 | (c & 0x3FF);
                // And let the other char output in general loop
            }
            if (outPtr >= outputBuffer.length) {
                outPtr = 0;
            }
            outputBuffer[outPtr++] = (char) c;
        }
        return mByteCount;
    }

    private int testScannerInts2()
        throws IOException
    {
        int outPtr = 0;
        int c = 0;
        final int[] TYPES = CHAR_TYPES;

        final byte[] inputBuffer = mInputBuffer;
        char[] outputBuffer = mOutputBuffer;

        mInputLen = 0;
        mInputPtr = 0;

        main_loop:
        while (true) {
            // Next thing: let's get the first byte:

            ascii_loop:
            while (true) {
                int ptr = mInputPtr;
                if (ptr >= mInputLen) {
                    if (!loadMoreBytes()) {
                        break main_loop;
                    }
                    ptr = mInputPtr;
                }
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = mOutputBuffer;
                    outPtr = 0;
                }
                int max = mInputLen;
                {
                    int max2 = ptr + (outputBuffer.length - outPtr);
                    if (max2 < max) {
                        max = max2;
                    }
                }
                while (ptr < max) {
                    c = (int) inputBuffer[ptr++] & 0xFF;
                    if (TYPES[c] != 0) {
                        mInputPtr = ptr;
                        break ascii_loop;
                    }
                    outputBuffer[outPtr++] = (char) c;
                }
                mInputPtr = ptr;
            }

            switch (TYPES[c]) {
            case 1:
                throw new Error("Invalid white space");
            case 2:
                if (c == INT_CR) {
                    ++mLineNr;
                } else if (c == INT_LF) {
                    ++mLineNr;
                }
                break;
            case 3:
                ++mTagCount;
                break;
            case 4:
                // should expand entity
                ++mEntityCount;
                break;
            case 5:
                ++mBracketCount;
                break;
            case 6: // 2 bytes
                c = decodeMultiByteChar(c, mInputPtr);
                break;
            case 7: // 3 bytes
                c = decodeMultiByteChar(c, mInputPtr);
                break;
            case 8: // 4 bytes
                {
                    c = decodeMultiByteChar(c, mInputPtr);
                    if (outPtr >= outputBuffer.length) {
                        outputBuffer = mOutputBuffer;
                        outPtr = 0;
                    }
                    outputBuffer[outPtr++] = (char) (0xD800 | (c >> 10));
                    c = 0xDC00 | (c & 0x3FF);
                }
                break;
            default:
                throw new Error();
            }
            
            if (outPtr >= outputBuffer.length) {
                outputBuffer = mOutputBuffer;
                outPtr = 0;
            }
            outputBuffer[outPtr++] = (char) c;
        }
        return mByteCount;
    }

    /*
    private final int decode(int ptr, int c, int type)
        throws IOException
    {
        switch (type) {
        case 1:
            throw new Error("Invalid white space");
        case 2:
            if (c == INT_CR) {
                ++mLineNr;
            } else if (c == INT_LF) {
                ++mLineNr;
            }
            break;
        case 3:
            ++mTagCount;
            break;
        case 4:
            // should expand entity
            ++mEntityCount;
            break;
        case 5:
            ++mBracketCount;
            break;
        case 6: // 2 bytes
        case 7: // 3 bytes
        case 8: // 4 bytes
            c = decodeMultiByteChar(c, ptr);
            break;
        default:
            throw new Error();
        }
        mInputPtr = ptr;
        return c;
    }
    */

    private final boolean loadMoreBytes()
        throws IOException
    {
        mByteCount += mInputLen;
        mInputPtr = 0;
        int count = mIn.read(mInputBuffer);
        if (count < 0) {
            mInputLen = 0;
            return false;
        }
        mInputLen = count;
        return true;
    }

    private final void loadMoreBytesGuaranteed()
        throws IOException
    {
        if (!loadMoreBytes()) {
            throw new Error();
        }
    }

    /*
    private final void markLF()
    {
        ++mLineNr;
    }

    private final void markLF(int pos)
    {
        ++mLineNr;
    }

    private final int handleEntityInText()
    {
        ++mEntityCount;
        return '&';
    }
    */

    private final int decodeMultiByteChar(int c, int ptr)
        throws IOException
    {
        int needed;

        if ((c & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
            c &= 0x1F;
            needed = 1;
        } else if ((c & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
            c &= 0x0F;
            needed = 2;
        } else if ((c & 0xF8) == 0xF0) {
            // 4 bytes; double-char with surrogates and all...
            c &= 0x07;
            needed = 3;
        } else {
            throw new Error("Unexpected multi-byte first byte 0x"+Integer.toHexString(c));
        }

        if (ptr >= mInputLen) { // 2nd byte
            loadMoreBytesGuaranteed();
            ptr = mInputPtr;
        }
        int d = (int) mInputBuffer[ptr++];
        if ((d & 0xC0) != 0x080) {
            throw new Error();
        }
        c = (c << 6) | (d & 0x3F);
        
        if (needed > 1) { // needed == 1 means 2 bytes total
            if (ptr >= mInputLen) {
                loadMoreBytesGuaranteed();
                ptr = mInputPtr;
            }
            d = (int) mInputBuffer[ptr++];
            if ((d & 0xC0) != 0x080) {
                throw new Error();
            }
            c = (c << 6) | (d & 0x3F);
            if (needed > 2) { // 4 bytes? (need surrogates)
                if (ptr >= mInputLen) {
                    loadMoreBytesGuaranteed();
                    ptr = mInputPtr;
                }
                d = (int) mInputBuffer[ptr++];
                if ((d & 0xC0) != 0x080) {
                    throw new Error();
                }
                c = (c << 6) | (d & 0x3F);
                /* Need to signal such pair differently (to make comparison
                 * easier)
                 */
                return -c;
            }
        }
        mInputPtr = ptr;
        return c;
    }

    /*
    private final int decodeMultiByteChar(int c, int type, int ptr)
        throws IOException
    {
        // let's see how many add'l bytes are needed
        type -= MB_CODE_BASE;
        c &= (0x3F >> type); // 1f/0f/07 (for 2/3/4 bytes)
        if (ptr >= mInputEnd) { // 2nd byte
            loadMoreBytesGuaranteed();
            ptr = mInputPtr;
        }
        int d = (int) mInputBuffer[ptr++];
        if ((d & 0xC0) != 0x080) {
            throw new Error();
        }
        c = (c << 6) | (d & 0x3F);
        
        if (type > 1) { // needed == 1 means 2 bytes total
            if (ptr >= mInputEnd) {
                loadMoreBytesGuaranteed();
                ptr = mInputPtr;
            }
            d = (int) mInputBuffer[ptr++];
            if ((d & 0xC0) != 0x080) {
                throw new Error();
            }
            c = (c << 6) | (d & 0x3F);
            if (type > 2) { // 4 bytes? (need surrogates)
                if (ptr >= mInputEnd) {
                    loadMoreBytesGuaranteed();
                    ptr = mInputPtr;
                }
                d = (int) mInputBuffer[ptr++];
                if ((d & 0xC0) != 0x080) {
                    throw new Error();
                }
                c = (c << 6) | (d & 0x3F);
                // Need to signal such pair differently (to make comparison
                // easier)
                return -c;
            }
        }
        mInputPtr = ptr;
        return c;
    }
    */

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
        throws IOException
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
        new TestScannerPerf(data, repCount).test();
    }
}
