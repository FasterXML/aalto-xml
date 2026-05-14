package util;

import com.fasterxml.aalto.util.XmlNames;

/**
 * Exercises {@link XmlNames#findIllegalNameChar(String, boolean)}.
 *
 * Note: production code only ever calls this with xml11=false (see
 * StreamReaderImpl.verifyQName). The xml11=true path's
 * is11NameStartChar/is11NameChar reject ASCII letters (the check
 * {@code c < 0x00C0} returns false for codepoints below 0xC0), so
 * everyday ASCII names fail under xml11. Tests focus on the realistic
 * xml10 path; one xml11 test covers the surrogate fallthrough.
 */
public class TestXmlNames
    extends base.BaseTestCase
{
    public void testValidSimpleName()
    {
        assertEquals(-1, XmlNames.findIllegalNameChar("a", false));
        assertEquals(-1, XmlNames.findIllegalNameChar("name", false));
        assertEquals(-1, XmlNames.findIllegalNameChar("_underscore", false));
        assertEquals(-1, XmlNames.findIllegalNameChar("Z", false));
        // Digits, hyphens, dots are valid after the first character.
        assertEquals(-1, XmlNames.findIllegalNameChar("a1b2c3", false));
        assertEquals(-1, XmlNames.findIllegalNameChar("a-b.c_d", false));
    }

    public void testColonRejected()
    {
        // Colon was removed from the valid set, so "ns:elem" is rejected at the colon.
        assertEquals(2, XmlNames.findIllegalNameChar("ns:elem", false));
    }

    public void testIllegalStartChar()
    {
        // Digits cannot start a name.
        assertEquals(0, XmlNames.findIllegalNameChar("1abc", false));
        // Hyphen cannot start a name either.
        assertEquals(0, XmlNames.findIllegalNameChar("-abc", false));
        // Period.
        assertEquals(0, XmlNames.findIllegalNameChar(".abc", false));
        // Pure punctuation.
        assertEquals(0, XmlNames.findIllegalNameChar("!", false));
    }

    public void testIllegalMidChar()
    {
        // Space is invalid in the middle of a name.
        assertEquals(2, XmlNames.findIllegalNameChar("ab cd", false));
        // Punctuation in the middle.
        assertEquals(2, XmlNames.findIllegalNameChar("ab!cd", false));
        // At the very end.
        assertEquals(2, XmlNames.findIllegalNameChar("ab*", false));
    }

    public void testHighBmpNameStartChar()
    {
        // U+4E00 is the first CJK Ideograph block — valid 10-name-start char.
        assertEquals(-1, XmlNames.findIllegalNameChar("\u4E00a", false));
        // U+E000 (private use area) is rejected by is10NameStartChar.
        assertEquals(0, XmlNames.findIllegalNameChar("\uE000", false));
    }

    public void testSurrogateStartCharShortName()
    {
        // High surrogate alone (string length 1) is rejected at offset 0
        // via the "len < 2" branch.
        assertEquals(0, XmlNames.findIllegalNameChar("\uD800", false));
    }

    public void testSurrogateStartCharLongerName()
    {
        // High+low surrogate at start: validSurrogateNameChar always returns
        // false in this implementation, so the failure is reported at offset 1.
        assertEquals(1, XmlNames.findIllegalNameChar("\uD83D\uDE00rest", false));
    }

    public void testLowSurrogateFirstRejected()
    {
        // Low surrogate first is invalid — validSurrogateNameChar checks the
        // first-half range and returns false.
        assertEquals(1, XmlNames.findIllegalNameChar("\uDC00\uD800", false));
    }

    public void testTrailingUnpairedSurrogate()
    {
        // Valid start, then unpaired high surrogate at the end.
        // Loop hits the surrogate at ptr=1; (ptr+1) >= len -> reports ptr=1.
        assertEquals(1, XmlNames.findIllegalNameChar("a\uD800", false));
    }

    public void testXml11SurrogateFallthrough()
    {
        // Hits the xml11 surrogate handling at start, which calls
        // validSurrogateNameChar (always returns false here): index 1.
        assertEquals(1, XmlNames.findIllegalNameChar("\uD83D\uDE00rest", true));
    }
}
