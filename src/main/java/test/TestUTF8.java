package test;

import java.io.*;

public final class TestUTF8 extends BasePerfTest
{
    final char[] _buffer = new char[4000];

    protected TestUTF8() { }

    @Override
    protected int testExec(File file) throws Exception
    {
        int total = 0;
        InputStream in = new FileInputStream(file);
        Reader r = new InputStreamReader(in, "UTF-8");
        int count;
        char[] cbuf = _buffer;

        while ((count = r.read(cbuf)) > 0) {
            for (int i = 0; i < count; ++i) {
                total += cbuf[i];
            }
        }

        in.close();

        return total;
    }

    public static void main(String[] args) throws Exception
    {
        new TestUTF8().test(args);
    }
}
