package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.guess.*;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.SchemaNotFoundException;
import java.io.*;
import com.sun.java.util.collections.*;
import java.util.StringTokenizer;
import org.xml.sax.SAXException;

/**
 * This class creates Gnutella query messages, either from scratch, or
 * from data read from the network.  Queries can contain query strings, 
 * XML query strings, URNs, etc.  The minimum speed field is now used
 * for bit flags to indicate such things as the firewalled status of
 * the querier.<p>
 * 
 * This class also has factory constructors for requeries originated
 * from this LimeWire.  These requeries have specially marked GUIDs
 * that allow us to identify them as requeries.
 */
public class QueryRequest extends Message implements Serializable{

    // these specs may seem backwards, but they are not - ByteOrder.short2leb
    // puts the low-order byte first, so over the network 0x0080 would look
    // like 0x8000
    public static final int SPECIAL_MINSPEED_MASK  = 0x0080;
    public static final int SPECIAL_FIREWALL_MASK  = 0x0040;
    public static final int SPECIAL_XML_MASK       = 0x0020;
    public static final int SPECIAL_OUTOFBAND_MASK = 0x0004;

    /**
     * The payload for the query -- includes the query string, the
     * XML query, any URNs, GGEP, etc.
     */
    private final byte[] PAYLOAD;

    /**
     * The "min speed" field.  This was originally used to specify
     * a minimum speed for returned results, but it was never really
     * used this way.  As of LimeWire 3.0 (02/2003), the bits of 
     * this field were changed to specify things like the firewall
     * status of the querier.
     */
    private final int MIN_SPEED;

    /**
     * The query string.
     */
    private final String QUERY;
    
    /**
     * The LimeXMLDocument of the rich query.
     */
    private final LimeXMLDocument XML_DOC;

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
	 * Cached hash code for this instance.
	 */
	private volatile int _hashCode = 0;

	/**
	 * Constant for an empty, unmodifiable <tt>Set</tt>.  This is necessary
	 * because Collections.EMPTY_SET is not serializable in the collections 
	 * 1.1 implementation.
	 */
	private static final Set EMPTY_SET = 
		Collections.unmodifiableSet(new HashSet());

    /**
     * Constant for the default query TTL.
     */
    private static final byte DEFAULT_TTL = 6;

    /**
     * Cached illegal characters in search strings.
     */
    private static final char[] ILLEGAL_CHARS =
        SearchSettings.ILLEGAL_CHARS.getValue();


    /**
     * Cache the maximum length for queries, in bytes.
     */
    private static final int MAX_QUERY_LENGTH =
        SearchSettings.MAX_QUERY_LENGTH.getValue();

    /**
     * Cache the maximum length for XML queries, in bytes.
     */
    private static final int MAX_XML_QUERY_LENGTH =
        SearchSettings.MAX_XML_QUERY_LENGTH.getValue();


	/**
	 * Creates a new requery for the specified SHA1 value.
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
        return new QueryRequest(newQueryGUID(true), DEFAULT_TTL, "\\", "", 
                                UrnType.SHA1_SET, sha1Set, null,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false);

	}

	/**
	 * Creates a new query for the specified SHA1 value.
	 *
	 * @param sha1 the <tt>URN</tt> of the file to search for
	 * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument
	 *  is <tt>null</tt>
	 */
	public static QueryRequest createQuery(URN sha1) {
        if(sha1 == null) {
            throw new NullPointerException("null sha1");
        }
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, "\\", "", 
                                UrnType.SHA1_SET, sha1Set, null,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false);

	}
	/**
	 * Creates a new requery for the specified SHA1 value with file name 
     * thrown in for good measure (or at least until \ works as a query).
	 *
	 * @param sha1 the <tt>URN</tt> of the file to search for
	 * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument
	 *  is <tt>null</tt>
	 */
	public static QueryRequest createRequery(URN sha1, String filename) {
        if(sha1 == null) {
            throw new NullPointerException("null sha1");
        }
        if(filename == null) {
            throw new NullPointerException("null query");
        }
		if(filename.length() == 0) {
			filename = "\\";
		}
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(true), DEFAULT_TTL, filename, "", 
                                UrnType.SHA1_SET, sha1Set, null,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false);

	}

	/**
	 * Creates a new query for the specified SHA1 value with file name 
     * thrown in for good measure (or at least until \ works as a query).
	 *
	 * @param sha1 the <tt>URN</tt> of the file to search for
	 * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument
	 *  is <tt>null</tt>
	 */
	public static QueryRequest createQuery(URN sha1, String filename) {
        if(sha1 == null) {
            throw new NullPointerException("null sha1");
        }
        if(filename == null) {
            throw new NullPointerException("null query");
        }
		if(filename.length() == 0) {
			filename = "\\";
		}
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, filename, "", 
                                UrnType.SHA1_SET, sha1Set, null,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false);

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
		if(ttl <= 0 || ttl > 6) {
			throw new IllegalArgumentException("invalid TTL: "+ttl);
		}
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(true), ttl, "\\", "", 
                                UrnType.SHA1_SET, sha1Set, null,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false);
	}
	
	/**
	 * Creates a new query for the specified UrnType set and URN set.
	 *
	 * @param urnTypeSet the <tt>Set</tt> of <tt>UrnType</tt>s to request.
	 * @param urnSet the <tt>Set</tt> of <tt>URNs</tt>s to request.
	 * @return a new <tt>QueryRequest</tt> for the specied UrnTypes and URNs
	 * @throws <tt>NullPointerException</tt> if either sets are null.
	 */
	public static QueryRequest createQuery(Set urnTypeSet, Set urnSet) {
	    if(urnSet == null)
	        throw new NullPointerException("null urnSet");
	    if(urnTypeSet == null)
	        throw new NullPointerException("null urnTypeSet");
	    return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, "\\", "",
	                            urnTypeSet, urnSet, null,
	                            !RouterService.acceptedIncomingConnection(),
	                            Message.N_UNKNOWN, false);
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
		return new QueryRequest(newQueryGUID(true), query);
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
		return new QueryRequest(newQueryGUID(false), query);
	}


    public static QueryRequest createOutOfBandQuery(String query,
                                                    byte[] ip, int port) {
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(query.length() == 0) {
			throw new IllegalArgumentException("empty query");
		}
        return new QueryRequest(GUID.makeAddressEncodedGuid(ip, port),
                                DEFAULT_TTL, query, "", true);
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
	 *  argument and the xml query are both zero-length (empty)
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
		if(xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) {
			throw new IllegalArgumentException("invalid XML");
		}
		return new QueryRequest(newQueryGUID(false), query, xmlQuery);
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
		if(ttl <= 0 || ttl > 6) {
			throw new IllegalArgumentException("invalid TTL: "+ttl);
		}
		return new QueryRequest(newQueryGUID(false), ttl, query);
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
	 *  not 16, if both the query strings are empty, or if the XML does
	 *  not appear to be valid
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
		if(xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) {
			throw new IllegalArgumentException("invalid XML");
		}
		return new QueryRequest(guid, query, xmlQuery);
	}

	/**
	 * Creates a new query from the existing query with the specified
	 * ttl.
	 *
	 * @param qr the <tt>QueryRequest</tt> to copy
	 * @param ttl the new ttl
	 * @return a new <tt>QueryRequest</tt> with the specified ttl
	 */
	public static QueryRequest createQuery(QueryRequest qr, byte ttl) {
		return new QueryRequest(qr.getGUID(), ttl, qr.getQuery(),
								qr.getRichQueryString(), 
								qr.getRequestedUrnTypes(),
								qr.getQueryUrns(), qr.getQueryKey(),
								qr.isFirewalledSource(),
								qr.getNetwork(), false);
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
        return new QueryRequest(newQueryGUID(false), (byte)1, query, "", 
                                UrnType.ANY_TYPE_SET, EMPTY_SET, key,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false);
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
        QueryRequest mQr =
            new QueryRequest(qr.getGUID(), (byte)1, qr.getQuery(),
								qr.getRichQueryString(), 
								qr.getRequestedUrnTypes(),
								qr.getQueryUrns(), qr.getQueryKey(),
								false, Message.N_MULTICAST, false);
        mQr.setHops(qr.getHops());
        return mQr;
	}

    /** 
	 * Creates a new <tt>QueryRequest</tt> that is a copy of the input 
	 * query, except that it includes the specified query key.
	 *
	 * @param qr the <tt>QueryRequest</tt> to use
	 * @param key the <tt>QueryKey</tt> to add
	 * @return a new <tt>QueryRequest</tt> from the specified query and
	 *  key
     */
	public static QueryRequest 
		createQueryKeyQuery(QueryRequest qr, QueryKey key) {
        return new QueryRequest(qr.getGUID(), qr.getTTL(), 
                                qr.getQuery(), qr.getRichQueryString(), 
                                qr.getRequestedUrnTypes(), qr.getQueryUrns(),
                                key, qr.isFirewalledSource(), Message.N_UNKNOWN,
                                false);
	}

	/**
	 * Creates a new specialized <tt>QueryRequest</tt> instance for 
	 * browse host queries so that <tt>FileManager</tt> can understand them.
	 *
	 * @return a new <tt>QueryRequest</tt> for browse host queries
	 */
	public static QueryRequest createBrowseHostQuery() {
        return new QueryRequest(newQueryGUID(false), (byte)1, 
								FileManager.INDEXING_QUERY, "", 
                                UrnType.ANY_TYPE_SET, EMPTY_SET, null,
                                false, Message.N_UNKNOWN, false);
	}

	/**
	 * Specialized constructor used to create a query without the firewalled
	 * bit set.  This should primarily be used for testing.
	 *
	 * @param query the query string
	 * @return a new <tt>QueryRequest</tt> with the specified query string
	 *  and without the firewalled bit set
	 */
	public static QueryRequest 
		createNonFirewalledQuery(String query, byte ttl) {
		return new QueryRequest(newQueryGUID(false), ttl, 
								query, "", 
                                UrnType.ANY_TYPE_SET, EMPTY_SET, null,
                                false, Message.N_UNKNOWN, false);
	}



	/**
	 * Creates a new query from the network.
	 *
	 * @param guid the GUID of the query
	 * @param ttl the time to live of the query
	 * @param hops the hops of the query
	 * @param payload the query payload
	 *
	 * @return a new <tt>QueryRequest</tt> instance from the specified data
	 */
	public static QueryRequest 
		createNetworkQuery(byte[] guid, byte ttl, byte hops, byte[] payload, int network) 
	    throws BadPacketException {
		return new QueryRequest(guid, ttl, hops, payload, network);
	}

    /**
     * Builds a new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.  GUID must have
     * been created via newQueryGUID; this allows the caller to match up results
     */
    private QueryRequest(byte[] guid, String query) {
        this(guid, query, "");
    }

    /**
     * Builds a new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.  GUID must have
     * been created via newQueryGUID; this allows the caller to match up results
     */
    private QueryRequest(byte[] guid, byte ttl, String query) {
        this(guid, ttl, query, "");
    }

    /**
     * Builds a new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.  GUID must have
     * been created via newQueryGUID; this allows the caller to match up results
     */
    private QueryRequest(byte[] guid, String query, String xmlQuery) {
        this(guid, DEFAULT_TTL, query, xmlQuery);
    }

    /**
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.  GUID must have
     * been created via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes) 
     */
    private QueryRequest(byte[] guid, byte ttl, String query, String richQuery) {
        this(guid, ttl, query, richQuery, UrnType.ANY_TYPE_SET, EMPTY_SET, null,
			 !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, false);
    }

    /**
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.  GUID must have
     * been created via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes) 
     */
    private QueryRequest(byte[] guid, byte ttl, String query, String richQuery,
                         boolean canReceiveOutOfBandReplies) {
        this(guid, ttl, query, richQuery, UrnType.ANY_TYPE_SET, EMPTY_SET, null,
			 !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, 
             canReceiveOutOfBandReplies);
    }

    /**
     * Builds a new query from scratch but you can flag it as a Requery, if 
     * needed.  If you need to make a query that accepts out-of-band results, 
     * be sure to set the guid correctly (see GUID.makeAddressEncodedGUI) and 
     * set canReceiveOutOfBandReplies .
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
    public QueryRequest(byte[] guid, byte ttl,  
                        String query, String richQuery, 
                        Set requestedUrnTypes, Set queryUrns,
                        QueryKey queryKey, boolean isFirewalled, 
                        int network, boolean canReceiveOutOfBandReplies) {
        // don't worry about getting the length right at first
        super(guid, Message.F_QUERY, ttl, /* hops */ (byte)0, /* length */ 0, network);
		if((query == null || query.length() == 0) &&
		   (richQuery == null || richQuery.length() == 0) &&
		   (queryUrns == null || queryUrns.size() == 0)) {
			throw new IllegalArgumentException("cannot create empty query");
		}		


            
		// the new Min Speed format - looks reversed but
		// it isn't because of ByteOrder.short2leb
		int minSpeed = SPECIAL_MINSPEED_MASK; 
		// set the firewall bit if i'm firewalled
		if (isFirewalled && !isMulticast())
			minSpeed |= SPECIAL_FIREWALL_MASK;
        // THE DEAL:
        // if we can NOT receive out of band replies, we want in-band XML - so
		// set the correct bit.
        // if we can receive out of band replies, we do not want in-band XML -
		// we'll hope the out-of-band reply guys will provide us all necessary XML.
        if (!canReceiveOutOfBandReplies) 
            minSpeed |= SPECIAL_XML_MASK;
        else // bit 10 flags out-of-band support
            minSpeed |= SPECIAL_OUTOFBAND_MASK;

        MIN_SPEED = minSpeed;
		if(query == null) {
			this.QUERY = "";
		} else {
			this.QUERY = query;
		}
		if(richQuery == null || richQuery.equals("") ) {
			this.XML_DOC = null;
		} else {
		    LimeXMLDocument doc = null;
		    try {
		        doc = new LimeXMLDocument(richQuery);
            } catch(SAXException ignored) {
            } catch(SchemaNotFoundException ignored) {
            } catch(IOException ignored) {
            }
            this.XML_DOC = doc;
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

			// this specifies whether or not the extension was successfully
			// written, meaning that the HUGE GEM delimiter should be
			// written before the next extension
            boolean addDelimiterBefore = false;
			
            byte[] richQueryBytes = null;
            if(XML_DOC != null) {
                richQueryBytes = richQuery.getBytes("UTF-8");
			}
            
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
		    ErrorService.error(e);
		}

		PAYLOAD = baos.toByteArray();
		updateLength(PAYLOAD.length);

		this.QUERY_URNS = Collections.unmodifiableSet(tempQueryUrns);
		this.REQUESTED_URN_TYPES = Collections.unmodifiableSet(tempRequestedUrnTypes);

    }


    /**
     * Build a new query with data snatched from network
     *
     * @param guid the message guid
	 * @param ttl the time to live of the query
	 * @param hops the hops of the query
	 * @param payload the query payload, containing the query string and any
	 *  extension strings
	 * @param network the network that this query came from.
	 * @throws <tt>BadPacketException</tt> if this is not a valid query
     */
    private QueryRequest(
      byte[] guid, byte ttl, byte hops, byte[] payload, int network) 
		throws BadPacketException {
        super(guid, Message.F_QUERY, ttl, hops, payload.length, network);
		if(payload == null) {
			throw new BadPacketException("no payload");
		}
		PAYLOAD=payload;
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
                        // another GEM extension
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
                        else if (curExtStr.startsWith("<?xml")) {
                            // rich query
                            tempRichQuery = curExtStr;
						}
                    }
                    currIndex = delimIndex+1;
                }
            }
        } catch (IOException ioe) {
            ErrorService.error(ioe);
        }
		QUERY = tempQuery;
	    LimeXMLDocument tempDoc = null;
	    try {
	        tempDoc = new LimeXMLDocument(tempRichQuery);
        } catch(SAXException ignored) {
        } catch(SchemaNotFoundException ignored) {
        } catch(IOException ignored) {
        }
        this.XML_DOC = tempDoc;
		MIN_SPEED = tempMinSpeed;
		if(tempQueryUrns == null) {
			QUERY_URNS = EMPTY_SET; 
		}
		else {
			QUERY_URNS = Collections.unmodifiableSet(tempQueryUrns);
		}
		if(tempRequestedUrnTypes == null) {
			REQUESTED_URN_TYPES = EMPTY_SET;
		}
		else {
			REQUESTED_URN_TYPES =
			    Collections.unmodifiableSet(tempRequestedUrnTypes);
		}	
        QUERY_KEY = tempQueryKey;
		if(QUERY.length() == 0 &&
		   tempRichQuery.length() == 0 &&
		   QUERY_URNS.size() == 0) {
		    if( RECORD_STATS )
		        ReceivedErrorStat.QUERY_EMPTY.incrementStat();
			throw new BadPacketException("empty query");
		}       
        if(QUERY_URNS.size() != 0) {
            if( RECORD_STATS )
                ReceivedErrorStat.QUERY_URN.incrementStat();
            throw BadPacketException.CANNOT_ACCEPT_URN_QUERIES;
        }
        if(QUERY.length() > MAX_QUERY_LENGTH) {
            if( RECORD_STATS )
                ReceivedErrorStat.QUERY_TOO_LARGE.incrementStat();
            throw BadPacketException.QUERY_TOO_BIG;
        }        

        if(tempRichQuery.length() > MAX_XML_QUERY_LENGTH) {
            if( RECORD_STATS )
                ReceivedErrorStat.QUERY_XML_TOO_LARGE.incrementStat();
            throw BadPacketException.XML_QUERY_TOO_BIG;
        }

        if(hasIllegalChars(QUERY)) {
            if( RECORD_STATS )
                ReceivedErrorStat.QUERY_ILLEGAL_CHARS.incrementStat();
            throw BadPacketException.ILLEGAL_CHAR_IN_QUERY;
        }
    }

    /**
     * Utility method for checking whether or not the query string contains
     * illegal characters.
     *
     * @param query the query string to check
     */
    private static boolean hasIllegalChars(String query) {
        char[] chars = query.toCharArray();
        Arrays.sort(chars);
        for(int i=0; i<ILLEGAL_CHARS.length; i++) {
            if(Arrays.binarySearch(chars, ILLEGAL_CHARS[i]) >= 0) return true;
        }
        return false;
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
	 * Returns the rich query LimeXMLDocument.
	 *
	 * @return the rich query LimeXMLDocument
	 */
    public LimeXMLDocument getRichQuery() {
        return XML_DOC;
    }
    
    /**
     * Helper method used internally for getting the rich query string.
     */
    private String getRichQueryString() {
        if( XML_DOC == null )
            return null;
        else {
            try {
                return XML_DOC.getXMLString();
            } catch(SchemaNotFoundException snfe) {
                return null;
            }
        }
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


    /**
     * Returns true if the query source is a firewalled servent.
     */
    public boolean isFirewalledSource() {
        if ( !isMulticast() ) {
            if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
                if ((MIN_SPEED & SPECIAL_FIREWALL_MASK) > 0)
                    return true;
            }
        }
        return false;
    }
 
 
    /**
     * Returns true if the query source desires Lime meta-data in responses.
     */
    public boolean desiresXMLResponses() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_XML_MASK) > 0)
                return true;
        }
        return false;        
    }


    /**
     * Returns true if the query source can accept out-of-band replies.  Use
     * getReplyAddress() and getReplyPort() if this is true to know where to
     * it.  Always send XML if you are sending an out-of-band reply.
     */
    public boolean desiresOutOfBandReplies() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_OUTOFBAND_MASK) > 0)
                return true;
        }
        return false;
    }

    /** Returns the address to send a out-of-band reply to.  Only useful
     *  when desiresOutOfBandReplies() == true.
     */
    public String getReplyAddress() {
        return (new GUID(getGUID())).getIP();
    }

        
    /** Returns the port to send a out-of-band reply to.  Only useful
     *  when desiresOutOfBandReplies() == true.
     */
    public int getReplyPort() {
        return (new GUID(getGUID())).getPort();
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

	public int hashCode() {
		if(_hashCode == 0) {
			int result = 17;
			result = (37*result) + QUERY.hashCode();
			if( XML_DOC != null )
			    result = (37*result) + XML_DOC.hashCode();
			result = (37*result) + REQUESTED_URN_TYPES.hashCode();
			result = (37*result) + QUERY_URNS.hashCode();
			if(QUERY_KEY != null) {
				result = (37*result) + QUERY_KEY.hashCode();
			}
			// TODO:: ADD GUID!!
			_hashCode = result;
		}
		return _hashCode;
	}

	// overrides Object.toString
	public boolean equals(Object o) {
		if(o == this) return true;
		if(!(o instanceof QueryRequest)) return false;
		QueryRequest qr = (QueryRequest)o;
		return (MIN_SPEED == qr.MIN_SPEED &&
				QUERY.equals(qr.QUERY) &&
				(XML_DOC == null ? qr.XML_DOC == null : 
				    XML_DOC.equals(qr.XML_DOC)) &&
				REQUESTED_URN_TYPES.equals(qr.REQUESTED_URN_TYPES) &&
				QUERY_URNS.equals(qr.QUERY_URNS) &&
				Arrays.equals(getGUID(), qr.getGUID()) &&
				Arrays.equals(PAYLOAD, qr.PAYLOAD));
	}


    public String toString() {
 		return "<query: \""+getQuery()+"\", "+
            "ttl: "+getTTL()+", "+
            "hops: "+getHops()+", "+            
            "meta: \""+getRichQueryString()+"\", "+
            "types: "+getRequestedUrnTypes().size()+","+
            "urns: "+getQueryUrns().size()+">";
    }
}








