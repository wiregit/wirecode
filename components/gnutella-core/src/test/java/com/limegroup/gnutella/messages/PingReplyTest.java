package com.limegroup.gnutella.messages;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.guess.*;
import com.sun.java.util.collections.*;
import java.io.*;

public class PingReplyTest extends com.limegroup.gnutella.util.BaseTestCase {
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
     * Tests the method for creating a new pong with a changed GUID out
     * of an existing pong
     */
    public void testMutateGUID() throws Exception {
        byte[] guid = new GUID().bytes();
        byte ttl = 4;
        int port = 6444;
        byte[] ip = new byte[6];
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
        assertEquals(pr.getIP(), testPR.getIP());
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
            PingReply pong = 
                PingReply.createFromNetwork(guid, (byte)4, (byte)3,
                                            payload);
            fail("should have not accepted payload size");
        } catch(BadPacketException e) {
            // expected because the payload size is invalid
        }

        // make sure we reject null guids
        payload = new byte[PingReply.STANDARD_PAYLOAD_SIZE];
        try {
            PingReply pong = 
                PingReply.createFromNetwork(null, (byte)4, (byte)3,
                                            payload);
            fail("should have not accepted null guid");
        } catch(NullPointerException e) {
            // expected because the payload size is invalid
        }        

        // make sure we reject null payloads
        try {
            PingReply pong = 
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

        System.arraycopy(extensions, 0, 
                         payload, PingReply.STANDARD_PAYLOAD_SIZE, 
                         extensions.length);
        try {
            PingReply pong = 
                PingReply.createFromNetwork(guid, (byte)4, (byte)3,
                                            payload);
            fail("should have not accepted bad GGEP in payload");
        } catch(BadPacketException e) {
            // expected because the payload size is invalid
        }                

        // test one that should go through fine
        payload = new byte[PingReply.STANDARD_PAYLOAD_SIZE];

        // this one should go through
        PingReply pong = 
            PingReply.createFromNetwork(guid, (byte)4, (byte)3,
                                        payload);
    }


    public void testNewPong() {
        long u4=0x00000000FFFFFFFFl;
        int u2=0x0000FFFF;
        byte[] ip={(byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x1};
        PingReply pr = PingReply.create(new byte[16], (byte)0,
                                        u2, ip, u4, u4);

        assertEquals(u2, pr.getPort());
        assertEquals(u4, pr.getFiles());
        long kbytes=pr.getKbytes();
        assertEquals(Long.toHexString(kbytes), u4, kbytes);
        String ip2=pr.getIP();
        assertEquals("255.0.0.1", ip2);
        //assertTrue(pr.ip!=null);  //Looking at private data
        ip2=pr.getIP();
        assertEquals("255.0.0.1", ip2);
        assertTrue(! pr.isUltrapeer());
    }      
      
    //TODO: check construction from raw bytes

    public void testPongMarking() {
        PingReply pr = 
            PingReply.createExternal(new byte[16], (byte)2, 6346, new byte[4],
                                     false);

        
        assertTrue(! pr.isUltrapeer());        
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = PingReply.createExternal(new byte[16], (byte)2, 6346, new byte[4],
                                      true);
        assertTrue(pr.isUltrapeer());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = PingReply.create(new byte[16], (byte)2, 6346, new byte[4],
                              5, 2348, false, 0, false);        
        assertTrue(! pr.isUltrapeer());
        assertEquals(2348, pr.getKbytes());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = PingReply.create(new byte[16], (byte)2, 6346, new byte[4],
                              5, 2348, true, 0, true);
        assertTrue(pr.isUltrapeer());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = PingReply.create(new byte[16], (byte)2, 6346, new byte[4],
                              5, 345882, false, 0, false);
        assertTrue(! pr.isUltrapeer());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = PingReply.create(new byte[16], (byte)2, 6346, new byte[4],
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
        String ip = pr.getIP();
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
            PingReply.createExternal(new byte[16], (byte)3, 6349, new byte[4], false);
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

        PingReply pr = PingReply.create(new byte[16], (byte)3, 6349, new byte[4],
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

        PingReply pr=PingReply.create(new byte[16], (byte)3, 6349, new byte[4],
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
        assertEquals(pr1.getIP(), pr2.getIP());

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

    // TODO: build a test to test multiple GGEP blocks in the payload!!  the
    // implementation does not cover this it seems, so it should fail ;)

}


