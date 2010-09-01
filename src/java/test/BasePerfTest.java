package test;

import java.io.*;

public abstract class BasePerfTest
{
    final static int DEFAULT_SECS = 15;

    protected BasePerfTest() { }

    protected final int test(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+getClass().getName()+" [file]");
            System.exit(1);
        }
        try {
            int total = test2(new File(args[0]), DEFAULT_SECS);
            System.out.println();
            System.out.println("Total: "+total);
            return total;
        } catch (Exception t) {
            System.err.println("Error: "+t);
            t.printStackTrace();
            throw t;
        }
    }

    protected final int test2(File file, int SECS)
        throws Exception
    {
        /* Let's try to ensure GC is done so that real test can start from
         * a clean state.
         */
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        System.gc();
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        System.gc();
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

        System.out.println("Ok, warmup done. Now doing real testing, running for "+SECS+" seconds.");

        long nextTime = System.currentTimeMillis();
        long endTime = nextTime + (SECS * 1000);
        int count = 0;
        int total = 0;
        int subtotal = 0;
        final long SUB_PERIOD = 1000L; // print once a second
        nextTime += SUB_PERIOD;

        /* Let's try to reduce overhead of System.currentTimeMillis()
         * by calling test method twice each round. May be a problem for
         * big docs/slow readers... but otherwise not.
         */
        while (true) {
            total += testExec(file);
            total += testExec(file);
            long now = System.currentTimeMillis();
            if (now > endTime) {
                break;
            }
            /* let's only print once a second... limits console overhead,
             * but still informs about progress.
             */
            subtotal += 2;
            if (now > nextTime) {
                count += subtotal;
                /* Let's normalize to start from 0 (since 2 is minimum
                 * possible, in increments of 2)
                 */
                subtotal = (subtotal >> 1) - 1;

                char c;
                if (subtotal > 35) {
                    c = '+';
                } else if (subtotal > 9) {
                    c = (char) ('a' + (subtotal-10));
                } else {
                    c = (char) ('0' + subtotal);
                }
                System.out.print(c);
                nextTime += SUB_PERIOD;
                if (nextTime < now) {
                    nextTime = now;
                }
                subtotal = 0;
            }
        }

        System.out.println("Total iterations done: "+count+" [done "+total+"]");
        return total;
    }

    protected abstract int testExec(File file)
        throws Exception;
}
