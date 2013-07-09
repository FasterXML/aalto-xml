package test;

import java.io.*;

public final class TestRawStream extends BasePerfTest
{
    final byte[] mBuffer = new byte[4000];

    protected TestRawStream() { }

    @Override
    protected int testExec(File file) throws Exception
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

    public static void main(String[] args) throws Exception
    {
        new TestRawStream().test(args);
    }
}
