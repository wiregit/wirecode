package com.limegroup.gnutella;

import java.io.*;

public class QueryRequest extends Message implements Serializable{
    /** The minimum speed and query request, including the null terminator.
     *  We extract the minimum speed and String lazily. */
    private byte[] payload;
    /** The query string, if we've already extracted it.  Null otherwise. 
     *  LOCKING: obtain this' lock. */
    private String query=null;
    private String richQuery = null;

    /**
     * Builds a new query from scratch
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     */
    public QueryRequest(byte ttl, int minSpeed, String query, String richQuery) {
        //Allocate two bytes for min speed plus query string and null terminator
        super(Message.F_QUERY, ttl, 2+query.length()+1+richQuery.length()+1);
        payload=new byte[2+query.length()+1+richQuery.length()+1];
        int i = 0;//Num bytes in the payload
        //Extract minimum speed.  It's ok if "(short)minSpeed" is negative.
        ByteOrder.short2leb((short)minSpeed, payload, 0);
        i +=2;//two bytes for this  min speed.
        //Copy bytes from query string to payload
        byte[] qbytes=query.getBytes();
        System.arraycopy(qbytes,0,payload,i,qbytes.length);
        i += qbytes.length;
        payload[i]=(byte)0;//Null terminate the plain text query
        i++;
        byte[] richBytes = richQuery.getBytes();
        System.arraycopy(richBytes,0,payload,i,richBytes.length);
        i += richBytes.length;
        payload[i] = (byte)0;//Null to terminate the rich query.
        i++; //just so the records are straight. 
    }

    /**
     * Older form of the constructor calls the newer form of the constructor
     * with a empty rich query
     */
    public QueryRequest(byte ttl, int minSpeed, String query) {
        this(ttl, minSpeed, query, "");
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

    /** 
     * Returns the query string of this message.<p>
     *
     * The caller should not call the getBytes() method on the returned value,
     * as this seems to cause problems on the Japanese Macintosh.  If you need
     * the raw bytes of the query string, call getQueryByteAt(int).
     */
    public synchronized String getQuery() {
        //Use cached result if possible.  This is always safe since
        //strings are immutable.
        if (query!=null)
            return query;
        
        int end;
        //find the first null terminator from byte 2 till the null
        for(end=2; (end < payload.length) && (payload[end] != (byte)0); end++);

        query = new String(payload, 2, end-2);
        return query;

        /* Sumeet : commented out the older version of this method
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
          Assert.that(query!=null, "Returning null value in getQuery");
          return query;
        */
    }

    public synchronized String getRichQuery() {
        if (richQuery != null)
            return richQuery;
        //if we have found it out already use it from before
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
            richQuery = "";// we have checked - the rich query is empty
        return richQuery;
    }
            
    /** 
     * Returns the number of raw bytes used to represent the query in this
     * message, excluding any null terminators. The returned value is typically
     * used in conjunction with getQueryByteAt. Because of character encoding
     * problems, the returned value does not necessarily equal getQuery.length()
     * or getQuery.getBytes().length.  
     */
    public int getQueryLength(){
        //if it's double null terminated
        if (super.getLength()>3 && payload[payload.length-2]==(byte)0)
            return payload.length-4;
        else //normal case
            return payload.length-3;
    }
    
    /** 
     * Returns the pseudoIndex'th byte of the raw query in this message. Throws
     * ArrayIndexOutOfBoundsException if pseudoIndex<0 or
     * pseudoIndex>=getQueryLength, i.e., if the given index is either within
     * the first two bytes of the payload or goes into the null termination
     * area.  Because of different character encodings, the returned value does
     * not necessarily equal getQuery().getBytes()[pseudoIndex].
     */
    public byte getQueryByteAt(int pseudoIndex)throws 
                                      ArrayIndexOutOfBoundsException{
        if (pseudoIndex<0 || pseudoIndex > getQueryLength()-1)
            throw new ArrayIndexOutOfBoundsException();
        return payload[pseudoIndex+2];
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

    /** Unit test */
    /*
    public static void main(String args[]) {
        int u2=0x0000FFFF;
        QueryRequest qr=new QueryRequest((byte)3,u2,"");
        Assert.that(qr.getMinSpeed()==u2);
        Assert.that(qr.getQuery().equals(""));
        Assert.that(qr.getQueryLength()==0);
        

        qr=new QueryRequest((byte)3,(byte)1,"ZZZ");
        Assert.that(qr.getMinSpeed()==(byte)1);
        Assert.that(qr.getQuery().equals("ZZZ"));
        Assert.that(qr.getQueryLength()==3);
        System.out.println("(ZZZ) First byte = "+qr.getQueryByteAt(0));

        //String is single null-terminated.
        byte[] payload=new byte[2+2];
        payload[2]=(byte)65;
        qr=new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
        Assert.that(qr.getLength()==4);
        String s=qr.getQuery();
        Assert.that(s.equals("A"), s);
        Assert.that(qr.getQueryLength()==1);
        Assert.that(qr.getQueryByteAt(0)==65);//first byte of query.


        //String is double null-terminated.
        payload=new byte[2+3];
        payload[2]=(byte)65;
        qr=new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
        s=qr.getQuery();
        Assert.that(s.equals("A"), s);
        Assert.that(qr.getQueryLength()==1);
        Assert.that(qr.getQueryByteAt(0)==65);

        //String is empty.
        payload=new byte[2+1];
        qr=new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
        s=qr.getQuery();
        Assert.that(s.equals(""), s);
        Assert.that(qr.getQueryLength()==0);
        System.out.println("here comes an exception");
        qr.getQueryByteAt(0);
    }
    */
}








