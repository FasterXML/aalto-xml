package util;

import com.fasterxml.aalto.util.DataUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestDataUtil
    extends base.BaseTestCase
{
    @Test
    public void testEmptyCharArray()
    {
        char[] a = DataUtil.getEmptyCharArray();
        assertNotNull(a);
        assertEquals(0, a.length);
        // Always returns the same shared instance.
        assertSame(a, DataUtil.getEmptyCharArray());
    }

    @Test
    public void testGrowStringArrayFromNull()
    {
        String[] a = DataUtil.growArrayBy((String[]) null, 4);
        assertNotNull(a);
        assertEquals(4, a.length);
        // Freshly allocated arrays are all-null.
        for (int i = 0; i < a.length; ++i) {
            assertNull(a[i]);
        }
    }

    @Test
    public void testGrowStringArrayPreservesContents()
    {
        String[] in = new String[] { "a", "b", "c" };
        String[] out = DataUtil.growArrayBy(in, 2);
        assertEquals(5, out.length);
        assertEquals("a", out[0]);
        assertEquals("b", out[1]);
        assertEquals("c", out[2]);
        assertNull(out[3]);
        assertNull(out[4]);
        // Returns a fresh array, not the same reference.
        assertNotSame(in, out);
    }

    @Test
    public void testGrowIntArrayFromNull()
    {
        int[] a = DataUtil.growArrayBy((int[]) null, 3);
        assertNotNull(a);
        assertEquals(3, a.length);
        // Freshly allocated arrays are zero-filled.
        for (int i = 0; i < a.length; ++i) {
            assertEquals(0, a[i]);
        }
    }

    @Test
    public void testGrowIntArrayPreservesContents()
    {
        int[] in = new int[] { 1, 2, 3 };
        int[] out = DataUtil.growArrayBy(in, 4);
        assertEquals(7, out.length);
        assertEquals(1, out[0]);
        assertEquals(2, out[1]);
        assertEquals(3, out[2]);
        assertEquals(0, out[3]); // tail zero-filled
        assertNotSame(in, out);
    }

    @Test
    public void testGrowCharArrayFromNull()
    {
        char[] a = DataUtil.growArrayBy((char[]) null, 5);
        assertNotNull(a);
        assertEquals(5, a.length);
    }

    @Test
    public void testGrowCharArrayPreservesContents()
    {
        char[] in = new char[] { 'a', 'b' };
        char[] out = DataUtil.growArrayBy(in, 3);
        assertEquals(5, out.length);
        assertEquals('a', out[0]);
        assertEquals('b', out[1]);
        assertEquals('\0', out[2]);
        assertNotSame(in, out);
    }

    @Test
    public void testGrowAnyArray()
    {
        Long[] in = new Long[] { 10L, 20L };
        Object out = DataUtil.growAnyArrayBy(in, 2);
        assertTrue(out instanceof Long[], "grown array should still be Long[]");
        Long[] grown = (Long[]) out;
        assertEquals(4, grown.length);
        assertEquals(Long.valueOf(10L), grown[0]);
        assertEquals(Long.valueOf(20L), grown[1]);
        assertNull(grown[2]);
        assertNull(grown[3]);
    }

    @Test
    public void testGrowAnyArrayPrimitive()
    {
        // Primitive component types go through a different Array.newInstance
        // code path than reference types.
        byte[] in = new byte[] { 1, 2, 3 };
        Object out = DataUtil.growAnyArrayBy(in, 2);
        assertTrue(out instanceof byte[], "grown array should still be byte[]");
        byte[] grown = (byte[]) out;
        assertEquals(5, grown.length);
        assertEquals((byte) 1, grown[0]);
        assertEquals((byte) 2, grown[1]);
        assertEquals((byte) 3, grown[2]);
        assertEquals((byte) 0, grown[3]); // tail zero-filled
        assertEquals((byte) 0, grown[4]);
    }

    @Test
    public void testGrowAnyArrayRejectsNull()
    {
        try {
            DataUtil.growAnyArrayBy(null, 1);
            fail("Expected IllegalArgumentException for null array");
        } catch (IllegalArgumentException expected) {
        }
    }
}
