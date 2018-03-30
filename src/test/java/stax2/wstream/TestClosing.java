package stax2.wstream;

import java.io.*;

import javax.xml.stream.*;
import javax.xml.transform.stream.StreamResult;

import org.codehaus.stax2.*;
import org.codehaus.stax2.io.Stax2BlockResult;

/**
 * This unit test suite verifies that the auto-closing feature works
 * as expected (both explicitly, and via Result object being passed).
 */
@SuppressWarnings("resource")
public class TestClosing
    extends BaseWriterTest
{
    /**
     * This unit test checks the default behaviour; with no auto-close, no
     * automatic closing should occur, nor explicit one unless specific
     * forcing method is used.
     */
    public void testNoAutoCloseWriter()
        throws XMLStreamException
    {
        XMLOutputFactory2 f = getFactory(false);
        MyWriter output = new MyWriter();
        XMLStreamWriter2 sw = (XMLStreamWriter2) f.createXMLStreamWriter(output);
        // shouldn't be closed to begin with...
        assertFalse(output.isClosed());
        writeDoc(sw);
        assertFalse(output.isClosed());

        // nor closed half-way through with basic close()
        sw.close();
        assertFalse(output.isClosed());

        // but needs to close when forced to:
        sw.closeCompletely();
        assertTrue(output.isClosed());

        // ... and should be ok to call it multiple times:
        sw.closeCompletely();
        sw.closeCompletely();
        assertTrue(output.isClosed());
    }

    public void testNoAutoCloseStream()
        throws XMLStreamException
    {
        XMLOutputFactory2 f = getFactory(false);
        MyStream output = new MyStream();
        XMLStreamWriter2 sw = (XMLStreamWriter2) f.createXMLStreamWriter(output, "UTF-8");
        // shouldn't be closed to begin with...
        assertFalse(output.isClosed());
        writeDoc(sw);
        assertFalse(output.isClosed());

        // nor closed half-way through with basic close()
        sw.close();
        assertFalse(output.isClosed());

        // but needs to close when forced to:
        sw.closeCompletely();
        assertTrue(output.isClosed());

        // ... and should be ok to call it multiple times:
        sw.closeCompletely();
        sw.closeCompletely();
        assertTrue(output.isClosed());
    }

    /**
     * This unit test checks that when auto-closing option is set, the
     * passed in output stream does get properly closed
     * when we call close(), as well as when do writeEndDocument().
     */
    public void testEnabledAutoClose()
        throws XMLStreamException
    {
        // First, explicit close:
        XMLOutputFactory2 f = getFactory(true);
        MyWriter output = new MyWriter();
        XMLStreamWriter2 sw = (XMLStreamWriter2) f.createXMLStreamWriter(output);
        assertFalse(output.isClosed());

        writeDoc(sw);

        sw.close();
        assertTrue(output.isClosed());

        // also, let's verify we can call more than once:
        sw.close();
        sw.close();
        assertTrue(output.isClosed());

        // Then implicit close:
        output = new MyWriter();
        sw = (XMLStreamWriter2) f.createXMLStreamWriter(output);
        writeDoc(sw);
        assertTrue(output.isClosed());
    }

    /**
     * This unit test checks what happens when we use Result abstraction
     * for passing in result stream/writer. Their handling differs depending
     * on whether caller is considered to have access to the underlying
     * physical object or not.
     */
    public void testAutoCloseImplicit()
        throws XMLStreamException
    {
        XMLOutputFactory2 f = getFactory(false); // auto-close disabled

        /* Ok, first: with regular (OutputStream, Writer) results not auto-closing
         * because caller does have access: StreamResult does retain given
         * stream/writer as is.
         */
        MyResult output = new MyResult();
        XMLStreamWriter2 sw = (XMLStreamWriter2) f.createXMLStreamWriter(output);
        assertFalse(output.isClosed());
        writeDoc(sw);
        sw.close();
        assertFalse(output.isClosed());

        /* And then more interesting case; verifying that Stax2Source
         * sub-classes are implicitly auto-closed: they need to be, because
         * they do not (necessarily) expose underlying physical stream.
         * We can test this by using any Stax2Source impl.
         */
        MyStringResult result = new MyStringResult();
        sw = (XMLStreamWriter2) f.createXMLStreamWriter(result);
        // closed if we write end doc
        writeDoc(sw);
        assertTrue(result.isClosed());

        // as well as if we just call regular close
        result = new MyStringResult();
        sw = (XMLStreamWriter2) f.createXMLStreamWriter(result);
        sw.writeStartDocument();
        sw.writeEmptyElement("test");
        // no call to write end doc, so writer can't yet close; but we do call close:
        sw.close();
        assertTrue(result.isClosed());
    }

    /*
    ////////////////////////////////////////
    // Non-test methods
    ////////////////////////////////////////
     */

    XMLOutputFactory2 getFactory(boolean autoClose)
    {
        XMLOutputFactory2 f = getOutputFactory();
        f.setProperty(XMLOutputFactory2.P_AUTO_CLOSE_OUTPUT, Boolean.valueOf(autoClose));
        return f;
    }

    void writeDoc(XMLStreamWriter sw) throws XMLStreamException
    {
        sw.writeStartDocument();
        sw.writeEmptyElement("root");
        sw.writeEndDocument();
    }

    /*
    ////////////////////////////////////////
    // Helper mock classes
    ////////////////////////////////////////
     */

    final static class MyWriter
        extends StringWriter
    {
        boolean mIsClosed = false;

        public MyWriter() { }

        @Override
        public void close() throws IOException {
            mIsClosed = true;
            super.close();
        }

        public boolean isClosed() { return mIsClosed; }
    }

    final static class MyStream
        extends ByteArrayOutputStream
    {
        boolean mIsClosed = false;

        public MyStream() { }

        @Override
        public void close() throws IOException {
            mIsClosed = true;
            super.close();
        }
        public boolean isClosed() { return mIsClosed; }
    }

    final static class MyResult
        extends StreamResult
    {
        final MyWriter mWriter;

        private MyResult() {
            super();
            mWriter = new MyWriter();
            setWriter(mWriter);
        }

        public boolean isClosed() {
            return mWriter.isClosed();
        }
    }

    /**
     * Need a helper class to verify whether resources (OutputStream, Writer)
     * created via Stax2Result instances are (auto)closed or not.
     */
    private final static class MyStringResult
        extends Stax2BlockResult
    {
        MyWriter mWriter;

        public MyStringResult() { super(); }

        @Override
        public Writer constructWriter() {
            mWriter = new MyWriter();
            return mWriter;
        }
        @Override
        public OutputStream constructOutputStream() { return null; }

        public boolean isClosed() { return mWriter.isClosed(); }
    }
}
