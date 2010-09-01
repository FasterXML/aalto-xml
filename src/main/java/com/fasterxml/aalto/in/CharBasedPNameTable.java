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

package com.fasterxml.aalto.in;

import com.fasterxml.aalto.util.NameTable;

/**
 * This is a symbol table implementation used for storing byte-based
 * <code>PNames</code>, specifically, instances of
 * ({@link PNameC}).
 */
public class CharBasedPNameTable
    extends NameTable
{
    final static int MIN_HASH_SIZE = 16;

    protected static final float DEFAULT_FILL_FACTOR = 0.75f;

    /*
    ////////////////////////////////////////
    // Actual symbol table data:
    ////////////////////////////////////////
     */

    /**
     * Primary matching symbols; it's expected most match occur from
     * here.
     */
    protected PNameC[] mSymbols;

    /**
     * Overflow buckets; if primary doesn't match, lookup is done
     * from here.
     *<p>
     * Note: Number of buckets is half of number of symbol entries, on
     * assumption there's less need for buckets.
     */
    protected Bucket[] mBuckets;

    /**
     * Current size (number of entries); needed to know if and when
     * rehash.
     */
    protected int mSize;

    /**
     * Limit that indicates maximum size this instance can hold before
     * it needs to be expanded and rehashed. Calculated using fill
     * factor passed in to constructor.
     */
    protected int mSizeThreshold;

    /**
     * Mask used to get index from hash values; equal to
     * <code>mBuckets.length - 1</code>, when mBuckets.length is
     * a power of two.
     */
    protected int mIndexMask;

    /*
    ////////////////////////////////////////
    // Information about concurrency
    ////////////////////////////////////////
     */

    /**
     * Flag that indicates if any changes have been made to the data;
     * used to both determine if bucket array needs to be copied when
     * (first) change is made, and potentially if updated bucket list
     * is to be resync'ed back to master instance.
     */
    protected boolean mDirty;

    /*
    ////////////////////////////////////////
    // Life-cycle:
    ////////////////////////////////////////
     */

    /**
     * Main method for constructing a master symbol table instance; will
     * be called by other public constructors.
     *
     * @param initialSize Minimum initial size for bucket array; internally
     *   will always use a power of two equal to or bigger than this value.
     */
    public CharBasedPNameTable(int initialSize)
    {
        // Let's set flags so no copying of buckets is needed:
        mDirty = true;

        // No point in requesting funny initial sizes...
        if (initialSize < 1) {
            throw new IllegalArgumentException("Can not use negative/zero initial size: "+initialSize);
        }
        {
            int currSize = MIN_HASH_SIZE;
            while (currSize < initialSize) {
                currSize += currSize;
            }
            initialSize = currSize;
        }

        mSymbols = new PNameC[initialSize];
        mBuckets = new Bucket[initialSize >> 1];
        // Mask is easy to calc for powers of two.
        mIndexMask = initialSize - 1;
        mSize = 0;

        /* Let's use 3/4 fill factor...
         */
        mSizeThreshold = (initialSize * 3 + 3) >> 2;
    }

    CharBasedPNameTable(CharBasedPNameTable parent)
    {
        mSymbols = parent.mSymbols;
        mBuckets = parent.mBuckets;
        mSize = parent.mSize;
        mSizeThreshold = parent.mSizeThreshold;
        mIndexMask = parent.mIndexMask;

        // Need to make copies of arrays, if/when adding new entries
        mDirty = false;
    }

    /**
     * Method that allows contents of child table to potentially be
     * "merged in" with contents of this symbol table.
     *<p>
     * Note that caller has to make sure symbol table passed in is
     * really a child or sibling of this symbol table.
     */
    public synchronized void mergeFromChild(CharBasedPNameTable child)
    {
        // Let's do a basic sanity check first:
        if (child.size() <= size()) { // nothing to add
            return;
        }

        // Okie dokie, let's get the data in!
        mSymbols = child.mSymbols;
        mBuckets = child.mBuckets;
        mSize = child.mSize;
        mSizeThreshold = child.mSizeThreshold;
        mIndexMask = child.mIndexMask;

        // Dirty flag... well, let's just clear it, to force copying just
        // in case. Shouldn't really matter, for master tables.
        mDirty = false;

        /* However, we have to mark child as dirty, so that it will not
         * be modifying arrays we "took over" (since child may have
         * returned an updated table before it stopped fully using
         * the SymbolTable: for example, it may still use it for
         * parsing PI targets in epilog)
         */
        child.mDirty = false;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, generic accessors:
    ////////////////////////////////////////////////////
     */

    public int size() { return mSize; }

    public boolean maybeDirty() { return mDirty; }

    /*
    ////////////////////////////////////////////////////
    // Public API, accessing symbols:
    ////////////////////////////////////////////////////
     */

    public PNameC findSymbol(char[] buffer, int start, int len, int hash)
    {
        int index = hash & mIndexMask;
        PNameC sym = mSymbols[index];

        // Optimal case; checking existing primary symbol for hash index:
        if (sym != null) {
            if (sym.equalsPName(buffer, start, len, hash)) {
                return sym;
            }
            // How about collision bucket?
            Bucket b = mBuckets[index >> 1];
            if (b != null) {
                sym = b.find(buffer, start, len, hash);
                if (sym != null) {
                    return sym;
                }
            }
        }
        return null;
    }

    public PNameC addSymbol(char[] buffer, int start, int len, int hash)
    {
        String newStr = new String(buffer, start, len).intern();
        PNameC pname = PNameC.construct(newStr, hash);
        int index = hash & mIndexMask;
        boolean primary;

        // First... let's see if we can add it without a collision.
        if (null == mSymbols[index]) {
            primary = true; // yup, we are good then
        } else {
            /* Otherwise, better check if we need to expand; after
             * which there may be a slot in primary hash?
             */
            if (mSize >= mSizeThreshold) {
                rehash();
                // Need to recalc hash index
                index = hash & mIndexMask;
                primary = (null == mSymbols[index]);
            } else { // nope: need a bucket
                primary = false;
            }
        }

        // good, can just update primary hash table
        if (!mDirty) { // need to do copy-on-write?
            copyArrays();
        }

        ++mSize;
        if (primary) {
            mSymbols[index] = pname;
        } else {
            // Ok, all right: need to add to a bucket
            int bix = (index >> 1);
            mBuckets[bix] = new Bucket(pname, mBuckets[bix]);
        }
        return pname;
    }

    /*
    //////////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////////
     */

    /**
     * Method called when copy-on-write is needed; generally when first
     * change is made to a derived symbol table.
     */
    private void copyArrays()
    {
        PNameC[] oldSyms = mSymbols;
        int size = oldSyms.length;
        mSymbols = new PNameC[size];
        System.arraycopy(oldSyms, 0, mSymbols, 0, size);
        Bucket[] oldBuckets = mBuckets;
        size = oldBuckets.length;
        mBuckets = new Bucket[size];
        System.arraycopy(oldBuckets, 0, mBuckets, 0, size);

        mDirty = true;
    }

    /**
     * Method called when size (number of entries) of symbol table grows
     * so big that load factor is exceeded. Since size has to remain
     * power of two, arrays will then always be doubled. Main work
     * is really redistributing old entries into new String/Bucket
     * entries.
     */
    private void rehash()
    {
        int size = mSymbols.length;
        int newSize = size + size;
        PNameC[] oldSyms = mSymbols;
        Bucket[] oldBuckets = mBuckets;
        mSymbols = new PNameC[newSize];
        mBuckets = new Bucket[newSize >> 1];
        // Let's update index mask, threshold, now (needed for rehashing)
        mIndexMask = newSize - 1;
        mSizeThreshold += mSizeThreshold;
        
        int count = 0; // let's do sanity check

        /* Need to do two loops, unfortunately, since spillover area is
         * only half the size:
         */
        for (int i = 0; i < size; ++i) {
            PNameC symbol = oldSyms[i];
            if (symbol != null) {
                ++count;
                int index = symbol.getCustomHash() & mIndexMask;
                if (mSymbols[index] == null) {
                    mSymbols[index] = symbol;
                } else {
                    int bix = index >> 1;
                    mBuckets[bix] = new Bucket(symbol, mBuckets[bix]);
                }
            }
        }

        size >>= 1;
        for (int i = 0; i < size; ++i) {
            Bucket b = oldBuckets[i];
            while (b != null) {
                ++count;
                PNameC symbol = b.getSymbol();
                int index = symbol.getCustomHash() & mIndexMask;
                if (mSymbols[index] == null) {
                    mSymbols[index] = symbol;
                } else {
                    int bix = index >> 1;
                    mBuckets[bix] = new Bucket(symbol, mBuckets[bix]);
                }
                b = b.getNext();
            }
        }

        if (count != mSize) {
            throw new Error("Internal error on SymbolTable.rehash(): had "+mSize+" entries; now have "+count+".");
        }
    }

    /*
    //////////////////////////////////////////////////////////
    // Bucket class
    //////////////////////////////////////////////////////////
     */

    /**
     * This class is a symbol table entry. Each entry acts as a node
     * in a linked list.
     */
    final static class Bucket
    {
        private final PNameC mSymbol;
        private final Bucket mNext;

        public Bucket(PNameC symbol, Bucket next)
        {
            mSymbol = symbol;
            mNext = next;
        }

        public PNameC getSymbol() { return mSymbol; }
        public Bucket getNext() { return mNext; }

        public PNameC find(char[] buf, int start, int len, int hash)
        {
            Bucket b = this;
            do {
                PNameC sym = b.mSymbol;
                if (sym.equalsPName(buf, start, len, hash)) {
                    return sym;
                }
                b = b.getNext();
            } while (b != null);
            return null;
        }
    }
}
