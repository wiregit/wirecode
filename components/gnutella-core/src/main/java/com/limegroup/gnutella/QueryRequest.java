package com.limegroup.gnutella;

import java.io.*;

/**
 * A Gnutella query request method.  In addition to a query string, queries can
 * include a minimum size string and metadata.  There are four constructors in
 * this to make new outgoing messages from scratch.  Two of them take GUIDs as
 * arguments; this allows the GUI to prepare a result panel <i>before</i>
 * sending the query to the network.  One takes a isRequery argument; this is
 * used for automatic re-query capabilities (DownloadManager).
 */
public class QueryRequest extends Message implements Serializable{
    /** The minimum speed and query request, including the null terminator.
     *  We extract the minimum speed and String lazily. */
    private byte[] payload;
    /** The query string, if we've already extracted it.  Null otherwise. 
     *  LOCKING: obtain this' lock. */
    private String query=null;
    private String richQuery = null;


    /**
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.  GUID must have
     * been created via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes) 
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed, 
                        String query, String richQuery) {
        //Allocate two bytes for min speed plus query string and null terminator
        super(guid,
              Message.F_QUERY, ttl, (byte)0, 
              2+query.length()+1+richQuery.length()+1);
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
     * Builds a new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.  GUID must have
     * been created via newQueryGUID; this allows the caller to match up results
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed, String query) {
        this(guid, ttl, minSpeed, query, "");
    }

    /**
     * Builds a new query from scratch, with no metadata, with a default GUID.
     */
    public QueryRequest(byte ttl, int minSpeed, String query) {
        this(newQueryGUID(false), ttl, minSpeed, query, "");
    }


    /**
     * Builds a new query from scratch, with no metadata, marking the GUID
     * as a requery iff isRequery.
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, boolean isRequery) {
        this(newQueryGUID(isRequery), ttl, minSpeed, query, "");
    }


    /**
     * Builds a new query from scratch, with metadata, marking the GUID
     * as a requery iff isRequery.
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, String richQuery,
                        boolean isRequery) {
        this(newQueryGUID(isRequery), ttl, minSpeed, query, richQuery);
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

    /**
     * Returns a new GUID appropriate for query requests.  If isRequery,
     * the GUID query is marked.
     */
    public static byte[] newQueryGUID(boolean isRequery) {
        return isRequery ? GUID.makeGuidRequery() : GUID.makeGuid();
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
        Assert.that(! GUID.isLimeRequeryGUID(qr.getGUID()));

        qr=new QueryRequest((byte)3,(byte)1,"ABC");
        Assert.that(qr.getMinSpeed()==(byte)1);
        Assert.that(qr.getQuery().equals("ABC"));
        Assert.that(qr.getQueryLength()==3);
        Assert.that(! GUID.isLimeRequeryGUID(qr.getGUID()));
        Assert.that(qr.getQueryByteAt(0)==(byte)65);
        Assert.that(! GUID.isLimeRequeryGUID(qr.getGUID()));

        //String is single null-terminated.
        byte[] payload=new byte[2+2];
        payload[2]=(byte)65;
        byte[] guid=GUID.makeGuid();
        qr=new QueryRequest(guid, (byte)0, (byte)0, payload);
        Assert.that(qr.getLength()==4);
        String s=qr.getQuery();
        Assert.that(s.equals("A"), s);
        Assert.that(qr.getQueryLength()==1);
        Assert.that(qr.getQueryByteAt(0)==65);//first byte of query.
        Assert.that(! GUID.isLimeRequeryGUID(qr.getGUID()));


        //String is double null-terminated.
        payload=new byte[2+3];
        payload[2]=(byte)65;
        qr=new QueryRequest(guid, (byte)0, (byte)0, payload);
        s=qr.getQuery();
        Assert.that(s.equals("A"), s);
        Assert.that(qr.getQueryLength()==1);
        Assert.that(qr.getQueryByteAt(0)==65);
        Assert.that(! GUID.isLimeRequeryGUID(qr.getGUID()));

        //String is empty.
        payload=new byte[2+1];
        qr=new QueryRequest(guid, (byte)0, (byte)0, payload);
        s=qr.getQuery();
        Assert.that(s.equals(""), s);
        Assert.that(qr.getQueryLength()==0);
        try {
            qr.getQueryByteAt(0);
            Assert.that(false);
        } catch (ArrayIndexOutOfBoundsException e) { }
        Assert.that(! GUID.isLimeRequeryGUID(qr.getGUID()));

        //Test that only one constructor marks requery status.
        qr=new QueryRequest(RouterService.newQueryGUID(), (byte)2,
                            0, "test");
        Assert.that(! GUID.isLimeRequeryGUID(qr.getGUID()));
        qr=new QueryRequest(RouterService.newQueryGUID(), (byte)2,
                            0, "test", "");
        Assert.that(! GUID.isLimeRequeryGUID(qr.getGUID()));
        qr=new QueryRequest((byte)2, 0, "test", false);
        Assert.that(! GUID.isLimeRequeryGUID(qr.getGUID()));

        qr=new QueryRequest((byte)2, 0, "test", true);
        Assert.that(GUID.isLimeRequeryGUID(qr.getGUID()));
        Assert.that(GUID.isLimeRequeryGUID(qr.getGUID(), false));
        Assert.that(! GUID.isLimeRequeryGUID(qr.getGUID(), true));
        qr=new QueryRequest(newQueryGUID(true), (byte)2, 0, "test");
        Assert.that(GUID.isLimeRequeryGUID(qr.getGUID()));
        Assert.that(GUID.isLimeRequeryGUID(qr.getGUID(), false));
        Assert.that(! GUID.isLimeRequeryGUID(qr.getGUID(), true));
    }
    */
}








