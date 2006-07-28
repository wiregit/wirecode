
// Commented for the Learning branch

package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.xml.sax.SAXException;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnType;
import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.I18NConvert;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.SchemaNotFoundException;

/**
 * A QueryRequest object represents a Gnutella query packet.
 * We make a query packet to search Gnutella.
 * 
 * Query packets travel over Gnutella TCP socket connections.
 * Results return out of band in UDP packets.
 * After a download has failed, LimeWire can send a query in a UDP packet.
 * This method of searching is called GUESS, and is not used much anymore.
 * 
 * There are 2 reasons to make a QueryRequest object:
 * Send: We're preparing a new query packet that we will send.
 * Receive: We've read a packet we've just received, and want to make an object to represent it.
 * 
 * A query packet looks like this:
 * 
 * gnutella packet header 23 bytes
 * SS
 * search text\0
 * XML[0x1C]urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ[0x1C]urn:sha1:[0x1C]GGEP block\0
 * 
 * The first 2 bytes of the payload are the speed flags.
 * After that is the search text, which ends with a 0 byte.
 * The extended area doesn't contain any 0 bytes, and is null terminated.
 * It contains any number of extensions separated by 0x1C bytes.
 * We write the extensions in the following order.
 * 
 * The XML of a rich query.
 * A URN with a hash, making this a search for the file with that hash exactly.
 * URN types, "urn:sha1:" means we support SHA1 hashes and want them in hits.
 * The GGEP block.
 */
public class QueryRequest extends Message implements Serializable {

    /*
     * Bit flags for the minimum speed.
     * These are the 2 bytes before the search text.
     * Initially, they were used to hold a speed.
     * Computers that couldn't deliver a file that fast wouldn't respond with a hit even if they had one.
     * Now, the first byte is used to hold flags.
     * The flags give information about the computer that initiated the search, the computer an uploader will have to give a hit file to.
     * On the wire, the bits look like this:
     * 
     *                             mfx0 0ow0
     * SPECIAL_MINSPEED_MASK  0x80 m         We're using these 2 bytes to hold flags, not a speed
     * SPECIAL_FIREWALL_MASK  0x40  f        The searching computer isn't externally contactable on the Internet
     * SPECIAL_XML_MASK       0x20   x       The searching computer wants hits with XML metadata
     * SPECIAL_OUTOFBAND_MASK 0x04       o   Hit computers should respond to this query out of band, with UDP packets
     * SPECIAL_FWTRANS_MASK   0x02        w  The searching computer can do UDP firewall-to-firewall file transfers
     * 
     * The second byte is the maximum number of hits the searching computer wants.
     * LimeWire leaves it 0 to not set this limit.
     */

    /*
     * these specs may seem backwards, but they are not - ByteOrder.short2leb
     * puts the low-order byte first, so over the network 0x0080 would look
     * like 0x8000
     */

    /** 0x80, the bit at 1000 0000, marks the 2 minimum speed bytes as holding flags, not a speed. */
    public static final int SPECIAL_MINSPEED_MASK  = 0x0080;
    /** 0x40, the bit at 0100 0000, the searching computer isn't externally contactable. */
    public static final int SPECIAL_FIREWALL_MASK  = 0x0040;
    /** 0x20, the bit at 0010 0000, the searching computer wants hits with XML metadata. */
    public static final int SPECIAL_XML_MASK       = 0x0020;
    /** 0x04, the bit at 0000 0100, the searching computer can get UDP packets. */
    public static final int SPECIAL_OUTOFBAND_MASK = 0x0004;
    /** 0x02, the bit at 0000 0010, the searching computer can do UDP firewall-to-firewall file transfers. */
    public static final int SPECIAL_FWTRANS_MASK   = 0x0002;

    /*
     * Bit flags for the value of the GGEP "M" Meta extension.
     * Its bits will tell what kind of media this search is filtered on.
     * 
     *                    lwid va00
     * LIN_PROG_MASK 0x80 l         Linux and Mac OS X programs
     * WIN_PROG_MASK 0x40  w        Windows programs
     * IMAGE_MASK    0x20   i       Images
     * DOC_MASK      0x10    d      Documents
     * VIDEO_MASK    0x08      v    Video
     * AUDIO_MASK    0x04       a   Audio
     * 
     * If this query has "M", _metaMask will not be null.
     */

    /** 0x04, the bit at 0000 0100, filter search for audio in the GGEP "M" byte value. */
    public static final int AUDIO_MASK    = 0x0004;
    /** 0x08, the bit at 0000 1000, filter search for video in the GGEP "M" byte value. */
    public static final int VIDEO_MASK    = 0x0008;
    /** 0x10, the bit at 0001 0000, filter search for documents in the GGEP "M" byte value. */
    public static final int DOC_MASK      = 0x0010;
    /** 0x20, the bit at 0010 0000, filter search for images in the GGEP "M" byte value. */
    public static final int IMAGE_MASK    = 0x0020;
    /** 0x40, the bit at 0100 0000, filter search for Windows programs in the GGEP "M" byte value. */
    public static final int WIN_PROG_MASK = 0x0040;
    /** 0x80, the bit at 1000 0000, filter search for Linux and Mac OS X programs in the GGEP "M" byte value. */
    public static final int LIN_PROG_MASK = 0x0080;

    /** "WhatIsNewXOXO", the search text for a LimeWire what is new search. */
    public static final String WHAT_IS_NEW_QUERY_STRING = "WhatIsNewXOXO";

    /**
     * The data of the payload of this query packet.
     * This is all the data after the 23 byte Gnutella packet header.
     * It includes the speed byte, the search text, and all the HUGE extensions.
     */
    private final byte[] PAYLOAD;

    /**
     * The minimum speed byte of flags.
     * This is the first byte of the payload, right before the search text.
     * 
     * Originally, Gnutella programs used this byte to specify a minimum speed for returned results.
     * It was never really used that way.
     * LimeWire 3.0, released February 2003, started using the bits as flags instead.
     */
    private final int MIN_SPEED;

    /**
     * The search text.
     * In the payload, this is after the speed flags byte and null terminated.
     */
    private final String QUERY;
    
    /**
     * The XML rich query.
     * XML_DOC is a LimeXMLDocument object.
     * 
     * One kind of extension is text that starts "<?xml".
     * We parsed it into a LimeXMLDocument object and saved it here.
     */
    private final LimeXMLDocument XML_DOC;

    /**
     * The feature that this query is.
     * The int value of the GGEP "WH" What's New extension.
     */
    private int _featureSelector = 0;

    /**
     * True if the searching computer doesn't want a computer to proxy this search for it.
     * Whether or not the GGEP header for Do Not Proxy was found.
     * False by default.
     * If the query's GGEP block has the "NP" No Proxy extension, we set _doNotProxy to true.
     */
    private boolean _doNotProxy = false;

    /*
     * HUGE v0.93 fields
     */

    /**
	 * The types of hash URNs this search wants results to specify.
     * Represents text extensions like "urn:sha1:" with no hash.
     * This is how a search indicates it wants the SHA1 hash included in results.
     * A HashSet of UrnType objects.
	 */
    private final Set REQUESTED_URN_TYPES;

    /**
     * The hash URNs this search is for.
     * Represents text extensions like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ".
     * This is a search by hash.
     * A HashSet of URN objects.
	 */
    private final Set QUERY_URNS;

    /**
     * QueryKey is a part of GUESS, which LimeWire no longer uses.
     * 
     * The Query Key associated with this query.
     * Can be null.
     */
    private final QueryKey QUERY_KEY;

    /**
     * The media type search filter byte.
     * The byte that has flags that show what kind of media, like audio or video, this search is for.
     * 
     * If this query packet has a GGEP block with the "M" Meta extension, this is its byte value.
     * The "M" value is a byte, but this member variable is an Integer object with the same bits set.
     * 
     * If _metaMask is null, the GGEP block doesn't have the "M" extension, and this is a normal search.
     * If _metaMask is not null, the GGEP block does have the "M" extension, and this search is filtered for certain kinds of media.
     */
    private Integer _metaMask = null;

    /**
     * True if we've re-originated this query for a leaf.
     * You can set and read this flag after making a QueryRequest object.
     * The originate() method sets it to true, and the isOriginated() method returns it.
     */
    private boolean originated = false;

	/**
     * The hash code of this QueryRequest object.
     * The hashCode() method computes it once and keeps it here to not have to compute it again.
	 */
	private volatile int _hashCode = 0;

	/**
     * An empty unmodifiable HashSet.
     * This is necessary because Collections.EMPTY_SET is not serializable in the collections 1.1 implementation.
	 */
	private static final Set EMPTY_SET = Collections.unmodifiableSet(new HashSet());

    /**
     * 6, The default TTL for queries.
     * (do) isn't this high?
     */
    private static final byte DEFAULT_TTL = 6;

    /** The characters that are not allowed in search text, like _ # ! |. */
    private static final char[] ILLEGAL_CHARS = SearchSettings.ILLEGAL_CHARS.getValue();

    /** 30 characters, the maximum number to allow in search text. */
    private static final int MAX_QUERY_LENGTH = SearchSettings.MAX_QUERY_LENGTH.getValue();

    /** 500 characters, the maximum number to allow in an XML extension. */
    private static final int MAX_XML_QUERY_LENGTH = SearchSettings.MAX_XML_QUERY_LENGTH.getValue();

    /**
     * "\" a single backslash.
     * LimeWire doesn't search by hash, but if it did, it would put "\" as the search text, and then have the SHA1 hash in a URN in the extended area.
     * Without this search text, a LimeWire program would see no search text and drop the query.
     */
    private static final String DEFAULT_URN_QUERY = "\\";

	/**
     * Not used, LimeWire does not search by hash.
     * 
	 * Creates a new requery for the specified SHA1 value.
	 * @param sha1 the <tt>URN</tt> of the file to search for
	 * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument
	 *  is <tt>null</tt>
	 */
	public static QueryRequest createRequery(URN sha1) {
        if (sha1 == null) throw new NullPointerException("null sha1");
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(true), DEFAULT_TTL, DEFAULT_URN_QUERY, "", UrnType.SHA1_SET, sha1Set, null, !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, false, 0, false, 0);

	}

	/**
     * Not used, LimeWire does not search by hash.
     * 
	 * Creates a new query for the specified SHA1 value.
	 * @param sha1 the <tt>URN</tt> of the file to search for
	 * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument is <tt>null</tt>
	 */
	public static QueryRequest createQuery(URN sha1) {
        if (sha1 == null) throw new NullPointerException("null sha1");
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, DEFAULT_URN_QUERY, "",  UrnType.SHA1_SET, sha1Set, null, !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, false, 0, false, 0);
	}

    /**
     * Not used, LimeWire does not search by hash.
     * 
	 * Creates a new requery for the specified SHA1 value with file name 
     * thrown in for good measure (or at least until \ works as a query).
	 * @param sha1 the <tt>URN</tt> of the file to search for
	 * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument is <tt>null</tt>
	 */
	public static QueryRequest createRequery(URN sha1, String filename) {
        if (sha1 == null) throw new NullPointerException("null sha1");
        if (filename == null) throw new NullPointerException("null query");
		if (filename.length() == 0) filename = DEFAULT_URN_QUERY;
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(true), DEFAULT_TTL, filename, "", UrnType.SHA1_SET, sha1Set, null, !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, false, 0, false, 0);

	}

	/**
     * Not used, LimeWire does not search by hash.
     * 
	 * Creates a new query for the specified SHA1 value with file name 
     * thrown in for good measure (or at least until \ works as a query).
	 * @param sha1 the <tt>URN</tt> of the file to search for
	 * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument is <tt>null</tt>
	 */
	public static QueryRequest createQuery(URN sha1, String filename) {
        if (sha1 == null) throw new NullPointerException("null sha1");
        if (filename == null) throw new NullPointerException("null query");
		if (filename.length() == 0) filename = DEFAULT_URN_QUERY;
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, filename, "", UrnType.SHA1_SET, sha1Set, null, !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, false, 0, false, 0);
	}

	/**
     * Not used, LimeWire does not search by hash.
     * 
	 * Creates a new requery for the specified SHA1 value and the specified firewall boolean.
	 * @param sha1 the <tt>URN</tt> of the file to search for
	 * @param ttl the time to live (ttl) of the query
	 * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
	 * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the ttl value is negative or greater than the maximum allowed value
	 */
	public static QueryRequest createRequery(URN sha1, byte ttl) {
        if (sha1 == null) throw new NullPointerException("null sha1");
		if (ttl <= 0 || ttl > 6) throw new IllegalArgumentException("invalid TTL: " + ttl);
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(true), ttl, DEFAULT_URN_QUERY, "", UrnType.SHA1_SET, sha1Set, null, !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, false, 0, false, 0);
	}

	/**
     * Not used.
     * 
	 * Creates a new query for the specified UrnType set and URN set.
	 * @param urnTypeSet the <tt>Set</tt> of <tt>UrnType</tt>s to request.
	 * @param urnSet the <tt>Set</tt> of <tt>URNs</tt>s to request.
	 * @return a new <tt>QueryRequest</tt> for the specied UrnTypes and URNs
	 * @throws <tt>NullPointerException</tt> if either sets are null.
	 */
	public static QueryRequest createQuery(Set urnTypeSet, Set urnSet) {
	    if (urnSet == null) throw new NullPointerException("null urnSet");
	    if (urnTypeSet == null) throw new NullPointerException("null urnTypeSet");
	    return new QueryRequest(newQueryGUID(false), DEFAULT_TTL, DEFAULT_URN_QUERY, "", urnTypeSet, urnSet, null, !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, false, 0, false, 0);
    }

	/**
     * Not used.
     * 
	 * Creates a requery for when we don't know the hash of the file -- we don't know the hash.
	 * @param query the query string
	 * @return a new <tt>QueryRequest</tt> for the specified query
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt> argument is zero-length (empty)
	 */
	public static QueryRequest createRequery(String query) {
        if (query == null) throw new NullPointerException("null query");
		if (query.length() == 0) throw new IllegalArgumentException("empty query");
		return new QueryRequest(newQueryGUID(true), query);
	}

	/**
     * Make a new query for us to send that will search for the given file name.
     * ResumeDownloader.newRequery(int), ManagedDownloader.newRequery(int), and MagnetDownloader.newRequery(int) calls this.
     * Leads to the packet maker.
     * 
     * @param query The search text
     * @return      A new QueryRequest object with that search text, and all the defaults
	 */
	public static QueryRequest createQuery(String query) {

        // Make sure the caller gave us search text
        if (query == null) throw new NullPointerException("null query");
		if (query.length() == 0) throw new IllegalArgumentException("empty query");

        // Make and return a new QueryRequest object with all the defaults
        return new QueryRequest(
            newQueryGUID(false), // Pass false to give the query packet a normal LimeWire GUID, not a special requery one (do)
            query);              // The search text
	}

	/**
     * Not used.
     * 
	 * Creates a new query for the specified file name and the designated XML.
	 * @param query the file name to search for
     * @param guid I trust that this is a address encoded guid.  Your loss if it isn't....
	 * @return a new <tt>QueryRequest</tt> for the specified query that has
	 * encoded the input ip and port into the GUID and appropriate marked the
	 * query to signify out of band support.
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt> argument is zero-length (empty)
	 */
    public static QueryRequest createOutOfBandQuery(byte[] guid, String query, String xmlQuery) {
        query = I18NConvert.instance().getNorm(query);
        if (query == null) throw new NullPointerException("null query");
		if (xmlQuery == null) throw new NullPointerException("null xml query");
		if (query.length() == 0 && xmlQuery.length() == 0) throw new IllegalArgumentException("empty query");
		if (xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) throw new IllegalArgumentException("invalid XML");
        return new QueryRequest(guid, DEFAULT_TTL, query, xmlQuery, true);
    }

	/**
     * Make a query packet marked to get query hit packets out of band in UDP.
     * Takes a GUID with our IP address and port number hidden in it, this is how hit computers will address UDP packets back to us.
     * Sets 0x04 in the speed flags bytes, this marks the query as wanting out of band results.
     * 
     * RouterService.query() calls this.
     * Leads to the packet maker.
     * 
     * @param guid     The message GUID, which has our IP address and port number hidden in it
     * @param query    The search text
     * @param xmlQuery XML search
     * @param type     A MediaType object we'll turn into GGEP "M" with bits for audio and video to filter the search, or null to not do this
     * @return         A new QueryRequest object
	 */
    public static QueryRequest createOutOfBandQuery(byte[] guid, String query, String xmlQuery, MediaType type) {

        // Replace accented characters in the search text with core characters to get more hits
        query = I18NConvert.instance().getNorm(query);

        // Make sure we have search text or XML, and the XML starts with "<?xml"
        if (query    == null) throw new NullPointerException("null query");
		if (xmlQuery == null) throw new NullPointerException("null xml query");
		if (query.length()    == 0 && xmlQuery.length() == 0)        throw new IllegalArgumentException("empty query");
		if (xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) throw new IllegalArgumentException("invalid XML");

        // Make and return a new QueryRequest object with the given values and more defaults
        return new QueryRequest(
            guid,
            DEFAULT_TTL, // byte    ttl                        Default TTL 6, we'll lower it before we actually send it
            query,
            xmlQuery,
            true,        // boolean canReceiveOutOfBandReplies Add SPECIAL_OUTOFBAND_MASK and not SPECIAL_XML_MASK to the flags byte
            type);
    }                                

	/**
     * Not used.
     * 
	 * Creates a new query for the specified file name, with no XML.
	 * @param query the file name to search for
	 * @return a new <tt>QueryRequest</tt> for the specified query that has
	 * encoded the input ip and port into the GUID and appropriate marked the
	 * query to signify out of band support.
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt> argument is zero-length (empty)
	 */
    public static QueryRequest createOutOfBandQuery(String query, byte[] ip, int port) {
        byte[] guid = GUID.makeAddressEncodedGuid(ip, port);
        return QueryRequest.createOutOfBandQuery(guid, query, "");
    }                                

    /**
     * Not used.
     * 
     * Creates a new 'What is new'? query with the specified guid and ttl.
     * @param ttl the desired ttl of the query.
     * @param guid the desired guid of the query.
     */
    public static QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl) {
        return createWhatIsNewQuery(guid, ttl, null);
    }

    /**
     * Make a new What's New query for us to send.
     * RouterService.queryWhatIsNew() calls this.
     * Leads to the packet maker.
     * 
     * @param guid The message GUID
     * @param ttl  The message TTL must be 1 or more
     * @param type A MediaType object we'll turn into GGEP "M" with bits for audio and video to filter the search, or null to not do this
     * @return     A new QueryRequest object
     */
    public static QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl, MediaType type) {

        // Make sure the TTL is 1 or more
        if (ttl < 1) throw new IllegalArgumentException("Bad TTL.");

        // Make and return a new QueryRequest objet with the given values and defaults for a What's New query
        return new QueryRequest(
            guid,
            ttl,
            WHAT_IS_NEW_QUERY_STRING,                    // String   query                      The search text for a What's New query is "WhatIsNewXOXO"
            "",                                          // String   richQuery                  No XML
            null,                                        // Set      requestedUrnTypes          No URN types like "urn:sha1:"
            null,                                        // Set      queryUrns                  No URNs with actual hashes
            null,                                        // QueryKey queryKey                   QueryKey is part of GUESS, which LimeWire doesn't use anymore
            !RouterService.acceptedIncomingConnection(), // boolean  isFirewalled               Set the second flag bit if our TCP connect back tests have failed
            Message.N_UNKNOWN,                           // int      network                    We don't know if we will send this query over TCP or UDP yet
            false,                                       // boolean  canReceiveOutOfBandReplies Request XML metadata in the in band hits that flow back to us
            FeatureSearchData.WHAT_IS_NEW,               // int      featureSelector            Add the GGEP "WH" What is new extension with the value 1
            false,                                       // boolean  doNotProxy                 Leave out the "NP" No Proxy GGEP extension
            getMetaFlag(type));
    }

    /**
     * Not used.
     * 
     * Creates a new 'What is new'? OOB query with the specified guid and ttl.
     * @param ttl the desired ttl of the query.
     * @param guid the desired guid of the query.
     */
    public static QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl) {
        return createWhatIsNewOOBQuery(guid, ttl, null);
    }

    /**
     * Make a new What's New query for us to send in a UDP packet.
     * RouterService.queryWhatIsNew() calls this.
     * Leads to the packet maker.
     * 
     * @param guid The message GUID
     * @param ttl  The message TTL must be 1 or more
     * @param type A MediaType object we'll turn into GGEP "M" with bits for audio and video to filter the search, or null to not do this
     * @return     A new QueryRequest object
     */
    public static QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl, MediaType type) {

        // Make sure the TTL is 1 or more
        if (ttl < 1) throw new IllegalArgumentException("Bad TTL.");

        // Make and return a new QueryRequest objet with the given values and defaults for a What's New query
        return new QueryRequest(
            guid,
            ttl,
            WHAT_IS_NEW_QUERY_STRING,                    // String   query                      The search text for a What's New query is "WhatIsNewXOXO"
            "",                                          // String   richQuery                  No XML
            null,                                        // Set      requestedUrnTypes          No URN types like "urn:sha1:"
            null,                                        // Set      queryUrns                  No URNs with actual hashes
            null,                                        // QueryKey queryKey                   QueryKey is part of GUESS, which LimeWire doesn't use anymore
            !RouterService.acceptedIncomingConnection(), // boolean  isFirewalled               Set the second flag bit if our TCP connect back tests have failed
            Message.N_UNKNOWN,                           // int      network                    We don't know if we will send this query over TCP or UDP yet
            true,                                        // boolean  canReceiveOutOfBandReplies We'll get hits out of band, so we don't need in band XML
            FeatureSearchData.WHAT_IS_NEW,               // int      featureSelector            Add the GGEP "WH" What is new extension with the value 1
            false,                                       // boolean  doNotProxy                 Leave out the "NP" No Proxy GGEP extension
            getMetaFlag(type));
    }

	/**
     * Not used.
     * 
	 * Creates a new query for the specified file name, with no XML.
	 * @param query the file name to search for
	 * @return a new <tt>QueryRequest</tt> for the specified query
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument is <tt>null</tt> or if the <tt>xmlQuery</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt> argument and the xml query are both zero-length (empty)
	 */
	public static QueryRequest createQuery(String query, String xmlQuery) {
        if (query == null) throw new NullPointerException("null query");
		if (xmlQuery == null) throw new NullPointerException("null xml query");
		if (query.length() == 0 && xmlQuery.length() == 0) throw new IllegalArgumentException("empty query");
		if (xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) throw new IllegalArgumentException("invalid XML");
		return new QueryRequest(newQueryGUID(false), query, xmlQuery);
	}

	/**
     * Not used.
     * 
	 * Creates a new query for the specified file name, with no XML.
	 * @param query the file name to search for
	 * @param ttl the time to live (ttl) of the query
	 * @return a new <tt>QueryRequest</tt> for the specified query and ttl
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt> argument is zero-length (empty)
	 * @throws <tt>IllegalArgumentException</tt> if the ttl value is negative or greater than the maximum allowed value
	 */
	public static QueryRequest createQuery(String query, byte ttl) {
        if (query == null) throw new NullPointerException("null query");
		if (query.length() == 0) throw new IllegalArgumentException("empty query");
		if (ttl <= 0 || ttl > 6) throw new IllegalArgumentException("invalid TTL: "+ttl);
		return new QueryRequest(newQueryGUID(false), ttl, query);
	}

	/**
     * Not used.
     * 
	 * Creates a new query with the specified guid, query string, and xml query string.
	 * @param guid the message GUID for the query
	 * @param query the query string
	 * @param xmlQuery the xml query string
	 * @return a new <tt>QueryRequest</tt> for the specified query, xml query, and guid
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument is <tt>null</tt>, if the <tt>xmlQuery</tt> argument is <tt>null</tt>, or if the <tt>guid</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the guid length is not 16, if both the query strings are empty, or if the XML does not appear to be valid
	 */
	public static QueryRequest createQuery(byte[] guid, String query, String xmlQuery) {
        query = I18NConvert.instance().getNorm(query);
		if (guid == null) throw new NullPointerException("null guid");
		if (guid.length != 16) throw new IllegalArgumentException("invalid guid length");
        if (query == null) throw new NullPointerException("null query");
        if (xmlQuery == null) throw new NullPointerException("null xml query");
		if (query.length() == 0 && xmlQuery.length() == 0) throw new IllegalArgumentException("empty query");
		if (xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) throw new IllegalArgumentException("invalid XML");
		return new QueryRequest(guid, query, xmlQuery);
	}

	/**
     * Make a new query for us to send with search text and XML.
     * RouterService.query() calls this.
     * Leads to the packet maker.
     * 
     * @param guid     The message GUID
     * @param query    The search text
     * @param xmlQuery XML search
     * @param type     A MediaType object we'll turn into GGEP "M" with bits for audio and video to filter the search, or null to not do this
     * @return         A new QueryRequest object
	 */
	public static QueryRequest createQuery(byte[] guid, String query, String xmlQuery, MediaType type) {

        // Replace accented characters in the search text with core characters to get more hits
        query = I18NConvert.instance().getNorm(query);

        // Make sure the GUID is 16 bytes
		if (guid == null) throw new NullPointerException("null guid");
		if (guid.length != 16) throw new IllegalArgumentException("invalid guid length");

        // Make sure we have search text or XML, and the XML starts with "<?xml"
        if (query    == null) throw new NullPointerException("null query");
        if (xmlQuery == null) throw new NullPointerException("null xml query");
		if (query.length()    == 0 && xmlQuery.length() == 0)        throw new IllegalArgumentException("empty query");
		if (xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) throw new IllegalArgumentException("invalid XML");

        // Make and return a new QueryRequest object with the given values and more defaults
		return new QueryRequest(
            guid,
            DEFAULT_TTL, // byte ttl Default TTL 6
            query,
            xmlQuery,
            type);
	}

	/**
     * Copy a QueryRequest object, putting in a new TTL.
     * QueryHandler.createQuery() calls this.
     * Leads to the packet parser.
     * 
     * @param qr  The QueryRequest object to copy
     * @param ttl The TTL for the new copy
     * @return    A new QueryRequest object that's just the same, but with the new ttl
	 */
	public static QueryRequest createQuery(QueryRequest qr, byte ttl) {

        /*
         * Construct a query request that is EXACTLY like the other query,
         * but with a different TTL.
         */

        try {

            // Use the serialized data of the given QueryRequest object to make a new one, inserting the given TTL
	        return createNetworkQuery(qr.getGUID(), ttl, qr.getHops(), qr.PAYLOAD, qr.getNetwork());

        // Turn a parsing error into an IllegalArgumentException
	    } catch (BadPacketException ioe) { throw new IllegalArgumentException(ioe.getMessage()); }
	}

	/**
     * Copy a QueryRequest object, putting in a new GUID and marking it out of band for UDP.
     * The GUID should have our IP address in it.
     * ManagedConnection.tryToProxy() calls this.
     * Leads to the packet parser.
     * 
     * @param qr   The QueryRequest object to copy
     * @param guid The GUID for the new copy
     * @return     A new QueryRequest object that's a copy turned out of band and given the new GUID
	 */
	public static QueryRequest createProxyQuery(QueryRequest qr, byte[] guid) {

        // Make sure the GUID is 16 bytes
        if (guid.length != 16) throw new IllegalArgumentException("bad guid size: " + guid.length);

        /*
         * i can't just call a new constructor, i have to recreate stuff
         */

        // Copy the payload
        byte[] newPayload = new byte[qr.PAYLOAD.length];
        System.arraycopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);

        // In the first flags byte, set 0x04 to indicate we can receive UDP packets
        newPayload[0] |= SPECIAL_OUTOFBAND_MASK;

        try {

            // Use the serialized data of the given QueryRequest object to make a new one, inserting the given GUID
            return createNetworkQuery(guid, qr.getTTL(), qr.getHops(), newPayload, qr.getNetwork());

        // Turn a parsing error into an IllegalArgumentException
        } catch (BadPacketException ioe) { throw new IllegalArgumentException(ioe.getMessage()); }
	}

	/**
     * Copy a QueryRequest object, removing its out of band UDP marking.
     * MessageRouter.originateLeafQuery() calls this.
     * Leads to the packet parser.
     * 
     * @param qr The QueryRequest object to copy
     * @return   A new QueryRequest object that's a copy turned back in band for a TCP socket Gnutella connection
	 */
	public static QueryRequest unmarkOOBQuery(QueryRequest qr) {

        // Copy the payload
        byte[] newPayload = new byte[qr.PAYLOAD.length];
        System.arraycopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);

        // Switch from UDP and no XML to TCP and XML
        newPayload[0] &= ~SPECIAL_OUTOFBAND_MASK; // Set 0x04 to 0, we can't receive UDP packets
        newPayload[0] |= SPECIAL_XML_MASK;        // Set 0x20 to 1, we want XML metadata in the in band results we get

        try {

            // Use the serialized data of the given QueryRequest object to make a new one
            return createNetworkQuery(qr.getGUID(), qr.getTTL(), qr.getHops(), newPayload, qr.getNetwork());

        // Turn a parsing error into an IllegalArgumentException
        } catch (BadPacketException ioe) { throw new IllegalArgumentException(ioe.getMessage()); }
	}

    /**
     * Not used, LimeWire no longer uses GUESS.
     * 
     * Creates a new query with the specified query key for use in GUESS-style UDP queries.
     * @param query the query string
     * @param key the query key
	 * @return a new <tt>QueryRequest</tt> instance with the specified query string and query key
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument is <tt>null</tt> or if the <tt>key</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt> argument is zero-length (empty)
     */
    public static QueryRequest createQueryKeyQuery(String query, QueryKey key) {
        if (query == null) throw new NullPointerException("null query");
		if (query.length() == 0) throw new IllegalArgumentException("empty query");
        if (key == null) throw new NullPointerException("null query key");
        return new QueryRequest(newQueryGUID(false), (byte)1, query, "", UrnType.ANY_TYPE_SET, EMPTY_SET, key, !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, false, 0, false, 0);
    }

    /**
     * Not used, LimeWire no longer uses GUESS.
     * 
     * Creates a new query with the specified query key for use in GUESS-style UDP queries.
     * @param sha1 the URN
     * @param key the query key
	 * @return a new <tt>QueryRequest</tt> instance with the specified URN request and query key
	 * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument is <tt>null</tt> or if the <tt>key</tt> argument is <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt> argument is zero-length (empty)
     */
    public static QueryRequest createQueryKeyQuery(URN sha1, QueryKey key) {
        if (sha1 == null) throw new NullPointerException("null sha1");
        if (key == null) throw new NullPointerException("null query key");
		Set sha1Set = new HashSet();
		sha1Set.add(sha1);
        return new QueryRequest(newQueryGUID(false), (byte) 1, DEFAULT_URN_QUERY, "", UrnType.SHA1_SET, sha1Set, key, !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, false, 0, false, 0);
    }

	/**
     * Make a query to send UDP multicast on the LAN.
     * MessageRouter.sendDynamicQuery() calls this.
     * Leads to the packet parser.
     * 
	 * Creates a new QueryRequest instance for multicast queries.
	 * This is necessary due to the unique properties of multicast queries,
     * such as the firewalled bit not being set regardless of whether or not the node is truly firewalled/NATted to the world outside the subnet.
     * 
     * @param qr The QueryRequest object to copy to make the multicast query
     * @return   A new QueryRequest object that's a copy modified to be a multicast query
	 */
	public static QueryRequest createMulticastQuery(QueryRequest qr) {

        // Make sure the caller gave us a QueryRequest object to copy
		if (qr == null) throw new NullPointerException("null query");

        /*
         * modify the payload to not be OOB.
         */

        // Copy the payload
        byte[] newPayload = new byte[qr.PAYLOAD.length];
        System.arraycopy(qr.PAYLOAD, 0, newPayload, 0, newPayload.length);

        // Switch from UDP and no XML to TCP and XML
        newPayload[0] &= ~SPECIAL_OUTOFBAND_MASK; // Set 0x04 to 0, we can't receive UDP packets
        newPayload[0] |= SPECIAL_XML_MASK;        // Set 0x20 to 1, we want XML metadata in the in band results we get

        try {

            // Use the serialized data of the given QueryRequest object to make a new one
            return createNetworkQuery(
                qr.getGUID(),
                (byte)1,              // TTL of 1
                qr.getHops(),
                newPayload,
                Message.N_MULTICAST); // This packet will travel over multicast UDP on the LAN

        // Turn a parsing error into an IllegalArgumentException
        } catch (BadPacketException ioe) { throw new IllegalArgumentException(ioe.getMessage()); }
	}

    /**
     * Not used, LimeWire no longer uses GUESS.
     * 
	 * Creates a new <tt>QueryRequest</tt> that is a copy of the input query, except that it includes the specified query key.
	 * @param qr the <tt>QueryRequest</tt> to use
	 * @param key the <tt>QueryKey</tt> to add
	 * @return a new <tt>QueryRequest</tt> from the specified query and key
     */
	public static QueryRequest createQueryKeyQuery(QueryRequest qr, QueryKey key) {
        // TODO: Copy the payload verbatim, except add the query-key into the GGEP section.
        return new QueryRequest(qr.getGUID(), qr.getTTL(), qr.getQuery(), qr.getRichQueryString(), qr.getRequestedUrnTypes(), qr.getQueryUrns(), key, qr.isFirewalledSource(), Message.N_UNKNOWN, qr.desiresOutOfBandReplies(), qr.getFeatureSelector(), false, qr.getMetaMask());
	}

	/**
     * Make a new browse host query packet with 4 spaces "    " as the search text.
     * BrowseHostUploadState.writeMessageHeaders() calls this.
     * Leads to the packet maker.
	 */
	public static QueryRequest createBrowseHostQuery() {

        // Make and return a new QueryRequest object with values for a browse host query
		return new QueryRequest(
            newQueryGUID(false),                         // byte[]   guid                       Make a new LimeWire GUID
            (byte)1,                                     // byte     ttl                        TTL of 1
            FileManager.INDEXING_QUERY,                  // String   query                      "    " A String of 4 spaces
            "",                                          // String   richQuery                  No XML
            UrnType.ANY_TYPE_SET,                        // Set      requestedUrnTypes          A HashSet with 1 UrnType object in it, UrnType.ANY_TYPE, which is new UrnType("")
            EMPTY_SET,                                   // Set      queryUrns                  An empty HashSet, this is not a search by hash
            null,                                        // QueryKey queryKey                   QueryKey is a part of GUESS, which LimeWire doesn't use anymore
            !RouterService.acceptedIncomingConnection(), // boolean  isFirewalled               Set the second flag bit if our TCP connect back tests have failed
            Message.N_UNKNOWN,                           // int      network                    We don't know if we will send this query over TCP or UDP yet
            false,                                       // boolean  canReceiveOutOfBandReplies Request XML metadata in the in band hits that flow back to us
            0,                                           // int      featureSelector            Leave out the "WH" What is new GGEP extension
            false,                                       // boolean  doNotProxy                 Leave out the "NP" No Proxy GGEP extension
            0);
	}

	/**
     * Not used.
     * 
	 * Specialized constructor used to create a query without the firewalled bit set.  This should primarily be used for testing.
	 * @param query the query string
	 * @return a new <tt>QueryRequest</tt> with the specified query string and without the firewalled bit set
	 */
	public static QueryRequest createNonFirewalledQuery(String query, byte ttl) {
		return new QueryRequest(newQueryGUID(false), ttl, query, "", UrnType.ANY_TYPE_SET, EMPTY_SET, null, false, Message.N_UNKNOWN, false, 0, false, 0);
	}

	/**
     * Parse the data of a query packet we've received into a new QueryRequest object.
     * QueryHandler.createQuery() and Message.createMessage() call this.
     * Leads to the packet parser.
     * 
	 * @param guid    The GUID from the Gnutella message header
	 * @param ttl     The TTL from the header
	 * @param hops    The hops count from the header
	 * @param payload The payload data
     * @return        A new QueryRequest object
	 */
	public static QueryRequest createNetworkQuery(byte[] guid, byte ttl, byte hops, byte[] payload, int network) throws BadPacketException {

        // Parse the header information and payload data into a new QueryRequest object
		return new QueryRequest(guid, ttl, hops, payload, network);
	}

    /**
     * Make a new query for us to send with the given information.
     * 
     * Builds a new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.
     * The GUID must have been created via newQueryGUID, this allows the caller to match up results.
     * 
     * @param guid  The GUID that marks this message unique
     * @param query Search text
     * @return      A new QueryRequest object representing a Gnutella query packet with all the given information in it
     */
    private QueryRequest(byte[] guid, String query) {

        // Make and return a new QueryRequest object with the given information and more defaults
        this(
            guid,
            query,
            ""); // String xmlQuery No XML
    }

    /**
     * Not used.
     * 
     * Builds a new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.  GUID must have
     * been created via newQueryGUID; this allows the caller to match up results
     */
    private QueryRequest(byte[] guid, byte ttl, String query) {
        this(guid, ttl, query, "");
    }

    /**
     * Make a new query for us to send with the given information.
     * 
     * Builds a new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.
     * The GUID must have been created via newQueryGUID, this allows the caller to match up results.
     * 
     * @param guid     The GUID that marks this message unique
     * @param query    Search text
     * @param xmlQuery A String of XML with a rich query
     * @return         A new QueryRequest object representing a Gnutella query packet with all the given information in it
     */
    private QueryRequest(byte[] guid, String query, String xmlQuery) {

        // Make and return a new QueryRequest object with the given information and more defaults
        this(
            guid,
            DEFAULT_TTL, // byte ttl Default TTL 6
            query,
            xmlQuery);
    }

    /**
     * Make a new query for us to send with the given information.
     * 
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.
     * The GUID must have been created via newQueryGUID, this allows the caller to match up results.
     * 
     * @param guid      The GUID that marks this message unique
     * @param ttl       The number of times this query will be able to travel between ultrapeers
     * @param query     Search text
     * @param richQuery A String of XML with a rich query
     * @return          A new QueryRequest object representing a Gnutella query packet with all the given information in it
     */
    private QueryRequest(byte[] guid, byte ttl, String query, String richQuery) {

        // Make and return a new QueryRequest object with the given information and more defaults
        this(
            guid,
            ttl,
            query,
            richQuery,
            UrnType.ANY_TYPE_SET,                        // Set      requestedUrnTypes          A HashSet with 1 UrnType object in it, UrnType.ANY_TYPE, which is new UrnType("")
            EMPTY_SET,                                   // Set      queryUrns                  An empty HashSet, this is not a search by hash
            null,                                        // QueryKey queryKey                   QueryKey is a part of GUESS, which LimeWire doesn't use anymore
            !RouterService.acceptedIncomingConnection(), // boolean  isFirewalled               Set the second flag bit if our TCP connect back tests have failed
            Message.N_UNKNOWN,                           // int      network                    We don't know if we will send this query over TCP or UDP yet
            false,                                       // boolean  canReceiveOutOfBandReplies Request XML metadata in the in band hits that flow back to us
            0,                                           // int      featureSelector            Leave out the "WH" What is new GGEP extension
            false,                                       // boolean  doNotProxy                 Leave out the "NP" No Proxy GGEP extension
            0);                                          // int      metaFlagMask               Don't add GGEP "M" Meta that would filter by media type
    }

    /**
     * Make a new query for us to send with the given information.
     * 
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.
     * The GUID must have been created via newQueryGUID, this allows the caller to match up results.
     * 
     * @param guid      The GUID that marks this message unique
     * @param ttl       The number of times this query will be able to travel between ultrapeers
     * @param query     Search text
     * @param richQuery A String of XML with a rich query
     * @param type      A MediaType object we'll turn into the flags byte value of the "M" Meta GGEP extension, which filters for audio or video
     * @return          A new QueryRequest object representing a Gnutella query packet with all the given information in it
     */
    private QueryRequest(byte[] guid, byte ttl, String query, String richQuery, MediaType type) {

        // Make and return a new QueryRequest object with the given information and more defaults
        this(
            guid,
            ttl,
            query,
            richQuery,
            UrnType.ANY_TYPE_SET,                        // Set      requestedUrnTypes          A HashSet with 1 UrnType object in it, UrnType.ANY_TYPE, which is new UrnType("")
            EMPTY_SET,                                   // Set      queryUrns                  An empty HashSet, this is not a search by hash
            null,                                        // QueryKey queryKey                   QueryKey is a part of GUESS, which LimeWire doesn't use anymore
            !RouterService.acceptedIncomingConnection(), // boolean  isFirewalled               Set the second flag bit if our TCP connect back tests have failed
            Message.N_UNKNOWN,                           // int      network                    We don't know if we will send this query over TCP or UDP yet
            false,                                       // boolean  canReceiveOutOfBandReplies Request XML metadata in the in band hits that flow back to us
            0,                                           // int      featureSelector            Leave out the "WH" What is new GGEP extension
            false,                                       // boolean  doNotProxy                 Leave out the "NP" No Proxy GGEP extension
            getMetaFlag(type));
    }

    /**
     * Not used.
     * 
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.
     * The GUID must have been created via newQueryGUID, this allows the caller to match up results.
     */
    private QueryRequest(byte[] guid, byte ttl, String query, String richQuery, boolean canReceiveOutOfBandReplies) {
        this(guid, ttl, query, richQuery, UrnType.ANY_TYPE_SET, EMPTY_SET, null, !RouterService.acceptedIncomingConnection(), Message.N_UNKNOWN, canReceiveOutOfBandReplies, 0, false, 0);
    }

    /**
     * Make a new query for us to send with the given information.
     * 
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid.
     * The GUID must have been created via newQueryGUID, this allows the caller to match up results.
     * 
     * @param guid                       The GUID that marks this message unique
     * @param ttl                        The number of times this query will be able to travel between ultrapeers
     * @param query                      Search text
     * @param richQuery                  A String of XML with a rich query
     * @param canReceiveOutOfBandReplies True if we can get UDP, and don't need XML in band, false if we can't and do
     * @param type                       A MediaType object we'll turn into the flags byte value of the "M" Meta GGEP extension, which filters for audio or video
     * @return                           A new QueryRequest object representing a Gnutella query packet with all the given information in it
     */
    private QueryRequest(byte[] guid, byte ttl, String query, String richQuery, boolean canReceiveOutOfBandReplies, MediaType type) {

        // Make and return a new QueryRequest object with the given information and more defaults
        this(
            guid,
            ttl,
            query,
            richQuery,
            UrnType.ANY_TYPE_SET,                        // Set      requestedUrnTypes A HashSet with 1 UrnType object in it, UrnType.ANY_TYPE, which is new UrnType("")
            EMPTY_SET,                                   // Set      queryUrns         An empty HashSet, this is not a search by hash
            null,                                        // QueryKey queryKey          QueryKey is a part of GUESS, which LimeWire doesn't use anymore
            !RouterService.acceptedIncomingConnection(), // boolean  isFirewalled      Set the second flag bit if our TCP connect back tests have failed
            Message.N_UNKNOWN,                           // int      network           We don't know if we will send this query over TCP or UDP yet
            canReceiveOutOfBandReplies,
            0,                                           // int      featureSelector   Leave out the "WH" What is new GGEP extension
            false,                                       // boolean  doNotProxy        Leave out the "NP" No Proxy GGEP extension
            getMetaFlag(type));
    }

    /**
     * Turn a MediaType object into the byte value of the GGEP "M" Meta extension.
     * type.getDescriptionKey() returns values like MediaType.AUDIO, which matches the bit 0x04 AUDIO_MASK for the "M" extension value.
     * 
     * @param type A MediaType object.
     * @return     An int with bits set for media types like audio and video, the value of the GGEP "M" Meta extension.
     *             0 if type is null to not include "M" to filter on media type.
     */
    private static int getMetaFlag(MediaType type) {

        // Start out with the "M" extension value an int with the lowest 8 bits all 0
        int metaFlag = 0;

        // If the caller gave us null instead of a MediaType
        if (type == null) ; // Leave metaFlag 0 to not include the GGEP "M" extension at all

        // Otherwise, sort by the MediaType description key, and flip the bit for each kind of media
        else if (type.getDescriptionKey() == MediaType.AUDIO)     metaFlag |= AUDIO_MASK;
        else if (type.getDescriptionKey() == MediaType.VIDEO)     metaFlag |= VIDEO_MASK;
        else if (type.getDescriptionKey() == MediaType.IMAGES)    metaFlag |= IMAGE_MASK;
        else if (type.getDescriptionKey() == MediaType.DOCUMENTS) metaFlag |= DOC_MASK;

        // The description key is for a computer program
        else if (type.getDescriptionKey() == MediaType.PROGRAMS) {

            // Flip the bit for the kind of computer we're running on
            if      (CommonUtils.isLinux() || CommonUtils.isAnyMac()) metaFlag |= LIN_PROG_MASK;
            else if (CommonUtils.isWindows())                         metaFlag |= WIN_PROG_MASK;
            else                                                      metaFlag |= (LIN_PROG_MASK | WIN_PROG_MASK); // Other operating system, request both kinds
        }

        // Return the byte of flags we composed
        return metaFlag;
    }

    /**
     * Not used.
     * 
     * Builds a new query from scratch but you can flag it as a Requery, if 
     * needed.  If you need to make a query that accepts out-of-band results, 
     * be sure to set the guid correctly (see GUID.makeAddressEncodedGUI) and 
     * set canReceiveOutOfBandReplies .
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
    public QueryRequest(byte[] guid, byte ttl, String query, String richQuery, Set requestedUrnTypes, Set queryUrns, QueryKey queryKey, boolean isFirewalled, int network, boolean canReceiveOutOfBandReplies, int featureSelector) {
        // calls me with the doNotProxy flag set to false
        this(guid, ttl, query, richQuery, requestedUrnTypes, queryUrns, queryKey, isFirewalled, network, canReceiveOutOfBandReplies, featureSelector, false, 0);
    }

    /**
     * Make a new query for us to send with the given information.
     * 
     * Builds a new query from scratch but you can flag it as a Requery, if needed.
     * If you need to make a query that accepts out-of-band results, be sure to set the guid correctly
     * (see GUID.makeAddressEncodedGUI) and set canReceiveOutOfBandReplies.
     * 
     * @param guid                       The GUID that marks this message unique
     * @param ttl                        The number of times this query will be able to travel between ultrapeers
     * @param query                      Search text
     * @param richQuery                  A String of XML with a rich query
     * @param requestedUrnTypes          A HashSet of UrnType objects like "urn:sha1:" to indicate this search gets SHA1 hashes in hits
     * @param queryUrns                  A HashSet of URN objects like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ", making this a search for the file with that hash
     * @param queryKey                   QueryKey is a part of GUESS, which LimeWire no longer uses
     * @param isFirewalled               Sets the second bit of the flags, indicating the searching computer isn't externally contactable
     * @param network                    The Internet protocol this packet will travel on, like Message.N_TCP or Message.N_UDP
     * @param canReceiveOutOfBandReplies True if we can get UDP, and don't need XML in band, false if we can't and do
     * @param featureSelector            The flags byte value for the "WH" What is new GGEP extension
     * @param doNotProxy                 True to add "NP" No Proxy to this query packet's GGEP block
     * @param metaFlagMask               The flags byte value for the "M" Meta GGEP extension which filters on media type like audio and video, or 0 to not do this
     * @return                           A new QueryRequest object representing a Gnutella query packet with all the given information in it
     */
    public QueryRequest(byte[] guid, byte ttl, String query, String richQuery, Set requestedUrnTypes, Set queryUrns, QueryKey queryKey, boolean isFirewalled, int network, boolean canReceiveOutOfBandReplies, int featureSelector, boolean doNotProxy, int metaFlagMask) {

        // Make and return a new QueryRequest object with the given information and more defaults
        this(
            guid,
            ttl,
            0, // int minSpeed Have the constructor compose default flags about us
            query,
            richQuery,
            requestedUrnTypes,
            queryUrns,
            queryKey,
            isFirewalled,
            network,
            canReceiveOutOfBandReplies,
            featureSelector,
            doNotProxy,
            metaFlagMask);
    }

    /**
     * Make a new QueryRequest object to represent a Gnutella query packet with all the given information.
     * The packet this constructor makes can include additional information about us.
     * This is the packet maker.
     * 
     * A query packet has this structure:
     * 
     * gnutella packet header 23 bytes
     * SS
     * search text\0
     * XML[0x1C]urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ[0x1C]urn:sha1:[0x1C][GGEP block]\0
     * 
     * The first 2 bytes of the payload are the speed flags.
     * The first byte contains flags, and the second is 0 to not impose a limit on the number of hits we'll receive.
     * 
     * mfx0 0ow0 0000 0000
     * m         We're using these 2 bytes to hold flags, not a speed
     *  f        The searching computer isn't externally contactable on the Internet
     *   x       The searching computer wants hits with XML metadata
     *       o   This query message was sent in a UDP packet (do)
     *        w  The searching computer can do UDP firewall-to-firewall file transfers
     * 
     * After that is the search text, which ends with a 0 byte.
     * The extended area doesn't contain any 0 bytes, and is null terminated.
     * It contains any number of extensions separated by 0x1C bytes.
     * We write the extensions in the following order.
     * 
     * The XML of a rich query.
     * A URN with a hash, making this a search for the file with that hash exactly.
     * URN types, "urn:sha1:" means we support SHA1 hashes and want them in hits.
     * The GGEP block.
     * 
     * Builds a new query from scratch but you can flag it as a Requery, if
     * needed.  If you need to make a query that accepts out-of-band results,
     * be sure to set the guid correctly (see GUID.makeAddressEncodedGUI) and
     * set canReceiveOutOfBandReplies.
     * 
     * @param guid                       The GUID that marks this message unique
     * @param ttl                        The number of times this query will be able to travel between ultrapeers
     * @param minSpeed                   Flags about the searching computer, or 0 to have this constructor compose default ones about us
     * @param query                      Search text
     * @param richQuery                  A String of XML with a rich query
     * @param requestedUrnTypes          A HashSet of UrnType objects like "urn:sha1:" to indicate this search gets SHA1 hashes in hits
     * @param queryUrns                  A HashSet of URN objects like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ", making this a search for the file with that hash
     * @param queryKey                   QueryKey is a part of GUESS, which LimeWire no longer uses
     * @param isFirewalled               Sets the second bit of the flags, indicating the searching computer isn't externally contactable
     * @param network                    The Internet protocol this packet will travel on, like Message.N_TCP or Message.N_UDP
     * @param canReceiveOutOfBandReplies True if we can get UDP, and don't need XML in band, false if we can't and do
     * @param featureSelector            The flags byte value for the "WH" What is new GGEP extension
     * @param doNotProxy                 True to add "NP" No Proxy to this query packet's GGEP block
     * @param metaFlagMask               The flags byte value for the "M" Meta GGEP extension which filters on media type like audio and video, or 0 to not do this
     * @return                           A new QueryRequest object representing a Gnutella query packet with all the given information in it
     */
    public QueryRequest(byte[] guid, byte ttl, int minSpeed, String query, String richQuery, Set requestedUrnTypes, Set queryUrns, QueryKey queryKey, boolean isFirewalled, int network, boolean canReceiveOutOfBandReplies, int featureSelector, boolean doNotProxy, int metaFlagMask) {

        // Call the Message constructor to save information in the Gnutella packet header
        super(
            guid,            // The GUID that marks this packet as unique
            Message.F_QUERY, // 0x80, this is a query packet
            ttl,             // The number of times this query will be able to travel between ultrapeers
            (byte)0,         // No hops yet
            0,               // We don't know the length yet, 0 for now
            network);        // The Internet protocol, like N_TCP or N_UDP, we'll send this packet on

        // Make sure the caller gave us text, XML, or a hash URN for this new query packet to search for
		if ((query     == null || query.length()     == 0) && // There is no search text, and
		    (richQuery == null || richQuery.length() == 0) && // There is no search XML, and
		    (queryUrns == null || queryUrns.size()   == 0)) { // There is no search hash
			throw new IllegalArgumentException("cannot create empty query");
        }

        // Make sure the search text is 30 characters or less, and the XML is 500 characters or less
        if (query     != null && query.length()     > MAX_QUERY_LENGTH)     throw new IllegalArgumentException("query too big: " + query);
        if (richQuery != null && richQuery.length() > MAX_XML_QUERY_LENGTH) throw new IllegalArgumentException("xml too big: "   + richQuery);

        // Make sure the search text doesn't contain any illegal characters
        if (query != null &&                                                                   // If we were given search text, and
            !(queryUrns != null && queryUrns.size() > 0 && query.equals(DEFAULT_URN_QUERY)) && // This isn't a search by hash, and
            hasIllegalChars(query)) {                                                          // The search text contains illegal characters
            throw new IllegalArgumentException("illegal chars: " + query);
        }

        // Save the given feature selector byte, it will be the value of the "WH" feature query GGEP extension
        if (featureSelector < 0) throw new IllegalArgumentException("Bad feature = " + featureSelector); // The bits should all be in the lowest byte
        _featureSelector = featureSelector;

        // Save the given media type filter byte, it will be the value of the "M" Meta GGEP extension
        if ((metaFlagMask > 0) && (metaFlagMask < 4) || (metaFlagMask > 248)) throw new IllegalArgumentException("Bad Meta Flag = " + metaFlagMask);
        if (metaFlagMask > 0) _metaMask = new Integer(metaFlagMask); // Wrap it in an Integer object so the _metaMask reference is not null

        // If the caller gave us no speed flags byte, we'll compose a default one here
        if (minSpeed == 0) {

            /*
             * the new Min Speed format - looks reversed but
             * it isn't because of ByteOrder.short2leb
             * 
             * SPECIAL_MINSPEED_MASK is 0x0080 which is 0000 0000 1000 0000.
             * When we write that to the wire, the least significant byte will go first, like 1000 0000 0000 0000.
             */

            // Set 0x80 1000 0000, marking the speed bytes as holding flags instead of a minimum speed
            minSpeed = SPECIAL_MINSPEED_MASK;

            // Set the next bit if the caller gave us isFirewalled true
            if (isFirewalled &&                    // The caller says the sender of this query packet is firewalled, and
                !isMulticast())                    // The caller gave us an Internet protocol equal to something other than N_MULTICAST
                minSpeed |= SPECIAL_FIREWALL_MASK; // Add 0x40, making minSpeed 1100 0000

            // The caller gave us isFirewalled true, but we can do firewall-to-firewall transfers
            if (isFirewalled && UDPService.instance().canDoFWT()) minSpeed |= SPECIAL_FWTRANS_MASK; // Set 0x02 1100 0010

            /*
             * THE DEAL:
             * if we can NOT receive out of band replies, we want in-band XML -
             * so set the correct bit.
             * if we can receive out of band replies, we do not want in-band XML
             * we'll hope the out-of-band reply guys will provide us all
             * necessary XML.
             */

            // If we can receive UDP packets, say so with 0x04, otherwise ask for XML in the TCP hits that flow back to use with 0x20
            if (!canReceiveOutOfBandReplies) minSpeed |= SPECIAL_XML_MASK;       // We can't receive UDP hits, request XML with   0x20 0010 0000
            else                             minSpeed |= SPECIAL_OUTOFBAND_MASK; // Remote computers can send us UDP packets, add 0x04 0100 0000
        }

        // Save the speed flags byte we were gave or just composed
        MIN_SPEED = minSpeed; // Leave the second byte 0 to not specify a maximum number of hits

        // Save the search text
		if (query == null) this.QUERY = ""; // Turn null into blank
		else               this.QUERY = query;

        // Save the XML, parsing it into a LimeXMLDocument object
		if (richQuery == null || richQuery.equals("") ) {
			this.XML_DOC = null;
		} else {
		    LimeXMLDocument doc = null;
		    try {
		        doc = new LimeXMLDocument(richQuery);
            } catch(SAXException ignored) {
            } catch(SchemaNotFoundException ignored) {
            } catch(IOException ignored) {}
            this.XML_DOC = doc;
		}

        // Copy the references in the given Set objects, and point to empty sets instead of null
		Set tempRequestedUrnTypes = null;
		Set tempQueryUrns = null;
		if (requestedUrnTypes != null) tempRequestedUrnTypes = new HashSet(requestedUrnTypes); // The URN types, like "urn:sha1:" that specify support for SHA1
		else                           tempRequestedUrnTypes = EMPTY_SET;
		if (queryUrns != null) tempQueryUrns = new HashSet(queryUrns); // The URNs, like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ" that contain the hash of a file
		else                   tempQueryUrns = EMPTY_SET;

        // QueryKey is a part of GUESS, which LimeWire doesn't use anymore
        this.QUERY_KEY = queryKey;

        // Save the given doNotProxy option, which if true will add "NP" No Proxy to this query packet's GGEP block
        this._doNotProxy = doNotProxy;

        // Write the data of the payload
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); // Make a new ByteArrayOutputStream that will grow to hold the data we write to it
        try {
            
            /*
             * A query packet looks like this:
             * 
             * gnutella packet header 23 bytes
             * speed flags 2
             * search\0
             * extended area\0
             * 
             * The header is held by the Message object this QueryRequest extends.
             * This method writes the payload, which follows.
             * 
             * SS
             * search text\0
             * XML[0x1C]urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ[0x1C]urn:sha1:[0x1C][GGEP block]\0
             * 
             * The first 2 bytes are the speed flags.
             * After that is the search text, which ends with a 0 byte.
             * The extended area doesn't contain any 0 bytes, and is null terminated.
             * It contains any number of extensions separated by 0x1C bytes.
             * We write the extensions in the following order.
             * 
             * The XML of a rich query.
             * A URN with a hash, making this a search for the file with that hash exactly.
             * URN types, "urn:sha1:" means we support SHA1 hashes and want them in hits.
             * The GGEP block.
             */

            // Write the 2 speed flag bytes
            ByteOrder.short2leb((short)MIN_SPEED, baos);

            // Write the null terminated search text
            baos.write(QUERY.getBytes("UTF-8")); // UTF-8 is ASCII
            baos.write(0);                       // Write a 0 byte, the null terminator

            /*
             * now write any & all HUGE v0.93 General Extension Mechanism
             * extensions
             */

            /*
             * this specifies whether or not the extension was successfully
             * written, meaning that the HUGE GEM delimiter should be
             * written before the next extension
             */

            // True if we need to write 0x1C before a second or additional extension
            boolean addDelimiterBefore = false;

            // Add the rich query XML text
            byte[] richQueryBytes = null;
            if (XML_DOC != null) richQueryBytes = richQuery.getBytes("UTF-8");
            addDelimiterBefore = writeGemExtension(baos, addDelimiterBefore, richQueryBytes); // Won't write 0x1C before, and returns true if there was XML to write

            // Add the URNs like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ", indicating a search for exactly that file
            addDelimiterBefore = writeGemExtensions(baos, addDelimiterBefore, tempQueryUrns == null ? null : tempQueryUrns.iterator());

            // Add the URN types like "urn:sha1:" indicating we want hits with SHA1 hashes in them
            addDelimiterBefore = writeGemExtensions(baos, addDelimiterBefore, tempRequestedUrnTypes == null ? null : tempRequestedUrnTypes.iterator());

            /*
             * Add the GGEP block.
             */

            // Make a new empty GGEP block
            GGEP ggepBlock = new GGEP(false); // Pass false to enable COBS encoding, this will make sure there are no 0 bytes in the GGEP block

            // QueryKey is a part of GUESS, which LimeWire no longer uses
            if (this.QUERY_KEY != null) {
                ByteArrayOutputStream qkBytes = new ByteArrayOutputStream();
                this.QUERY_KEY.write(qkBytes);
                ggepBlock.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT, qkBytes.toByteArray());
            }

            // If the caller gave us a feature selector byte, make it the value of the "WH" What is new feature query
            if (_featureSelector > 0) ggepBlock.put(GGEP.GGEP_HEADER_FEATURE_QUERY, _featureSelector);

            // If the caller gave us doNotProxy true, add the "NP" No Proxy extension, which doesn't have a value
            if (doNotProxy) ggepBlock.put(GGEP.GGEP_HEADER_NO_PROXY);

            // If the caller gave us a byte of flags that filter the search to certain media types, make it the value of "M" Meta
            if (_metaMask != null) ggepBlock.put(GGEP.GGEP_HEADER_META, _metaMask.intValue());

            // If we just added at least 1 extension to our GGEP block
            if ((this.QUERY_KEY != null) || (_featureSelector > 0) || _doNotProxy || (_metaMask != null)) {

                // Write a 0x1C byte and the data of the GGEP block
                ByteArrayOutputStream ggepBytes = new ByteArrayOutputStream();                             // Make ggepBytes to hold the data of the GGEP block
                ggepBlock.write(ggepBytes);                                                                // Have the GGEP object write its data to ggepBytes
                addDelimiterBefore = writeGemExtension(baos, addDelimiterBefore, ggepBytes.toByteArray()); // Add that data to baos
            }

            // Write the 0 byte that terminates the extended area
            baos.write(0);

        // The call richQuery.getBytes("UTF-8") didn't recognize ASCII encoding
		} catch (UnsupportedEncodingException uee) {

            /*
             * this should never happen from the getBytes("UTF-8") call
             * but there are UnsupportedEncodingExceptions being reported
             * with UTF-8.
             * Is there other information we want to pass in as the message?
             */

            // Treat it as an illegal argument
            throw new IllegalArgumentException("could not get UTF-8 bytes for query :" + QUERY + " with richquery :" + richQuery);

        // There was a problem writing to a ByteArrayOutputStream we made, log it and keep going
        } catch (IOException e) { ErrorService.error(e); }

        // Save the data of the payload
		PAYLOAD = baos.toByteArray();
		updateLength(PAYLOAD.length); // Keep the payload length in the Gnutella packet header

        // Save the HashSet objects of URN and UrnType objects we set up to point to the given lists or an empty list in place of null
		this.QUERY_URNS          = Collections.unmodifiableSet(tempQueryUrns);
		this.REQUESTED_URN_TYPES = Collections.unmodifiableSet(tempRequestedUrnTypes);
    }

    /**
     * Make a new QueryRequest object to represent a Gnutella query packet from data we received from a remote computer.
     * This is the packet parser.
     * 
     * @param guid    The GUID from the packet header
     * @param ttl     The TTL from the packet header
     * @param hops    The hops count from the packet header
     * @param payload The packet payload data, including the speed byte, search text, and extended area with hash URNs and a GGEP block
     * @param network The Internet protocol this packet came to us over, like Message.N_TCP or Message.N_UDP
     */
    private QueryRequest(byte[] guid, byte ttl, byte hops, byte[] payload, int network) throws BadPacketException {

        // Save the GUID, message type byte, TTL and hops counts, payload length, and source protocol in the Message part of this QueryRequest object
        super(guid, Message.F_QUERY, ttl, hops, payload.length, network);

        // Query packets must have a payload, save it
		if (payload == null) throw new BadPacketException("no payload");
		PAYLOAD = payload;

        // We'll parse data into these temporary variables, check them, and then set corresponding member variables
		String   tempQuery             = "";   // The search text before the first 0 byte
		String   tempRichQuery         = "";   // The XML extension, text that starts "<?xml"
		int      tempMinSpeed          = 0;    // The flags byte that starts the query packet payload
		Set      tempQueryUrns         = null; // A HashSet of URN objects representing text like "urn:sha1:" with no hash
		Set      tempRequestedUrnTypes = null; // A HashSet of UrnType objects representing text like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ"
        QueryKey tempQueryKey          = null; // QueryKey is a part of GUESS, which LimeWire no longer uses

        try {

            // Wrap the entire payload into a ByteArrayInputStream that we can call read() on to get the data a piece at a time
            ByteArrayInputStream bais = new ByteArrayInputStream(this.PAYLOAD);

            /*
             * A query payload looks like this:
             * 
             * FF
             * search text\0
             * extension[0x1C]extesion[0x1C]extension\0
             * 
             * FF are the 2 flags bytes
             * The search text is null terminated.
             * The extensions are separated by 0x1C bytes, don't contain null bytes, and are terminated by one.
             */

            // Read the minimum speed flags 2 bytes
			short sp = ByteOrder.leb2short(bais);    // Read 2 bytes, and return it in a short
			tempMinSpeed = ByteOrder.ushort2int(sp); // Turn that into an int

            // Read the search text after that
            tempQuery = new String(super.readNullTerminatedBytes(bais), "UTF-8"); // Read the text and the 0 from bais, and turn the returned bytes into a String

            // Read the extensions, which can't contain a 0 byte and are terminated by one, and turn them into a HUGEExtension object
            byte[] extsBytes = super.readNullTerminatedBytes(bais); // Read the bytes and the 0 from bais, but return just the bytes up to the 0
            HUGEExtension huge = new HUGEExtension(extsBytes);      // Have the HUGEExtension constructor find and parse them

            // Get the GGEP object the HUGEExtension constructor found and parsed
            GGEP ggep = huge.getGGEP();
            if (ggep != null) {

                try {

                    // QueryKey is a part of GUESS, which LimeWire no longer uses
                    if (ggep.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                        byte[] qkBytes = ggep.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
                        tempQueryKey = QueryKey.getQueryKey(qkBytes, false);
                    }

                    // The query's GGEP block has the "WH" feature query extension, get its number value
                    if (ggep.hasKey(GGEP.GGEP_HEADER_FEATURE_QUERY)) _featureSelector = ggep.getInt(GGEP.GGEP_HEADER_FEATURE_QUERY);

                    // If the query's GGEP block has the "NP" No Proxy extension, set _doNotProxy to true
                    if (ggep.hasKey(GGEP.GGEP_HEADER_NO_PROXY)) _doNotProxy = true;

                    // The query's GGEP block has the "M" Meta extension
                    if (ggep.hasKey(GGEP.GGEP_HEADER_META)) {

                        // Read the byte that has flags that show what kind of media, like audio or video, this search is for
                        _metaMask = new Integer(ggep.getInt(GGEP.GGEP_HEADER_META));
                        if ((_metaMask.intValue() < 4) || (_metaMask.intValue() > 248)) _metaMask = null; // Make sure the value is good
                    }

                // There was a problem reading one of the GGEP extension values
                } catch (BadGGEPPropertyException ignored) {}
            }

            // Get the lists of the URNs and URN types in the extended area
            tempQueryUrns         = huge.getURNS();     // A HashSet of URN objects representing text like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ"
            tempRequestedUrnTypes = huge.getURNTypes(); // A HashSet of UrnType objects representing text like "urn:sha1:" with no hash

            // Loop through the other text extensions the HUGEExtension constructor found
            for (Iterator iter = huge.getMiscBlocks().iterator(); iter.hasNext() && tempRichQuery.equals(""); ) { // Until we've set tempRichQuery
                String currMiscBlock = (String)iter.next();

                // If this text extension starts "<?xml", copy it into tempRichQuery
                if (currMiscBlock.startsWith("<?xml")) tempRichQuery = currMiscBlock;
            }

        // Turning the search text into the String tempQuery caused an exception
        } catch (UnsupportedEncodingException uee) {

            // This packet is bad
            throw new BadPacketException(uee.getMessage());

        // Unable to read parts of the packet
        } catch (IOException ioe) {

            // Record it in the ErrorService and keep going
            ErrorService.error(ioe);
        }

        // Save the search text in this QueryRequest object
        QUERY = tempQuery;

        // Parse the XML text into a XML document, and save it as XML_DOC
	    LimeXMLDocument tempDoc = null;                   // If we didn't find XML or there is an error parsing it, tempDoc will remain null
	    try {
	        tempDoc = new LimeXMLDocument(tempRichQuery); // Parse the XML text into a new LimeXMLDocument
        } catch(SAXException ignored) {                   // Ignore parsing exceptions, and leave tempDoc null
        } catch(SchemaNotFoundException ignored) {
        } catch(IOException ignored) {}
        this.XML_DOC = tempDoc;                           // Save the LimeXMLDocument in this QueryRequest object

        // Save the speed byte, which is actually a group of flags
		MIN_SPEED = tempMinSpeed;

        // Save the HashSet of URN objects representing text extensions like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ"
        if (tempQueryUrns == null) QUERY_URNS = EMPTY_SET;
        else                       QUERY_URNS = Collections.unmodifiableSet(tempQueryUrns);

        // Save the HashSet of UrnType objects representing text extensions like "urn:sha1:" with no hash
        if (tempRequestedUrnTypes == null) REQUESTED_URN_TYPES = EMPTY_SET;
        else                               REQUESTED_URN_TYPES = Collections.unmodifiableSet(tempRequestedUrnTypes);

        // QueryKey is a part of GUESS, which LimeWire doesn't use anymore
        QUERY_KEY = tempQueryKey;

        // Make sure there is a search, either in normal text, XML, or by hash
		if (QUERY.length()         == 0 && // The main query text in the packet is blank, and
            tempRichQuery.length() == 0 && // The wasn't a text extension that started "<?xml" either, so, no XML search, and
            QUERY_URNS.size()      == 0) { // There wasn't even a hash search like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ"

            // This query packet isn't asking for anything
		    ReceivedErrorStat.QUERY_EMPTY.incrementStat();
			throw new BadPacketException("empty query");
		}

        // Make sure the search text is 30 characters or less
        if (QUERY.length() > MAX_QUERY_LENGTH) {
            ReceivedErrorStat.QUERY_TOO_LARGE.incrementStat();
            throw new BadPacketException("query too big: " + QUERY);
        }        

        // Make sure the XML is 500 characters or less
        if (tempRichQuery.length() > MAX_XML_QUERY_LENGTH) {
            ReceivedErrorStat.QUERY_XML_TOO_LARGE.incrementStat();
            throw new BadPacketException("xml too big: " + tempRichQuery);
        }

        /*
         * A packet searching by hash looks like this:
         * QUERY_URNS will have at least 1 URN object representing a hash search like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ".
         * The QUERY text will just be "\", a single backslash.
         */

        // Make sure the query text doesn't have any illegal characters
        if (!(QUERY_URNS.size() > 0 && QUERY.equals(DEFAULT_URN_QUERY)) && // If this packet isn't searching by hash, and
            hasIllegalChars(QUERY)) {                                      // The query text has illegal characters
            ReceivedErrorStat.QUERY_ILLEGAL_CHARS.incrementStat();         // Drop it
            throw new BadPacketException("illegal chars: " + QUERY);
        }
    }

    /**
     * Determine if a given string contains an illegal character for search text, like _ # ! |.
     * 
     * @param query A String
     * @return      True if the given text contains an illegal character
     */
    private static boolean hasIllegalChars(String query) {

        // Search the text for each illegal character, returning true on found
        return StringUtils.containsCharacters(query, ILLEGAL_CHARS);
    }

    /**
     * Generate a LimeWire GUID, optionally with the requery tag at byte 13.
     * 
     * @param isRequery True to generate a LimeWire GUID marked as a requery, false to make a LimeWire GUID without that marking
     * @return          A new GUID in a byte array of 16 bytes
     */
    public static byte[] newQueryGUID(boolean isRequery) {

        // Call makeGuidRequery() or makeGuid()
        return isRequery ? GUID.makeGuidRequery() : GUID.makeGuid();
	}

    /**
     * Have this QueryRequest object write the bytes of its payload to you.
     * 
     * @param out The OutputStream we can call out.write() on
     */
    protected void writePayload(OutputStream out) throws IOException {

        // Give it the PAYLOAD data we already parsed or serialized
        out.write(PAYLOAD);

        // Record that we sent this query packet over TCP
		SentMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(this);
    }

    /**
     * Get the payload of this query packet.
     *
     * @return A byte array.
     */
    public byte[] getPayload() {

        // Return the byte array we parsed or serialized
        return PAYLOAD;
    }

    /**
     * Get the search text in this query packet.
     *
     * The caller should not call the getBytes() method on the returned value,
     * as this seems to cause problems on the Japanese Macintosh.  If you need
     * the raw bytes of the query string, call getQueryByteAt(int).
     * 
     * @return A String with the search text
     */
    public String getQuery() {

        // Return the String we saved
        return QUERY;
    }

	/**
     * Get the XML rich query.
     * This XML exists as a text extension in the extended area, along with hash URNs and the GGEP block.
	 * 
	 * @return The XML as a LimeXMLDocument
	 */
    public LimeXMLDocument getRichQuery() {

        // Return the LimeXMLDocument we parsed
        return XML_DOC;
    }

    /**
     * Get the XML rich query.
     * This XML exists as a text extension in the extended area, along with hash URNs and the GGEP block.
     * 
     * @return The XML as a String.
     *         If there's no XML in this query packet, returns null, not blank.
     */
    private String getRichQueryString() {

        // Return null, or the parsed XML written out again as a String
        if (XML_DOC == null) return null;
        else                 return XML_DOC.getXMLString();
    }

	/**
     * The types of hash URNs this search wants results to specify.
     * Represents text extensions like "urn:sha1:" with no hash.
     * This is how a search indicates it wants the SHA1 hash included in results.
     * 
     * @return A HashSet of UrnType objects.
     *         If this query packet has no text extensions like "urn:sha1:", returns an empty Set, not null.
	 */
    public Set getRequestedUrnTypes() {

        // Return the Set we saved
		return REQUESTED_URN_TYPES;
    }

	/**
     * The hash URNs this search is for.
     * Represents text extensions like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ".
     * This is a search by hash.
     * 
     * @return A HashSet of URN objects.
     *         If this query packet has no text hash URNs, returns an empty Set, not null.
	 */
    public Set getQueryUrns() {

        // Return the Set we saved
        return QUERY_URNS;
    }

	/**
     * Determine if this QueryRequest is searching by hash.
     * If it is, it will have a text extension like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ".
     * We parsed that into a URN object, and added it to the HashSet QUERY_URNS.
     * 
     * @return True if this query is searching by hash, false if it's not
	 */
	public boolean hasQueryUrns() {

        // If we found a hash in this search, QUERY_URNS won't be empty
		return !QUERY_URNS.isEmpty();
	}

    /**
     * Get the speed flags byte.
     * 
	 * The speed flags is a 2 byte unsigned number.
     * The Java short type, however, is signed.
     * So, we keep the 2 bytes in an int.
	 */
	public int getMinSpeed() {

        // Return the int with the flags we parsed or composed
        return MIN_SPEED;
	}

    /**
     * Determine if the searching computer isn't externally contactable for TCP socket connections.
     * Looks for 0x40, the bit at 0100 0000, in the speed flags bytes.
     * 
     * @return True if the speed flags byte has the firewalled bit set
     */
    public boolean isFirewalledSource() {

        // Look for 0x40, the bit at 0100 0000, indicating the searching computer isn't externally contactable
        if (!isMulticast()) { // If we haven't marked this Message object as one that travels on multicast UDP on the LAN
            if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) { // Make sure the byte is holding flags, not a speed
                if ((MIN_SPEED & SPECIAL_FIREWALL_MASK) > 0) return true;
            }
        }
        return false;
    }

    /**
     * Determine if the searching computer wants LimeWire XML metadata in returned in band hits.
     * Looks for 0x20, the bit at 0010 0000, in the speed flags bytes.
     * 
     * @return True if the speed flags byte has the XML bit set
     */
    public boolean desiresXMLResponses() {

        // Look for 0x20, the bit at 0010 0000, the searching computer wants hits with XML metadata
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) { // Make sure the byte is holding flags, not a speed
            if ((MIN_SPEED & SPECIAL_XML_MASK) > 0) return true;
        }
        return false;
    }

    /**
     * Determine if the searching computer can do firewall-to-firewall transfers.
     * Looks for 0x02, the bit at 0000 0010, in the speed flags bytes.
     * 
     * @return True if the speed flags byte has the firewall-to-firewall transfer bit set
     */
    public boolean canDoFirewalledTransfer() {

        // Look for 0x02, the bit at 0000 0010, the searching computer can do UDP firewall-to-firewall file transfers
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) { // Make sure the byte is holding flags, not a speed
            if ((MIN_SPEED & SPECIAL_FWTRANS_MASK) > 0) return true;
        }
        return false;
    }

    /**
     * Determine if the searching computer is externally contactable for UDP packets, and wants query hits sent that way.
     * Looks for 0x04, the bit at 0000 0100, in the speed flags bytes.
     * 
     * If true, use getReplyAddress() and getReplyPort() to get the computer's address. (do)
     * In out of band hits, always send XML.
     * 
     * @return True if the speed flags byte has the out of band bit set
     */
    public boolean desiresOutOfBandReplies() {

        // Look for 0x04, the bit at 0000 0100, the searching computer can get UDP packets
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) { // Make sure the byte is holding flags, not a speed
            if ((MIN_SPEED & SPECIAL_OUTOFBAND_MASK) > 0) return true;
        }
        return false;
    }

    /**
     * Returns true if the searching computer doesn't want a computer to proxy this search for it.
     * Indicates the presence of the GGEP "NP" No Proxy extension.
     * 
     * @return True if this query shouldn't be proxied
     */
    public boolean doNotProxy() {

        // Return true if this query's GGEP block has the "NP" No Proxy extension
        return _doNotProxy;
    }

    /**
     * Determine if this is a What's New search.
     * If so, it wants the 3 newest files in a computer's library.
     * 
     * @return True if this query's GGEP block has "WH" with a byte value of 1
     */
    public boolean isWhatIsNewRequest() {

        // Return true if this query's GGEP block has "WH" with a byte value of 1
        return _featureSelector == FeatureSearchData.WHAT_IS_NEW;
    }

    /**
     * Determine if this query packet is a What's New search.
     * Looks for the GGEP extension "WH", with a byte value of 1 or more.
     * In a query packet, "WH" means this is a What's New search.
     * Other places, "WH" means the sending computer supports What's New search.
     * 
     * @return True if this query's GGEP block has "WH" with a byte value of 1 or more
     */
    public boolean isFeatureQuery() {

        // Return true if this query's GGEP block has "WH" with a byte value of 1 or more
        return _featureSelector > 0;
    }

    /**
     * Returns 0 if this is a normal query, or 1 if it's a What's New search.
     * 
     * @return The byte value of this query's GGEP "WH" extension, or 0 if it doesn't have "WH"
     */
    public int getFeatureSelector() {

        // Return the byte value of the GGEP "WH" extension
        return _featureSelector;
    }

    /**
     * Get the IP address this query wants out of band UDP replies sent to.
     * The IP address and port number are hidden in the message GUID.
     * This is only useful when desiresOutOfBandReplies() is true.
     * 
     * @return The IP address from inside this query packet's GUID as a String
     */
    public String getReplyAddress() {

        // Get the IP address hidden in the GUID
        return (new GUID(getGUID())).getIP();
    }

    /**
     * Determine if this query wants out of band UDP replies sent to the given IP address.
     * The IP address and port number are hidden in the message GUID.
     * This is only useful when desiresOutOfBandReplies() is true.
     * 
     * @param ip An IP address
     * @return   True if the IP address inside this query packet's GUID matches the given one
     */
    public boolean matchesReplyAddress(byte[] ip) {

        // See if the IP address hidden in the GUID matches the given one
        return (new GUID(getGUID())).matchesIP(ip);
    }

    /**
     * Get the port number this query wants out of band UDP replies sent to.
     * The IP address and port number are hidden in the message GUID.
     * This is only useful when desiresOutOfBandReplies() is true.
     * 
     * @return The port number inside this query packet's GUID
     */
    public int getReplyPort() {

        // Get the port number hidden in the GUID
        return (new GUID(getGUID())).getPort();
    }

    /**
     * Determine if this is a requery from LimeWire.
     * 
     * @return True if the message GUID has LimeWire's markings and is marked a requery
	 */
	public boolean isLimeRequery() {

        // Look for the LimeWire identity and requery markings
		return GUID.isLimeRequeryGUID(getGUID());
	}

    /**
     * QueryKey is a part of GUESS, which LimeWire doesn't use anymore.
     * 
     * Returns the QueryKey associated with this Request. May very well be
     * null.  Usually only UDP QueryRequests will have non-null QueryKeys.
     */
    public QueryKey getQueryKey() {
        return QUERY_KEY;
    }

    /**
     * Determine if this query isn't filtering on media type.
     * 
     * @return True if there is no GGEP "M" Meta extension
     */
    public boolean desiresAll() {

        // If _metaMask is null, there's no "M" extension that would filter on media type
        return (_metaMask == null);
    }

    /**
     * Determine if this query only wants audio results.
     * 
     * @return True if the GGEP "M" Meta extension's byte value has the bit for audio set
     */
    public boolean desiresAudio() {

        // Look for the bit in _metaMask, the value of the GGEP "M" extension
        if (_metaMask != null) return ((_metaMask.intValue() & AUDIO_MASK) > 0);
        return true;
    }

    /**
     * Determine if this query only wants video results.
     * 
     * @return True if the GGEP "M" Meta extension's byte value has the bit for video set
     */
    public boolean desiresVideo() {

        // Look for the bit in _metaMask, the value of the GGEP "M" extension
        if (_metaMask != null) return ((_metaMask.intValue() & VIDEO_MASK) > 0);
        return true;
    }

    /**
     * Determine if this query only wants document results.
     * 
     * @return True if the GGEP "M" Meta extension's byte value has the bit for documents set
     */
    public boolean desiresDocuments() {

        // Look for the bit in _metaMask, the value of the GGEP "M" extension
        if (_metaMask != null) return ((_metaMask.intValue() & DOC_MASK) > 0);
        return true;
    }

    /**
     * Determine if this query only wants image results.
     * 
     * @return True if the GGEP "M" Meta extension's byte value has the bit for images set
     */
    public boolean desiresImages() {

        // Look for the bit in _metaMask, the value of the GGEP "M" extension
        if (_metaMask != null) return ((_metaMask.intValue() & IMAGE_MASK) > 0);
        return true;
    }

    /**
     * Determine if this query only wants Windows program results.
     * 
     * @return True if the GGEP "M" Meta extension's byte value has the bit for Windows programs set
     */
    public boolean desiresWindowsPrograms() {

        // Look for the bit in _metaMask, the value of the GGEP "M" extension
        if (_metaMask != null) return ((_metaMask.intValue() & WIN_PROG_MASK) > 0);
        return true;
    }

    /**
     * Determine if this query only wants Linux and Mac OS X program results.
     * 
     * @return True if the GGEP "M" Meta extension's byte value has the bit for Linux and Mac OS X programs set
     */
    public boolean desiresLinuxOSXPrograms() {

        // Look for the bit in _metaMask, the value of the GGEP "M" extension
        if (_metaMask != null) return ((_metaMask.intValue() & LIN_PROG_MASK) > 0);
        return true;
    }

    /**
     * Get the media type bit mask, the value of the GGEP "M" Meta extension that filters on media types like audio and video.
     * 
     * @return An int with the bits set in the lowest byte.
     *         If this query doesn't have "M", returns 0.
     */
    public int getMetaMask() {

        // Return _metaMask, or 0 if this packet's GGEP block doesn't have the "M" extension
        if (_metaMask != null) return _metaMask.intValue();
        return 0;
    }

    /**
     * Count that we've dropped this query packet in the program's statistics.
     */
	public void recordDrop() {

        // Give this QueryRequest object to the DroppedSentMessageHandler for TCP queries
		DroppedSentMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(this);
	}

    /**
     * Doesn't strip the payload from this query.
     * 
     * @return A reference to this same QueryRequest object
     */
    public Message stripExtendedPayload() {

        // Return a reference to this same object
        return this;
    }

    /**
     * Call originate() to mark this QueryRequest as one we've re-originated on behalf of one of our leaves.
     * Sets originated to true, so isOriginated() will start returning true.
     */
    public void originate() {

        // Set originated to true so isOriginated() will start returning true
        originated = true;
    }

    /**
     * Determine if we re-originated this query for a leaf.
     * 
     * @return True if code called originate() on this object
     */
    public boolean isOriginated() {

        // Return the originated flag
        return originated;
    }

    /**
     * Have this QueryRequest object describe the information its holding as a number.
     * 
     * @return The int hash code
     */
	public int hashCode() {

        // If we haven't computed the hash code yet
		if (_hashCode == 0) {

            // Compute it
			int result = 17;
			result = (37 * result) + QUERY.hashCode();
			if (XML_DOC != null) result = (37 * result) + XML_DOC.hashCode();
			result = (37 * result) + REQUESTED_URN_TYPES.hashCode();
			result = (37 * result) + QUERY_URNS.hashCode();
			if (QUERY_KEY != null) result = (37*result) + QUERY_KEY.hashCode();

            /*
             * TODO:: ADD GUID!!
             */

            // Save it
			_hashCode = result;
		}

        // Return the hash code number we computed now or earlier
		return _hashCode;
	}

    /**
     * Determine if a given QueryRequest object has the same data as this one.
     * Overrides Object.equals()
     * 
     * @param o The QueryRequest object to compare this one to
     * @return  True if they are the same, false if they are different
     */
	public boolean equals(Object o) {

        // If o is this one, same, if it's not even a QueryRequest object, different
		if (o == this) return true;
		if (!(o instanceof QueryRequest)) return false;

        // Cast o to a QueryRequest, and compare the contents
		QueryRequest qr = (QueryRequest)o;
		return
            (MIN_SPEED == qr.MIN_SPEED &&
			QUERY.equals(qr.QUERY) &&
			(XML_DOC == null ? qr.XML_DOC == null : XML_DOC.equals(qr.XML_DOC)) &&
			REQUESTED_URN_TYPES.equals(qr.REQUESTED_URN_TYPES) &&
			QUERY_URNS.equals(qr.QUERY_URNS) &&
			Arrays.equals(getGUID(), qr.getGUID()) &&
			Arrays.equals(PAYLOAD, qr.PAYLOAD));
	}

    /**
     * Express this QueryRequest object as text.
     * Composes a tag like:
     * 
     * <query: "search text", ttl: 6 ... >
     * 
     * Overrides Object.toString()
     * 
     * @return A String
     */
    public String toString() {

        // Compose a String with information from this object
 		return
            "<query: \"" + getQuery()                    + "\", " +
            "ttl: "      + getTTL()                      + ", "   +
            "hops: "     + getHops()                     + ", "   +
            "meta: \""   + getRichQueryString()          + "\", " +
            "types: "    + getRequestedUrnTypes().size() + ","    +
            "urns: "     + getQueryUrns().size()         + ">";
    }
}
