package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.guess.*;
import java.io.*;
import com.sun.java.util.collections.*;
import java.util.StringTokenizer;

/**
 * A Gnutella query request method.  In addition to a query string, queries can
 * include a minimum size string, metadata, URN info, and a QueryKey.  There are
 * seven constructors in this to make new outgoing messages from scratch.
 * Several take GUIDs as arguments; this allows the GUI to prepare a result
 * panel <i>before</i> sending the query to the network.  One takes a isRequery
 * argument; this is used for automatic re-query capabilities (DownloadManager).
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
    private final Set /* of UrnType */ requestedUrnTypes;

    /** 
	 * Specific URNs requested.
	 */
    private final Set /* of URN */ queryUrns;

    /**
     * The Query Key associated with this Query.
     */
    private QueryKey queryKey = null;

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
        this(guid, ttl, minSpeed, query, richQuery, false, null, null, null);
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
        this(newQueryGUID(false), ttl, minSpeed, query, "", false, null, null,
             null);
    }


    /**
     * Builds a new query from scratch, with no metadata, marking the GUID
     * as a requery iff isRequery.
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, boolean isRequery) {
        this(newQueryGUID(isRequery), ttl, minSpeed, query, "", isRequery, 
			 null, null, null);
    }


    /**
     * Builds a new query from scratch, with metadata, marking the GUID
     * as a requery iff isRequery.
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, String richQuery,
                        boolean isRequery) {
        this(newQueryGUID(isRequery), ttl, minSpeed, query, richQuery, isRequery, 
			 null, null, null);
    }

    /**
     * Builds a new query from scratch but you can flag it as a Requery, if 
     * needed.
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     * @param requestedUrnTypes <tt>Set</tt> of <tt>UrnType</tt> instances
     *  requested for this query, which may be empty or null if no types were
     *  requested
	 * @param queryUrns <tt>Set</tt> of <tt>URN</tt> instances requested for 
     *  this query, which may be empty or null if no URNs were requested
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed, 
                        String query, String richQuery, boolean isRequery,
                        Set requestedUrnTypes, Set queryUrns) {
        this(guid, ttl, minSpeed, query, richQuery, isRequery,
             requestedUrnTypes, queryUrns, null);
    }


    /**
     * Builds a new query from scratch but you can flag it as a Requery, if 
     * needed.
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     * @param requestedUrnTypes <tt>Set</tt> of <tt>UrnType</tt> instances
     *  requested for this query, which may be empty or null if no types were
     *  requested
	 * @param queryUrns <tt>Set</tt> of <tt>URN</tt> instances requested for 
     *  this query, which may be empty or null if no URNs were requested
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed, 
                        String query, String richQuery, boolean isRequery,
                        Set requestedUrnTypes, Set queryUrns,
                        QueryKey queryKey) {
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
        if (queryKey != null)
            this.queryKey = queryKey;
		
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

            // add the GGEP Extension
            if (this.queryKey != null) {
                // get query key in byte form....
                ByteArrayOutputStream qkBytes = new ByteArrayOutputStream();
                this.queryKey.write(qkBytes);
                // construct the GGEP block
                GGEP ggepBlock = new GGEP(false); // do COBS
                ggepBlock.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT,
                              qkBytes.toByteArray());
                ByteArrayOutputStream ggepBytes = new ByteArrayOutputStream();
                ggepBlock.write(ggepBytes);
                // write out GGEP
                addDelimiterBefore =
                    writeGemExtension(baos, addDelimiterBefore,
                                      new String(ggepBytes.toByteArray()));
            }

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
        QueryKey tempQueryKey = null;
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
					if(UrnType.isSupportedUrnType(curExtStr)) {
						tempRequestedUrnTypes.add(UrnType.createUrnType(curExtStr));
					}
				} else if (curExtStr.startsWith("<?xml")) {
					// rich query
					tempRichQuery = curExtStr;
				} else if ((curExtStr.getBytes())[0] == 
                           GGEP.GGEP_PREFIX_MAGIC_NUMBER)
                    tempQueryKey = parseGGEP(curExtStr);
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
        queryKey = tempQueryKey;
    }

    // handles parsing of all GGEP blocks.  may need to change return sig
    // if new things are needed....
    // TODO: technically there may be multiple GGEP blocks here - we tried to
    // get all but encountered a infinite loop so just try to get one for now.
    private final QueryKey parseGGEP(String ggepString) {
        QueryKey retQK = null;
        byte[] ggepBytes = ggepString.getBytes();
        try {
            GGEP ggepBlock = new GGEP(ggepBytes, 0, null);
            if (ggepBlock.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                byte[] qkBytes = 
                ggepBlock.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
                retQK = QueryKey.getQueryKey(qkBytes);
            }
        }        
        catch (BadGGEPBlockException ignored) {}
        catch (BadGGEPPropertyException ignored) {}
        return retQK;
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
		if(RECORD_STATS) {
			SentMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(this);
		}
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
	 * Returns the <tt>Set</tt> of URN types requested for this query.
	 *
	 * @return the <tt>Set</tt> of <tt>UrnType</tt> instances requested for this
     * query, which may be empty (not null) if no types were requested
	 */
    public Set getRequestedUrnTypes() {
		return requestedUrnTypes;
    }
    
	/**
	 * Returns the <tt>Set</tt> of <tt>URN</tt> instances for this query.
	 *
	 * @return  the <tt>Set</tt> of <tt>URN</tt> instances for this query, which
	 * may be empty (not null) if no URNs were requested
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

    
    /**
     * Returns the QueryKey associated with this Request.  May very well be
     * null.  Usually only UDP QueryRequests will have non-null QueryKeys.
     */
    public QueryKey getQueryKey() {
        return queryKey;
    }

	// inherit doc comment
	public void recordDrop() {
		if(RECORD_STATS) {
			DroppedSentMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(this);	   
		}
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
		       "query urns:        "+getQueryUrns().size()+"\r\n"+
		       "min speed:         "+getMinSpeed();
    }
}








