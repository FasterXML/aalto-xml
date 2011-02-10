package async;

import async.AsyncTestBase.AsyncReaderWrapper;

import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class TestDoctypeParsing extends AsyncTestBase
{
    public void testSimplest() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testSimplest(SPC, 1);
            _testSimplest(SPC, 2);
            _testSimplest(SPC, 3);
            _testSimplest(SPC, 5);
            _testSimplest(SPC, 11);
            _testSimplest(SPC, 1000);
        }
    }

    public void testWithSystemId() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testWithSystemId(SPC, 1);
            _testWithSystemId(SPC, 2);
            _testWithSystemId(SPC, 3);
            _testWithSystemId(SPC, 6);
            _testWithSystemId(SPC, 900);
        }
    }

    public void testFull() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = spaces(spaces);
            _testFull(SPC, 1);
            _testFull(SPC, 2);
            _testFull(SPC, 3);
            _testFull(SPC, 6);
            _testFull(SPC, 900);
        }
    }
    
    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */
    
    private void _testSimplest(String spaces, int chunkSize) throws Exception
    {
        String XML = spaces+"<!DOCTYPE root>  <root />";
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);
        int t = verifyStart(reader);
        assertTokenType(DTD, t);
        // as per Stax API, can't call getLocalName (ugh), but Stax2 gives us this:
        assertEquals("root", sr.getPrefixedName());
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
    }

    private void _testWithSystemId(String spaces, int chunkSize) throws Exception
    {
        String XML = spaces+"<!DOCTYPE root>  <root />";
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);
        int t = verifyStart(reader);
        assertTokenType(DTD, t);
        // as per Stax API, can't call getLocalName (ugh), but Stax2 gives us this:
        assertEquals("root", sr.getPrefixedName());
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
    }

    private void _testFull(String spaces, int chunkSize) throws Exception
    {
        String XML = spaces+"<!DOCTYPE root>  <root />";
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);
        int t = verifyStart(reader);
        assertTokenType(DTD, t);
        // as per Stax API, can't call getLocalName (ugh), but Stax2 gives us this:
        assertEquals("root", sr.getPrefixedName());
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertTokenType(END_ELEMENT, reader.nextToken());
    }


}
