package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.*;

import junit.framework.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.Iterator;
import com.sun.java.util.collections.Arrays;

/**
 * Tests the reading and writing of messages on the wire, without  worrying
 * about handshaking.  Concentrates on non-blocking featuers.
 */
public class MessageReadWriteTest extends TestCase {
    final int port=6666;

    Socket sender;
    Socket receiver;

    MessageReader reader;
    MessageWriter writer;

    public MessageReadWriteTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MessageReadWriteTest.class);
    }
    
    /** Connects sender and receiver. */
    public void setUp() throws IOException { 
        //This is slightly tricky.  To do this without threads, we use
        //non-blocking IO.
        Selector selector=Selector.open();

        //Start listen
        ServerSocketChannel listener=ServerSocketChannel.open();
        listener.configureBlocking(false);
        listener.socket().bind(new InetSocketAddress(port));    
        listener.register(selector, SelectionKey.OP_ACCEPT);

        //Start connect
        SocketChannel client=SocketChannel.open();
        client.configureBlocking(false);
        if (client.connect(new InetSocketAddress("127.0.0.1", port))) {
            //System.out.println("CONNECT true");
            sender=client.socket();
        } else {
            //System.out.println("CONNECT false");
            client.register(selector, SelectionKey.OP_CONNECT);
        }

        //Wait for them to connect
        while (sender==null || receiver==null) {
            selector.select();
            Iterator iter=selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key=(SelectionKey)iter.next();
                if ((key.readyOps()&SelectionKey.OP_CONNECT) != 0) {
                    if (client.finishConnect()) {
                        //System.out.println("CONNECT2 true");
                        sender=client.socket();
                    } else {
                        //System.out.println("CONNECT2 false");
                    }
                }
                if ((key.readyOps()&SelectionKey.OP_ACCEPT) != 0) {
                    //System.out.println("Accept");
                    receiver=listener.accept().socket();    //may be null
                }
            }
        }

        reader=new MessageReader(sender.getChannel());      //aka, client
        writer=new MessageWriter(receiver.getChannel());    

        listener.close();        //TODO: why doesn't this work?
    }
    
    public void tearDown() {
        try {
            sender.close();
            receiver.close();
        } catch (IOException e) { }
    }

    //////////////////////////////////////////////////////////////////////////

    //test multiple writes/reads per message (big messages)
    //test write when nothing queued
    //test read when nothing available
    //test messages with payloads

    public void testEmptyWrite() throws IOException {
        assertTrue(! writer.write());
    }

    public void testEmptyRead() throws IOException {
        try {
            assertTrue(reader.read()==null);
        } catch (BadPacketException e) {
            fail("Bad packet");
        }
    }

    public void testNormal() throws IOException {
        //Send ping
        PingRequest ping=new PingRequest((byte)3);
        assertTrue("Couldn't queue", writer.queue(ping));
        assertTrue("Couldn't write in one try", writer.write());
        sleep(100);           //wait in case of buffering

        try {
            Message m=reader.read();
            assertTrue("No message read", m!=null);
            assertTrue("Read wrong kind of message", m instanceof PingRequest);        
            assertTrue("Differing GUIDs", 
                       Arrays.equals(m.getGUID(), ping.getGUID()));
        } catch (BadPacketException e) {
            fail("Bad packet");
        }

        //Send query
        QueryRequest query=new QueryRequest((byte)3, 0, "query");
        assertTrue("Couldn't queue", writer.queue(query));
        assertTrue("Couldn't write in one try", writer.write());
        sleep(100);           //wait in case of buffering

        try {
            Message m=reader.read();
            assertTrue("No message read", m!=null);
            assertTrue("Read wrong kind of message", m instanceof QueryRequest);
            assertTrue("Differing GUIDs", 
                       Arrays.equals(m.getGUID(), query.getGUID()));
        } catch (BadPacketException e) {
            fail("Bad packet");
        }
    }   

    private void sleep(long msecs) {
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException e) { }
    }
}
