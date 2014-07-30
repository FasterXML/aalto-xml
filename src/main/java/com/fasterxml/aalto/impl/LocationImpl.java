package com.fasterxml.aalto.impl;

import org.codehaus.stax2.XMLStreamLocation2;

/**
 * Basic implementation of {@link XMLStreamLocation2}, used by stream
 * readers and writers.
 */
public class LocationImpl
    implements XMLStreamLocation2
{
    private final static LocationImpl sEmptyLocation = new LocationImpl("", "", -1, -1, -1);

    final protected String mPublicId, mSystemId;
    
    final protected int mCharOffset;
    final protected int mCol, mRow;

    transient protected  String mDesc = null;

    public LocationImpl(String pubId, String sysId,
                        int charOffset, int row, int col)
    {
        mPublicId = pubId;
        mSystemId = sysId;
        /* Overflow? Can obviously only handle limited range of overflows,
         * but let's do that at least?
         */
        mCharOffset = (charOffset < 0) ? Integer.MAX_VALUE : charOffset;
        mCol = col;
        mRow = row;
    }

    /**
     * Helper method that will adjust given internal zero-based values
     * to 1-based values that should be externally visible.
     */
    public static LocationImpl fromZeroBased(String pubId, String sysId,
            long rawOffset, int rawRow, int rawCol)
    {
        // row, column are 1-based, offset 0-based
        // TODO: handle overflow
        int offset = (int) rawOffset;
        return new LocationImpl(pubId, sysId, offset, rawRow+1, rawCol+1);
    }

    public static LocationImpl getEmptyLocation() {
        return sEmptyLocation;
    }

    @Override
    public int getCharacterOffset() { return mCharOffset; }

    @Override
    public int getColumnNumber() { return mCol; }

    @Override
    public int getLineNumber() { return mRow; }
    
    @Override
    public String getPublicId() { return mPublicId; }

    @Override
    public String getSystemId() { return mSystemId; }

    /*
    /**********************************************************************
    /* Stax2 API
    /**********************************************************************
     */

    @Override
    public XMLStreamLocation2 getContext() {
        // !!! TBI
        return null;
    }

    /*
    /**********************************************************************
    /* Overridden standard methods
    /**********************************************************************
     */

    @Override
    public String toString()
    {
        if (mDesc == null) {
            StringBuffer sb = new StringBuffer(100);
            appendDesc(sb);
            mDesc = sb.toString();
        }
        return mDesc;
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    private void appendDesc(StringBuffer sb)
    {
        String srcId;

        if (mSystemId != null) {
            sb.append("[row,col,system-id]: ");
            srcId = mSystemId;
        } else if (mPublicId != null) {
            sb.append("[row,col,public-id]: ");
            srcId = mPublicId;
        } else {
            sb.append("[row,col {unknown-source}]: ");
            srcId = null;
        }
        sb.append('[');
        sb.append(mRow);
        sb.append(',');
        sb.append(mCol);

        if (srcId != null) {
            sb.append(',');
            sb.append('"');
            sb.append(srcId);
            sb.append('"');
        }
        sb.append(']');
    }
}
