package com.limegroup.gnutella;

import java.net.InetAddress;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessageStubHelper;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 * First ultrapeer supports new out-of-band proxying control protocol.
 * 
 */
public class ClientSideOOBProxyControlTest extends ClientSideTestCase {

    public ClientSideOOBProxyControlTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideOOBProxyControlTest.class);
    }

    @SuppressWarnings("unused")
    private static void doSettings() {
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
    }
    
    public static Integer numUPs() {
        return new Integer(2);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    
    public void testQueryV2Disabled() throws Exception {
        
        drainAll();
        
        // send client a message that we support proxy control for OOB v3
        testUP[0].send(MessagesSupportedVendorMessage.instance());
        testUP[0].flush();
        
        // send client a message that we support leaf guidance and everything
        // but the proxy control protocol for OOB v3
        testUP[1].send(MessagesSupportedVendorMessageStubHelper.makeMSVMWithoutOOBProxyControl());
        testUP[1].flush();
        
        Thread.sleep(250);
        
        QueryRequest query = QueryRequest.createQuery("txt");
        assertFalse(query.desiresOutOfBandReplies());
        assertFalse(query.doNotProxy());
        
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        
        // send query through router service
        PrivilegedAccessor.invokeMethod(RouterService.class, "recordAndSendQuery",
                query, MediaType.getDocumentMediaType());
        
        Thread.sleep(250);
        
        // new ultrapeer should not get do_not_proxy
        assertQueryNoOOB(testUP[0], query, false);
        
        // old ultrapper should get do_not_proxy
        assertQueryNoOOB(testUP[1], query, true);
    }
    
    
    public void testOOBQueryV2Disabled()  throws Exception {
        
        drainAll();
        
        QueryRequest query = QueryRequest.createOutOfBandQuery("txt", InetAddress.getLocalHost().getAddress(), 1340);
        
        assertTrue(query.desiresOutOfBandReplies());
        assertFalse(query.doNotProxy());
        
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        
        // send query through router service
        PrivilegedAccessor.invokeMethod(RouterService.class, "recordAndSendQuery",
                query, MediaType.getDocumentMediaType());
        Thread.sleep(250);
        
        // new ultrapeer should not get do_not_proxy
        assertQuery(testUP[0], query, false, true);
        
        // old ultrapper get do_not_proxy 
        assertQuery(testUP[1], query, true, true);
    }
    
    public void testOOBQueryV2Enabled()  throws Exception {
        
        drainAll();
        
        QueryRequest query = QueryRequest.createOutOfBandQuery("txt", InetAddress.getLocalHost().getAddress(), 1340);
        
        assertTrue(query.desiresOutOfBandReplies());
        assertFalse(query.doNotProxy());
        
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
        
        // send query through router service
        PrivilegedAccessor.invokeMethod(RouterService.class, "recordAndSendQuery",
                query, MediaType.getDocumentMediaType());
        Thread.sleep(250);
        
        // new ultrapeer should not get do_not_proxy
        assertQuery(testUP[0], query, false, true);
        
        // old ultrapper should not get do_not_proxy either
        assertQuery(testUP[1], query, false, true);
    }
    
    public void testQueryV2Enabled() throws Exception {
        
        drainAll();
        
        QueryRequest query = QueryRequest.createQuery("txt");
        assertFalse(query.desiresOutOfBandReplies());
        assertFalse(query.doNotProxy());
        
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
        
        // send query through router service
        PrivilegedAccessor.invokeMethod(RouterService.class, "recordAndSendQuery",
                query, MediaType.getDocumentMediaType());
        Thread.sleep(250);
        
        // new ultrapeer should not get do_not_proxy
        assertQueryNoOOB(testUP[0], query, false);
        
        // old ultrapper shouldn't get do_not_proxy either
        assertQueryNoOOB(testUP[1], query, false);
    }
    
    private QueryRequest assertQuery(Connection c, QueryRequest query, boolean doNotProxy,
            boolean desiresOOB) throws BadPacketException {
        QueryRequest qr = getFirstInstanceOfMessageType(c, QueryRequest.class);
        assertNotNull(qr);
        assertEquals(query.getGUID(), qr.getGUID());
        assertEquals(doNotProxy, qr.doNotProxy());
        assertEquals(desiresOOB, qr.desiresOutOfBandReplies());
        return qr;
    }
    
    private QueryRequest assertQueryNoOOB(Connection c, QueryRequest query, boolean doNotProxy) throws BadPacketException {
        return assertQuery(c, query, doNotProxy, false);
    }

}
