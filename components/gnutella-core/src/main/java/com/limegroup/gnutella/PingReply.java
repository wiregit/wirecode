package com.limegroup.gnutella;

import java.io.*;

/**
 * A ping reply message, aka, "pong".  This implementation provides a way
 * to "mark" pongs as being from supernodes.
 */
public class PingReply extends Message implements Serializable{
    //WARNING: see note in Message about IP addresses

    /** All the data.  We extract the port, ip address, number of files,
     *  and number of kilobytes lazily. */
    private byte[] payload;
    /** The IP string as extracted from payload[2..5].  Cached to avoid
     *  allocations.  LOCKING: obtain this' monitor. */
    private String ip;

    /**
     * Create a new unmarked PingReply from scratch
     *
     * @requires ip.length==4 and ip is in <i>BIG-endian</i> byte order,
     *  0 < port < 2^16 (i.e., can fit in 2 unsigned bytes),
     *  0 < files and kbytes < 2^32 (i.e., can fit in 4 unsigned bytes)
     */
    public PingReply(byte[] guid, byte ttl,
             int port, byte[] ip, long files, long kbytes) {
        super(guid, Message.F_PING_REPLY, ttl, (byte)0, 14);
        this.payload=new byte[14];
        //It's ok if casting port, files, or kbytes turns negative.
        ByteOrder.short2leb((short)port, payload, 0);
        //Payload stores IP in BIG-ENDIAN
        payload[2]=ip[0];
        payload[3]=ip[1];
        payload[4]=ip[2];
        payload[5]=ip[3];
        ByteOrder.int2leb((int)files, payload, 6);
        ByteOrder.int2leb((int)kbytes, payload, 10);
    }

    /**
     * Exactly like PingReply(guid, ttl, port, ip, files, kbytes), except that
     * the message is "marked" if marked==true.  If the message is marked, the
     * files or kbytes values may be adjusted.  
     */
    public PingReply(byte[] guid, byte ttl,
             int port, byte[] ip, long files, long kbytes, 
             boolean marked) { 
        this(guid, ttl, port, ip, files,
             marked ? mark(kbytes) : kbytes);
    }

    /**
     * Wrap a PingReply around stuff snatched from the network.
     * <p>
     * Initially this method required that payload.lenght == 14. But now we
     * want to have support for big pings and pongs. 
     */
    public PingReply(byte[] guid, byte ttl, byte hops,
             byte[] payload) {
        super(guid, Message.F_PING_REPLY, ttl, hops, payload.length);
        this.payload=payload;
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

    private static boolean isPowerOf2(int x) {
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


    /** Unit test */
    /*
      public static void main(String args[]) {
      long u4=0x00000000FFFFFFFFl;
      int u2=0x0000FFFF;
      byte[] ip={(byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x1};
      PingReply pr=new PingReply(new byte[16], (byte)0,
      u2, ip, u4, u4);
      Assert.that(pr.getPort()==u2);
      Assert.that(pr.getFiles()==u4);
      long kbytes=pr.getKbytes();
      Assert.that(kbytes==u4, Long.toHexString(kbytes));
      String ip2=pr.getIP();
      Assert.that(ip2.equals("255.0.0.1"), ip2);
      Assert.that(pr.ip!=null);  //Looking at private data
      ip2=pr.getIP();
      Assert.that(ip2.equals("255.0.0.1"), ip2);
      Assert.that(! pr.isMarked());
      
      Assert.that(! isPowerOf2(-1));
      Assert.that(! isPowerOf2(0));
      Assert.that(isPowerOf2(1));
      Assert.that(isPowerOf2(2));
      Assert.that(! isPowerOf2(3));
      Assert.that(isPowerOf2(4));
      Assert.that(isPowerOf2(16));
      Assert.that(! isPowerOf2(18));
      Assert.that(isPowerOf2(64));
      Assert.that(! isPowerOf2(71));
      
      pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
      0, 0, false);
      Assert.that(! pr.isMarked());        
      pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
      0, 0, true);
      Assert.that(pr.isMarked());
      pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
      5, 2348, false);        
      Assert.that(! pr.isMarked());
      Assert.that(pr.getKbytes()==2348);
      pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
      5, 2348, true);
      Assert.that(pr.isMarked());
      pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
      5, 345882, false);
      Assert.that(! pr.isMarked());
      pr=new PingReply(new byte[16], (byte)2, 6346, new byte[4],
      5, 345882, true);
      Assert.that(pr.isMarked());
      doBigPongTest();
      }
      
      private static void doBigPongTest() {
      byte[] payload = new byte[14+2];
      //add the port
      payload[0] = 0x0F;
      payload[1] = 0x00;//port 
      
      payload[2] = 0x10;
      payload[3] = 0x10;
      payload[4] = 0x10;
      payload[5] = 0x10;//ip = 16.16.16.16
      
      payload[6] = 0x0F;//
      payload[7] = 0x00;//
      payload[8] = 0x00;//
      payload[9] = 0x00;//15 files shared
      
      payload[10] = 0x0F;//
      payload[11] = 0x00;//
      payload[12] = 0x00;//
      payload[13] = 0x00;//15 KB
      //OK Now for the big pong part
      payload[14] = (byte) 65;
      payload[15] = (byte) 66;
      PingReply pr = new PingReply(new byte[4], (byte)2, (byte)4, payload);
      //Start testing
      Assert.that(pr.getPort() == 15, "wrong port");
      String ip = pr.getIP();
      Assert.that(ip.equals("16.16.16.16"),"wrong IP");
      Assert.that(pr.getFiles() == 15, "wrong files");
      Assert.that(pr.getKbytes() == 15, "Wrong share size");
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try{
      pr.writePayload(stream);
      }catch(IOException ioe){
      ioe.printStackTrace();
      Assert.that(false, "problem with writing out big pong");
      }
      byte[] op = stream.toByteArray();
      byte[] big = new byte[2];
      big[0] = op[op.length-2];
      big[1] = op[op.length-1];
      String out = new String(big);
      Assert.that(out.equals("AB"), "Big part of pong lost");
      //come this far means its OK
      System.out.println("Passed");
      }
    */
}
