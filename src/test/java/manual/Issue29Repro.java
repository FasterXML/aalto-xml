package manual;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.util.XMLEventAllocator;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.evt.EventAllocatorImpl;
import com.fasterxml.aalto.stax.InputFactoryImpl;

/**
 * Manually runnable reproduction of [aalto-xml#29]
 */
public class Issue29Repro implements Runnable
{
    private static String xml = "<?xml version='1.0'?><stream:stream xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' id='4095288169' from='localhost' version='1.0' xml:lang='en'>";
    private static int NUM_THREADS = 5;
    private static XMLEventAllocator allocator = EventAllocatorImpl.getDefaultInstance();
    private static AsyncXMLInputFactory inputFactory = new InputFactoryImpl();

    public static void main(String[] args) throws InterruptedException {
        ExecutorService ex = Executors.newFixedThreadPool(NUM_THREADS);

        for (int i = 0; i < 100000; i++) {
            ex.submit(new Issue29Repro(i));
        }

        ex.shutdown();
        ex.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
    }

    private final int count;

    public Issue29Repro(int count) {
        this.count = count;
    }

    @Override
    public void run() {
        try {
            ByteBuffer bb = StandardCharsets.UTF_8.encode(xml);
            AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = inputFactory.createAsyncForByteBuffer();
            parser.getInputFeeder().feedInput(bb);
            while (parser.hasNext()) {
                int eventType = parser.next();
                if (eventType == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
                    break;
                }

                allocator.allocate(parser);
            }
        } catch (Exception e) {
            System.out.println("Error in " + count);
            e.printStackTrace();
        }
    }
}
