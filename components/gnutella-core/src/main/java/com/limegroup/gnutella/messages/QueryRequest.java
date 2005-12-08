pbckage com.limegroup.gnutella.messages;

import jbva.io.ByteArrayInputStream;
import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.io.Serializable;
import jbva.io.UnsupportedEncodingException;
import jbva.util.Arrays;
import jbva.util.Collections;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Set;

import org.xml.sbx.SAXException;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.FileManager;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.MediaType;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.UDPService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.UrnType;
import com.limegroup.gnutellb.guess.QueryKey;
import com.limegroup.gnutellb.settings.SearchSettings;
import com.limegroup.gnutellb.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutellb.statistics.ReceivedErrorStat;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.I18NConvert;
import com.limegroup.gnutellb.util.StringUtils;
import com.limegroup.gnutellb.xml.LimeXMLDocument;
import com.limegroup.gnutellb.xml.SchemaNotFoundException;

/**
 * This clbss creates Gnutella query messages, either from scratch, or
 * from dbta read from the network.  Queries can contain query strings, 
 * XML query strings, URNs, etc.  The minimum speed field is now used
 * for bit flbgs to indicate such things as the firewalled status of
 * the querier.<p>
 * 
 * This clbss also has factory constructors for requeries originated
 * from this LimeWire.  These requeries hbve specially marked GUIDs
 * thbt allow us to identify them as requeries.
 */
public clbss QueryRequest extends Message implements Serializable{

    // these specs mby seem backwards, but they are not - ByteOrder.short2leb
    // puts the low-order byte first, so over the network 0x0080 would look
    // like 0x8000
    public stbtic final int SPECIAL_MINSPEED_MASK  = 0x0080;
    public stbtic final int SPECIAL_FIREWALL_MASK  = 0x0040;
    public stbtic final int SPECIAL_XML_MASK       = 0x0020;
    public stbtic final int SPECIAL_OUTOFBAND_MASK = 0x0004;
    public stbtic final int SPECIAL_FWTRANS_MASK   = 0x0002;

    /** Mbsk for audio queries - input 0 | AUDIO_MASK | .... to specify
     *  budio responses.
     */
    public stbtic final int AUDIO_MASK  = 0x0004;
    /** Mbsk for video queries - input 0 | VIDEO_MASK | .... to specify
     *  video responses.
     */
    public stbtic final int VIDEO_MASK  = 0x0008; 
    /** Mbsk for document queries - input 0 | DOC_MASK | .... to specify
     *  document responses.
     */
    public stbtic final int DOC_MASK  = 0x0010;
    /** Mbsk for image queries - input 0 | IMAGE_MASK | .... to specify
     *  imbge responses.
     */
    public stbtic final int IMAGE_MASK  = 0x0020;
    /** Mbsk for windows programs/packages queries - input 0 | WIN_PROG_MASK
     *  | .... to specify windows progrbms/packages responses.
     */
    public stbtic final int WIN_PROG_MASK  = 0x0040;
    /** Mbsk for linux/osx programs/packages queries - input 0 | LIN_PROG_MASK
     *  | .... to specify linux/osx progrbms/packages responses.
     */
    public stbtic final int LIN_PROG_MASK  = 0x0080;

    public stbtic final String WHAT_IS_NEW_QUERY_STRING = "WhatIsNewXOXO";

    /**
     * The pbyload for the query -- includes the query string, the
     * XML query, bny URNs, GGEP, etc.
     */
    privbte final byte[] PAYLOAD;

    /**
     * The "min speed" field.  This wbs originally used to specify
     * b minimum speed for returned results, but it was never really
     * used this wby.  As of LimeWire 3.0 (02/2003), the bits of 
     * this field were chbnged to specify things like the firewall
     * stbtus of the querier.
     */
    privbte final int MIN_SPEED;

    /**
     * The query string.
     */
    privbte final String QUERY;
    
    /**
     * The LimeXMLDocument of the rich query.
     */
    privbte final LimeXMLDocument XML_DOC;

    /**
     * The febture that this query is.
     */
    privbte int _featureSelector = 0;

    /**
     * Whether or not the GGEP hebder for Do Not Proxy was found.
     */
    privbte boolean _doNotProxy = false;

    // HUGE v0.93 fields
    /** 
	 * The types of requested URNs.
	 */
    privbte final Set /* of UrnType */ REQUESTED_URN_TYPES;

    /** 
	 * Specific URNs requested.
	 */
    privbte final Set /* of URN */ QUERY_URNS;

    /**
     * The Query Key bssociated with this query -- can be null.
     */
    privbte final QueryKey QUERY_KEY;

    /**
     * The flbg in the 'M' GGEP extension - if non-null, the query is requesting
     * only certbin types.
     */
    privbte Integer _metaMask = null;
    
    /**
     * If we're re-originbted this query for a leaf.  This can be set/read
     * bfter creation.
     */
    privbte boolean originated = false;

	/**
	 * Cbched hash code for this instance.
	 */
	privbte volatile int _hashCode = 0;

	/**
	 * Constbnt for an empty, unmodifiable <tt>Set</tt>.  This is necessary
	 * becbuse Collections.EMPTY_SET is not serializable in the collections 
	 * 1.1 implementbtion.
	 */
	privbte static final Set EMPTY_SET = 
		Collections.unmodifibbleSet(new HashSet());

    /**
     * Constbnt for the default query TTL.
     */
    privbte static final byte DEFAULT_TTL = 6;

    /**
     * Cbched illegal characters in search strings.
     */
    privbte static final char[] ILLEGAL_CHARS =
        SebrchSettings.ILLEGAL_CHARS.getValue();


    /**
     * Cbche the maximum length for queries, in bytes.
     */
    privbte static final int MAX_QUERY_LENGTH =
        SebrchSettings.MAX_QUERY_LENGTH.getValue();

    /**
     * Cbche the maximum length for XML queries, in bytes.
     */
    privbte static final int MAX_XML_QUERY_LENGTH =
        SebrchSettings.MAX_XML_QUERY_LENGTH.getValue();

    /**
     * The mebningless query string we put in URN queries.  Needed because
     * LimeWire's drop empty queries....
     */
    privbte static final String DEFAULT_URN_QUERY = "\\";

	/**
	 * Crebtes a new requery for the specified SHA1 value.
	 *
	 * @pbram sha1 the <tt>URN</tt> of the file to search for
	 * @return b new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>shb1</tt> argument
	 *  is <tt>null</tt>
	 */
	public stbtic QueryRequest createRequery(URN sha1) {
        if(shb1 == null) {
            throw new NullPointerException("null shb1");
        }
		Set shb1Set = new HashSet();
		shb1Set.add(sha1);
        return new QueryRequest(newQueryGUID(true), DEFAULT_TTL, 
                                DEFAULT_URN_QUERY, "", 
                                UrnType.SHA1_SET, shb1Set, null,
                                !RouterService.bcceptedIncomingConnection(),
								Messbge.N_UNKNOWN, false, 0, false, 0);

	}

	/**
	 * Crebtes a new query for the specified SHA1 value.
	 *
	 * @pbram sha1 the <tt>URN</tt> of the file to search for
	 * @return b new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>shb1</tt> argument
	 *  is <tt>null</tt>
	 */
	public stbtic QueryRequest createQuery(URN sha1) {
        if(shb1 == null) {
            throw new NullPointerException("null shb1");
        }
		Set shb1Set = new HashSet();
		shb1Set.add(sha1);
        return new QueryRequest(newQueryGUID(fblse), DEFAULT_TTL, 
                                DEFAULT_URN_QUERY, "",  UrnType.SHA1_SET, 
                                shb1Set, null,
                                !RouterService.bcceptedIncomingConnection(),
								Messbge.N_UNKNOWN, false, 0, false, 0);

	}
	/**
	 * Crebtes a new requery for the specified SHA1 value with file name 
     * thrown in for good mebsure (or at least until \ works as a query).
	 *
	 * @pbram sha1 the <tt>URN</tt> of the file to search for
	 * @return b new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>shb1</tt> argument
	 *  is <tt>null</tt>
	 */
	public stbtic QueryRequest createRequery(URN sha1, String filename) {
        if(shb1 == null) {
            throw new NullPointerException("null shb1");
        }
        if(filenbme == null) {
            throw new NullPointerException("null query");
        }
		if(filenbme.length() == 0) {
			filenbme = DEFAULT_URN_QUERY;
		}
		Set shb1Set = new HashSet();
		shb1Set.add(sha1);
        return new QueryRequest(newQueryGUID(true), DEFAULT_TTL, filenbme, "", 
                                UrnType.SHA1_SET, shb1Set, null,
                                !RouterService.bcceptedIncomingConnection(),
								Messbge.N_UNKNOWN, false, 0, false, 0);

	}

	/**
	 * Crebtes a new query for the specified SHA1 value with file name 
     * thrown in for good mebsure (or at least until \ works as a query).
	 *
	 * @pbram sha1 the <tt>URN</tt> of the file to search for
	 * @return b new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>shb1</tt> argument
	 *  is <tt>null</tt>
	 */
	public stbtic QueryRequest createQuery(URN sha1, String filename) {
        if(shb1 == null) {
            throw new NullPointerException("null shb1");
        }
        if(filenbme == null) {
            throw new NullPointerException("null query");
        }
		if(filenbme.length() == 0) {
			filenbme = DEFAULT_URN_QUERY;
		}
		Set shb1Set = new HashSet();
		shb1Set.add(sha1);
        return new QueryRequest(newQueryGUID(fblse), DEFAULT_TTL, filename, "", 
                                UrnType.SHA1_SET, shb1Set, null,
                                !RouterService.bcceptedIncomingConnection(),
								Messbge.N_UNKNOWN, false, 0, false, 0);

	}

	/**
	 * Crebtes a new requery for the specified SHA1 value and the specified
	 * firewbll boolean.
	 *
	 * @pbram sha1 the <tt>URN</tt> of the file to search for
	 * @pbram ttl the time to live (ttl) of the query
	 * @return b new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>shb1</tt> argument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the ttl value is
	 *  negbtive or greater than the maximum allowed value
	 */
	public stbtic QueryRequest createRequery(URN sha1, byte ttl) {
        if(shb1 == null) {
            throw new NullPointerException("null shb1");
        } 
		if(ttl <= 0 || ttl > 6) {
			throw new IllegblArgumentException("invalid TTL: "+ttl);
		}
		Set shb1Set = new HashSet();
		shb1Set.add(sha1);
        return new QueryRequest(newQueryGUID(true), ttl, DEFAULT_URN_QUERY, "", 
                                UrnType.SHA1_SET, shb1Set, null,
                                !RouterService.bcceptedIncomingConnection(),
								Messbge.N_UNKNOWN, false, 0, false, 0);
	}
	
	/**
	 * Crebtes a new query for the specified UrnType set and URN set.
	 *
	 * @pbram urnTypeSet the <tt>Set</tt> of <tt>UrnType</tt>s to request.
	 * @pbram urnSet the <tt>Set</tt> of <tt>URNs</tt>s to request.
	 * @return b new <tt>QueryRequest</tt> for the specied UrnTypes and URNs
	 * @throws <tt>NullPointerException</tt> if either sets bre null.
	 */
	public stbtic QueryRequest createQuery(Set urnTypeSet, Set urnSet) {
	    if(urnSet == null)
	        throw new NullPointerException("null urnSet");
	    if(urnTypeSet == null)
	        throw new NullPointerException("null urnTypeSet");
	    return new QueryRequest(newQueryGUID(fblse), DEFAULT_TTL, 
                                DEFAULT_URN_QUERY, "",
	                            urnTypeSet, urnSet, null,
	                            !RouterService.bcceptedIncomingConnection(),
	                            Messbge.N_UNKNOWN, false, 0, false, 0);
    }
	    
	
	/**
	 * Crebtes a requery for when we don't know the hash of the file --
	 * we don't know the hbsh.
	 *
	 * @pbram query the query string
	 * @return b new <tt>QueryRequest</tt> for the specified query
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> brgument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the <tt>query</tt>
	 *  brgument is zero-length (empty)
	 */
	public stbtic QueryRequest createRequery(String query) {
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(query.length() == 0) {
			throw new IllegblArgumentException("empty query");
		}
		return new QueryRequest(newQueryGUID(true), query);
	}


	/**
	 * Crebtes a new query for the specified file name, with no XML.
	 *
	 * @pbram query the file name to search for
	 * @return b new <tt>QueryRequest</tt> for the specified query
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> brgument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the <tt>query</tt>
	 *  brgument is zero-length (empty)
	 */
	public stbtic QueryRequest createQuery(String query) {
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(query.length() == 0) {
			throw new IllegblArgumentException("empty query");
		}
		return new QueryRequest(newQueryGUID(fblse), query);
	}                           
    

	/**
	 * Crebtes a new query for the specified file name and the designated XML.
	 *
	 * @pbram query the file name to search for
     * @pbram guid I trust that this is a address encoded guid.  Your loss if
     * it isn't....
	 * @return b new <tt>QueryRequest</tt> for the specified query that has
	 * encoded the input ip bnd port into the GUID and appropriate marked the
	 * query to signify out of bbnd support.
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> brgument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the <tt>query</tt>
	 *  brgument is zero-length (empty)
	 */
    public stbtic QueryRequest createOutOfBandQuery(byte[] guid, String query, 
                                                    String xmlQuery) {
        query = I18NConvert.instbnce().getNorm(query);
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(xmlQuery == null) {
			throw new NullPointerException("null xml query");
		}
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegblArgumentException("empty query");
		}
		if(xmlQuery.length() != 0 && !xmlQuery.stbrtsWith("<?xml")) {
			throw new IllegblArgumentException("invalid XML");
		}
        return new QueryRequest(guid, DEFAULT_TTL, query, xmlQuery, true);
    }                                

	/**
	 * Crebtes a new query for the specified file name and the designated XML.
	 *
	 * @pbram query the file name to search for
     * @pbram guid I trust that this is a address encoded guid.  Your loss if
     * it isn't....
     * @pbram type can be null - the type of results you want.
	 * @return b new <tt>QueryRequest</tt> for the specified query that has
	 * encoded the input ip bnd port into the GUID and appropriate marked the
	 * query to signify out of bbnd support AND specifies a file type category.
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> brgument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the <tt>query</tt>
	 *  brgument is zero-length (empty)
	 */
    public stbtic QueryRequest createOutOfBandQuery(byte[] guid, String query, 
                                                    String xmlQuery,
                                                    MedibType type) {
        query = I18NConvert.instbnce().getNorm(query);
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(xmlQuery == null) {
			throw new NullPointerException("null xml query");
		}
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegblArgumentException("empty query");
		}
		if(xmlQuery.length() != 0 && !xmlQuery.stbrtsWith("<?xml")) {
			throw new IllegblArgumentException("invalid XML");
		}
        return new QueryRequest(guid, DEFAULT_TTL, query, xmlQuery, true, type);
    }                                

	/**
	 * Crebtes a new query for the specified file name, with no XML.
	 *
	 * @pbram query the file name to search for
	 * @return b new <tt>QueryRequest</tt> for the specified query that has
	 * encoded the input ip bnd port into the GUID and appropriate marked the
	 * query to signify out of bbnd support.
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> brgument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the <tt>query</tt>
	 *  brgument is zero-length (empty)
	 */
    public stbtic QueryRequest createOutOfBandQuery(String query,
                                                    byte[] ip, int port) {
        byte[] guid = GUID.mbkeAddressEncodedGuid(ip, port);
        return QueryRequest.crebteOutOfBandQuery(guid, query, "");
    }                                

    /**
     * Crebtes a new 'What is new'? query with the specified guid and ttl.
     * @pbram ttl the desired ttl of the query.
     * @pbram guid the desired guid of the query.
     */
    public stbtic QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl) {
        return crebteWhatIsNewQuery(guid, ttl, null);
    }
   

    /**
     * Crebtes a new 'What is new'? query with the specified guid and ttl.
     * @pbram ttl the desired ttl of the query.
     * @pbram guid the desired guid of the query.
     */
    public stbtic QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl,
                                                    MedibType type) {
        if (ttl < 1) throw new IllegblArgumentException("Bad TTL.");
        return new QueryRequest(guid, ttl, WHAT_IS_NEW_QUERY_STRING,
                                "", null, null, null,
                                !RouterService.bcceptedIncomingConnection(),
                                Messbge.N_UNKNOWN, false, 
                                FebtureSearchData.WHAT_IS_NEW, false, 
                                getMetbFlag(type));
    }
   

    /**
     * Crebtes a new 'What is new'? OOB query with the specified guid and ttl.
     * @pbram ttl the desired ttl of the query.
     * @pbram guid the desired guid of the query.
     */
    public stbtic QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl) {
        return crebteWhatIsNewOOBQuery(guid, ttl, null);
    }
   

    /**
     * Crebtes a new 'What is new'? OOB query with the specified guid and ttl.
     * @pbram ttl the desired ttl of the query.
     * @pbram guid the desired guid of the query.
     */
    public stbtic QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl,
                                                       MedibType type) {
        if (ttl < 1) throw new IllegblArgumentException("Bad TTL.");
        return new QueryRequest(guid, ttl, WHAT_IS_NEW_QUERY_STRING,
                                "", null, null, null,
                                !RouterService.bcceptedIncomingConnection(),
                                Messbge.N_UNKNOWN, true, FeatureSearchData.WHAT_IS_NEW,
                                fblse, getMetaFlag(type));
    }
   

	/**
	 * Crebtes a new query for the specified file name, with no XML.
	 *
	 * @pbram query the file name to search for
	 * @return b new <tt>QueryRequest</tt> for the specified query
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> brgument
	 *  is <tt>null</tt> or if the <tt>xmlQuery</tt> brgument is 
	 *  <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the <tt>query</tt>
	 *  brgument and the xml query are both zero-length (empty)
	 */
	public stbtic QueryRequest createQuery(String query, String xmlQuery) {
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(xmlQuery == null) {
			throw new NullPointerException("null xml query");
		}
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegblArgumentException("empty query");
		}
		if(xmlQuery.length() != 0 && !xmlQuery.stbrtsWith("<?xml")) {
			throw new IllegblArgumentException("invalid XML");
		}
		return new QueryRequest(newQueryGUID(fblse), query, xmlQuery);
	}


	/**
	 * Crebtes a new query for the specified file name, with no XML.
	 *
	 * @pbram query the file name to search for
	 * @pbram ttl the time to live (ttl) of the query
	 * @return b new <tt>QueryRequest</tt> for the specified query and ttl
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> brgument
	 *  is <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the <tt>query</tt>
	 *  brgument is zero-length (empty)
	 * @throws <tt>IllegblArgumentException</tt> if the ttl value is
	 *  negbtive or greater than the maximum allowed value
	 */
	public stbtic QueryRequest createQuery(String query, byte ttl) {
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(query.length() == 0) {
			throw new IllegblArgumentException("empty query");
		}
		if(ttl <= 0 || ttl > 6) {
			throw new IllegblArgumentException("invalid TTL: "+ttl);
		}
		return new QueryRequest(newQueryGUID(fblse), ttl, query);
	}

	/**
	 * Crebtes a new query with the specified guid, query string, and
	 * xml query string.
	 *
	 * @pbram guid the message GUID for the query
	 * @pbram query the query string
	 * @pbram xmlQuery the xml query string
	 * @return b new <tt>QueryRequest</tt> for the specified query, xml
	 *  query, bnd guid
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> brgument
	 *  is <tt>null</tt>, if the <tt>xmlQuery</tt> brgument is <tt>null</tt>,
	 *  or if the <tt>guid</tt> brgument is <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the guid length is
	 *  not 16, if both the query strings bre empty, or if the XML does
	 *  not bppear to be valid
	 */
	public stbtic QueryRequest createQuery(byte[] guid, String query, 
										   String xmlQuery) {
        query = I18NConvert.instbnce().getNorm(query);
		if(guid == null) {
			throw new NullPointerException("null guid");
		}
		if(guid.length != 16) {
			throw new IllegblArgumentException("invalid guid length");
		}
        if(query == null) {
            throw new NullPointerException("null query");
        }
        if(xmlQuery == null) {
            throw new NullPointerException("null xml query");
        }
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegblArgumentException("empty query");
		}
		if(xmlQuery.length() != 0 && !xmlQuery.stbrtsWith("<?xml")) {
			throw new IllegblArgumentException("invalid XML");
		}
		return new QueryRequest(guid, query, xmlQuery);
	}

	/**
	 * Crebtes a new query with the specified guid, query string, and
	 * xml query string.
	 *
	 * @pbram guid the message GUID for the query
	 * @pbram query the query string
	 * @pbram xmlQuery the xml query string
	 * @return b new <tt>QueryRequest</tt> for the specified query, xml
	 *  query, bnd guid
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> brgument
	 *  is <tt>null</tt>, if the <tt>xmlQuery</tt> brgument is <tt>null</tt>,
	 *  or if the <tt>guid</tt> brgument is <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the guid length is
	 *  not 16, if both the query strings bre empty, or if the XML does
	 *  not bppear to be valid
	 */
	public stbtic QueryRequest createQuery(byte[] guid, String query, 
										   String xmlQuery, MedibType type) {
        query = I18NConvert.instbnce().getNorm(query);
		if(guid == null) {
			throw new NullPointerException("null guid");
		}
		if(guid.length != 16) {
			throw new IllegblArgumentException("invalid guid length");
		}
        if(query == null) {
            throw new NullPointerException("null query");
        }
        if(xmlQuery == null) {
            throw new NullPointerException("null xml query");
        }
		if(query.length() == 0 && xmlQuery.length() == 0) {
			throw new IllegblArgumentException("empty query");
		}
		if(xmlQuery.length() != 0 && !xmlQuery.stbrtsWith("<?xml")) {
			throw new IllegblArgumentException("invalid XML");
		}
		return new QueryRequest(guid, DEFAULT_TTL, query, xmlQuery, type);
	}

	/**
	 * Crebtes a new query from the existing query with the specified
	 * ttl.
	 *
	 * @pbram qr the <tt>QueryRequest</tt> to copy
	 * @pbram ttl the new ttl
	 * @return b new <tt>QueryRequest</tt> with the specified ttl
	 */
	public stbtic QueryRequest createQuery(QueryRequest qr, byte ttl) {
	    // Construct b query request that is EXACTLY like the other query,
	    // but with b different TTL.
	    try {
	        return crebteNetworkQuery(qr.getGUID(), ttl, qr.getHops(),
	                                  qr.PAYLOAD, qr.getNetwork());
	    } cbtch(BadPacketException ioe) {
	        throw new IllegblArgumentException(ioe.getMessage());
	    }
	}

	/**
	 * Crebtes a new OOBquery from the existing query with the specified guid
     * (which should be bddress encoded).
	 *
	 * @pbram qr the <tt>QueryRequest</tt> to copy
	 * @return b new <tt>QueryRequest</tt> with the specified guid that is now
     * OOB mbrked.
     * @throws IllegblArgumentException thrown if guid is not right size of if
     * query is bbd.
	 */
	public stbtic QueryRequest createProxyQuery(QueryRequest qr, byte[] guid) {
        if (guid.length != 16)
            throw new IllegblArgumentException("bad guid size: " + guid.length);

        // i cbn't just call a new constructor, i have to recreate stuff
        byte[] newPbyload = new byte[qr.PAYLOAD.length];
        System.brraycopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);
        newPbyload[0] |= SPECIAL_OUTOFBAND_MASK;
        
        try {
            return crebteNetworkQuery(guid, qr.getTTL(), qr.getHops(), 
                                      newPbyload, qr.getNetwork());
        } cbtch (BadPacketException ioe) {
            throw new IllegblArgumentException(ioe.getMessage());
        }
	}

	/**
	 * Crebtes a new query from the existing query and loses the OOB marking.
	 *
	 * @pbram qr the <tt>QueryRequest</tt> to copy
	 * @return b new <tt>QueryRequest</tt> with no OOB marking
	 */
	public stbtic QueryRequest unmarkOOBQuery(QueryRequest qr) {
        //modify the pbyload to not be OOB.
        byte[] newPbyload = new byte[qr.PAYLOAD.length];
        System.brraycopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);
        newPbyload[0] &= ~SPECIAL_OUTOFBAND_MASK;
        newPbyload[0] |= SPECIAL_XML_MASK;
        
        try {
            return crebteNetworkQuery(qr.getGUID(), qr.getTTL(), qr.getHops(), 
                                      newPbyload, qr.getNetwork());
        } cbtch (BadPacketException ioe) {
            throw new IllegblArgumentException(ioe.getMessage());
        }
	}

    /**
     * Crebtes a new query with the specified query key for use in 
     * GUESS-style UDP queries.
     *
     * @pbram query the query string
     * @pbram key the query key
	 * @return b new <tt>QueryRequest</tt> instance with the specified 
	 *  query string bnd query key
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> brgument
	 *  is <tt>null</tt> or if the <tt>key</tt> brgument is <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the <tt>query</tt>
	 *  brgument is zero-length (empty)
     */
    public stbtic QueryRequest 
        crebteQueryKeyQuery(String query, QueryKey key) {
        if(query == null) {
            throw new NullPointerException("null query");
        }
		if(query.length() == 0) {
			throw new IllegblArgumentException("empty query");
		}
        if(key == null) {
            throw new NullPointerException("null query key");
        }
        return new QueryRequest(newQueryGUID(fblse), (byte)1, query, "", 
                                UrnType.ANY_TYPE_SET, EMPTY_SET, key,
                                !RouterService.bcceptedIncomingConnection(),
								Messbge.N_UNKNOWN, false, 0, false, 0);
    }


    /**
     * Crebtes a new query with the specified query key for use in 
     * GUESS-style UDP queries.
     *
     * @pbram sha1 the URN
     * @pbram key the query key
	 * @return b new <tt>QueryRequest</tt> instance with the specified 
	 *  URN request bnd query key
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> brgument
	 *  is <tt>null</tt> or if the <tt>key</tt> brgument is <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the <tt>query</tt>
	 *  brgument is zero-length (empty)
     */
    public stbtic QueryRequest 
        crebteQueryKeyQuery(URN sha1, QueryKey key) {
        if(shb1 == null) {
            throw new NullPointerException("null shb1");
        }
        if(key == null) {
            throw new NullPointerException("null query key");
        }
		Set shb1Set = new HashSet();
		shb1Set.add(sha1);
        return new QueryRequest(newQueryGUID(fblse), (byte) 1, DEFAULT_URN_QUERY,
                                "", UrnType.SHA1_SET, shb1Set, key,
                                !RouterService.bcceptedIncomingConnection(),
								Messbge.N_UNKNOWN, false, 0, false, 0);
    }


	/**
	 * Crebtes a new <tt>QueryRequest</tt> instance for multicast queries.	 
	 * This is necessbry due to the unique properties of multicast queries,
	 * such bs the firewalled bit not being set regardless of whether or
	 * not the node is truly firewblled/NATted to the world outside the
	 * subnet.
	 * 
	 * @pbram qr the <tt>QueryRequest</tt> instance containing all the 
	 *  dbta necessary to create a multicast query
	 * @return b new <tt>QueryRequest</tt> instance with bits set for
	 *  multicbst -- a min speed bit in particular
	 * @throws <tt>NullPointerException</tt> if the <tt>qr</tt> brgument
	 *  is <tt>null</tt> 
	 */
	public stbtic QueryRequest createMulticastQuery(QueryRequest qr) {
		if(qr == null)
			throw new NullPointerException("null query");

        //modify the pbyload to not be OOB.
        byte[] newPbyload = new byte[qr.PAYLOAD.length];
        System.brraycopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);
        newPbyload[0] &= ~SPECIAL_OUTOFBAND_MASK;
        newPbyload[0] |= SPECIAL_XML_MASK;
        
        try {
            return crebteNetworkQuery(qr.getGUID(), (byte)1, qr.getHops(),
                                      newPbyload, Message.N_MULTICAST);
        } cbtch (BadPacketException ioe) {
            throw new IllegblArgumentException(ioe.getMessage());
        }
	}

    /** 
	 * Crebtes a new <tt>QueryRequest</tt> that is a copy of the input 
	 * query, except thbt it includes the specified query key.
	 *
	 * @pbram qr the <tt>QueryRequest</tt> to use
	 * @pbram key the <tt>QueryKey</tt> to add
	 * @return b new <tt>QueryRequest</tt> from the specified query and
	 *  key
     */
	public stbtic QueryRequest 
		crebteQueryKeyQuery(QueryRequest qr, QueryKey key) {
		    
        // TODO: Copy the pbyload verbatim, except add the query-key
        //       into the GGEP section.
        return new QueryRequest(qr.getGUID(), qr.getTTL(), 
                                qr.getQuery(), qr.getRichQueryString(), 
                                qr.getRequestedUrnTypes(), qr.getQueryUrns(),
                                key, qr.isFirewblledSource(), Message.N_UNKNOWN,
                                qr.desiresOutOfBbndReplies(),
                                qr.getFebtureSelector(), false,
                                qr.getMetbMask());
	}

	/**
	 * Crebtes a new specialized <tt>QueryRequest</tt> instance for 
	 * browse host queries so thbt <tt>FileManager</tt> can understand them.
	 *
	 * @return b new <tt>QueryRequest</tt> for browse host queries
	 */
	public stbtic QueryRequest createBrowseHostQuery() {
		return new QueryRequest(newQueryGUID(fblse), (byte)1, 
				FileMbnager.INDEXING_QUERY, "", 
                UrnType.ANY_TYPE_SET, EMPTY_SET, null,
                !RouterService.bcceptedIncomingConnection(), 
				Messbge.N_UNKNOWN, false, 0, false, 0);
	}
	

	/**
	 * Speciblized constructor used to create a query without the firewalled
	 * bit set.  This should primbrily be used for testing.
	 *
	 * @pbram query the query string
	 * @return b new <tt>QueryRequest</tt> with the specified query string
	 *  bnd without the firewalled bit set
	 */
	public stbtic QueryRequest 
		crebteNonFirewalledQuery(String query, byte ttl) {
		return new QueryRequest(newQueryGUID(fblse), ttl, 
								query, "", 
                                UrnType.ANY_TYPE_SET, EMPTY_SET, null,
                                fblse, Message.N_UNKNOWN, false, 0, false, 0);
	}



	/**
	 * Crebtes a new query from the network.
	 *
	 * @pbram guid the GUID of the query
	 * @pbram ttl the time to live of the query
	 * @pbram hops the hops of the query
	 * @pbram payload the query payload
	 *
	 * @return b new <tt>QueryRequest</tt> instance from the specified data
	 */
	public stbtic QueryRequest 
		crebteNetworkQuery(byte[] guid, byte ttl, byte hops, byte[] payload, int network) 
	    throws BbdPacketException {
		return new QueryRequest(guid, ttl, hops, pbyload, network);
	}

    /**
     * Builds b new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is b repeat query is encoded in guid.  GUID must have
     * been crebted via newQueryGUID; this allows the caller to match up results
     */
    privbte QueryRequest(byte[] guid, String query) {
        this(guid, query, "");
    }

    /**
     * Builds b new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is b repeat query is encoded in guid.  GUID must have
     * been crebted via newQueryGUID; this allows the caller to match up results
     */
    privbte QueryRequest(byte[] guid, byte ttl, String query) {
        this(guid, ttl, query, "");
    }

    /**
     * Builds b new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is b repeat query is encoded in guid.  GUID must have
     * been crebted via newQueryGUID; this allows the caller to match up results
     */
    privbte QueryRequest(byte[] guid, String query, String xmlQuery) {
        this(guid, DEFAULT_TTL, query, xmlQuery);
    }

    /**
     * Builds b new query from scratch, with metadata, using the given GUID.
     * Whether or not this is b repeat query is encoded in guid.  GUID must have
     * been crebted via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., cbn fit in 2 unsigned bytes) 
     */
    privbte QueryRequest(byte[] guid, byte ttl, String query, String richQuery) {
        this(guid, ttl, query, richQuery, UrnType.ANY_TYPE_SET, EMPTY_SET, null,
			 !RouterService.bcceptedIncomingConnection(), Message.N_UNKNOWN,
             fblse, 0, false, 0);
    }

    /**
     * Builds b new query from scratch, with metadata, using the given GUID.
     * Whether or not this is b repeat query is encoded in guid.  GUID must have
     * been crebted via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., cbn fit in 2 unsigned bytes) 
     */
    privbte QueryRequest(byte[] guid, byte ttl, String query, String richQuery,
                         MedibType type) {
        this(guid, ttl, query, richQuery, UrnType.ANY_TYPE_SET, EMPTY_SET, null,
			 !RouterService.bcceptedIncomingConnection(), Message.N_UNKNOWN,
             fblse, 0, false, getMetaFlag(type));
    }

    /**
     * Builds b new query from scratch, with metadata, using the given GUID.
     * Whether or not this is b repeat query is encoded in guid.  GUID must have
     * been crebted via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., cbn fit in 2 unsigned bytes) 
     */
    privbte QueryRequest(byte[] guid, byte ttl, String query, String richQuery,
                         boolebn canReceiveOutOfBandReplies) {
        this(guid, ttl, query, richQuery, UrnType.ANY_TYPE_SET, EMPTY_SET, null,
			 !RouterService.bcceptedIncomingConnection(), Message.N_UNKNOWN, 
             cbnReceiveOutOfBandReplies, 0, false, 0);
    }
 
    /**
     * Builds b new query from scratch, with metadata, using the given GUID.
     * Whether or not this is b repeat query is encoded in guid.  GUID must have
     * been crebted via newQueryGUID; this allows the caller to match up
     * results.
     *
     * @requires 0<=minSpeed<2^16 (i.e., cbn fit in 2 unsigned bytes) 
     */
    privbte QueryRequest(byte[] guid, byte ttl, String query, String richQuery,
                         boolebn canReceiveOutOfBandReplies, MediaType type) {
        this(guid, ttl, query, richQuery, UrnType.ANY_TYPE_SET, EMPTY_SET, null,
			 !RouterService.bcceptedIncomingConnection(), Message.N_UNKNOWN, 
             cbnReceiveOutOfBandReplies, 0, false, getMetaFlag(type));
    }
 
    privbte static int getMetaFlag(MediaType type) {
        int metbFlag = 0;
        if (type == null)
            ;
        else if (type.getDescriptionKey() == MedibType.AUDIO)
            metbFlag |= AUDIO_MASK;
        else if (type.getDescriptionKey() == MedibType.VIDEO)
            metbFlag |= VIDEO_MASK;
        else if (type.getDescriptionKey() == MedibType.IMAGES)
            metbFlag |= IMAGE_MASK;
        else if (type.getDescriptionKey() == MedibType.DOCUMENTS)
            metbFlag |= DOC_MASK;
        else if (type.getDescriptionKey() == MedibType.PROGRAMS) {
            if (CommonUtils.isLinux() || CommonUtils.isAnyMbc())
                metbFlag |= LIN_PROG_MASK;
            else if (CommonUtils.isWindows())
                metbFlag |= WIN_PROG_MASK;
            else // Other OS, sebrch any type of programs
                metbFlag |= (LIN_PROG_MASK|WIN_PROG_MASK);
        }
        return metbFlag;
    }

   /**
     * Builds b new query from scratch but you can flag it as a Requery, if 
     * needed.  If you need to mbke a query that accepts out-of-band results, 
     * be sure to set the guid correctly (see GUID.mbkeAddressEncodedGUI) and 
     * set cbnReceiveOutOfBandReplies .
     *
     * @requires 0<=minSpeed<2^16 (i.e., cbn fit in 2 unsigned bytes)
     * @pbram requestedUrnTypes <tt>Set</tt> of <tt>UrnType</tt> instances
     *  requested for this query, which mby be empty or null if no types were
     *  requested
	 * @pbram queryUrns <tt>Set</tt> of <tt>URN</tt> instances requested for 
     *  this query, which mby be empty or null if no URNs were requested
	 * @throws <tt>IllegblArgumentException</tt> if the query string, the xml
	 *  query string, bnd the urns are all empty, or if the feature selector
     *  is bbd
     */
    public QueryRequest(byte[] guid, byte ttl,  
                        String query, String richQuery, 
                        Set requestedUrnTypes, Set queryUrns,
                        QueryKey queryKey, boolebn isFirewalled, 
                        int network, boolebn canReceiveOutOfBandReplies,
                        int febtureSelector) {
        // cblls me with the doNotProxy flag set to false
        this(guid, ttl, query, richQuery, requestedUrnTypes, queryUrns,
             queryKey, isFirewblled, network, canReceiveOutOfBandReplies,
             febtureSelector, false, 0);
    }

    /**
     * Builds b new query from scratch but you can flag it as a Requery, if 
     * needed.  If you need to mbke a query that accepts out-of-band results, 
     * be sure to set the guid correctly (see GUID.mbkeAddressEncodedGUI) and 
     * set cbnReceiveOutOfBandReplies .
     *
     * @requires 0<=minSpeed<2^16 (i.e., cbn fit in 2 unsigned bytes)
     * @pbram requestedUrnTypes <tt>Set</tt> of <tt>UrnType</tt> instances
     *  requested for this query, which mby be empty or null if no types were
     *  requested
	 * @pbram queryUrns <tt>Set</tt> of <tt>URN</tt> instances requested for 
     *  this query, which mby be empty or null if no URNs were requested
	 * @throws <tt>IllegblArgumentException</tt> if the query string, the xml
	 *  query string, bnd the urns are all empty, or if the feature selector
     *  is bbd
     */
    public QueryRequest(byte[] guid, byte ttl,  
                        String query, String richQuery, 
                        Set requestedUrnTypes, Set queryUrns,
                        QueryKey queryKey, boolebn isFirewalled, 
                        int network, boolebn canReceiveOutOfBandReplies,
                        int febtureSelector, boolean doNotProxy,
                        int metbFlagMask) {
        this(guid, ttl, 0, query, richQuery, requestedUrnTypes, queryUrns,
             queryKey, isFirewblled, network, canReceiveOutOfBandReplies,
             febtureSelector, doNotProxy, metaFlagMask);
    }

    /**
     * Builds b new query from scratch but you can flag it as a Requery, if 
     * needed.  If you need to mbke a query that accepts out-of-band results, 
     * be sure to set the guid correctly (see GUID.mbkeAddressEncodedGUI) and 
     * set cbnReceiveOutOfBandReplies .
     *
     * @requires 0<=minSpeed<2^16 (i.e., cbn fit in 2 unsigned bytes)
     * @pbram requestedUrnTypes <tt>Set</tt> of <tt>UrnType</tt> instances
     *  requested for this query, which mby be empty or null if no types were
     *  requested
	 * @pbram queryUrns <tt>Set</tt> of <tt>URN</tt> instances requested for 
     *  this query, which mby be empty or null if no URNs were requested
	 * @throws <tt>IllegblArgumentException</tt> if the query string, the xml
	 *  query string, bnd the urns are all empty, or if the capability selector
     *  is bbd
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed,
                        String query, String richQuery, 
                        Set requestedUrnTypes, Set queryUrns,
                        QueryKey queryKey, boolebn isFirewalled, 
                        int network, boolebn canReceiveOutOfBandReplies,
                        int febtureSelector, boolean doNotProxy,
                        int metbFlagMask) {
        // don't worry bbout getting the length right at first
        super(guid, Messbge.F_QUERY, ttl, /* hops */ (byte)0, /* length */ 0, 
              network);
		if((query == null || query.length() == 0) &&
		   (richQuery == null || richQuery.length() == 0) &&
		   (queryUrns == null || queryUrns.size() == 0)) {
			throw new IllegblArgumentException("cannot create empty query");
		}		

        if(query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new IllegblArgumentException("query too big: " + query);
        }        

        if(richQuery != null && richQuery.length() > MAX_XML_QUERY_LENGTH) {
            throw new IllegblArgumentException("xml too big: " + richQuery);
        }

        if(query != null && 
          !(queryUrns != null && queryUrns.size() > 0 &&
            query.equbls(DEFAULT_URN_QUERY))
           && hbsIllegalChars(query)) {
            throw new IllegblArgumentException("illegal chars: " + query);
        }

        if (febtureSelector < 0)
            throw new IllegblArgumentException("Bad feature = " +
                                               febtureSelector);
        _febtureSelector = featureSelector;
        if ((metbFlagMask > 0) && (metaFlagMask < 4) || (metaFlagMask > 248))
            throw new IllegblArgumentException("Bad Meta Flag = " +
                                               metbFlagMask);
        if (metbFlagMask > 0)
            _metbMask = new Integer(metaFlagMask);

        // only set the minspeed if none wbs input...x
        if (minSpeed == 0) {
            // the new Min Speed formbt - looks reversed but
            // it isn't becbuse of ByteOrder.short2leb
            minSpeed = SPECIAL_MINSPEED_MASK; 
            // set the firewbll bit if i'm firewalled
            if (isFirewblled && !isMulticast())
                minSpeed |= SPECIAL_FIREWALL_MASK;
            // if i'm firewblled and can do solicited, mark the query for fw
            // trbnsfer capability.
            if (isFirewblled && UDPService.instance().canDoFWT())
                minSpeed |= SPECIAL_FWTRANS_MASK;
            // THE DEAL:
            // if we cbn NOT receive out of band replies, we want in-band XML -
            // so set the correct bit.
            // if we cbn receive out of band replies, we do not want in-band XML
            // we'll hope the out-of-bbnd reply guys will provide us all
            // necessbry XML.
            
            if (!cbnReceiveOutOfBandReplies) 
                minSpeed |= SPECIAL_XML_MASK;
            else // bit 10 flbgs out-of-band support
                minSpeed |= SPECIAL_OUTOFBAND_MASK;
        }

        MIN_SPEED = minSpeed;
		if(query == null) {
			this.QUERY = "";
		} else {
			this.QUERY = query;
		}
		if(richQuery == null || richQuery.equbls("") ) {
			this.XML_DOC = null;
		} else {
		    LimeXMLDocument doc = null;
		    try {
		        doc = new LimeXMLDocument(richQuery);
            } cbtch(SAXException ignored) {
            } cbtch(SchemaNotFoundException ignored) {
            } cbtch(IOException ignored) {
            }
            this.XML_DOC = doc;
		}
		Set tempRequestedUrnTypes = null;
		Set tempQueryUrns = null;
		if(requestedUrnTypes != null) {
			tempRequestedUrnTypes = new HbshSet(requestedUrnTypes);
		} else {
			tempRequestedUrnTypes = EMPTY_SET;
		}
		
		if(queryUrns != null) {
			tempQueryUrns = new HbshSet(queryUrns);
		} else {
			tempQueryUrns = EMPTY_SET;
		}

        this.QUERY_KEY = queryKey;
        this._doNotProxy = doNotProxy;
		
		ByteArrbyOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteOrder.short2leb((short)MIN_SPEED,bbos); // write minspeed
            bbos.write(QUERY.getBytes("UTF-8"));              // write query
            bbos.write(0);                             // null

			
            // now write bny & all HUGE v0.93 General Extension Mechanism 
			// extensions

			// this specifies whether or not the extension wbs successfully
			// written, mebning that the HUGE GEM delimiter should be
			// written before the next extension
            boolebn addDelimiterBefore = false;
			
            byte[] richQueryBytes = null;
            if(XML_DOC != null) {
                richQueryBytes = richQuery.getBytes("UTF-8");
			}
            
			// bdd the rich query
            bddDelimiterBefore = 
			    writeGemExtension(bbos, addDelimiterBefore, richQueryBytes);

			// bdd the urns
            bddDelimiterBefore = 
			    writeGemExtensions(bbos, addDelimiterBefore, 
								   tempQueryUrns == null ? null : 
								   tempQueryUrns.iterbtor());

			// bdd the urn types
            bddDelimiterBefore = 
			    writeGemExtensions(bbos, addDelimiterBefore, 
								   tempRequestedUrnTypes == null ? null : 
								   tempRequestedUrnTypes.iterbtor());

            // bdd the GGEP Extension, if necessary....
            // *----------------------------
            // construct the GGEP block
            GGEP ggepBlock = new GGEP(fblse); // do COBS

            // bdd the query key?
            if (this.QUERY_KEY != null) {
                // get query key in byte form....
                ByteArrbyOutputStream qkBytes = new ByteArrayOutputStream();
                this.QUERY_KEY.write(qkBytes);
                ggepBlock.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT,
                              qkBytes.toByteArrby());
            }

            // bdd the What Is header
            if (_febtureSelector > 0)
                ggepBlock.put(GGEP.GGEP_HEADER_FEATURE_QUERY, _febtureSelector);

            // bdd a GGEP-block if we shouldn't proxy
            if (doNotProxy)
                ggepBlock.put(GGEP.GGEP_HEADER_NO_PROXY);

            // bdd a meta flag
            if (_metbMask != null)
                ggepBlock.put(GGEP.GGEP_HEADER_META, _metbMask.intValue());

            // if there bre GGEP headers, write them out...
            if ((this.QUERY_KEY != null) || (_febtureSelector > 0) ||
                _doNotProxy || (_metbMask != null)) {
                ByteArrbyOutputStream ggepBytes = new ByteArrayOutputStream();
                ggepBlock.write(ggepBytes);
                // write out GGEP
                bddDelimiterBefore = writeGemExtension(baos, addDelimiterBefore,
                                                       ggepBytes.toByteArrby());
            }
            // ----------------------------*

            bbos.write(0);                             // final null
		} 
        cbtch(UnsupportedEncodingException uee) {
            //this should never hbppen from the getBytes("UTF-8") call
            //but there bre UnsupportedEncodingExceptions being reported
            //with UTF-8.
            //Is there other informbtion we want to pass in as the message?
            throw new IllegblArgumentException("could not get UTF-8 bytes for query :"
                                               + QUERY 
                                               + " with richquery :"
                                               + richQuery);
        }
        cbtch (IOException e) {
		    ErrorService.error(e);
		}

		PAYLOAD = bbos.toByteArray();
		updbteLength(PAYLOAD.length);

		this.QUERY_URNS = Collections.unmodifibbleSet(tempQueryUrns);
		this.REQUESTED_URN_TYPES = Collections.unmodifibbleSet(tempRequestedUrnTypes);

    }


    /**
     * Build b new query with data snatched from network
     *
     * @pbram guid the message guid
	 * @pbram ttl the time to live of the query
	 * @pbram hops the hops of the query
	 * @pbram payload the query payload, containing the query string and any
	 *  extension strings
	 * @pbram network the network that this query came from.
	 * @throws <tt>BbdPacketException</tt> if this is not a valid query
     */
    privbte QueryRequest(
      byte[] guid, byte ttl, byte hops, byte[] pbyload, int network) 
		throws BbdPacketException {
        super(guid, Messbge.F_QUERY, ttl, hops, payload.length, network);
		if(pbyload == null) {
			throw new BbdPacketException("no payload");
		}
		PAYLOAD=pbyload;
		String tempQuery = "";
		String tempRichQuery = "";
		int tempMinSpeed = 0;
		Set tempQueryUrns = null;
		Set tempRequestedUrnTypes = null;
        QueryKey tempQueryKey = null;
        try {
            ByteArrbyInputStream bais = new ByteArrayInputStream(this.PAYLOAD);
			short sp = ByteOrder.leb2short(bbis);
			tempMinSpeed = ByteOrder.ushort2int(sp);
            tempQuery = new String(super.rebdNullTerminatedBytes(bais), "UTF-8");
            // hbndle extensions, which include rich query and URN stuff
            byte[] extsBytes = super.rebdNullTerminatedBytes(bais);
            HUGEExtension huge = new HUGEExtension(extsBytes);
            GGEP ggep = huge.getGGEP();

            if(ggep != null) {
                try {
                    if (ggep.hbsKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                        byte[] qkBytes = ggep.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
                        tempQueryKey = QueryKey.getQueryKey(qkBytes, fblse);
                    }
                    if (ggep.hbsKey(GGEP.GGEP_HEADER_FEATURE_QUERY))
                        _febtureSelector = ggep.getInt(GGEP.GGEP_HEADER_FEATURE_QUERY);
                    if (ggep.hbsKey(GGEP.GGEP_HEADER_NO_PROXY))
                        _doNotProxy = true;
                    if (ggep.hbsKey(GGEP.GGEP_HEADER_META)) {
                        _metbMask = new Integer(ggep.getInt(GGEP.GGEP_HEADER_META));
                        // if the vblue is something we can't handle, don't even set it
                        if ((_metbMask.intValue() < 4) || (_metaMask.intValue() > 248))
                            _metbMask = null;
                    }
                } cbtch (BadGGEPPropertyException ignored) {}
            }

            tempQueryUrns = huge.getURNS();
            tempRequestedUrnTypes = huge.getURNTypes();
            for (Iterbtor iter = huge.getMiscBlocks().iterator();
                 iter.hbsNext() && tempRichQuery.equals(""); ) {
                String currMiscBlock = (String) iter.next();
                if (currMiscBlock.stbrtsWith("<?xml"))
                    tempRichQuery = currMiscBlock;                
            }
        } cbtch(UnsupportedEncodingException uee) {
            //couldn't build query from network due to unsupportedencodingexception
            //so throw b BadPacketException 
            throw new BbdPacketException(uee.getMessage());
        } cbtch (IOException ioe) {
            ErrorService.error(ioe);
        }
		QUERY = tempQuery;
	    LimeXMLDocument tempDoc = null;
	    try {
	        tempDoc = new LimeXMLDocument(tempRichQuery);
        } cbtch(SAXException ignored) {
        } cbtch(SchemaNotFoundException ignored) {
        } cbtch(IOException ignored) {
        }
        this.XML_DOC = tempDoc;
		MIN_SPEED = tempMinSpeed;
		if(tempQueryUrns == null) {
			QUERY_URNS = EMPTY_SET; 
		}
		else {
			QUERY_URNS = Collections.unmodifibbleSet(tempQueryUrns);
		}
		if(tempRequestedUrnTypes == null) {
			REQUESTED_URN_TYPES = EMPTY_SET;
		}
		else {
			REQUESTED_URN_TYPES =
			    Collections.unmodifibbleSet(tempRequestedUrnTypes);
		}	
        QUERY_KEY = tempQueryKey;
		if(QUERY.length() == 0 &&
		   tempRichQuery.length() == 0 &&
		   QUERY_URNS.size() == 0) {
		    ReceivedErrorStbt.QUERY_EMPTY.incrementStat();
			throw new BbdPacketException("empty query");
		}       
        if(QUERY.length() > MAX_QUERY_LENGTH) {
            ReceivedErrorStbt.QUERY_TOO_LARGE.incrementStat();
            //throw BbdPacketException.QUERY_TOO_BIG;
            throw new BbdPacketException("query too big: " + QUERY);
        }        

        if(tempRichQuery.length() > MAX_XML_QUERY_LENGTH) {
            ReceivedErrorStbt.QUERY_XML_TOO_LARGE.incrementStat();
            //throw BbdPacketException.XML_QUERY_TOO_BIG;
            throw new BbdPacketException("xml too big: " + tempRichQuery);
        }

        if(!(QUERY_URNS.size() > 0 && QUERY.equbls(DEFAULT_URN_QUERY))
           && hbsIllegalChars(QUERY)) {
            ReceivedErrorStbt.QUERY_ILLEGAL_CHARS.incrementStat();
            //throw BbdPacketException.ILLEGAL_CHAR_IN_QUERY;
            throw new BbdPacketException("illegal chars: " + QUERY);
        }
    }
    
    privbte static boolean hasIllegalChars(String query) {
        return StringUtils.contbinsCharacters(query,ILLEGAL_CHARS);
    }

    /**
     * Returns b new GUID appropriate for query requests.  If isRequery,
     * the GUID query is mbrked.
     */
    public stbtic byte[] newQueryGUID(boolean isRequery) {
        return isRequery ? GUID.mbkeGuidRequery() : GUID.makeGuid();
	}

    protected void writePbyload(OutputStream out) throws IOException {
        out.write(PAYLOAD);
		SentMessbgeStatHandler.TCP_QUERY_REQUESTS.addMessage(this);
    }

    /**
     * Accessor fot the pbyload of the query hit.
     *
     * @return the query hit pbyload
     */
    public byte[] getPbyload() {
        return PAYLOAD;
    }

    /** 
     * Returns the query string of this messbge.<p>
     *
     * The cbller should not call the getBytes() method on the returned value,
     * bs this seems to cause problems on the Japanese Macintosh.  If you need
     * the rbw bytes of the query string, call getQueryByteAt(int).
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
     * Helper method used internblly for getting the rich query string.
     */
    privbte String getRichQueryString() {
        if( XML_DOC == null )
            return null;
        else
            return XML_DOC.getXMLString();
    }       
 
	/**
	 * Returns the <tt>Set</tt> of URN types requested for this query.
	 *
	 * @return the <tt>Set</tt> of <tt>UrnType</tt> instbnces requested for this
     * query, which mby be empty (not null) if no types were requested
	 */
    public Set getRequestedUrnTypes() {
		return REQUESTED_URN_TYPES;
    }
    
	/**
	 * Returns the <tt>Set</tt> of <tt>URN</tt> instbnces for this query.
	 *
	 * @return  the <tt>Set</tt> of <tt>URN</tt> instbnces for this query, which
	 * mby be empty (not null) if no URNs were requested
	 */
    public Set getQueryUrns() {
		return QUERY_URNS;
    }
	
	/**
	 * Returns whether or not this query contbins URNs.
	 *
	 * @return <tt>true</tt> if this query contbins URNs,<tt>false</tt> otherwise
	 */
	public boolebn hasQueryUrns() {
		return !QUERY_URNS.isEmpty();
	}

    /**
	 * Note: the minimum speed cbn be represented as a 2-byte unsigned
	 * number, but Jbva shorts are signed.  Hence we must use an int.  The
	 * vblue returned is always smaller than 2^16.
	 */
	public int getMinSpeed() {
		return MIN_SPEED;
	}


    /**
     * Returns true if the query source is b firewalled servent.
     */
    public boolebn isFirewalledSource() {
        if ( !isMulticbst() ) {
            if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
                if ((MIN_SPEED & SPECIAL_FIREWALL_MASK) > 0)
                    return true;
            }
        }
        return fblse;
    }
 
 
    /**
     * Returns true if the query source desires Lime metb-data in responses.
     */
    public boolebn desiresXMLResponses() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_XML_MASK) > 0)
                return true;
        }
        return fblse;        
    }


    /**
     * Returns true if the query source cbn do a firewalled transfer.
     */
    public boolebn canDoFirewalledTransfer() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_FWTRANS_MASK) > 0)
                return true;
        }
        return fblse;        
    }


    /**
     * Returns true if the query source cbn accept out-of-band replies.  Use
     * getReplyAddress() bnd getReplyPort() if this is true to know where to
     * it.  Alwbys send XML if you are sending an out-of-band reply.
     */
    public boolebn desiresOutOfBandReplies() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_OUTOFBAND_MASK) > 0)
                return true;
        }
        return fblse;
    }


    /**
     * Returns true if the query source does not wbnt you to proxy for it.
     */
    public boolebn doNotProxy() {
        return _doNotProxy;
    }

    /**
     * Returns true if this query is for 'Whbt is new?' content, i.e. usually
     * the top 3 YOUNGEST files in your librbry.
     */
    public boolebn isWhatIsNewRequest() {
        return _febtureSelector == FeatureSearchData.WHAT_IS_NEW;
    }
    
    /**
     * Returns true if this is b feature query.
     */
    public boolebn isFeatureQuery() {
        return _febtureSelector > 0;
    }

    /**
     * Returns 0 if this is not b "feature" query, else it returns the selector
     * of the febture query, e.g. What Is New returns 1.
     */
    public int getFebtureSelector() {
        return _febtureSelector;
    }

    /** Returns the bddress to send a out-of-band reply to.  Only useful
     *  when desiresOutOfBbndReplies() == true.
     */
    public String getReplyAddress() {
        return (new GUID(getGUID())).getIP();
    }

        
    /** Returns true if the input bytes mbtch the OOB address of this query.
     */
    public boolebn matchesReplyAddress(byte[] ip) {
        return (new GUID(getGUID())).mbtchesIP(ip);
    }

        
    /** Returns the port to send b out-of-band reply to.  Only useful
     *  when desiresOutOfBbndReplies() == true.
     */
    public int getReplyPort() {
        return (new GUID(getGUID())).getPort();
    }


	/**
	 * Accessor for whether or not this is b requery from a LimeWire.
	 *
	 * @return <tt>true</tt> if it is bn automated requery from a LimeWire,
	 *  otherwise <tt>fblse</tt>
	 */
	public boolebn isLimeRequery() {
		return GUID.isLimeRequeryGUID(getGUID());
	}
        
    /**
     * Returns the QueryKey bssociated with this Request.  May very well be
     * null.  Usublly only UDP QueryRequests will have non-null QueryKeys.
     */
    public QueryKey getQueryKey() {
        return QUERY_KEY;
    }

    /** @return true if the query hbs no constraints on the type of results
     *  it wbnts back.
     */
    public boolebn desiresAll() {
        return (_metbMask == null);
    }

    /** @return true if the query desires 'Audio' results bbck.
     */
    public boolebn desiresAudio() {
        if (_metbMask != null) 
            return ((_metbMask.intValue() & AUDIO_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Video' results bbck.
     */
    public boolebn desiresVideo() {
        if (_metbMask != null) 
            return ((_metbMask.intValue() & VIDEO_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Document' results bbck.
     */
    public boolebn desiresDocuments() {
        if (_metbMask != null) 
            return ((_metbMask.intValue() & DOC_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Imbge' results back.
     */
    public boolebn desiresImages() {
        if (_metbMask != null) 
            return ((_metbMask.intValue() & IMAGE_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Progrbms/Packages' for Windows
     *  results bbck.
     */
    public boolebn desiresWindowsPrograms() {
        if (_metbMask != null) 
            return ((_metbMask.intValue() & WIN_PROG_MASK) > 0);
        return true;
    }
    
    /** @return true if the query desires 'Progrbms/Packages' for Linux/OSX
     *  results bbck.
     */
    public boolebn desiresLinuxOSXPrograms() {
        if (_metbMask != null) 
            return ((_metbMask.intValue() & LIN_PROG_MASK) > 0);
        return true;
    }
    
    /**
     * Returns the mbsk of allowed programs.
     */
    public int getMetbMask() {
        if (_metbMask != null)
            return _metbMask.intValue();
        return 0;
    }

	// inherit doc comment
	public void recordDrop() {
		DroppedSentMessbgeStatHandler.TCP_QUERY_REQUESTS.addMessage(this);
	}

    /** Returns this, becbuse it's always safe to send big queries. */
    public Messbge stripExtendedPayload() {
        return this;
    }
    
    /** Mbrks this as being an re-originated query. */
    public void originbte() {
        originbted = true;
    }
    
    /** Determines if this is bn originated query */
    public boolebn isOriginated() {
        return originbted;
    }

	public int hbshCode() {
		if(_hbshCode == 0) {
			int result = 17;
			result = (37*result) + QUERY.hbshCode();
			if( XML_DOC != null )
			    result = (37*result) + XML_DOC.hbshCode();
			result = (37*result) + REQUESTED_URN_TYPES.hbshCode();
			result = (37*result) + QUERY_URNS.hbshCode();
			if(QUERY_KEY != null) {
				result = (37*result) + QUERY_KEY.hbshCode();
			}
			// TODO:: ADD GUID!!
			_hbshCode = result;
		}
		return _hbshCode;
	}

	// overrides Object.toString
	public boolebn equals(Object o) {
		if(o == this) return true;
		if(!(o instbnceof QueryRequest)) return false;
		QueryRequest qr = (QueryRequest)o;
		return (MIN_SPEED == qr.MIN_SPEED &&
				QUERY.equbls(qr.QUERY) &&
				(XML_DOC == null ? qr.XML_DOC == null : 
				    XML_DOC.equbls(qr.XML_DOC)) &&
				REQUESTED_URN_TYPES.equbls(qr.REQUESTED_URN_TYPES) &&
				QUERY_URNS.equbls(qr.QUERY_URNS) &&
				Arrbys.equals(getGUID(), qr.getGUID()) &&
				Arrbys.equals(PAYLOAD, qr.PAYLOAD));
	}


    public String toString() {
 		return "<query: \""+getQuery()+"\", "+
            "ttl: "+getTTL()+", "+
            "hops: "+getHops()+", "+            
            "metb: \""+getRichQueryString()+"\", "+
            "types: "+getRequestedUrnTypes().size()+","+
            "urns: "+getQueryUrns().size()+">";
    }
}
