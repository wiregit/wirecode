package com.limegroup.gnutella;

import com.limegroup.gnutella.statistics.*;
import java.io.*;
import com.sun.java.util.collections.Arrays;

/**
 * A Gnutella push request, used to download files behind a firewall.
 */

public class PushRequest extends Message implements Serializable {
    private static final int STANDARD_PAYLOAD_SIZE=26;
    
    /** The unparsed payload--because I don't care what's inside.
     *  NOTE: IP address is BIG-endian.
     */
    private byte[] payload;

    /**
     * Wraps a PushRequest around stuff snatched from the network.
     * @exception BadPacketException the payload length is wrong
     */
    public PushRequest(byte[] guid, byte ttl, byte hops,
             byte[] payload) throws BadPacketException {
        super(guid, Message.F_PUSH, ttl, hops, payload.length);
        if (payload.length < STANDARD_PAYLOAD_SIZE)
            throw new BadPacketException("Payload too small: "+payload.length);
        this.payload=payload;
    }

    /**
     * Creates a new PushRequest from scratch.
     *
     * @requires clientGUID.length==16,
     *           0 < index < 2^32 (i.e., can fit in 4 unsigned bytes),
     *           ip.length==4 and ip is in <i>BIG-endian</i> byte order,
     *           0 < port < 2^16 (i.e., can fit in 2 unsigned bytes),
     */
    public PushRequest(byte[] guid, byte ttl,
               byte[] clientGUID, long index, byte[] ip, int port) {
        super(guid, Message.F_PUSH, ttl, (byte)0, STANDARD_PAYLOAD_SIZE);
        Assert.that(clientGUID.length==16);
        Assert.that((index&0xFFFFFFFF00000000l)==0);
        Assert.that(ip.length==4);
        Assert.that((port&0xFFFF0000)==0);

        payload=new byte[STANDARD_PAYLOAD_SIZE];
        System.arraycopy(clientGUID, 0, payload, 0, 16);
        ByteOrder.int2leb((int)index,payload,16); //downcast ok
        payload[20]=ip[0]; //big endian
        payload[21]=ip[1];
        payload[22]=ip[2];
        payload[23]=ip[3];
        ByteOrder.short2leb((short)port,payload,24); //downcast ok
    }


    protected void writePayload(OutputStream out) throws IOException {
        out.write(payload);
        SentMessageStatHandler.TCP_PUSH_REQUESTS.addMessage(this);
        //for (int i=0; i<payload.length; i++) {
            //TODO3: buffer and send in batch.
        //out.write(payload[i]);
        //}
    }

    public byte[] getClientGUID() {
        byte[] ret=new byte[16];
        System.arraycopy(payload, 0, ret, 0, 16);
        return ret;
    }

    public long getIndex() {
        return ByteOrder.ubytes2long(ByteOrder.leb2int(payload, 16));
    }

    public byte[] getIP() {
        byte[] ret=new byte[4];
        ret[0]=payload[20];
        ret[1]=payload[21];
        ret[2]=payload[22];
        ret[3]=payload[23];
        return ret;
    }

    public int getPort() {
        return ByteOrder.ubytes2int(ByteOrder.leb2short(payload, 24));
    }

    public Message stripExtendedPayload() {
        //TODO: if this is too slow, we can alias parts of this, as as the
        //payload.  In fact we could even return a subclass of PushRequest that
        //simply delegates to this.
        byte[] newPayload=new byte[STANDARD_PAYLOAD_SIZE];
        System.arraycopy(payload, 0,
                         newPayload, 0,
                         STANDARD_PAYLOAD_SIZE);
        try {
            return new PushRequest(this.getGUID(), this.getTTL(), this.getHops(),
                                   newPayload);
        } catch (BadPacketException e) {
            Assert.that(false, "Standard packet length not allowed!");
            return null;
        }
    }

    public String toString() {
        return "PushRequest("+super.toString()+")";
    }

    //Unit tests: tests/com/limegroup/gnutella/messages/PushRequestTest
}
