package com.limegroup.gnutella;

import com.sun.java.util.collections.Random;
import java.io.*;
import com.limegroup.gnutella.routing.RouteTableMessage;

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
    //Functional IDs defined by Gnutella protocol.
    public static final byte F_PING=(byte)0x0;
    public static final byte F_PING_REPLY=(byte)0x1;
    public static final byte F_PUSH=(byte)0x40;
    public static final byte F_QUERY=(byte)0x80;
    public static final byte F_QUERY_REPLY=(byte)0x81;
    public static final byte F_ROUTE_TABLE_UPDATE=(byte)0x30;

    /** Same as GUID.makeGUID.  This exists for backwards compatibility. */
    static byte[] makeGuid() {
        return GUID.makeGuid();
    }

    ////////////////////////// Instance Data //////////////////////

    private byte[] guid;
    private byte func;

    /* We do not support TTLs > 2^7, nor do we support packets
     * of length > 2^31 */
    private byte ttl;
    private byte hops;
    private int length;

    /** Priority for flow-control.  Lower numbers mean higher priority.NOT
     *  written to network. */
    private int priority=0;
    /** Time this was created.  Not written to network. */
    private long creationTime=System.currentTimeMillis();

    /** Rep. invariant */
    protected void repOk() {
        Assert.that(guid.length==16);
        Assert.that(func==F_PING || func==F_PING_REPLY
                    || func==F_PUSH
                    || func==F_QUERY || func==F_QUERY_REPLY,
                    "Bad function code");
        //if (func==F_PING) Assert.that(length==0, "Bad ping length: "+length);
        if (func==F_PING_REPLY) Assert.that(length==14, "Bad pong length: "+length);
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
        this(makeGuid(), func, ttl, (byte)0, length);
    }

    /**
     * Same as above, but caller specifies TTL and number of hops.
     * This is used when reading packets off network.
     */
    protected Message(byte[] guid, byte func, byte ttl,
              byte hops, int length) {
        this.guid=guid; this.func=func; this.ttl=ttl;
        this.hops=hops; this.length=length;
        //repOk();
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
    public static Message read(InputStream in)
            throws BadPacketException, IOException {
        return Message.read(in, new byte[23]);
    }

    /**
     * @requires buf.length==23
     * @effects exactly like Message.read(in), but buf is used as scratch for
     *  reading the header.  This is an optimization that lets you avoid
     *  repeatedly allocating 23-byte arrays.  buf may be used when this returns,
     *  but the contents are not guaranteed to contain any useful data.  
     */
    static Message read(InputStream in, byte[] buf)
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
            if (got==-1) throw new IOException("Connection closed.");
            i+=got;
        }

        //2. Unpack.
        byte[] guid=new byte[16];
        for (int i=0; i<16; i++) //TODO3: can optimize
            guid[i]=buf[i];
        byte func=buf[16];
        byte ttl=buf[17];
        byte hops=buf[18];
        int length=ByteOrder.leb2int(buf,19);
        //2.5 If the length is hopelessly off (this includes lengths >
        //    than 2^31 bytes, throw an irrecoverable exception to
        //    cause this connection to be closed.
        if (length<0 || length>SettingsManager.instance().getMaxLength())
            throw new IOException("Unreasonable message length: "+length);

        //3. Read rest of payload.  This must be done even for bad
        //   packets, so we can resume reading packets.
        byte[] payload=null;
        if (length!=0) {
            payload=new byte[length];
            for (int i=0; i<length; ) {
            int got=in.read(payload, i, length-i);
            if (got==-1) throw new IOException("Connection closed.");
            i+=got;
            }
        }

        //4. Check values.   These are based on the recommendations from the
        //   GnutellaDev page.  This also catches those TTLs and hops whose
        //   high bit is set to 0.
        byte softMax=SettingsManager.instance().getSoftMaxTTL();
        byte hardMax=SettingsManager.instance().getMaxTTL();
        if (hops<0)
            throw new BadPacketException("Negative (or very large) hops");
        else if (ttl<0)
            throw new BadPacketException("Negative (or very large) TTL");
        else if (hops>softMax)
            throw new BadPacketException("Hops already exceeds soft maximum");
        else if (ttl+hops > hardMax)
            throw new BadPacketException("TTL+hops exceeds hard max; probably spam");
        else if (ttl+hops > softMax) {
            ttl=(byte)(softMax - hops);  //overzealous client;
                                         //readjust accordingly
            Assert.that(ttl>=0);     //should hold since hops<=softMax ==>
                                     //new ttl>=0
        }

        //Dispatch based on opcode.
        switch (func) {
            case F_PING:
                if (length>=15) {
				    // Build a GroupPingRequest
                    return new GroupPingRequest(guid,ttl,hops,payload);
				}
				else if (length>0) break;
                return new PingRequest(guid,ttl,hops);

            case F_PING_REPLY:
                if (length!=14) break;
                return new PingReply(guid,ttl,hops,payload);
            case F_QUERY:
                if (length<3) break;
                return new QueryRequest(guid,ttl,hops,payload);
            case F_QUERY_REPLY:
                if (length<26) break;
                return new QueryReply(guid,ttl,hops,payload);
            case F_PUSH:
                if (length!=26) break;
                return new PushRequest(guid,ttl,hops,payload);
            case F_ROUTE_TABLE_UPDATE:
                //The exact subclass of RouteTableMessage returned depends on
                //the variant stored within the payload.  So leave it to the
                //static read(..) method of RouteTableMessage to actually call
                //the right constructor.
                return RouteTableMessage.read(guid, ttl, hops, payload);            
        }
        throw new BadPacketException("Unrecognized function code: "+func);
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

    ////////////////////////////////////////////////////////////////////

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
            throw new IllegalArgumentException();
        this.ttl = ttl;
    }

    public byte getHops() {
        return hops;
    }

    /** Returns the length of this' payload, in bytes. */
    public int getLength() {
        return length;
    }

    /** Returns the total length of this, in bytes */
    public int getTotalLength() {
        //Header is 23 bytes.
        return 23+length;
    }

    /** Returns the ip (given in BIG-endian) format as standard
     *  dotted-decimal, e.g., 192.168.0.1<p> */
     static final String ip2string(byte[] ip) {
         return ip2string(ip, 0);
     }
         
    /** Returns the ip (given in BIG-endian) format of
     *  buf[offset]...buf[offset+3] as standard dotted-decimal, e.g.,
     *  192.168.0.1<p> */
    static final String ip2string(byte[] buf, int offset) {
        StringBuffer sbuf=new StringBuffer(16);   //xxx.xxx.xxx.xxx => 15 chars
        sbuf.append(ByteOrder.ubyte2int(buf[offset]));
        sbuf.append('.');
        sbuf.append(ByteOrder.ubyte2int(buf[offset+1]));
        sbuf.append('.');
        sbuf.append(ByteOrder.ubyte2int(buf[offset+2]));
        sbuf.append('.');
        sbuf.append(ByteOrder.ubyte2int(buf[offset+3]));
        return sbuf.toString();
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

    /** Unit test. */
    /*
    public static void main(String args[]) {
        byte[] buf=new byte[10];
        buf[3]=(byte)192;
        buf[4]=(byte)168;
        buf[5]=(byte)0;
        buf[6]=(byte)1;       
        Assert.that(ip2string(buf, 3).equals("192.168.0.1"));
        
        buf=new byte[4];
        buf[0]=(byte)0;
        buf[1]=(byte)1;
        buf[2]=(byte)2;
        buf[3]=(byte)3;
        Assert.that(ip2string(buf).equals("0.1.2.3"));

        buf=new byte[4];
        buf[0]=(byte)252;
        buf[1]=(byte)253;
        buf[2]=(byte)254;
        buf[3]=(byte)255;
        Assert.that(ip2string(buf).equals("252.253.254.255"));

        Message m1=new PingRequest((byte)3);
        Message m2=new PingRequest((byte)3);
        m2.setPriority(5);
        Assert.that(m1.compareTo(m2)>0);
        Assert.that(m2.compareTo(m1)<0);
        Assert.that(m2.compareTo(m2)==0);
    }
    */
}
