package com.limegroup.gnutella;

import com.sun.java.util.collections.*;
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
    /** Functional IDs defined by Gnutella protocol. */
    public static final byte F_PING=(byte)0x0;
    public static final byte F_PING_REPLY=(byte)0x1;
    public static final byte F_PUSH=(byte)0x40;
    public static final byte F_QUERY=(byte)0x80;
    public static final byte F_QUERY_REPLY=(byte)0x81;
    public static final byte F_ROUTE_TABLE_UPDATE=(byte)0x30;

    /** Constants for use by MessageReader. */
    public static final int HEADER_SIZE=23;
    public static final int GUID_SIZE=16;
    public static final int OPCODE_OFFSET=16;
    public static final int TTL_OFFSET=17;
    public static final int HOPS_OFFSET=18;
    public static final int LENGTH_OFFSET=19;

    /** Should extended pings be read as GroupPingRequest's or PingRequest's?
     *  False on client, true on server. */
    public final static boolean PARSE_GROUP_PINGS = false;

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
										String ext) throws IOException {
        if (ext == null || (ext.length()==0)) {
            return addPrefixDelimiter;
        }
        if(addPrefixDelimiter) {
            os.write(0x1c);
        }
        os.write(ext.getBytes());
        return true; // any subsequent extensions should have delimiter 
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
     * @effects utility function to read null-terminated string from stream
     */
    protected String readNullTerminatedString(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i;
        while ((is.available()>0)&&(i=is.read())!=0) {
            baos.write(i);
        }
        return new String(baos.toByteArray());
    }

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
    
    /**
     * Sets the guid for this message. Is needed, when we want to cache 
     * query replies or other messages, and change the GUID as per the 
     * request
     * @param guid The guid to be set
     */
    protected void setGUID(GUID guid) {
        this.guid = guid.bytes();
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

    /** Unit test. */
    /*
    public static void main(String args[]) {
        //Note: some of Message's code is covered by subclass tests, e.g.,
        //PushRequestTest.
    
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
