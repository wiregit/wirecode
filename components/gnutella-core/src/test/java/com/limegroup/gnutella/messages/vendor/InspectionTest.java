package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Inflater;

import junit.framework.Test;

import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.util.Base32;
import org.limewire.util.ByteOrder;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.BlockingConnectionUtils;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ServerSideTestCase;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.MessageSettings;

@Singleton
public class InspectionTest extends ServerSideTestCase {
    
    /**
     * Ultrapeer 1 UDP connection.
     */
    private DatagramSocket UDP_ACCESS;
    
    @InspectablePrimitive("") @SuppressWarnings("unused") private String inspectedValue;
    @SuppressWarnings("unused") private String otherValue;

    private MessageFactory messageFactory;

    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;

    private MessageRouter messageRouter;

    private UDPService udpService;

    private ConnectionManager connectionManager;

    private NetworkManager networkManager;

    
    public InspectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(InspectionTest.class);
    }
    
    @Override
    public int getNumberOfUltrapeers() {
        return 1;
    }

    @Override
    public int getNumberOfLeafpeers() {
        return 2;
    }
    
    @Override
    public void setSettings() throws Exception {
        UDP_ACCESS = new DatagramSocket();
        UDP_ACCESS.setSoTimeout(1000);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                new String[] {InetAddress.getLocalHost().getHostAddress(),"127.*.*.*"});
        FilterSettings.INSPECTOR_IP_ADDRESSES.setValue(new String[]{"127.*.*.*"});
        MessageSettings.INSPECTION_VERSION.setValue(0);
    }
    
    @Override
    public void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                // This test uses the default SMV for inspection.
                bind(SecureMessageVerifier.class).annotatedWith(Names.named("inspection")).to(SecureMessageVerifier.class);
                // Binding must be applied otherwise InspectionUtil won't find it.
                bind(InspectionTest.class).toInstance(InspectionTest.this);
            }
        });
        super.setUp(injector);
        messageFactory = injector.getInstance(MessageFactory.class);
        messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
        messageRouter = injector.getInstance(MessageRouter.class);
        udpService = injector.getInstance(UDPService.class);
        connectionManager = injector.getInstance(ConnectionManager.class);
        networkManager = injector.getInstance(NetworkManager.class);
    }
    
    private InspectionRequest getRequest(String base32) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(Base32.decode(base32));
        return (InspectionRequest) messageFactory.read(bais);
    }

    public void testInspection() throws Exception {
        inspectedValue = "a";
        otherValue = "b";
        // "com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue",
        // "com.limegroup.gnutella.messages.vendor.InspectionTest,otherValue",
        // "com.limegroup.gnutella.messages.vendor.InspectionTest,invalidValue");
        String requestStr = "4X2I4BXJNR62FTBLHBCQVEZMAAYQCAFAAAAAATCJJVCR4AABADBSCSMBK54JZJOOWEGYAMAMAXIYLEAXMABHURA7EVP4DETDI6YZH6JJ3AAMU246FZNSHYMGHJWHNKR2AMRISGW4KOC5HAQWDN2KQ57EMDJRGHQ3X6EXEJMZ3DZSPRHCY34AHMFOET6D4PG2WBHJ7AKWIEA4GASTIJAIGU2JI5XDALACCQF3G6NAYMM4VFTZXOAQG7SPZPXSIA5NYABBIHMQ37ZF6US4CCYALOEU3KI4PAULVX5MA";
        InspectionRequest request = getRequest(requestStr);
        assertEquals(1, request.getRoutableVersion());
        assertEquals(false, request.requestsTimeStamp());
        assertNull(request.getReturnAddress());
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue",
                request.getRequestedFields()[0]);
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTest,otherValue",
                request.getRequestedFields()[1]);
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTest,invalidValue",
                request.getRequestedFields()[2]);
        Map response = tryMessage(request);
        assertEquals(1, response.size());
        assertEquals("a",new String((byte[])response.get("0")));
        
                // "com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue",
                // "com.limegroup.gnutella.messages.vendor.InspectionTest,otherValue",
                // "com.limegroup.gnutella.messages.vendor.InspectionTest,invalidValue");
        requestStr = "6UMOPYGZP6KPHLSLZXTXHE4ZAAYQCAFDAAAAATCJJVCR4AABADBSCSMBK54JZJOOWEGYAMAMAXIYLEAXMABHURA7EVP4DETDI6YZH6JJ3AAMU246FZNSHYMGHJWHNKR2AMRISGW4KOC5HAQWDN2KQ57EMDJRGHQ3X6EXEJMZ3DZSPRHCY34AHMFOET6D4PG2WBHJ6AKUICAVMQICYMBFGQSAQNJUSR3OGAWAEFB5H4SLMDRVDL3IOS7FB6IINOWQZCNM3KACCRU35H5NDMKRTS3QDETE36QIPMW6JDGSJA";
        request = getRequest(requestStr);
        assertEquals(2, request.getRoutableVersion());
        assertEquals(true, request.requestsTimeStamp());
        assertNull(request.getReturnAddress());
        response = tryMessage(request);
        assertEquals(2, response.size());
        Thread.sleep(20);
        long timestamp = Long.valueOf(response.get("-1").toString());
        assertLessThan(System.currentTimeMillis(), timestamp);
        assertGreaterThan(System.currentTimeMillis() - 100, timestamp);
    }

    public void testEmpty() throws Exception {
        String requestStr = "MLRUKAU25UMVXW3ROTPAUHN4AAYQCAEMAAAAATCJJVCR4AABADBSCSMBIN4JYBOBJMFMAIAMAXAAXFO4UN56SPXIIMBPTCBRT27TGPMMKQGHGRZNTJPAPKSMQZGJ4SF26AIZWXWPQV7CJPBBZ4RX4WLFPSWIKH3GAUNARAKWIEA4GASTIJAIGU2JI5XDALACCRR36MRH2OJAYKSCWJHPAFJDSZJY7RFBEYBBI5JLVMUEVPPD23CER5QCB5DMJLRM35UFE";
        InspectionRequest request = getRequest(requestStr);
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTest,invalidValue", request.getRequestedFields()[0]);
        assertEquals(1, request.getRequestedFields().length);
        try {
            tryMessage(request);
            fail("should not receive anything");
        }catch(IOException expected){}
    }
    
    public void testInvalidSig() throws Exception {
        InspectionRequestImpl request = new InspectionRequestImpl(
                new RoutableGGEPMessage.GGEPSigner() {
                    public GGEP getSecureGGEP(GGEP original) {
                        GGEP ret = new GGEP(true);
                        ret.put(GGEP.GGEP_HEADER_SECURE_BLOCK);
                        ret.put(GGEP.GGEP_HEADER_SIGNATURE," adsf adsf asdf ".getBytes());
                        return ret;
                    }
                }, "com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue");
        try {
            tryMessage(request);
            fail("should not receive anything");
        }catch(IOException expected){}
    }

    
    public void testRouting() throws Exception {
        // one of the leafs supports inspections
        LEAF[0].send(messagesSupportedVendorMessage);
        LEAF[0].flush();
        
        // create a request with a return address and one without
        String routedStr = "TJDZMNSU2BMQBE665ZF6V2EFAAYQCAEZAAAAATCJJVCR4AABADBSCSMBIR4JYJOHXME4AMAMAXAILAXWJBX5EC73EEGPUGCLZ36CS4W56UYNE2MQDVTZD6BJVAZBSMSZSD2MER3MXI6RO6WN6CDKY276YV4FQDZ6TO5RVYCCKJAUOAT7AECACICOQFLECAWDAJJUEQEDKNEUO3ZQFUBBKAET3DOHWUHCAUYMMHGNC3JX2SE2FZNQIKACCRYF4HPDK5WODOJ2N2EWPEAFHYEKJNGF5Q";
        String notRoutedStr = "CHWVX3POK3SAFPSS7S2MDKL5AAYQCAENAAAAATCJJVCR4AABADBSCSMBIR4JYJOHXME4AMAMAXAILAXWJBX5EC73EEGPUGCLZ36CS4W56UYNE2MQDVTZD6BJVAZBSMSZSD2MER3MXI6RO6WN6CDKY276YV4FQDZ6TO5RVYEBKZAQDQYCKNBEBA2TJFDW4MBMAIKFYBTXLHNXOKCRELJKZA75AHO5WOQ25DTQEFAJ7753YUSGPK33J4MEQHC2PJOIZXAQ6GI";
        InspectionRequest routed = getRequest(routedStr);
        InspectionRequest notRouted = getRequest(notRoutedStr);
        
        assertEquals(1, notRouted.getRoutableVersion());
        assertNull(notRouted.getReturnAddress());
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue",
                notRouted.getRequestedFields()[0]);
        
        assertEquals(2,routed.getRoutableVersion());
        IpPort retAddr = routed.getReturnAddress();
        assertEquals(0, 
                IpPort.COMPARATOR.compare(new IpPortImpl("127.0.0.1",20000),retAddr));
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue",
                routed.getRequestedFields()[0]);
        
        
        // the one without should not be forwarded
        BlockingConnectionUtils.drainAll(LEAF);
        tryMessage(notRouted);
        assertNull(BlockingConnectionUtils.getFirstMessageOfType(LEAF[0], InspectionRequestImpl.class));
        assertNull(BlockingConnectionUtils.getFirstMessageOfType(LEAF[1], InspectionRequestImpl.class));
        
        // the one with return address should get answered 
        DatagramSocket socket2 = new DatagramSocket(20000);
        socket2.setSoTimeout(100);
        BlockingConnectionUtils.drainAll(LEAF);
        try {
            tryMessage(routed);
            fail("response should have been sent elsewhere");
        } catch (IOException expected){}
        DatagramPacket pack = new DatagramPacket(new byte[1000],1000);
        socket2.receive(pack);
        assertEquals(2, MessageSettings.INSPECTION_VERSION.getValue());
        
        // and forwarded to the leaf that supports it.
        InspectionRequestImpl received = BlockingConnectionUtils.getFirstMessageOfType(LEAF[0], InspectionRequestImpl.class);
        assertNull(BlockingConnectionUtils.getFirstMessageOfType(LEAF[1], InspectionRequestImpl.class));
        assertNotNull(received);
        
        // the forwarded message should be identical
        assertEquals(routed.getGUID(), received.getGUID());
        assertEquals(20000,received.getReturnAddress().getPort());
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTest,inspectedValue",
                received.getRequestedFields()[0]);
        assertEquals(2, received.getRoutableVersion());
        
        // and handling it should also respond to the return address
        MessageSettings.INSPECTION_VERSION.setValue(1);
        MessageRouter router = messageRouter;
        router.handleMessage(received, connectionManager.getInitializedClientConnections().get(0));
        pack = new DatagramPacket(new byte[1000],1000);
        socket2.receive(pack);
        assertEquals(2, received.getRoutableVersion());
    }
    
    private Map tryMessage(Message m) throws Exception {
        assertTrue(udpService.isListening());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.write(baos);
        byte [] b = baos.toByteArray();
        DatagramPacket pack = new DatagramPacket(b,
                b.length,InetAddress.getByName("127.0.0.1"),networkManager.getPort());
        UDP_ACCESS.send(pack);
        
        //now read the response       
        pack = new DatagramPacket(new byte[10000],10000);
        
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
