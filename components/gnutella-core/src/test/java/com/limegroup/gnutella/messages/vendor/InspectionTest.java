package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

import junit.framework.Test;

import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.io.IpPortImpl;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.security.Verifier;
import org.limewire.util.ByteOrder;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.ServerSideTestCase;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

@SuppressWarnings("unused")
public class InspectionTest extends ServerSideTestCase {

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;
    
    
    public InspectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(InspectionTest.class);
    }
    public static Integer numUPs() {
        return new Integer(1);
    }

    public static Integer numLeaves() {
        return new Integer(2);
    }
    
    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    public static void setUpQRPTables() throws Exception {}
    public static void setSettings() throws Exception{
        PrivilegedAccessor.setValue(RouterService.class,"secureMessageVerifier",new TestVerifier());
        UDP_ACCESS = new DatagramSocket();
        UDP_ACCESS.setSoTimeout(1000);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                new String[] {InetAddress.getLocalHost().getHostAddress(),"127.*.*.*"});
        FilterSettings.INSPECTOR_IP_ADDRESSES.setValue(new String[]{"127.*.*.*"});
        MessageSettings.INSPECTION_VERSION.setValue(0);
    }
    
    @InspectablePrimitive
    private static String inspectedValue;
    private static String otherValue;
    
    public void testInspection() throws Exception {
        inspectedValue = "a";
        otherValue = "b";
        InspectionRequest request = new InspectionRequest(
                "com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTest,otherValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTest,invalidValue");
        Map response = tryMessage(request);
        assertEquals(1, response.size());
        assertEquals("a",new String((byte[])response.get("0")));
        request = new InspectionRequest(true, 2, null,
                "com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTest,otherValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTest,invalidValue");
        response = tryMessage(request);
        assertEquals(2, response.size());
        Thread.sleep(20);
        long timestamp = Long.valueOf(response.get("-1").toString());
        assertLessThan(System.currentTimeMillis(), timestamp);
        assertGreaterThan(System.currentTimeMillis() - 100, timestamp);
    }

    public void testEmpty() throws Exception {
        InspectionRequest request = new InspectionRequest(
                "com.limegroup.gnutella.messages.vendor.InspectionTest,invalidValue");
        try {
            tryMessage(request);
            fail("should not receive anything");
        }catch(IOException expected){}
    }
    
    public void testDynamicQuerying() throws Exception {
        InspectionRequest request = new InspectionRequest(
                "com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,QUERIES",
                "com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,NEW_QUERIES",
                "com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,_active");
        
        // no queries - lists should be empty, dispatcher inactive
        Map response = tryMessage(request);
        assertEquals(response.toString(),3,response.size());
        assertEquals("false", new String((byte[])response.get("2")));
        assertEquals("0",new String((byte[])response.get("0")));
        assertEquals("0",new String((byte[])response.get("1")));
        
        // send a query
        MessageSettings.INSPECTION_VERSION.setValue(0);
        Message query = QueryRequest.createQuery("asdf");
        byte []guid = query.getGUID();
        LEAF[0].send(query);
        LEAF[0].flush();
        Thread.sleep(2000);
        response = tryMessage(request);
        assertEquals("true", new String((byte[])response.get("2")));
        // there should be an element either in NEW_QUERIES or QUERIES
        int queries = Integer.valueOf(new String((byte[])response.get("0")));
        int newQueries = Integer.valueOf(new String((byte[])response.get("1")));
        assertEquals(1,queries + newQueries);
        
        // shut off the query
        MessageSettings.INSPECTION_VERSION.setValue(0);
        LEAF[0].send(new QueryStatusResponse(new GUID(guid), 65535));
        LEAF[0].flush();
        Thread.sleep(2000);
        response = tryMessage(request);
        assertEquals("false", new String((byte[])response.get("2")));
        assertEquals("0",new String((byte[])response.get("0")));
        assertEquals("0",new String((byte[])response.get("1")));
    }
    
    public void testBEncoded() throws Exception {
        BEObject valid = new BEObject();
        BEObject.self = valid;
        InspectionRequest request = new InspectionRequest("com.limegroup.gnutella.messages.vendor.BEObject,self");
        Map m = tryMessage(request);
        assertEquals(1, m.size());
        Map contained = (Map)m.get("0");
        List l = (List) contained.get("some list");
        assertEquals(new Long(5), contained.get("some field"));
        assertFalse(contained.containsKey("wrong type"));
    }
    
    @SuppressWarnings("unchecked")
    public void testConnectionManager() throws Exception {
        // send a lot of messages through the UP connection
        for (int i = 0; i < 10; i++)
            ULTRAPEER[0].send(QueryRequest.createQuery("asdf"));
        ULTRAPEER[0].flush();
        
        // send an inspection request
        InspectionRequest request = new InspectionRequest(false, 2, null, "com.limegroup.gnutella.LegacyConnectionStats,UP",
                "com.limegroup.gnutella.LegacyConnectionStats,LEAF");
        Map m = tryMessage(request);
        Map leafs = (Map) m.get("1");
        Map up = (Map) m.get("0");
        
        // in test cases, all connections will report same address:port so they overwrite
        // each other in the maps.
        assertEquals(1, leafs.size());
        assertEquals(1, up.size()); 
        
        Map leaf1 = (Map) leafs.get("127.0.0.1:6667");
        Map up1 = (Map) up.get("127.0.0.1:6667");
        assertGreaterThan(20,leaf1.size()); // should have all kinds of parameters
        
        // the parameters reported for the leaf and up should be the same
        assertTrue(leaf1.keySet().containsAll(up1.keySet()));
        assertTrue(up1.keySet().containsAll(leaf1.keySet()));
        
        // but the values should be different
        assertFalse(leaf1.values().containsAll(up1.values()));
        
        // the number of messages received through the up should be larger than that through the leaf
        int upReceived = Integer.valueOf(up1.get("nmr").toString());
        int leafReceived = Integer.valueOf(leaf1.get("nmr").toString());
        assertGreaterThan(leafReceived, upReceived);
    }
    
    public void testRouting() throws Exception {
        // one of the leafs supports inspections
        LEAF[0].send(MessagesSupportedVendorMessage.instance());
        
        // create a request with a return address and one without
        InspectionRequest notRouted = new InspectionRequest("com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue");
        InspectionRequest routed = new InspectionRequest(false, 2, new IpPortImpl("127.0.0.1",20000),
                "com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue");
        
        // the one without should not be forwarded
        drainAll(LEAF);
        tryMessage(notRouted);
        assertNull(getFirstMessageOfType(LEAF[0], InspectionRequest.class));
        assertNull(getFirstMessageOfType(LEAF[1], InspectionRequest.class));
        
        // the one with return address should get answered 
        DatagramSocket socket2 = new DatagramSocket(20000);
        socket2.setSoTimeout(100);
        drainAll(LEAF);
        try {
            tryMessage(routed);
            fail("response should have been sent elsewhere");
        } catch (IOException expected){}
        DatagramPacket pack = new DatagramPacket(new byte[1000],1000);
        socket2.receive(pack);
        assertEquals(2, MessageSettings.INSPECTION_VERSION.getValue());
        
        // and forwarded to the leaf that supports it.
        InspectionRequest received = getFirstMessageOfType(LEAF[0], InspectionRequest.class);
        assertNull(getFirstMessageOfType(LEAF[1], InspectionRequest.class));
        
        // the forwarded message should be identical
        assertEquals(routed.getGUID(), received.getGUID());
        assertEquals(20000,received.getReturnAddress().getPort());
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue",
                received.getRequestedFields()[0]);
        assertEquals(2, received.getRoutableVersion());
        
        // and handling it should also respond to the return address
        MessageSettings.INSPECTION_VERSION.setValue(1);
        MessageRouter router = RouterService.getMessageRouter();
        router.handleMessage(received, RouterService.getConnectionManager().getInitializedClientConnections().get(0));
        pack = new DatagramPacket(new byte[1000],1000);
        socket2.receive(pack);
        assertEquals(2, received.getRoutableVersion());
    }
    
    private Map tryMessage(Message m) throws Exception {
        assertTrue(UDPService.instance().isListening());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.write(baos);
        byte [] b = baos.toByteArray();
        DatagramPacket pack = new DatagramPacket(b,
                b.length,InetAddress.getByName("127.0.0.1"),RouterService.getPort());
        UDP_ACCESS.send(pack);
        
        //now read the response       
        pack = new DatagramPacket(new byte[1000],1000);
        
        //not catching IOEx here because not replying is a valid scenario.
        
        UDP_ACCESS.receive(pack);
        byte [] data = pack.getData();
        byte [] guid = new byte[16];
        System.arraycopy(data,0,guid,0,16);
        assertEquals(m.getGUID(),guid);
        assertEquals((byte)0x31,data[16]);
        byte vendorId[] = new byte[4];
        System.arraycopy(data,23,vendorId,0,4);
        assertEquals(VendorMessage.F_LIME_VENDOR_ID, vendorId);
        byte [] selectorVersion = new byte[4];
        System.arraycopy(data,27,selectorVersion,0,4);
        //get the selector....
        int selector = ByteOrder.ushort2int(ByteOrder.leb2short(selectorVersion, 0));
        // get the version....
        int version = ByteOrder.ushort2int(ByteOrder.leb2short(selectorVersion, 2));
        assertEquals(VendorMessage.F_INSPECTION_RESP, selector);
        assertEquals(1, version);
        
        // inflate the rest
        Inflater in = new Inflater();
        in.setInput(data, 31, data.length - 31);
        in.finished();
        byte [] inflated = new byte[60000];
        int numInflated = in.inflate(inflated);
        String s = new String(inflated,0, numInflated);
        return (Map)Token.parse(s.getBytes());
    }
    
}
@SuppressWarnings("unchecked")
class BEObject implements Inspectable {
    static BEObject self;
    public Object inspect() {
        Map m = new HashMap();
        m.put("empty list",new ArrayList());
        m.put("some field",5);
        m.put("wrong type", new BEObject());
        return m;
    }
}   
class TestVerifier extends SecureMessageVerifier {
    TestVerifier() {
        super("asdf","asdf");
    }
    @Override
    public void verify(Verifier verifier) {
        verifier.getSecureMessageCallback().handleSecureMessage(verifier.getSecureMessage(), true);
    }

    @Override
    public void verify(SecureMessage sm, SecureMessageCallback smc) {
        smc.handleSecureMessage(sm, true);
    }

    @Override
    public void verify(PublicKey pubKey, String algorithm, 
            SecureMessage sm, SecureMessageCallback smc) {
        smc.handleSecureMessage(sm, true);
    }
}

