package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Test;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.PrivilegedAccessor;

public class PingReplyTest extends com.limegroup.gnutella.util.BaseTestCase {
    
    /**
     * A non blank IP
     */
    private static final byte[] IP = new byte[] { 1, 1, 1, 1 };
    
    public PingReplyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PingReplyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
   
    /**
     * Tests the methods for getting the leaf and ultrapeer slots from the 
     * pong.
     * 
     * @throws Exception if an error occurs
     */
    public void testHasFreeSlots() throws Exception {
        
        byte[] guid = GUID.makeGuid();
        byte[] ip = {1,1,1,1};
        PingReply pr = PingReply.create(guid, (byte)3, 6346, ip, 
            (long)10, (long)10, true, 100, true);    

        //All values are determined based on connection status, and because
        // we haven't set up connections yet, we don't have free anything.
        assertTrue("slots unexpectedly empty", !pr.hasFreeSlots());
        assertEquals("unexpected number leaf slots", 0, pr.getNumLeafSlots());
        assertTrue("slots unexpectedly not empty", !pr.hasFreeLeafSlots());
        assertTrue("slots unexpectedly empty", !pr.hasFreeUltrapeerSlots());
        assertEquals("slots unexpectedly empty", 0, pr.getNumUltrapeerSlots());
        
        // Switch ConnectionManager to report different values for free leaf
        // and ultrapeer slots.
        ConnectionManager manager = new TestConnectionManager(7, 10);
        PrivilegedAccessor.setValue(RouterService.class, "manager", manager);
        
        pr = PingReply.create(guid, (byte)3, 6346, ip, 
            (long)10, (long)10, true, 100, true);    
            
        assertTrue("slots unexpectedly full", pr.hasFreeSlots());

        assertTrue("slots unexpectedly full", pr.hasFreeLeafSlots());
        
        assertTrue("slots unexpectedly full", pr.hasFreeUltrapeerSlots());
        
        // Should now have leaf slots
        assertEquals("unexpected number leaf slots", 
                    manager.getNumFreeLimeWireLeafSlots(), 
                    pr.getNumLeafSlots());
        
        assertEquals("unexpected number ultrapeer slots", 
                    manager.getNumFreeLimeWireNonLeafSlots(), 
                    pr.getNumUltrapeerSlots());
    }
   
    
    /**
     * Tests the method for creating a new pong with a changed GUID out
     * of an existing pong
     */
    public void testMutateGUID() throws Exception {
        byte[] guid = new GUID().bytes();
        byte ttl = 4;
        int port = 6444;
        byte[] ip = {1, 1, 1, 1};
        long files = 500L;
        long kbytes = 75580L;
        boolean isUltrapeer = false;
        int dailyUptime = 10;
        boolean isGUESSCapable = false;
        
        PingReply pr = 
            PingReply.create(guid, ttl, port, ip, files, kbytes, 
                             isUltrapeer, dailyUptime, isGUESSCapable);
        
        PingReply testPR = pr.mutateGUID(new GUID().bytes());
        
        assertNotEquals(pr.getGUID(), testPR.getGUID());
        assertEquals(pr.getTTL(), testPR.getTTL());
        assertEquals(pr.getPort(), testPR.getPort());
        assertEquals(pr.getInetAddress(), testPR.getInetAddress());
        assertEquals(pr.getFiles(), testPR.getFiles());
        assertEquals(pr.getKbytes(), testPR.getKbytes());
        assertEquals(pr.isUltrapeer(), testPR.isUltrapeer());
        assertEquals(pr.getDailyUptime(), testPR.getDailyUptime());
        assertEquals(pr.supportsUnicast(), testPR.supportsUnicast());
    }


    /**
     * Tests the method for creating a pong from the network.
     */
    public void testCreatePongFromNetwork() throws Exception {
        byte[] guid = new GUID().bytes();
        byte[] payload = new byte[2];

        // make sure we reject invalid payload sizes
        try {
            PingReply.createFromNetwork(guid, (byte)4, (byte)3,
                                            payload);
            fail("should have not accepted payload size");
        } catch(BadPacketException e) {
            // expected because the payload size is invalid
        }

        // make sure we reject null guids
        payload = new byte[PingReply.STANDARD_PAYLOAD_SIZE];
        addIP(payload);
        try {
            PingReply.createFromNetwork(null, (byte)4, (byte)3,
                                            payload);
            fail("should have not accepted null guid");
        } catch(NullPointerException e) {
            // expected because the payload size is invalid
        }        

        // make sure we reject null payloads
        try {
            PingReply.createFromNetwork(guid, (byte)4, (byte)3,
                                            null);
            fail("should have not accepted null payload");
        } catch(NullPointerException e) {
            // expected because the payload size is invalid
        }        

        // make sure we reject bad ggep
        GGEP ggep = new GGEP(true);
        payload = new byte[3];
        // set 'LIM'  -- incorrect value to make sure it fails
        System.arraycopy("LIM".getBytes(),
                         0, payload, 0,
                         2);
         // add it
        ggep.put(GGEP.GGEP_HEADER_VENDOR_INFO, payload);  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ggep.write(baos);

        byte[] extensions = baos.toByteArray();
        payload = 
            new byte[PingReply.STANDARD_PAYLOAD_SIZE+extensions.length];
        addIP(payload);            

        System.arraycopy(extensions, 0, 
                         payload, PingReply.STANDARD_PAYLOAD_SIZE, 
                         extensions.length);
        try {
            PingReply.createFromNetwork(guid, (byte)4, (byte)3,
                                            payload);
            fail("should have not accepted bad GGEP in payload");
        } catch(BadPacketException e) {
            // expected because the payload size is invalid
        }                

        // test one that should go through fine
        payload = new byte[PingReply.STANDARD_PAYLOAD_SIZE];
        payload[0] = 1;
        addIP(payload);        

        // this one should go through
        PingReply.createFromNetwork(guid, (byte)4, (byte)3,
                                        payload);
    }


    public void testNewPong() {
        long u4=0x00000000FFFFFFFFl;
        int u2=0x0000FFFF;
        byte[] ip={(byte)0xFE, (byte)0x00, (byte)0x00, (byte)0x1};
        PingReply pr = PingReply.create(new byte[16], (byte)0,
                                        u2, ip, u4, u4);

        assertEquals(u2, pr.getPort());
        assertEquals(u4, pr.getFiles());
        long kbytes=pr.getKbytes();
        assertEquals(Long.toHexString(kbytes), u4, kbytes);
        String ip2=pr.getAddress();
        assertEquals("254.0.0.1", ip2);
        assertTrue(! pr.isUltrapeer());
    }      
      
    //TODO: check construction from raw bytes

    public void testPongMarking() {
        PingReply pr = 
            PingReply.createExternal(new byte[16], (byte)2, 6346, IP,
                                     false);

        
        assertTrue(! pr.isUltrapeer());        
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = PingReply.createExternal(new byte[16], (byte)2, 6346, IP,
                                      true);
        assertTrue(pr.isUltrapeer());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = PingReply.create(new byte[16], (byte)2, 6346, IP,
                              5, 2348, false, 0, false);        
        assertTrue(! pr.isUltrapeer());
        assertEquals(2348, pr.getKbytes());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = PingReply.create(new byte[16], (byte)2, 6346, IP,
                              5, 2348, true, 0, true);
        assertTrue(pr.isUltrapeer());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = PingReply.create(new byte[16], (byte)2, 6346, IP,
                              5, 345882, false, 0, false);
        assertTrue(! pr.isUltrapeer());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = PingReply.create(new byte[16], (byte)2, 6346, IP,
                              5, 345882, true, -1, true);
        assertTrue(pr.isUltrapeer());
        // after added unicast support, all Ultrapeer Pongs have GGEP extension
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        
        assertEquals("pong should not have a daily uptime", -1,
                     pr.getDailyUptime());        
    }
      
    public void testPowerOf2() {
        assertTrue(! PingReply.isPowerOf2(-1));
        assertTrue(! PingReply.isPowerOf2(0));
        assertTrue(PingReply.isPowerOf2(1));
        assertTrue(PingReply.isPowerOf2(2));
        assertTrue(! PingReply.isPowerOf2(3));
        assertTrue(PingReply.isPowerOf2(4));
        assertTrue(PingReply.isPowerOf2(16));
        assertTrue(! PingReply.isPowerOf2(18));
        assertTrue(PingReply.isPowerOf2(64));
        assertTrue(! PingReply.isPowerOf2(71));
    }

    public void testNonGGEPBigPong() throws Exception  {
        //Will this pass big pongs--even if the contents are not GGEP?
        byte[] payload = new byte[14+2];
        //add the port
        payload[0] = 0x0F;
        payload[1] = 0x00;//port 
      
        payload[2] = 0x10;
        payload[3] = 0x10;
        payload[4] = 0x10;
        payload[5] = 0x10;//ip = 16.16.16.16
      
        payload[6] = 0x0F;//
        payload[7] = 0x00;//
        payload[8] = 0x00;//
        payload[9] = 0x00;//15 files shared
      
        payload[10] = 0x0F;//
        payload[11] = 0x00;//
        payload[12] = 0x00;//
        payload[13] = 0x00;//15 KB
        //OK Now for the big pong part
        payload[14] = (byte) 65;
        payload[15] = (byte) 66;
        PingReply pr=null;
        pr = PingReply.createFromNetwork(new byte[16], (byte)2, (byte)4, payload);
        assertTrue(! pr.hasGGEPExtension());

        assertEquals("pong should not have a daily uptime", -1,
                     pr.getDailyUptime());  
        
        //Start testing
        assertEquals("wrong port", 15, pr.getPort());
        String ip = pr.getAddress();
        assertEquals("wrong IP", "16.16.16.16", ip);
        assertEquals("wrong files", 15, pr.getFiles());
        assertEquals("Wrong share size", 15, pr.getKbytes());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pr.write(stream);
        byte[] op = stream.toByteArray();
        byte[] big = new byte[2];
        big[0] = op[op.length-2];
        big[1] = op[op.length-1];
        String out = new String(big);
        assertEquals("Big part of pong lost", "AB", out);
        //come this far means its OK
    }


    public void testBasicGGEP() throws Exception {
        // create a pong
        PingReply pr = 
            PingReply.createExternal(new byte[16], (byte)3, 6349, IP, false);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        pr.write(baos);

        byte[] bytes=baos.toByteArray(); 

        //Decode and check contents.
        Message m=Message.read(new ByteArrayInputStream(bytes));
        PingReply pong=(PingReply)m;
        assertTrue(m instanceof PingReply);
        assertTrue(pong.getPort()==6349);
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());
        assertTrue(pong.supportsUnicast()==false);
        assertTrue(pong.getVendor().equals("LIME"));
        assertTrue("Major Version = " + pong.getVendorMajorVersion(), 
                   pong.getVendorMajorVersion()==2);
        assertTrue("Minor Version = " + pong.getVendorMinorVersion(), 
                   pong.getVendorMinorVersion()==7);
    }


    /** Test the raw bytes of an encoded GGEP'ed pong.  Then checks that
     *  these can be decoded.  Note that this will need to be changed if
     *  more extensions are added. */
    public void testGGEPEncodeDecode() throws Exception {
        //Create pong

        PingReply pr = PingReply.create(new byte[16], (byte)3, 6349, IP,
                                   0l, 0l, true, 523, true);        

        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        try {
            pr.write(baos);
        } catch (IOException e) {
            assertTrue("Couldn't write stream.", false);
        }

        //Encode and check raw bytes.
        //UDP is the last extension, so it is DUPTIME and then UDP.  should take
        //this into account....
        byte[] bytes=baos.toByteArray(); 
        int idLength=GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME.length();
        int udpLength=GGEP.GGEP_HEADER_UNICAST_SUPPORT.length();
        int vcLength=GGEP.GGEP_HEADER_VENDOR_INFO.length();
        int upLength=GGEP.GGEP_HEADER_UP_SUPPORT.length();
        int ggepLength=1   //magic number
                      +1   //"DUPTIME" extension flags
                      +idLength //ID
                      +1   //data length
                      +2   //data bytes
                      +1   //"UDP" extension flags
                      +udpLength // ID
                      +1   //data length
                      +1   //data bytes
                      +1   //"UP" extension flags
                      +upLength // ID
                      +1   // data length
                      +3  // data bytes
                      +1   //"VC" extension flags
                      +vcLength // ID
                      +1   // data length
                      +5;  // data bytes
        assertTrue("Length: "+bytes.length, bytes.length==(23+14+ggepLength));
        int offset=23+14;                              //GGEP offset
        assertTrue(bytes[offset]==(byte)0xc3);         //GGEP magic number
        assertTrue("Got: "+(0xFF&bytes[offset+1]), 
                   bytes[offset+1]==(byte)(0x00 | idLength)); //extension flags
        assertTrue(bytes[offset+2]==(byte)'D');
        assertTrue(bytes[offset+3]==(byte)'U');
        assertTrue(bytes[offset+2+idLength+4]==(byte)'G');
        assertTrue(bytes[offset+2+idLength+5]==(byte)'U');
        assertTrue(bytes[offset+2+idLength+6]==(byte)'E');
        assertTrue(bytes[offset+2+idLength+4+udpLength+3]==(byte)'U');
        assertTrue(bytes[offset+2+idLength+4+udpLength+4]==(byte)'P');
        assertTrue(bytes[offset+2+idLength+4+udpLength+3+upLength+5]==(byte)'V');
        assertTrue(bytes[offset+2+idLength+4+udpLength+3+upLength+6]==(byte)'C');
        assertTrue(bytes[offset+2+idLength+4+udpLength+3+upLength+8]==(byte)'L');
        assertTrue(bytes[offset+2+idLength+4+udpLength+3+upLength+9]==(byte)'I');
        assertTrue(bytes[offset+2+idLength+4+udpLength+3+upLength+10]==(byte)'M');
        assertTrue(bytes[offset+2+idLength+4+udpLength+3+upLength+11]==(byte)'E');
        assertTrue(bytes[offset+2+idLength+4+udpLength+3+upLength+12]==39);
        //...etc.
        assertTrue(bytes[bytes.length-2-(3+udpLength)-(5+upLength)-(7+vcLength)]==(byte)0x0B); //little byte of 523
        assertTrue(bytes[bytes.length-1-(3+udpLength)-(5+upLength)-(7+vcLength)]==(byte)0x02); //big byte of 523


        //Decode and check contents.
        Message m=Message.read(new ByteArrayInputStream(bytes));
        PingReply pong=(PingReply)m;
        assertTrue(m instanceof PingReply);
        assertTrue(pong.getPort()==6349);
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());
        assertTrue(pong.getDailyUptime()==523);
        assertTrue(pong.supportsUnicast()==true);
        assertTrue(pong.getVendor().equals("LIME"));
        assertTrue("Major Version = " + pong.getVendorMajorVersion(), 
                   pong.getVendorMajorVersion()==2);
        assertTrue("Minor Version = " + pong.getVendorMinorVersion(), 
                   pong.getVendorMinorVersion()==7);

    }

    /** Test the raw bytes of an encoded GGEP'ed pong.  Then checks that
     *  these can be decoded.  Note that this will need to be changed if
     *  more extensions are added. */
    public void testGGEPEncodeDecodeNoGUESS() throws Exception {
        //Create pong

        PingReply pr=PingReply.create(new byte[16], (byte)3, 6349, IP,
                                      0l, 0l, true, 523, false);        
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        try {
            pr.write(baos);
        } catch (IOException e) {
            assertTrue("Couldn't write stream.", false);
        }

        //Encode and check raw bytes.
        //UDP is the last extension, so it is DUPTIME and then UDP.  should take
        //this into account....
        byte[] bytes=baos.toByteArray(); 
        int idLength=GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME.length();
        int vcLength=GGEP.GGEP_HEADER_VENDOR_INFO.length();
        int upLength=GGEP.GGEP_HEADER_UP_SUPPORT.length();
        int ggepLength=1   //magic number
                      +1   //"DUPTIME" extension flags
                      +idLength //ID
                      +1   //data length
                      +2   //data bytes
                      +1   //"UP" extension flags
                      +upLength // ID
                      +1   // data length
                      +3  // data bytes
                      +1   //"VC" extension flags
                      +vcLength // ID
                      +1   // data length
                      +5;  // data bytes
        assertTrue("Length: "+bytes.length, bytes.length==(23+14+ggepLength));
        int offset=23+14;                              //GGEP offset
        assertTrue(bytes[offset]==(byte)0xc3);         //GGEP magic number
        assertTrue("Got: "+(0xFF&bytes[offset+1]), 
                   bytes[offset+1]==(byte)(0x00 | idLength)); //extension flags
        assertTrue(bytes[offset+2]==(byte)'D');
        assertTrue(bytes[offset+3]==(byte)'U');
        assertTrue(bytes[offset+2+idLength+4]==(byte)'U');
        assertTrue(bytes[offset+2+idLength+5]==(byte)'P');
        assertTrue(bytes[offset+2+idLength+4+upLength+5]==(byte)'V');
        assertTrue(bytes[offset+2+idLength+4+upLength+6]==(byte)'C');
        assertTrue(bytes[offset+2+idLength+4+upLength+8]==(byte)'L');
        assertTrue(bytes[offset+2+idLength+4+upLength+9]==(byte)'I');
        assertTrue(bytes[offset+2+idLength+4+upLength+10]==(byte)'M');
        assertTrue(bytes[offset+2+idLength+4+upLength+11]==(byte)'E');
        assertTrue(bytes[offset+2+idLength+4+upLength+12]==39);
        //...etc.
        assertTrue(bytes[bytes.length-2-(5+upLength)-(7+vcLength)]==(byte)0x0B); //little byte of 523
        assertTrue(bytes[bytes.length-1-(5+upLength)-(7+vcLength)]==(byte)0x02); //big byte of 523


        //Decode and check contents.
        Message m=Message.read(new ByteArrayInputStream(bytes));
        PingReply pong=(PingReply)m;
        assertTrue(m instanceof PingReply);
        assertTrue(pong.getPort()==6349);
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());
        assertTrue(pong.getDailyUptime()==523);
        assertTrue(pong.supportsUnicast()==false);
        assertTrue(pong.getVendor().equals("LIME"));
        assertTrue("Major Version = " + pong.getVendorMajorVersion(), 
                   pong.getVendorMajorVersion()==2);
        assertTrue("Minor Version = " + pong.getVendorMinorVersion(), 
                   pong.getVendorMinorVersion()==7);
    }

    public void testStripGGEP2() throws Exception {
        byte[] guid=GUID.makeGuid();
        byte[] ip={(byte)18, (byte)239, (byte)3, (byte)144};
        PingReply pr1 = PingReply.create(guid, (byte)3, 6349, ip,
                                    13l, 14l, false, 4321, false); 
        PingReply pr2=(PingReply)pr1.stripExtendedPayload();
        assertTrue(Arrays.equals(pr1.getGUID(), pr2.getGUID()));
        assertEquals(pr1.getHops(), pr2.getHops());
        assertEquals(pr1.getTTL(), pr2.getTTL());
        assertEquals(pr1.getFiles(), pr2.getFiles());
        assertEquals(pr1.getKbytes(), pr2.getKbytes());
        assertEquals(pr1.getPort(), pr2.getPort());
        assertEquals(pr1.getInetAddress(), pr2.getInetAddress());

        assertTrue(! pr2.hasGGEPExtension());
        assertEquals("pong should not have a daily uptime", -1,
                     pr2.getDailyUptime());

        ByteArrayOutputStream out=new ByteArrayOutputStream();
        pr2.write(out);
        assertTrue(out.toByteArray().length==(23+14));

        //Check no aliasing
        pr1.hop();
        assertTrue(pr1.getHops()!=pr2.getHops());
        assertTrue(pr1.getTTL()!=pr2.getTTL());
    }

    public void testPongTooSmall() throws Exception {
        byte[] bytes=new byte[23+25];  //one byte too small
        bytes[16]=Message.F_PING_REPLY;
        bytes[17]=(byte)3;     //hops
        bytes[18]=(byte)3;     //ttl
        bytes[19]=(byte)13;    //payload length
        ByteArrayInputStream in=new ByteArrayInputStream(bytes);
        try {
            Message.read(in);
            fail("No exception thrown");
        } catch (BadPacketException pass) { 
            //Pass!
        }
    }


    public void testQueryKeyPong() throws Exception {
        byte[] randBytes = new byte[8];
        (new Random()).nextBytes(randBytes);
        QueryKey qk = null;
        GUID guid = new GUID(GUID.makeGuid());
        byte[] ip={(byte)18, (byte)239, (byte)3, (byte)144};
        qk = QueryKey.getQueryKey(randBytes, true);
        PingReply pr = 
            PingReply.createQueryKeyReply(guid.bytes(), (byte) 1, 6346, ip,
                                          2, 2, true, qk);
        assertTrue(pr.getQueryKey().equals(qk));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pr.write(baos);
        ByteArrayInputStream bais = 
             new ByteArrayInputStream(baos.toByteArray());
        PingReply prStreamed = (PingReply) Message.read(bais);
        assertTrue(prStreamed.getQueryKey().equals(qk));
            
    }
    
    public void testIpRequestPong() throws Exception {
        RouterService.getAcceptor().setAddress(InetAddress.getLocalHost());
        
        // a pong carrying an ip:port
        Endpoint e = new Endpoint("1.2.3.4",5);
        PingReply p = PingReply.create(GUID.makeGuid(),(byte)1,e);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        p.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PingReply fromNet = (PingReply)Message.read(bais);
        
        assertEquals("1.2.3.4",fromNet.getMyInetAddress().getHostAddress());
        assertEquals(5,fromNet.getMyPort());
        
        // a pong carrying invalid port
        e = new Endpoint("1.2.3.4",5) {
            public int getPort() {
                return 0;
            }
        };
        
        p = PingReply.create(GUID.makeGuid(),(byte)1,e);
        baos = new ByteArrayOutputStream();
        p.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        fromNet = (PingReply)Message.read(bais);
        
        assertNull(fromNet.getMyInetAddress());
        assertEquals(0,fromNet.getMyPort());
        
        //a pong carrying private ip
        e = new Endpoint("192.168.0.1",20);
        p = PingReply.create(GUID.makeGuid(),(byte)1,e);
        baos = new ByteArrayOutputStream();
        p.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        fromNet = (PingReply)Message.read(bais);
        
        assertNull(fromNet.getMyInetAddress());
        assertEquals(0,fromNet.getMyPort());
        
        // a pong not carrying ip:port
        p = PingReply.create(GUID.makeGuid(),(byte)1);
        baos = new ByteArrayOutputStream();
        p.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        fromNet = (PingReply)Message.read(bais);
        
        assertNull(fromNet.getMyInetAddress());
        assertEquals(0,fromNet.getMyPort());
    }
    
    
    public void testUDPHostCacheExtension() throws Exception {
        GGEP ggep = new GGEP(true);
        ggep.put(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
        PingReply pr = PingReply.create(GUID.makeGuid(), (byte)1, 1,
                    new byte[] { 1, 1, 1, 1 },
                    (long)0, (long)0, false, ggep);
        assertTrue(pr.isUDPHostCache());
        assertEquals("1.1.1.1", pr.getUDPCacheAddress());
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pr.write(out);
        byte[] b = out.toByteArray();
        
        PingReply read = (PingReply)Message.read(new ByteArrayInputStream(b));
        assertTrue(read.isUDPHostCache());
        assertEquals("1.1.1.1", read.getUDPCacheAddress());
        
        ggep = new GGEP(true);
        ggep.put(GGEP.GGEP_HEADER_UDP_HOST_CACHE, "www.nowhere.org");
        pr = PingReply.create(GUID.makeGuid(), (byte)1, 1,
                    new byte[] { 1, 1, 1, 1 },
                    (long)0, (long)0, false, ggep);
        assertTrue(pr.isUDPHostCache());
        assertEquals("www.nowhere.org", pr.getUDPCacheAddress());

        out = new ByteArrayOutputStream();
        pr.write(out);
        b = out.toByteArray();
        
        read = (PingReply)Message.read(new ByteArrayInputStream(b));
        assertTrue(read.isUDPHostCache());
        assertEquals("www.nowhere.org", read.getUDPCacheAddress());
    }
    
    public void testPackedIPsInPong() throws Exception {
        GGEP ggep = new GGEP(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        PingReply pr = PingReply.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);

        List l = pr.getPackedIPPorts();
        assertEquals(4, l.size());
        IpPort ipp = (IpPort)l.get(0);
        assertEquals("1.1.1.1", ipp.getAddress());
        assertEquals(1, ipp.getPort());
        ipp = (IpPort)l.get(1);
        assertEquals("1.2.3.4", ipp.getAddress());
        assertEquals(2, ipp.getPort());
        ipp = (IpPort)l.get(2);
        assertEquals("3.4.2.3", ipp.getAddress());
        assertEquals(3, ipp.getPort());
        ipp = (IpPort)l.get(3);
        assertEquals("254.0.0.3", ipp.getAddress());
        assertEquals(4, ipp.getPort());
        
        // Try with invalid list of IPs (invalid by not being multiple of 6)
        ggep = new GGEP(true);
        out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, /* no port */ } );
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        pr = PingReply.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        l = pr.getPackedIPPorts();
        assertTrue(l.isEmpty());
        
        // Try with invalid IPs (invalid by invalid IP addr)
        ggep = new GGEP(true);
        out = new ByteArrayOutputStream();
        out.write(new byte[] { 0, 0, 0, 0, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        pr = PingReply.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        l = pr.getPackedIPPorts();
        assertTrue(l.isEmpty());
        
        // Try with invalid IPs (invalid by invalid port)
        ggep = new GGEP(true);
        out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 0, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        pr = PingReply.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        l = pr.getPackedIPPorts();
        assertTrue(l.isEmpty());
        
        // Make sure the extension works with other GGEP flags (like UDP Host Cache)
        ggep = new GGEP(true);
        out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        ggep.put(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
        pr = PingReply.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        assertTrue(pr.isUDPHostCache());
        l = pr.getPackedIPPorts();
        assertEquals(4, l.size());
        ipp = (IpPort)l.get(0);
        assertEquals("1.1.1.1", ipp.getAddress());
        assertEquals(1, ipp.getPort());
        ipp = (IpPort)l.get(1);
        assertEquals("1.2.3.4", ipp.getAddress());
        assertEquals(2, ipp.getPort());
        ipp = (IpPort)l.get(2);
        assertEquals("3.4.2.3", ipp.getAddress());
        assertEquals(3, ipp.getPort());
        ipp = (IpPort)l.get(3);
        assertEquals("254.0.0.3", ipp.getAddress());
        assertEquals(4, ipp.getPort());        
        
        // and make sure we can read from network data.
        out = new ByteArrayOutputStream();
        pr.write(out);
        
        pr = (PingReply)Message.read(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(pr.isUDPHostCache());
        l = pr.getPackedIPPorts();
        assertEquals(4, l.size());
        ipp = (IpPort)l.get(0);
        assertEquals("1.1.1.1", ipp.getAddress());
        assertEquals(1, ipp.getPort());
        ipp = (IpPort)l.get(1);
        assertEquals("1.2.3.4", ipp.getAddress());
        assertEquals(2, ipp.getPort());
        ipp = (IpPort)l.get(2);
        assertEquals("3.4.2.3", ipp.getAddress());
        assertEquals(3, ipp.getPort());
        ipp = (IpPort)l.get(3);
        assertEquals("254.0.0.3", ipp.getAddress());
        assertEquals(4, ipp.getPort());
        
        // Try with one of the constructors.
        l = new LinkedList(l);
        l.add(new Endpoint("1.5.3.5", 5));
        pr = PingReply.create(GUID.makeGuid(), (byte)1, l);
        l = pr.getPackedIPPorts();
        assertFalse(pr.isUDPHostCache());
        l = pr.getPackedIPPorts();
        assertEquals(5, l.size());
        ipp = (IpPort)l.get(0);
        assertEquals("1.1.1.1", ipp.getAddress());
        assertEquals(1, ipp.getPort());
        ipp = (IpPort)l.get(1);
        assertEquals("1.2.3.4", ipp.getAddress());
        assertEquals(2, ipp.getPort());
        ipp = (IpPort)l.get(2);
        assertEquals("3.4.2.3", ipp.getAddress());
        assertEquals(3, ipp.getPort());
        ipp = (IpPort)l.get(3);
        assertEquals("254.0.0.3", ipp.getAddress());
        assertEquals(4, ipp.getPort());        
        ipp = (IpPort)l.get(4);
        assertEquals("1.5.3.5", ipp.getAddress());
        assertEquals(5, ipp.getPort());
    }
    
    public void testPackedHostCachesInPong() throws Exception {
        // test with compression.
        GGEP ggep = new GGEP(true);
        List addrs = new LinkedList();
        addrs.add("1.2.3.4:81");
        addrs.add("www.limewire.com:6379");
        addrs.add("www.eff.org");
        addrs.add("www.test.org:1&something=somethingelse&nothing=this");
        ggep.putCompressed(GGEP.GGEP_HEADER_PACKED_HOSTCACHES, toBytes(addrs));
        PingReply pr = PingReply.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);

        Set s = new TreeSet(IpPort.COMPARATOR);
        s.addAll(pr.getPackedUDPHostCaches());
        assertEquals(4, s.size());
        IpPort ipp = new IpPortImpl("1.2.3.4", 81);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.limewire.com", 6379);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.eff.org", 6346);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.test.org", 1);
        assertContains(s, ipp);
        s.remove(ipp);
        assertEquals(0, s.size());
        
        // test without compression
        ggep = new GGEP(true);
        addrs.clear();
        addrs.add("1.2.3.4:81");
        addrs.add("www.limewire.com:6379");
        addrs.add("www.eff.org");
        addrs.add("www.test.org:1&something=somethingelse&nothing=this");
        ggep.put(GGEP.GGEP_HEADER_PACKED_HOSTCACHES, toBytes(addrs));
        pr = PingReply.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        s.clear();
        s.addAll(pr.getPackedUDPHostCaches());
        assertEquals(4, s.size());
        ipp = new IpPortImpl("1.2.3.4", 81);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.limewire.com", 6379);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.eff.org", 6346);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.test.org", 1);
        assertContains(s, ipp);
        s.remove(ipp);
        assertEquals(0, s.size());        
        
        ggep = new GGEP(true);
        addrs.clear();
        addrs.add("1.2.3.4:");
        addrs.add("3.4.2.3");
        addrs.add("5.4.3.2:1:1");
        addrs.add("13.13.1.1:notanumber");
        ggep.putCompressed(GGEP.GGEP_HEADER_PACKED_HOSTCACHES, toBytes(addrs));
        pr = PingReply.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        s.addAll(pr.getPackedUDPHostCaches());
        assertEquals(1, s.size());
        ipp = new IpPortImpl("3.4.2.3", 6346);
        assertContains(s, ipp);
        s.remove(ipp);
        assertEquals(0, s.size());
        
        ggep = new GGEP(true);
        ggep.put(GGEP.GGEP_HEADER_PACKED_HOSTCACHES, new byte[0]);
        pr = PingReply.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        assertEquals(0, pr.getPackedUDPHostCaches().size());
        
        ggep = new GGEP(true);
        ggep.putCompressed(GGEP.GGEP_HEADER_PACKED_HOSTCACHES, new byte[] { 1, 1, 1, 1 } );
        pr = PingReply.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        assertEquals(0, pr.getPackedUDPHostCaches().size());
        
        ggep = new GGEP(true);
        ggep.put(GGEP.GGEP_HEADER_PACKED_HOSTCACHES, new byte[] { 1, 1, 1, 1 } );
        pr = PingReply.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        assertEquals(0, pr.getPackedUDPHostCaches().size());        
    }
    
    private final byte[] toBytes(List l) throws Exception {
        StringBuffer sb = new StringBuffer();
        for(Iterator i = l.iterator(); i.hasNext(); ) {
            sb.append(i.next().toString());
            if(i.hasNext())
                sb.append("\n");
        }
        return sb.toString().getBytes();
    }
    
    private final void addIP(byte[] payload) {
        // fill up the ip so its not blank.
        payload[2] = 1;
        payload[3] = 1;
        payload[4] = 1;
        payload[5] = 1;
    }

    /**
     * Utility class that overrides ConnectionManager methods for getting the
     * number of free leaf and ultrapeer slots.
     */
    private static class TestConnectionManager extends ConnectionManager {
        private final int NUM_FREE_NON_LEAF_SLOTS;
        private final int NUM_FREE_LEAF_SLOTS;

        TestConnectionManager(int numFreeNonLeafSlots, int numFreeLeafSlots) {
            super();
            NUM_FREE_NON_LEAF_SLOTS = numFreeNonLeafSlots;
            NUM_FREE_LEAF_SLOTS = numFreeLeafSlots;
        }
        
        public int getNumFreeNonLeafSlots() {
            return NUM_FREE_NON_LEAF_SLOTS;
        }
        
        public int getNumFreeLeafSlots() {
            return NUM_FREE_LEAF_SLOTS;
        }
    }
    // TODO: build a test to test multiple GGEP blocks in the payload!!  the
    // implementation does not cover this it seems, so it should fail ;)

}


