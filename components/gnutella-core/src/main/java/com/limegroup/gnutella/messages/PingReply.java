package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.guess.*;
import com.limegroup.gnutella.statistics.*;
import java.io.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;

/**
 * A ping reply message, aka, "pong".  This implementation provides a way
 * to "mark" pongs as being from supernodes.
 */
public class PingReply extends Message implements Serializable {
    private static final int STANDARD_PAYLOAD_SIZE=14;
    
    /** All the data.  We extract the port, ip address, number of files,
     *  and number of kilobytes lazily. */
    private final byte[] PAYLOAD;

    /** The IP string as extracted from payload[2..5].  Cached to avoid
     *  allocations.  LOCKING: obtain this' monitor. */
    private final String IP;

    /**
     * Constant for the port number of this pong.
     */
    private final int PORT;

    /**
     * Constant for the number of shared files reported in the pong.
     */
    private final long FILES;

    /**
     * Constant for the number of shared kilobytes reported in the pong.
     */
    private final long KILOBYTES;

    /**
     * Constant int for the daily average uptime.
     */
    private final int DAILY_UPTIME;

    /**
     * Constant for whether or not the remote node supports unicast.
     */
    private final boolean SUPPORTS_UNICAST;

    /**
     * Constant for the vendor of the remote host.
     */
    private final String VENDOR;

    /**
     * Constant for the major version number reported in the vendor block.
     */
    private final int VENDOR_MAJOR_VERSION;

    /**
     * Constant for the minor version number reported in the vendor block.
     */
    private final int VENDOR_MINOR_VERSION;

    /**
     * Constant for the query key reported for the pong.
     */
    private final QueryKey QUERY_KEY;

    /**
     * Constant boolean for whether or not this pong contains any GGEP
     * extensions.
     */
    private final boolean HAS_GGEP_EXTENSION;

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
    public PingReply(byte[] guid, byte ttl, int port, byte[] ip, long files, 
                     long kbytes) {
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
        this(guid, ttl, port, ip, files, kbytes, isUltrapeer, -1, false);
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
             boolean isUltrapeer, int dailyUptime, boolean isGUESSCapable) {
        this(guid, ttl, port, ip, files, kbytes, isUltrapeer,
             newGGEP(dailyUptime, isUltrapeer, isGUESSCapable));
    }

    /**
     * Use this constructor to make a PingReply with a QueryKey.  
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
     * @param queryKey the QueryKey you want the receiving host to use for UDP
     *  query credentials.
     */
    public PingReply(byte[] guid, byte ttl, int port, byte[] ip, 
                     long files, long kbytes, boolean isUltrapeer,
                     QueryKey queryKey) {
        this(guid, ttl, port, ip, files, kbytes, isUltrapeer,
             qkGGEP(queryKey));
    }

    /** Internal constructor used to bind the encoded GGEP payload, avoiding the
     *  need to construct it more than once.  
     *  @param extension the encoded GGEP payload, or null if none */
    private PingReply(byte[] guid, byte ttl, int port, byte[] ip, long files, 
                      long kbytes, boolean isUltrapeer, byte[] extensions) {
        super(guid, Message.F_PING_REPLY, ttl, (byte)0, 
            STANDARD_PAYLOAD_SIZE + (extensions==null ? 0 : extensions.length));
 		if(!CommonUtils.isValidPort(port))
			throw new IllegalArgumentException("invalid port: "+port);
        
        PAYLOAD = new byte[getLength()];
        //It's ok if casting port, files, or kbytes turns negative.
        ByteOrder.short2leb((short)port, PAYLOAD, 0);
        //Payload stores IP in BIG-ENDIAN
        PAYLOAD[2]=ip[0];
        PAYLOAD[3]=ip[1];
        PAYLOAD[4]=ip[2];
        PAYLOAD[5]=ip[3];
        ByteOrder.int2leb((int)files, PAYLOAD, 6);
        ByteOrder.int2leb((int) (isUltrapeer ? mark(kbytes) : kbytes), 
                          PAYLOAD, 
                          10);
        
        //Encode GGEP block if included.
        if (extensions!=null) {
            System.arraycopy(extensions, 0, 
                             PAYLOAD, STANDARD_PAYLOAD_SIZE, 
                             extensions.length);
        }            

        PORT = port;//ByteOrder.ubytes2int(ByteOrder.leb2short(payload,0));
        FILES = ByteOrder.ubytes2long(ByteOrder.leb2int(PAYLOAD,6));
        KILOBYTES = ByteOrder.ubytes2long(ByteOrder.leb2int(PAYLOAD,10));

        // IP is big-endian
        IP = ip2string(PAYLOAD, 2);

        // GGEP parsing
        GGEP ggep = parseGGEP();
        int dailyUptime = -1;
        boolean supportsUnicast = false;
        String vendor = "";
        int vendorMajor = -1;
        int vendorMinor = -1;
        QueryKey key = null;
        if(ggep != null) {
            if(ggep.hasKey(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME)) {
                try {
                    dailyUptime = 
                        ggep.getInt(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME); 
                } catch(BadGGEPPropertyException e) {
                    // simply don't assign it
                }
            }

            supportsUnicast = 
                ggep.hasKey(GGEP.GGEP_HEADER_UNICAST_SUPPORT); 

            if(ggep.hasKey(GGEP.GGEP_HEADER_VENDOR_INFO)) {
                try {
                    vendor = 
                        new String(ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO),
                                   0, 4);   
                } catch(BadGGEPPropertyException e) {
                    // simply don't assign it
                }

                try {
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);
                    vendorMajor = (bytes[4] >> 4);
                } catch (BadGGEPPropertyException e) {
                    // simply don't assign it
                }
                try {
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);
                    vendorMinor = (bytes[4] & 15);
                }
                catch (BadGGEPPropertyException e) {
                    // simply don't assign it
                }
             }

            if (ggep.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                try {
                    byte[] bytes = 
                        ggep.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
                    key = QueryKey.getQueryKey(bytes, false);
                }
                catch (IllegalArgumentException malformedQueryKey) { 
                    // simply don't assign it
                }
                catch (BadGGEPPropertyException corrupt) { 
                    // simply don't assign it
                }
            }
        }

        HAS_GGEP_EXTENSION = ggep != null;
        DAILY_UPTIME = dailyUptime;
        SUPPORTS_UNICAST = supportsUnicast;
        VENDOR = vendor;
        VENDOR_MAJOR_VERSION = vendorMajor;
        VENDOR_MINOR_VERSION = vendorMinor;
        QUERY_KEY = key;

    }

    /**
     * Wrap a PingReply around stuff snatched from the network.
     * <p>
     * Initially this method required that payload.lenghth == 14. But now we
     * support for big pings and pongs. 
     *
     * @exception BadPacketException payload is too small
     */
    public PingReply(byte[] guid, byte ttl, byte hops, byte[] payload) 
        throws BadPacketException {
        super(guid, Message.F_PING_REPLY, ttl, hops, payload.length);
        if (payload.length<STANDARD_PAYLOAD_SIZE)
            throw new BadPacketException("invalid payload length");
        this.PAYLOAD=payload;
        PORT = ByteOrder.ubytes2int(ByteOrder.leb2short(PAYLOAD,0));
        FILES = ByteOrder.ubytes2long(ByteOrder.leb2int(PAYLOAD,6));
        KILOBYTES = ByteOrder.ubytes2long(ByteOrder.leb2int(PAYLOAD,10));
 		if(!CommonUtils.isValidPort(getPort()))
			throw new BadPacketException("invalid port: "+PORT);

        // IP is big-endian
        IP = ip2string(PAYLOAD, 2);

        // GGEP parsing
        GGEP ggep = parseGGEP();
        int dailyUptime = -1;
        boolean supportsUnicast = false;
        String vendor = "";
        int vendorMajor = -1;
        int vendorMinor = -1;
        QueryKey key = null;
        if(ggep != null) {
            if(ggep.hasKey(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME)) {
                try {
                    dailyUptime = 
                        ggep.getInt(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME); 
                } catch(BadGGEPPropertyException e) {
                    // simply don't assign it
                }
            }

            supportsUnicast = 
                ggep.hasKey(GGEP.GGEP_HEADER_UNICAST_SUPPORT); 

            if(ggep.hasKey(GGEP.GGEP_HEADER_VENDOR_INFO)) {
                try {
                    vendor = 
                        new String(ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO),
                                   0, 4);   
                } catch(BadGGEPPropertyException e) {
                    // simply don't assign it
                }

                try {
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);
                    vendorMajor = (bytes[4] >> 4);
                } catch (BadGGEPPropertyException e) {
                    // simply don't assign it
                }
                try {
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);
                    vendorMinor = (bytes[4] & 15);
                }
                catch (BadGGEPPropertyException e) {
                    // simply don't assign it
                }
             }

            if (ggep.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                try {
                    byte[] bytes = 
                        ggep.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
                    key = QueryKey.getQueryKey(bytes, false);
                }
                catch (IllegalArgumentException malformedQueryKey) { 
                    // simply don't assign it
                }
                catch (BadGGEPPropertyException corrupt) { 
                    // simply don't assign it
                }
            }
        }

        HAS_GGEP_EXTENSION = ggep != null;
        DAILY_UPTIME = dailyUptime;
        SUPPORTS_UNICAST = supportsUnicast;
        VENDOR = vendor;
        VENDOR_MAJOR_VERSION = vendorMajor;
        VENDOR_MINOR_VERSION = vendorMinor;
        QUERY_KEY = key;
    }
     

    /** Returns the GGEP payload bytes to encode the given uptime */
    private static byte[] newGGEP(int dailyUptime, boolean isUltrapeer,
                                  boolean isGUESSCapable) {
        try {
            GGEP ggep=new GGEP(true);

            if (dailyUptime >= 0)
                ggep.put(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME, dailyUptime);

            if (isGUESSCapable && isUltrapeer) {
                // indicate guess support
                byte[] vNum = {
                convertToGUESSFormat(CommonUtils.getGUESSMajorVersionNumber(),
                                     CommonUtils.getGUESSMinorVersionNumber())};
                ggep.put(GGEP.GGEP_HEADER_UNICAST_SUPPORT, vNum);
            }

            if (isUltrapeer) { 
                // indicate UP support
                addUltrapeerExtension(ggep);
            }

            // all pongs should have vendor info
            addVendorExtension(ggep);

            // actually write the badboy
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            ggep.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            //See above.
            Assert.that(false, "Couldn't encode uptime or udp");
            return null;
        }
    }


    /** Returns the GGEP payload bytes to encode the given QueryKey */
    private static byte[] qkGGEP(QueryKey queryKey) {
        try {
            GGEP ggep=new GGEP(true);

            // get qk bytes....
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            queryKey.write(baos);
            // populate GGEP....
            ggep.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT, baos.toByteArray());

            // actually write the badboy
            baos=new ByteArrayOutputStream();
            ggep.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            //See above.
            Assert.that(false, "Couldn't encode QueryKey" + queryKey);
            return null;
        }
    }


    private static void addUltrapeerExtension(GGEP ggep) {
        byte[] payload = new byte[3];
        // put version
        payload[0] = convertToGUESSFormat(CommonUtils.getUPMajorVersionNumber(),
                                          CommonUtils.getUPMinorVersionNumber()
                                          );
        payload[1] = (byte) RouterService.getNumFreeLeafSlots();
        payload[2] = (byte) RouterService.getNumFreeNonLeafSlots();

        // add it
        ggep.put(GGEP.GGEP_HEADER_UP_SUPPORT, payload);
    }


    private static void addVendorExtension(GGEP ggep) {
        byte[] payload = new byte[5];
        // set 'LIME'
        System.arraycopy(CommonUtils.QHD_VENDOR_NAME.getBytes(),
                         0, payload, 0,
                         CommonUtils.QHD_VENDOR_NAME.getBytes().length);
        payload[4] = convertToGUESSFormat(CommonUtils.getMajorVersionNumber(),
                                          CommonUtils.getMinorVersionNumber());
         // add it
        ggep.put(GGEP.GGEP_HEADER_VENDOR_INFO, payload);
    }

    /** puts major as the high order bits, minor as the low order bits.
     *  @exception IllegalArgumentException thrown if major/minor is greater than
     *  15 or less than 0.
     */
    private static byte convertToGUESSFormat(int major, int minor) 
        throws IllegalArgumentException {
        if ((major < 0) || (minor < 0) || (major > 15) || (minor > 15))
            throw new IllegalArgumentException();
        // set major
        int retInt = major;
        retInt = retInt << 4;
        // set minor
        retInt |= minor;

        return (byte) retInt;
    }


    protected void writePayload(OutputStream out) throws IOException {
        out.write(PAYLOAD);
		if(RECORD_STATS) {
			SentMessageStatHandler.TCP_PING_REPLIES.addMessage(this);
		}
    }

    /**
     * Accessor for the port reported in this pong.
     *
     * @return the port number reported in the pong
     */
    public int getPort() {
        return PORT;
    }

    /**
     * Returns the ip field in standard dotted decimal format, e.g.,
     * "127.0.0.1".  The most significant byte is written first.
     */
    public String getIP() { 
        return IP;
    }

    /**
     * Returns the ip address bytes (MSB first)
     */
    public byte[] getIPBytes() {
        byte[] ip=new byte[4];
        ip[0]=PAYLOAD[2];
        ip[1]=PAYLOAD[3];
        ip[2]=PAYLOAD[4];
        ip[3]=PAYLOAD[5];
        
        return ip;
    }
    
    /**
     * Accessor for the number of files shared, as reported in the
     * pong.
     *
     * @return the number of files reported shared
     */
    public long getFiles() {
        return FILES;
    }

    /**
     * Accessor for the number of kilobytes shared, as reported in the
     * pong.
     *
     * @return the number of kilobytes reported shared
     */
    public long getKbytes() {
        return KILOBYTES;
    }

    /** Returns the average daily uptime in seconds from the GGEP payload.
     *  If the pong did not report a daily uptime, returns -1.
     *
     * @return the daily uptime reported in the pong, or -1 if the uptime
     *  was not present or could not be read
     */
    public int getDailyUptime() {
        return DAILY_UPTIME;
    }


    /** Returns whether or not this host support unicast, GUESS-style
     *  queries.
     *
     * @return <tt>true</tt> if this host does support GUESS-style queries,
     *  otherwise <tt>false</tt>
     */
    public boolean supportsUnicast() {
        return SUPPORTS_UNICAST;
    }


    /** Returns the 4-character vendor string associated with this Pong.
     *
     * @return the 4-character vendor code reported in the pong, or the
     *  empty string if no vendor code was successfully read
     */
    public String getVendor() {
        return VENDOR;
    }


    /** Returns the major version number of the vendor returning this pong.
     * 
     * @return the major version number of the vendor returning this pong,
     *  or -1 if the version could not be read
     */
    public int getVendorMajorVersion() {
        return VENDOR_MAJOR_VERSION;
    }

    /** Returns the minor version number of the vendor returning this pong.
     * 
     * @return the minor version number of the vendor returning this pong,
     *  or -1 if the version could not be read
     */
    public int getVendorMinorVersion() {
        return VENDOR_MINOR_VERSION;
    }


    /** Returns the QueryKey (if any) associated with this pong.  May be null!
     *
     * @return the <tt>QueryKey</tt> for this pong, or <tt>null</tt> if no
     *  key was specified
     */
    public QueryKey getQueryKey() {
        return QUERY_KEY;
    }


    /**
     * Returns whether or not this pong has a GGEP extension.
     *
     * @return <tt>true</tt> if the pong has a GGEP extension, otherwise
     *  <tt>false</tt>
     */
    public boolean hasGGEPExtension() {
        return HAS_GGEP_EXTENSION;
    }
    
    // TODO : change this to look for multiple GGEP block in the payload....
    /** Ensure GGEP data parsed...if possible. */
    private GGEP parseGGEP() {
        //Return if this is a plain pong without space for GGEP.  If 
        //this has bad GGEP data, multiple calls to
        //parseGGEP will result in multiple parse attempts.  While this is
        //inefficient, it is sufficiently rare to not justify a parsedGGEP
        //variable.
        if (getLength()<=STANDARD_PAYLOAD_SIZE)
            return null;
    
        try {
            return new GGEP(PAYLOAD, STANDARD_PAYLOAD_SIZE, null);
        } catch (BadGGEPBlockException e) { 
            return null;
        }
    }

    // inherit doc comment from message superclass
    public Message stripExtendedPayload() {
        //TODO: if this is too slow, we can alias parts of this, as as the
        //payload.  In fact we could even return a subclass of PingReply that
        //simply delegates to this.
        byte[] newPayload=new byte[STANDARD_PAYLOAD_SIZE];
        System.arraycopy(PAYLOAD, 0,
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
        long kb = getKbytes();
        if (kb < 8)
            return false;
        return isPowerOf2(ByteOrder.long2int(kb));
    }

    public static boolean isPowerOf2(int x) {  //package access for testability
        if (x<=0)
            return false;
        else
            return (x&(x - 1)) == 0;
    }

	// inherit doc comment
	public void recordDrop() {
		if(RECORD_STATS) {
			DroppedSentMessageStatHandler.TCP_PING_REPLIES.addMessage(this);
		}
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

    // overrides Object.toString
    public String toString() {
        return "PingReply("+getIP()+":"+getPort()
            +", "+super.toString()+")";
    }

    //Unit test: tests/com/limegroup/gnutella/messages/PingReplyTest
}
