package test;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.typed.*;

/**
 * Simple helper test class for checking how stream reader handles xml
 * documents.
 */
public class TestBase64Reader
    implements XMLStreamConstants
{
    final static String CARNAL = "TWFuIGlzIGRpc3Rpbmd1"
        +"aXNoZWQsIG5vdCBvbmx5"
        +"IGJ5IGhpcyByZWFzb24s"
        +"IGJ1dCBieSB0aGlz\n"
        +"IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\n"
        +"dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\n"
        +"dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\n"
        +"ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4="
        ;

    public void test() throws XMLStreamException
    {
        System.setProperty("javax.xml.stream.XMLInputFactory", com.fasterxml.aalto.stax.InputFactoryImpl.class.getName());
        XMLInputFactory f = XMLInputFactory.newInstance();
        String xml = "<root>"+

            CARNAL + "\r\n" + CARNAL

            /*
"3Q==8SG5lQS8PNE=jKv9qvA=M00W0ddAMg==JvqSRVfamzoPRkLDdk0=zg==ipxTn18=DRvg/NSejA==kPROt9r3hg==KXsnaRLrRw==iw==rljC18si8Q==hE"
+"Q=BMsmlADRGg==PrTreugryi6aKQ==RB8m1MV0N9U=Aqg=+cspTHXQ5g==yg==EA==iGjKFpPsyw==EWj4Sx/LijsX6IGOVg==xdmpbA==ugCY7kkX2A==3875iQ==rdMBOg==k9Q=WHsAjg==0YBlxBH3f4g=8iBCZNc=F/vmYh0n7SVDzSw=ias=Qjk=GQ==58jWyTB2JBixA9HoaHA=BdsHPA==laoeNg==ZckUeu0BPQ==nWSrYCS6spA=CGtFgnApf1s=sBw5rRw2BCI=rw==mw==hFHZdytEE6Y=wRsogw==22HyIOg=gWZqJddv7WM=dPAd5Q==nbFW1JY=asFzKK1UWxzUMfb3oA==aw25qUQ=zw==H5P+ZuMHlA==HFJjQ7p+xtM=tWowSOGpavY51wY=nDE=di+7GhQ=CJpL0g==nFaelQ==1HM=9a1lS0h2vV9SWQ==07hVBPGlgA==NtOnxg==cNNRjZtSFg==HaOD2A==ykDJ2w==WxWY+X+An5M=Mg==cA==Ops=pkY=Wg==MnmcTXaeNis=9nTusctd2A==u8ipom18WiRuVYYp9w==ODB9b17WtQ==uZ8=fA==bj4Q5sw=VQ==iZjKSG1yUvXRTbRdAQ==Nj58XHuwSn3m3T4pVQ==pjTxHg==+g==sQ==saQ2dbbLLDjmrSTyTQ==iHYciDfKybfM+A==kLI53oIYF8Q=g8fz/7ymP5MnKjM=Og==ViMoVA==jWF2mtuHzV4=Q53k6Q==qQ==fLk=50e11w==8dPRumtqE4c=GCcj9/4qM0w=zD4+pTfVkQ==jQtHA19oTUWrjTE=4spKcpvgZQk=ILA=Cb1rAJ7Bfw==LnoE+s1kmA==DBnsLKg=ig==RXwXLQ==N/u/sJUfn1k48RzOPg==YcFYytmHle8z5ss=ZsLymyX/wKzgEf9eLA==GVs=iLJ8ew==NPTh0W6vLKo=LvETVhke9ps=5y3zw1E=ZJ3iFQ77xHE=S7c=Yw==12i7Ta3cFBtTGcgVG1B5/w==wN6LG9/3Mm+fKvF6zsk=j9Lvxn4pKYbaVlC/jlA=7QY=Gt++uQ==ZFQ=oRartA==UQ==GA==cteMWetqTA==gC4=Lgb8fA==GA==PA==71UnCeN43kM="
            */
            +"</root>";

        final int CHUNK_LEN = 19;
        byte[] buffer = new byte[CHUNK_LEN];

        TypedXMLStreamReader sr = (TypedXMLStreamReader) f.createXMLStreamReader(new StringReader(xml));

        // TEST: to see if Stax2 reader adapter works, let's try this:
        //sr = new TestAdapter(sr);

        // First, advance to root START_ELEMENT
        sr.next();

        // and then may try to move to CHARACTERS?
        /*
        if (sr.next() != XMLStreamConstants.CHARACTERS) {
            throw new IllegalStateException("State not CHARACTERS, but "+sr.getEventType());
        }
        */

        int offset = 0;

        while (true) {
            int count = sr.readElementAsBinary(buffer, 0, buffer.length,
                                               //Base64Variants.MODIFIED_FOR_URL
                                               Base64Variants.MIME
                                               );
            System.out.print("Result("+offset+"+"+count+"): ");
            if (count < 0) {
                break;
            }
            System.out.print('"');
            for (int i = 0; i < count; ++i) {
                System.out.print((char) buffer[i]);
                //System.out.print(" 0x"+Integer.toHexString(buffer[i] & 0xFF));
            }
            System.out.print('"');
            offset += count;
            System.out.println();
        }
        System.out.println("DONE!");
        sr.close();
    }

    public static void main(String[] args)
        throws Exception
    {
        new TestBase64Reader().test();
    }

    // Need a sub-class, as base class constructor is not public
    final static class TestAdapter
        extends org.codehaus.stax2.ri.Stax2ReaderAdapter
    {
        public TestAdapter(XMLStreamReader sr) { super(sr); }
    }
}
