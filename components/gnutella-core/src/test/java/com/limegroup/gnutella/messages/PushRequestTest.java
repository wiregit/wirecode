package com.limegroup.gnutella.messages;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.io.*;

public class PushRequestTest extends TestCase {
    public PushRequestTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(PushRequestTest.class);
    }

    public void testBasic() {
        byte[] guid=new byte[16];
        byte[] clientGUID=new byte[16]; clientGUID[0]=(byte)0xFF;
        clientGUID[15]=(byte)0xF1;
        long index=2343;
        byte[] ip={(byte)0xFF, (byte)0, (byte)0, (byte)1};
        int port=6346;

        PushRequest pr=new PushRequest(guid, (byte)0,
                                       clientGUID, index, ip, port);
        assertTrue(Arrays.equals(pr.getClientGUID(), clientGUID));
        assertTrue(pr.getIndex()==index);
        assertTrue(Arrays.equals(pr.getIP(), ip));
        assertTrue(pr.getPort()==port);

        //Test some maximum values
        long u4=0x00000000FFFFFFFFl;
        int u2=0x0000FFFF;
        pr=new PushRequest(guid, (byte)0,
                           clientGUID, u4, ip, u2);
        assertTrue(pr.getIndex()==u4);
        assertTrue(pr.getPort()==u2);
    }

    public void testBigPush() {        
        byte[] bytes=new byte[23+26+10];
        bytes[16]=Message.F_PUSH;
        bytes[17]=(byte)3;     //hops
        bytes[18]=(byte)3;     //ttl
        bytes[19]=(byte)26+10; //payload length
        bytes[23+16]=(byte)3;  //index
        bytes[23+26+3]=(byte)7;//random big pong payload
        ByteArrayInputStream in=new ByteArrayInputStream(bytes);
        try {
            //1. Test that we can read big push
            PushRequest pr=(PushRequest)Message.read(in);            
            assertEquals(pr.getIndex(), 3);
            assertEquals(pr.getTotalLength(), bytes.length);
            assertEquals(pr.getLength(), bytes.length-23);

            //2. Test that yields returns the same thing
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            pr.write(out);
            assertTrue(Arrays.equals(out.toByteArray(),
                                     bytes));

            //3. Test that we can strip the payload out
            PushRequest pr2=(PushRequest)pr.stripExtendedPayload();
            assertEquals(pr2.getLength(), 26);
            assertEquals(pr2.getHops(), pr.getHops());
            ByteArrayOutputStream out2=new ByteArrayOutputStream();
            pr2.write(out2);
            byte[] bytes2=out2.toByteArray();
            assertEquals(bytes2.length, 23+26);
            for (int i=0; i<bytes2.length; i++)
                if (i!=19) //skip payload length
                    assertEquals(bytes2[i], bytes[i]);
        } catch (BadPacketException e) {
            fail("Bad packet exception: "+e);
        } catch (IOException e) {
            fail("Unexpected IO problem");
        }
    }

    public void testPushTooSmall() {
        byte[] bytes=new byte[23+25];  //one byte too small
        bytes[16]=Message.F_PUSH;
        bytes[17]=(byte)3;     //hops
        bytes[18]=(byte)3;     //ttl
        bytes[19]=(byte)25;    //payload length
        bytes[23+16]=(byte)3;  //index
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
