package com.limegroup.gnutella;

import java.io.*;

public class QueryRequest extends Message implements Serializable{
    /** The minimum speed and query request, including the null terminator.
     *  We extract the minimum speed and String lazily. */
    private byte[] payload;
    /** The query string, if we've already extracted it.  Null otherwise. */
    private String textQuery=null;
    /** The rich query string, if we've already extracted it.
     *  Null otherwise. */
    private String richQuery=null;

    /**
     * Builds a new query from scratch
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     */
    public QueryRequest(byte ttl, int minSpeed, String textQuery,
                        String richQuery) {
        //Allocate two bytes for min speed plus query string and null terminator
        //plus rich query string and null terminator
        super(Message.F_QUERY, ttl, 2+textQuery.length()+1+
                                      richQuery.length()+1);
        this.textQuery=textQuery;
        this.richQuery=richQuery;
        payload=new byte[2+textQuery.length()+1+richQuery.length()+1];
        //Extract minimum speed.  It's ok if "(short)minSpeed" is negative.
        int i = 0;
        ByteOrder.short2leb((short)minSpeed, payload, 0);
        i+=2;
        //Copy bytes from query string to payload
        byte[] textBytes=textQuery.getBytes();
        System.arraycopy(textBytes,0,payload,i,textBytes.length);
        i+=textBytes.length;
        //Null terminate it.
        payload[i++]=(byte)0;
        //Copy bytes from metadata string to payload
        byte[] richBytes=richQuery.getBytes();
        System.arraycopy(richBytes,0,payload,i,richBytes.length);
        i+=richBytes.length;
        //Null terminate it.
        payload[i++]=(byte)0;
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

    public String getTextQuery() {
        //Use cached result if possible.
        if (textQuery!=null)
            return textQuery;

        // Find the null terminator
        int end;
        for(end=2; (end < payload.length) && (payload[end] != (byte)0); end++);

        textQuery = new String(payload, 2, end-2);
        return textQuery;
    }

    public String getRichQuery() {
        //Use cached result if possible.
        if (richQuery!=null)
            return richQuery;

        // Find the first null terminator
        int start;
        for(start=2; (start < payload.length) && (payload[start] != (byte)0);
            start++);
        // Advance past the first null
        start++;
        // Find the second null terminator
        int end;
        for(end=start; (end < payload.length) && (payload[end] != (byte)0);
            end++);

        // Catch the no rich query case.
        if(end < payload.length)
            richQuery = new String(payload, start, end-start);
        else
            richQuery = "";
        return richQuery;
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
        return "QueryRequest("+getTextQuery()+", "+getMinSpeed()
            +", "+super.toString()+")";
    }

//      /** Unit test */
//      public static void main(String args[]) {
//      int u2=0x0000FFFF;
//      QueryRequest qr=new QueryRequest((byte)3,u2,"");
//      Assert.that(qr.getMinSpeed()==u2);
//      Assert.that(qr.getTextQuery().equals(""));

//      qr=new QueryRequest((byte)3,(byte)1,"ZZZ");
//      Assert.that(qr.getMinSpeed()==(byte)1);
//      Assert.that(qr.getTextQuery().equals("ZZZ"));

//      //String is single null-terminated.
//      byte[] payload=new byte[2+2];
//      payload[2]=(byte)65;
//      qr=new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
//      Assert.that(qr.getLength()==4);
//      String s=qr.getTextQuery();
//      Assert.that(s.equals("A"), s);

//      //String is double null-terminated.
//      payload=new byte[2+3];
//      payload[2]=(byte)65;
//      qr=new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
//      s=qr.getTextQuery();
//      Assert.that(s.equals("A"), s);

//      //String is empty.
//      payload=new byte[2+1];
//      qr=new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
//      s=qr.getTextQuery();
//      Assert.that(s.equals(""), s);
//      }
}
