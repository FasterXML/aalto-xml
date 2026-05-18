package in;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;

import com.fasterxml.aalto.in.FixedNsContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link FixedNsContext}. The class only exposes a public
 * EMPTY_CONTEXT field and is otherwise populated via reuseOrCreate(), which
 * needs a real NsDeclaration. Easiest path: parse an XML document and ask
 * the Stax2 reader for getNonTransientNamespaceContext().
 */
public class TestFixedNsContext
    extends base.BaseTestCase
{
    private FixedNsContext contextAtRoot(String xml) throws XMLStreamException
    {
        XMLStreamReader2 sr = (XMLStreamReader2) getInputFactory().createXMLStreamReader(
                new ByteArrayInputStream(xml.getBytes()));
        // Advance to first element so namespace declarations have been processed.
        sr.nextTag();
        NamespaceContext nc = sr.getNonTransientNamespaceContext();
        // Drain reader so streams are closed cleanly.
        while (sr.hasNext()) sr.next();
        sr.close();
        return (FixedNsContext) nc;
    }

    @Test
    public void testEmptyContextLookups()
    {
        FixedNsContext ctx = FixedNsContext.EMPTY_CONTEXT;

        // Predefined xml/xmlns prefixes always resolve.
        assertEquals(XMLConstants.XML_NS_URI, ctx.getNamespaceURI(XMLConstants.XML_NS_PREFIX));
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, ctx.getNamespaceURI(XMLConstants.XMLNS_ATTRIBUTE));

        // Default ns prefix ("") on an empty context is unbound.
        assertNull(ctx.getNamespaceURI(""));
        // Unknown prefix is unbound.
        assertNull(ctx.getNamespaceURI("nope"));

        // Reverse lookup: the well-known URIs map back to canonical prefixes.
        assertEquals(XMLConstants.XML_NS_PREFIX, ctx.getPrefix(XMLConstants.XML_NS_URI));
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, ctx.getPrefix(XMLConstants.XMLNS_ATTRIBUTE_NS_URI));
        assertNull(ctx.getPrefix("urn:nobody"));

        // Iterators for the well-known URIs are singletons.
        Iterator<String> it = ctx.getPrefixes(XMLConstants.XML_NS_URI);
        assertTrue(it.hasNext());
        assertEquals(XMLConstants.XML_NS_PREFIX, it.next());
        assertFalse(it.hasNext());

        it = ctx.getPrefixes(XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, it.next());
        assertFalse(it.hasNext());

        // Unknown URI yields the empty iterator.
        it = ctx.getPrefixes("urn:nobody");
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("Expected NoSuchElementException from empty iterator");
        } catch (NoSuchElementException expected) {
        }

        // toString has a marker for EMPTY.
        assertEquals("[EMPTY non-transient NsContext]", ctx.toString());
    }

    @Test
    public void testNullPrefixRejected()
    {
        try {
            FixedNsContext.EMPTY_CONTEXT.getNamespaceURI(null);
            fail("Expected IAE for null prefix");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testNullOrEmptyUriRejected()
    {
        try {
            FixedNsContext.EMPTY_CONTEXT.getPrefix(null);
            fail("Expected IAE for null URI");
        } catch (IllegalArgumentException expected) {
        }
        try {
            FixedNsContext.EMPTY_CONTEXT.getPrefix("");
            fail("Expected IAE for empty URI");
        } catch (IllegalArgumentException expected) {
        }
        try {
            FixedNsContext.EMPTY_CONTEXT.getPrefixes(null);
            fail("Expected IAE for null URI");
        } catch (IllegalArgumentException expected) {
        }
        try {
            FixedNsContext.EMPTY_CONTEXT.getPrefixes("");
            fail("Expected IAE for empty URI");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testReuseOrCreateOnEmptyForSameDecl()
    {
        // Calling reuseOrCreate(null) on the empty context: the last-declaration
        // pointer is already null, so it should hand back the same instance.
        FixedNsContext ctx = FixedNsContext.EMPTY_CONTEXT.reuseOrCreate(null);
        assertSame(FixedNsContext.EMPTY_CONTEXT, ctx);
    }

    @Test
    public void testPopulatedContext() throws Exception
    {
        FixedNsContext ctx = contextAtRoot(
                "<root xmlns:a='urn:A' xmlns:b='urn:B'/>");

        // Forward lookups.
        assertEquals("urn:A", ctx.getNamespaceURI("a"));
        assertEquals("urn:B", ctx.getNamespaceURI("b"));
        assertNull(ctx.getNamespaceURI("c"));

        // Reverse lookups.
        assertEquals("a", ctx.getPrefix("urn:A"));
        assertEquals("b", ctx.getPrefix("urn:B"));
        assertNull(ctx.getPrefix("urn:nobody"));

        // Predefined prefixes/uris still work on populated contexts.
        assertEquals(XMLConstants.XML_NS_URI, ctx.getNamespaceURI(XMLConstants.XML_NS_PREFIX));
        assertEquals(XMLConstants.XML_NS_PREFIX, ctx.getPrefix(XMLConstants.XML_NS_URI));

        // getPrefixes for a singly-bound URI is a singleton iterator.
        Iterator<String> it = ctx.getPrefixes("urn:A");
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());

        // toString covers the non-empty branch.
        String s = ctx.toString();
        assertTrue(s.contains("\"a\"->\"urn:A\""), "toString must contain a prefix mapping, was: " + s);
    }

    // [aalto-xml#97]: default namespace must be reachable via DEFAULT_NS_PREFIX ("")
    @Test
    public void testDefaultNamespaceLookup() throws Exception
    {
        FixedNsContext ctx = contextAtRoot(
                "<root xmlns='urn:default' xmlns:a='urn:A'/>");

        // Forward lookup for the default ns must use "" per JAXP spec
        assertEquals("urn:default", ctx.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
        assertEquals("urn:default", ctx.getNamespaceURI(""));
        // Sibling explicit prefix still resolves
        assertEquals("urn:A", ctx.getNamespaceURI("a"));

        // Reverse lookup: default ns URI maps back to ""
        assertEquals(XMLConstants.DEFAULT_NS_PREFIX, ctx.getPrefix("urn:default"));

        // getPrefixes for the default-ns URI yields ""
        Iterator<String> it = ctx.getPrefixes("urn:default");
        assertTrue(it.hasNext());
        assertEquals(XMLConstants.DEFAULT_NS_PREFIX, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testMultipleBindingsToSameUri() throws Exception
    {
        // Two prefixes bound to the same URI — getPrefixes should return both
        // and getPrefix returns the most-recent one.
        FixedNsContext ctx = contextAtRoot(
                "<root xmlns:a='urn:same' xmlns:b='urn:same'/>");

        Iterator<String> it = ctx.getPrefixes("urn:same");
        int count = 0;
        boolean sawA = false, sawB = false;
        while (it.hasNext()) {
            String p = it.next();
            if ("a".equals(p)) sawA = true;
            if ("b".equals(p)) sawB = true;
            count++;
        }
        assertEquals(2, count, "expected two prefixes for shared URI");
        assertTrue(sawA, "expected prefix 'a'");
        assertTrue(sawB, "expected prefix 'b'");
    }
}
