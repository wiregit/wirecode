package com.limegroup.gnutella.messages;

import com.sun.java.util.collections.*;
import java.io.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * A Gnutella message (packet).  This class is abstract; subclasses
 * implement specific messages such as search requests.<p>
 *
 * All messages have message IDs, function IDs, TTLs, hops taken, and
 * data length.  Messages come in two flavors: requests (ping, search)
 * and replies (pong, search results).  Message are mostly immutable;
 * only the TTL, hops, and priority field can be changed.
 */
public abstract class Message 
    implements Serializable, com.sun.java.util.collections.Comparable {

    /**
     * Operation code for a ping.
     */
    public static final byte F_PING = (byte)0x0;
    
    /**
     * Operation code for a pong.
     */
    public static final byte F_PING_REPLY = (byte)0x1;
    
    /**
     * Operation code for a push.
     */
    public static final byte F_PUSH = (byte)0x40;
    
    /**
     * Operation code for a query.
     */
    public static final byte F_QUERY = (byte)0x80;
    
    /**
     * Operation code for a query hit.
     */
    public static final byte F_QUERY_REPLY = (byte)0x81;
    
    /**
     * Operation code for a route table update.
     */
    public static final byte F_ROUTE_TABLE_UPDATE = (byte)0x30;
    
    /**
     * Operation code for a vendor message.
     */
    public static final byte F_VENDOR_MESSAGE = (byte)0x31;
    
    /**
     * Operation code for a vendor message.
     */
    public static final byte F_VENDOR_MESSAGE_STABLE = (byte)0x32;
        
    /**
     * Cached soft max ttl -- if the TTL+hops is greater than SOFT_MAX,
     * the TTL is set to SOFT_MAX-hops.
     */
    public static final byte SOFT_MAX = 
        ConnectionSettings.SOFT_MAX.getValue();

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


	/**
	 * Constant for whether or not to record stats.
	 */
	protected static final boolean RECORD_STATS = !CommonUtils.isJava118();
    
    public static final int N_UNKNOWN = -1;
    public static final int N_TCP = 1;
    public static final int N_UDP = 2;
    public static final int N_MULTICAST = 3;
   

    ////////////////////// Constructors and Producers /////////////////

    /**
     * @requires func is a valid functional id (i.e., 0, 1, 64, 128, 129),
     *  0 &<;= ttl, 0 &<;= length (i.e., high bit not used)
     * @effects Creates a new message with the following data.
     *  The GUID is set appropriately, and the number of hops is set to 0.
     */
    protected Message(byte func, byte ttl, int length) {
        this(makeGuid(), func, ttl, (byte)0, length, N_UNKNOWN);
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
			throw new IllegalArgumentException("invalid guid length: "+
                guid.length);
		} 		
        this.guid=guid; this.func=func; this.ttl=ttl;
        this.hops=hops; this.length=length; this.network = network;
        //repOk();
    }

    /**
     * @modifies out
     * @effects Writes an encoding of this to out.  Does NOT flush out.
     */
    public void write(OutputStream out) throws IOException {
        byte[] buf=new byte[23];
        for (int i=0; i<16; i++) //TODO3: can optimize
            buf[i]=guid[i];
        buf[16]=func;
        buf[17]=ttl;
        buf[18]=hops;
        ByteOrder.int2leb(length, buf, 19);
        out.write(buf);
        writePayload(out);
    }

    /** @modifies out
     *  @effects writes the payload specific data to out (the stuff
     *   following the header).  Does NOT flush out.
     */
    protected abstract void writePayload(OutputStream out) throws IOException;

     /**
     * @effects Writes given extension string to given stream, adding
     * delimiter if necessary, reporting whether next call should add
     * delimiter. ext may be null or zero-length, in which case this is noop
     */
    protected boolean writeGemExtension(OutputStream os, 
										boolean addPrefixDelimiter, 
										byte[] extBytes) throws IOException {
        if (extBytes == null || (extBytes.length == 0)) {
            return addPrefixDelimiter;
        }
        if(addPrefixDelimiter) {
            os.write(0x1c);
        }
        os.write(extBytes);
        return true; // any subsequent extensions should have delimiter 
    }
    
     /**
     * @effects Writes given extension string to given stream, adding
     * delimiter if necessary, reporting whether next call should add
     * delimiter. ext may be null or zero-length, in which case this is noop
     */
    protected boolean writeGemExtension(OutputStream os, 
										boolean addPrefixDelimiter, 
										String ext) throws IOException {
        if (ext != null)
            return writeGemExtension(os, addPrefixDelimiter, ext.getBytes());
        else
            return writeGemExtension(os, addPrefixDelimiter, new byte[0]);
    }
    
    /**
     * @effects Writes each extension string in exts to given stream,
     * adding delimiters as necessary. exts may be null or empty, in
     *  which case this is noop
     */
    protected boolean writeGemExtensions(OutputStream os, 
										 boolean addPrefixDelimiter, 
										 Iterator iter) throws IOException {
        if (iter == null) {
            return addPrefixDelimiter;
        }
        while(iter.hasNext()) {
            addPrefixDelimiter = writeGemExtension(os, addPrefixDelimiter, 
												   iter.next().toString());
        }
        return addPrefixDelimiter; // will be true is anything at all was written 
    }
    
    /**
     * @effects utility function to read null-terminated byte[] from stream
     */
    protected byte[] readNullTerminatedBytes(InputStream is) 
        throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i;
        while ((is.available()>0)&&(i=is.read())!=0) {
            baos.write(i);
        }
        return baos.toByteArray();
    }

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
    public int compareTo(Object message) {
        Message m=(Message)message;
        return m.getPriority() - this.getPriority();
    }

    public String toString() {
        return "{guid="+(new GUID(guid)).toString()
             +", ttl="+ttl
             +", priority="+getPriority()+"}";
    }

	/**
	 * Records the dropping of this message in statistics.
	 */
	public abstract void recordDrop();
}
