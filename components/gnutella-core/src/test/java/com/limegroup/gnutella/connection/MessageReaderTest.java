package com.limegroup.gnutella.connection;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;

import junit.framework.Test;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.util.*;

/**
 * Tests that MessageReader extracts messages from a source channel correctly.
 */
public final class MessageReaderTest extends BaseTestCase {
    
    private StubMessageReceiver STUB = new StubMessageReceiver();
    private MessageReader READER = new MessageReader(STUB);

	public MessageReaderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(MessageReaderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void setUp() {
	    STUB.clear();
	}
	
	public void testSingleMessageRead() throws Exception {
	    Message out = new PingRequest((byte)1);
	    READER.setReadChannel(channel(buffer(out)));
	    assertEquals(0, STUB.size());
	    READER.handleRead();
	    assertEquals(1, STUB.size());
	    Message in = STUB.getMessage();
	    assertEquals(buffer(out), buffer(in));
    }
    
    private ReadableByteChannel channel(ByteBuffer buffer) throws Exception {
        return new ReadBufferChannel(buffer);
    }
    
    private ByteBuffer buffer(Message m) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.write(out);
        return ByteBuffer.wrap(out.toByteArray());
    }
}