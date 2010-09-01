/* Woodstox Lite ("wool") XML processor
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

package com.fasterxml.aalto.util;

/**
 * This is a small utility class, whose main functionality is to allow
 * simple reuse of raw byte/char buffers. It is usually used through
 * <code>ThreadLocal</code> member of the owning class pointing to
 * instance of this class through a <code>SoftReference</code>. The
 * end result is a low-overhead GC-cleanable recycling: hopefully
 * ideal for use by stream readers.
 *<p>
 * Regarding implementation: the key design goal is simplicity; and to
 * that end, different types of buffers are handled separately. While
 * code may look inelegant as a result (would be cleaner to just
 * have generic char[]/byte[] buffer accessors), benefit is that
 * no data structures are needed, just simple references. As long
 * as usage pattern is well known (which it is, for stream readers)
 * this should be highly optimal and robust implementation.
 */
public final class BufferRecycler
{
    private char[] mSmallCBuffer = null; // temp buffers
    private char[] mMediumCBuffer = null; // text collector
    private char[] mFullCBuffer = null; // for actual parsing buffer

    private byte[] mFullBBuffer = null;

    public BufferRecycler() { }

    // // // Char buffers:

    // // Small buffers, for temporary parsing

    public char[] getSmallCBuffer(int minSize)
    {
        char[] result = null;
        if (mSmallCBuffer != null && mSmallCBuffer.length >= minSize) {
            result = mSmallCBuffer;
            mSmallCBuffer = null;
        }
//System.err.println("DEBUG: Alloc CSmall: "+result);
        return result;
    }

    public void returnSmallCBuffer(char[] buffer)
    {
//System.err.println("DEBUG: Return CSmall ("+buffer.length+"): "+buffer);
        mSmallCBuffer = buffer;
    }

    // // Medium buffers, for text output collection

    public char[] getMediumCBuffer(int minSize)
    {
        char[] result = null;
        if (mMediumCBuffer != null && mMediumCBuffer.length >= minSize) {
            result = mMediumCBuffer;
            mMediumCBuffer = null;
        }
//System.err.println("DEBUG: Alloc CMed: "+result);
        return result;
    }

    public void returnMediumCBuffer(char[] buffer)
    {
        mMediumCBuffer = buffer;
//System.err.println("DEBUG: Return CMed ("+buffer.length+"): "+buffer);
    }

    // // Full buffers, for parser buffering

    public char[] getFullCBuffer(int minSize)
    {
        char[] result = null;
        if (mFullCBuffer != null && mFullCBuffer.length >= minSize) {
            result = mFullCBuffer;
            mFullCBuffer = null;
        }
//System.err.println("DEBUG: Alloc CFull: "+result);
        return result;
    }

    public void returnFullCBuffer(char[] buffer)
    {
        mFullCBuffer = buffer;
//System.err.println("DEBUG: Return CFull ("+buffer.length+"): "+buffer);
    }

    // // // Byte buffers:

    // // Full byte buffers, for byte->char conversion (Readers)

    public byte[] getFullBBuffer(int minSize)
    {
        byte[] result = null;
        if (mFullBBuffer != null && mFullBBuffer.length >= minSize) {
            result = mFullBBuffer;
            mFullBBuffer = null;
        }
//System.err.println("DEBUG: Alloc BFull: "+result);
        return result;
    }

    public void returnFullBBuffer(byte[] buffer)
    {
        mFullBBuffer = buffer;
//System.err.println("DEBUG: Return BFull ("+buffer.length+"): "+buffer);
    }
}
