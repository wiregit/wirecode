package com.limegroup.gnutella.messages;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.io.*;

public class MessageTest extends com.limegroup.gnutella.util.BaseTestCase {
    
    public MessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MessageTest.class);
    }

    public void testLegacy() throws Exception {
        //Note: some of Message's code is covered by subclass tests, e.g.,
        //PushRequestTest.
        byte[] buf=new byte[10];
        buf[3]=(byte)192;
        buf[4]=(byte)168;
        buf[5]=(byte)0;
        buf[6]=(byte)1;       
        assertEquals("192.168.0.1", Message.ip2string(buf, 3));
        
        buf=new byte[4];
        buf[0]=(byte)0;
        buf[1]=(byte)1;
        buf[2]=(byte)2;
        buf[3]=(byte)3;
        assertEquals("0.1.2.3", Message.ip2string(buf));

        buf=new byte[4];
        buf[0]=(byte)252;
        buf[1]=(byte)253;
        buf[2]=(byte)254;
        buf[3]=(byte)255;
        assertEquals("252.253.254.255",Message.ip2string(buf));

        Message m1=new PingRequest((byte)3);
        Message m2=new PingRequest((byte)3);
        m2.setPriority(5);
        assertGreaterThan(0, m1.compareTo(m2));
        assertLessThan(0, m2.compareTo(m1));
        assertEquals(0, m2.compareTo(m2));
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
            m = Message.read(bais,b,(byte)4);
            fail("bpe should have been thrown.");
        } catch(BadPacketException bpe) {
        }
        PingReply pr = (PingReply)m;
    }
}
