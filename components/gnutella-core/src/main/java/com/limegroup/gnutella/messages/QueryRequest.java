package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.limewire.security.QueryKey;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;
import org.limewire.util.I18NConvert;
import org.limewire.util.OSUtils;
import org.limewire.util.StringUtils;
import org.xml.sax.SAXException;

import sun.nio.cs.ext.ISCII91;

import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.messages.HUGEExtension.GGEPBlock;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.SchemaNotFoundException;

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
    public static final int SPECIAL_FWTRANS_MASK   = 0x0002;

    /** Mask for audio queries - input 0 | AUDIO_MASK | .... to specify
     *  audio responses.
     */
    public static final int AUDIO_MASK  = 0x0004;
    /** Mask for video queries - input 0 | VIDEO_MASK | .... to specify
     *  video responses.
     */
    public static final int VIDEO_MASK  = 0x0008; 
    /** Mask for document queries - input 0 | DOC_MASK | .... to specify
     *  document responses.
     */
    public static final int DOC_MASK  = 0x0010;
    /** Mask for image queries - input 0 | IMAGE_MASK | .... to specify
     *  image responses.
     */
    public static final int IMAGE_MASK  = 0x0020;
    /** Mask for windows programs/packages queries - input 0 | WIN_PROG_MASK
     *  | .... to specify windows programs/packages responses.
     */
    public static final int WIN_PROG_MASK  = 0x0040;
    /** Mask for linux/osx programs/packages queries - input 0 | LIN_PROG_MASK
     *  | .... to specify linux/osx programs/packages responses.
     */
    public static final int LIN_PROG_MASK  = 0x0080;

    public static final String WHAT_IS_NEW_QUERY_STRING = "WhatIsNewXOXO";

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

    /**
     * The feature that this query is.
     */
    private int _featureSelector = 0;
    
    private boolean _isSecurityTokenRequired;

    /**
     * Whether or not the GGEP header for Do Not Proxy was found. Defaults
     * to true in new clients and should only be set differently from
     * network constructor.
     */
    private boolean _doNotProxyV2 = true;
    /**
     * Whether or not the GGEP header for Do Not Proxy for protocol version 3
     * was found.
     */
    private boolean _doNotProxyV3 = false;

    private boolean _isPayloadModifiable = true;
    
    // HUGE v0.93 fields
    /** 
	 * The types of requested URNs.
	 */
    private final Set<URN.Type> REQUESTED_URN_TYPES;

    /** 
	 * Specific URNs requested.
	 */
    private final Set<URN> QUERY_URNS;

    /**
     * The Query Key associated with this query -- can be null.
     */
    private final QueryKey QUERY_KEY;

    /**
     * The flag in the 'M' GGEP extension - if non-null, the query is requesting
     * only certain types.
     */
    private Integer _metaMask = null;
    
    /**
     * If we're re-originated this query for a leaf.  This can be set/read
     * after creation.
     */
    private boolean originated = false;

	/**
	 * Cached hash code for this instance.
	 */
	private volatile int _hashCode = 0;

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
     * The meaningless query string we put in URN queries.  Needed because
     * LimeWire's drop empty queries....
     */
    private static final String DEFAULT_URN_QUERY = "\\";

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
		Set<URN> sha1Set = new UrnSet(sha1);
        return new QueryRequest(newQueryGUID(true), DEFAULT_TTL, 
                                DEFAULT_URN_QUERY, "", 
                                URN.Type.SHA1_SET, sha1Set, null,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);

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
		Set<URN> sha1Set = new UrnSet(sha1);
        return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, 
                                DEFAULT_URN_QUERY, "",  URN.Type.SHA1_SET, 
                                sha1Set, null,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);

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
			filename = DEFAULT_URN_QUERY;
		}
		Set<URN> sha1Set = new UrnSet(sha1);
        return new QueryRequest(newQueryGUID(true), DEFAULT_TTL, filename, "", 
                                URN.Type.SHA1_SET, sha1Set, null,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);

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
			filename = DEFAULT_URN_QUERY;
		}
		Set<URN> sha1Set = new UrnSet(sha1);
        return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, filename, "", 
                                URN.Type.SHA1_SET, sha1Set, null,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);

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
		Set<URN> sha1Set = new UrnSet(sha1);
        return new QueryRequest(newQueryGUID(true), ttl, DEFAULT_URN_QUERY, "", 
                                URN.Type.SHA1_SET, sha1Set, null,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);
	}
	
	/**
	 * Creates a new query for the specified UrnType set and URN set.
	 *
	 * @param urnTypeSet the <tt>Set</tt> of <tt>UrnType</tt>s to request.
	 * @param urnSet the <tt>Set</tt> of <tt>URNs</tt>s to request.
	 * @return a new <tt>QueryRequest</tt> for the specied UrnTypes and URNs
	 * @throws <tt>NullPointerException</tt> if either sets are null.
	 */
	public static QueryRequest createQuery(Set<URN.Type> urnTypeSet, Set<? extends URN> urnSet) {
	    if(urnSet == null)
	        throw new NullPointerException("null urnSet");
	    if(urnTypeSet == null)
	        throw new NullPointerException("null urnTypeSet");
	    return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, 
                                DEFAULT_URN_QUERY, "",
	                            urnTypeSet, urnSet, null,
	                            !RouterService.acceptedIncomingConnection(),
	                            Message.N_UNKNOWN, false, 0, false, 0);
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
    

	/**
	 * Creates a new query for the specified file name and the designated XML.
	 *
	 * @param query the file name to search for
     * @param guid I trust that this is a address encoded guid.  Your loss if
     * it isn't....
	 * @return a new <tt>QueryRequest</tt> for the specified query that has
	 * encoded the input ip and port into the GUID and appropriate marked the
	 * query to signify out of band support.
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 */
    public static QueryRequest createOutOfBandQuery(byte[] guid, String query, 
                                                    String xmlQuery) {
        query = I18NConvert.instance().getNorm(query);
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
        return new QueryRequest(guid, DEFAULT_TTL, query, xmlQuery, true);
    }                                

	/**
	 * Creates a new query for the specified file name and the designated XML.
	 *
	 * @param query the file name to search for
     * @param guid I trust that this is a address encoded guid.  Your loss if
     * it isn't....
     * @param type can be null - the type of results you want.
	 * @return a new <tt>QueryRequest</tt> for the specified query that has
	 * encoded the input ip and port into the GUID and appropriate marked the
	 * query to signify out of band support AND specifies a file type category.
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 */
    public static QueryRequest createOutOfBandQuery(byte[] guid, String query, 
                                                    String xmlQuery,
                                                    MediaType type) {
        query = I18NConvert.instance().getNorm(query);
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
        return new QueryRequest(guid, DEFAULT_TTL, query, xmlQuery, true, type);
    }                                

	/**
	 * Creates a new query for the specified file name, with no XML.
	 *
	 * @param query the file name to search for
	 * @return a new <tt>QueryRequest</tt> for the specified query that has
	 * encoded the input ip and port into the GUID and appropriate marked the
	 * query to signify out of band support.
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 */
    public static QueryRequest createOutOfBandQuery(String query,
                                                    byte[] ip, int port) {
        byte[] guid = GUID.makeAddressEncodedGuid(ip, port);
        return QueryRequest.createOutOfBandQuery(guid, query, "");
    }                                

    /**
     * Creates a new 'What is new'? query with the specified guid and ttl.
     * @param ttl the desired ttl of the query.
     * @param guid the desired guid of the query.
     */
    public static QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl) {
        return createWhatIsNewQuery(guid, ttl, null);
    }
   

    /**
     * Creates a new 'What is new'? query with the specified guid and ttl.
     * @param ttl the desired ttl of the query.
     * @param guid the desired guid of the query.
     */
    public static QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl,
                                                    MediaType type) {
        if (ttl < 1) throw new IllegalArgumentException("Bad TTL.");
        return new QueryRequest(guid, ttl, WHAT_IS_NEW_QUERY_STRING,
                                "", null, null, null,
                                !RouterService.acceptedIncomingConnection(),
                                Message.N_UNKNOWN, false, 
                                FeatureSearchData.WHAT_IS_NEW, false, 
                                getMetaFlag(type));
    }
   

    /**
     * Creates a new 'What is new'? OOB query with the specified guid and ttl.
     * @param ttl the desired ttl of the query.
     * @param guid the desired guid of the query.
     */
    public static QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl) {
        return createWhatIsNewOOBQuery(guid, ttl, null);
    }
   

    /**
     * Creates a new 'What is new'? OOB query with the specified guid and ttl.
     * @param ttl the desired ttl of the query.
     * @param guid the desired guid of the query.
     */
    public static QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl,
                                                       MediaType type) {
        if (ttl < 1) throw new IllegalArgumentException("Bad TTL.");
        return new QueryRequest(guid, ttl, WHAT_IS_NEW_QUERY_STRING,
                                "", null, null, null,
                                !RouterService.acceptedIncomingConnection(),
                                Message.N_UNKNOWN, true, FeatureSearchData.WHAT_IS_NEW,
                                false, getMetaFlag(type));
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
        query = I18NConvert.instance().getNorm(query);
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
										   String xmlQuery, MediaType type) {
        query = I18NConvert.instance().getNorm(query);
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
		return new QueryRequest(guid, DEFAULT_TTL, query, xmlQuery, type);
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
	    // Construct a query request that is EXACTLY like the other query,
	    // but with a different TTL.
	    try {
	        return createNetworkQuery(qr.getGUID(), ttl, qr.getHops(),
	                                  qr.PAYLOAD, qr.getNetwork());
	    } catch(BadPacketException ioe) {
	        throw new IllegalArgumentException(ioe.getMessage());
	    }
	}

	/**
	 * Creates a new OOBquery from the existing query with the specified guid
     * (which should be address encoded).
	 *
	 * @param qr the <tt>QueryRequest</tt> to copy
	 * @return a new <tt>QueryRequest</tt> with the specified guid that is now
     * OOB marked.
     * @throws IllegalArgumentException thrown if guid is not right size of if
     * query is bad.
	 */
	public static QueryRequest createProxyQuery(QueryRequest qr, byte[] guid) {
        if (guid.length != 16)
            throw new IllegalArgumentException("bad guid size: " + guid.length);
        
        if (!qr.isPayloadModifiable()) {
            throw new IllegalArgumentException("payload is not modifiable " + qr);
        }

        // i can't just call a new constructor, since there might be stuff in
        // the payload we don't understand and would get lost
        byte[] newPayload = new byte[qr.PAYLOAD.length];
        System.arraycopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);
        // disable old out of bounds if there
        newPayload[0] &= ~SPECIAL_OUTOFBAND_MASK;
        GGEP ggep = new GGEP(false);
        // disable proxying for old protocol version
        ggep.put(GGEP.GGEP_HEADER_NO_PROXY);
        // signal oob capability
        ggep.put(GGEP.GGEP_HEADER_SECURE_OOB);
        
        try {
            newPayload = patchInGGEP(newPayload, ggep);
            return createNetworkQuery(guid, qr.getTTL(), qr.getHops(), 
                    newPayload, qr.getNetwork());
        } catch (BadPacketException ioe) {
            throw new IllegalArgumentException(ioe.getMessage());
        }
	}
    
	/**
	 * Creates a new query from the existing query and loses the OOB marking.
	 *
	 * @param qr the <tt>QueryRequest</tt> to copy
	 * @return a new <tt>QueryRequest</tt> with no OOB marking
	 */
	public static QueryRequest unmarkOOBQuery(QueryRequest qr) {
        if (!qr.isPayloadModifiable()) {
            throw new IllegalArgumentException("payload is not modifiable " + qr);
        }
        
        //modify the payload to not be OOB.
        byte[] newPayload = new byte[qr.PAYLOAD.length];
        System.arraycopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);
        newPayload[0] &= ~SPECIAL_OUTOFBAND_MASK;
        newPayload[0] |= SPECIAL_XML_MASK;
        
        // TODO fberger
        GGEP ggep = new GGEP(false);
        // disable proxying for old protocol version
        ggep.put(GGEP.GGEP_HEADER_NO_PROXY);
        // signal oob capability
        ggep.put(GGEP.GGEP_HEADER_SECURE_OOB);
        
        
        try {
            return createNetworkQuery(qr.getGUID(), qr.getTTL(), qr.getHops(), 
                                      newPayload, qr.getNetwork());
        } catch (BadPacketException ioe) {
            throw new IllegalArgumentException(ioe.getMessage());
        }
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
                                URN.Type.ANY_TYPE_SET, URN.NO_URN_SET, key,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);
    }


    /**
     * Creates a new query with the specified query key for use in 
     * GUESS-style UDP queries.
     *
     * @param sha1 the URN
     * @param key the query key
	 * @return a new <tt>QueryRequest</tt> instance with the specified 
	 *  URN request and query key
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt> or if the <tt>key</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
     */
    public static QueryRequest 
        createQueryKeyQuery(URN sha1, QueryKey key) {
        if(sha1 == null) {
            throw new NullPointerException("null sha1");
        }
        if(key == null) {
            throw new NullPointerException("null query key");
        }
		Set<URN> sha1Set = new UrnSet(sha1);
        return new QueryRequest(newQueryGUID(false), (byte) 1, DEFAULT_URN_QUERY,
                                "", URN.Type.SHA1_SET, sha1Set, key,
                                !RouterService.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);
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
	public static QueryRequest createMulticastQuery(byte[] guid, QueryRequest qr) {
		if(qr == null)
			throw new NullPointerException("null query");

        //modify the payload to not be OOB.
        byte[] newPayload = new byte[qr.PAYLOAD.length];
        System.arraycopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);
        newPayload[0] &= ~SPECIAL_OUTOFBAND_MASK;
        newPayload[0] |= SPECIAL_XML_MASK;
        
        try {
            return createNetworkQuery(guid, (byte)1, qr.getHops(),
                                      newPayload, Message.N_MULTICAST);
        } catch (BadPacketException ioe) {
            throw new IllegalArgumentException(ioe.getMessage());
        }
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
		    
        // TODO: Copy the payload verbatim, except add the query-key
        //       into the GGEP section.
        return new QueryRequest(qr.getGUID(), qr.getTTL(), 
                                qr.getQuery(), qr.getRichQueryString(), 
                                qr.getRequestedUrnTypes(), qr.getQueryUrns(),
                                key, qr.isFirewalledSource(), Message.N_UNKNOWN,
                                qr.desiresOutOfBandReplies(),
                                qr.getFeatureSelector(), false,
                                qr.getMetaMask());
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
                URN.Type.ANY_TYPE_SET, URN.NO_URN_SET, null,
                !RouterService.acceptedIncomingConnection(), 
				Message.N_UNKNOWN, false, 0, false, 0, false);
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
                                URN.Type.ANY_TYPE_SET, URN.NO_URN_SET, null,
                                false, Message.N_UNKNOWN, false, 0, false, 0);
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
        this(guid, ttl, query, richQuery, URN.Type.ANY_TYPE_SET, URN.NO_URN_SET, null,
			 !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN,
             false, 0, false, 0);
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
                         MediaType type) {
        this(guid, ttl, query, richQuery, URN.Type.ANY_TYPE_SET, URN.NO_URN_SET, null,
			 !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN,
             false, 0, false, getMetaFlag(type));
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
        this(guid, ttl, query, richQuery, URN.Type.ANY_TYPE_SET, URN.NO_URN_SET, null,
			 !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, 
             canReceiveOutOfBandReplies, 0, false, 0);
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
                         boolean canReceiveOutOfBandReplies, MediaType type) {
        this(guid, ttl, query, richQuery, URN.Type.ANY_TYPE_SET, URN.NO_URN_SET, null,
			 !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, 
             canReceiveOutOfBandReplies, 0, false, getMetaFlag(type));
    }
 
    private static int getMetaFlag(MediaType type) {
        int metaFlag = 0;
        if (type == null)
            ;
        else if (type.getDescriptionKey() == MediaType.AUDIO)
            metaFlag |= AUDIO_MASK;
        else if (type.getDescriptionKey() == MediaType.VIDEO)
            metaFlag |= VIDEO_MASK;
        else if (type.getDescriptionKey() == MediaType.IMAGES)
            metaFlag |= IMAGE_MASK;
        else if (type.getDescriptionKey() == MediaType.DOCUMENTS)
            metaFlag |= DOC_MASK;
        else if (type.getDescriptionKey() == MediaType.PROGRAMS) {
            if (OSUtils.isLinux() || OSUtils.isAnyMac())
                metaFlag |= LIN_PROG_MASK;
            else if (OSUtils.isWindows())
                metaFlag |= WIN_PROG_MASK;
            else // Other OS, search any type of programs
                metaFlag |= (LIN_PROG_MASK|WIN_PROG_MASK);
        }
        return metaFlag;
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
	 *  query string, and the urns are all empty, or if the feature selector
     *  is bad
     */
    public QueryRequest(byte[] guid, byte ttl,  
                        String query, String richQuery, 
                        Set<URN.Type> requestedUrnTypes,
                        Set<? extends URN> queryUrns,
                        QueryKey queryKey, boolean isFirewalled, 
                        int network, boolean canReceiveOutOfBandReplies,
                        int featureSelector) {
        // calls me with the doNotProxy flag set to false
        this(guid, ttl, query, richQuery, requestedUrnTypes, queryUrns,
             queryKey, isFirewalled, network, canReceiveOutOfBandReplies,
             featureSelector, false, 0);
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
	 *  query string, and the urns are all empty, or if the feature selector
     *  is bad
     */
    public QueryRequest(byte[] guid, byte ttl,  
                        String query, String richQuery, 
                        Set<URN.Type> requestedUrnTypes,
                        Set<? extends URN> queryUrns,
                        QueryKey queryKey, boolean isFirewalled, 
                        int network, boolean canReceiveOutOfBandReplies,
                        int featureSelector, boolean doNotProxy,
                        int metaFlagMask) {
        this(guid, ttl, 0, query, richQuery, requestedUrnTypes, queryUrns,
             queryKey, isFirewalled, network, canReceiveOutOfBandReplies,
             featureSelector, doNotProxy, metaFlagMask, true);
    }
    
    /**
     * Constructs a query with an optional 'normalize' parameter, which if false,
     * does not normalize the query string.
     */
    private QueryRequest(byte[] guid, byte ttl,  
                        String query, String richQuery, 
                        Set<URN.Type> requestedUrnTypes,
                        Set<? extends URN> queryUrns,
                        QueryKey queryKey, boolean isFirewalled, 
                        int network, boolean canReceiveOutOfBandReplies,
                        int featureSelector, boolean doNotProxy,
                        int metaFlagMask,
                        boolean normalize) {
        this(guid, ttl, 0, query, richQuery, requestedUrnTypes, queryUrns,
             queryKey, isFirewalled, network, canReceiveOutOfBandReplies,
             featureSelector, doNotProxy, metaFlagMask, normalize);
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
	 *  query string, and the urns are all empty, or if the capability selector
     *  is bad
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed,
                        String query, String richQuery, 
                        Set<URN.Type> requestedUrnTypes,
                        Set<? extends URN> queryUrns,
                        QueryKey queryKey, boolean isFirewalled, 
                        int network, boolean canReceiveOutOfBandReplies,
                        int featureSelector, boolean doNotProxy,
                        int metaFlagMask) {
        this(guid, ttl, minSpeed, query, richQuery, requestedUrnTypes,
             queryUrns, queryKey, isFirewalled, network, canReceiveOutOfBandReplies,
             featureSelector, doNotProxy, metaFlagMask, true);
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
     *  query string, and the urns are all empty, or if the capability selector
     *  is bad
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed,
                        String query, String richQuery, 
                        Set<URN.Type> requestedUrnTypes,
                        Set<? extends URN> queryUrns,
                        QueryKey queryKey, boolean isFirewalled, 
                        int network, boolean canReceiveOutOfBandReplies,
                        int featureSelector, boolean doNotProxy,
                        int metaFlagMask, boolean normalize) {
        // don't worry about getting the length right at first
        super(guid, Message.F_QUERY, ttl, /* hops */ (byte)0, /* length */ 0, 
              network);
        
        // make sure the query is normalized.
        // (this may have been normalized elsewhere, but it's okay to do it again)
        if(normalize && query != null)
            query = I18NConvert.instance().getNorm(query);
        
		if((query == null || query.length() == 0) &&
		   (richQuery == null || richQuery.length() == 0) &&
		   (queryUrns == null || queryUrns.size() == 0)) {
			throw new IllegalArgumentException("cannot create empty query");
		}		

        if(query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("query too big: " + query);
        }        

        if(richQuery != null && richQuery.length() > MAX_XML_QUERY_LENGTH) {
            throw new IllegalArgumentException("xml too big: " + richQuery);
        }

        if(query != null && 
          !(queryUrns != null && queryUrns.size() > 0 &&
            query.equals(DEFAULT_URN_QUERY))
           && hasIllegalChars(query)) {
            throw new IllegalArgumentException("illegal chars: " + query);
        }

        if (featureSelector < 0)
            throw new IllegalArgumentException("Bad feature = " +
                                               featureSelector);
        _featureSelector = featureSelector;
        if ((metaFlagMask > 0) && (metaFlagMask < 4) || (metaFlagMask > 248))
            throw new IllegalArgumentException("Bad Meta Flag = " +
                                               metaFlagMask);
        if (metaFlagMask > 0)
            _metaMask = new Integer(metaFlagMask);

        // only set the minspeed if none was input...x
        if (minSpeed == 0) {
            // the new Min Speed format - looks reversed but
            // it isn't because of ByteOrder.short2leb
            minSpeed = SPECIAL_MINSPEED_MASK; 
            // set the firewall bit if i'm firewalled
            if (isFirewalled && !isMulticast())
                minSpeed |= SPECIAL_FIREWALL_MASK;
            // if i'm firewalled and can do solicited, mark the query for fw
            // transfer capability.
            if (isFirewalled && UDPService.instance().canDoFWT())
                minSpeed |= SPECIAL_FWTRANS_MASK;
            // THE DEAL:
            // if we can NOT receive out of band replies, we want in-band XML -
            // so set the correct bit.
            // if we can receive out of band replies, we do not want in-band XML
            // we'll hope the out-of-band reply guys will provide us all
            // necessary XML.
            
            if (!canReceiveOutOfBandReplies) 
                minSpeed |= SPECIAL_XML_MASK;
            // don't mark old oob flag anymore
        }

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
		Set<URN.Type> tempRequestedUrnTypes = null;
		Set<URN> tempQueryUrns = null;
		if(requestedUrnTypes != null) {
			tempRequestedUrnTypes = EnumSet.copyOf(requestedUrnTypes);
		} else {
			tempRequestedUrnTypes = URN.Type.NO_TYPE_SET;
		}
		
		if(queryUrns != null) {
			tempQueryUrns = new UrnSet(queryUrns);
		} else {
			tempQueryUrns = URN.NO_URN_SET;
		}

        this.QUERY_KEY = queryKey;
        this._doNotProxyV3 = doNotProxy;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteOrder.short2leb((short)MIN_SPEED,baos); // write minspeed
            baos.write(QUERY.getBytes("UTF-8"));              // write query
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

            // add the GGEP Extension, if necessary....
            // *----------------------------
            // construct the GGEP block
            GGEP ggepBlock = new GGEP(false); // do COBS

            // add the query key?
            if (this.QUERY_KEY != null) {
                // get query key in byte form....
                ByteArrayOutputStream qkBytes = new ByteArrayOutputStream();
                this.QUERY_KEY.write(qkBytes);
                ggepBlock.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT,
                              qkBytes.toByteArray());
            }

            // add the What Is header
            if (_featureSelector > 0)
                ggepBlock.put(GGEP.GGEP_HEADER_FEATURE_QUERY, _featureSelector);

            // add a GGEP-block if we shouldn't proxy
            if (_doNotProxyV2) {
                if (_doNotProxyV3) {
                    // for newer versions we specify which version should not
                    // be proxied
                    ggepBlock.put(GGEP.GGEP_HEADER_NO_PROXY, 3);
                }
                else {
                    // for version two, just setting the key means to not proxy
                    ggepBlock.put(GGEP.GGEP_HEADER_NO_PROXY);
                }
            }

            // add a meta flag
            if (_metaMask != null)
                ggepBlock.put(GGEP.GGEP_HEADER_META, _metaMask.intValue());

            // mark oob query to require support of security tokens
            if (canReceiveOutOfBandReplies) {
                _isSecurityTokenRequired = true;
                ggepBlock.put(GGEP.GGEP_HEADER_SECURE_OOB);
            }
            
            // if there are GGEP headers, write them out...
            if (!ggepBlock.isEmpty()) {
                ByteArrayOutputStream ggepBytes = new ByteArrayOutputStream();
                ggepBlock.write(ggepBytes);
                // write out GGEP
                addDelimiterBefore = writeGemExtension(baos, addDelimiterBefore,
                                                       ggepBytes.toByteArray());
            }
            // ----------------------------*

            baos.write(0);                             // final null
		} 
        catch(UnsupportedEncodingException uee) {
            //this should never happen from the getBytes("UTF-8") call
            //but there are UnsupportedEncodingExceptions being reported
            //with UTF-8.
            //Is there other information we want to pass in as the message?
            throw new IllegalArgumentException("could not get UTF-8 bytes for query :"
                                               + QUERY 
                                               + " with richquery :"
                                               + richQuery);
        }
        catch (IOException e) {
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
		
        QueryRequestPayloadParser parser = new QueryRequestPayloadParser(payload);
        
		QUERY = parser.query;
	    LimeXMLDocument tempDoc = null;
	    try {
	        tempDoc = new LimeXMLDocument(parser.richQuery);
        } catch(SAXException ignored) {
        } catch(SchemaNotFoundException ignored) {
        } catch(IOException ignored) {
        }
        this.XML_DOC = tempDoc;
		MIN_SPEED = parser.minSpeed;
        
		_featureSelector = parser.featureSelector;
        
        _doNotProxyV2 = parser.doNotProxyV2;
        
        _doNotProxyV3 = parser.doNotProxyV3;
        
        _metaMask = parser.metaMask;
        
        _isSecurityTokenRequired = parser.hasSecurityTokenRequest;
        
        _isPayloadModifiable = parser.isPayloadModifiable;
        
		if(parser.queryUrns == null) {
			QUERY_URNS =Collections.emptySet(); 
		}
		else {
			QUERY_URNS = Collections.unmodifiableSet(parser.queryUrns);
		}
		if(parser.requestedUrnTypes == null) {
			REQUESTED_URN_TYPES = Collections.emptySet();
		}
		else {
			REQUESTED_URN_TYPES =
			    Collections.unmodifiableSet(parser.requestedUrnTypes);
		}	
        QUERY_KEY = parser.queryKey;
		if(QUERY.length() == 0 &&
		   parser.richQuery.length() == 0 &&
		   QUERY_URNS.size() == 0) {
		    ReceivedErrorStat.QUERY_EMPTY.incrementStat();
			throw new BadPacketException("empty query");
		}       
        if(QUERY.length() > MAX_QUERY_LENGTH) {
            ReceivedErrorStat.QUERY_TOO_LARGE.incrementStat();
            //throw BadPacketException.QUERY_TOO_BIG;
            throw new BadPacketException("query too big: " + QUERY);
        }        

        if(parser.richQuery.length() > MAX_XML_QUERY_LENGTH) {
            ReceivedErrorStat.QUERY_XML_TOO_LARGE.incrementStat();
            //throw BadPacketException.XML_QUERY_TOO_BIG;
            throw new BadPacketException("xml too big: " + parser.richQuery);
        }

        if(!(QUERY_URNS.size() > 0 && QUERY.equals(DEFAULT_URN_QUERY))
           && hasIllegalChars(QUERY)) {
            ReceivedErrorStat.QUERY_ILLEGAL_CHARS.incrementStat();
            //throw BadPacketException.ILLEGAL_CHAR_IN_QUERY;
            throw new BadPacketException("illegal chars: " + QUERY);
        }
    }
    
    private static boolean hasIllegalChars(String query) {
        return StringUtils.containsCharacters(query,ILLEGAL_CHARS);
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
		SentMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(this);
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
    /* package for tests */ String getRichQueryString() {
        if( XML_DOC == null )
            return null;
        else
            return XML_DOC.getXMLString();
    }       
 
	/**
	 * Returns the <tt>Set</tt> of URN types requested for this query.
	 *
	 * @return the <tt>Set</tt> of <tt>UrnType</tt> instances requested for this
     * query, which may be empty (not null) if no types were requested
	 */
    public Set<URN.Type> getRequestedUrnTypes() {
		return REQUESTED_URN_TYPES;
    }
    
	/**
	 * Returns the <tt>Set</tt> of <tt>URN</tt> instances for this query.
	 *
	 * @return  the <tt>Set</tt> of <tt>URN</tt> instances for this query, which
	 * may be empty (not null) if no URNs were requested
	 */
    public Set<URN> getQueryUrns() {
		return QUERY_URNS;
    }
	
	/**
	 * Returns whether or not this query contains URNs.
	 *
	 * @return <tt>true</tt> if this query contains URNs,<tt>false</tt> otherwise
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
     * Returns true if the query source can do a firewalled transfer.
     */
    public boolean canDoFirewalledTransfer() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_FWTRANS_MASK) > 0)
                return true;
        }
        return false;        
    }


    /**
     * Returns true if the query source can accept out-of-band replies for
     * any supported protocol version.
     * 
     * Use getReplyAddress() and getReplyPort() if this is true to know where to
     * it. Always send XML if you are sending an out-of-band reply.
     */
    public boolean desiresOutOfBandReplies() {
        return desiresOutOfBandRepliesV2() || desiresOutOfBandRepliesV3();
    }
    
    /**
     * Returns true if sender desires out-of-band replies for protocol version
     * 2.
     */
    public boolean desiresOutOfBandRepliesV2() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_OUTOFBAND_MASK) > 0)
                return true;
        }
        return false;
    }
    
    /**
     * Returns true if sender desires out-of-band replies for protocol version
     * 3.
     */
    public boolean desiresOutOfBandRepliesV3() {
        return isSecurityTokenRequired(); 
    }
    
    /**
     * Returns true if the query source does not want you to proxy for it for
     * protocol version 2 of oob queries.
     */
    public boolean doNotProxyV2() {
        return _doNotProxyV2;
    }
    
    /**
     * Returns true if the query source does not want you to proxy for it for
     * the current protocol version of oob queries.
     */
    public boolean doNotProxyV3() {
        return _doNotProxyV3;
    }
    
    /**
     * Return whether or not the payload of the query may be modified
     * when rerouting or proxying it.
     */
    public boolean isPayloadModifiable() {
        return _isPayloadModifiable;
    }
    
    /**
     * Returns true if this query is for 'What is new?' content, i.e. usually
     * the top 3 YOUNGEST files in your library.
     */
    public boolean isWhatIsNewRequest() {
        return _featureSelector == FeatureSearchData.WHAT_IS_NEW;
    }
    
    /**
     * Returns true if this is a feature query.
     */
    public boolean isFeatureQuery() {
        return _featureSelector > 0;
    }
    
    /**
     * @return whether this is a browse host query
     */
    public boolean isBrowseHostQuery() {
        return FileManager.INDEXING_QUERY.equals(getQuery());
    }

    /**
     * Returns 0 if this is not a "feature" query, else it returns the selector
     * of the feature query, e.g. What Is New returns 1.
     */
    public int getFeatureSelector() {
        return _featureSelector;
    }
    
    /**
     * Returns true if the query request has a security token request,
     * this implies the sender requests OOB replies, protocol version 3.
     */
    public boolean isSecurityTokenRequired() {
        return _isSecurityTokenRequired;
    }

    /** Returns the address to send a out-of-band reply to.  Only useful
     *  when desiresOutOfBandReplies() == true.
     */
    public String getReplyAddress() {
        return (new GUID(getGUID())).getIP();
    }

        
    /** Returns true if the input bytes match the OOB address of this query.
     */
    public boolean matchesReplyAddress(byte[] ip) {
        return (new GUID(getGUID())).matchesIP(ip);
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

    /** @return true if the query has no constraints on the type of results
     *  it wants back.
     */
    public boolean desiresAll() {
        return (_metaMask == null);
    }

    /** @return true if the query desires 'Audio' results back.
     */
    public boolean desiresAudio() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & AUDIO_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Video' results back.
     */
    public boolean desiresVideo() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & VIDEO_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Document' results back.
     */
    public boolean desiresDocuments() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & DOC_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Image' results back.
     */
    public boolean desiresImages() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & IMAGE_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Programs/Packages' for Windows
     *  results back.
     */
    public boolean desiresWindowsPrograms() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & WIN_PROG_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Programs/Packages' for Linux/OSX
     *  results back.
     */
    public boolean desiresLinuxOSXPrograms() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & LIN_PROG_MASK) > 0);
        return true;
    }
    
    /**
     * Returns the mask of allowed programs.
     */
    public int getMetaMask() {
        if (_metaMask != null)
            return _metaMask.intValue();
        return 0;
    }

	// inherit doc comment
	public void recordDrop() {
		DroppedSentMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(this);
	}

    /** Returns this, because it's always safe to send big queries. */
    public Message stripExtendedPayload() {
        return this;
    }
    
    /** Marks this as being an re-originated query. */
    public void originate() {
        originated = true;
    }
    
    /** Determines if this is an originated query */
    public boolean isOriginated() {
        return originated;
    }

    /**
     * @effects Writes given extension string to given stream, adding
     * delimiter if necessary, reporting whether next call should add
     * delimiter. ext may be null or zero-length, in which case this is noop
     */
    protected boolean writeGemExtension(OutputStream os, 
                                        boolean addPrefixDelimiter, 
                                        byte[] extBytes) throws IOException {
        if (extBytes == null || (extBytes.length == 0)) {
            return addPrefixDelimiter;
        }
        if(addPrefixDelimiter) {
            os.write(0x1c);
        }
        os.write(extBytes);
        return true; // any subsequent extensions should have delimiter 
    }
    
     /**
     * @effects Writes given extension string to given stream, adding
     * delimiter if necessary, reporting whether next call should add
     * delimiter. ext may be null or zero-length, in which case this is noop
     */
    protected boolean writeGemExtension(OutputStream os, 
                                        boolean addPrefixDelimiter, 
                                        String ext) throws IOException {
        if (ext != null)
            return writeGemExtension(os, addPrefixDelimiter, ext.getBytes());
        else
            return writeGemExtension(os, addPrefixDelimiter, new byte[0]);
    }
    
    /**
     * @effects Writes each extension string in exts to given stream,
     * adding delimiters as necessary. exts may be null or empty, in
     *  which case this is noop
     */
    protected boolean writeGemExtensions(OutputStream os, 
                                         boolean addPrefixDelimiter, 
                                         Iterator<?> iter) throws IOException {
        if (iter == null) {
            return addPrefixDelimiter;
        }
        while(iter.hasNext()) {
            addPrefixDelimiter = writeGemExtension(os, addPrefixDelimiter, 
                                                   iter.next().toString());
        }
        return addPrefixDelimiter; // will be true is anything at all was written 
    }
    
    /**
     * @effects utility function to read null-terminated byte[] from stream
     */
    protected static byte[] readNullTerminatedBytes(InputStream is) 
        throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i;
        while ((is.available()>0)&&(i=is.read())!=0) {
            baos.write(i);
        }
        return baos.toByteArray();
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


    static byte[] patchInGGEP(byte[] payload, GGEP ggep) throws BadPacketException {
        QueryRequestPayloadParser parser = new QueryRequestPayloadParser(payload);
        HUGEExtension huge = parser.huge;
        if (huge != null) {
            // we write in the last modifiable block if available, so our
            // values are still there in the merged version that is read back 
            // from the network: this is not good
            // TODO fberger
            GGEPBlock block = getLastModifiableBlock(huge.getGGEPBlocks());
            if (block != null) {
                GGEP merge = new GGEP(false);
                // first merge in original block and then ours, to make sure
                // our values prevail
                merge.merge(block.getGGEP());
                merge.merge(ggep);
                return insertGGEP(payload, parser.hugeStart + block.getStartPos(), parser.hugeStart + block.getEndPos(), merge);
            }
            else {
                return insertGGEP(payload, payload.length, payload.length, ggep);
            }
        }
        else {
            return insertGGEP(payload, payload.length, payload.length, ggep);
        }
    }
    
    /**
     * Return the last modifiable GGEPBlock in the list or null if there
     * is none or if the list is empty.
     */
    private static GGEPBlock getLastModifiableBlock(List<GGEPBlock> blocks) {
        GGEPBlock last = null;
        for (GGEPBlock block : blocks) {
            if (!block.getGGEP().hasKey(GGEP.GGEP_HEADER_DO_NOT_MODIFY_GGEP)) {
                last = block;
            }
        }
        return last;
    }
    
    private static byte[] insertGGEP(byte[] payload, int start, int end, GGEP ggep) {
        byte[] ggepBytes = ggep.toByteArray(); 
        byte[] newPayload = new byte[payload.length + ggepBytes.length - (end - start)];
        
        System.arraycopy(payload, 0, newPayload, 0, start);
        System.arraycopy(ggepBytes, 0, newPayload, start, ggepBytes.length);
        
        if (end < payload.length) {
            System.arraycopy(payload, end, newPayload, start + ggepBytes.length, payload.length - end);
        }
        
        return newPayload;
    }
    
    static class QueryRequestPayloadParser {

        String query = "";
        String richQuery = "";
        int minSpeed = 0;
        Set<URN> queryUrns = null;
        Set<URN.Type> requestedUrnTypes = null;
        QueryKey queryKey = null;
        
        HUGEExtension huge;
        
        int featureSelector;
        
        boolean doNotProxyV2;
        
        boolean doNotProxyV3;
        
        Integer metaMask;
        
        boolean hasSecurityTokenRequest;
        
        int hugeStart;
        
        int hugeEnd;
        
        boolean isPayloadModifiable = true;
        
        public QueryRequestPayloadParser(byte[] payload) throws BadPacketException {
            try {
                PositionByteArrayInputStream bais = new PositionByteArrayInputStream(payload);
                short sp = ByteOrder.leb2short(bais);
                minSpeed = ByteOrder.ushort2int(sp);
                query = new String(readNullTerminatedBytes(bais), "UTF-8");
                 
                // handle extensions, which include rich query and URN stuff
                hugeStart = bais.getPos();
                byte[] extsBytes = readNullTerminatedBytes(bais);
                huge = new HUGEExtension(extsBytes);
                hugeEnd = bais.getPos();
                GGEP ggep = huge.getGGEP();

                if(ggep != null) {
                    try {
                        if (ggep.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                            byte[] qkBytes = ggep.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
                            queryKey = new QueryKey(qkBytes);
                        }
                        if (ggep.hasKey(GGEP.GGEP_HEADER_FEATURE_QUERY))
                            featureSelector = ggep.getInt(GGEP.GGEP_HEADER_FEATURE_QUERY);
                        if (ggep.hasKey(GGEP.GGEP_HEADER_NO_PROXY)) {
                            doNotProxyV2 = true;
                            try {
                                if (ggep.getInt(GGEP.GGEP_HEADER_NO_PROXY) == 3) {
                                    doNotProxyV3 = true;
                                }
                            }
                            catch (BadGGEPPropertyException bgpe) { }
                        }
                        if (ggep.hasKey(GGEP.GGEP_HEADER_META)) {
                            metaMask = new Integer(ggep.getInt(GGEP.GGEP_HEADER_META));
                            // if the value is something we can't handle, don't even set it
                            if ((metaMask.intValue() < 4) || (metaMask.intValue() > 248))
                                metaMask = null;
                        }
                        if (ggep.hasKey(GGEP.GGEP_HEADER_SECURE_OOB)) {
                            hasSecurityTokenRequest = true;
                        }
                        if (ggep.hasKey(GGEP.GGEP_HEADER_DO_NOT_MODIFY_PAYLOAD)) {
                            isPayloadModifiable = false;
                        }
                    } catch (BadGGEPPropertyException ignored) {}
                }

                queryUrns = huge.getURNS();
                requestedUrnTypes = huge.getURNTypes();
                for(String currMiscBlock : huge.getMiscBlocks()) {
                    if(!richQuery.equals(""))
                        break;
                    if (currMiscBlock.startsWith("<?xml"))
                        richQuery = currMiscBlock;                
                }
            } catch(UnsupportedEncodingException uee) {
                //couldn't build query from network due to unsupportedencodingexception
                //so throw a BadPacketException 
                throw new BadPacketException(uee.getMessage());
            } catch (IOException ioe) {
                ErrorService.error(ioe);
            }
        }
        
        static class PositionByteArrayInputStream extends ByteArrayInputStream {

            public PositionByteArrayInputStream(byte[] buf) {
                super(buf);
            }
            
            public int getPos() {
                return pos;
            }
            
        }
        
    }
    
}
