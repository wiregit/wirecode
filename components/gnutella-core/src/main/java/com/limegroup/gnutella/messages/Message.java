package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;

/**
 * A Gnutella message (packet).  This class is abstract; subclasses
 * implement specific messages such as search requests.<p>
 *
 * All messages have message IDs, function IDs, TTLs, hops taken, and
 * data length.  Messages come in two flavors: requests (ping, search)
 * and replies (pong, search results).  Message are mostly immutable;
 * only the TTL, hops, and priority field can be changed.
 */
public abstract class Message implements Comparable<Message> {
    
    // Functional IDs defined by Gnutella protocol.
    public static final byte F_PING                  = (byte)0x0;
    public static final byte F_PING_REPLY            = (byte)0x1;
    public static final byte F_PUSH                  = (byte)0x40;
    public static final byte F_QUERY                 = (byte)0x80;
    public static final byte F_QUERY_REPLY           = (byte)0x81;
    public static final byte F_ROUTE_TABLE_UPDATE    = (byte)0x30;
    public static final byte F_VENDOR_MESSAGE        = (byte)0x31;
    public static final byte F_VENDOR_MESSAGE_STABLE = (byte)0x32;
    public static final byte F_UDP_CONNECTION        = (byte)0x41;
    
    public static final int N_UNKNOWN = -1;
    public static final int N_TCP = 1;
    public static final int N_UDP = 2;
    public static final int N_MULTICAST = 3;

    /** Same as GUID.makeGUID.  This exists for backwards compatibility. */
    public static byte[] makeGuid() {
        return GUID.makeGuid();
    }

    ////////////////////////// Instance Data //////////////////////

    private byte[] guid;
    private final byte func;

    /* We do not support TTLs > 2^7, nor do we support packets
     * of length > 2^31 */
    private byte ttl;
    private byte hops;
    private int length;

    /** Priority for flow-control.  Lower numbers mean higher priority.NOT
     *  written to network. */
    private int priority=0;
    /** Time this was created.  Not written to network. */
    private final long creationTime=System.currentTimeMillis();
    /**
     * The network that this was received on or is going to be sent to.
     */
    private final int network;
   
    /** Rep. invariant */
    protected void repOk() {
        Assert.that(guid.length==16);
        Assert.that(func==F_PING || func==F_PING_REPLY
                    || func==F_PUSH
                    || func==F_QUERY || func==F_QUERY_REPLY
                    || func==F_VENDOR_MESSAGE 
                    || func == F_VENDOR_MESSAGE_STABLE
                    || func == F_UDP_CONNECTION,
                    "Bad function code");

        if (func==F_PUSH) Assert.that(length==26, "Bad push length: "+length);
        Assert.that(ttl>=0, "Negative TTL: "+ttl);
        Assert.that(hops>=0, "Negative hops: "+hops);
        Assert.that(length>=0, "Negative length: "+length);
    }

    ////////////////////// Constructors and Producers /////////////////

    /**
     * @requires func is a valid functional id (i.e., 0, 1, 64, 128, 129),
     *  0 &<;= ttl, 0 &<;= length (i.e., high bit not used)
     * @effects Creates a new message with the following data.
     *  The GUID is set appropriately, and the number of hops is set to 0.
     */
    protected Message(byte func, byte ttl, int length) {
        this(func, ttl, length, N_UNKNOWN);
    }

    protected Message(byte func, byte ttl, int length, int network) {
        this(makeGuid(), func, ttl, (byte)0, length, network);
    }

    /**
     * Same as above, but caller specifies TTL and number of hops.
     * This is used when reading packets off network.
     */
    protected Message(byte[] guid, byte func, byte ttl,
              byte hops, int length) {
        this(guid, func, ttl, hops, length, N_UNKNOWN);
    }

    /**
     * Same as above, but caller specifies the network.
     * This is used when reading packets off network.
     */
    protected Message(byte[] guid, byte func, byte ttl,
              byte hops, int length, int network) {
		if(guid.length != 16) {
			throw new IllegalArgumentException("invalid guid length: "+guid.length);
		} 		
        this.guid=guid; this.func=func; this.ttl=ttl;
        this.hops=hops; this.length=length; this.network = network;
        //repOk();
    }
	
    /**
     * Writes a message quickly, without using temporary buffers or crap.
     */
    public void writeQuickly(OutputStream out) throws IOException {
        out.write(guid, 0, 16);
        out.write(func);
        out.write(ttl);
        out.write(hops);
        ByteOrder.int2leb(length, out);
        writePayload(out);
    }
    
    /**
     * Writes a message out, using the buffer as the temporary header.
     */
    public void write(OutputStream out, byte[] buf) throws IOException {
        System.arraycopy(guid, 0, buf, 0, guid.length /* 16 */);
        buf[16]=func;
        buf[17]=ttl;
        buf[18]=hops;
        ByteOrder.int2leb(length, buf, 19);
        out.write(buf);
        writePayload(out);
    }

    /**
     * @modifies out
     * @effects Writes an encoding of this to out.  Does NOT flush out.
     */
    public void write(OutputStream out) throws IOException {
        write(out, new byte[23]);
    }

    /** @modifies out
     *  @effects writes the payload specific data to out (the stuff
     *   following the header).  Does NOT flush out.
     */
    protected abstract void writePayload(OutputStream out) throws IOException;
    
    ////////////////////////////////////////////////////////////////////
    public int getNetwork() {
        return network;
    }
    
    public boolean isMulticast() {
        return network == N_MULTICAST;
    }
    
    public boolean isUDP() {
        return network == N_UDP;
    }
    
    public boolean isTCP() {
        return network == N_TCP;
    }
    
    public boolean isUnknownNetwork() {
        return network == N_UNKNOWN;
    }

    public byte[] getGUID() {
        return guid;
    }

    public byte getFunc() {
        return func;
    }

    public byte getTTL() {
        return ttl;
    }

    /**
     * If ttl is less than zero, throws IllegalArgumentException.  Otherwise sets
     * this TTL to the given value.  This is useful when you want certain messages
     * to travel less than others.
     *    @modifies this' TTL
     */
    public void setTTL(byte ttl) throws IllegalArgumentException {
        if (ttl < 0)
            throw new IllegalArgumentException("invalid TTL: "+ttl);
        this.ttl = ttl;
    }
    
    /**
     * Sets the guid for this message. Is needed, when we want to cache 
     * query replies or other messages, and change the GUID as per the 
     * request
     * @param guid The guid to be set
     */
    protected void setGUID(GUID guid) {
        this.guid = guid.bytes();
    }
    
    /**
     * If the hops is less than zero, throws IllegalArgumentException.
     * Otherwise sets this hops to the given value.  This is useful when you
     * want certain messages to look as if they've travelled further.
     *   @modifies this' hops
     */
    public void setHops(byte hops) throws IllegalArgumentException {
        if(hops < 0)
            throw new IllegalArgumentException("invalid hops: " + hops);
        this.hops = hops;
    }

    public byte getHops() {
        return hops;
    }

    /** Returns the length of this' payload, in bytes. */
    public int getLength() {
        return length;
    }

    /** Updates length of this' payload, in bytes. */
    protected void updateLength(int l) {
        length=l;
    }

    /** Returns the total length of this, in bytes */
    public int getTotalLength() {
        //Header is 23 bytes.
        return 23+length;
    }

    /** @modifies this
     *  @effects increments hops, decrements TTL if > 0, and returns the
     *   OLD value of TTL.
     */
    public byte hop() {
        hops++;
        if (ttl>0)
            return ttl--;
        else
            return ttl;
    }

    /** 
     * Returns the system time (i.e., the result of System.currentTimeMillis())
     * this was instantiated.
     */
    public long getCreationTime() {
        return creationTime;
    }

    /** Returns this user-defined priority.  Lower values are higher priority. */
    public int getPriority() {
        return priority;
    }

    /** Set this user-defined priority for flow-control purposes.  Lower values
     *  are higher priority. */
    public void setPriority(int priority) {
        this.priority=priority;
    }

    /** 
     * Returns a message identical to this but without any extended (typically
     * GGEP) data.  Since Message's are mostly immutable, the returned message
     * may alias parts of this; in fact the returned message could even be this.
     * The caveat is that the hops and TTL field of Message can be mutated for
     * efficiency reasons.  Hence you must not call hop() on either this or the
     * returned value.  Typically this is not a problem, as hop() is called
     * before forwarding/broadcasting a message.  
     *
     * @return an instance of this without any dangerous extended payload
     */
    public abstract Message stripExtendedPayload();

    /** 
     * Returns a negative value if this is of lesser priority than message,
     * positive value if of higher priority, or zero if of same priority.
     * Remember that lower priority numbers mean HIGHER priority.
     *
     * @exception ClassCastException message not an instance of Message 
     */
    public int compareTo(Message m) {
        return m.getPriority() - this.getPriority();
    }

    public String toString() {
        return "{guid="+(new GUID(guid)).toString()
             +", ttl="+ttl
             +", hops="+hops
             +", priority="+getPriority()+"}";
    }

	/**
	 * Records the dropping of this message in statistics.
	 */
	public abstract void recordDrop();
}
