package util;
import com.fasterxml.aalto.util.TextAccumulator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests for testing {@link TextAccumulator}. That class
 * is generally used to try to minimize shuffling between char arrays,
 * Strings and StringBuilders -- most common case being that only one
 * instance is passed, before a String is needed.
 */
public class TestTextAccumulator
{
    @Test
    public void testBasic()
    {
        TextAccumulator acc = new TextAccumulator();

        acc.addText("foo");
        assertEquals("foo", acc.getAndClear());

        acc.addText("foo".toCharArray(), 0, 3);
        acc.addText("bar");
        assertEquals("foobar", acc.getAndClear());
    }

    // as per [WSTX-349]
    @Test
    public void testBasicWithCharArray()
    {
        TextAccumulator acc = new TextAccumulator();

        acc.addText("foobar".toCharArray(), 3, 5);
        assertEquals("ba", acc.getAndClear());

        acc.addText("xxfoo".toCharArray(), 2, 5);
        acc.addText("bar".toCharArray(), 2, 3);
        acc.addText(new char[] { '1', '2', '3' }, 2, 3);
        assertEquals("foor3", acc.getAndClear());

        acc.addText("a");
        acc.addText(new char[] { '1', '2', '3' }, 2, 3);
        assertEquals("a3", acc.getAndClear());
    }
}
