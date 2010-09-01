package test;

import java.io.*;

import com.fasterxml.aalto.in.*;

public final class TestNameHashing
{
    ByteBasedPNameTable mTable = null;

    int mCharCount = 0;

    protected TestNameHashing() { }

    protected void test(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java "+getClass().getName()+" <file>");
            System.exit(1);
        }
        test2(args);
        int wordCount = mTable.size();
        double avgLen = (double) mCharCount / (double) wordCount;

        // Let's check memory usage too:
        Runtime rt = Runtime.getRuntime();
        long freeMin = rt.freeMemory();
        System.out.println("DEBUG: Free1: "+freeMin+", total: "+rt.totalMemory()+", max: "+rt.maxMemory());

        try { Thread.sleep(400L); } catch (InterruptedException ie) { }
        Thread.yield();
        System.gc();
        Thread.yield();
        try { Thread.sleep(400L); } catch (InterruptedException ie) { }
        Thread.yield();
        System.gc();
        Thread.yield();

        freeMin = rt.freeMemory();
        System.out.println("DEBUG: Free2: "+freeMin+", total: "+rt.totalMemory()+", max: "+rt.maxMemory());

        mTable.nuke();
        mTable = null;
        try { Thread.sleep(400L); } catch (InterruptedException ie) { }
        Thread.yield();
        System.gc();
        Thread.yield();
        try { Thread.sleep(400L); } catch (InterruptedException ie) { }
        Thread.yield();
        System.gc();
        Thread.yield();

        long freeMax = rt.freeMemory();
        System.out.println("DEBUG: Free3: "+freeMax+", total: "+rt.totalMemory()+", max: "+rt.maxMemory());

        long tableSize = freeMax - freeMin;
        double avgSize = tableSize / (double) wordCount;

        System.out.println("Memory used by table: "+tableSize+" -> "+avgSize+" bytes per word ("+avgLen+" chars/word)");
    }

    protected void test2(String[] args)
        throws Exception
    {
        mTable = new ByteBasedPNameTable(64);
        InputStream in = new FileInputStream(args[0]);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        System.out.println("Ok, starting to read in names: ");

        String word;
        mCharCount = 0;

        while ((word = br.readLine()) != null) {
            if (tryToFind(mTable, word) == null) {
                addSymbol(mTable, word);
                //System.out.print("+'"+word+"' ");
                //System.out.print('+');
                mCharCount += word.length();
            } else {
                System.out.print('.');
            }
        }
        System.out.println(".");
        System.out.println("Done! Table: "+mTable.toString());
        //System.out.println(" -> "+mTable.toDebugString());

        in.close();
    }

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
            wordBytes = word.getBytes("UTF-8");
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

    public static void main(String[] args)
        throws Exception
    {
        new TestNameHashing().test(args);
    }
}
