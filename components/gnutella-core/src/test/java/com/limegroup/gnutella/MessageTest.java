package com.limegroup.gnutella;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.io.*;

public class MessageTest extends TestCase {
    
    public MessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MessageTest.class);
    }

    public void testLegacy() {
        //Note: some of Message's code is covered by subclass tests, e.g.,
        //PushRequestTest.
        byte[] buf=new byte[10];
        buf[3]=(byte)192;
        buf[4]=(byte)168;
        buf[5]=(byte)0;
        buf[6]=(byte)1;       
        assertTrue(Message.ip2string(buf, 3).equals("192.168.0.1"));
        
        buf=new byte[4];
        buf[0]=(byte)0;
        buf[1]=(byte)1;
        buf[2]=(byte)2;
        buf[3]=(byte)3;
        assertTrue(Message.ip2string(buf).equals("0.1.2.3"));

        buf=new byte[4];
        buf[0]=(byte)252;
        buf[1]=(byte)253;
        buf[2]=(byte)254;
        buf[3]=(byte)255;
        assertTrue(Message.ip2string(buf).equals("252.253.254.255"));

        Message m1=new PingRequest((byte)3);
        Message m2=new PingRequest((byte)3);
        m2.setPriority(5);
        assertTrue(m1.compareTo(m2)>0);
        assertTrue(m2.compareTo(m1)<0);
        assertTrue(m2.compareTo(m2)==0);
        //Test for null payload with Ping
        
        byte[] bytes = new byte[23];
        byte[] g = GUID.makeGuid();
        for(int i=0;i<16;i++) 
            bytes[i] = g[i];
        bytes[16] = Message.F_PING_REPLY;
        bytes[17] = (byte) 2;//ttl
        bytes[18] = (byte) 2; //hops
        bytes[19] = (byte)0;
        bytes[20] = (byte)0;
        bytes[21] = (byte)0;
        bytes[22] = (byte)0;
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        byte[] b = new byte[40];
        Message m  = null;
        try {
            m = Message.read(bais,b);
        } catch(BadPacketException boe) {
        } catch(Exception e) {
            assertTrue("Messasge.read failed",false);
        } 
        PingReply pr = (PingReply)m;
    }
}
