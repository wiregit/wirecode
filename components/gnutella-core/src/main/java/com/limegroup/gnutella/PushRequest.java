package com.limegroup.gnutella;

import java.io.*;
import java.util.*;

/**
 * A Gnutella push request, used to download files behind a firewall.
 */

public class PushRequest extends Message implements Serializable {
    /** The unparsed payload--because I don't care what's inside.
    NOTE: IP address is BIG-endian.
     */
    private byte[] payload;

    /**
     * Wraps a PushRequest around stuff snatched from the network.
     *
     * @requires payload.length==26
     */
    public PushRequest(byte[] guid, byte ttl, byte hops,
             byte[] payload) {
        super(guid, Message.F_PUSH, ttl, hops, 26);
        Assert.that(payload.length==26);
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
        super(guid, Message.F_PUSH, ttl, (byte)0, 26);
        Assert.that(clientGUID.length==16);
        Assert.that((index&0xFFFFFFFF00000000l)==0);
        Assert.that(ip.length==4);
        Assert.that((port&0xFFFF0000)==0);

        payload=new byte[26];
        System.arraycopy(clientGUID, 0, payload, 0, 16);
        ByteOrder.int2leb((int)index,payload,16); //downcast ok
        payload[20]=ip[0]; //big endian
        payload[21]=ip[1];
        payload[22]=ip[2];
        payload[23]=ip[3];
        ByteOrder.short2leb((short)port,payload,24); //downcast ok
    }


    protected void writePayload(OutputStream out) throws IOException {
        for (int i=0; i<payload.length; i++) {
            //TODO3: buffer and send in batch.
            out.write(payload[i]);
        }
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

    public String toString() {
        return "PushRequest("+super.toString()+")";
    }

//      /** Unit tests */
//      public static void main(String args[]) {
//      byte[] guid=new byte[16];
//      byte[] clientGUID=new byte[16]; clientGUID[0]=(byte)0xFF;
//          clientGUID[15]=(byte)0xF1;
//      long index=2343;
//      byte[] ip={(byte)0xFF, (byte)0, (byte)0, (byte)1};
//      int port=6346;

//      PushRequest pr=new PushRequest(guid, (byte)0,
//                         clientGUID, index, ip, port);
//      Assert.that(Arrays.equals(pr.getClientGUID(), clientGUID));
//      Assert.that(pr.getIndex()==index);
//      Assert.that(Arrays.equals(pr.getIP(), ip));
//      Assert.that(pr.getPort()==port);

//      //Test some maximum values
//      long u4=0x00000000FFFFFFFFl;
//      int u2=0x0000FFFF;
//      pr=new PushRequest(guid, (byte)0,
//                 clientGUID, u4, ip, u2);
//      Assert.that(pr.getIndex()==u4);
//      Assert.that(pr.getPort()==u2);
//      }
}
