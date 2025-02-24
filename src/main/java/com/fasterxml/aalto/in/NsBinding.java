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

import javax.xml.XMLConstants;

/**
 * This is a simple container class to use for encapsulating dynamic
 * (current) binding from a prefix, to a namespace URI (or missing
 * binding by URI being null). It is often included in
 * {@link PName} object.
 */
final class NsBinding
{
    // // // Couple of immutable bindings:

    /**
     * Default binding for prefix "xml"
     */
    public final static NsBinding XML_BINDING = new NsBinding
        ("xml", XMLConstants.XML_NS_URI, null);

    /**
     * Default binding for prefix "xmlns"
     */
    public final static NsBinding XMLNS_BINDING = new NsBinding
        ("xmlns", XMLConstants.XMLNS_ATTRIBUTE_NS_URI, null);

    public final String mPrefix;
    public String mURI;

    public NsBinding(String prefix)
    {
        /* For internal consistency, it is illegal to create multiple
         * bindings for "xml" or "xmlns". These should be catched earlier,
         * but just in case they aren't let's verify them here.
         */
        if ("xml".equals(prefix) || "xmlns".equals(prefix)) {
            throw new RuntimeException("Trying to create non-singleton binding for ns prefix '"+prefix+"'");
        }
        mPrefix = prefix;
        mURI = null;
    }

    public final static NsBinding createDefaultNs() {
        return new NsBinding(XMLConstants.DEFAULT_NS_PREFIX);
    }

    private NsBinding(String prefix, String uri, Object DUMMY)
    {
        mPrefix = prefix;
        mURI = uri;
    }

    public boolean isImmutable()
    {
        return (this == XML_BINDING || this == XMLNS_BINDING);
    }
}
