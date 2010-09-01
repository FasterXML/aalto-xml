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

import javax.xml.namespace.QName;

/**
 * Prefixed Name is similar to {@link javax.xml.namespace.QName}
 * (qualified name), but
 * only contains information about local name optionally prefixed by
 * a prefix and colon, without namespace binding information.
 */
public abstract class PName
{
    protected final String mPrefixedName;
    protected final String mPrefix;
    protected final String mLocalName;

    /**
     * Binding of this qualified/prefixed name. Null if there is no
     * prefix; in which case name is either bound to the default namespace
     * (when element name), or no namespace (when other name, like attribute)
     */
    protected NsBinding mNsBinding = null;

    /*
    //////////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////////
     */

    protected PName(String pname, String prefix, String ln)
    {
        mPrefixedName = pname;
        mPrefix = prefix;
        mLocalName = ln;
    }

    public abstract PName createBoundName(NsBinding nsb);

    /*
      // 26-Jun-2006, TSa: Doesn't seem to be needed any more...
    protected void bind(NsBinding nsb)
    {
        if (mNsBinding != null) { // !!! Temporary assertion
            throw new RuntimeException("Trying to re-set binding (for '"+getPrefixedName()+"'), was: "+mNsBinding+", new: "+nsb);
        }
        mNsBinding = nsb;
    }
    */

    /*
    //////////////////////////////////////////////////////////
    // Accessors
    //////////////////////////////////////////////////////////
     */

    public final String getPrefixedName() { return mPrefixedName; }

    /**
     * @return Prefix of this name, if it has one; null if not.
     */
    public final String getPrefix() { return mPrefix; }
    public final String getLocalName() { return mLocalName; }

    public boolean hasPrefix() { return mPrefix != null; }

    public final NsBinding getNsBinding() { return mNsBinding; }

    public final String getNsUri() {
        return (mNsBinding == null) ? null : mNsBinding.mURI;
    }

    public final QName constructQName()
    {
        String pr = mPrefix;
        String uri = (mNsBinding == null) ? null : mNsBinding.mURI;
        // Stupid QName: some impls barf on nulls...
        return new QName((uri == null) ? "" : uri,
                         mLocalName,
                         (pr == null) ? "" : pr);
    }

    /**
     * Method called to construct a QName representation of elemented
     * represented by this PName. Because of namespace defaulting,
     * current default namespace binding also needs to be passed
     * (since only explicit ones get bound to PName instances).
     */
    public final QName constructQName(NsBinding defaultNs)
    {
        String pr = mPrefix;
        if (pr == null) { // QName barfs on nulls
            pr = "";
        }
        // Do we have a local binding?
        if (mNsBinding != null) {
            String uri = mNsBinding.mURI;
            if (uri != null) { // yup
                return new QName(uri,  mLocalName, pr);
            }
        }
        // Nope. Default ns?
        String uri = defaultNs.mURI;
        return new QName((uri == null) ? "" : uri,  mLocalName, pr);
    }

    /*
    //////////////////////////////////////////////////////////
    // Namespace binding
    //////////////////////////////////////////////////////////
     */

    /**
     * @return True if the name has no binding object, but will need
     *    one (has prefix)
     */
    public final boolean needsBinding() {
        return (mPrefix != null) && (mNsBinding == null);
    }

    /**
     * @return True if the name as described either has no prefix (either
     *    belongs to the default ns [elems], or to 'no namespace' [attrs]),
     *    or has a prefix that is bound currently. False if name has a prefix
     *    that is unbound.
     */
    public final boolean isBound()
    {
        return (mNsBinding == null) || (mNsBinding.mURI != null);
    }

    /**
     * Method that compares two bound PNames for semantic equality. This
     * means that the local name, as well as bound URI are compared.
     */
    public final boolean boundEquals(PName other)
    {
        if (other == null || other.mLocalName != mLocalName) {
            return false;
        }
        // Let's assume URIs are canonicalized at least on per-doc basis?
        return other.getNsUri() == getNsUri();
    }

    public final boolean unboundEquals(PName other)
    {
        return (other.mPrefixedName == mPrefixedName);
    }

    public final boolean boundEquals(String nsUri, String ln)
    {
        if (!mLocalName.equals(ln)) {
            return false;
        }
        String thisUri = getNsUri();
        if (nsUri == null || nsUri.length() == 0) {
            return (thisUri == null);
        }
        return nsUri.equals(thisUri);
    }

    public final int unboundHashCode()
    {
        return mPrefixedName.hashCode();
    }

    public final int boundHashCode()
    {
        /* How often do we have same local name, but differing URI?
         * Probably not often... thus, let's only use local name's hash.
         */
        return mLocalName.hashCode();
    }

    public static int boundHashCode(String nsURI, String localName)
    {
        return localName.hashCode();
    }

    /*
    //////////////////////////////////////////////////////////
    // Redefined standard methods
    //////////////////////////////////////////////////////////
     */

    public final String toString() { return mPrefixedName; }

    public final boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PName)) {
            return false;
        }
        PName other = (PName) o;
        /* Only prefix and ln are interned, not the full prefixed name...
         * so let's compare separately. Can use identity comparison with
         * those though:
         */
        return (other.mPrefix == mPrefix) && (other.mLocalName == mLocalName);
    }

    /*
    //////////////////////////////////////////////////////////
    // Methods for package/core parser
    //////////////////////////////////////////////////////////
     */

    /* Note: These 3 methods really should be in the byte-based sub-class...
     * but there are performance reasons to keep there, to remove
     * some otherwise necessary casts.
     */

    public abstract int sizeInQuads();

    public abstract int getFirstQuad();

    public abstract int getQuad(int index);

    public abstract int getLastQuad();
}
