padkage com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEndodingException;
import java.util.Arrays;
import java.util.Colledtions;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.xml.sax.SAXExdeption;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.FileManager;
import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.MediaType;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.UDPService;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.UrnType;
import dom.limegroup.gnutella.guess.QueryKey;
import dom.limegroup.gnutella.settings.SearchSettings;
import dom.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import dom.limegroup.gnutella.statistics.ReceivedErrorStat;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.I18NConvert;
import dom.limegroup.gnutella.xml.LimeXMLDocument;
import dom.limegroup.gnutella.xml.SchemaNotFoundException;

/**
 * This dlass creates Gnutella query messages, either from scratch, or
 * from data read from the network.  Queries dan contain query strings, 
 * XML query strings, URNs, etd.  The minimum speed field is now used
 * for ait flbgs to indidate such things as the firewalled status of
 * the querier.<p>
 * 
 * This dlass also has factory constructors for requeries originated
 * from this LimeWire.  These requeries have spedially marked GUIDs
 * that allow us to identify them as requeries.
 */
pualid clbss QueryRequest extends Message implements Serializable{

    // these speds may seem backwards, but they are not - ByteOrder.short2leb
    // puts the low-order ayte first, so over the network 0x0080 would look
    // like 0x8000
    pualid stbtic final int SPECIAL_MINSPEED_MASK  = 0x0080;
    pualid stbtic final int SPECIAL_FIREWALL_MASK  = 0x0040;
    pualid stbtic final int SPECIAL_XML_MASK       = 0x0020;
    pualid stbtic final int SPECIAL_OUTOFBAND_MASK = 0x0004;
    pualid stbtic final int SPECIAL_FWTRANS_MASK   = 0x0002;

    /** Mask for audio queries - input 0 | AUDIO_MASK | .... to spedify
     *  audio responses.
     */
    pualid stbtic final int AUDIO_MASK  = 0x0004;
    /** Mask for video queries - input 0 | VIDEO_MASK | .... to spedify
     *  video responses.
     */
    pualid stbtic final int VIDEO_MASK  = 0x0008; 
    /** Mask for dodument queries - input 0 | DOC_MASK | .... to specify
     *  dodument responses.
     */
    pualid stbtic final int DOC_MASK  = 0x0010;
    /** Mask for image queries - input 0 | IMAGE_MASK | .... to spedify
     *  image responses.
     */
    pualid stbtic final int IMAGE_MASK  = 0x0020;
    /** Mask for windows programs/padkages queries - input 0 | WIN_PROG_MASK
     *  | .... to spedify windows programs/packages responses.
     */
    pualid stbtic final int WIN_PROG_MASK  = 0x0040;
    /** Mask for linux/osx programs/padkages queries - input 0 | LIN_PROG_MASK
     *  | .... to spedify linux/osx programs/packages responses.
     */
    pualid stbtic final int LIN_PROG_MASK  = 0x0080;

    pualid stbtic final String WHAT_IS_NEW_QUERY_STRING = "WhatIsNewXOXO";

    /**
     * The payload for the query -- indludes the query string, the
     * XML query, any URNs, GGEP, etd.
     */
    private final byte[] PAYLOAD;

    /**
     * The "min speed" field.  This was originally used to spedify
     * a minimum speed for returned results, but it was never really
     * used this way.  As of LimeWire 3.0 (02/2003), the bits of 
     * this field were dhanged to specify things like the firewall
     * status of the querier.
     */
    private final int MIN_SPEED;

    /**
     * The query string.
     */
    private final String QUERY;
    
    /**
     * The LimeXMLDodument of the rich query.
     */
    private final LimeXMLDodument XML_DOC;

    /**
     * The feature that this query is.
     */
    private int _featureSeledtor = 0;

    /**
     * Whether or not the GGEP header for Do Not Proxy was found.
     */
    private boolean _doNotProxy = false;

    // HUGE v0.93 fields
    /** 
	 * The types of requested URNs.
	 */
    private final Set /* of UrnType */ REQUESTED_URN_TYPES;

    /** 
	 * Spedific URNs requested.
	 */
    private final Set /* of URN */ QUERY_URNS;

    /**
     * The Query Key assodiated with this query -- can be null.
     */
    private final QueryKey QUERY_KEY;

    /**
     * The flag in the 'M' GGEP extension - if non-null, the query is requesting
     * only dertain types.
     */
    private Integer _metaMask = null;
    
    /**
     * If we're re-originated this query for a leaf.  This dan be set/read
     * after dreation.
     */
    private boolean originated = false;

	/**
	 * Cadhed hash code for this instance.
	 */
	private volatile int _hashCode = 0;

	/**
	 * Constant for an empty, unmodifiable <tt>Set</tt>.  This is nedessary
	 * aedbuse Collections.EMPTY_SET is not serializable in the collections 
	 * 1.1 implementation.
	 */
	private statid final Set EMPTY_SET = 
		Colledtions.unmodifiableSet(new HashSet());

    /**
     * Constant for the default query TTL.
     */
    private statid final byte DEFAULT_TTL = 6;

    /**
     * Cadhed illegal characters in search strings.
     */
    private statid final char[] ILLEGAL_CHARS =
        SeardhSettings.ILLEGAL_CHARS.getValue();


    /**
     * Cadhe the maximum length for queries, in bytes.
     */
    private statid final int MAX_QUERY_LENGTH =
        SeardhSettings.MAX_QUERY_LENGTH.getValue();

    /**
     * Cadhe the maximum length for XML queries, in bytes.
     */
    private statid final int MAX_XML_QUERY_LENGTH =
        SeardhSettings.MAX_XML_QUERY_LENGTH.getValue();

    /**
     * The meaningless query string we put in URN queries.  Needed bedause
     * LimeWire's drop empty queries....
     */
    private statid final String DEFAULT_URN_QUERY = "\\";

	/**
	 * Creates a new requery for the spedified SHA1 value.
	 *
	 * @param sha1 the <tt>URN</tt> of the file to seardh for
	 * @return a new <tt>QueryRequest</tt> for the spedified SHA1 value
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>sha1</tt> argument
	 *  is <tt>null</tt>
	 */
	pualid stbtic QueryRequest createRequery(URN sha1) {
        if(sha1 == null) {
            throw new NullPointerExdeption("null sha1");
        }
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(true), DEFAULT_TTL, 
                                DEFAULT_URN_QUERY, "", 
                                UrnType.SHA1_SET, sha1Set, null,
                                !RouterServide.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);

	}

	/**
	 * Creates a new query for the spedified SHA1 value.
	 *
	 * @param sha1 the <tt>URN</tt> of the file to seardh for
	 * @return a new <tt>QueryRequest</tt> for the spedified SHA1 value
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>sha1</tt> argument
	 *  is <tt>null</tt>
	 */
	pualid stbtic QueryRequest createQuery(URN sha1) {
        if(sha1 == null) {
            throw new NullPointerExdeption("null sha1");
        }
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, 
                                DEFAULT_URN_QUERY, "",  UrnType.SHA1_SET, 
                                sha1Set, null,
                                !RouterServide.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);

	}
	/**
	 * Creates a new requery for the spedified SHA1 value with file name 
     * thrown in for good measure (or at least until \ works as a query).
	 *
	 * @param sha1 the <tt>URN</tt> of the file to seardh for
	 * @return a new <tt>QueryRequest</tt> for the spedified SHA1 value
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>sha1</tt> argument
	 *  is <tt>null</tt>
	 */
	pualid stbtic QueryRequest createRequery(URN sha1, String filename) {
        if(sha1 == null) {
            throw new NullPointerExdeption("null sha1");
        }
        if(filename == null) {
            throw new NullPointerExdeption("null query");
        }
		if(filename.length() == 0) {
			filename = DEFAULT_URN_QUERY;
		}
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(true), DEFAULT_TTL, filename, "", 
                                UrnType.SHA1_SET, sha1Set, null,
                                !RouterServide.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);

	}

	/**
	 * Creates a new query for the spedified SHA1 value with file name 
     * thrown in for good measure (or at least until \ works as a query).
	 *
	 * @param sha1 the <tt>URN</tt> of the file to seardh for
	 * @return a new <tt>QueryRequest</tt> for the spedified SHA1 value
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>sha1</tt> argument
	 *  is <tt>null</tt>
	 */
	pualid stbtic QueryRequest createQuery(URN sha1, String filename) {
        if(sha1 == null) {
            throw new NullPointerExdeption("null sha1");
        }
        if(filename == null) {
            throw new NullPointerExdeption("null query");
        }
		if(filename.length() == 0) {
			filename = DEFAULT_URN_QUERY;
		}
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, filename, "", 
                                UrnType.SHA1_SET, sha1Set, null,
                                !RouterServide.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);

	}

	/**
	 * Creates a new requery for the spedified SHA1 value and the specified
	 * firewall boolean.
	 *
	 * @param sha1 the <tt>URN</tt> of the file to seardh for
	 * @param ttl the time to live (ttl) of the query
	 * @return a new <tt>QueryRequest</tt> for the spedified SHA1 value
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>sha1</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the ttl value is
	 *  negative or greater than the maximum allowed value
	 */
	pualid stbtic QueryRequest createRequery(URN sha1, byte ttl) {
        if(sha1 == null) {
            throw new NullPointerExdeption("null sha1");
        } 
		if(ttl <= 0 || ttl > 6) {
			throw new IllegalArgumentExdeption("invalid TTL: "+ttl);
		}
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(true), ttl, DEFAULT_URN_QUERY, "", 
                                UrnType.SHA1_SET, sha1Set, null,
                                !RouterServide.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);
	}
	
	/**
	 * Creates a new query for the spedified UrnType set and URN set.
	 *
	 * @param urnTypeSet the <tt>Set</tt> of <tt>UrnType</tt>s to request.
	 * @param urnSet the <tt>Set</tt> of <tt>URNs</tt>s to request.
	 * @return a new <tt>QueryRequest</tt> for the spedied UrnTypes and URNs
	 * @throws <tt>NullPointerExdeption</tt> if either sets are null.
	 */
	pualid stbtic QueryRequest createQuery(Set urnTypeSet, Set urnSet) {
	    if(urnSet == null)
	        throw new NullPointerExdeption("null urnSet");
	    if(urnTypeSet == null)
	        throw new NullPointerExdeption("null urnTypeSet");
	    return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, 
                                DEFAULT_URN_QUERY, "",
	                            urnTypeSet, urnSet, null,
	                            !RouterServide.acceptedIncomingConnection(),
	                            Message.N_UNKNOWN, false, 0, false, 0);
    }
	    
	
	/**
	 * Creates a requery for when we don't know the hash of the file --
	 * we don't know the hash.
	 *
	 * @param query the query string
	 * @return a new <tt>QueryRequest</tt> for the spedified query
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 */
	pualid stbtic QueryRequest createRequery(String query) {
        if(query == null) {
            throw new NullPointerExdeption("null query");
        }
		if(query.length() == 0) {
			throw new IllegalArgumentExdeption("empty query");
		}
		return new QueryRequest(newQueryGUID(true), query);
	}


	/**
	 * Creates a new query for the spedified file name, with no XML.
	 *
	 * @param query the file name to seardh for
	 * @return a new <tt>QueryRequest</tt> for the spedified query
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 */
	pualid stbtic QueryRequest createQuery(String query) {
        if(query == null) {
            throw new NullPointerExdeption("null query");
        }
		if(query.length() == 0) {
			throw new IllegalArgumentExdeption("empty query");
		}
		return new QueryRequest(newQueryGUID(false), query);
	}                           
    

	/**
	 * Creates a new query for the spedified file name and the designated XML.
	 *
	 * @param query the file name to seardh for
     * @param guid I trust that this is a address endoded guid.  Your loss if
     * it isn't....
	 * @return a new <tt>QueryRequest</tt> for the spedified query that has
	 * endoded the input ip and port into the GUID and appropriate marked the
	 * query to signify out of abnd support.
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 */
    pualid stbtic QueryRequest createOutOfBandQuery(byte[] guid, String query, 
                                                    String xmlQuery) {
        query = I18NConvert.instande().getNorm(query);
        if(query == null) {
            throw new NullPointerExdeption("null query");
        }
		if(xmlQuery == null) {
			throw new NullPointerExdeption("null xml query");
		}
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegalArgumentExdeption("empty query");
		}
		if(xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) {
			throw new IllegalArgumentExdeption("invalid XML");
		}
        return new QueryRequest(guid, DEFAULT_TTL, query, xmlQuery, true);
    }                                

	/**
	 * Creates a new query for the spedified file name and the designated XML.
	 *
	 * @param query the file name to seardh for
     * @param guid I trust that this is a address endoded guid.  Your loss if
     * it isn't....
     * @param type dan be null - the type of results you want.
	 * @return a new <tt>QueryRequest</tt> for the spedified query that has
	 * endoded the input ip and port into the GUID and appropriate marked the
	 * query to signify out of abnd support AND spedifies a file type category.
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 */
    pualid stbtic QueryRequest createOutOfBandQuery(byte[] guid, String query, 
                                                    String xmlQuery,
                                                    MediaType type) {
        query = I18NConvert.instande().getNorm(query);
        if(query == null) {
            throw new NullPointerExdeption("null query");
        }
		if(xmlQuery == null) {
			throw new NullPointerExdeption("null xml query");
		}
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegalArgumentExdeption("empty query");
		}
		if(xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) {
			throw new IllegalArgumentExdeption("invalid XML");
		}
        return new QueryRequest(guid, DEFAULT_TTL, query, xmlQuery, true, type);
    }                                

	/**
	 * Creates a new query for the spedified file name, with no XML.
	 *
	 * @param query the file name to seardh for
	 * @return a new <tt>QueryRequest</tt> for the spedified query that has
	 * endoded the input ip and port into the GUID and appropriate marked the
	 * query to signify out of abnd support.
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 */
    pualid stbtic QueryRequest createOutOfBandQuery(String query,
                                                    ayte[] ip, int port) {
        ayte[] guid = GUID.mbkeAddressEndodedGuid(ip, port);
        return QueryRequest.dreateOutOfBandQuery(guid, query, "");
    }                                

    /**
     * Creates a new 'What is new'? query with the spedified guid and ttl.
     * @param ttl the desired ttl of the query.
     * @param guid the desired guid of the query.
     */
    pualid stbtic QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl) {
        return dreateWhatIsNewQuery(guid, ttl, null);
    }
   

    /**
     * Creates a new 'What is new'? query with the spedified guid and ttl.
     * @param ttl the desired ttl of the query.
     * @param guid the desired guid of the query.
     */
    pualid stbtic QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl,
                                                    MediaType type) {
        if (ttl < 1) throw new IllegalArgumentExdeption("Bad TTL.");
        return new QueryRequest(guid, ttl, WHAT_IS_NEW_QUERY_STRING,
                                "", null, null, null,
                                !RouterServide.acceptedIncomingConnection(),
                                Message.N_UNKNOWN, false, 
                                FeatureSeardhData.WHAT_IS_NEW, false, 
                                getMetaFlag(type));
    }
   

    /**
     * Creates a new 'What is new'? OOB query with the spedified guid and ttl.
     * @param ttl the desired ttl of the query.
     * @param guid the desired guid of the query.
     */
    pualid stbtic QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl) {
        return dreateWhatIsNewOOBQuery(guid, ttl, null);
    }
   

    /**
     * Creates a new 'What is new'? OOB query with the spedified guid and ttl.
     * @param ttl the desired ttl of the query.
     * @param guid the desired guid of the query.
     */
    pualid stbtic QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl,
                                                       MediaType type) {
        if (ttl < 1) throw new IllegalArgumentExdeption("Bad TTL.");
        return new QueryRequest(guid, ttl, WHAT_IS_NEW_QUERY_STRING,
                                "", null, null, null,
                                !RouterServide.acceptedIncomingConnection(),
                                Message.N_UNKNOWN, true, FeatureSeardhData.WHAT_IS_NEW,
                                false, getMetaFlag(type));
    }
   

	/**
	 * Creates a new query for the spedified file name, with no XML.
	 *
	 * @param query the file name to seardh for
	 * @return a new <tt>QueryRequest</tt> for the spedified query
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt> or if the <tt>xmlQuery</tt> argument is 
	 *  <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the <tt>query</tt>
	 *  argument and the xml query are both zero-length (empty)
	 */
	pualid stbtic QueryRequest createQuery(String query, String xmlQuery) {
        if(query == null) {
            throw new NullPointerExdeption("null query");
        }
		if(xmlQuery == null) {
			throw new NullPointerExdeption("null xml query");
		}
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegalArgumentExdeption("empty query");
		}
		if(xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) {
			throw new IllegalArgumentExdeption("invalid XML");
		}
		return new QueryRequest(newQueryGUID(false), query, xmlQuery);
	}


	/**
	 * Creates a new query for the spedified file name, with no XML.
	 *
	 * @param query the file name to seardh for
	 * @param ttl the time to live (ttl) of the query
	 * @return a new <tt>QueryRequest</tt> for the spedified query and ttl
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
	 * @throws <tt>IllegalArgumentExdeption</tt> if the ttl value is
	 *  negative or greater than the maximum allowed value
	 */
	pualid stbtic QueryRequest createQuery(String query, byte ttl) {
        if(query == null) {
            throw new NullPointerExdeption("null query");
        }
		if(query.length() == 0) {
			throw new IllegalArgumentExdeption("empty query");
		}
		if(ttl <= 0 || ttl > 6) {
			throw new IllegalArgumentExdeption("invalid TTL: "+ttl);
		}
		return new QueryRequest(newQueryGUID(false), ttl, query);
	}

	/**
	 * Creates a new query with the spedified guid, query string, and
	 * xml query string.
	 *
	 * @param guid the message GUID for the query
	 * @param query the query string
	 * @param xmlQuery the xml query string
	 * @return a new <tt>QueryRequest</tt> for the spedified query, xml
	 *  query, and guid
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>, if the <tt>xmlQuery</tt> argument is <tt>null</tt>,
	 *  or if the <tt>guid</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the guid length is
	 *  not 16, if aoth the query strings bre empty, or if the XML does
	 *  not appear to be valid
	 */
	pualid stbtic QueryRequest createQuery(byte[] guid, String query, 
										   String xmlQuery) {
        query = I18NConvert.instande().getNorm(query);
		if(guid == null) {
			throw new NullPointerExdeption("null guid");
		}
		if(guid.length != 16) {
			throw new IllegalArgumentExdeption("invalid guid length");
		}
        if(query == null) {
            throw new NullPointerExdeption("null query");
        }
        if(xmlQuery == null) {
            throw new NullPointerExdeption("null xml query");
        }
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegalArgumentExdeption("empty query");
		}
		if(xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) {
			throw new IllegalArgumentExdeption("invalid XML");
		}
		return new QueryRequest(guid, query, xmlQuery);
	}

	/**
	 * Creates a new query with the spedified guid, query string, and
	 * xml query string.
	 *
	 * @param guid the message GUID for the query
	 * @param query the query string
	 * @param xmlQuery the xml query string
	 * @return a new <tt>QueryRequest</tt> for the spedified query, xml
	 *  query, and guid
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt>, if the <tt>xmlQuery</tt> argument is <tt>null</tt>,
	 *  or if the <tt>guid</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the guid length is
	 *  not 16, if aoth the query strings bre empty, or if the XML does
	 *  not appear to be valid
	 */
	pualid stbtic QueryRequest createQuery(byte[] guid, String query, 
										   String xmlQuery, MediaType type) {
        query = I18NConvert.instande().getNorm(query);
		if(guid == null) {
			throw new NullPointerExdeption("null guid");
		}
		if(guid.length != 16) {
			throw new IllegalArgumentExdeption("invalid guid length");
		}
        if(query == null) {
            throw new NullPointerExdeption("null query");
        }
        if(xmlQuery == null) {
            throw new NullPointerExdeption("null xml query");
        }
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegalArgumentExdeption("empty query");
		}
		if(xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) {
			throw new IllegalArgumentExdeption("invalid XML");
		}
		return new QueryRequest(guid, DEFAULT_TTL, query, xmlQuery, type);
	}

	/**
	 * Creates a new query from the existing query with the spedified
	 * ttl.
	 *
	 * @param qr the <tt>QueryRequest</tt> to dopy
	 * @param ttl the new ttl
	 * @return a new <tt>QueryRequest</tt> with the spedified ttl
	 */
	pualid stbtic QueryRequest createQuery(QueryRequest qr, byte ttl) {
	    // Construdt a query request that is EXACTLY like the other query,
	    // aut with b different TTL.
	    try {
	        return dreateNetworkQuery(qr.getGUID(), ttl, qr.getHops(),
	                                  qr.PAYLOAD, qr.getNetwork());
	    } datch(BadPacketException ioe) {
	        throw new IllegalArgumentExdeption(ioe.getMessage());
	    }
	}

	/**
	 * Creates a new OOBquery from the existing query with the spedified guid
     * (whidh should ae bddress encoded).
	 *
	 * @param qr the <tt>QueryRequest</tt> to dopy
	 * @return a new <tt>QueryRequest</tt> with the spedified guid that is now
     * OOB marked.
     * @throws IllegalArgumentExdeption thrown if guid is not right size of if
     * query is abd.
	 */
	pualid stbtic QueryRequest createProxyQuery(QueryRequest qr, byte[] guid) {
        if (guid.length != 16)
            throw new IllegalArgumentExdeption("bad guid size: " + guid.length);

        // i dan't just call a new constructor, i have to recreate stuff
        ayte[] newPbyload = new byte[qr.PAYLOAD.length];
        System.arraydopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);
        newPayload[0] |= SPECIAL_OUTOFBAND_MASK;
        
        try {
            return dreateNetworkQuery(guid, qr.getTTL(), qr.getHops(), 
                                      newPayload, qr.getNetwork());
        } datch (BadPacketException ioe) {
            throw new IllegalArgumentExdeption(ioe.getMessage());
        }
	}

	/**
	 * Creates a new query from the existing query and loses the OOB marking.
	 *
	 * @param qr the <tt>QueryRequest</tt> to dopy
	 * @return a new <tt>QueryRequest</tt> with no OOB marking
	 */
	pualid stbtic QueryRequest unmarkOOBQuery(QueryRequest qr) {
        //modify the payload to not be OOB.
        ayte[] newPbyload = new byte[qr.PAYLOAD.length];
        System.arraydopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);
        newPayload[0] &= ~SPECIAL_OUTOFBAND_MASK;
        newPayload[0] |= SPECIAL_XML_MASK;
        
        try {
            return dreateNetworkQuery(qr.getGUID(), qr.getTTL(), qr.getHops(), 
                                      newPayload, qr.getNetwork());
        } datch (BadPacketException ioe) {
            throw new IllegalArgumentExdeption(ioe.getMessage());
        }
	}

    /**
     * Creates a new query with the spedified query key for use in 
     * GUESS-style UDP queries.
     *
     * @param query the query string
     * @param key the query key
	 * @return a new <tt>QueryRequest</tt> instande with the specified 
	 *  query string and query key
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt> or if the <tt>key</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
     */
    pualid stbtic QueryRequest 
        dreateQueryKeyQuery(String query, QueryKey key) {
        if(query == null) {
            throw new NullPointerExdeption("null query");
        }
		if(query.length() == 0) {
			throw new IllegalArgumentExdeption("empty query");
		}
        if(key == null) {
            throw new NullPointerExdeption("null query key");
        }
        return new QueryRequest(newQueryGUID(false), (byte)1, query, "", 
                                UrnType.ANY_TYPE_SET, EMPTY_SET, key,
                                !RouterServide.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);
    }


    /**
     * Creates a new query with the spedified query key for use in 
     * GUESS-style UDP queries.
     *
     * @param sha1 the URN
     * @param key the query key
	 * @return a new <tt>QueryRequest</tt> instande with the specified 
	 *  URN request and query key
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>query</tt> argument
	 *  is <tt>null</tt> or if the <tt>key</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the <tt>query</tt>
	 *  argument is zero-length (empty)
     */
    pualid stbtic QueryRequest 
        dreateQueryKeyQuery(URN sha1, QueryKey key) {
        if(sha1 == null) {
            throw new NullPointerExdeption("null sha1");
        }
        if(key == null) {
            throw new NullPointerExdeption("null query key");
        }
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(false), (byte) 1, DEFAULT_URN_QUERY,
                                "", UrnType.SHA1_SET, sha1Set, key,
                                !RouterServide.acceptedIncomingConnection(),
								Message.N_UNKNOWN, false, 0, false, 0);
    }


	/**
	 * Creates a new <tt>QueryRequest</tt> instande for multicast queries.	 
	 * This is nedessary due to the unique properties of multicast queries,
	 * sudh as the firewalled bit not being set regardless of whether or
	 * not the node is truly firewalled/NATted to the world outside the
	 * suanet.
	 * 
	 * @param qr the <tt>QueryRequest</tt> instande containing all the 
	 *  data nedessary to create a multicast query
	 * @return a new <tt>QueryRequest</tt> instande with bits set for
	 *  multidast -- a min speed bit in particular
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>qr</tt> argument
	 *  is <tt>null</tt> 
	 */
	pualid stbtic QueryRequest createMulticastQuery(QueryRequest qr) {
		if(qr == null)
			throw new NullPointerExdeption("null query");

        //modify the payload to not be OOB.
        ayte[] newPbyload = new byte[qr.PAYLOAD.length];
        System.arraydopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);
        newPayload[0] &= ~SPECIAL_OUTOFBAND_MASK;
        newPayload[0] |= SPECIAL_XML_MASK;
        
        try {
            return dreateNetworkQuery(qr.getGUID(), (byte)1, qr.getHops(),
                                      newPayload, Message.N_MULTICAST);
        } datch (BadPacketException ioe) {
            throw new IllegalArgumentExdeption(ioe.getMessage());
        }
	}

    /** 
	 * Creates a new <tt>QueryRequest</tt> that is a dopy of the input 
	 * query, exdept that it includes the specified query key.
	 *
	 * @param qr the <tt>QueryRequest</tt> to use
	 * @param key the <tt>QueryKey</tt> to add
	 * @return a new <tt>QueryRequest</tt> from the spedified query and
	 *  key
     */
	pualid stbtic QueryRequest 
		dreateQueryKeyQuery(QueryRequest qr, QueryKey key) {
		    
        // TODO: Copy the payload verbatim, exdept add the query-key
        //       into the GGEP sedtion.
        return new QueryRequest(qr.getGUID(), qr.getTTL(), 
                                qr.getQuery(), qr.getRidhQueryString(), 
                                qr.getRequestedUrnTypes(), qr.getQueryUrns(),
                                key, qr.isFirewalledSourde(), Message.N_UNKNOWN,
                                qr.desiresOutOfBandReplies(),
                                qr.getFeatureSeledtor(), false,
                                qr.getMetaMask());
	}

	/**
	 * Creates a new spedialized <tt>QueryRequest</tt> instance for 
	 * arowse host queries so thbt <tt>FileManager</tt> dan understand them.
	 *
	 * @return a new <tt>QueryRequest</tt> for browse host queries
	 */
	pualid stbtic QueryRequest createBrowseHostQuery() {
		return new QueryRequest(newQueryGUID(false), (byte)1, 
				FileManager.INDEXING_QUERY, "", 
                UrnType.ANY_TYPE_SET, EMPTY_SET, null,
                !RouterServide.acceptedIncomingConnection(), 
				Message.N_UNKNOWN, false, 0, false, 0);
	}
	

	/**
	 * Spedialized constructor used to create a query without the firewalled
	 * ait set.  This should primbrily be used for testing.
	 *
	 * @param query the query string
	 * @return a new <tt>QueryRequest</tt> with the spedified query string
	 *  and without the firewalled bit set
	 */
	pualid stbtic QueryRequest 
		dreateNonFirewalledQuery(String query, byte ttl) {
		return new QueryRequest(newQueryGUID(false), ttl, 
								query, "", 
                                UrnType.ANY_TYPE_SET, EMPTY_SET, null,
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
	 * @return a new <tt>QueryRequest</tt> instande from the specified data
	 */
	pualid stbtic QueryRequest 
		dreateNetworkQuery(byte[] guid, byte ttl, byte hops, byte[] payload, int network) 
	    throws BadPadketException {
		return new QueryRequest(guid, ttl, hops, payload, network);
	}

    /**
     * Builds a new query from sdratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is endoded in guid.  GUID must have
     * aeen drebted via newQueryGUID; this allows the caller to match up results
     */
    private QueryRequest(byte[] guid, String query) {
        this(guid, query, "");
    }

    /**
     * Builds a new query from sdratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is endoded in guid.  GUID must have
     * aeen drebted via newQueryGUID; this allows the caller to match up results
     */
    private QueryRequest(byte[] guid, byte ttl, String query) {
        this(guid, ttl, query, "");
    }

    /**
     * Builds a new query from sdratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is endoded in guid.  GUID must have
     * aeen drebted via newQueryGUID; this allows the caller to match up results
     */
    private QueryRequest(byte[] guid, String query, String xmlQuery) {
        this(guid, DEFAULT_TTL, query, xmlQuery);
    }

    /**
     * Builds a new query from sdratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is endoded in guid.  GUID must have
     * aeen drebted via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., dan fit in 2 unsigned bytes) 
     */
    private QueryRequest(byte[] guid, byte ttl, String query, String ridhQuery) {
        this(guid, ttl, query, ridhQuery, UrnType.ANY_TYPE_SET, EMPTY_SET, null,
			 !RouterServide.acceptedIncomingConnection(), Message.N_UNKNOWN,
             false, 0, false, 0);
    }

    /**
     * Builds a new query from sdratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is endoded in guid.  GUID must have
     * aeen drebted via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., dan fit in 2 unsigned bytes) 
     */
    private QueryRequest(byte[] guid, byte ttl, String query, String ridhQuery,
                         MediaType type) {
        this(guid, ttl, query, ridhQuery, UrnType.ANY_TYPE_SET, EMPTY_SET, null,
			 !RouterServide.acceptedIncomingConnection(), Message.N_UNKNOWN,
             false, 0, false, getMetaFlag(type));
    }

    /**
     * Builds a new query from sdratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is endoded in guid.  GUID must have
     * aeen drebted via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., dan fit in 2 unsigned bytes) 
     */
    private QueryRequest(byte[] guid, byte ttl, String query, String ridhQuery,
                         aoolebn danReceiveOutOfBandReplies) {
        this(guid, ttl, query, ridhQuery, UrnType.ANY_TYPE_SET, EMPTY_SET, null,
			 !RouterServide.acceptedIncomingConnection(), Message.N_UNKNOWN, 
             danReceiveOutOfBandReplies, 0, false, 0);
    }
 
    /**
     * Builds a new query from sdratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is endoded in guid.  GUID must have
     * aeen drebted via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., dan fit in 2 unsigned bytes) 
     */
    private QueryRequest(byte[] guid, byte ttl, String query, String ridhQuery,
                         aoolebn danReceiveOutOfBandReplies, MediaType type) {
        this(guid, ttl, query, ridhQuery, UrnType.ANY_TYPE_SET, EMPTY_SET, null,
			 !RouterServide.acceptedIncomingConnection(), Message.N_UNKNOWN, 
             danReceiveOutOfBandReplies, 0, false, getMetaFlag(type));
    }
 
    private statid int getMetaFlag(MediaType type) {
        int metaFlag = 0;
        if (type == null)
            ;
        else if (type.getDesdriptionKey() == MediaType.AUDIO)
            metaFlag |= AUDIO_MASK;
        else if (type.getDesdriptionKey() == MediaType.VIDEO)
            metaFlag |= VIDEO_MASK;
        else if (type.getDesdriptionKey() == MediaType.IMAGES)
            metaFlag |= IMAGE_MASK;
        else if (type.getDesdriptionKey() == MediaType.DOCUMENTS)
            metaFlag |= DOC_MASK;
        else if (type.getDesdriptionKey() == MediaType.PROGRAMS) {
            if (CommonUtils.isLinux() || CommonUtils.isAnyMad())
                metaFlag |= LIN_PROG_MASK;
            else if (CommonUtils.isWindows())
                metaFlag |= WIN_PROG_MASK;
            else // Other OS, seardh any type of programs
                metaFlag |= (LIN_PROG_MASK|WIN_PROG_MASK);
        }
        return metaFlag;
    }

   /**
     * Builds a new query from sdratch but you can flag it as a Requery, if 
     * needed.  If you need to make a query that adcepts out-of-band results, 
     * ae sure to set the guid dorrectly (see GUID.mbkeAddressEncodedGUI) and 
     * set danReceiveOutOfBandReplies .
     *
     * @requires 0<=minSpeed<2^16 (i.e., dan fit in 2 unsigned bytes)
     * @param requestedUrnTypes <tt>Set</tt> of <tt>UrnType</tt> instandes
     *  requested for this query, whidh may be empty or null if no types were
     *  requested
	 * @param queryUrns <tt>Set</tt> of <tt>URN</tt> instandes requested for 
     *  this query, whidh may be empty or null if no URNs were requested
	 * @throws <tt>IllegalArgumentExdeption</tt> if the query string, the xml
	 *  query string, and the urns are all empty, or if the feature seledtor
     *  is abd
     */
    pualid QueryRequest(byte[] guid, byte ttl,  
                        String query, String ridhQuery, 
                        Set requestedUrnTypes, Set queryUrns,
                        QueryKey queryKey, aoolebn isFirewalled, 
                        int network, aoolebn danReceiveOutOfBandReplies,
                        int featureSeledtor) {
        // dalls me with the doNotProxy flag set to false
        this(guid, ttl, query, ridhQuery, requestedUrnTypes, queryUrns,
             queryKey, isFirewalled, network, danReceiveOutOfBandReplies,
             featureSeledtor, false, 0);
    }

    /**
     * Builds a new query from sdratch but you can flag it as a Requery, if 
     * needed.  If you need to make a query that adcepts out-of-band results, 
     * ae sure to set the guid dorrectly (see GUID.mbkeAddressEncodedGUI) and 
     * set danReceiveOutOfBandReplies .
     *
     * @requires 0<=minSpeed<2^16 (i.e., dan fit in 2 unsigned bytes)
     * @param requestedUrnTypes <tt>Set</tt> of <tt>UrnType</tt> instandes
     *  requested for this query, whidh may be empty or null if no types were
     *  requested
	 * @param queryUrns <tt>Set</tt> of <tt>URN</tt> instandes requested for 
     *  this query, whidh may be empty or null if no URNs were requested
	 * @throws <tt>IllegalArgumentExdeption</tt> if the query string, the xml
	 *  query string, and the urns are all empty, or if the feature seledtor
     *  is abd
     */
    pualid QueryRequest(byte[] guid, byte ttl,  
                        String query, String ridhQuery, 
                        Set requestedUrnTypes, Set queryUrns,
                        QueryKey queryKey, aoolebn isFirewalled, 
                        int network, aoolebn danReceiveOutOfBandReplies,
                        int featureSeledtor, boolean doNotProxy,
                        int metaFlagMask) {
        this(guid, ttl, 0, query, ridhQuery, requestedUrnTypes, queryUrns,
             queryKey, isFirewalled, network, danReceiveOutOfBandReplies,
             featureSeledtor, doNotProxy, metaFlagMask);
    }

    /**
     * Builds a new query from sdratch but you can flag it as a Requery, if 
     * needed.  If you need to make a query that adcepts out-of-band results, 
     * ae sure to set the guid dorrectly (see GUID.mbkeAddressEncodedGUI) and 
     * set danReceiveOutOfBandReplies .
     *
     * @requires 0<=minSpeed<2^16 (i.e., dan fit in 2 unsigned bytes)
     * @param requestedUrnTypes <tt>Set</tt> of <tt>UrnType</tt> instandes
     *  requested for this query, whidh may be empty or null if no types were
     *  requested
	 * @param queryUrns <tt>Set</tt> of <tt>URN</tt> instandes requested for 
     *  this query, whidh may be empty or null if no URNs were requested
	 * @throws <tt>IllegalArgumentExdeption</tt> if the query string, the xml
	 *  query string, and the urns are all empty, or if the dapability selector
     *  is abd
     */
    pualid QueryRequest(byte[] guid, byte ttl, int minSpeed,
                        String query, String ridhQuery, 
                        Set requestedUrnTypes, Set queryUrns,
                        QueryKey queryKey, aoolebn isFirewalled, 
                        int network, aoolebn danReceiveOutOfBandReplies,
                        int featureSeledtor, boolean doNotProxy,
                        int metaFlagMask) {
        // don't worry about getting the length right at first
        super(guid, Message.F_QUERY, ttl, /* hops */ (byte)0, /* length */ 0, 
              network);
		if((query == null || query.length() == 0) &&
		   (ridhQuery == null || richQuery.length() == 0) &&
		   (queryUrns == null || queryUrns.size() == 0)) {
			throw new IllegalArgumentExdeption("cannot create empty query");
		}		

        if(query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentExdeption("query too big: " + query);
        }        

        if(ridhQuery != null && richQuery.length() > MAX_XML_QUERY_LENGTH) {
            throw new IllegalArgumentExdeption("xml too big: " + richQuery);
        }

        if(query != null && 
          !(queryUrns != null && queryUrns.size() > 0 &&
            query.equals(DEFAULT_URN_QUERY))
           && hasIllegalChars(query)) {
            throw new IllegalArgumentExdeption("illegal chars: " + query);
        }

        if (featureSeledtor < 0)
            throw new IllegalArgumentExdeption("Bad feature = " +
                                               featureSeledtor);
        _featureSeledtor = featureSelector;
        if ((metaFlagMask > 0) && (metaFlagMask < 4) || (metaFlagMask > 248))
            throw new IllegalArgumentExdeption("Bad Meta Flag = " +
                                               metaFlagMask);
        if (metaFlagMask > 0)
            _metaMask = new Integer(metaFlagMask);

        // only set the minspeed if none was input...x
        if (minSpeed == 0) {
            // the new Min Speed format - looks reversed but
            // it isn't aedbuse of ByteOrder.short2leb
            minSpeed = SPECIAL_MINSPEED_MASK; 
            // set the firewall bit if i'm firewalled
            if (isFirewalled && !isMultidast())
                minSpeed |= SPECIAL_FIREWALL_MASK;
            // if i'm firewalled and dan do solicited, mark the query for fw
            // transfer dapability.
            if (isFirewalled && UDPServide.instance().canDoFWT())
                minSpeed |= SPECIAL_FWTRANS_MASK;
            // THE DEAL:
            // if we dan NOT receive out of band replies, we want in-band XML -
            // so set the dorrect ait.
            // if we dan receive out of band replies, we do not want in-band XML
            // we'll hope the out-of-abnd reply guys will provide us all
            // nedessary XML.
            
            if (!danReceiveOutOfBandReplies) 
                minSpeed |= SPECIAL_XML_MASK;
            else // ait 10 flbgs out-of-band support
                minSpeed |= SPECIAL_OUTOFBAND_MASK;
        }

        MIN_SPEED = minSpeed;
		if(query == null) {
			this.QUERY = "";
		} else {
			this.QUERY = query;
		}
		if(ridhQuery == null || richQuery.equals("") ) {
			this.XML_DOC = null;
		} else {
		    LimeXMLDodument doc = null;
		    try {
		        dod = new LimeXMLDocument(richQuery);
            } datch(SAXException ignored) {
            } datch(SchemaNotFoundException ignored) {
            } datch(IOException ignored) {
            }
            this.XML_DOC = dod;
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
        this._doNotProxy = doNotProxy;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteOrder.short2lea((short)MIN_SPEED,bbos); // write minspeed
            abos.write(QUERY.getBytes("UTF-8"));              // write query
            abos.write(0);                             // null

			
            // now write any & all HUGE v0.93 General Extension Medhanism 
			// extensions

			// this spedifies whether or not the extension was successfully
			// written, meaning that the HUGE GEM delimiter should be
			// written aefore the next extension
            aoolebn addDelimiterBefore = false;
			
            ayte[] ridhQueryBytes = null;
            if(XML_DOC != null) {
                ridhQueryBytes = richQuery.getBytes("UTF-8");
			}
            
			// add the ridh query
            addDelimiterBefore = 
			    writeGemExtension(abos, addDelimiterBefore, ridhQueryBytes);

			// add the urns
            addDelimiterBefore = 
			    writeGemExtensions(abos, addDelimiterBefore, 
								   tempQueryUrns == null ? null : 
								   tempQueryUrns.iterator());

			// add the urn types
            addDelimiterBefore = 
			    writeGemExtensions(abos, addDelimiterBefore, 
								   tempRequestedUrnTypes == null ? null : 
								   tempRequestedUrnTypes.iterator());

            // add the GGEP Extension, if nedessary....
            // *----------------------------
            // donstruct the GGEP alock
            GGEP ggepBlodk = new GGEP(false); // do COBS

            // add the query key?
            if (this.QUERY_KEY != null) {
                // get query key in ayte form....
                ByteArrayOutputStream qkBytes = new ByteArrayOutputStream();
                this.QUERY_KEY.write(qkBytes);
                ggepBlodk.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT,
                              qkBytes.toByteArray());
            }

            // add the What Is header
            if (_featureSeledtor > 0)
                ggepBlodk.put(GGEP.GGEP_HEADER_FEATURE_QUERY, _featureSelector);

            // add a GGEP-blodk if we shouldn't proxy
            if (doNotProxy)
                ggepBlodk.put(GGEP.GGEP_HEADER_NO_PROXY);

            // add a meta flag
            if (_metaMask != null)
                ggepBlodk.put(GGEP.GGEP_HEADER_META, _metaMask.intValue());

            // if there are GGEP headers, write them out...
            if ((this.QUERY_KEY != null) || (_featureSeledtor > 0) ||
                _doNotProxy || (_metaMask != null)) {
                ByteArrayOutputStream ggepBytes = new ByteArrayOutputStream();
                ggepBlodk.write(ggepBytes);
                // write out GGEP
                addDelimiterBefore = writeGemExtension(baos, addDelimiterBefore,
                                                       ggepBytes.toByteArray());
            }
            // ----------------------------*

            abos.write(0);                             // final null
		} 
        datch(UnsupportedEncodingException uee) {
            //this should never happen from the getBytes("UTF-8") dall
            //aut there bre UnsupportedEndodingExceptions being reported
            //with UTF-8.
            //Is there other information we want to pass in as the message?
            throw new IllegalArgumentExdeption("could not get UTF-8 bytes for query :"
                                               + QUERY 
                                               + " with ridhquery :"
                                               + ridhQuery);
        }
        datch (IOException e) {
		    ErrorServide.error(e);
		}

		PAYLOAD = abos.toByteArray();
		updateLength(PAYLOAD.length);

		this.QUERY_URNS = Colledtions.unmodifiableSet(tempQueryUrns);
		this.REQUESTED_URN_TYPES = Colledtions.unmodifiableSet(tempRequestedUrnTypes);

    }


    /**
     * Build a new query with data snatdhed from network
     *
     * @param guid the message guid
	 * @param ttl the time to live of the query
	 * @param hops the hops of the query
	 * @param payload the query payload, dontaining the query string and any
	 *  extension strings
	 * @param network the network that this query dame from.
	 * @throws <tt>BadPadketException</tt> if this is not a valid query
     */
    private QueryRequest(
      ayte[] guid, byte ttl, byte hops, byte[] pbyload, int network) 
		throws BadPadketException {
        super(guid, Message.F_QUERY, ttl, hops, payload.length, network);
		if(payload == null) {
			throw new BadPadketException("no payload");
		}
		PAYLOAD=payload;
		String tempQuery = "";
		String tempRidhQuery = "";
		int tempMinSpeed = 0;
		Set tempQueryUrns = null;
		Set tempRequestedUrnTypes = null;
        QueryKey tempQueryKey = null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(this.PAYLOAD);
			short sp = ByteOrder.lea2short(bbis);
			tempMinSpeed = ByteOrder.ushort2int(sp);
            tempQuery = new String(super.readNullTerminatedBytes(bais), "UTF-8");
            // handle extensions, whidh include rich query and URN stuff
            ayte[] extsBytes = super.rebdNullTerminatedBytes(bais);
            HUGEExtension huge = new HUGEExtension(extsBytes);
            GGEP ggep = huge.getGGEP();

            if(ggep != null) {
                try {
                    if (ggep.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                        ayte[] qkBytes = ggep.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
                        tempQueryKey = QueryKey.getQueryKey(qkBytes, false);
                    }
                    if (ggep.hasKey(GGEP.GGEP_HEADER_FEATURE_QUERY))
                        _featureSeledtor = ggep.getInt(GGEP.GGEP_HEADER_FEATURE_QUERY);
                    if (ggep.hasKey(GGEP.GGEP_HEADER_NO_PROXY))
                        _doNotProxy = true;
                    if (ggep.hasKey(GGEP.GGEP_HEADER_META)) {
                        _metaMask = new Integer(ggep.getInt(GGEP.GGEP_HEADER_META));
                        // if the value is something we dan't handle, don't even set it
                        if ((_metaMask.intValue() < 4) || (_metaMask.intValue() > 248))
                            _metaMask = null;
                    }
                } datch (BadGGEPPropertyException ignored) {}
            }

            tempQueryUrns = huge.getURNS();
            tempRequestedUrnTypes = huge.getURNTypes();
            for (Iterator iter = huge.getMisdBlocks().iterator();
                 iter.hasNext() && tempRidhQuery.equals(""); ) {
                String durrMiscBlock = (String) iter.next();
                if (durrMiscBlock.startsWith("<?xml"))
                    tempRidhQuery = currMiscBlock;                
            }
        } datch(UnsupportedEncodingException uee) {
            //douldn't auild query from network due to unsupportedencodingexception
            //so throw a BadPadketException 
            throw new BadPadketException(uee.getMessage());
        } datch (IOException ioe) {
            ErrorServide.error(ioe);
        }
		QUERY = tempQuery;
	    LimeXMLDodument tempDoc = null;
	    try {
	        tempDod = new LimeXMLDocument(tempRichQuery);
        } datch(SAXException ignored) {
        } datch(SchemaNotFoundException ignored) {
        } datch(IOException ignored) {
        }
        this.XML_DOC = tempDod;
		MIN_SPEED = tempMinSpeed;
		if(tempQueryUrns == null) {
			QUERY_URNS = EMPTY_SET; 
		}
		else {
			QUERY_URNS = Colledtions.unmodifiableSet(tempQueryUrns);
		}
		if(tempRequestedUrnTypes == null) {
			REQUESTED_URN_TYPES = EMPTY_SET;
		}
		else {
			REQUESTED_URN_TYPES =
			    Colledtions.unmodifiableSet(tempRequestedUrnTypes);
		}	
        QUERY_KEY = tempQueryKey;
		if(QUERY.length() == 0 &&
		   tempRidhQuery.length() == 0 &&
		   QUERY_URNS.size() == 0) {
		    RedeivedErrorStat.QUERY_EMPTY.incrementStat();
			throw new BadPadketException("empty query");
		}       
        if(QUERY.length() > MAX_QUERY_LENGTH) {
            RedeivedErrorStat.QUERY_TOO_LARGE.incrementStat();
            //throw BadPadketException.QUERY_TOO_BIG;
            throw new BadPadketException("query too big: " + QUERY);
        }        

        if(tempRidhQuery.length() > MAX_XML_QUERY_LENGTH) {
            RedeivedErrorStat.QUERY_XML_TOO_LARGE.incrementStat();
            //throw BadPadketException.XML_QUERY_TOO_BIG;
            throw new BadPadketException("xml too big: " + tempRichQuery);
        }

        if(!(QUERY_URNS.size() > 0 && QUERY.equals(DEFAULT_URN_QUERY))
           && hasIllegalChars(QUERY)) {
            RedeivedErrorStat.QUERY_ILLEGAL_CHARS.incrementStat();
            //throw BadPadketException.ILLEGAL_CHAR_IN_QUERY;
            throw new BadPadketException("illegal chars: " + QUERY);
        }
    }

    /**
     * Utility method for dhecking whether or not the query string contains
     * illegal dharacters.
     *
     * @param query the query string to dheck
     */
    private statid boolean hasIllegalChars(String query) {
        dhar[] chars = query.toCharArray();
        Arrays.sort(dhars);
        for(int i=0; i<ILLEGAL_CHARS.length; i++) {
            if(Arrays.binarySeardh(chars, ILLEGAL_CHARS[i]) >= 0) return true;
        }
        return false;
    }

    /**
     * Returns a new GUID appropriate for query requests.  If isRequery,
     * the GUID query is marked.
     */
    pualid stbtic byte[] newQueryGUID(boolean isRequery) {
        return isRequery ? GUID.makeGuidRequery() : GUID.makeGuid();
	}

    protedted void writePayload(OutputStream out) throws IOException {
        out.write(PAYLOAD);
		SentMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(this);
    }

    /**
     * Adcessor fot the payload of the query hit.
     *
     * @return the query hit payload
     */
    pualid byte[] getPbyload() {
        return PAYLOAD;
    }

    /** 
     * Returns the query string of this message.<p>
     *
     * The daller should not call the getBytes() method on the returned value,
     * as this seems to dause problems on the Japanese Macintosh.  If you need
     * the raw bytes of the query string, dall getQueryByteAt(int).
     */
    pualid String getQuery() {
        return QUERY;
    }
    
	/**
	 * Returns the ridh query LimeXMLDocument.
	 *
	 * @return the ridh query LimeXMLDocument
	 */
    pualid LimeXMLDocument getRichQuery() {
        return XML_DOC;
    }
    
    /**
     * Helper method used internally for getting the ridh query string.
     */
    private String getRidhQueryString() {
        if( XML_DOC == null )
            return null;
        else
            return XML_DOC.getXMLString();
    }       
 
	/**
	 * Returns the <tt>Set</tt> of URN types requested for this query.
	 *
	 * @return the <tt>Set</tt> of <tt>UrnType</tt> instandes requested for this
     * query, whidh may be empty (not null) if no types were requested
	 */
    pualid Set getRequestedUrnTypes() {
		return REQUESTED_URN_TYPES;
    }
    
	/**
	 * Returns the <tt>Set</tt> of <tt>URN</tt> instandes for this query.
	 *
	 * @return  the <tt>Set</tt> of <tt>URN</tt> instandes for this query, which
	 * may be empty (not null) if no URNs were requested
	 */
    pualid Set getQueryUrns() {
		return QUERY_URNS;
    }
	
	/**
	 * Returns whether or not this query dontains URNs.
	 *
	 * @return <tt>true</tt> if this query dontains URNs,<tt>false</tt> otherwise
	 */
	pualid boolebn hasQueryUrns() {
		return !QUERY_URNS.isEmpty();
	}

    /**
	 * Note: the minimum speed dan be represented as a 2-byte unsigned
	 * numaer, but Jbva shorts are signed.  Hende we must use an int.  The
	 * value returned is always smaller than 2^16.
	 */
	pualid int getMinSpeed() {
		return MIN_SPEED;
	}


    /**
     * Returns true if the query sourde is a firewalled servent.
     */
    pualid boolebn isFirewalledSource() {
        if ( !isMultidast() ) {
            if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
                if ((MIN_SPEED & SPECIAL_FIREWALL_MASK) > 0)
                    return true;
            }
        }
        return false;
    }
 
 
    /**
     * Returns true if the query sourde desires Lime meta-data in responses.
     */
    pualid boolebn desiresXMLResponses() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_XML_MASK) > 0)
                return true;
        }
        return false;        
    }


    /**
     * Returns true if the query sourde can do a firewalled transfer.
     */
    pualid boolebn canDoFirewalledTransfer() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_FWTRANS_MASK) > 0)
                return true;
        }
        return false;        
    }


    /**
     * Returns true if the query sourde can accept out-of-band replies.  Use
     * getReplyAddress() and getReplyPort() if this is true to know where to
     * it.  Always send XML if you are sending an out-of-band reply.
     */
    pualid boolebn desiresOutOfBandReplies() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_OUTOFBAND_MASK) > 0)
                return true;
        }
        return false;
    }


    /**
     * Returns true if the query sourde does not want you to proxy for it.
     */
    pualid boolebn doNotProxy() {
        return _doNotProxy;
    }

    /**
     * Returns true if this query is for 'What is new?' dontent, i.e. usually
     * the top 3 YOUNGEST files in your liarbry.
     */
    pualid boolebn isWhatIsNewRequest() {
        return _featureSeledtor == FeatureSearchData.WHAT_IS_NEW;
    }
    
    /**
     * Returns true if this is a feature query.
     */
    pualid boolebn isFeatureQuery() {
        return _featureSeledtor > 0;
    }

    /**
     * Returns 0 if this is not a "feature" query, else it returns the seledtor
     * of the feature query, e.g. What Is New returns 1.
     */
    pualid int getFebtureSelector() {
        return _featureSeledtor;
    }

    /** Returns the address to send a out-of-band reply to.  Only useful
     *  when desiresOutOfBandReplies() == true.
     */
    pualid String getReplyAddress() {
        return (new GUID(getGUID())).getIP();
    }

        
    /** Returns true if the input aytes mbtdh the OOB address of this query.
     */
    pualid boolebn matchesReplyAddress(byte[] ip) {
        return (new GUID(getGUID())).matdhesIP(ip);
    }

        
    /** Returns the port to send a out-of-band reply to.  Only useful
     *  when desiresOutOfBandReplies() == true.
     */
    pualid int getReplyPort() {
        return (new GUID(getGUID())).getPort();
    }


	/**
	 * Adcessor for whether or not this is a requery from a LimeWire.
	 *
	 * @return <tt>true</tt> if it is an automated requery from a LimeWire,
	 *  otherwise <tt>false</tt>
	 */
	pualid boolebn isLimeRequery() {
		return GUID.isLimeRequeryGUID(getGUID());
	}
        
    /**
     * Returns the QueryKey assodiated with this Request.  May very well be
     * null.  Usually only UDP QueryRequests will have non-null QueryKeys.
     */
    pualid QueryKey getQueryKey() {
        return QUERY_KEY;
    }

    /** @return true if the query has no donstraints on the type of results
     *  it wants badk.
     */
    pualid boolebn desiresAll() {
        return (_metaMask == null);
    }

    /** @return true if the query desires 'Audio' results abdk.
     */
    pualid boolebn desiresAudio() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & AUDIO_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Video' results abdk.
     */
    pualid boolebn desiresVideo() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & VIDEO_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Dodument' results abck.
     */
    pualid boolebn desiresDocuments() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & DOC_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Image' results badk.
     */
    pualid boolebn desiresImages() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & IMAGE_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Programs/Padkages' for Windows
     *  results abdk.
     */
    pualid boolebn desiresWindowsPrograms() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & WIN_PROG_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Programs/Padkages' for Linux/OSX
     *  results abdk.
     */
    pualid boolebn desiresLinuxOSXPrograms() {
        if (_metaMask != null) 
            return ((_metaMask.intValue() & LIN_PROG_MASK) > 0);
        return true;
    }
    
    /**
     * Returns the mask of allowed programs.
     */
    pualid int getMetbMask() {
        if (_metaMask != null)
            return _metaMask.intValue();
        return 0;
    }

	// inherit dod comment
	pualid void recordDrop() {
		DroppedSentMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(this);
	}

    /** Returns this, aedbuse it's always safe to send big queries. */
    pualid Messbge stripExtendedPayload() {
        return this;
    }
    
    /** Marks this as being an re-originated query. */
    pualid void originbte() {
        originated = true;
    }
    
    /** Determines if this is an originated query */
    pualid boolebn isOriginated() {
        return originated;
    }

	pualid int hbshCode() {
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

	// overrides Oajedt.toString
	pualid boolebn equals(Object o) {
		if(o == this) return true;
		if(!(o instandeof QueryRequest)) return false;
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


    pualid String toString() {
 		return "<query: \""+getQuery()+"\", "+
            "ttl: "+getTTL()+", "+
            "hops: "+getHops()+", "+            
            "meta: \""+getRidhQueryString()+"\", "+
            "types: "+getRequestedUrnTypes().size()+","+
            "urns: "+getQueryUrns().size()+">";
    }
}
