package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;

public class PingRequestTest extends com.limegroup.gnutella.util.BaseTestCase {
    public PingRequestTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PingRequestTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() {
        // runs before every testX method
    }
    
    public void tearDown() {
        // runs after every testX method
    }
    
    public static void globalSetUp() {
        // runs before the first testX
    }
    
    public static void globalTearDown() {
        // runs after the last textX
    }


    //TODO: test other parts of ping!

    public void testQueryKeyPing() throws Exception {
        PingRequest pr = PingRequest.createQueryKeyRequest();
        assertFalse(pr.isQueryKeyRequest()); // hasn't been hopped yet
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pr.write(baos);
        ByteArrayInputStream bais = 
        new ByteArrayInputStream(baos.toByteArray());
        PingRequest prRead = (PingRequest) Message.read(bais);
        prRead.hop();
        assertTrue(prRead.isQueryKeyRequest());

    }


    public void testGGEPPing() throws Exception {
        // first make a GGEP block....
        GGEP ggepBlock = new GGEP(false);
        ggepBlock.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ggepBlock.write(baos);

        byte[] ggepBytes = baos.toByteArray();

        //Headers plus payload(6 bytes)
        byte[] buffer = new byte[23+ggepBytes.length+1];
        byte[] guid = GUID.makeGuid();//get a GUID
        System.arraycopy(guid,0,buffer,0,guid.length);//copy GUID
        int currByte = guid.length;
        buffer[currByte] = Message.F_PING;
        currByte++;
        buffer[currByte] = 0x0001; // TTL 
        currByte++;
        buffer[currByte] = 0x0000;// Hops
        currByte++;
        buffer[currByte] = (byte)(ggepBytes.length+1);//1st byte = 6
        currByte++;
        buffer[currByte] = 0x0000;//2nd byte = 0
        currByte++;
        buffer[currByte] = 0x0000;//3rd byte = 0
        currByte++;
        buffer[currByte] = 0x0000;//4th byte = 0 - remember it's little endian
        currByte++;
        // stick in GGEP
        for (int i = 0; i < ggepBytes.length; i++)
            buffer[currByte++] = ggepBytes[i];
        buffer[currByte++] = 0; // trailing 0
        assertGreaterThanOrEquals(buffer.length, currByte);

        //OK, ggep ping ready
        ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
        Message m = null;
        m = Message.read(stream);
        PingRequest pr = null;
        pr = (PingRequest)m;
        assertTrue(!pr.isQueryKeyRequest());
        pr.hop();
        assertTrue(pr.isQueryKeyRequest());
        //Came this far means its all OK
    }


    public void testBigPing() throws Exception {
        byte[] buffer = new byte[23+16];//Headers plus payload(16 bytes)
        //Note: We choose a size of 16 to make sure it does not create a
        //group ping, 
        byte[] guid = GUID.makeGuid();//get a GUID
        System.arraycopy(guid,0,buffer,0,guid.length);//copy GUID
        int currByte = guid.length;
        buffer[currByte] = Message.F_PING;
        currByte++;
        buffer[currByte] = 0x0004; // TTL 
        currByte++;
        buffer[currByte] = 0x0000;// Hops
        currByte++;
        buffer[currByte] = 0x0010;//1st byte = 16, A thro' P
        currByte++;
        buffer[currByte] = 0x0000;//2nd byte = 0
        currByte++;
        buffer[currByte] = 0x0000;//3rd byte = 0
        currByte++;
        buffer[currByte] = 0x0000;//4th byte = 0 - remember it's little endian
        currByte++;
        byte c = 65;//"A"
        byte[] payload = new byte[16];//to be used to test constrcutor
        for(int i=0; i<16; i++){
            buffer[currByte] = c;
            payload[i] = buffer[currByte];
            currByte++;
            c++;
        }
        //OK, Big ping ready
        ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
        Message m = null;
        m = Message.read(stream);
        PingRequest pr = null;
        pr = (PingRequest)m;
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        pr.write(outBuffer);
        byte [] outb = outBuffer.toByteArray();
        String out = new String(outb,23,outb.length-23);
        assertEquals("Wrong payload", "ABCDEFGHIJKLMNOP", out);
      
        //Test the new constructor for big pings read from the network
      
        PingRequest bigPing = new PingRequest(GUID.makeGuid(),(byte)7,
                                              (byte)0,payload);
        assertEquals(0, bigPing.getHops());
        assertEquals(7, bigPing.getTTL());
        assertEquals("bad length", 16, bigPing.getLength());
        //Came this far means its all OK
    }

    public void testStripNoPayload() {
        byte[] guid=new byte[16];  guid[0]=(byte)0xFF;        
        PingRequest pr=new PingRequest(guid, (byte)3, (byte)4);
        assertEquals(pr, pr.stripExtendedPayload());
    }


    public void testStripPayload() throws Exception  {
        byte[] guid=new byte[16];  guid[0]=(byte)0xFF;       
        byte[] payload=new byte[20]; payload[3]=(byte)0xBC;
        PingRequest pr=new PingRequest(guid, (byte)3, (byte)4, payload);
        PingRequest pr2=(PingRequest)pr.stripExtendedPayload();
        assertEquals(pr.getHops(), pr2.getHops());
        assertEquals(pr.getTTL(), pr2.getTTL());
        assertTrue(Arrays.equals(pr.getGUID(), pr2.getGUID()));
        
        assertEquals(pr2.getTotalLength(),23);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        pr2.write(out);
        assertEquals(out.toByteArray().length, 23);
        
    }
    
    public void testAddIP() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //try a ping which doesn't ask for ip 
        
        PingRequest noRequest = new PingRequest((byte)1);
        assertFalse(noRequest.requestsIP());
        
        noRequest.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PingRequest fromNet = (PingRequest) Message.read(bais);
        
        assertFalse(fromNet.requestsIP());
        
        //try a ping without any other ggeps except the ip request
        byte []guid = GUID.makeGuid();
        
        PingRequest noPayload = new PingRequest(guid,(byte)1,(byte)0);
        
        assertFalse(noPayload.requestsIP());
        
        noPayload.addIPRequest();
        assertTrue(noPayload.requestsIP());
        
        baos = new ByteArrayOutputStream();
        noPayload.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        fromNet = (PingRequest) Message.read(bais);
        
        assertTrue(fromNet.requestsIP());
        
        // now try a ping with locale
        String original = ApplicationSettings.LANGUAGE.getValue();
        ApplicationSettings.LANGUAGE.setValue("zz");
        PingRequest withLocale = new PingRequest((byte)1);
        ApplicationSettings.LANGUAGE.setValue(original);
        
        withLocale.addIPRequest();
        
        baos = new ByteArrayOutputStream();
        withLocale.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        fromNet = (PingRequest) Message.read(bais);
        assertTrue(fromNet.requestsIP());
        assertEquals("zz",fromNet.getLocale());
     
        assertFalse(fromNet.supportsCachedPongs());
        assertNull(fromNet.getSupportsCachedPongData());   
    }
    
    public void testUDPPingRequest() {
        PingRequest pr = PingRequest.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        
        UltrapeerSettings.MIN_CONNECT_TIME.setValue(0);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        assertTrue(RouterService.isSupernode());
        pr = PingRequest.createUDPPing();
        assertFalse(pr.requestsIP());
        byte[] data = pr.getSupportsCachedPongData();
        assertEquals(0x1, data[0] & 0x1);
        
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        assertFalse(RouterService.isSupernode());
        pr = PingRequest.createUDPPing();
        assertFalse(pr.requestsIP());
        data = pr.getSupportsCachedPongData();
        assertEquals(0x0, data[0] & 0x1);
        
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        pr = PingRequest.createUDPPing();
        assertTrue(pr.requestsIP());
    }
    
    public void testSupportsCachedPongData() throws Exception {
        List ggeps = new LinkedList();
        ggeps.add(new NameValue(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS));
        PingRequest pr = make(ggeps);
        assertTrue(pr.supportsCachedPongs());
        byte[] data = pr.getSupportsCachedPongData();
        assertNotNull(data);
        assertEquals(0, data.length);
        
        ggeps.clear();
        ggeps.add(new NameValue(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS, new byte[1]));
        pr = make(ggeps);
        assertTrue(pr.supportsCachedPongs());
        data = pr.getSupportsCachedPongData();
        assertNotNull(data);
        assertEquals(1, data.length);        
    }
    
    private PingRequest make(List ggeps) throws Exception {
        return (PingRequest)PrivilegedAccessor.invokeConstructor(
            PingRequest.class,
            new Object[] { new byte[16], new Byte((byte)1), ggeps },
            new Class[] { byte[].class, byte.class, List.class });
    }
}
