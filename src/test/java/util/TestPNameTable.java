package util;

import junit.framework.TestCase;

import com.fasterxml.aalto.in.*;

public class TestPNameTable
    extends TestCase
{
    final static String UTF8 = "UTF-8";

    public void testUTF8()
    {
        ByteBasedPNameTable table = new ByteBasedPNameTable(16);
        final String[] names = new String[] {
            "someElementName", "a", "ns:a", "a:ns",
            "foobar", "_x11:elem", "attribute", "x",
            "xml:id", "Soap:Envelope", "root", "branch",
            "SomeSlightlyLongerNsPrefix:andElementNameToo", "b", "ab", "aaaa"
        };
        for (int i = 0; i < names.length; ++i) {
            String currWord = names[i];

            // Shouldn't be found yet:
            assertNull(tryToFind(table, currWord));
            assertEquals(i, table.size());

            // So let's add it
            PName added = addSymbol(table, currWord);
            assertNotNull(added);

            // Should now find it
            assertEquals(added, tryToFind(table, currWord));
            assertEquals(i+1, table.size());

            // ... as well as all prior words:
            for (int j = i; --j >= 0; ) {
                PName other = tryToFind(table, names[j]);
                if (other == null) {
System.err.println("FAIL: table('"+names[j]+"') == "+table.toDebugString());
                    fail("Should have found '"+names[j]+"' (entry #"+j+" out of "+i+")");
                }
                assertEquals(names[j], other.getPrefixedName());
            }
        }
    }

    /*
    ////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////
     */

    PName tryToFind(ByteBasedPNameTable table, String word)
    {
        int[] quads = calcQuads(word);
        int hash = ByteBasedPNameTable.calcHash(quads, quads.length);
        if (quads.length < 3) {
            return table.findSymbol(hash, quads[0], (quads.length < 2) ? 0 : quads[1]);
        }
        return table.findSymbol(hash, quads, quads.length);
    }

    PName addSymbol(ByteBasedPNameTable table, String word)
    {
        int[] quads = calcQuads(word);
        int colonIx = word.indexOf(':');
        int hash = ByteBasedPNameTable.calcHash(quads, quads.length);
        if (quads.length < 3) {
            return table.addSymbol(hash, word, colonIx, quads[0], (quads.length < 2) ? 0 : quads[1]);
        }
        return table.addSymbol(hash, word, colonIx, quads, quads.length);
    }

    int[] calcQuads(String word)
    {
        byte[] wordBytes;
        try {
            wordBytes = word.getBytes(UTF8);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new Error("Internal error: "+ex); // should never occur
        }
        int blen = wordBytes.length;
        int[] result = new int[(blen + 3) / 4];
        for (int i = 0; i < blen; ++i) {
            int x = wordBytes[i] & 0xFF;

            if (++i < blen) {
                x = (x << 8) | (wordBytes[i] & 0xFF);
                if (++i < blen) {
                    x = (x << 8) | (wordBytes[i] & 0xFF);
                    if (++i < blen) {
                        x = (x << 8) | (wordBytes[i] & 0xFF);
                    }
                }
            }
            result[i / 4] = x;
        }

        return result;
    }
}
