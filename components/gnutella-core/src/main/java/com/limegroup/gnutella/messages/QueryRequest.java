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
     * The Query Key associated with this query -- can be null.
     */
    private final QueryKey queryKey;

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
						String richQuery, boolean isFirewalled) {
        this(guid, ttl, minSpeed, query, richQuery, false, null, null, null,
             isFirewalled);
    }

    /**
     * Builds a new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.  GUID must have
     * been created via newQueryGUID; this allows the caller to match up results
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed, String query,
                        boolean isFirewalled) {
        this(guid, ttl, minSpeed, query, "", isFirewalled);
    }

    /**
     * Builds a new query from scratch, with no metadata, with a default GUID.
     */
    public QueryRequest(byte ttl, int minSpeed, String query, 
                        boolean isFirewalled) {
        this(newQueryGUID(false), ttl, minSpeed, query, "", false, null, null,
             null, isFirewalled);
    }


    /**
     * Builds a new query from scratch, with no metadata, marking the GUID
     * as a requery iff isRequery.
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, boolean isRequery, boolean isFirewalled) {
        this(newQueryGUID(isRequery), ttl, minSpeed, query, "", isRequery, 
			 null, null, null, isFirewalled);
    }


    /**
     * Builds a new query from scratch, with metadata, marking the GUID
     * as a requery iff isRequery.
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, String richQuery,
                        boolean isRequery, boolean isFirewalled) {
        this(newQueryGUID(isRequery), ttl, minSpeed, query, richQuery, 
             isRequery, null, null, null, isFirewalled);
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
                        boolean isFirewalled) {
        this(guid, ttl, minSpeed, query, richQuery, isRequery,
             requestedUrnTypes, queryUrns, null, isFirewalled);
    }

	/**
	 * Creates a new requery for the specified SHA1 value and the specified
	 * firewall boolean.
	 *
	 * @param sha1 the <tt>URN</tt> of the file to search for
	 * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
	 */
	public static QueryRequest createRequery(URN sha1) {
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
		return new QueryRequest(newQueryGUID(true), (byte)6, 0, "\\", "", 
								true, UrnType.SHA1_SET, sha1Set, 
								!RouterService.acceptedIncomingConnection());
	}
	
	/**
	 * Creates a requery for when we don't know the hash of the file --
	 * we don't know the hash.
	 *
	 * @param query the query string
	 */
	public static QueryRequest createRequery(String query) {
		return new QueryRequest(newQueryGUID(true), (byte)6, 0, query, "",
								true, EMPTY_SET, EMPTY_SET,
								!RouterService.acceptedIncomingConnection());
	}

	/**
	 * Creates a new query for the specified file name, with no XML.
	 *
	 * @param fileName the file name to search for
	 */
	public static QueryRequest createQuery(String fileName) {
		return new QueryRequest(newQueryGUID(false), (byte)6, 0, fileName,
								"", false, EMPTY_SET, EMPTY_SET, 
								!RouterService.acceptedIncomingConnection());
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
                        QueryKey queryKey, boolean isFirewalled) {
        // don't worry about getting the length right at first
        super(guid, Message.F_QUERY, ttl, /* hops */ (byte)0, /* length */ 0);
        if (minSpeed == 0) {
            // user has not specified a Min Speed - go ahead and set it for
            // them, as appropriate
            
            // the new Min Speed format - looks reversed but
            // it isn't because of ByteOrder.short2leb
            minSpeed = 0x00000080; 
            // set the firewall bit if i'm firewalled
            if (isFirewalled)
                minSpeed |= 0x40;
            // LimeWire's ALWAYS want rich results....
            if (true)
                minSpeed |= 0x20;
        }
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
			tempRequestedUrnTypes = EMPTY_SET;
		}
		
		if(queryUrns != null) {
			tempQueryUrns = new HashSet(queryUrns);
		} else {
			tempQueryUrns = EMPTY_SET;
		}

        this.queryKey = queryKey;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteOrder.short2leb((short)minSpeed,baos); // write minspeed
            baos.write(query.getBytes());              // write query
            baos.write(0);                             // null
            // now write any & all HUGE v0.93 General Extension Mechanism 
			// extensions
            boolean addDelimiterBefore = false;
			
            byte[] richQueryBytes = null;
            if(richQuery!=null)
                richQueryBytes = richQuery.getBytes("UTF-8");
            
			// add the rich query
            addDelimiterBefore = 
			    writeGemExtension(baos, addDelimiterBefore, richQueryBytes);

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
                                      ggepBytes.toByteArray());
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
            tempQuery = new String(super.readNullTerminatedBytes(bais));
            // handle extensions, which include rich query and URN stuff
            byte[] extsBytes = super.readNullTerminatedBytes(bais);
            int currIndex = 0;
            // while we don't encounter a null....
            while ((currIndex < extsBytes.length) && 
                   (extsBytes[currIndex] != (byte)0x00)) {

                // HANDLE GGEP STUFF
                if (extsBytes[currIndex] == GGEP.GGEP_PREFIX_MAGIC_NUMBER) {
                    int[] endIndex = new int[1];
                    endIndex[0] = currIndex+1;
                    final String QK_SUPP = GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT;
                    try {
                        GGEP ggep = new GGEP(extsBytes, currIndex, endIndex);
                        if (ggep.hasKey(QK_SUPP)) {
                            byte[] qkBytes = ggep.getBytes(QK_SUPP);
                            tempQueryKey = QueryKey.getQueryKey(qkBytes, false);
                        }
                    }
                    catch (BadGGEPBlockException ignored) {}
                    catch (BadGGEPPropertyException ignored) {}
                    
                    currIndex = endIndex[0];
                }
                else { // HANDLE HUGE STUFF
                    int delimIndex = currIndex;
                    while ((delimIndex < extsBytes.length) 
                           && (extsBytes[delimIndex] != (byte)0x1c))
                        delimIndex++;
                    if (delimIndex > extsBytes.length) 
                        ; // we've overflown and not encounted a 0x1c - discard
                    else {
                        String curExtStr = new String(extsBytes, currIndex,
                                          delimIndex - currIndex, "UTF-8");
                        if (URN.isUrn(curExtStr)) {
                            // it's an URN to match, of form "urn:namespace:etc"
                            URN urn = null;
                            try {
                                urn = URN.createSHA1Urn(curExtStr);
                            } 
                            catch(IOException e) {
                                // the urn string is invalid -- so continue
                                continue;
                            }
                            if(tempQueryUrns == null) 
                                tempQueryUrns = new HashSet();
                            tempQueryUrns.add(urn);
                        } 
                        else if (UrnType.isSupportedUrnType(curExtStr)) {
                            // it's an URN type to return, of form "urn" or 
                            // "urn:namespace"
                            if(tempRequestedUrnTypes == null) 
                                tempRequestedUrnTypes = new HashSet();
                            if(UrnType.isSupportedUrnType(curExtStr)) 
                                tempRequestedUrnTypes.add(UrnType.createUrnType(curExtStr));
                        } 
                        else if (curExtStr.startsWith("<?xml"))
                            // rich query
                            tempRichQuery = curExtStr;
                    }
                    currIndex = delimIndex+1;
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
        queryKey = tempQueryKey;
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
     * Accessor fot the payload of the query hit.
     *
     * @return the query hit payload
     */
    public byte[] getPayload() {
        return payload;
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


    /*    
     * Returns true if the query source is a firewalled servent.
     */
    public boolean isFirewalledSource() {
        if ((minSpeed & 0x0080) > 0) {
            if ((minSpeed & 0x0040) > 0)
                return true;
        }
        return false;
    }
 
 
    /**
     * Returns true if the query source desires Lime meta-data in responses.
     */
    public boolean desiresXMLResponses() {
        if ((minSpeed & 0x0080) > 0) {
            if ((minSpeed & 0x0020) > 0)
                return true;
        }
        return false;        
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
 		return "<query: \""+getQuery()+"\", "+
            "ttl: "+getTTL()+", "+
            "hops: "+getHops()+", "+            
            "meta: \""+getRichQuery()+"\", "+
            "types: "+getRequestedUrnTypes().size()+","+
            "urns: "+getQueryUrns().size()+">";
    }
}








