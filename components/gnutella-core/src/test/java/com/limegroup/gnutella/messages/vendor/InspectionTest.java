package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.zip.Inflater;

import junit.framework.Test;

import org.limewire.inspection.InspectablePrimitive;
import org.limewire.util.ByteOrder;

import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.ServerSideTestCase;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.FilterSettings;
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
        return new Integer(1);
    }
    
    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    public static void setUpQRPTables() throws Exception {}
    public static void setSettings() throws Exception{
        UDP_ACCESS = new DatagramSocket();
        UDP_ACCESS.setSoTimeout(1000);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                new String[] {InetAddress.getLocalHost().getHostAddress(),"127.*.*.*"});
        FilterSettings.INSPECTOR_IP_ADDRESSES.setValue(new String[]{"127.*.*.*"});
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
        assertEquals("a",new String((byte[])response.get("com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue")));
        
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
        assertEquals("false", new String((byte[])response.get("com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,_active")));
        assertEquals("0",new String((byte[])response.get("com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,QUERIES")));
        assertEquals("0",new String((byte[])response.get("com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,NEW_QUERIES")));
        
        // send a query
        Message query = QueryRequest.createQuery("asdf");
        byte []guid = query.getGUID();
        LEAF[0].send(query);
        LEAF[0].flush();
        Thread.sleep(2000);
        response = tryMessage(request);
        assertEquals("true", new String((byte[])response.get("com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,_active")));
        // there should be an element either in NEW_QUERIES or QUERIES
        int queries = Integer.valueOf(new String((byte[])response.get("com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,QUERIES")));
        int newQueries = Integer.valueOf(new String((byte[])response.get("com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,NEW_QUERIES")));
        assertEquals(1,queries + newQueries);
        
        // shut off the query
        LEAF[0].send(new QueryStatusResponse(new GUID(guid), 65535));
        LEAF[0].flush();
        Thread.sleep(2000);
        response = tryMessage(request);
        assertEquals("false", new String((byte[])response.get("com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,_active")));
        assertEquals("0",new String((byte[])response.get("com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,QUERIES")));
        assertEquals("0",new String((byte[])response.get("com.limegroup.gnutella.search.QueryDispatcher,INSTANCE,NEW_QUERIES")));
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
