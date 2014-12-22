package com.fasterxml.aalto;

import java.nio.ByteBuffer;

import javax.xml.stream.XMLStreamException;

public interface AsyncByteBufferFeeder extends AsyncInputFeeder
{
     /**
      * Method that can be called to feed more data, if (and only if)
      * {@link AsyncInputFeeder#needMoreInput} returns true.
      * 
      * @param buffer Buffer that contains additional input to read
      * 
      * @throws XMLStreamException if the state is such that this method should not be called
      *   (has not yet consumed existing input data, or has been marked as closed)
      */
     public void feedInput(ByteBuffer buffer) throws XMLStreamException;

}
