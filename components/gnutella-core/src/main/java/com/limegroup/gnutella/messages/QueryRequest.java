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
    private final byte[] PAYLOAD;
    private final int MIN_SPEED;
    /** The query string, if we've already extracted it.  Null otherwise. 
     *  LOCKING: obtain this' lock. */
    private final String QUERY;

	/** The XML query string. */
    private final String XML_QUERY;

    // HUGE v0.93 fields
    /** 
	 * The types of requested URNs.
	 */
    private final Set /* of UrnType */ REQUESTED_URN_TYPES;

    /** 
	 * Specific URNs requested.
	 */
    private final Set /* of URN */ QUERY_URNS;

    /**
     * The Query Key associated with this query -- can be null.
     */
    private final QueryKey QUERY_KEY;

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
        this(guid, ttl, minSpeed, query, richQuery, EMPTY_SET, EMPTY_SET, null,
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
        this(newQueryGUID(false), ttl, minSpeed, query, "", false, null,
             null, isFirewalled);
    }


    /**
     * Builds a new query from scratch, with no metadata, marking the GUID
     * as a requery iff isRequery.
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, boolean isRequery, boolean isFirewalled) {
        this(newQueryGUID(isRequery), ttl, minSpeed, query, "", 
			 EMPTY_SET, EMPTY_SET, null, isFirewalled);
    }


    /**
     * Builds a new query from scratch, with metadata, marking the GUID
     * as a requery iff isRequery.
     */
    public QueryRequest(byte ttl, int minSpeed, 
                        String query, String richQuery,
                        boolean isRequery, boolean isFirewalled) {
        this(newQueryGUID(isRequery), ttl, minSpeed, query, richQuery, 
             EMPTY_SET, EMPTY_SET, null, isFirewalled);
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
        this(guid, ttl, minSpeed, query, richQuery, 
             requestedUrnTypes, queryUrns, null, isFirewalled,
			 false);
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
    private QueryRequest(byte[] guid, byte ttl, int minSpeed, 
                        String query, String richQuery, boolean isRequery,
                        Set requestedUrnTypes, Set queryUrns,
                        boolean isFirewalled, boolean multicast) {
        this(guid, ttl, minSpeed, query, richQuery,
             requestedUrnTypes, queryUrns, null, isFirewalled,
			 multicast);
    }

	/**
	 * Creates a new requery for the specified SHA1 value and the specified
	 * firewall boolean.
	 *
	 * @param sha1 the <tt>URN</tt> of the file to search for
	 * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument
	 *  is <tt>null</tt>
	 */
	public static QueryRequest createRequery(URN sha1) {
        if(sha1 == null) {
            throw new NullPointerException("null sha1");
        }
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
		return new QueryRequest(newQueryGUID(true), (byte)6, 0, "\\", "", 
								UrnType.SHA1_SET, sha1Set, 
								!RouterService.acceptedIncomingConnection());
	}

	/**
	 * Creates a new requery for the specified SHA1 value and the specified
	 * firewall boolean.
	 *
	 * @param sha1 the <tt>URN</tt> of the file to search for
	 * @param ttl the time to live (ttl) of the query
	 * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the ttl value is
	 *  negative or greater than the maximum allowed value
	 */
	public static QueryRequest createRequery(URN sha1, byte ttl) {
        if(sha1 == null) {
            throw new NullPointerException("null sha1");
        } 
		if(ttl <= 0 || ttl > 7) {
			throw new IllegalArgumentException("invalid TTL: "+ttl);
		}
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
		return new QueryRequest(newQueryGUID(true), ttl, 0, "\\", "", 
								UrnType.SHA1_SET, sha1Set, 
								!RouterService.acceptedIncomingConnection());
	}
	
	/**
	 * Creates a requery for when we don't know the hash of the file --
	 * we don't know the hash.
	 *
	 * @param query the query string
	 * @return a new <tt>QueryRequest</tt> for the specified query
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 */
	public static QueryRequest createRequery(String query) {
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(query.length() == 0) {
			throw new IllegalArgumentException("empty query");
		}
		return new QueryRequest(newQueryGUID(true), (byte)6, 0, query, "",
								UrnType.ANY_TYPE_SET, EMPTY_SET, 
								!RouterService.acceptedIncomingConnection());
	}


	/**
	 * Creates a new query for the specified file name, with no XML.
	 *
	 * @param query the file name to search for
	 * @return a new <tt>QueryRequest</tt> for the specified query
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 */
	public static QueryRequest createQuery(String query) {
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(query.length() == 0) {
			throw new IllegalArgumentException("empty query");
		}
		return new QueryRequest(newQueryGUID(false), (byte)6, 0, query, "", 
                                UrnType.ANY_TYPE_SET, EMPTY_SET, 
								!RouterService.acceptedIncomingConnection());
	}

	/**
	 * Creates a new query for the specified file name, with no XML.
	 *
	 * @param query the file name to search for
	 * @return a new <tt>QueryRequest</tt> for the specified query
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt> or if the <tt>xmlQuery</tt> argument is 
	 *  <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 */
	public static QueryRequest createQuery(String query, String xmlQuery) {
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(xmlQuery == null) {
			throw new NullPointerException("null xml query");
		}
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegalArgumentException("empty query");
		}
		return new QueryRequest(newQueryGUID(false), (byte)6, 0, query, 
								xmlQuery, UrnType.ANY_TYPE_SET, 
								EMPTY_SET, 
								!RouterService.acceptedIncomingConnection());
	}


	/**
	 * Creates a new query for the specified file name, with no XML.
	 *
	 * @param query the file name to search for
	 * @param ttl the time to live (ttl) of the query
	 * @return a new <tt>QueryRequest</tt> for the specified query and ttl
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 * @throws <tt>IllegalArgumentException</tt> if the ttl value is
	 *  negative or greater than the maximum allowed value
	 */
	public static QueryRequest createQuery(String query, byte ttl) {
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(query.length() == 0) {
			throw new IllegalArgumentException("empty query");
		}
		if(ttl <= 0 || ttl > 7) {
			throw new IllegalArgumentException("invalid TTL: "+ttl);
		}
		return new QueryRequest(newQueryGUID(false), ttl, 0, query, "",
								UrnType.ANY_TYPE_SET, EMPTY_SET, 
								!RouterService.acceptedIncomingConnection());
	}

	/**
	 * Creates a new query with the specified guid, query string, and
	 * xml query string.
	 *
	 * @param guid the message GUID for the query
	 * @param query the query string
	 * @param xmlQuery the xml query string
	 * @return a new <tt>QueryRequest</tt> for the specified query, xml
	 *  query, and guid
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>, if the <tt>xmlQuery</tt> argument is <tt>null</tt>,
	 *  or if the <tt>guid</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the guid length is
	 *  not 16 or if both the query strings are empty
	 */
	public static QueryRequest createQuery(byte[] guid, String query, 
										   String xmlQuery) {
		if(guid == null) {
			throw new NullPointerException("null guid");
		}
		if(guid.length != 16) {
			throw new IllegalArgumentException("invalid guid length");
		}
        if(query == null) {
            throw new NullPointerException("null query");
        }
        if(xmlQuery == null) {
            throw new NullPointerException("null xml query");
        }
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegalArgumentException("empty query");
		}
		return new QueryRequest(guid, (byte)6, 0, query, xmlQuery,
								UrnType.ANY_TYPE_SET, EMPTY_SET, 
								!RouterService.acceptedIncomingConnection());
	}

    /**
     * Creates a new query with the specified query key for use in 
     * GUESS-style UDP queries.
     *
     * @param query the query string
     * @param key the query key
	 * @return a new <tt>QueryRequest</tt> instance with the specified 
	 *  query string and query key
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt> or if the <tt>key</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
     */
    public static QueryRequest 
        createQueryKeyQuery(String query, QueryKey key) {
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(query.length() == 0) {
			throw new IllegalArgumentException("empty query");
		}
        if(key == null) {
            throw new NullPointerException("null query key");
        }
        return new QueryRequest(newQueryGUID(false), (byte)1, 0, query, "", 
                                UrnType.ANY_TYPE_SET, EMPTY_SET, key,
                                !RouterService.acceptedIncomingConnection(),
								false);
    }


	/**
	 * Creates a new <tt>QueryRequest</tt> instance for multicast queries.	 
	 * This is necessary due to the unique properties of multicast queries,
	 * such as the firewalled bit not being set regardless of whether or
	 * not the node is truly firewalled/NATted to the world outside the
	 * subnet.
	 * 
	 * @param qr the <tt>QueryRequest</tt> instance containing all the 
	 *  data necessary to create a multicast query
	 * @return a new <tt>QueryRequest</tt> instance with bits set for
	 *  multicast -- a min speed bit in particular
	 * @throws <tt>NullPointerException</tt> if the <tt>qr</tt> argument
	 *  is <tt>null</tt> 
	 */
	public static QueryRequest 
		createMulticastQuery(QueryRequest qr) {
		if(qr == null) {
			throw new NullPointerException("null query");
		}
		return new QueryRequest(qr.getGUID(), (byte)1, 0, qr.getQuery(),
								qr.getRichQuery(), 
								qr.getRequestedUrnTypes(),
								qr.getQueryUrns(), qr.getQueryKey(),
								false, true);
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
	 * @throws <tt>IllegalArgumentException</tt> if the query string, the xml
	 *  query string, and the urns are all empty
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed, 
                        String query, String richQuery,
                        Set requestedUrnTypes, Set queryUrns,
                        QueryKey queryKey, boolean isFirewalled) { 
		this(guid, ttl, minSpeed, query, richQuery, 
			 requestedUrnTypes, queryUrns, queryKey, isFirewalled,
			 false);
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
	 * @throws <tt>IllegalArgumentException</tt> if the query string, the xml
	 *  query string, and the urns are all empty
     */
    private QueryRequest(byte[] guid, byte ttl, int minSpeed, 
						 String query, String richQuery,
						 Set requestedUrnTypes, Set queryUrns,
						 boolean isFirewalled) { 
		this(guid, ttl, minSpeed, query, richQuery, 
			 requestedUrnTypes, queryUrns, null, isFirewalled,
			 false);
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
	 * @throws <tt>IllegalArgumentException</tt> if the query string, the xml
	 *  query string, and the urns are all empty
     */
    private QueryRequest(byte[] guid, byte ttl, int minSpeed, 
                        String query, String richQuery, 
                        Set requestedUrnTypes, Set queryUrns,
                        QueryKey queryKey, boolean isFirewalled, 
						boolean isMulticast) {
        // don't worry about getting the length right at first
        super(guid, Message.F_QUERY, ttl, /* hops */ (byte)0, /* length */ 0);
		if((query == null || query.length() == 0) &&
		   (richQuery == null || richQuery.length() == 0) &&
		   (queryUrns == null || queryUrns.size() == 0)) {
			throw new IllegalArgumentException("cannot create empty query");
		}		

        if (minSpeed == 0) {
            // user has not specified a Min Speed - go ahead and set it for
            // them, as appropriate
            
            // the new Min Speed format - looks reversed but
            // it isn't because of ByteOrder.short2leb
            minSpeed = 0x00000080; 
            // set the firewall bit if i'm firewalled
            if (isFirewalled && !isMulticast)
                minSpeed |= 0x40;
            // LimeWire's ALWAYS want rich results....
            if (true)
                minSpeed |= 0x20;

			// set the multicast bit
			if (isMulticast) 
				minSpeed |= 0x10;
        }
        this.MIN_SPEED=minSpeed;
		if(query == null) {
			this.QUERY = "";
		} else {
			this.QUERY = query;
		}
		if(richQuery == null) {
			this.XML_QUERY = "";
		} else{
			this.XML_QUERY = richQuery;
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

        this.QUERY_KEY = queryKey;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteOrder.short2leb((short)MIN_SPEED,baos); // write minspeed
            baos.write(QUERY.getBytes());              // write query
            baos.write(0);                             // null
            // now write any & all HUGE v0.93 General Extension Mechanism 
			// extensions
            boolean addDelimiterBefore = false;
			
            byte[] richQueryBytes = null;
            if(XML_QUERY!=null)
                richQueryBytes = XML_QUERY.getBytes("UTF-8");
            
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
            if (this.QUERY_KEY != null) {
                // get query key in byte form....
                ByteArrayOutputStream qkBytes = new ByteArrayOutputStream();
                this.QUERY_KEY.write(qkBytes);
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
		PAYLOAD=baos.toByteArray();
		updateLength(PAYLOAD.length); 

		this.QUERY_URNS = Collections.unmodifiableSet(tempQueryUrns);
		this.REQUESTED_URN_TYPES = Collections.unmodifiableSet(tempRequestedUrnTypes);

    }


	public static QueryRequest 
		createNetworkQuery(byte[] guid, byte ttl, byte hops, byte[] payload) 
	    throws BadPacketException {
		return new QueryRequest(guid, ttl, hops, payload);
	}

    /**
     * Build a new query with data snatched from network
     *
     * @param guid the message guid
	 * @param ttl the time to live of the query
	 * @param hops the hops of the query
	 * @param payload the query payload, containing the query string and any
	 *  extension strings
	 * @throws <tt>BadPacketException</tt> if this is not a valid query
     */
    public QueryRequest(byte[] guid, byte ttl, byte hops, byte[] payload) 
		throws BadPacketException {
        super(guid, Message.F_QUERY, ttl, hops, payload.length);
		if(payload == null) {
			this.PAYLOAD = EMPTY_BYTE_ARRAY;
		}
		else {
			this.PAYLOAD=payload;
		}
		String tempQuery = "";
		String tempRichQuery = "";
		int tempMinSpeed = 0;
		Set tempQueryUrns = null;
		Set tempRequestedUrnTypes = null;
        QueryKey tempQueryKey = null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(this.PAYLOAD);
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
		QUERY = tempQuery;
		XML_QUERY = tempRichQuery;
		MIN_SPEED = tempMinSpeed;
		if(tempQueryUrns == null) {
			this.QUERY_URNS = EMPTY_SET; 
		}
		else {
			this.QUERY_URNS = Collections.unmodifiableSet(tempQueryUrns);
		}
		if(tempRequestedUrnTypes == null) {
			this.REQUESTED_URN_TYPES = EMPTY_SET;
		}
		else {
			this.REQUESTED_URN_TYPES =
			    Collections.unmodifiableSet(tempRequestedUrnTypes);
		}	
        QUERY_KEY = tempQueryKey;
		if(QUERY.length() == 0 &&
		   XML_QUERY.length() == 0 &&
		   QUERY_URNS.size() == 0) {
			throw new BadPacketException("empty query");
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
        out.write(PAYLOAD);
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
        return PAYLOAD;
    }

    /** 
     * Returns the query string of this message.<p>
     *
     * The caller should not call the getBytes() method on the returned value,
     * as this seems to cause problems on the Japanese Macintosh.  If you need
     * the raw bytes of the query string, call getQueryByteAt(int).
     */
    public String getQuery() {
        return QUERY;
    }
    
	/**
	 * Returns the rich query string.
	 *
	 * @return the rich query string
	 */
    public String getRichQuery() {
        return XML_QUERY;
    }
 
	/**
	 * Returns the <tt>Set</tt> of URN types requested for this query.
	 *
	 * @return the <tt>Set</tt> of <tt>UrnType</tt> instances requested for this
     * query, which may be empty (not null) if no types were requested
	 */
    public Set getRequestedUrnTypes() {
		return REQUESTED_URN_TYPES;
    }
    
	/**
	 * Returns the <tt>Set</tt> of <tt>URN</tt> instances for this query.
	 *
	 * @return  the <tt>Set</tt> of <tt>URN</tt> instances for this query, which
	 * may be empty (not null) if no URNs were requested
	 */
    public Set getQueryUrns() {
		return QUERY_URNS;
    }
	
	/**
	 * Returns whether or not this query contains URNs.
	 *
	 * @return <tt>true</tt> if this query contains URNs, <tt>false</tt> otherwise
	 */
	public boolean hasQueryUrns() {
		return !QUERY_URNS.isEmpty();
	}

    /**
	 * Note: the minimum speed can be represented as a 2-byte unsigned
	 * number, but Java shorts are signed.  Hence we must use an int.  The
	 * value returned is always smaller than 2^16.
	 */
    public int getMinSpeed() {
        return MIN_SPEED;
    }


    /*    
     * Returns true if the query source is a firewalled servent.
     */
    public boolean isFirewalledSource() {
        if ((MIN_SPEED & 0x0080) > 0) {
            if ((MIN_SPEED & 0x0040) > 0)
                return true;
        }
        return false;
    }
 
 
    /**
     * Returns true if the query source desires Lime meta-data in responses.
     */
    public boolean desiresXMLResponses() {
        if ((MIN_SPEED & 0x0080) > 0) {
            if ((MIN_SPEED & 0x0020) > 0)
                return true;
        }
        return false;        
    }
    
	/**
	 * Accessor for whether or not this query was sent via IP multicast,
	 * and so is on the same subnet, and should follow different firewall 
	 * rules.
	 *
	 * @return <tt>true</tt> if the query was sent via multicast,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isMulticast() {
        if ((MIN_SPEED & 0x0080) > 0) {
            if ((MIN_SPEED & 0x0010) > 0)
                return true;
        }
        return false;		
	}

	/**
	 * Accessor for whether or not this is a requery from a LimeWire.
	 *
	 * @return <tt>true</tt> if it is an automated requery from a LimeWire,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isLimeRequery() {
		return GUID.isLimeRequeryGUID(getGUID());
	}
        
    /**
     * Returns the QueryKey associated with this Request.  May very well be
     * null.  Usually only UDP QueryRequests will have non-null QueryKeys.
     */
    public QueryKey getQueryKey() {
        return QUERY_KEY;
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








