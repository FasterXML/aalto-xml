package test;

import java.io.*;

import com.fasterxml.aalto.in.*;

public final class TestNameTable
    extends BasePerfTest
{
    final byte[] mBuffer = new byte[4000];
    final ByteBasedPNameTable mTable = new ByteBasedPNameTable(128);

    protected TestNameTable() { }

    @Override
    protected int testExec(File file)
        throws Exception
    {
        int total = 0;
        InputStream in = new FileInputStream(file);
        int count;
        byte[] buf = mBuffer;

        while ((count = in.read(buf)) > 0) {
            for (int i = 0; i < count; ++i) {
                total += buf[i];
            }
        }

        in.close();

        return total;
    }

    public static void main(String[] args)
        throws Exception
    {
        new TestNameTable().test(args);
    }
}
