package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import junit.framework.Test;

public class PushRequestTest extends com.limegroup.gnutella.util.LimeTestCase {
    public PushRequestTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PushRequestTest.class);
    }

    public void testBasic() {
        byte[] guid=new byte[16];
        byte[] clientGUID=new byte[16]; clientGUID[0]=(byte)0xFF;
        clientGUID[15]=(byte)0xF1;
        long index=2343;
        byte[] ip={(byte)0xFE, (byte)0, (byte)0, (byte)1};
        int port=6346;

        PushRequest pr=new PushRequest(guid, (byte)0,
                                       clientGUID, index, ip, port);
        assertTrue(Arrays.equals(pr.getClientGUID(), clientGUID));
        assertEquals(index, pr.getIndex());
        assertTrue(Arrays.equals(pr.getIP(), ip));
        assertEquals(port, pr.getPort());

        //Test some maximum values
        long u4=0x00000000FFFFFFFFl;
        int u2=0x0000FFFF;
        pr=new PushRequest(guid, (byte)0,
                           clientGUID, u4, ip, u2);
        assertEquals(u4, pr.getIndex());
        assertEquals(u2, pr.getPort());
    }

    public void testBigPush() throws Exception {        
        byte[] bytes=new byte[23+26+10];
        bytes[16]=Message.F_PUSH;
        bytes[17]=(byte)2;     //ttl .. ttl + hops must be <= 3
        bytes[18]=(byte)1;     //hops
        bytes[19]=(byte)26+10; //payload length
        bytes[23+16]=(byte)3;  //index
        bytes[23+20]=(byte)254; // non-zero ip.
        bytes[23+24]=(byte)1;  //non-zero port.
        bytes[23+26+3]=(byte)7;//random big pong payload
        ByteArrayInputStream in=new ByteArrayInputStream(bytes);
        //1. Test that we can read big push
        PushRequest pr=(PushRequest)MessageFactory.read(in);     
        assertEquals("unexpected push index", 3, pr.getIndex());
        assertEquals("unexpected total length", bytes.length,
            pr.getTotalLength() );
        assertEquals("unexpected length", bytes.length-23,
            pr.getLength());
        assertEquals("unexpected func", Message.F_PUSH, pr.getFunc());
        assertEquals("unexpected hops", (byte)1, pr.getHops());
        assertEquals("unexpected ttl", (byte)2, pr.getTTL());

        //2. Test that yields returns the same thing
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        pr.write(out);
        byte[] outBytes = out.toByteArray();        
        assertEquals("written push different length than read push",
            outBytes.length, bytes.length);
        for (int i=0; i<outBytes.length; i++)
            assertEquals("byte # " + i + " not equal", bytes[i], outBytes[i]);

        //assertTrue("written bytes should be equal", 
        //           Arrays.equals(out.toByteArray(), bytes));
    }

    public void testPushTooSmall() throws Exception {
        byte[] bytes=new byte[23+25];  //one byte too small
        bytes[16]=Message.F_PUSH;
        bytes[17]=(byte)3;     //hops
        bytes[18]=(byte)3;     //ttl
        bytes[19]=(byte)25;    //payload length
        bytes[23+16]=(byte)3;  //index
        ByteArrayInputStream in=new ByteArrayInputStream(bytes);
        try {
            MessageFactory.read(in);
            fail("No exception thrown");
        } catch (BadPacketException pass) { 
            //Pass!
        }
        
    }
    
    public void testNetworkConstructor() throws Exception {
    	byte[] guid=new byte[16];
        byte[] clientGUID=new byte[16]; clientGUID[0]=(byte)0xFF;
        clientGUID[15]=(byte)0xF1;
        long index=2343;
        byte[] ip={(byte)0xFE, (byte)0, (byte)0, (byte)1};
        int port=6346;

        PushRequest pr=new PushRequest(guid, (byte)0,
                                       clientGUID, index, ip, port,
									   Message.N_UDP);
        
        assertEquals(Message.N_UDP,pr.getNetwork());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pr.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PushRequest pr2 = (PushRequest)MessageFactory.read(bais);
        assertNotNull(pr2);
    }
}
