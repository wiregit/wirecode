package com.limegroup.gnutella.messages;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.sun.java.util.collections.*;
import java.io.*;

public class PingRequestTest extends com.limegroup.gnutella.util.BaseTestCase {
    public PingRequestTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(PingRequestTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }


    //TODO: test other parts of ping!

    public void testQueryKeyPing() {
        try {
            PingRequest pr = new PingRequest();
            assertFalse(pr.isQueryKeyRequest()); // hasn't been hopped yet
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pr.write(baos);
            ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
            PingRequest prRead = (PingRequest) Message.read(bais);
            prRead.hop();
            assertTrue(prRead.isQueryKeyRequest());
        }
        catch (Exception crap) {
            assertTrue(false);
        }
    }


    public void testGGEPPing() {
        // first make a GGEP block....
        GGEP ggepBlock = new GGEP(false);
        ggepBlock.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ggepBlock.write(baos);
        }
        catch (IOException uhoh) {
            assertTrue(false);
        }
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
        assertTrue(currByte >= buffer.length);

        //OK, ggep ping ready
        ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
        Message m = null;
        try{
            m = Message.read(stream);
        }catch(BadPacketException bpe ){
            bpe.printStackTrace();
            assertTrue("TEST FAILED : bad packet in test", false);
        }
        catch (IOException ioe){
            ioe.printStackTrace();
            assertTrue("TEST FAILED : IO Exception", false);
        }
        PingRequest pr = null;
        try{
            pr = (PingRequest)m;
        }catch(ClassCastException cce){
            assertTrue("TEST FAILED : did not create Big ping", false);
        }
        assertTrue(!pr.isQueryKeyRequest());
        pr.hop();
        assertTrue(pr.isQueryKeyRequest());
        //Came this far means its all OK
    }


    public void testBigPing() {
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
        try{
            m = Message.read(stream);
        }catch(BadPacketException bpe ){
            bpe.printStackTrace();
            assertTrue("TEST FAILED : bad packet in test", false);
        }
        catch (IOException ioe){
            ioe.printStackTrace();
            assertTrue("TEST FAILED : IO Exception", false);
        }
        PingRequest pr = null;
        try{
            pr = (PingRequest)m;
        }catch(ClassCastException cce){
            assertTrue("TEST FAILED : did not create Big ping", false);
        }
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        try{
            pr.write(outBuffer);
        }catch (IOException e){
            assertTrue("error while writing payload of Big ping", false);
        }
        String out = outBuffer.toString().substring(23);
        assertTrue("Wrong payload "+out, out.equals("ABCDEFGHIJKLMNOP"));
      
        //Test the new constructor for big pings read from the network
      
        PingRequest bigPing = new PingRequest(GUID.makeGuid(),(byte)7,
                                              (byte)0,payload);
        assertTrue(bigPing.getHops() == 0);
        assertTrue(bigPing.getTTL() == 7);
        assertTrue("bad length "+bigPing.getLength(), bigPing.getLength()==16);
        //Came this far means its all OK
    }

    public void testStripNoPayload() {
        byte[] guid=new byte[16];  guid[0]=(byte)0xFF;        
        PingRequest pr=new PingRequest(guid, (byte)3, (byte)4);
        assertTrue(pr.stripExtendedPayload()==pr);
    }


    public void testStripPayload() {
        byte[] guid=new byte[16];  guid[0]=(byte)0xFF;       
        byte[] payload=new byte[20]; payload[3]=(byte)0xBC;
        PingRequest pr=new PingRequest(guid, (byte)3, (byte)4, payload);
        PingRequest pr2=(PingRequest)pr.stripExtendedPayload();
        assertTrue(pr.getHops()==pr2.getHops());
        assertTrue(pr.getTTL()==pr2.getTTL());
        assertTrue(Arrays.equals(pr.getGUID(), pr2.getGUID()));
        
        assertTrue(pr2.getTotalLength()==23);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try {
            pr2.write(out);
        } catch (IOException e) {
            fail("Unexpected IO problem");
        }
        assertTrue(out.toByteArray().length==23);
        
    }
}
