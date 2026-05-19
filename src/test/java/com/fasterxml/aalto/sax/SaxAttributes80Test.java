package com.fasterxml.aalto.sax;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// [aalto-xml#80] Attributes.getValue(qName) was returning a value from a
// previous element when the current element had no attributes — because
// the SAX layer routes through AttributeCollector.findIndex, which still
// holds the hash table from the previous element.
public class SaxAttributes80Test extends base.BaseTestCase
{
    @Test
    public void testEmptyElementAfterThreeAttrs() throws Exception
    {
        // Three attributes on <first/> populate the hash table; then
        // <second/> is parsed with no attributes. A by-name lookup of "a"
        // on <second/> must NOT return "1".
        Recording r = _parse("<root><first a='1' b='2' c='3'/><second/></root>");

        assertEquals(0, r.lengthOf("second"),
                "second element must report 0 attributes");
        assertNull(r.valueByQName("second", "a"),
                "getValue('a') on empty <second/> must be null, not stale 'a' from <first/>");
        assertNull(r.valueByQName("second", "b"),
                "getValue('b') on empty <second/> must be null");
        assertNull(r.valueByLocalName("second", "a"),
                "getValue(uri, 'a') on empty <second/> must be null");
        assertEquals(-1, r.indexByQName("second", "a"),
                "getIndex('a') on empty <second/> must be -1");
    }

    @Test
    public void testEmptyElementAfterTwoAttrs() throws Exception
    {
        // Two attributes — keeps AttributeCollector in linear-search mode
        // (hashAreaSize stays at 0). Still must not leak.
        Recording r = _parse("<root><first a='1' b='2'/><second/></root>");
        assertNull(r.valueByQName("second", "a"));
        assertNull(r.valueByQName("second", "b"));
    }

    @Test
    public void testEmptyElementAfterOneAttr() throws Exception
    {
        Recording r = _parse("<root><first a='1'/><second/></root>");
        assertNull(r.valueByQName("second", "a"));
    }

    @Test
    public void testNonEmptyElementAfterThreeAttrsLeaksNothing() throws Exception
    {
        // <second/> now has its own attribute 'x'. Asking for 'a' (from
        // previous) must still be null; asking for 'x' must return "9".
        Recording r = _parse("<root><first a='1' b='2' c='3'/><second x='9'/></root>");
        assertEquals(1, r.lengthOf("second"));
        assertEquals("9", r.valueByQName("second", "x"));
        assertNull(r.valueByQName("second", "a"),
                "stale 'a' from <first/> must not leak into <second/>");
    }

    // -----------------------------------------------------------------

    private Recording _parse(String xml) throws IOException, SAXException
    {
        SAXParserFactoryImpl spf = new SAXParserFactoryImpl();
        SAXParser sp = spf.newSAXParser();
        Recording handler = new Recording();
        sp.parse(new InputSource(new StringReader(xml)), handler);
        return handler;
    }

    static final class StartElementSnapshot
    {
        final String qName;
        final int length;
        // Looked-up values captured at startElement time for assertions later.
        final Map<String,String> byQName = new HashMap<>();
        final Map<String,String> byLocal = new HashMap<>();
        final Map<String,Integer> indexes = new HashMap<>();

        StartElementSnapshot(String qName, Attributes attrs) {
            this.qName = qName;
            this.length = attrs.getLength();
            // Probe a fixed set of names so callers don't need their own
            // handler subclass per test.
            for (String probe : new String[] { "a", "b", "c", "x" }) {
                byQName.put(probe, attrs.getValue(probe));
                byLocal.put(probe, attrs.getValue("", probe));
                indexes.put(probe, attrs.getIndex(probe));
            }
        }
    }

    static final class Recording extends DefaultHandler
    {
        final List<StartElementSnapshot> snapshots = new ArrayList<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            snapshots.add(new StartElementSnapshot(qName, attrs));
        }

        StartElementSnapshot find(String qName) {
            for (StartElementSnapshot s : snapshots) {
                if (s.qName.equals(qName)) return s;
            }
            String seen = snapshots.stream().map(s -> s.qName).collect(Collectors.joining(", "));
            throw new AssertionError("no startElement seen for '" + qName + "'; got [" + seen + "]");
        }

        int lengthOf(String qName)          { return find(qName).length; }
        String valueByQName(String el, String attr)   { return find(el).byQName.get(attr); }
        String valueByLocalName(String el, String attr) { return find(el).byLocal.get(attr); }
        int indexByQName(String el, String attr)      { return find(el).indexes.get(attr); }
    }
}
