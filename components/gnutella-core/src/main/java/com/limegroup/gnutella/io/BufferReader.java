package com.limegroup.gnutella.io;

import java.nio.ByteBuffer;

/**
 * Analagous to com.limegroup.gnutella.ByteReader, but with Buffers.
 * Somewhat more complex, but essentially the same.
 * 
 * To use this to read headers and whatnot, do something like:
 * 
 *  List headers = new LinkedList();
 *  void method(ByteBuffer buffer) {
 *      BufferReader reader = new BufferReader(buffer);
 *      String current = "";
 *      if(!headers.isEmpty())
 *          current = (String)headers.remove(headers.size()-1);
 *      while(true) {
 *          current += reader.readLine();
 *          if(!isLineReadCompletely()) {
 *              headers.add(current);
 *              break;
 *          }
 *           
 *          if(current.isEmpty())
 *              break;
 *              
 *          headers.add(current);
 *      }
 *  }
 */
public class BufferReader {
    
    private static final byte R = '\r';
    private static final byte N = '\n';    
    
    private ByteBuffer buffer;
    private boolean lineReadCompletely;

    public BufferReader(ByteBuffer buffer) {
        this.buffer = buffer;
    }
    
    public String readLine() {
        lineReadCompletely = false;

        StringBuffer sBuffer = new StringBuffer();
        int c = -1; //the character just read
        boolean keepReading = true;
        
        while(buffer.hasRemaining() && keepReading) {
            c = buffer.get();
            switch(c) {
                // if this was a \n character, break out of the reading loop
                case  N: keepReading = false;
                         break;
                // if this was a \r character, ignore it.
                case  R: continue;                        
                // if it was any other character, append it to the buffer.
                default: sBuffer.append((char)c);
            }
        }
        
        // we only read the line completely if we unset keepReading 
        lineReadCompletely = !keepReading;

        // return the string we have read.
        return sBuffer.toString();
    }
    
    public boolean isLineReadCompletely() {
        return lineReadCompletely;
    }

}
