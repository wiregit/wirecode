package com.limegroup.gnutella.messagehandlers;

import java.net.InetAddress;
import java.util.Random;
import java.util.Set;

import junit.framework.Test;

import org.limewire.security.InvalidSecurityTokenException;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.SecurityToken;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.settings.SearchSettings;
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
    
    public static void globalSetUp() throws Exception {
        router = new MyMessageRouter();
        new RouterService(new QueryAliveActivityCallback(), router);
        router.initialize();
    }

    public void setUp() throws Exception {
        handler = new OOBHandler(router);
        g = new GUID(GUID.makeGuid());
        address = InetAddress.getByName("1.2.3.4");
        replyHandler = new MyReplyHandler(address, 1);
    }

    public void testDropsLargeResponses() throws Exception {

        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);

        // and we request all of them
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        SecurityToken token = assertACKSent(replyHandler, 10);

        // first send back only 8 results - those should be accepted
        QueryReply reply = getReplyWithResults(g.bytes(), 8, address
                .getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertSame(reply, router.reply);

        // then send back 4 more results - those should not be accepted.
        router.reply = null;
        reply = getReplyWithResults(g.bytes(), 4, address.getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
    }

    public void testMessagesWithoutEchoedTokenAreDiscardedForDisabledOOBV2() {
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        testMessagesWithoutEchoedTokenAreHandled(true);
    }
    
    public void testMessagesWithoutEchoedTokenAreNotDiscardedForEnabledOOBV2() {
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
        testMessagesWithoutEchoedTokenAreHandled(false);
    }
    
    public void testMessagesWithoutEchoedTokenAreHandled(boolean disableOOBV2) {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);

        // and we request all of them
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        assertACKSent(replyHandler, 10);

        // first send back only 10 results without token - those should be
        // discarded
        QueryReply reply = getReplyWithResults(g.bytes(), 10, address
                .getAddress(), null);
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);
        if (disableOOBV2) {
            assertNotSame(reply, router.reply);
        }
        else {
            assertSame(reply, router.reply);
        }
    }

    public void testMessagesWithDifferentTokenAreDiscardedForDisabledOOBV2() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        testMessagesWithDifferentTokenAreHandled(true);
    }
    
    public void testMessagesWithDifferentTokenAreAcceptedForEnabeldOOBV2() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
        testMessagesWithDifferentTokenAreHandled(false);
    }
    
    public void testMessagesWithDifferentTokenAreHandled(boolean disableOOBV2)
            throws InvalidSecurityTokenException {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);

        // and we request all of them
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        assertACKSent(replyHandler, 10);

        byte[] bytes = new byte[8];
        new Random().nextBytes(bytes);
        SecurityToken tokenFake = new AddressSecurityToken(bytes);

        // send back messages with fake token
        QueryReply reply = getReplyWithResults(g.bytes(), 10, address
                .getAddress(), tokenFake);
        
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);
        
        if (disableOOBV2) {
            assertNotSame(reply, router.reply);
        }
        else {
            assertSame(reply, router.reply);
        }
    }

    
    public void testDropsUnAckedForDisabledOOBV2() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        testHandlesUnAcked(true);
    }
    
    public void testAcceptsUnAckedForEnabledOOBV2() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
        testHandlesUnAcked(false);
    }

    public void testHandlesUnAcked(boolean disableOOBV2) throws Exception {

        // send some results w/o an RNVM
        QueryReply reply = getReplyWithResults(g.bytes(), 10, address
                .getAddress(), null);
        
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);

        if (disableOOBV2) {
            // should be ignored.
            assertNull(router.reply);
        }
        else {
            assertSame(reply, router.reply);
        }
            
        // send an RNVM now
        ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);

        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        // and resend the almost same results
        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);

        // they should be forwarded to the router now
        assertSame(reply, router.reply);
    }

    public void testSessionsExpireBecauseOfAliveQuery() throws Exception {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);

        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        router.reply = null;

        // send reply, only then session objects are created
        QueryReply reply = getReplyWithResults(g.bytes(), 5, address
                .getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);

        router.reply = null;
        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token,
                5);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);

        // the next ones should be ignored since there is a full session for
        // them
        router.reply = null;
        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token,
                10);
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);

        // session is cleared by signaling that there is no query alive for it
        // anymore
        router.isAlive = false;
        handler.run();

        // message is accepted again since we pretend there is an alive query
        // for it
        router.isAlive = true;
        handler.handleMessage(reply, null, replyHandler);
        assertSame(reply, router.reply);

    }

    public void testQueryIsAliveOverridesSessionTimeout()
            throws InterruptedException {
        router.isAlive = true;
        router.timeToExpire = 50;
        router.reply = null;

        // send reply number message
        ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);

        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        router.reply = null;
        QueryReply reply = getReplyWithResults(g.bytes(), 8, address
                .getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertSame(reply, router.reply);

        router.reply = null;
        reply = getReplyWithResults(g.bytes(), 4, address.getAddress(), token,
                8);
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);

        // run timeout, but should be still discarded, since query is alive
        Thread.sleep(100);
        handler.run();

        handler.handleMessage(reply, null, replyHandler);
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
        SecurityToken token1 = assertACKSent(replyHandler, 10);

        // then we receive the second rnvm and want only 15/20
        replyHandler.m = null;
        router.numToRequest = 5;
        handler.handleMessage(second, null, replyHandler);
        SecurityToken token2 = assertACKSent(replyHandler, 5);

        // if we get back a reply with 25 results we should accept it.
        QueryReply reply = getReplyWithResults(g.bytes(), 10, address
                .getAddress(), token1);
        handler.handleMessage(reply, null, replyHandler);
        assertSame(reply, router.reply);

        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token2,
                10);
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
        QueryReply reply = getReplyWithResults(g.bytes(), 5, address
                .getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);
        router.reply = null;

        try {
            router.isAlive = false;

            handler.handleMessage(reply, null, replyHandler);

            // should be ignored.
            assertNull(router.reply);
        } finally {
            router.isAlive = true;
        }

        // send 5 other ones, marked by new offset again, now that search is
        // alive again
        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token,
                5);
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
        QueryReply reply = getReplyWithResults(g.bytes(), 5, address
                .getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);

        router.reply = null;

        // send same five and message should be discarded
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
    }

    public void testStoresBypassedResults() throws Exception {

        PrivilegedAccessor.setValue(UDPService.instance(), "_acceptedUnsolicitedIncoming", true);
        assertTrue(RouterService.canReceiveUnsolicited());
        
        // 1. case there's no route back
        GUID guid = new GUID();
        ReplyNumberVendorMessage rnvm = new ReplyNumberVendorMessage(guid, 10);
        assertTrue(rnvm.canReceiveUnsolicited());
        router.numToRequest = 10;
        router.isAlive = false;
        router.reply = null;
        
        handler.handleMessage(rnvm, null, replyHandler);
        
        assertNull(router.reply);
        Set<GUESSEndpoint> points = router.getQueryLocs(guid);
        assertFalse(points.isEmpty());
        
        GUESSEndpoint point = points.iterator().next();
        assertEquals(replyHandler.getInetAddress(), point.getInetAddress());
        assertEquals(replyHandler.getPort(), point.getPort());
        
        // 2. case we received enough results, i.e, it's more than 150 results
        guid = new GUID();
        rnvm = new ReplyNumberVendorMessage(guid, 10);
        assertTrue(rnvm.canReceiveUnsolicited());
        router.numToRequest = -1; // signals enough results have been received
        router.isAlive = true;
        router.reply = null;
        
        handler.handleMessage(rnvm, null, replyHandler);
        
        assertNull(router.reply);
        points = router.getQueryLocs(guid);
        assertFalse(points.isEmpty());
        
        point = points.iterator().next();
        assertEquals(replyHandler.getInetAddress(), point.getInetAddress());
        assertEquals(replyHandler.getPort(), point.getPort());
        
        // 3. case the first query reply comes in but the query is not alive
        guid = new GUID();
        rnvm = new ReplyNumberVendorMessage(guid, 10);
        router.numToRequest = 10;
        router.isAlive = true;
        router.reply = null;
        
        handler.handleMessage(rnvm, null, replyHandler);
        // source is not remembered yet
        assertTrue(router.getQueryLocs(guid).isEmpty());
        
        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        // send reply, only then session objects are created
        QueryReply reply = getReplyWithResults(guid.bytes(), 10, address
                .getAddress(), token);
        
        // timeout query
        router.isAlive = false;
        
        // send same five and message should be discarded
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
        
        assertFalse(router.getQueryLocs(guid).isEmpty());
        point = router.getQueryLocs(guid).iterator().next();
        assertEquals(replyHandler.getInetAddress(), point.getInetAddress());
        assertEquals(replyHandler.getPort(), point.getPort());
    }

    private static SecurityToken assertACKSent(MyReplyHandler rhandler,
            int numExpected) {
        assertNotNull(rhandler.m);
        assertTrue(rhandler.m instanceof LimeACKVendorMessage);
        LimeACKVendorMessage ack = (LimeACKVendorMessage) rhandler.m;
        assertEquals(numExpected, ack.getNumResults());
        assertNotNull(ack.getSecurityToken());
        return ack.getSecurityToken();
    }

    private QueryReply getReplyWithResults(byte[] guid, int numResults,
            byte[] addr, SecurityToken token) {
        return getReplyWithResults(guid, numResults, addr, token, 0);
    }

    private QueryReply getReplyWithResults(byte[] guid, int numResults,
            byte[] addr, SecurityToken token, int offset) {
        Response[] res = new Response[numResults];
        for (int j = 0; j < res.length; j++)
            res[j] = new Response(10, 10, "susheel" + j + offset);

        return new QueryReply(guid, (byte) 1, 1, addr, 0, res, GUID.makeGuid(),
                new byte[0], false, false, true, true, false, false, true,
                null, token);
    }

    private static class MyMessageRouter extends MessageRouterStub {
        volatile QueryReply reply;

        volatile int numToRequest;

        volatile long timeToExpire;

        volatile boolean isAlive = true;

        @Override
        public int getNumOOBToRequest(ReplyNumberVendorMessage reply) {
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

        @Override
        public boolean isQueryAlive(GUID guid) {
            return isAlive;
        }
    }
    
    private static class QueryAliveActivityCallback extends ActivityCallbackStub {
        @Override
        public boolean isQueryAlive(GUID guid) {
            return true;
        }
    }

    private static class MyReplyHandler extends ReplyHandlerStub {
        volatile Message m;

        final InetAddress addr;

        final int port;

        MyReplyHandler(InetAddress addr, int port) {
            this.addr = addr;
            this.port = port;
        }

        public void reply(Message m) {
            this.m = m;
        }
    }
}
