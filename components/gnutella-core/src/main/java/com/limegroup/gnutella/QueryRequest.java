package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import java.util.StringTokenizer;

public class QueryRequest extends Message implements Serializable{
    /** The minimum speed and query request, including the null terminator.
     *  We extract the minimum speed and String lazily. */
    private byte[] payload;
    private int minSpeed;
    /** The query string, if we've already extracted it.  Null otherwise. 
     *  LOCKING: obtain this' lock. */
    private String query=null;
    private String richQuery = null;
    /** Flag indicating if byte[] payload and inst vars are synced */
    private boolean payloadHarmonized = false; 

    // HUGE v0.93 fields
    /** Any URN types requested on responses */
    private Set requestedUrnTypes = null;
    /** Any exact URNs requested to match */
    private Set queryUrns = null;

    /**
     * Builds a new query from scratch
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     */
    public QueryRequest(byte ttl, int minSpeed, String query, String richQuery) {
        this(ttl, minSpeed, query, richQuery, false, null, null);
    }


    /**
     * Builds a new query from scratch but you can flag it as a Requery, if 
     * needed.
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, String richQuery, boolean isRequery,
                        Set requestedUrnTypes, Set queryUrns) {
        // don't worry about getting the length right at first
        super((isRequery ? GUID.makeGuidRequery() : GUID.makeGuid()),
              Message.F_QUERY, ttl, /* hops */ (byte)0, /* length */ 0);
        this.minSpeed=minSpeed;
        this.query=query;
        this.richQuery=richQuery;
		if(requestedUrnTypes != null) {
			this.requestedUrnTypes = new HashSet(requestedUrnTypes);
		}
		if(queryUrns != null) {
			this.queryUrns = new HashSet(queryUrns);
		}
        buildPayload(); // now the length has been set
    }

    /**
     * Older form of the constructor calls the newer form of the constructor
     * with a empty rich query
     */
    public QueryRequest(byte ttl, int minSpeed, String query) {
        this(ttl, minSpeed, query, "");
    }


    /**
     * Older form of the constructor calls the newer form of the constructor
     * with a empty rich query, but allows you to flag as a requery
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, boolean isRequery) {
        this(ttl, minSpeed, query, "", isRequery, null, null);
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
    
    private void buildPayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)minSpeed,baos); // write minspeed
            baos.write(query.getBytes());              // write query
            baos.write(0);                             // null
            // now write any & all HUGE v0.93 General Extension Mechanism extensions
            boolean addDelimiterBefore = false;
			
			// add the rich query
            addDelimiterBefore = 
			    writeGemExtension(baos, addDelimiterBefore, richQuery);

			// add the urns
            addDelimiterBefore = 
			    writeGemExtensions(baos, addDelimiterBefore, 
								   queryUrns == null ? null : queryUrns.iterator());

			// add the urn types
            addDelimiterBefore = 
			    writeGemExtensions(baos, addDelimiterBefore, 
								   requestedUrnTypes == null ? null : 
								   requestedUrnTypes.iterator());

            baos.write(0);                             // final null
            payload=baos.toByteArray();
            updateLength(payload.length); 
            payloadHarmonized=true;
        } catch (IOException ioe) {
            System.out.println("QueryRequest.buildPayload() IOException");
        }
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
        if(!payloadHarmonized) {
            scanPayload();
        }
        return query;
    }
    
	/**
	 * Returns the rich query string.
	 *
	 * @return the rich query string
	 */
    public synchronized String getRichQuery() {
        if(!payloadHarmonized) {
            scanPayload();
        }
        return richQuery;
    }
 
	/**
	 * Returns the <tt>Set</tt> of URN types requested for this query, or
	 * <tt>null</tt> if there are no specified URN types.
	 *
	 * @return the <tt>Set</tt> of URN types requested for this query, or
	 * <tt>null</tt> if there are no specified URN types
	 */
    public synchronized Set getRequestedUrnTypes() {
        if(!payloadHarmonized) {
            scanPayload();
        }
		if(requestedUrnTypes != null) {
			return new HashSet(requestedUrnTypes);
		}
		return null;
    }
    
	/**
	 * Returns the <tt>Set</tt> of <tt>URN</tt> instances for this query, or 
	 * <tt>null</tt> if there are no URNs specified for the query.
	 *
	 * @return  the <tt>Set</tt> of <tt>URN</tt> instances for this query, or 
	 * <tt>null</tt> if there are no URNs specified for the query
	 */
    public synchronized Set getQueryUrns() {
        if(!payloadHarmonized) {
            scanPayload();
        }
		if( queryUrns != null) {
			return new HashSet(queryUrns);
		}
		return null;
    }
    
	/**
	 * Scans the payload for the query request, initializing variables
	 * for the query request in the process.
	 */
    private void scanPayload() {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(payload);
            short sp = ByteOrder.leb2short(bais);
            minSpeed = ByteOrder.ubytes2int(sp);
            query = readNullTerminatedString(bais);
            // handle extensions, which include rich query and URN stuff
            queryUrns=null;
            requestedUrnTypes=null;
            String exts = readNullTerminatedString(bais);
            StringTokenizer stok = new StringTokenizer(exts,"\u001c");
            while (stok.hasMoreElements()) {
                handleGemExtensionString(stok.nextToken());
            }
            if (richQuery == null) richQuery=""; 
            payloadHarmonized=true;
        } catch (IOException ioe) {
            System.out.println("QueryRequest.scanPayload() IOException");
        }
    }
    
	/**
	 * Handles an individual HUGE "General Extension Mechanism" (GEM)
	 * string, adding the appropriate URN, URN type, or xml query data 
	 * to the query reply.
	 *
	 * @param urnString the string containing the GEM data
	 */
    private void handleGemExtensionString(String urnString) {
		if(URN.isUrn(urnString)) {
			// it's an URN to match, of form "urn:namespace:etc"
			URN urn = null;
			try {
				urn = URNFactory.createUrn(urnString);
			} catch(IOException e) {
				// the urn string is invalid -- just return
				return;
			}
			if(queryUrns == null) {
				queryUrns = new HashSet();
			}
			queryUrns.add(urn);
			// but also, it's an implicit request for similar
			// URNs on responses, so add the URN prefix there, too
			requestedUrnTypes.add(urn.getTypeString());
		} else if(URN.isUrnType(urnString)) {
			// it's an URN type to return, of form "urn" or "urn:namespace"
			if(requestedUrnTypes == null) requestedUrnTypes = new HashSet();
			requestedUrnTypes.add(urnString);
		} else if (urnString.startsWith("<?xml")) {
            // rich query
            richQuery = urnString;
        }
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
    public byte getQueryByteAt(int pseudoIndex)
		throws ArrayIndexOutOfBoundsException {
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
        if(!payloadHarmonized) {
            scanPayload();
        }
        return minSpeed;
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








