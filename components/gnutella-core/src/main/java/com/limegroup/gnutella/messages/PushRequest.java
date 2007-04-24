package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.limewire.io.NetworkUtils;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * A Gnutella push request, used to download files behind a firewall.
 */

public class PushRequest extends Message implements Serializable {
    private static final int STANDARD_PAYLOAD_SIZE=26;

    public static final long FW_TRANS_INDEX = Integer.MAX_VALUE - 2;
    
    /** The unparsed payload--because I don't care what's inside.
     *  NOTE: IP address is BIG-endian.
     */
    private byte[] payload;

    /**
     * Wraps a PushRequest around stuff snatched from the network.
     * @exception BadPacketException the payload length is wrong
     */
    public PushRequest(byte[] guid, byte ttl, byte hops,
             byte[] payload, Network network) throws BadPacketException {
        super(guid, Message.F_PUSH, ttl, hops, payload.length, network);
        if (payload.length < STANDARD_PAYLOAD_SIZE) {
            ReceivedErrorStat.PUSH_INVALID_PAYLOAD.incrementStat();
            throw new BadPacketException("Payload too small: "+payload.length);
        }
        this.payload=payload;
		if(!NetworkUtils.isValidPort(getPort())) {
		    ReceivedErrorStat.PUSH_INVALID_PORT.incrementStat();
			throw new BadPacketException("invalid port");
		}
		String ip = NetworkUtils.ip2string(payload, 20);
		if(!NetworkUtils.isValidAddress(ip)) {
		    ReceivedErrorStat.PUSH_INVALID_ADDRESS.incrementStat();
		    throw new BadPacketException("invalid address: " + ip);
		}
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
    	this(guid, ttl, clientGUID, index, ip, port, Network.UNKNOWN);
    }
    
    /**
     * Creates a new PushRequest from scratch.  Allows the caller to 
     * specify the network.
     *
     * @requires clientGUID.length==16,
     *           0 < index < 2^32 (i.e., can fit in 4 unsigned bytes),
     *           ip.length==4 and ip is in <i>BIG-endian</i> byte order,
     *           0 < port < 2^16 (i.e., can fit in 2 unsigned bytes),
     */
    public PushRequest(byte[] guid, byte ttl,
            byte[] clientGUID, long index, byte[] ip, int port, Network network) {
    	super(guid, Message.F_PUSH, ttl, (byte)0, STANDARD_PAYLOAD_SIZE,network);
    	
    	if(clientGUID.length != 16) {
			throw new IllegalArgumentException("invalid guid length: "+
											   clientGUID.length);
		} else if((index&0xFFFFFFFF00000000l)!=0) {
			throw new IllegalArgumentException("invalid index: "+index);
		} else if(ip.length!=4) {
			throw new IllegalArgumentException("invalid ip length: " + ip.length);
        } else if(!NetworkUtils.isValidAddress(ip)) {
            throw new IllegalArgumentException("invalid ip "+NetworkUtils.ip2string(ip));
		} else if(!NetworkUtils.isValidPort(port)) {
			throw new IllegalArgumentException("invalid port: "+port);
		}

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
    }

    public byte[] getClientGUID() {
        byte[] ret=new byte[16];
        System.arraycopy(payload, 0, ret, 0, 16);
        return ret;
    }

    public long getIndex() {
        return ByteOrder.uint2long(ByteOrder.leb2int(payload, 16));
    }

    public boolean isFirewallTransferPush() {
        return (getIndex() == FW_TRANS_INDEX);
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
        return ByteOrder.ushort2int(ByteOrder.leb2short(payload, 24));
    }

	// inherit doc comment
	public void recordDrop() {
		DroppedSentMessageStatHandler.TCP_PUSH_REQUESTS.addMessage(this);
	}

    public String toString() {
        return "PushRequest("+super.toString()+" "+
            NetworkUtils.ip2string(getIP())+":"+getPort()+")";
    }

    //Unit tests: tests/com/limegroup/gnutella/messages/PushRequestTest
}
