package failing;

import java.io.*;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;

public class Issue35LFTest extends base.BaseTestCase
{
    static final String TEXT="a&#13;a";
    static final String EXPANDED_TEXT = "a a";
    static final String NEWLINE_TEXT = "a\na";
    static final String XML = "<x>" + TEXT + "</x>";
    static final byte[] XML_BYTES = XML.getBytes(StandardCharsets.UTF_8);

    /** Run some tests for each StAX implementation. */
    public void testLFHandling() throws Exception
    {
        final XMLInputFactory inputFactory = getInputFactory();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final XMLEventReader r = inputFactory.createXMLEventReader(
                new ByteArrayInputStream(XML_BYTES));
        final StringBuilder buffer = new StringBuilder();
        while (r.hasNext()) {
            final XMLEvent e = r.nextEvent();
            if (e.isStartDocument()) {
                // Avoid the XML declaration. Not present in the input.
                continue;
            }
            if (e.isCharacters()) {
                String text = e.asCharacters().getData();
                buffer.append(text);
                System.out.println("[CHARACTERS] ("+text.length()+" ["+text+"]");
System.err.println("char #1: "+((int) text.charAt(1)));                
            }
        }
        r.close();
        final byte[] resultBytes = baos.toByteArray();
        System.out.println("StAX XML: ("+resultBytes.length+" bytes) [" + new String(resultBytes,
                StandardCharsets.UTF_8) + "]");
    }

    /*
    private static void printName(final String name, final Object obj) {
        System.out.println(name + "=" + obj.getClass().getName());
    }
*/

    private static void testText(final String text) {
        System.out.println("Buffered text: [" + text + "]");
        System.out.println("Code point at index 1: " +
                Character.codePointAt(text, 1));
        System.out.println("Buffered text equals input text? " +
                TEXT.equals(text));
        System.out.println(
                "Buffered text equals expanded text? " +
                        EXPANDED_TEXT.equals(text));
        System.out.println("Buffered text has \\n for \\r? " +
                NEWLINE_TEXT.equals(text));
    }
}
