package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import java.util.StringTokenizer;

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
    private final byte[] payload;
    private final int minSpeed;
    /** The query string, if we've already extracted it.  Null otherwise. 
     *  LOCKING: obtain this' lock. */
    private final String query;
    private final String richQuery;

    // HUGE v0.93 fields
    /** 
	 * The types of requested URNs.
	 */
    private final Set requestedUrnTypes;

    /** 
	 * Specific URNs requested.
	 */
    private final Set queryUrns;

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
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.  GUID must have
     * been created via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes) 
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed, String query, 
						String richQuery) {
        this(guid, ttl, minSpeed, query, richQuery, false, null, null);
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
        this(newQueryGUID(false), ttl, minSpeed, query, "", false, null, null);
    }


    /**
     * Builds a new query from scratch, with no metadata, marking the GUID
     * as a requery iff isRequery.
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, boolean isRequery) {
        this(newQueryGUID(isRequery), ttl, minSpeed, query, "", isRequery, 
			 null, null);
    }


    /**
     * Builds a new query from scratch, with metadata, marking the GUID
     * as a requery iff isRequery.
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, String richQuery,
                        boolean isRequery) {
        this(newQueryGUID(isRequery), ttl, minSpeed, query, richQuery, isRequery, 
			 null, null);
    }

    /**
     * Builds a new query from scratch but you can flag it as a Requery, if 
     * needed.
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed, 
                        String query, String richQuery, boolean isRequery,
                        Set requestedUrnTypes, Set queryUrns) {
        // don't worry about getting the length right at first
        super(guid, Message.F_QUERY, ttl, /* hops */ (byte)0, /* length */ 0);
        this.minSpeed=minSpeed;
		if(query == null) {
			this.query = "";
		} else {
			this.query = query;
		}
		if(richQuery == null) {
			this.richQuery = "";
		} else{
			this.richQuery = richQuery;
		}
		Set tempRequestedUrnTypes = null;
		Set tempQueryUrns = null;
		if(requestedUrnTypes != null) {
			tempRequestedUrnTypes = new HashSet(requestedUrnTypes);
		} else {
			tempRequestedUrnTypes = new HashSet();
		}
		
		if(queryUrns != null) {
			tempQueryUrns = new HashSet(queryUrns);
		} else {
			tempQueryUrns = new HashSet();
		}
			
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteOrder.short2leb((short)minSpeed,baos); // write minspeed
            baos.write(query.getBytes());              // write query
            baos.write(0);                             // null
            // now write any & all HUGE v0.93 General Extension Mechanism 
			// extensions
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
     * Build a new query with data snatched from network
     *
     * @requires payload.length>=3
     */
    public QueryRequest(byte[] guid, byte ttl, byte hops,
						byte[] payload) {
        super(guid, Message.F_QUERY, ttl, hops, payload.length);
		if(payload == null) {
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
            ByteArrayInputStream bais = new ByteArrayInputStream(this.payload);
			short sp = ByteOrder.leb2short(bais);
			tempMinSpeed = ByteOrder.ubytes2int(sp);
            tempQuery = super.readNullTerminatedString(bais);
            // handle extensions, which include rich query and URN stuff
            String exts = super.readNullTerminatedString(bais);
            StringTokenizer stok = new StringTokenizer(exts,"\u001c");
            while (stok.hasMoreElements()) {
				String curExtStr = stok.nextToken();
				if(URN.isUrn(curExtStr)) {
					// it's an URN to match, of form "urn:namespace:etc"
					URN urn = null;
					try {
						urn = URN.createSHA1Urn(curExtStr);
					} catch(IOException e) {
						// the urn string is invalid -- so continue
						continue;
					}
					if(tempQueryUrns == null) {
						tempQueryUrns = new HashSet();
					}
					tempQueryUrns.add(urn);
				} else if(UrnType.isSupportedUrnType(curExtStr)) {
					// it's an URN type to return, of form "urn" or 
					// "urn:namespace"
					if(tempRequestedUrnTypes == null) {
						tempRequestedUrnTypes = new HashSet();
					}
					tempRequestedUrnTypes.add(UrnType.createUrnType(curExtStr));
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
	 * Note: the minimum speed can be represented as a 2-byte unsigned
	 * number, but Java shorts are signed.  Hence we must use an int.  The
	 * value returned is always smaller than 2^16.
	 */
    public int getMinSpeed() {
        return minSpeed;
    }

    /** Returns this, because it's always safe to send big queries. */
    public Message stripExtendedPayload() {
        return this;
    }

    public String toString() {
		return "QueryRequest:\r\n"+ 
		       "query:             "+getQuery()+"\r\n"+
		       "rich query:        "+getRichQuery()+"\r\n"+
		       "requestedUrnTypes: "+getRequestedUrnTypes()+"\r\n"+
		       "query urns:        "+getQueryUrns()+"\r\n"+
		       "min speed:         "+getMinSpeed();
    }
}








