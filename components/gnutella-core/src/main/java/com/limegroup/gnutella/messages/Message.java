package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Iterator;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.udpconnect.UDPConnectionMessage;

/**
 * A Gnutella message (packet).  This class is abstract; subclasses
 * implement specific messages such as search requests.<p>
 *
 * All messages have message IDs, function IDs, TTLs, hops taken, and
 * data length.  Messages come in two flavors: requests (ping, search)
 * and replies (pong, search results).  Message are mostly immutable;
 * only the TTL, hops, and priority field can be changed.
 */
public abstract class Message implements Serializable, Comparable {
    //Functional IDs defined by Gnutella protocol.
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
   
    /** Rep. invariant */
    protected void repOk() {
        Assert.that(guid.length==16);
        Assert.that(func==F_PING || func==F_PING_REPLY
                    || func==F_PUSH
                    || func==F_QUERY || func==F_QUERY_REPLY
                    || func==F_VENDOR_MESSAGE 
                    || func == F_VENDOR_MESSAGE_STABLE,
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
     * Reads a Gnutella message from the specified input stream.  The returned
     * message can be any one of the recognized Gnutella message, such as
     * queries, query hits, pings, pongs, etc.
     *
     * @param in the <tt>InputStream</tt> instance containing message data
     * @return a new Gnutella message instance
     * @throws <tt>BadPacketException</tt> if the message is not considered
     *  valid for any reason
     * @throws <tt>IOException</tt> if there is any IO problem reading the
     *  message
     */
    public static Message read(InputStream in)
		throws BadPacketException, IOException {
        return Message.read(in, new byte[23], N_UNKNOWN, SOFT_MAX);
    }

    /**
     * @modifies in
     * @effects reads a packet from the network and returns it as an
     *  instance of a subclass of Message, unless one of the following happens:
     *    <ul>
     *    <li>No data is available: returns null
     *    <li>A bad packet is read: BadPacketException.  The client should be
     *      able to recover from this.
     *    <li>A major problem occurs: IOException.  This includes reading packets
     *      that are ridiculously long and half-completed messages. The client
     *      is not expected to recover from this.
     *    </ul>
     */
    public static Message read(InputStream in, byte softMax)
		throws BadPacketException, IOException {
        return Message.read(in, new byte[23], N_UNKNOWN, softMax);
    }
    
    /**
     * @modifies in
     * @effects reads a packet from the network and returns it as an
     *  instance of a subclass of Message, unless one of the following happens:
     *    <ul>
     *    <li>No data is available: returns null
     *    <li>A bad packet is read: BadPacketException.  The client should be
     *      able to recover from this.
     *    <li>A major problem occurs: IOException.  This includes reading packets
     *      that are ridiculously long and half-completed messages. The client
     *      is not expected to recover from this.
     *    </ul>
     */
    public static Message read(InputStream in, int network)
		throws BadPacketException, IOException {
        return Message.read(in, new byte[23], network, SOFT_MAX);
    }    
    
    /**
     * @requires buf.length==23
     * @effects exactly like Message.read(in), but buf is used as scratch for
     *  reading the header.  This is an optimization that lets you avoid
     *  repeatedly allocating 23-byte arrays.  buf may be used when this returns,
     *  but the contents are not guaranteed to contain any useful data.  
     */
    public static Message read(InputStream in, byte[] buf, byte softMax)
		throws BadPacketException, IOException {
        return Message.read(in, buf, N_UNKNOWN, softMax);
    }
    
    /**
     * Reads a message using the specified buffer & network and the default
     * soft max.
     */
    public static Message read(InputStream in, int network, byte[] buf)
        throws BadPacketException, IOException {
            return Message.read(in, buf, network, SOFT_MAX);
    }


    /**
     * @param network the network this was received from.
     * @requires buf.length==23
     * @effects exactly like Message.read(in), but buf is used as scratch for
     *  reading the header.  This is an optimization that lets you avoid
     *  repeatedly allocating 23-byte arrays.  buf may be used when this returns,
     *  but the contents are not guaranteed to contain any useful data.  
     */
    public static Message read(InputStream in, byte[] buf, int network, byte softMax)
		throws BadPacketException, IOException {

        //1. Read header bytes from network.  If we timeout before any
        //   data has been read, return null instead of throwing an
        //   exception.
        for (int i=0; i<23; ) {
            int got;
            try {
                got=in.read(buf, i, 23-i);
            } catch (InterruptedIOException e) {
                //have we read any of the message yet?
                if (i==0) return null;
                else throw e;
            }
            if (got==-1) {
                ReceivedErrorStat.CONNECTION_CLOSED.incrementStat();
                throw new IOException("Connection closed.");
            }
            i+=got;
        }

        //2. Unpack.
        byte func=buf[16];
        byte ttl=buf[17];
        byte hops=buf[18];
        int length=ByteOrder.leb2int(buf,19);
        //2.5 If the length is hopelessly off (this includes lengths >
        //    than 2^31 bytes, throw an irrecoverable exception to
        //    cause this connection to be closed.
        if (length<0 || length > MessageSettings.MAX_LENGTH.getValue()) {
            ReceivedErrorStat.INVALID_LENGTH.incrementStat();
            throw new IOException("Unreasonable message length: "+length);
        }

        //3. Read rest of payload.  This must be done even for bad
        //   packets, so we can resume reading packets.
        byte[] payload=null;
        if (length!=0) {
            payload=new byte[length];
            for (int i=0; i<length; ) {
                int got=in.read(payload, i, length-i);
                if (got==-1) {
                    ReceivedErrorStat.CONNECTION_CLOSED.incrementStat();
                    throw new IOException("Connection closed.");
                }
                i+=got;
            }
        }
        else
            payload = new byte[0];

        //4. Check values.   These are based on the recommendations from the
        //   GnutellaDev page.  This also catches those TTLs and hops whose
        //   high bit is set to 0.
        byte hardMax = (byte)14;
        if (hops<0) {
            ReceivedErrorStat.INVALID_HOPS.incrementStat();
            throw new BadPacketException("Negative (or very large) hops");
        } else if (ttl<0) {
            ReceivedErrorStat.INVALID_TTL.incrementStat();
            throw new BadPacketException("Negative (or very large) TTL");
        } else if ((hops > softMax) && 
                 (func != F_QUERY_REPLY) &&
                 (func != F_PING_REPLY)) {
            ReceivedErrorStat.HOPS_EXCEED_SOFT_MAX.incrementStat();
            throw BadPacketException.HOPS_EXCEED_SOFT_MAX;
        }
        else if (ttl+hops > hardMax) {
            ReceivedErrorStat.HOPS_AND_TTL_OVER_HARD_MAX.incrementStat();
            throw new BadPacketException("TTL+hops exceeds hard max; probably spam");
        } else if ((ttl+hops > softMax) && 
                 (func != F_QUERY_REPLY) &&
                 (func != F_PING_REPLY)) {
            ttl=(byte)(softMax - hops);  //overzealous client;
                                         //readjust accordingly
            Assert.that(ttl>=0);     //should hold since hops<=softMax ==>
                                     //new ttl>=0
        }

		// Delayed GUID allocation
        byte[] guid=new byte[16];
        for (int i=0; i<16; i++) //TODO3: can optimize
            guid[i]=buf[i];

        //Dispatch based on opcode.
        switch (func) {
            //TODO: all the length checks should be encapsulated in the various
            //constructors; Message shouldn't know anything about the various
            //messages except for their function codes.  I've started this
            //refactoring with PushRequest and PingReply.
            case F_PING:
				if (length>0) //Big ping
                    return new PingRequest(guid,ttl,hops,payload);
                return new PingRequest(guid,ttl,hops);

            case F_PING_REPLY:
                return PingReply.createFromNetwork(guid, ttl, hops, payload);
            case F_QUERY:
                if (length<3) break;
				return QueryRequest.createNetworkQuery(
				    guid, ttl, hops, payload, network);
            case F_QUERY_REPLY:
                if (length<26) break;
                return new QueryReply(guid,ttl,hops,payload,network);
            case F_PUSH:
                return new PushRequest(guid,ttl,hops,payload, network);
            case F_ROUTE_TABLE_UPDATE:
                //The exact subclass of RouteTableMessage returned depends on
                //the variant stored within the payload.  So leave it to the
                //static read(..) method of RouteTableMessage to actually call
                //the right constructor.
                return RouteTableMessage.read(guid, ttl, hops, payload);
            case F_VENDOR_MESSAGE:
                if ((ttl != 1) || (hops != 0))
                    throw new BadPacketException("VM with bad ttl/hops: " +
                                                 ttl + "/" + hops);
                return VendorMessage.deriveVendorMessage(guid, ttl, hops, 
                                                         payload, network);
            case F_VENDOR_MESSAGE_STABLE:
                if ((ttl != 1) || (hops != 0))
                    throw new BadPacketException("VM with bad ttl/hops: " +
                                                 ttl + "/" + hops);
                return VendorMessage.deriveVendorMessage(guid, ttl, hops, 
                                                         payload, network);
            case F_UDP_CONNECTION:
                return UDPConnectionMessage.createMessage(
				  guid, ttl, hops, payload);
        }
        
        ReceivedErrorStat.INVALID_CODE.incrementStat();
        throw new BadPacketException("Unrecognized function code: "+func);
    }
    
    /**
     * Writes a message out, using the buffer as the temporary header.
     */
    public void write(OutputStream out, byte[] buf) throws IOException {
        for (int i=0; i<16; i++) //TODO3: can optimize
            buf[i]=guid[i];
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
             +", hops="+hops
             +", priority="+getPriority()+"}";
    }

	/**
	 * Records the dropping of this message in statistics.
	 */
	public abstract void recordDrop();
}
