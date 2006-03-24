package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

public class BEncodeTest extends BaseTestCase {

    public BEncodeTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BEncodeTest.class);
    }
    
   
    
    private static class TestReadChannel implements ReadableByteChannel {
        
        private ByteBuffer src;
        private boolean closed;
        
        public void setString(String src) {
            this.src = ByteBuffer.wrap(src.getBytes());
        }

        public int read(ByteBuffer dst) throws IOException {
            if (!src.hasRemaining()) 
                return closed ? -1 : 0;
            
            int position = src.position();
            src.limit(Math.min(src.capacity(),src.position()+dst.remaining()));
            dst.put(src);
            src.limit(src.capacity());
            return src.position() - position;
        }

        public void close() throws IOException {
            closed = true;
        }

        public boolean isOpen() {
            return !closed;
        }
        
    }
    
    /**
     * TODO: add real tests
     */
    public void testBlah() throws Exception {
        TestReadChannel chan = new TestReadChannel();
        chan.setString("-");
        BELong myLong = new BELong(chan);
        if (myLong.getResult() != null)
            System.out.println("bad");
        myLong.handleRead();
        System.out.println(myLong.getResult());
        chan.setString("2e");
        myLong.handleRead();
        System.out.println(myLong.getResult());
        
        chan.setString("3:e3");
        BEString myString = (BEString) Token.getTokenType(chan);
        myString.handleRead();
        System.out.println(myString.getResult());
        myString.handleRead();
        System.out.println(myString.getResult());
        myString.handleRead();
        System.out.println(myString.getResult());
        chan.setString("r");
        myString.handleRead();
        System.out.println(myString.getResult());
        
        chan.setString("d3:asdli2ei3ee1:");
        Token t = Token.getTokenType(chan);
        t.handleRead();
        System.out.println(t.getResult());
        chan.setString("ai5ee");
        System.out.println("added last terminating element");
        t.handleRead();
        System.out.println(t.getResult());
    }

}
