package com.limegroup.gnutella;

import java.io.*;
import com.limegroup.gnutella.messages.*;
import com.sun.java.util.collections.*;

/**
 * A ping reply message, aka, "pong".  This implementation provides a way
 * to "mark" pongs as being from supernodes.
 */
public class PingReply extends Message implements Serializable {
    private static final int STANDARD_PAYLOAD_SIZE=14;
    
    /** All the data.  We extract the port, ip address, number of files,
     *  and number of kilobytes lazily. */
    private byte[] payload;
    /** The IP string as extracted from payload[2..5].  Cached to avoid
     *  allocations.  LOCKING: obtain this' monitor. */
    private volatile String ip;
    /** The GGEP block from payload[14...].  Cached to avoid allocations.  Null
     *  if the GGEP data has not been parsed, this has no GGEP data, or the GGEP
     *  data is corrupt.  LOCKING: obtain this' monitor. */
    private volatile GGEP ggep;

    /**
     * Creates a new ping from scratch.
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     */
    public PingReply(byte[] guid, byte ttl,
             int port, byte[] ip, long files, long kbytes) {
        this(guid, ttl, port, ip, files, kbytes, false);
    }

    /**
     * Creates a new ping from scratch with ultrapeer extension data.
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     * @param isUltrapeer true if this should be a marked ultrapeer pong,
     *  which sets kbytes to the nearest power of 2 not less than 8.
     */
    public PingReply(byte[] guid, byte ttl,
             int port, byte[] ip, long files, long kbytes, 
             boolean isUltrapeer) { 
        this(guid, ttl, port, ip, files, kbytes, isUltrapeer, -1);
    }

    /**
     * Creates a new ping from scratch with ultrapeer and daily uptime extension
     * data.
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     * @param isUltrapeer true if this should be a marked ultrapeer pong,
     *  which sets kbytes to the nearest power of 2 not less than 8.
     * @param dailyUptime my average daily uptime, in seconds, e.g., 
     *  3600 for one hour per day.  Negative values mean "don't know".
     *  GGEP extension blocks are allocated if dailyUptime is non-negative.  
     */
    public PingReply(byte[] guid, byte ttl,
             int port, byte[] ip, long files, long kbytes,
             boolean isUltrapeer, int dailyUptime) {
        this(guid, ttl, port, ip, files, kbytes, isUltrapeer,
             dailyUptime>=0 ? newGGEP(dailyUptime, true) : null);
    }

    /**
     * Wrap a PingReply around stuff snatched from the network.
     * <p>
     * Initially this method required that payload.lenghth == 14. But now we
     * support for big pings and pongs. 
     *
     * @exception BadPacketException payload is too small
     */
    public PingReply(byte[] guid, byte ttl, byte hops,
             byte[] payload) throws BadPacketException {
        super(guid, Message.F_PING_REPLY, ttl, hops, payload.length);
        if (payload.length<STANDARD_PAYLOAD_SIZE)
            throw new BadPacketException();
        this.payload=payload;
    }
     
    /** Internal constructor used to bind the encoded GGEP payload, avoiding the
     *  need to construct it more than once.  
     *  @param extension the encoded GGEP payload, or null if none */
    private PingReply(byte[] guid, byte ttl, 
             int port, byte[] ip, long files, long kbytes,
             boolean isUltrapeer, byte[] extensions) {    
        super(guid, Message.F_PING_REPLY, ttl, (byte)0, 
            STANDARD_PAYLOAD_SIZE + (extensions==null ? 0 : extensions.length));
        this.payload=new byte[getLength()];
        //It's ok if casting port, files, or kbytes turns negative.
        ByteOrder.short2leb((short)port, payload, 0);
        //Payload stores IP in BIG-ENDIAN
        payload[2]=ip[0];
        payload[3]=ip[1];
        payload[4]=ip[2];
        payload[5]=ip[3];
        ByteOrder.int2leb((int)files, payload, 6);
        ByteOrder.int2leb((int) (isUltrapeer ? mark(kbytes) : kbytes), 
                          payload, 
                          10);
        
        //Encode GGEP block if included.
        if (extensions!=null) {
            System.arraycopy(extensions, 0, 
                             payload, STANDARD_PAYLOAD_SIZE, 
                             extensions.length);
        }            
    }

    /** Returns the GGEP payload bytes to encode the given uptime */
    private static byte[] newGGEP(int dailyUptime, boolean udpSupported) {
        try {
            GGEP ggep=new GGEP(true);
            ggep.put(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME, dailyUptime);
            if (udpSupported)
                ggep.put(GGEP.GGEP_HEADER_UNICAST_SUPPORT);
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            ggep.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            //See above.
            Assert.that(false, "Couldn't encode uptime or udp");
            return null;
        }
    }

    protected void writePayload(OutputStream out) throws IOException {
        out.write(payload);
    }

    public String toString() {
        return "PingReply("+getIP()+":"+getPort()
            +", "+super.toString()+")";
    }

    public int getPort() {
        return  ByteOrder.ubytes2int(ByteOrder.leb2short(payload,0));
    }

    /**
     * Returns the ip field in standard dotted decimal format, e.g.,
     * "127.0.0.1".  The most significant byte is written first.
     */
    public synchronized String getIP() {        
        //Despite some incorrect early documentation, payload really is
        //big-endian.
        if (ip==null)
            ip=ip2string(payload, 2);
        return ip;
    }

     /**
     * Returns the ip address bytes (MSB first)
     */
    public byte[] getIPBytes() {
        byte[] ip=new byte[4];
        ip[0]=payload[2];
        ip[1]=payload[3];
        ip[2]=payload[4];
        ip[3]=payload[5];
        
        return ip;
    }
    
    public long getFiles() {
        return ByteOrder.ubytes2long(ByteOrder.leb2int(payload,6));
    }

    public long getKbytes() {
        return ByteOrder.ubytes2long(ByteOrder.leb2int(payload,10));
    }

    /** Returns the average daily uptime in seconds from the GGEP payload.
     *  @exception BadPacketException if the uptime is not known or corrupt. */
    public synchronized int getDailyUptime() throws BadPacketException {
        parseGGEP();
        if (ggep==null)
            throw new BadPacketException("Missing GGEP block");
        try {
            return ggep.getInt(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME);
        } catch (BadGGEPPropertyException e) {
            throw new BadPacketException("Couldn't find uptime extension.");
        }
    }


    /** Returns the average daily uptime in seconds from the GGEP payload.
     *  @exception BadPacketException if the uptime is not known or corrupt. */
    public synchronized boolean supportsUnicast() throws BadPacketException {
        parseGGEP();
        if (ggep==null)
            throw new BadPacketException("Missing GGEP block");
        return ggep.hasKey(GGEP.GGEP_HEADER_UNICAST_SUPPORT);
    }

    public synchronized boolean hasGGEPExtension() {
        parseGGEP();
        return ggep!=null;
    }
    
    /** Ensure GGEP data parsed...if possible. */
    private synchronized void parseGGEP() {
        //Return if we've already parsed the data or this is a plain pong
        //without space for GGEP.  If this has bad GGEP data, multiple calls to
        //parseGGEP will result in multiple parse attempts.  While this is
        //inefficient, it is sufficiently rare to not justify a parsedGGEP
        //variable.
        if (ggep!=null || getLength()<=STANDARD_PAYLOAD_SIZE)
            return;
    
        try {
            ggep=new GGEP(payload, STANDARD_PAYLOAD_SIZE, null);
        } catch (BadGGEPBlockException e) { }
    }

    public Message stripExtendedPayload() {
        //TODO: if this is too slow, we can alias parts of this, as as the
        //payload.  In fact we could even return a subclass of PingReply that
        //simply delegates to this.
        byte[] newPayload=new byte[STANDARD_PAYLOAD_SIZE];
        System.arraycopy(payload, 0,
                         newPayload, 0,
                         STANDARD_PAYLOAD_SIZE);
        try {
            return new PingReply(this.getGUID(), this.getTTL(), this.getHops(),
                                 newPayload);
        } catch (BadPacketException e) {
            Assert.that(false, "Couldn't strip payload! "+e);
            return null;
        }
    }


    ////////////////////////// Pong Marking //////////////////////////

    /** Returns true if this message is "marked", i.e., likely from a
     *  supernode. */
    public boolean isMarked() {
        //Returns true if kb is a power of two greater than or equal to eight.
        long kb=getKbytes();
        if (kb<8)
            return false;
        return isPowerOf2(ByteOrder.long2int(kb));
    }

    public static boolean isPowerOf2(int x) {  //package access for testability
        if (x<=0)
            return false;
        else
            return (x&(x - 1)) == 0;
    }

    /** Marks the given kbytes field */
    private static long mark(long kbytes) {
        int x=ByteOrder.long2int(kbytes);
        //Returns the power of two nearest to x.  TODO3: faster algorithms are
        //possible.  At the least, you can do binary search.  I imagine some bit
        //operations can be done as well.  This brute-force approach was
        //generated with the help of the the following Python program:
        //
        //  for i in xrange(0, 32):
        //      low=1<<i
        //      high=1<<(i+1)
        //      split=(low+high)/2
        //      print "else if (x<%d)" % split
        //      print "    return %d; //1<<%d" % (low, i)        
        if (x<12)
            return 8; //1<<3
        else if (x<24)
            return 16; //1<<4
        else if (x<48)
            return 32; //1<<5
        else if (x<96)
            return 64; //1<<6
        else if (x<192)
            return 128; //1<<7
        else if (x<384)
            return 256; //1<<8
        else if (x<768)
            return 512; //1<<9
        else if (x<1536)
            return 1024; //1<<10
        else if (x<3072)
            return 2048; //1<<11
        else if (x<6144)
            return 4096; //1<<12
        else if (x<12288)
            return 8192; //1<<13
        else if (x<24576)
            return 16384; //1<<14
        else if (x<49152)
            return 32768; //1<<15
        else if (x<98304)
            return 65536; //1<<16
        else if (x<196608)
            return 131072; //1<<17
        else if (x<393216)
            return 262144; //1<<18
        else if (x<786432)
            return 524288; //1<<19
        else if (x<1572864)
            return 1048576; //1<<20
        else if (x<3145728)
            return 2097152; //1<<21
        else if (x<6291456)
            return 4194304; //1<<22
        else if (x<12582912)
            return 8388608; //1<<23
        else if (x<25165824)
            return 16777216; //1<<24
        else if (x<50331648)
            return 33554432; //1<<25
        else if (x<100663296)
            return 67108864; //1<<26
        else if (x<201326592)
            return 134217728; //1<<27
        else if (x<402653184)
            return 268435456; //1<<28
        else if (x<805306368)
            return 536870912; //1<<29
        else 
            return 1073741824; //1<<30
    }

    //Unit test: tests/com/limegroup/gnutella/messages/PingReplyTest
}
