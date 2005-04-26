package com.limegroup.gnutella.connection;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.net.*;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;

/**
 * Tests that MessageReader extracts messages from a source channel correctly.
 */
public final class MessageReaderTest extends BaseTestCase {
    
    private StubMessageReceiver STUB = new StubMessageReceiver();
    private MessageReader READER = new MessageReader(STUB);
    
    private static final byte[] IP = new byte[] { (byte)127, 0, 0, 1 };

	public MessageReaderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(MessageReaderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void setUp() throws Exception {
	    STUB.clear();
        RouterService.getAcceptor().setAddress(InetAddress.getLocalHost());	    
	}
	
	public void testSingleMessageRead() throws Exception {
	    Message out = new PingRequest((byte)1);
	    READER.setReadChannel(channel(buffer(out)));
	    assertEquals(0, STUB.size());
	    READER.handleRead();
	    assertEquals(1, STUB.size());
	    Message in = STUB.getMessage();
	    assertEquals(buffer(out), buffer(in));
	    assertFalse(STUB.isClosed());
    }
    
    public void testReadMultipleMessages() throws Exception {
        Message out1 = new PingRequest((byte)1);
        Message out2 = QueryRequest.createQuery("test");
        Message out3 = new QueryReply(GUID.makeGuid(), (byte) 4, 
                                           6346, IP, 0, new Response[0],
                                           GUID.makeGuid(), new byte[0],
                                           false, false, true, true, true, false,
                                           null);
        Message out4 = new PushRequest(GUID.makeGuid(), (byte)0, GUID.makeGuid(), 0, IP, 6346);
        Message out5 = PingReply.create(GUID.makeGuid(),(byte)1, new Endpoint("1.2.3.4", 5));
        Message[] allOut = new Message[] { out1, out2, out3, out4, out5 };
        READER.setReadChannel(channel(buffer(allOut)));
	    assertEquals(0, STUB.size());
	    READER.handleRead();
	    assertEquals(5, STUB.size());
	    Message in1 = STUB.getMessage();
	    Message in2 = STUB.getMessage();
	    Message in3 = STUB.getMessage();
	    Message in4 = STUB.getMessage();
	    Message in5 = STUB.getMessage();
	    Message[] allIn = new Message[] { in1, in2, in3, in4, in5 };
	    assertEquals("out: " + out1 + ", in: " + in1, buffer(out1), buffer(in1));
	    assertEquals("out: " + out2 + ", in: " + in2, buffer(out2), buffer(in2));
	    assertEquals("out: " + out3 + ", in: " + in3, buffer(out3), buffer(in3));
	    assertEquals("out: " + out4 + ", in: " + in4, buffer(out4), buffer(in4));
	    assertEquals("out: " + out5 + ", in: " + in5, buffer(out5), buffer(in5));
	    assertEquals(buffer(allOut), buffer(allIn));
	    assertFalse(STUB.isClosed());
    }
    
    public void testBadPacketIgnored() throws Exception {
        Message out1 = new PingRequest((byte)1);
        Message out2 = QueryRequest.createQuery("test");
        Message out3 = new QueryReply(GUID.makeGuid(), (byte) 4, 
                                           6346, IP, 0, new Response[0],
                                           GUID.makeGuid(), new byte[0],
                                           false, false, true, true, true, false,
                                           null);
        Message out4 = new PushRequest(GUID.makeGuid(), (byte)0, GUID.makeGuid(), 0, IP, 6346);
        Message out5 = PingReply.create(GUID.makeGuid(),(byte)1, new Endpoint("1.2.3.4", 5));
        ByteBuffer b1 = buffer(out1);
        ByteBuffer b2 = buffer(out2);
        ByteBuffer b3 = buffer(out3);
        ByteBuffer b4 = buffer(out4);
        ByteBuffer b5 = buffer(out5);
        b2.put(18, (byte)100);   // change the hops of the query request. to be absurdly high
        READER.setReadChannel(channel(buffer(new ByteBuffer[] { b1, b2, b3, b4, b5 })));
	    assertEquals(0, STUB.size());
	    READER.handleRead();
	    assertEquals(4, STUB.size());
	    Message in1 = STUB.getMessage();
	    Message in3 = STUB.getMessage();
	    Message in4 = STUB.getMessage();
	    Message in5 = STUB.getMessage();
	    assertEquals("out: " + out1 + ", in: " + in1, buffer(out1), buffer(in1));
	    assertEquals("out: " + out3 + ", in: " + in3, buffer(out3), buffer(in3));
	    assertEquals("out: " + out4 + ", in: " + in4, buffer(out4), buffer(in4));
	    assertEquals("out: " + out5 + ", in: " + in5, buffer(out5), buffer(in5));
	    assertFalse(STUB.isClosed());
    }
    
    public void testLargeLengthThrows() throws Exception {
        Message out1 = new PingRequest((byte)1);
        Message out2 = QueryRequest.createQuery("test");
        Message out3 = new QueryReply(GUID.makeGuid(), (byte) 4, 
                                           6346, IP, 0, new Response[0],
                                           GUID.makeGuid(), new byte[0],
                                           false, false, true, true, true, false,
                                           null);
        Message out4 = new PushRequest(GUID.makeGuid(), (byte)0, GUID.makeGuid(), 0, IP, 6346);
        Message out5 = PingReply.create(GUID.makeGuid(),(byte)1, new Endpoint("1.2.3.4", 5));
        ByteBuffer b1 = buffer(out1);
        ByteBuffer b2 = buffer(out2);
        ByteBuffer b3 = buffer(out3);
        ByteBuffer b4 = buffer(out4);
        ByteBuffer b5 = buffer(out5);
        b4.order(ByteOrder.LITTLE_ENDIAN);
        b4.putInt(19, 64 * 1024 + 1);
        READER.setReadChannel(channel(buffer(new ByteBuffer[] { b1, b2, b3, b4, b5 })));
	    assertEquals(0, STUB.size());
	    try {
	        READER.handleRead();
	        fail("didn't throw IOX");
        } catch(IOException expected) {}
	    assertEquals(3, STUB.size());
	    Message in1 = STUB.getMessage();
	    Message in2 = STUB.getMessage();
	    Message in3 = STUB.getMessage();
	    assertEquals("out: " + out1 + ", in: " + in1, buffer(out1), buffer(in1));
	    assertEquals("out: " + out2 + ", in: " + in2, buffer(out2), buffer(in2));
	    assertEquals("out: " + out3 + ", in: " + in3, buffer(out3), buffer(in3));
	    // the close in real life is actually handled by NIODispatcher catching the IOX
	    // and shutting down the NIOSocket, which shuts down this MessageReader, which
	    // forwards the close to the MessageReceiver.  since we don't have that framework
	    // here, the close won't actually happen.
	    //assertTrue(STUB.isClosed());
    }
    
    public void testSmallLengthThrows() throws Exception {
        Message out1 = new PingRequest((byte)1);
        Message out2 = QueryRequest.createQuery("test");
        Message out3 = new QueryReply(GUID.makeGuid(), (byte) 4, 
                                           6346, IP, 0, new Response[0],
                                           GUID.makeGuid(), new byte[0],
                                           false, false, true, true, true, false,
                                           null);
        Message out4 = new PushRequest(GUID.makeGuid(), (byte)0, GUID.makeGuid(), 0, IP, 6346);
        Message out5 = PingReply.create(GUID.makeGuid(),(byte)1, new Endpoint("1.2.3.4", 5));
        ByteBuffer b1 = buffer(out1);
        ByteBuffer b2 = buffer(out2);
        ByteBuffer b3 = buffer(out3);
        ByteBuffer b4 = buffer(out4);
        ByteBuffer b5 = buffer(out5);
        b4.order(ByteOrder.LITTLE_ENDIAN);
        b4.putInt(19, -1);
        READER.setReadChannel(channel(buffer(new ByteBuffer[] { b1, b2, b3, b4, b5 })));
	    assertEquals(0, STUB.size());
	    try {
	        READER.handleRead();
	        fail("didn't throw IOX");
        } catch(IOException expected) {}
	    assertEquals(3, STUB.size());
	    Message in1 = STUB.getMessage();
	    Message in2 = STUB.getMessage();
	    Message in3 = STUB.getMessage();
	    assertEquals("out: " + out1 + ", in: " + in1, buffer(out1), buffer(in1));
	    assertEquals("out: " + out2 + ", in: " + in2, buffer(out2), buffer(in2));
	    assertEquals("out: " + out3 + ", in: " + in3, buffer(out3), buffer(in3));
	    // the close in real life is actually handled by NIODispatcher catching the IOX
	    // and shutting down the NIOSocket, which shuts down this MessageReader, which
	    // forwards the close to the MessageReceiver.  since we don't have that framework
	    // here, the close won't actually happen.
	    //assertTrue(STUB.isClosed());
    }
    
    
    
    private ReadableByteChannel channel(ByteBuffer buffer) throws Exception {
        return new ReadBufferChannel(buffer);
    }
    
    private ByteBuffer buffer(Message m) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.write(out);
        out.flush();
        return ByteBuffer.wrap(out.toByteArray());
    }
    
    private ByteBuffer buffer(Message m[]) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for(int i = 0; i < m.length; i++)
            m[i].write(out);
        out.flush();
        return ByteBuffer.wrap(out.toByteArray());
    }

    private ByteBuffer buffer(List ms) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for(Iterator i = ms.iterator(); i.hasNext(); )
            ((Message)i.next()).write(out);
        out.flush();
        return ByteBuffer.wrap(out.toByteArray());
    }
    
    private ByteBuffer buffer(ByteBuffer[] bufs) throws Exception {
        int length = 0;
        for(int i = 0; i < bufs.length; i++)
            length += bufs[i].limit();
        ByteBuffer combined = ByteBuffer.allocate(length);
        for(int i = 0; i < bufs.length; i++)
            combined.put(bufs[i]);
        combined.flip();
        return combined;
    }
}