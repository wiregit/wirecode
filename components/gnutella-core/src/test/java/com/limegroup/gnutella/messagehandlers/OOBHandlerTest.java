package com.limegroup.gnutella.messagehandlers;

import java.net.InetAddress;
import java.util.Random;

import junit.framework.Test;

import org.limewire.security.InvalidSecurityTokenException;
import org.limewire.security.QueryKey;
import org.limewire.security.SecurityToken;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
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
    static QueryAliveCallback callback;
    
    public void setUp() throws Exception {
        callback = new QueryAliveCallback();
        PrivilegedAccessor.setValue(RouterService.class, "callback", callback);
        PrivilegedAccessor.setValue(RouterService.class , "downloadManager", new QueryAliveDownloadManager());
        router = new MyMessageRouter();
        router.initialize();
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
    	SecurityToken token = assertACKSent(replyHandler, 10);
    	
    	// first send back only 8 results - those should be accepted
    	QueryReply reply = getReplyWithResults(g.bytes(),8,address.getAddress(), token);
    	handler.handleMessage(reply, null, replyHandler);
    	assertSame(reply,router.reply);
    	
    	// then send back 4 more results - those should not be accepted.
    	router.reply = null;
    	reply = getReplyWithResults(g.bytes(), 4, address.getAddress(), token);
    	handler.handleMessage(reply, null, replyHandler);
    	assertNull(router.reply);
    }
    
    public void testMessagesWithoutEchoedTokenAreDiscarded() {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
        
        // and we request all of them
        router.numToRequest = 10;
        handler.handleMessage(rnvm,null,replyHandler);
        assertACKSent(replyHandler, 10);
        
        // first send back only 10 results without token - those should be discarded
        QueryReply reply = getReplyWithResults(g.bytes(), 10, address.getAddress(), null);
        handler.handleMessage(reply, null, replyHandler);
        assertNotSame(reply, router.reply);
    }
    
    public void testMessagesWithDifferentTokenAreDiscarded() throws InvalidSecurityTokenException {
        //a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
        
        // and we request all of them
        router.numToRequest = 10;
        handler.handleMessage(rnvm,null,replyHandler);
        assertACKSent(replyHandler, 10);
        
        
        byte[] bytes = new byte[8];
        new Random().nextBytes(bytes);
        SecurityToken tokenFake = new QueryKey(bytes);
        
        // send back messages with fake token
        QueryReply reply = getReplyWithResults(g.bytes(), 10, address.getAddress(), tokenFake);
        handler.handleMessage(reply, null, replyHandler);
        assertNotSame(reply, router.reply);
    }
    
    public void testDropsUnAcked() throws Exception {
    	
    	// send some results w/o an RNVM
    	QueryReply reply = getReplyWithResults(g.bytes(), 10, address.getAddress(), null);
    	handler.handleMessage(reply, null, replyHandler);
    	
    	// should be ignored.
    	assertNull(router.reply);
    	
    	// send an RNVM now
    	ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
    	router.numToRequest = 10;
    	handler.handleMessage(rnvm, null, replyHandler);
    	
    	// double check we sent back an ack
    	SecurityToken token = assertACKSent(replyHandler, 10);
    	
    	// and resend the almost same results
        reply = getReplyWithResults(g.bytes(),5, address.getAddress(), token);
    	handler.handleMessage(reply, null, replyHandler);
    	
    	// they should be forwarded to the router now
    	assertSame(reply, router.reply);
    }
    
    public void testSessionsExpire() throws Exception {
    	// a host claims to have 10 results
    	ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
    	router.numToRequest = 10;
    	handler.handleMessage(rnvm, null, replyHandler);
    	
    	// double check we sent back an ack
    	SecurityToken token = assertACKSent(replyHandler, 10);
    	
        router.reply = null;
        
        // send reply, only then session objects are created
        QueryReply reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);
        
        router.reply = null;
        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token, 5);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);

        // the next ones should be ignored since there is a full session for them
        router.reply = null;
        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token, 10);
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
        
        // session is cleared by signaling that there is no query alive for it anymore
        callback.isAlive = false;
        handler.run();

        // message is accepted again since we pretend there is an alive query for it
        callback.isAlive = true;
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);
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
    	SecurityToken token1 = assertACKSent(replyHandler, 10);
    	
    	// then we receive the second rnvm and want only 15/20
    	replyHandler.m = null;
    	router.numToRequest = 5;
    	handler.handleMessage(second, null, replyHandler);
    	SecurityToken token2 = assertACKSent(replyHandler, 5);
    	
    	// if we get back a reply with 25 results we should accept it.
    	QueryReply reply = getReplyWithResults(g.bytes(), 10 , address.getAddress(), token1);
    	handler.handleMessage(reply, null, replyHandler);
    	assertSame(reply, router.reply);
        
        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token2, 10);
        handler.handleMessage(reply, null, replyHandler);
        assertSame(reply, router.reply);
    }
    
    public void testDiscardedWithoutAliveQuery() throws InterruptedException {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        
        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);
        
        // send reply, only then session objects are created
        QueryReply reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);
        router.reply = null;
     
        try {
            callback.isAlive = false;
            
            handler.handleMessage(reply, null, replyHandler);
            
            // should be ignored.
            assertNull(router.reply);
        }
        finally {
            callback.isAlive = true;
        }
        
        // send 5 other ones, marked by new offset again, now that search is alive again
        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token, 5);
        handler.handleMessage(reply, null, replyHandler);
        assertSame(reply, router.reply);
        
        // and now send the old ones again after expiration that does nothing
        handler.run();
        
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
    }
    
    public void testDuplicatePacketsIgnored() {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        
        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);
        
        // send reply, only then session objects are created
        QueryReply reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);
        
        router.reply = null;
        
        // send same five and message should be discarded
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
    }
    
    private static SecurityToken assertACKSent(MyReplyHandler rhandler, int numExpected) {
    	assertNotNull(rhandler.m);
    	assertTrue (rhandler.m instanceof LimeACKVendorMessage);
    	LimeACKVendorMessage ack = (LimeACKVendorMessage)rhandler.m;
    	assertEquals(numExpected, ack.getNumResults());
        assertNotNull(ack.getSecurityToken());
        return ack.getSecurityToken();
    }
    
    private QueryReply getReplyWithResults(byte[] guid, int numResults,byte [] addr, SecurityToken token) {
        return getReplyWithResults(guid, numResults, addr, token, 0);
    }
    
    private QueryReply getReplyWithResults(byte[] guid, int numResults,byte [] addr, SecurityToken token, int offset) {
        Response[] res = new Response[numResults];
        for (int j = 0; j < res.length; j++)
            res[j] = new Response(10, 10, "susheel"+j+offset);

        return new QueryReply(guid, (byte)1,
                1, addr, 0, res, 
                GUID.makeGuid(), new byte[0],
                false, false,
                true, true, false,
                false, true, null, token);
    }
    
    private static class MyMessageRouter extends MessageRouterStub {
    	volatile QueryReply reply;
    	volatile int numToRequest;

		@Override
		public int getNumOOBToRequest(ReplyNumberVendorMessage reply, ReplyHandler handler) {
			return numToRequest;
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
    
    private static class QueryAliveCallback extends ActivityCallbackStub {
        
        private boolean isAlive = true;
        
        @Override
        public boolean isQueryAlive(GUID guid) {
            return isAlive;
        }
    }
    
    private static class QueryAliveDownloadManager extends DownloadManagerStub {
        @Override
        public synchronized boolean isGuidForQueryDownloading(GUID guid) {
            return false;
        }
    }
}
