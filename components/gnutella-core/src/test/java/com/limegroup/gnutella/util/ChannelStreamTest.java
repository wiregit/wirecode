package com.limegroup.gnutella.util;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import junit.framework.*;
import java.util.Iterator;


public class ChannelStreamTest extends TestCase {
    public ChannelStreamTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ChannelStreamTest.class);
    }

    final int port=6667;
    SocketChannel _outChannel;
    SocketChannel _inChannel;
    ChannelOutputStream _out;
    ChannelInputStream _in;
    
    /** Connects sender and receiver. */
    public void setUp() throws IOException { 
        //This is slightly tricky.  To do this without threads, we use
        //non-blocking IO.
        Selector selector=Selector.open();

        //Start listen
        ServerSocketChannel listener=ServerSocketChannel.open();
        listener.configureBlocking(false);
        listener.socket().setReuseAddress(true);
        listener.socket().bind(new InetSocketAddress(port));    
        listener.register(selector, SelectionKey.OP_ACCEPT);

        //Start connect
        SocketChannel client=SocketChannel.open();
        client.configureBlocking(false);
        if (client.connect(new InetSocketAddress("127.0.0.1", port))) {
            //System.out.println("CONNECT true");
            _outChannel=client;
        } else {
            //System.out.println("CONNECT false");
            client.register(selector, SelectionKey.OP_CONNECT);
        }

        //Wait for them to connect
        while (_outChannel==null || _inChannel==null) {
            selector.select();
            Iterator iter=selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key=(SelectionKey)iter.next();
                iter.remove();
                if ((key.readyOps()&SelectionKey.OP_CONNECT) != 0) {
                    if (client.finishConnect()) {
                        //System.out.println("CONNECT2 true");
                        _outChannel=client;
                    } else {
                        //System.out.println("CONNECT2 false");
                    }
                }
                if ((key.readyOps()&SelectionKey.OP_ACCEPT) != 0) {
                    //System.out.println("Accept");
                    SocketChannel client2=listener.accept();
                    if (client2!=null) {
                        _inChannel=client2;
                    }
                }
            }
        }

        selector.close();
        listener.close();        //TODO: why doesn't this work?

        //Because the channels change blocking mode, we must wait until after
        //the selector is closed; otherwise we get an
        //IllegalBlockingModeException.
        _out=new ChannelOutputStream(_outChannel);
        _in=new ChannelInputStream(_inChannel);
    }
    
    public void tearDown() {
        try { _out.close(); } catch (IOException e) { }
        try { _in.close(); } catch (IOException e) { }
    }

    ////////////////////////////////////////////////////////////////////////

    public void testWrite() {
        byte b1=(byte)0xFC;
        byte b2=(byte)0x00;
        byte b3=(byte)0x0F;
        try {
            _out.write(b1);
            assertEquals(b1, _in.read());
            _out.write(b2);
            _out.write(b3);
            assertEquals(b2, _in.read());
            assertEquals(b3, _in.read());
        } catch (IOException e) {
            fail("IO problem");
        }
    }

    public void testReadFromClosed() {
        try {
            _out.close();
        } catch (IOException e) {
            fail("Couldn't close");
        }

        try {
            int b=_in.read();
            assertTrue(b<0);
        } catch (IOException e) {
            //also passes
        }
    }
}



