package com.limegroup.gnutella.version;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.util.*;

/**
 * Tests to make sure updates are sent, requested, etc...
 */
public class InterClientTest extends ClientSideTestCase {
    public InterClientTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(InterClientTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() throws Exception {
        setEmpty();
    }
    
    /**
     * Simple test to make sure that if someone sends a CapabilitiesVM
     * with a greater version than we have, that we request the new version.
     * And that we don't request if someone doesn't send it.
     */
    public void testRequestIsSent() throws Exception {
        drain(testUP[0]);
        
        assertEquals(0, UpdateHandler.instance().getLatestId());
        
        testUP[0].send(getCVM(1));
        testUP[0].flush();
        
        // We should get an UpdateRequest.
        Message m = getFirstInstanceOfMessageType(testUP[0], UpdateRequest.class);
        assertNotNull(m);
        assertInstanceof(UpdateRequest.class, m);
        
        setCurrentId(10);
        assertEquals(10, UpdateHandler.instance().getLatestId());
        testUP[0].send(getCVM(5));
        testUP[0].flush();
        
        // we shouldn't get a message, since they said they had 5 & we know of 10.
        m = getFirstInstanceOfMessageType(testUP[0], UpdateRequest.class);
        assertNull(m);
        
        // Now if they send with 11, we'll request.
        testUP[0].send(getCVM(11));
        testUP[0].flush();
        m = getFirstInstanceOfMessageType(testUP[0], UpdateRequest.class);
        assertNotNull(m);
        assertInstanceof(UpdateRequest.class, m);
    }
    
    /**
     * Tests that a response is sent in response to a rquest.
     */
    public void testResponseIsSent() throws Exception {
        drain(testUP[0]);
        
        assertEquals(0, UpdateHandler.instance().getLatestId());
        
        // We should get no response, since we have no data to give.
        testUP[0].send(new UpdateRequest());
        testUP[0].flush();
        Message m = getFirstInstanceOfMessageType(testUP[0], UpdateResponse.class);
        assertNull(m);
        
        // Alright, set some current bytes so we can do some testing.
        byte[] data = setCurrent(-10);
        testUP[0].send(new UpdateRequest());
        testUP[0].flush();
        m = getFirstInstanceOfMessageType(testUP[0], UpdateResponse.class);
        assertNotNull(m);
        assertInstanceof(UpdateResponse.class, m);
        assertEquals(data, payload(m));
    }
    
    /**
     * Tests that valid versions are used.
     */
    public void testValidVersion() throws Exception {
        drain(testUP[0]);
        
        setCurrentId(-11);
        assertEquals(-11, UpdateHandler.instance().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        testUP[0].send(new UpdateResponse(b));
        testUP[0].flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-10, UpdateHandler.instance().getLatestId());
        
        // Make sure we got a new CapabilitiesVM.
        Message m = getFirstInstanceOfMessageType(testUP[0], CapabilitiesVM.class);
        assertNotNull(m);
        // TODO: is 65526 right?
        assertEquals(65526, ((CapabilitiesVM)m).supportsUpdate());
    }
    
    /**
     * Tests that older versions are ignored.
     */
    public void testOlderVersionsIgnored() throws Exception {
        drain(testUP[0]);
        
        setCurrentId(-9);
        assertEquals(-9, UpdateHandler.instance().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        testUP[0].send(new UpdateResponse(b));
        testUP[0].flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-9, UpdateHandler.instance().getLatestId());        
    }
    
    /**
     * Test that invalid signatures are ignored.
     */
    public void testInvalidSignaturesIgnored() throws Exception {
        drain(testUP[0]);
        
        setCurrentId(-11);
        assertEquals(-11, UpdateHandler.instance().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        b[0] = '0'; // break the sig.
        testUP[0].send(new UpdateResponse(b));
        testUP[0].flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-11, UpdateHandler.instance().getLatestId());
    }
    
    /**
     * Tests that invalid bytes break verification.
     */
    public void testInvalidBytesIgnored() throws Exception {
        drain(testUP[0]);
        
        setCurrentId(-11);
        assertEquals(-11, UpdateHandler.instance().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        b[b.length-1] = '0'; // break the data.
        testUP[0].send(new UpdateResponse(b));
        testUP[0].flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-11, UpdateHandler.instance().getLatestId());
    }
    
    /**
     * Test invalid XML ignored.
     */
    public void testInvalidXMLIgnored() throws Exception {
        drain(testUP[0]);
        
        setCurrentId(-11);
        assertEquals(-11, UpdateHandler.instance().getLatestId());
        
        // Get the -9 file.
        byte[] b = readFile(-9);
        testUP[0].send(new UpdateResponse(b));
        testUP[0].flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-11, UpdateHandler.instance().getLatestId());
    }
    
    /**
     * Tests that updates are sent out when versions come in.
     */
    public void testUpdatesSentToGUI() throws Exception {
        drain(testUP[0]);
        
        setCurrentId(-11);
        assertEquals(-11, UpdateHandler.instance().getLatestId());
        
        MyActivityCallback cb = (MyActivityCallback)RouterService.getCallback();
        cb.lastUpdate = null;
        
        // Get the -8 file.
        byte[] b = readFile(-8);
        testUP[0].send(new UpdateResponse(b));
        testUP[0].flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-8, UpdateHandler.instance().getLatestId());
        // since version still == @version@, it wasn't sent to the callback.
        assertNull(cb.lastUpdate);
        
        setEmpty();
        setCurrentId(-11);
        assertEquals(-11, UpdateHandler.instance().getLatestId());
        setVersion("3.0.0");

        
        // Get the -8 file.
        b = readFile(-8);
        testUP[0].send(new UpdateResponse(b));
        testUP[0].flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-8, UpdateHandler.instance().getLatestId());
        assertNotNull(cb.lastUpdate);
    }    
    
    private static void setCurrentId(int i) throws Exception {
        PrivilegedAccessor.setValue(UpdateHandler.instance(), "_lastId", new Integer(i));
    }
    
    private static void setCurrentBytes(byte[] b) throws Exception {
        PrivilegedAccessor.setValue(UpdateHandler.instance(), "_lastBytes", b);
    }
    
    private static void setCurrentInfo(UpdateInformation info) throws Exception {
        PrivilegedAccessor.setValue(UpdateHandler.instance(), "_updateInfo", null);
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
        PrivilegedAccessor.setValue(CommonUtils.class, "testVersion", v);
    }
    
    private static byte[] payload(Message m) throws Exception {
        assertInstanceof("not a vendor message!", VendorMessage.class, m);
        return (byte[])PrivilegedAccessor.invokeMethod(m, "getPayload", null);
    }
    
    private static Message getCVM(int i) throws Exception {
        return CapabilitiesVMStubHelper.makeUpdateVM(i);
    }

    //////////////////////////////////////////////////////////////////
    // method for ClientSideTestCase.

    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    private static class MyActivityCallback extends ActivityCallbackStub {
        UpdateInformation lastUpdate;
        
        public void updateAvailable(UpdateInformation info) {
            lastUpdate = info;
        }
    }
}

