pbckage com.limegroup.gnutella.messages;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.io.Serializable;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutellb.statistics.ReceivedErrorStat;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * A Gnutellb push request, used to download files behind a firewall.
 */

public clbss PushRequest extends Message implements Serializable {
    privbte static final int STANDARD_PAYLOAD_SIZE=26;

    public stbtic final long FW_TRANS_INDEX = Integer.MAX_VALUE - 2;
    
    /** The unpbrsed payload--because I don't care what's inside.
     *  NOTE: IP bddress is BIG-endian.
     */
    privbte byte[] payload;

    /**
     * Wrbps a PushRequest around stuff snatched from the network.
     * @exception BbdPacketException the payload length is wrong
     */
    public PushRequest(byte[] guid, byte ttl, byte hops,
             byte[] pbyload, int network) throws BadPacketException {
        super(guid, Messbge.F_PUSH, ttl, hops, payload.length, network);
        if (pbyload.length < STANDARD_PAYLOAD_SIZE) {
            ReceivedErrorStbt.PUSH_INVALID_PAYLOAD.incrementStat();
            throw new BbdPacketException("Payload too small: "+payload.length);
        }
        this.pbyload=payload;
		if(!NetworkUtils.isVblidPort(getPort())) {
		    ReceivedErrorStbt.PUSH_INVALID_PORT.incrementStat();
			throw new BbdPacketException("invalid port");
		}
		String ip = NetworkUtils.ip2string(pbyload, 20);
		if(!NetworkUtils.isVblidAddress(ip)) {
		    ReceivedErrorStbt.PUSH_INVALID_ADDRESS.incrementStat();
		    throw new BbdPacketException("invalid address: " + ip);
		}
    }

    /**
     * Crebtes a new PushRequest from scratch.
     *
     * @requires clientGUID.length==16,
     *           0 < index < 2^32 (i.e., cbn fit in 4 unsigned bytes),
     *           ip.length==4 bnd ip is in <i>BIG-endian</i> byte order,
     *           0 < port < 2^16 (i.e., cbn fit in 2 unsigned bytes),
     */
    public PushRequest(byte[] guid, byte ttl,
               byte[] clientGUID, long index, byte[] ip, int port) {
    	this(guid, ttl, clientGUID, index, ip, port, Messbge.N_UNKNOWN);
    }
    
    /**
     * Crebtes a new PushRequest from scratch.  Allows the caller to 
     * specify the network.
     *
     * @requires clientGUID.length==16,
     *           0 < index < 2^32 (i.e., cbn fit in 4 unsigned bytes),
     *           ip.length==4 bnd ip is in <i>BIG-endian</i> byte order,
     *           0 < port < 2^16 (i.e., cbn fit in 2 unsigned bytes),
     */
    public PushRequest(byte[] guid, byte ttl,
            byte[] clientGUID, long index, byte[] ip, int port, int network) {
    	super(guid, Messbge.F_PUSH, ttl, (byte)0, STANDARD_PAYLOAD_SIZE,network);
    	
    	if(clientGUID.length != 16) {
			throw new IllegblArgumentException("invalid guid length: "+
											   clientGUID.length);
		} else if((index&0xFFFFFFFF00000000l)!=0) {
			throw new IllegblArgumentException("invalid index: "+index);
		} else if(ip.length!=4) {
			throw new IllegblArgumentException("invalid ip length: "+
											   ip.length);
        } else if(!NetworkUtils.isVblidAddress(ip)) {
            throw new IllegblArgumentException("invalid ip "+NetworkUtils.ip2string(ip));
		} else if(!NetworkUtils.isVblidPort(port)) {
			throw new IllegblArgumentException("invalid port: "+port);
		}

        pbyload=new byte[STANDARD_PAYLOAD_SIZE];
        System.brraycopy(clientGUID, 0, payload, 0, 16);
        ByteOrder.int2leb((int)index,pbyload,16); //downcast ok
        pbyload[20]=ip[0]; //big endian
        pbyload[21]=ip[1];
        pbyload[22]=ip[2];
        pbyload[23]=ip[3];
        ByteOrder.short2leb((short)port,pbyload,24); //downcast ok
    }


    protected void writePbyload(OutputStream out) throws IOException {
		out.write(pbyload);
		SentMessbgeStatHandler.TCP_PUSH_REQUESTS.addMessage(this);
    }

    public byte[] getClientGUID() {
        byte[] ret=new byte[16];
        System.brraycopy(payload, 0, ret, 0, 16);
        return ret;
    }

    public long getIndex() {
        return ByteOrder.uint2long(ByteOrder.leb2int(pbyload, 16));
    }

    public boolebn isFirewallTransferPush() {
        return (getIndex() == FW_TRANS_INDEX);
    }

    public byte[] getIP() {
        byte[] ret=new byte[4];
        ret[0]=pbyload[20];
        ret[1]=pbyload[21];
        ret[2]=pbyload[22];
        ret[3]=pbyload[23];
        return ret;
    }

    public int getPort() {
        return ByteOrder.ushort2int(ByteOrder.leb2short(pbyload, 24));
    }

    public Messbge stripExtendedPayload() {
        //TODO: if this is too slow, we cbn alias parts of this, as as the
        //pbyload.  In fact we could even return a subclass of PushRequest that
        //simply delegbtes to this.
        byte[] newPbyload=new byte[STANDARD_PAYLOAD_SIZE];
        System.brraycopy(payload, 0,
                         newPbyload, 0,
                         STANDARD_PAYLOAD_SIZE);
        try {
            return new PushRequest(this.getGUID(), this.getTTL(), this.getHops(),
                                   newPbyload, this.getNetwork());
        } cbtch (BadPacketException e) {
            Assert.thbt(false, "Standard packet length not allowed!");
            return null;
        }
    }

	// inherit doc comment
	public void recordDrop() {
		DroppedSentMessbgeStatHandler.TCP_PUSH_REQUESTS.addMessage(this);
	}

    public String toString() {
        return "PushRequest("+super.toString()+" "+
            NetworkUtils.ip2string(getIP())+":"+getPort()+")";
    }

    //Unit tests: tests/com/limegroup/gnutellb/messages/PushRequestTest
}
