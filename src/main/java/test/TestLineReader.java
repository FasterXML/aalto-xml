package test;

import java.io.*;

public final class TestLineReader
    extends BasePerfTest
{
    final char[] _buffer = new char[4000];

    protected TestLineReader() { }

    @Override
    protected int testExec(File file) throws Exception
    {
        int total = 0;
        InputStream in = new FileInputStream(file);
        Reader r = new InputStreamReader(in, "UTF-8");
        BufferedReader br = new BufferedReader(r);
        String line;

        while ((line = br.readLine()) != null) {
            total += line.length();
        }

        in.close();

        return total;
    }

    public static void main(String[] args) throws Exception
    {
        new TestLineReader().test(args);
    }
}
