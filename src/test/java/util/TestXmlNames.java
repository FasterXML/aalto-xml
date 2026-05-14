package util;

import com.fasterxml.aalto.util.XmlNames;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link XmlNames#findIllegalNameChar(String, boolean)}.
 *
 * Note: production code only ever calls this with xml11=false (see
 * StreamReaderImpl.verifyQName). The xml11=true path's
 * is11NameStartChar/is11NameChar reject ASCII letters (the check
 * {@code c < 0x00C0} returns false for codepoints below 0xC0), so
 * everyday ASCII names fail under xml11. Tests therefore use Latin-1
 * letters (0xC0+) to drive the xml11 paths.
 *
 * The surrogate-pair tests pin down current behavior of the private
 * helper validSurrogateNameChar, which always returns false. The
 * source flags this as suspected-incorrect (see the 2021 TODO in
 * XmlNames.java); when that helper is fixed, the "expected index 1"
 * assertions in the surrogate tests will need to flip accordingly.
 */
public class TestXmlNames
    extends base.BaseTestCase
{
    @Test
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

    @Test
    public void testColonRejected()
    {
        // Colon was removed from the valid set, so "ns:elem" is rejected at the colon.
        assertEquals(2, XmlNames.findIllegalNameChar("ns:elem", false));
    }

    @Test
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

    @Test
    public void testIllegalNonStartChar()
    {
        // Space.
        assertEquals(2, XmlNames.findIllegalNameChar("ab cd", false));
        // Punctuation in the middle.
        assertEquals(2, XmlNames.findIllegalNameChar("ab!cd", false));
        // Trailing illegal char.
        assertEquals(2, XmlNames.findIllegalNameChar("ab*", false));
    }

    @Test
    public void testHighBmpNameStartChar()
    {
        // U+4E00 is the first CJK Ideograph block — valid 10-name-start char.
        assertEquals(-1, XmlNames.findIllegalNameChar("\u4E00a", false));
        // U+E000 (private use area) is rejected by is10NameStartChar.
        assertEquals(0, XmlNames.findIllegalNameChar("\uE000", false));
    }

    @Test
    public void testSurrogateStartCharShortName()
    {
        // See class-level comment: pins down validSurrogateNameChar's current
        // (suspected-incorrect) behavior. High surrogate alone (string length 1)
        // is rejected at offset 0 via the "len < 2" branch.
        assertEquals(0, XmlNames.findIllegalNameChar("\uD800", false));
    }

    @Test
    public void testSurrogateStartCharLongerName()
    {
        // See class-level comment: validSurrogateNameChar always returns false,
        // so a valid-looking high+low pair at start is reported at offset 1.
        assertEquals(1, XmlNames.findIllegalNameChar("\uD83D\uDE00rest", false));
    }

    @Test
    public void testLowSurrogateFirstRejected()
    {
        // Low surrogate first is invalid — validSurrogateNameChar checks the
        // first-half range and returns false.
        assertEquals(1, XmlNames.findIllegalNameChar("\uDC00\uD800", false));
    }

    @Test
    public void testTrailingUnpairedSurrogate()
    {
        // Valid start, then unpaired high surrogate at the end.
        // Loop hits the surrogate at ptr=1; (ptr+1) >= len -> reports ptr=1.
        assertEquals(1, XmlNames.findIllegalNameChar("a\uD800", false));
    }

    @Test
    public void testValidUnderXml11()
    {
        // À / é / ñ are all valid xml11 name chars
        // (start: c >= 0x00C0; non-start: same range, not 0xD7/0xF7/0x37E).
        assertEquals(-1, XmlNames.findIllegalNameChar("\u00C0\u00E9\u00F1", true));
    }

    @Test
    public void testIllegalMidCharUnderXml11()
    {
        // Valid start (À), valid 2nd char (é), then a space.
        // Drives the xml11-specific mid-loop branch (XmlNames.java:43-59).
        assertEquals(2, XmlNames.findIllegalNameChar("\u00C0\u00E9 ", true));
    }

    @Test
    public void testTrailingUnpairedSurrogateUnderXml11()
    {
        // Valid Latin-1 start, then unpaired high surrogate at end.
        // Drives the xml11-specific (ptr+1) >= len branch.
        assertEquals(1, XmlNames.findIllegalNameChar("\u00C0\uD800", true));
    }
}
