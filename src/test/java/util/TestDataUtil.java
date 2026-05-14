package util;

import com.fasterxml.aalto.util.DataUtil;

public class TestDataUtil
    extends base.BaseTestCase
{
    public void testEmptyCharArray()
    {
        char[] a = DataUtil.getEmptyCharArray();
        assertNotNull(a);
        assertEquals(0, a.length);
        // Always returns the same shared instance.
        assertSame(a, DataUtil.getEmptyCharArray());
    }

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

    public void testGrowCharArrayFromNull()
    {
        char[] a = DataUtil.growArrayBy((char[]) null, 5);
        assertNotNull(a);
        assertEquals(5, a.length);
    }

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

    public void testGrowAnyArray()
    {
        Long[] in = new Long[] { 10L, 20L };
        Object out = DataUtil.growAnyArrayBy(in, 2);
        assertTrue("grown array should still be Long[]", out instanceof Long[]);
        Long[] grown = (Long[]) out;
        assertEquals(4, grown.length);
        assertEquals(Long.valueOf(10L), grown[0]);
        assertEquals(Long.valueOf(20L), grown[1]);
        assertNull(grown[2]);
        assertNull(grown[3]);
    }

    public void testGrowAnyArrayRejectsNull()
    {
        try {
            DataUtil.growAnyArrayBy(null, 1);
            fail("Expected IllegalArgumentException for null array");
        } catch (IllegalArgumentException expected) {
        }
    }
}
