package com.limegroup.gnutella;

import java.io.*;

/**
 * A Gnutella ping message.
 */

public class PingRequest extends Message {
    
    /**
     * With the Big Ping and Big Pong extensions pings may have a payload
     */
    private byte[] payload = null;
    
    /////////////////Constructors for incoming messages/////////////////
    /**
     * Creates a normal ping from data read on the network
     */
    public PingRequest(byte[] guid, byte ttl, byte hops) {
        super(guid, Message.F_PING, ttl, hops, 0);
    }

    /**
     * Creates an incoming group ping. Used only by boot-strap server
     */
    protected PingRequest(byte[] guid, byte ttl, byte hops, byte length) {
        super(guid, Message.F_PING, ttl, hops, length);
    }

    /**
     * Creates a big ping request from data read from the network
     * 
     * @param payload the headers etc. which the big pings contain.
     */
    public PingRequest(byte[] guid, byte ttl, byte hops, byte[] payload) {
        super(guid, Message.F_PING, ttl, hops, payload.length);
        this.payload = payload;
    }

    //////////////////////Constructors for outgoing Pings/////////////
    /**
     * Creates a normal ping with a new GUID
     *
     * @param ttl the ttl of the new Ping
     */
    public PingRequest(byte ttl) {
        super((byte)0x0, ttl, (byte)0);
    }

    /**
     * Creates an outgoing group ping. Used only by boot-strap server
     *
     * @param length is length of the payload of the GroupPing = 
     * 14(port+ip+files+kbytes)+group.length + 1(null)
     */
    protected PingRequest(byte ttl, byte length) {
        super((byte)0x0, ttl, (byte)length);
    }


    /////////////////////////////methods///////////////////////////

    protected void writePayload(OutputStream out) throws IOException {
        if(payload != null)
            out.write(payload);
        //Do nothing...there is no payload!
    }

    public String toString() {
        return "PingRequest("+super.toString()+")";
    }

    /////////////////////Unit Test/////////////////////////////
    /*
      public static void main(String args[]) {
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
      Assert.that(false, "TEST FAILED : bad packet in test");
      }
      catch (IOException ioe){
      ioe.printStackTrace();
      Assert.that(false, "TEST FAILED : IO Exception");
      }
      PingRequest pr = null;
      try{
      pr = (PingRequest)m;
      }catch(ClassCastException cce){
      Assert.that(false,"TEST FAILED : did not create Big ping");
      }
      ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
      try{
      pr.writePayload(outBuffer);
      }catch (IOException e){
      Assert.that(false, "error while writing payload of Big ping");
      }
      String out = outBuffer.toString();
      Assert.that(out.equals
      ("ABCDEFGHIJKLMNOP"),"wrong payload "+out);
      
      //Test the new constructor for big pings read from the network
      
      PingRequest bigPing = new PingRequest(GUID.makeGuid(),(byte)7,
      (byte)0,payload);
      Assert.that(bigPing.getHops() == 0);
      Assert.that(bigPing.getTTL() == 7);
      Assert.that(bigPing.getLength()==16,"bad length "+bigPing.getLength());
      //Came this far means its all OK
      System.out.println("Passed");
      }
    */
}
