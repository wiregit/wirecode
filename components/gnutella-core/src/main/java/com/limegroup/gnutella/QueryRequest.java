package com.limegroup.gnutella;

import java.io.*;

public class QueryRequest extends Message implements Serializable{
    /** The minimum speed and query request, including the null terminator.
     *  We extract the minimum speed and String lazily. */
    private byte[] payload;
    /** The query string, if we've already extracted it.  Null otherwise. */
    private String query=null;

    /**
     * Builds a new query from scratch
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     */
    public QueryRequest(byte ttl, int minSpeed, String query) {
        //Allocate two bytes for min speed plus query string and null terminator
        super(Message.F_QUERY, ttl, 2+query.length()+1);
        payload=new byte[2+query.length()+1];
        //Extract minimum speed.  It's ok if "(short)minSpeed" is negative.
        ByteOrder.short2leb((short)minSpeed, payload, 0);
        //Copy bytes from query string to payload
        byte[] qbytes=query.getBytes();
        System.arraycopy(qbytes,0,payload,2,qbytes.length);
        //Null terminate it.
        payload[payload.length-1]=(byte)0;
    }

    /*
     * Build a new query with data snatched from network
     *
     * @requires payload.length>=3
     */
    public QueryRequest(byte[] guid, byte ttl, byte hops,
            byte[] payload) {
        super(guid, Message.F_QUERY, ttl, hops, payload.length);
        this.payload=payload;
    }

    protected void writePayload(OutputStream out) throws IOException {
        out.write(payload);
    }

    public String getQuery() {
        //Use cached result if possible.  This is always safe since
        //strings are immutable.
        if (query!=null)
            return query;

        int n=payload.length;
        //Some clients (like Gnotella) DOUBLE null-terminate strings.
        //When you make a Java string with 0 in it, it is NOT ignored.
        //The solution is simple: just shave off the extra null terminator.
        if (super.getLength()>3 && payload[n-2]==(byte)0)
            query=new String(payload,2,payload.length-4);
        //Normal case: single null-terminated.
        //This also handles the special case of an empty search string.
        else
            query=new String(payload,2,payload.length-3);
        return query;
    }

    /**
      * Note: the minimum speed can be represented as a 2-byte unsigned
      * number, but Java shorts are signed.  Hence we must use an int.  The
      * value returned is always smaller than 2^16.
      */
    public int getMinSpeed() {
        short speed=ByteOrder.leb2short(payload,0);
        int ret=ByteOrder.ubytes2int(speed);
        Assert.that(ret>=0, "getMinSpeed got negative value");
        return ret;
    }

    public String toString() {
        return "QueryRequest("+getQuery()+", "+getMinSpeed()
            +", "+super.toString()+")";
    }

//      /** Unit test */
//      public static void main(String args[]) {
//      int u2=0x0000FFFF;
//      QueryRequest qr=new QueryRequest((byte)3,u2,"");
//      Assert.that(qr.getMinSpeed()==u2);
//      Assert.that(qr.getQuery().equals(""));

//      qr=new QueryRequest((byte)3,(byte)1,"ZZZ");
//      Assert.that(qr.getMinSpeed()==(byte)1);
//      Assert.that(qr.getQuery().equals("ZZZ"));

//      //String is single null-terminated.
//      byte[] payload=new byte[2+2];
//      payload[2]=(byte)65;
//      qr=new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
//      Assert.that(qr.getLength()==4);
//      String s=qr.getQuery();
//      Assert.that(s.equals("A"), s);

//      //String is double null-terminated.
//      payload=new byte[2+3];
//      payload[2]=(byte)65;
//      qr=new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
//      s=qr.getQuery();
//      Assert.that(s.equals("A"), s);

//      //String is empty.
//      payload=new byte[2+1];
//      qr=new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
//      s=qr.getQuery();
//      Assert.that(s.equals(""), s);
//      }
}
