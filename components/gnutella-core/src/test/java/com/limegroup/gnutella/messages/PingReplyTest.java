package com.limegroup.gnutella.messages;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.io.*;

public class PingReplyTest extends TestCase {
    public PingReplyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(PingReplyTest.class);
    }

    public void testNewPong() {
        long u4=0x00000000FFFFFFFFl;
        int u2=0x0000FFFF;
        byte[] ip={(byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x1};
        PingReply pr=new PingReply(new byte[16], (byte)0,
                                   u2, ip, u4, u4);
        assertTrue(pr.getPort()==u2);
        assertTrue(pr.getFiles()==u4);
        long kbytes=pr.getKbytes();
        assertTrue(Long.toHexString(kbytes), kbytes==u4);
        String ip2=pr.getIP();
        assertTrue(ip2, ip2.equals("255.0.0.1"));
        //assertTrue(pr.ip!=null);  //Looking at private data
        ip2=pr.getIP();
        assertTrue(ip2, ip2.equals("255.0.0.1"));
        assertTrue(! pr.isMarked());
    }      
      
    //TODO: check construction from raw bytes

    public void testPongMarking() {
        PingReply pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
                         0, 0, false);
        assertTrue(! pr.isMarked());        
        pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
                         0, 0, true);
        assertTrue(pr.isMarked());
        pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
                         5, 2348, false);        
        assertTrue(! pr.isMarked());
        assertTrue(pr.getKbytes()==2348);
        pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
                         5, 2348, true);
        assertTrue(pr.isMarked());
        pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
                         5, 345882, false);
        assertTrue(! pr.isMarked());
        pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
                         5, 345882, true);
        assertTrue(pr.isMarked());
        assertTrue(! pr.hasGGEPExtension());
        try {
            pr.getDailyUptime();
            assertTrue(false);
        } catch (BadPacketException e) { }
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

    public void testNonGGEPBigPong() {
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
        try {
            pr = new PingReply(new byte[16], (byte)2, (byte)4, payload);
        } catch (BadPacketException e) {
            fail("Packet bad: "+e);
        }
        assertTrue(! pr.hasGGEPExtension());
        try {
            pr.getDailyUptime();
            assertTrue(false);
        } catch (BadPacketException e) { }

        
        //Start testing
        assertTrue("wrong port", pr.getPort() == 15);
        String ip = pr.getIP();
        assertTrue("wrong IP", ip.equals("16.16.16.16"));
        assertTrue("wrong files", pr.getFiles() == 15);
        assertTrue("Wrong share size", pr.getKbytes() == 15);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try{
            pr.write(stream);
        }catch(IOException ioe){
            ioe.printStackTrace();
            assertTrue("problem with writing out big pong", false);
        }
        byte[] op = stream.toByteArray();
        byte[] big = new byte[2];
        big[0] = op[op.length-2];
        big[1] = op[op.length-1];
        String out = new String(big);
        assertTrue("Big part of pong lost", out.equals("AB"));
        //come this far means its OK
    }

    /** Test the raw bytes of an encoded GGEP'ed pong.  Then checks that
     *  these can be decoded.  Note that this will need to be changed if
     *  more extensions are added. */
    public void testGGEPEncodeDecode() {
        //Create pong
        PingReply pr=new PingReply(new byte[16], (byte)3, 6349, new byte[4],
                                   0l, 0l, false, 523);        
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
        int ggepLength=1   //magic number
                      +1   //"DUPTIME" extension flags
                      +idLength //ID
                      +1   //data length
                      +2   //data bytes
                      +1   //"UDP" extension flags
                      +udpLength // ID
                      +1   //data length
                      +1;  //data bytes
        assertTrue("Length: "+bytes.length, bytes.length==(23+14+ggepLength));
        int offset=23+14;                              //GGEP offset
        assertTrue(bytes[offset]==(byte)0xc3);         //GGEP magic number
        assertTrue("Got: "+(0xFF&bytes[offset+1]), 
                   bytes[offset+1]==(byte)(0x00 | idLength)); //extension flags
        assertTrue(bytes[offset+2]==(byte)'D');
        assertTrue(bytes[offset+3]==(byte)'U');
        assertTrue(bytes[offset+2+idLength+4]==(byte)'U');
        assertTrue(bytes[offset+2+idLength+5]==(byte)'D');
        assertTrue(bytes[offset+2+idLength+6]==(byte)'P');
                     //...etc.
        assertTrue(bytes[bytes.length-2-(3+udpLength)]==(byte)0x0B); //little byte of 523
        assertTrue(bytes[bytes.length-1-(3+udpLength)]==(byte)0x02); //big byte of 523


        //Decode and check contents.
        try {
            Message m=Message.read(new ByteArrayInputStream(bytes));
            PingReply pong=(PingReply)m;
            assertTrue(m instanceof PingReply);
            assertTrue(pong.getPort()==6349);
            assertTrue(pong.hasGGEPExtension());
            assertTrue(pong.getDailyUptime()==523);
            assertTrue(pong.supportsUnicast()==true);
        } catch (BadPacketException e) {
            fail("Couldn't extract uptime");
        } catch (IOException e) {
            fail("IO problem");
        }
    }

    public void testStripGGEP2() {
        byte[] guid=GUID.makeGuid();
        byte[] ip={(byte)18, (byte)239, (byte)3, (byte)144};
        PingReply pr1=new PingReply(guid, (byte)3, 6349, ip,
                                    13l, 14l, false, 4321);           
        PingReply pr2=(PingReply)pr1.stripExtendedPayload();
        assertTrue(Arrays.equals(pr1.getGUID(), pr2.getGUID()));
        assertEquals(pr1.getHops(), pr2.getHops());
        assertEquals(pr1.getTTL(), pr2.getTTL());
        assertEquals(pr1.getFiles(), pr2.getFiles());
        assertEquals(pr1.getKbytes(), pr2.getKbytes());
        assertEquals(pr1.getPort(), pr2.getPort());
        assertEquals(pr1.getIP(), pr2.getIP());

        assertTrue(! pr2.hasGGEPExtension());
        try {
            pr2.getDailyUptime();
            fail("No exception");
        } catch (BadPacketException e) { }
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try {
            pr2.write(out);
        } catch (IOException e) {
            fail("Mysterious IO problem");
        }
        assertTrue(out.toByteArray().length==(23+14));

        //Check no aliasing
        pr1.hop();
        assertTrue(pr1.getHops()!=pr2.getHops());
        assertTrue(pr1.getTTL()!=pr2.getTTL());
    }

    public void testPongTooSmall() {
        byte[] bytes=new byte[23+25];  //one byte too small
        bytes[16]=Message.F_PING_REPLY;
        bytes[17]=(byte)3;     //hops
        bytes[18]=(byte)3;     //ttl
        bytes[19]=(byte)13;    //payload length
        ByteArrayInputStream in=new ByteArrayInputStream(bytes);
        try {
            Message.read(in);
            fail("No exception thrown");
        } catch (IOException fail) {
            fail("Unexpected IO problem");
        } catch (BadPacketException pass) { 
            //Pass!
        }
    }
}
