package com.limegroup.gnutella.messagehandlers;

import java.net.InetAddress;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class OOBHandlerTest extends LimeTestCase {

	public OOBHandlerTest(String name) {
		super(name);
	}

    public static Test suite() {
        return buildTestSuite(OOBHandlerTest.class);
    }    
    
    static MyMessageRouter router;
    static OOBHandler handler;
    static GUID g;
    static InetAddress address;
    static MyReplyHandler replyHandler;
    
    public void setUp() throws Exception {
    	router = new MyMessageRouter();
    	handler = new OOBHandler(router);
    	g = new GUID(GUID.makeGuid());
    	address = InetAddress.getByName("1.2.3.4");
    	replyHandler = new MyReplyHandler(address,1);
    }
    
    public void testDropsLargeResponses() throws Exception {
    	
    	// a host claims to have 10 results
    	ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
    	
    	// and we request all of them
    	router.numToRequest = 10;
    	handler.handleMessage(rnvm,null,replyHandler);
    	assertACKSent(replyHandler, 10);
    	
    	// first send back only 8 results - those should be accepted
    	QueryReply reply = getReplyWithResults(g.bytes(),8,address.getAddress());
    	handler.handleMessage(reply, null, replyHandler);
    	assertSame(reply,router.reply);
    	
    	// then send back 4 more results - those should not be accepted.
    	router.reply = null;
    	reply = getReplyWithResults(g.bytes(), 4, address.getAddress());
    	handler.handleMessage(reply, null, replyHandler);
    	assertNull(router.reply);
    }
    
    public void testDropsUnAcked() throws Exception {
    	
    	// send some results w/o an RNVM
    	QueryReply reply = getReplyWithResults(g.bytes(), 10, address.getAddress());
    	handler.handleMessage(reply, null, replyHandler);
    	
    	// should be ignored.
    	assertNull(router.reply);
    	
    	// send an RNVM now
    	ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
    	router.numToRequest = 10;
    	handler.handleMessage(rnvm, null, replyHandler);
    	
    	// double check we sent back an ack
    	assertACKSent(replyHandler, 10);
    	
    	// and resend the same results
    	handler.handleMessage(reply, null, replyHandler);
    	
    	// they should be forwarded to the router now
    	assertSame(router.reply, reply);
    }
    
    public void testSessionsExpire() throws Exception {
    	// a host claims to have 10 results
    	ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
    	router.numToRequest = 10;
    	handler.handleMessage(rnvm, null, replyHandler);
    	
    	// double check we sent back an ack
    	assertACKSent(replyHandler, 10);
    	
    	// but they don't send them back in time
    	router.timeToExpire = 50;
    	Thread.sleep(100);
    	handler.run();
    	QueryReply reply = getReplyWithResults(g.bytes(), 10, address.getAddress());
    	handler.handleMessage(reply, null, replyHandler);
    	
    	// should be ignored.
    	assertNull(router.reply);
    }
    
    public void testAddsRNVMs() throws Exception {
    	
    	// a host claims to have 10 results at first
    	ReplyNumberVendorMessage first = new ReplyNumberVendorMessage(g, 10);
    	// and then 20 afterwards
    	ReplyNumberVendorMessage second = new ReplyNumberVendorMessage(g, 10);
    	
    	// we would like to get 10 
    	router.numToRequest = 10; 
    	
    	// first we receive the message saying they have 10
    	handler.handleMessage(first, null, replyHandler);
    	
    	// we sould respond we want them
    	assertACKSent(replyHandler, 10);
    	
    	// then we receive the second rnvm and want only 15/20
    	replyHandler.m = null;
    	router.numToRequest = 15;
    	handler.handleMessage(second, null, replyHandler);
    	assertACKSent(replyHandler,15);
    	
    	// if we get back a reply with 25 results we should accept it.
    	QueryReply reply = getReplyWithResults(g.bytes(), 25, address.getAddress());
    	handler.handleMessage(reply, null, replyHandler);
    	assertSame(router.reply,reply);
    }
    
    private static void assertACKSent(MyReplyHandler rhandler, int numExpected) {
    	assertNotNull(rhandler.m);
    	assertTrue (rhandler.m instanceof LimeACKVendorMessage);
    	LimeACKVendorMessage ack = (LimeACKVendorMessage)rhandler.m;
    	assertEquals(numExpected, ack.getNumResults());
    }
    
    private QueryReply getReplyWithResults(byte[] guid, int numResults,byte [] addr) {
    	Response[] res = new Response[numResults];
    	for (int j = 0; j < res.length; j++)
    		res[j] = new Response(10, 10, "susheel"+j);
    	return new QueryReply(guid, (byte) 1, 1, addr, 0, res,
    				GUID.makeGuid(), new byte[0], false, false, true,
    				true, false, false, null);
    	
    }
    
    private static class MyMessageRouter extends MessageRouterStub {
    	volatile QueryReply reply;
    	volatile int numToRequest;
    	volatile long timeToExpire;
		@Override
		public int getNumOOBToRequest(ReplyNumberVendorMessage reply, ReplyHandler handler) {
			return numToRequest;
		}
		@Override
		public long getOOBExpireTime() {
			return timeToExpire;
		}
		@Override
		public void handleQueryReply(QueryReply queryReply, ReplyHandler handler) {
			this.reply = queryReply;
		}
    }
    
    private static class MyReplyHandler extends ReplyHandlerStub {
    	volatile Message m;
    	final InetAddress addr;
    	final int port;
    	MyReplyHandler(InetAddress addr, int port){
    		this.addr = addr;
    		this.port = port;
    	}
    	public void reply (Message m) {
    		this.m = m;
    	}
    }
}
