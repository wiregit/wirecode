package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import java.util.StringTokenizer;

public class QueryRequest extends Message implements Serializable{
    /** The minimum speed and query request, including the null terminator.
     *  We extract the minimum speed and String lazily. */
    private final byte[] payload;
    private final int minSpeed;
    /** The query string, if we've already extracted it.  Null otherwise. 
     *  LOCKING: obtain this' lock. */
    private final String query;//=null;
    private final String richQuery;// = null;

    // HUGE v0.93 fields
    /** Any URN types requested on responses */
    private final Set requestedUrnTypes;// = null;
    /** Any exact URNs requested to match */
    private final Set queryUrns;// = null;

	/**
	 * Constant for an empty, unmodifiable <tt>Set</tt>.  This is necessary
	 * because Collections.EMPTY_SET is not serializable in the collections 
	 * 1.1 implementation.
	 */
	private static final Set EMPTY_SET = 
		Collections.unmodifiableSet(new HashSet());

	/**
	 * Cached immutable empty array of bytes to avoid unnecessary allocations.
	 */
	private final static byte[] EMPTY_BYTE_ARRAY = new byte[0];

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
		Set tempRequestedUrnTypes = null;
		Set tempQueryUrns = null;
		if(requestedUrnTypes != null) {
			tempRequestedUrnTypes = new HashSet(requestedUrnTypes);
			//this.requestedUrnTypes = new HashSet(requestedUrnTypes);
		} else {
			tempRequestedUrnTypes = new HashSet();
		}
		
		if(queryUrns != null) {
			tempQueryUrns = new HashSet(queryUrns);
			//this.queryUrns = new HashSet(queryUrns);
		} else {
			tempQueryUrns = new HashSet();
		}
			
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
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
								   tempQueryUrns == null ? null : 
								   tempQueryUrns.iterator());

			// add the urn types
            addDelimiterBefore = 
			    writeGemExtensions(baos, addDelimiterBefore, 
								   tempRequestedUrnTypes == null ? null : 
								   tempRequestedUrnTypes.iterator());

            baos.write(0);                             // final null
		} catch (IOException e) {
			e.printStackTrace();
		}
		payload=baos.toByteArray();
		updateLength(payload.length); 

		this.queryUrns = Collections.unmodifiableSet(tempQueryUrns);
		this.requestedUrnTypes = Collections.unmodifiableSet(tempRequestedUrnTypes);
    }

    /**
     * Older form of the constructor calls the newer form of the constructor
     * with a empty rich query
     */
    public QueryRequest(byte ttl, int minSpeed, String query) {
        this(ttl, minSpeed, query, "", false, null, null);
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
		if(this.payload == null) {
			this.payload = EMPTY_BYTE_ARRAY;
		}
		else {
			this.payload=payload;
		}
		String tempQuery = "";
		String tempRichQuery = "";
		int tempMinSpeed = 0;
		Set tempQueryUrns = null;
		Set tempRequestedUrnTypes = null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(payload);
            short sp = ByteOrder.leb2short(bais);
            tempMinSpeed = ByteOrder.ubytes2int(sp);
            //query = readNullTerminatedString(bais);
            tempQuery = readNullTerminatedString(bais);
            // handle extensions, which include rich query and URN stuff
            //queryUrns=null;
            //requestedUrnTypes=null;
            String exts = readNullTerminatedString(bais);
            StringTokenizer stok = new StringTokenizer(exts,"\u001c");
            while (stok.hasMoreElements()) {
                //handleGemExtensionString(stok.nextToken());
				String curExtStr = stok.nextToken();
				if(URN.isUrn(curExtStr)) {
					// it's an URN to match, of form "urn:namespace:etc"
					URN urn = null;
					try {
						urn = URNFactory.createUrn(curExtStr);
					} catch(IOException e) {
						// the urn string is invalid -- so continue
						continue;
					}
					if(tempQueryUrns == null) {
						tempQueryUrns = new HashSet();
					}
					tempQueryUrns.add(urn);
					if(tempRequestedUrnTypes == null) {
						tempRequestedUrnTypes = new HashSet();
					}
					// but also, it's an implicit request for similar
					// URNs on responses, so add the URN prefix there, too
					tempRequestedUrnTypes.add(urn.getUrnType());
				} else if(UrnType.isSupportedUrnType(curExtStr)) {
					// it's an URN type to return, of form "urn" or "urn:namespace"
					if(tempRequestedUrnTypes == null) {
						tempRequestedUrnTypes = new HashSet();
					}
					tempRequestedUrnTypes.add(curExtStr);
				} else if (curExtStr.startsWith("<?xml")) {
					// rich query
					tempRichQuery = curExtStr;
				}
            }
        } catch (IOException ioe) {
			ioe.printStackTrace();
        }
		query = tempQuery;
		richQuery = tempRichQuery;
		minSpeed = tempMinSpeed;
		if(tempQueryUrns == null) {
			this.queryUrns = EMPTY_SET; 
		}
		else {
			this.queryUrns = Collections.unmodifiableSet(tempQueryUrns);
		}
		if(tempRequestedUrnTypes == null) {
			this.requestedUrnTypes = EMPTY_SET;
		}
		else {
			this.requestedUrnTypes =
			    Collections.unmodifiableSet(tempRequestedUrnTypes);
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
    public String getQuery() {
        return query;
    }
    
	/**
	 * Returns the rich query string.
	 *
	 * @return the rich query string
	 */
    public String getRichQuery() {
        return richQuery;
    }
 
	/**
	 * Returns the <tt>Set</tt> of URN types requested for this query, or
	 * <tt>null</tt> if there are no specified URN types.
	 *
	 * @return the <tt>Set</tt> of URN types requested for this query, or
	 * <tt>null</tt> if there are no specified URN types
	 */
    public Set getRequestedUrnTypes() {
		return requestedUrnTypes;
    }
    
	/**
	 * Returns the <tt>Set</tt> of <tt>URN</tt> instances for this query, or 
	 * <tt>null</tt> if there are no URNs specified for the query.
	 *
	 * @return  the <tt>Set</tt> of <tt>URN</tt> instances for this query, or 
	 * <tt>null</tt> if there are no URNs specified for the query
	 */
    public Set getQueryUrns() {
		return queryUrns;
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








