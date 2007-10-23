package com.limegroup.gnutella.version;

import java.io.ByteArrayOutputStream;
import java.io.File;

import junit.framework.Test;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.PeerTestCase;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMStubHelper;
import com.limegroup.gnutella.messages.vendor.UpdateRequest;
import com.limegroup.gnutella.messages.vendor.UpdateResponse;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.settings.UpdateSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Tests to make sure updates are sent, requested, etc...
 */
public class InterClientTest extends PeerTestCase {
    
    private BlockingConnection PEER;
    
    public InterClientTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(InterClientTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    static UpdateRequest dummy = new UpdateRequest();
    public void setUp() throws Exception {
        super.setUp();
        setEmpty();
        PEER = connect(true);
        drain(PEER);
        doInitialExchange();
    }
    
    public void tearDown() throws Exception {
        super.tearDown();
        if(PEER != null)
            PEER.close();
    }
    
    /**
     * Simple test to make sure that if someone sends a CapabilitiesVM
     * with a greater version than we have, that we request the new version.
     * And that we don't request if someone doesn't send it.
     */
    public void testRequestIsSent() throws Exception {
        assertEquals(0, ProviderHacks.getUpdateHandler().getLatestId());
        
        PEER.send(getCVM(1));
        PEER.flush();
        
        // We should get an UpdateRequest.
        Message m = getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNotNull(m);
        assertInstanceof(UpdateRequest.class, m);
        
        setCurrentId(10);
        assertEquals(10, ProviderHacks.getUpdateHandler().getLatestId());
        PEER.send(getCVM(5));
        PEER.flush();
        
        // we shouldn't get a message, since they said they had 5 & we know of 10.
        m = getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNull(m);
        
        // Now if they send with 11, we'll request.
        PEER.send(getCVM(11));
        PEER.flush();
        m = getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNotNull(m);
        assertInstanceof(UpdateRequest.class, m);
    }
    
    /**
     * Tests that a response is sent in response to a rquest.
     */
    public void testResponseIsSent() throws Exception {
        assertEquals(0, ProviderHacks.getUpdateHandler().getLatestId());
        
        // We should get no response, since we have no data to give.
        PEER.send(new UpdateRequest());
        PEER.flush();
        Message m = getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNull(m);
        
        // Alright, set some current bytes so we can do some testing.
        byte[] data = setCurrent(-10);
        PEER.send(new UpdateRequest());
        PEER.flush();
        m = getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        assertInstanceof(UpdateResponse.class, m);
        assertEquals(data, payload(m));
    }
    
    /**
     * Tests that valid versions are used.
     */
    public void testValidVersion() throws Exception {
        setCurrentId(-11);
        assertEquals(-11, ProviderHacks.getUpdateHandler().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-10, ProviderHacks.getUpdateHandler().getLatestId());
        
        // Make sure we got a new CapabilitiesVM.
        Message m = getFirstInstanceOfMessageType(PEER, CapabilitiesVM.class);
        assertNotNull(m);
        // TODO: is 65526 right?
        assertEquals(65526, ((CapabilitiesVM)m).supportsUpdate());
    }
    
    /**
     * Tests that older versions are ignored.
     */
    public void testOlderVersionsIgnored() throws Exception {
        setCurrentId(-9);
        assertEquals(-9, ProviderHacks.getUpdateHandler().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-9, ProviderHacks.getUpdateHandler().getLatestId());        
    }
    
    /**
     * Test that invalid signatures are ignored.
     */
    public void testInvalidSignaturesIgnored() throws Exception {
        setCurrentId(-11);
        assertEquals(-11, ProviderHacks.getUpdateHandler().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        b[0] = '0'; // break the sig.
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-11, ProviderHacks.getUpdateHandler().getLatestId());
    }
    
    /**
     * Tests that invalid bytes break verification.
     */
    public void testInvalidBytesIgnored() throws Exception {
        setCurrentId(-11);
        assertEquals(-11, ProviderHacks.getUpdateHandler().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        b[b.length-1] = '0'; // break the data.
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-11, ProviderHacks.getUpdateHandler().getLatestId());
    }
    
    /**
     * Test invalid XML ignored.
     */
    public void testInvalidXMLIgnored() throws Exception {
        setCurrentId(-11);
        assertEquals(-11, ProviderHacks.getUpdateHandler().getLatestId());
        
        // Get the -9 file.
        byte[] b = readFile(-9);
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-11, ProviderHacks.getUpdateHandler().getLatestId());
    }
    
    /**
     * Tests that updates are sent out when versions come in.
     */
    public void testUpdatesSentToGUI() throws Exception {
        setCurrentId(-11);
        assertEquals(-11, ProviderHacks.getUpdateHandler().getLatestId());
        
        MyActivityCallback cb = (MyActivityCallback)ProviderHacks.getActivityCallback();
        cb.lastUpdate = null;
        
        // Get the -8 file.
        byte[] b = readFile(-8);
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-8, ProviderHacks.getUpdateHandler().getLatestId());
        // since version still == @version@, it wasn't sent to the callback.
        assertNull(cb.lastUpdate);
        
        setEmpty();
        setCurrentId(-11);
        assertEquals(-11, ProviderHacks.getUpdateHandler().getLatestId());
        setVersion("3.0.0");

        // Set the update style to zero to ensure the message is not ignored
        UpdateSettings.UPDATE_STYLE.setValue(0);
        
        // Get the -8 file.
        b = readFile(-8);
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-8, ProviderHacks.getUpdateHandler().getLatestId());
        assertNotNull(cb.lastUpdate);
    }    
    
    public void testCompressedResponse() throws Exception {
        assertEquals(0, ProviderHacks.getUpdateHandler().getLatestId());
        
        // We should get no response, since we have no data to give.
        UpdateRequestStub request = new UpdateRequestStub(2,true,true);
        PEER.send(new UpdateRequest());
        PEER.flush();
        Message m = getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNull(m);
        
        // Alright, set some current bytes so we can do some testing.
        byte[] data = setCurrent(-10);
        PEER.send(request);
        PEER.flush();
        m = getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        assertInstanceof(UpdateResponse.class, m);
        byte [] payload = payload(m);
        GGEP g = new GGEP(payload,0,null);
        assertEquals(g.getBytes("C"),data);
        assertFalse(g.hasKey("U"));
    }
    
    public void testUncompressedGGEPResponse() throws Exception {
        assertEquals(0, ProviderHacks.getUpdateHandler().getLatestId());
        
        // We should get no response, since we have no data to give.
        PEER.send(new UpdateRequest());
        PEER.flush();
        Message m = getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNull(m);
        
        UpdateRequestStub request = new UpdateRequestStub(2,true,false);
        byte[] data = setCurrent(-10);
        PEER.send(request);
        PEER.flush();
        m = getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        assertInstanceof(UpdateResponse.class, m);
        
        byte [] payload = payload(m);
        GGEP g = new GGEP(payload,0,null);
        assertEquals(g.getBytes("U"),data);
        assertFalse(g.hasKey("C"));
    }
    
    /**
     * tests that a request with higher version w/o GGEP gets responded to properly
     */
    public void testHigherVersionNoGGEP() throws Exception {
        assertEquals(0, ProviderHacks.getUpdateHandler().getLatestId());
        
        // We should get no response, since we have no data to give.
        PEER.send(new UpdateRequest());
        PEER.flush();
        Message m = getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNull(m);
        
        UpdateRequestStub request = new UpdateRequestStub(2,false,false);
        byte[] data = setCurrent(-10);
        PEER.send(request);
        PEER.flush();
        m = getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        assertEquals(data,payload(m));
    }
    
    private static void setCurrentId(int i) throws Exception {
        PrivilegedAccessor.setValue(ProviderHacks.getUpdateHandler(), "_lastId", new Integer(i));
    }
    
    private static void setCurrentBytes(byte[] b) throws Exception {
        PrivilegedAccessor.setValue(ProviderHacks.getUpdateHandler(), "_lastBytes", b);
    }
    
    private static void setCurrentInfo(UpdateInformation info) throws Exception {
        PrivilegedAccessor.setValue(ProviderHacks.getUpdateHandler(), "_updateInfo", null);
    }
    
    private static byte[] setCurrent(int i) throws Exception {
        setCurrentId(i);
        byte[] b = readFile(i);
        assertNotNull(b);
        setCurrentBytes(b);
        return b;
    }
    
    private static void setEmpty() throws Exception {
        setCurrentId(0);
        setCurrentBytes(null);
        setCurrentInfo(null);
    }
    
    private static byte[] readFile(int i) throws Exception {
        File f = CommonUtils.getResourceFile("com/limegroup/gnutella/version/test_" + i + ".xml");
        assertTrue(f.exists());
        assertTrue(f.isFile());
        return FileUtils.readFileFully(f);
    }
    
    private static void setVersion(String v) throws Exception {
        PrivilegedAccessor.setValue(LimeWireUtils.class, "testVersion", v);
    }
    
    private static byte[] payload(Message m) throws Exception {
        assertInstanceof("not a vendor message!", VendorMessage.class, m);
        return (byte[])PrivilegedAccessor.invokeMethod(m, "getPayload");
    }
    
    private static Message getCVM(int i) throws Exception {
        return CapabilitiesVMStubHelper.makeUpdateVM(i);
    }
    
    private void doInitialExchange() throws Exception {
        PEER.send(getCVM(0));
        PEER.flush();
        assertNotNull(getFirstInstanceOfMessageType(PEER, UpdateRequest.class));
    }

    /* Required for PeerTestCase. */
    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    /* Required for PeerTestCase. */
    private static class MyActivityCallback extends ActivityCallbackStub {
        UpdateInformation lastUpdate;
        
        public void updateAvailable(UpdateInformation info) {
            lastUpdate = info;
        }
    }
    
    static byte [] derivePayload(boolean hasGGEP, boolean requestsCompressed) throws Exception {
        if (!hasGGEP)
            return DataUtils.EMPTY_BYTE_ARRAY;
        
        GGEP g = new GGEP();
        if (requestsCompressed)
            g.put("C");
        else
            g.put("X"); //put something else
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        g.write(baos);
        return baos.toByteArray();
    }
    
    private static class UpdateRequestStub extends VendorMessage{

        public int version;
        public boolean hasGGEP,requestsCompressed;
        
        public UpdateRequestStub(int version, boolean hasGGEP, boolean requestsCompressed) 
        throws Exception {
            super(F_LIME_VENDOR_ID, F_UPDATE_REQ, version, derivePayload(hasGGEP, requestsCompressed));
            this.version = version;
            this.hasGGEP = hasGGEP;
            this.requestsCompressed = requestsCompressed;
        }
        
        public int getVersion() {
            return version;
        }

        public boolean hasGGEP() {
            return hasGGEP;
        }

        public boolean requestsCompressed() {
            return requestsCompressed;
        }
        
    }
}

