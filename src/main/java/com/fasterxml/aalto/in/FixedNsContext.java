package com.fasterxml.aalto.in;

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.codehaus.stax2.ri.EmptyIterator;
import org.codehaus.stax2.ri.SingletonIterator;

/**
 * Non-transient implementation of {@link NamespaceContext}.
 */
public final class FixedNsContext
    implements NamespaceContext
{
    public static FixedNsContext newEmptyContext() {
        return new FixedNsContext(null, new String[0]);
    }

    /*
    ////////////////////////////////////////////////////////
    // Persisted namespace information
    ////////////////////////////////////////////////////////
     */

    /**
     * We will keep a reference to the last namespace declaration
     * in effect at point when this instance was created. This is used
     * for lazy invalidation of instances: if last declaration for
     * an instance differs from the last seen by the reader, a new
     * context must be created.
     */
    protected final NsDeclaration _lastDeclaration;

    /**
     * Array that contains prefix/namespace-uri pairs, ordered from the
     * most recent declaration to older ones. Array is always exactly
     * sized so there are no empty entries at the end.
     */
    protected final String[] _declarationData;

    /**
     * Temporary List used for constructing compact namespace binding
     * information that we will actually use.
     */
    protected ArrayList<String> _tmpDecl = null;

    private FixedNsContext(NsDeclaration lastDecl, String[] declData)
    {
        _lastDeclaration = lastDecl;
        _declarationData = declData;
    }

    /**
     * Method called to either reuse this context or construct a new
     * one. Reuse is ok if the currently active last declaration has
     * not changed since time this instance was created.
     */
    public FixedNsContext reuseOrCreate(final NsDeclaration currLastDecl)
    {
        if (currLastDecl == _lastDeclaration) {
            return this;
        }
        if (_tmpDecl == null) {
            _tmpDecl = new ArrayList<String>();
        } else {
            _tmpDecl.clear();
        }
        for (NsDeclaration curr = currLastDecl; curr != null; curr = curr.getPrev()) {
            _tmpDecl.add(curr.getPrefix());
            _tmpDecl.add(curr.getCurrNsURI());
        }
        String[] data = _tmpDecl.toArray(new String[_tmpDecl.size()]);
        return new FixedNsContext(currLastDecl, data);
    }

    /*
    /////////////////////////////////////////////
    // NamespaceContext API
    /////////////////////////////////////////////
     */

    @Override
    public final String getNamespaceURI(String prefix)
    {
        /* First the known offenders; invalid args, 2 predefined xml
         * namespace prefixes
         */
        if (prefix == null) {
            throw new IllegalArgumentException("Null prefix not allowed");
        }
        if (prefix.length() > 0) {
            if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
                return XMLConstants.XML_NS_URI;
            }
            if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
            }
        }
        // here we count on never having null prefixes, just ""
        String[] ns = _declarationData;
        for (int i = 0, len = ns.length; i < len; i += 2) {
            if (prefix.equals(ns[i])) {
                return ns[i+1];
            }
        }
        return null;
    }

    @Override
    public final String getPrefix(String nsURI)
    {
        /* First the known offenders; invalid args, 2 predefined xml
         * namespace prefixes
         */
        if (nsURI == null || nsURI.length() == 0) {
            throw new IllegalArgumentException("Illegal to pass null/empty prefix as argument.");
        }
        if (nsURI.equals(XMLConstants.XML_NS_URI)) {
            return XMLConstants.XML_NS_PREFIX;
        }
        if (nsURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return XMLConstants.XMLNS_ATTRIBUTE;
        }
        String[] ns = _declarationData;

        main_loop:
        for (int i = 1, len = ns.length; i < len; i += 2) {
            if (nsURI.equals(ns[i])) {
                // may still suffer from masking, let's check
                String prefix = ns[i-1];
                for (int j = i+1; j < len; j += 2) {
                    // Prefixes are interned, can do straight equality check
                    if (ns[j] == prefix) {
                        continue main_loop; // was masked!
                    }
                }
                return ns[i-1];
            }
        }
        return null;

    }

    @Override
    public final Iterator<?> getPrefixes(String nsURI)
    {
        /* First the known offenders; invalid args, 2 predefined xml
         * namespace prefixes
         */
        if (nsURI == null || nsURI.length() == 0) {
            throw new IllegalArgumentException("Illegal to pass null/empty prefix as argument.");
        }
        if (nsURI.equals(XMLConstants.XML_NS_URI)) {
            return SingletonIterator.create(XMLConstants.XML_NS_PREFIX);
        }
        if (nsURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return SingletonIterator.create(XMLConstants.XMLNS_ATTRIBUTE);
        }

        String[] ns = _declarationData;

        String first = null;
        ArrayList<String> all = null;

        main_loop:
        for (int i = 1, len = ns.length; i < len; i += 2) {
            String currNS = ns[i];
            if (currNS == nsURI || currNS.equals(nsURI)) {
                // Need to ensure no masking occurs...
                String prefix = ns[i-1];
                for (int j = i+1; j < len; j += 2) {
                    // Prefixes are interned, can do straight equality check
                    if (ns[j] == prefix) {
                        continue main_loop; // was masked, need to ignore
                    }
                }
                if (first == null) {
                    first = prefix;
                } else {
                    if (all == null) {
                        all = new ArrayList<String>();
                        all.add(first);
                    }
                    all.add(prefix);
                }
            }
        }
        if (all != null) {
            return all.iterator();
        }
        if (first != null) {
            return SingletonIterator.create(first);
        }
        return EmptyIterator.getInstance();
    }

    /*
    /////////////////////////////////////////////
    // Other methods
    /////////////////////////////////////////////
     */

    @Override
        public String toString()
    {
        if (_lastDeclaration == null && _declarationData.length == 0) {
            return "[EMPTY non-transient NsContext]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0, len = _declarationData.length; i < len; i += 2) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(_declarationData[i]).append("\"->\"");
            sb.append(_declarationData[i+1]).append('"');
        }
        sb.append(']');
        return sb.toString();
    }
}
