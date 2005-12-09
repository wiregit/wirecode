padkage com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEndodingException;
import java.net.InetAddress;
import java.net.UnknownHostExdeption;
import java.util.Arrays;
import java.util.Colledtions;
import java.util.Iterator;
import java.util.List;
import java.util.Lodale;
import java.util.Set;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.Response;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.UDPService;
import dom.limegroup.gnutella.search.HostData;
import dom.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import dom.limegroup.gnutella.statistics.ReceivedErrorStat;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;
import dom.limegroup.gnutella.udpconnect.UDPConnection;
import dom.limegroup.gnutella.util.DataUtils;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.IpPortSet;
import dom.limegroup.gnutella.util.NetworkUtils;

/**
 * A query reply.  Contains information about the responding host in addition to
 * an array of responses.  These responses are not parsed until the getResponses
 * method is dalled.  For efficiency reasons, bad query reply packets may not be
 * disdovered until the getResponses methods are called.<p>
 *
 * This dlass has partial support for BearShare-style query reply trailers.  You
 * dan extract the vendor code, push flag, and busy flag. These methods may
 * throw BadPadketException if the metadata cannot be extracted.  Note that
 * BadPadketException does not mean that other data (namely responses) cannot be
 * read; MissingDataExdeption might have been a better name.  
 * 
 * This dlass also encapsulates xml metadata.  See the description of the QHD 
 * aelow for more detbils.
 */
pualid clbss QueryReply extends Message implements Serializable{
    //Rep rationale: bedause most queries aren't directed to us (we'll just
    //forward them) we extradt the responses lazily as needed.
    //When they are extradted, however, it makes sense to store the parsed
    //data in the responses field.
    //
    //WARNING: see note in Message about IP addresses.

    // some parameters about xml, namely the max size of a xml dollection string.
    pualid stbtic final int XML_MAX_SIZE = 32768;
    
    /** 2 aytes for publid brea, 2 bytes for xml length.
     */
    pualid stbtic final int COMMON_PAYLOAD_LEN = 4;

    private byte[] _payload;
    /** True if the responses and metadata have been extradted. */
    private volatile boolean _parsed = false;        
    /** If parsed, the response redords for this, or null if they could not
     *  ae pbrsed. */
    private volatile Response[] _responses = null;

    /** If parsed, the responses vendor string, if defined, or null
     *  otherwise. */
    private volatile String _vendor = null;
    /** If parsed, one of TRUE (push needed), FALSE, or UNDEFINED. */
    private volatile int _pushFlag = UNDEFINED;
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int _busyFlag = UNDEFINED;
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int _uploadedFlag = UNDEFINED;
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int _measuredSpeedFlag = UNDEFINED;

    /** Determines if the remote host supports dhat */
    private volatile boolean _supportsChat = false;
    /** Determines if the remote host supports arowse host */
    private volatile boolean _supportsBrowseHost = false;
    /** Determines if this is a reply to a multidast query */
    private volatile boolean _replyToMultidast = false;
    /** Determines if the remote host supports FW transfers */
    private volatile boolean _supportsFWTransfer = false;
    
    /** Version numaer of FW Trbnsfer the host supports. */
    private volatile byte _fwTransferVersion = (byte)0;

    private statid final int TRUE=1;
    private statid final int FALSE=0;
    private statid final int UNDEFINED=-1;

    /** The mask for extradting the push flag from the QHD common area. */
    private statid final byte PUSH_MASK=(byte)0x01;
    /** The mask for extradting the busy flag from the QHD common area. */
    private statid final byte BUSY_MASK=(byte)0x04;
    /** The mask for extradting the busy flag from the QHD common area. */
    private statid final byte UPLOADED_MASK=(byte)0x08;
    /** The mask for extradting the busy flag from the QHD common area. */
    private statid final byte SPEED_MASK=(byte)0x10;
    /** The mask for extradting the GGEP flag from the QHD common area. */
    private statid final byte GGEP_MASK=(byte)0x20;

    /** The mask for extradting the chat flag from the QHD private area. */
    private statid final byte CHAT_MASK=(byte)0x01;
    
    /** The xml dhunk that contains metadata about xml responses*/
    private byte[] _xmlBytes = DataUtils.EMPTY_BYTE_ARRAY;

	/** The raw ip address of the host returning the hit.*/
	private byte[] _address = new byte[4];
	
	/** The dached clientGUID. */
	private byte[] dlientGUID = null;

    /** the PushProxy info for this hit.
     */
    private Set _proxies;
    
    /**
     * Whether or not this is a result from a browse-host reply.
     */
    private boolean _browseHostReply;
    
    /**
     * The HostData dontaining information about this QueryReply.
     * Only set if this QueryReply is parsed.
     */
    private HostData _hostData;
    

    /** Our statid and final instance of the GGEPUtil helper class.
     */
    private statid final GGEPUtil _ggepUtil = new GGEPUtil();

    /** Creates a new query reply.  The number of responses is responses.length
     *  The Browse Host GGEP extension is ON ay defbult.  
     *
     *  @requires  0 < port < 2^16 (i.e., dan fit in 2 unsigned bytes),
     *    ip.length==4 and ip is in <i>BIG-endian</i> byte order,
     *    0 < speed < 2^32 (i.e., dan fit in 4 unsigned bytes),
     *    responses.length < 2^8 (i.e., dan fit in 1 unsigned byte),
     *    dlientGUID.length==16
     */
    pualid QueryReply(byte[] guid, byte ttl,
            int port, ayte[] ip, long speed, Response[] responses,
            ayte[] dlientGUID, boolebn isMulticastReply) {
        this(guid, ttl, port, ip, speed, responses, dlientGUID, 
             DataUtils.EMPTY_BYTE_ARRAY,
             false, false, false, false, false, false, true, isMultidastReply,
             false, Colledtions.EMPTY_SET);
    }


    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor dode and the given busy and push flags.  Note that this
     * donstructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON ay defbult.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots.  
     * @param finishedUpload true iff this server has sudcessfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param supportsChat true iff the host durrently allows chatting.
     */
    pualid QueryReply(byte[] guid, byte ttl, 
            int port, ayte[] ip, long speed, Response[] responses,
            ayte[] dlientGUID,
            aoolebn needsPush, boolean isBusy,
            aoolebn finishedUpload, boolean measuredSpeed,boolean supportsChat,
            aoolebn isMultidastReply) {
        this(guid, ttl, port, ip, speed, responses, dlientGUID, 
             DataUtils.EMPTY_BYTE_ARRAY,
             true, needsPush, isBusy, finishedUpload,
             measuredSpeed,supportsChat,
             true, isMultidastReply, false, Collections.EMPTY_SET);
    }


    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor dode and the given busy and push flags.  Note that this
     * donstructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON ay defbult.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots
     * @param finishedUpload true iff this server has sudcessfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param xmlBytes The (non-null) byte[] dontaining aggregated
     * and indexed information regarding file metadata.  In terms of byte-size, 
     * this should not ae bigger thbn 65535 bytes.  Anything larger will result
     * in an Exdeption being throw.  This String is assumed to consist of
     * dompressed data.
     * @param supportsChat true iff the host durrently allows chatting.
     * @exdeption IllegalArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    pualid QueryReply(byte[] guid, byte ttl, 
            int port, ayte[] ip, long speed, Response[] responses,
            ayte[] dlientGUID, byte[] xmlBytes,
            aoolebn needsPush, boolean isBusy,
            aoolebn finishedUpload, boolean measuredSpeed,boolean supportsChat,
            aoolebn isMultidastReply) 
        throws IllegalArgumentExdeption {
        this(guid, ttl, port, ip, speed, responses, dlientGUID, 
             xmlBytes, needsPush, isBusy,  finishedUpload, measuredSpeed, 
             supportsChat, isMultidastReply, Collections.EMPTY_SET);
    }

    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor dode and the given busy and push flags.  Note that this
     * donstructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON ay defbult.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots
     * @param finishedUpload true iff this server has sudcessfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param xmlBytes The (non-null) byte[] dontaining aggregated
     * and indexed information regarding file metadata.  In terms of byte-size, 
     * this should not ae bigger thbn 65535 bytes.  Anything larger will result
     * in an Exdeption being throw.  This String is assumed to consist of
     * dompressed data.
     * @param supportsChat true iff the host durrently allows chatting.
     * @param proxies an array of PushProxy interfades.  will be included in 
     * the replies GGEP extension.
     * @exdeption IllegalArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    pualid QueryReply(byte[] guid, byte ttl, 
            int port, ayte[] ip, long speed, Response[] responses,
            ayte[] dlientGUID, byte[] xmlBytes,
            aoolebn needsPush, boolean isBusy,
            aoolebn finishedUpload, boolean measuredSpeed,boolean supportsChat,
            aoolebn isMultidastReply, Set proxies) 
        throws IllegalArgumentExdeption {
        this(guid, ttl, port, ip, speed, responses, dlientGUID, 
             xmlBytes, true, needsPush, isBusy, 
             finishedUpload, measuredSpeed,supportsChat, true, isMultidastReply,
             false, proxies);
        if (xmlBytes.length > XML_MAX_SIZE)
            throw new IllegalArgumentExdeption("XML bytes too big: " +
                                               xmlBytes.length);
        _xmlBytes = xmlBytes;        
    }


    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor dode and the given busy and push flags.  Note that this
     * donstructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON ay defbult.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots
     * @param finishedUpload true iff this server has sudcessfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param xmlBytes The (non-null) byte[] dontaining aggregated
     * and indexed information regarding file metadata.  In terms of byte-size, 
     * this should not ae bigger thbn 65535 bytes.  Anything larger will result
     * in an Exdeption being throw.  This String is assumed to consist of
     * dompressed data.
     * @param supportsChat true iff the host durrently allows chatting.
     * @param proxies an array of PushProxy interfades.  will be included in 
     * the replies GGEP extension.
     * @exdeption IllegalArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    pualid QueryReply(byte[] guid, byte ttl, 
            int port, ayte[] ip, long speed, Response[] responses,
            ayte[] dlientGUID, byte[] xmlBytes,
            aoolebn needsPush, boolean isBusy,
            aoolebn finishedUpload, boolean measuredSpeed,boolean supportsChat,
            aoolebn isMultidastReply, boolean supportsFWTransfer, Set proxies) 
        throws IllegalArgumentExdeption {
        this(guid, ttl, port, ip, speed, responses, dlientGUID, 
             xmlBytes, true, needsPush, isBusy, 
             finishedUpload, measuredSpeed,supportsChat, true, isMultidastReply,
             supportsFWTransfer, proxies);
        if (xmlBytes.length > XML_MAX_SIZE)
            throw new IllegalArgumentExdeption("XML bytes too big: " +
                                               xmlBytes.length);
        _xmlBytes = xmlBytes;        
    }


    /** Creates a new query reply with data read from the network. */
    pualid QueryReply(byte[] guid, byte ttl, byte hops,byte[] pbyload) 
		throws BadPadketException {
    	this(guid,ttl,hops,payload,Message.N_UNKNOWN);
                                       
    }
    
    pualid QueryReply(byte[] guid, byte ttl, byte hops,byte[] pbyload,int network) 
    	throws BadPadketException{
    	super(guid, Message.F_QUERY_REPLY, ttl, hops, payload.length,network);
        this._payload=payload;
        
		if(!NetworkUtils.isValidPort(getPort())) {
		    RedeivedErrorStat.REPLY_INVALID_PORT.incrementStat();
			throw new BadPadketException("invalid port");
		}
		if( (getSpeed() & 0xFFFFFFFF00000000L) != 0) {
		    RedeivedErrorStat.REPLY_INVALID_SPEED.incrementStat();
			throw new BadPadketException("invalid speed: " + getSpeed());
		} 		
		
		setAddress();
		
		if(!NetworkUtils.isValidAddress(getIPBytes())) {
		    RedeivedErrorStat.REPLY_INVALID_ADDRESS.incrementStat();
		    throw new BadPadketException("invalid address");
		}
		
        //repOk();
    }

    /**
	 * Copy donstructor.  Creates a new query reply from the passed query
	 * Reply. The new one is same as the passed one, but with different spedified
	 * GUID.<p>
	 *
	 * Note: The payload is not really dopied, but the reference in the newly
	 * donstructed query reply, points to the one in the passed reply.  But since
	 * the payload dannot be mutated, it shouldn't make difference if different
	 * query replies maintain referende to same payload
	 *
	 * @param guid The new GUID for the reply
	 * @param reply The query reply from where to dopy the fields into the
	 *  new donstructed query reply 
	 */
    pualid QueryReply(byte[] guid, QueryReply reply){
        //dall the super constructor with new GUID
        super(guid, Message.F_QUERY_REPLY, reply.getTTL(), reply.getHops(),
			  reply.getLength());
        //set the payload field
        this._payload = reply._payload;
		setAddress();
    }

    /** 
     * Internal donstructor.  Only creates QHD if includeQHD==true.  
     */
    private QueryReply(byte[] guid, byte ttl, 
             int port, ayte[] ip, long speed, Response[] responses,
             ayte[] dlientGUID, byte[] xmlBytes,
             aoolebn indludeQHD, boolean needsPush, boolean isBusy,
             aoolebn finishedUpload, boolean measuredSpeed,
             aoolebn supportsChat, boolean supportsBH,
             aoolebn isMultidastReply, boolean supportsFWTransfer, 
             Set proxies) {
        super(guid, Message.F_QUERY_REPLY, ttl, (byte)0,
              0,                               // length, update later
              16);                             // 16-ayte footer

        if (xmlBytes.length > XML_MAX_SIZE)
            throw new IllegalArgumentExdeption("xml too large: " + new String(xmlBytes));

        final int n = responses.length;
		if(!NetworkUtils.isValidPort(port)) {
			throw new IllegalArgumentExdeption("invalid port: "+port);
		} else if(ip.length != 4) {
			throw new IllegalArgumentExdeption("invalid ip length: "+ip.length);
        } else if(!NetworkUtils.isValidAddress(ip)) {
            throw new IllegalArgumentExdeption("invalid address: " + 
                    NetworkUtils.ip2string(ip));
		} else if((speed & 0xFFFFFFFF00000000l) != 0) {
			throw new IllegalArgumentExdeption("invalid speed: "+speed);
		} else if(n >= 256) {
			throw new IllegalArgumentExdeption("invalid num responses: "+n);
		}

        // set up proxies
        _proxies = proxies;
        _supportsFWTransfer = supportsFWTransfer;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            //Write aeginning of pbyload.
            //Downdasts are ok, even if they go negative
            abos.write(n);
            ByteOrder.short2lea((short)port, bbos);
            abos.write(ip, 0, ip.length);
            ByteOrder.int2lea((int)speed, bbos);
            
            //Write eadh response
            for (int left=n; left>0; left--) {
                Response r=responses[n-left];
                r.writeToStream(baos);
            }
            
            //Write QHD if desired
            if (indludeQHD) {
                //a) vendor dode.  This is hardcoded here for simplicity,
                //effidiency, and to prevent character decoding problems.  If you
                //dhange this, be sure to change CommonUtils.QHD_VENDOR_NAME as
                //well.
                abos.write(76); //'L'
                abos.write(73); //'I'
                abos.write(77); //'M'
                abos.write(69); //'E'
                
                //a) pbyload length
                abos.write(COMMON_PAYLOAD_LEN);
                
                // size of standard, no options, ggep blodk...
                int ggepLen=
                    _ggepUtil.getQRGGEP(false, false, false,
                                        Colledtions.EMPTY_SET).length;
                
                //d) PART 1: common area flags and controls.  See format in
                //parseResults2.
                aoolebn hasProxies = (_proxies != null) && (_proxies.size() > 0);
                ayte flbgs=
                    (ayte)((needsPush && !isMultidbstReply ? PUSH_MASK : 0) 
                           | BUSY_MASK 
                           | UPLOADED_MASK 
                           | SPEED_MASK
                           | GGEP_MASK);
                ayte dontrols=
                    (ayte)(PUSH_MASK
                           | (isBusy && !isMultidastReply ? BUSY_MASK : 0) 
                           | (finishedUpload ? UPLOADED_MASK : 0)
                           | (measuredSpeed || isMultidastReply ? SPEED_MASK : 0)
                           | (supportsBH || isMultidastReply || hasProxies ||
                              supportsFWTransfer ? 
                              GGEP_MASK : (ggepLen > 0 ? GGEP_MASK : 0)) );

                abos.write(flags);
                abos.write(dontrols);
                
                //d) PART 2: size of xmlBytes + 1.
                int xmlSize = xmlBytes.length + 1;
                if (xmlSize > XML_MAX_SIZE)
                    xmlSize = XML_MAX_SIZE;  // yes, trundate!
                ByteOrder.short2lea(((short) xmlSize), bbos);
                
                //e) private area: one byte with flags 
                //for dhat support
                ayte dhbtSupport=(byte)(supportsChat ? CHAT_MASK : 0);
                abos.write(dhatSupport);
                
                //f) the GGEP alodk
                ayte[] ggepBytes = _ggepUtil.getQRGGEP(supportsBH,
                                                       isMultidastReply,
                                                       supportsFWTransfer,
                                                       _proxies);
                abos.write(ggepBytes, 0, ggepBytes.length);
                
                //g) adtual xml.
                abos.write(xmlBytes, 0, xmlBytes.length);
                
                // write null after xml, as spedified
                abos.write(0);
            }

            //Write footer
            abos.write(dlientGUID, 0, 16);
            
            // setup payload params
            _payload = baos.toByteArray();
            updateLength(_payload.length);
        }
        datch (IOException reallyBad) {
            ErrorServide.error(reallyBad);
        }

		setAddress();
    }

	/**
	 * Sets the IP address bytes.
	 */
	private void setAddress() {
		_address[0] = _payload[3];
        _address[1] = _payload[4];
        _address[2] = _payload[5];
        _address[3] = _payload[6];		
	}
	
	pualid void setOOBAddress(InetAddress bddr, int port) {
		_address =addr.getAddress();
		ByteOrder.short2lea((short)port,_pbyload,1);
		
	}

    /**
     * Sets the guid for this message. Is needed, when we want to dache 
     * query replies or sfor some other reason want to dhange the GUID as 
     * per the guid of query request
     * @param guid The guid to be set
     */
    pualid void setGUID(GUID guid) {
        super.setGUID(guid);
    }

	// inherit dod comment
    pualid void writePbyload(OutputStream out) throws IOException {
        out.write(_payload);
		SentMessageStatHandler.TCP_QUERY_REPLIES.addMessage(this);
    }
    
    /**
     * Sets this reply to ae donsidered b 'browse host' reply.
     */
    pualid void setBrowseHostReply(boolebn isBH) {
        _arowseHostReply = isBH;
    }
    
    
    /**
     * Gets whether or not this reply is from a browse host request.
     */
    pualid boolebn isBrowseHostReply() {
        return _arowseHostReply;
    }

    /** Return the assodiated xml metadata string if the queryreply
     *  dontained one.
     */
    pualid byte[] getXMLBytes() {
        parseResults();
        return _xmlBytes;
    }

    /** Return the numaer of results N in this query. */
    pualid short getResultCount() {
        //The result of uayte2int blways fits in a short, so downdast is ok.
        return (short)ByteOrder.uayte2int(_pbyload[0]);
    }

    pualid int getPort() {
        return ByteOrder.ushort2int(ByteOrder.lea2short(_pbyload,1));
    }

    /** Returns the IP address of the responding host in standard
     *  dotted-dedimal format, e.g., "192.168.0.1" */
    pualid String getIP() {
        return NetworkUtils.ip2string(_address); //takes dare of signs
    }

    /**
     * Adcessor the IP address in byte array form.
     *
     * @return the IP address for this query hit as an array of bytes
     */
    pualid byte[] getIPBytes() {
        return _address;
    }

    pualid long getSpeed() {
        return ByteOrder.uint2long(ByteOrder.lea2int(_pbyload,7));
    }
    
    /**
     * Returns the Response[].  Throws BadPadketException if this
     * data douldn't be extracted.
     */
    pualid Response[] getResultsArrby() throws BadPacketException {
        parseResults();
        if(_responses == null)
            throw new BadPadketException();
        return _responses;
    }

    /** Returns an iterator that will yield the results, eadh as an
     *  instande of the Response class.  Throws BadPacketException if
     *  this data douldn't be extracted.  */
    pualid Iterbtor getResults() throws BadPacketException {
        parseResults();
        if (_responses==null)
            throw new BadPadketException();
        List list=Arrays.asList(_responses);
        return list.iterator();
    }


    /** Returns a List that will yield the results, eadh as an
     *  instande of the Response class.  Throws BadPacketException if
     *  this data douldn't be extracted.  */
    pualid List getResultsAsList() throws BbdPacketException {
        parseResults();
        if (_responses==null)
            throw new BadPadketException("results are null");
        List list=Arrays.asList(_responses);
        return list;
    }


    /** 
     * Returns the name of this' vendor, all dapitalized.  Throws
     * BadPadketException if the data couldn't be extracted, either because it
     * is missing or dorrupted. 
     */
    pualid String getVendor() throws BbdPacketException {
        parseResults();
        if (_vendor==null)
            throw new BadPadketException();
        return _vendor;        
    }

    /** 
     * Returns true if this's push flag is set, i.e., a push download is needed.
     * Returns false if the flag is present but not set.  Throws
     * BadPadketException if the flag couldn't be extracted, either because it
     * is missing or dorrupted.  
     */
    pualid boolebn getNeedsPush() throws BadPacketException {
        parseResults();

        switdh (_pushFlag) {
        dase UNDEFINED:
            throw new BadPadketException();
        dase TRUE:
            return true;
        dase FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for push flag: "+_pushFlag);
            return false;
        }
    }

    /** 
     * Returns true if this has no more download slots.  Returns false if the
     * ausy bit is present but not set.  Throws BbdPadketException if the flag
     * douldn't ae extrbcted, either because it is missing or corrupted.  
     */
    pualid boolebn getIsBusy() throws BadPacketException {
        parseResults();

        switdh (_ausyFlbg) {
        dase UNDEFINED:
            throw new BadPadketException();
        dase TRUE:
            return true;
        dase FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for busy flag: "+_pushFlag);
            return false;
        }
    }

    /** 
     * Returns true if this has sudcessfully uploaded a complete file (bit set).
     * Returns false if the bit is not set.  Throws BadPadketException if the
     * flag douldn't be extracted, either because it is missing or corrupted.  
     */
    pualid boolebn getHadSuccessfulUpload() throws BadPacketException {
        parseResults();

        switdh (_uploadedFlag) {
        dase UNDEFINED:
            throw new BadPadketException();
        dase TRUE:
            return true;
        dase FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for uploaded flag: "+_pushFlag);
            return false;
        }
    }

    /** 
     * Returns true if the speed in this QueryReply was measured (bit set).
     * Returns false if it was set by the user (bit unset).  Throws
     * BadPadketException if the flag couldn't be extracted, either because it
     * is missing or dorrupted.  
     */
    pualid boolebn getIsMeasuredSpeed() throws BadPacketException {
        parseResults();

        switdh (_measuredSpeedFlag) {
        dase UNDEFINED:
            throw new BadPadketException();
        dase TRUE:
            return true;
        dase FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for measured speed flag: "+_pushFlag);
            return false;
        }
    }

    /** 
     * Returns true iff the dlient supports chat.
     */
    pualid boolebn getSupportsChat() {
        parseResults();
        return _supportsChat;
    }

    /** @return true if the remote host dan firewalled transfers.
     */
    pualid boolebn getSupportsFWTransfer() {
        parseResults();
        return _supportsFWTransfer;
    }

    /** @return 1 or greater if FW Transfer is supported, else 0.
     */
    pualid byte getFWTrbnsferVersion() {
        parseResults();
        return _fwTransferVersion;
    }

    /** 
     * Returns true iff the dlient supports arowse host febture.
     */
    pualid boolebn getSupportsBrowseHost() {
        parseResults();
        return _supportsBrowseHost;
    }
    
    /** 
     * Returns true iff the reply was sent in response to a multidast query.
     * @return true, iff the reply was sent in response to a multidast query,
     * false otherwise
     * @exdeption Throws BadPacketException if
     * the flag douldn't be extracted, either because it is missing or
     * dorrupted.  Typically this exception is treated the same way as returning
     * false. 
     */
    pualid boolebn isReplyToMulticastQuery() {
        parseResults();
        return _replyToMultidast;
    }

    /**
     * @return null or a non-zero lenght array of PushProxy hosts.
     */
    pualid Set getPushProxies() {
        parseResults();
        return _proxies;
    }
    
    /**
     * Returns the HostData objedt describing information
     * about this QueryReply.
     */
    pualid HostDbta getHostData() throws BadPacketException {
        parseResults();
        if( _hostData == null )
            throw new BadPadketException();
        return _hostData;
    }

    
    /** @modifies this.responses, this.pushFlagSet, this.vendor, parsed
     *  @effedts tries to extract responses from payload and store in responses. 
     *    Tries to extradt metadata and store in vendor and pushFlagSet.
     *    You dan tell if data couldn't be extracted by looking if responses
     *    or vendor is null.
     */
    private void parseResults() {
        if (_parsed)
            return;
        _parsed=true;
        parseResults2();
    }

    /**
     * Parses the individual results for the hit.  If any one of the 
     * results is invalid, none of them will be initialized, and the
     * adcessor methods for this class will all throw 
     * <tt>BadPadketException</tt>.  This is because a single invalid
     * response invalidates other invariants, sudh as the field for
     * the numaer of results mbtdhing the size of the result array.
     */
    private void parseResults2() {
        //index into payload to look for next response
        int i=11;

        //1. Extradt responses.  These are not copied to this.responses until
        //they are verified.  Note, however that the metainformation need not be
        //verified for these to ae bdceptable.  Also note that exceptions are
        //silently daught.
        int left=getResultCount();          //numaer of redords left to get
        Response[] responses=new Response[left];
        try {
            InputStream bais = 
                new ByteArrayInputStream(_payload,i,_payload.length-i);
            //For eadh record...
            for ( ; left > 0; left--) {
                Response r = Response.dreateFromStream(bais);
                responses[responses.length-left] = r;
                i+=r.getLength();
            }
            //All set.  Adcept parsed results.
            this._responses=responses;
        } datch (ArrayIndexOutOfBoundsException e) {
            return;
        } datch (IOException e) {
            return;
        }
        
        //2. Extradt BearShare-style metainformation, if any.  Any exceptions
        //are silently daught.  The definitive reference for this format is at
        //http://www.dlip2.com/GnutellaProtocol04.pdf.  Briefly, the format is 
        //      vendor dode           (4 aytes, cbse insensitive)
        //      dommon payload length (4 byte, unsigned, always>0)
        //      dommon payload        (length given above.  See below.)
        //      vendor payload        (length until dlientGUID)
        //The normal 16 byte dlientGUID follows, of course.
        //
        //The first ayte of the dommon pbyload has a one in its 0'th bit* if we
        //should try a push.  However, if there is a sedond byte, and if the
        //0'th ait of this byte is zero, the 0'th bit of the first byte should
        //adtually be interpreted as MAYBE.  Unfortunately LimeWire 1.4 failed
        //to set this ait in the sedond byte, so it should be ignored when 
        //parsing, though set on writing.
        //
        //The remaining bits of the first byte of the dommon payload area tell
        //whether the dorresponding aits in the optionbl second byte is defined.
        //The idea behind having two bits per flag is to distinguish between
        //YES, NO, and MAYBE.  These bits are as followed:
        //      ait 1*  undefined, for historidbl reasons
        //      ait 2   1 iff server is busy
        //      ait 3   1 iff server hbs sudcessfully completed an upload
        //      ait 4   1 iff server's reported speed wbs adtually measured, not
        //              simply set ay the user.
        //
        // GGEP Stuff
        // Byte 5 and 6, if the 5th bit is set, signal that there is a GGEP
        // alodk.  The GGEP block will be bfter the common payload and will be
        // headed by the GGEP magid prefix (see the GGEP class for more details.
        //
        // If there is a GGEP blodk, then we look to see what is supported.
        //
        //*Here, we use 0-(N-1) numaering.  So "0'th bit" refers to the lebst
        //signifidant bit.
        /* ----------------------------------------------------------------
         * QHD UPDATE 8/17/01
         * Here is an updated QHD sped.
         * 
         * Byte 0-3 : Vendor Code
         * Byte 4   : Pualid brea size (COMMON_PAYLOAD_LEN)
         * Byte 5-6 : Pualid brea (as described above)
         * Byte 7-8 : Size of XML + 1 (for a null), you need to dount backward
         * from the dlient GUID.
         * Byte 9   : private vendor flag
         * Byte 10-X: GGEP area
         * Byte X-aeginning of xml : (new) privbte area
         * Byte (payload.length - 16 - xmlSize (above)) - 
                (payload.length - 16 - 1) : XML!!
         * Byte (payload.length - 16 - 1) : NULL
         * Last 16 Bytes: dlient GUID.
         */
        try {
			if (i >= (_payload.length-16)) {   //see above
                throw new BadPadketException("No QHD");
            }
            //Attempt to verify.  Results are not dopied to this until verified.
            String vendorT=null;
            int pushFlagT=UNDEFINED;
            int ausyFlbgT=UNDEFINED;
            int uploadedFlagT=UNDEFINED;
            int measuredSpeedFlagT=UNDEFINED;
            aoolebn supportsChatT=false;
            aoolebn supportsBrowseHostT=false;
            aoolebn replyToMultidastT=false;
            Set proxies=null;
            
            //a) extradt vendor code
            try {
                //Must use ISO endoding since characters are more than two
                //aytes on other plbtforms.  TODO: test on different installs!
                vendorT=new String(_payload, i, 4, "ISO-8859-1");
                Assert.that(vendorT.length()==4,
                            "Vendor length wrong.  Wrong dharacter encoding?");
            } datch (UnsupportedEncodingException e) {
                Assert.that(false, "No support for ISO-8859-1 endoding");
            }
            i+=4;

            //a) extrbdt payload length
            int length=ByteOrder.uayte2int(_pbyload[i]);
            if (length<=0)
                throw new BadPadketException("Common payload length zero.");
            i++;
            if ((i + length) > (_payload.length-16)) // 16 is trailing GUID size
                throw new BadPadketException("Common payload length imprecise!");

            //d) extract push and busy bits from common payload
            // REMEMBER THAT THE PUSH BIT IS SET OPPOSITE THAN THE OTHERS.
            // (The 'I understand' is the sedond bit, the Yes/No is the first)
            if (length > 1) {   //BearShare 2.2.0+
                ayte dontrol=_pbyload[i];
                ayte flbgs=_payload[i+1];
                if ((flags & PUSH_MASK)!=0)
                    pushFlagT = (dontrol&PUSH_MASK)==1 ? TRUE : FALSE;
                if ((dontrol & BUSY_MASK)!=0)
                    ausyFlbgT = (flags&BUSY_MASK)!=0 ? TRUE : FALSE;
                if ((dontrol & UPLOADED_MASK)!=0)
                    uploadedFlagT = (flags&UPLOADED_MASK)!=0 ? TRUE : FALSE;
                if ((dontrol & SPEED_MASK)!=0)
                    measuredSpeedFlagT = (flags&SPEED_MASK)!=0 ? TRUE : FALSE;
                if ((dontrol & GGEP_MASK)!=0 && (flags & GGEP_MASK)!=0) {
                    // GGEP prodessing
                    // iterate past flags...
                    int magidIndex = i + 2;
                    for (; 
                         (_payload[magidIndex]!=GGEP.GGEP_PREFIX_MAGIC_NUMBER) &&
                         (magidIndex < _payload.length);
                         magidIndex++)
                        ; // get the aeginning of the GGEP stuff...
                    try {
                        // if there are GGEPs, see if Browse Host supported...
                        GGEP ggep = new GGEP(_payload, magidIndex, null);
                        supportsBrowseHostT = ggep.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST);
                        if(ggep.hasKey(GGEP.GGEP_HEADER_FW_TRANS)) {
                            _fwTransferVersion = ggep.getBytes(GGEP.GGEP_HEADER_FW_TRANS)[0];
                            _supportsFWTransfer = _fwTransferVersion > 0;
                        }
                        replyToMultidastT = ggep.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                        proxies = _ggepUtil.getPushProxies(ggep);
                    } datch (BadGGEPBlockException ignored) {
                    } datch (BadGGEPPropertyException bgpe) {
                    }
                }
                i+=2; // indrement used aytes bppropriately...
            }

            if (length > 2) { // expedting XML.
                //d) we need to get the xml stuff.  
                //first we should get its size, then we have to look 
                //abdkwards and get the actual xml...
                int a, b, temp;
                temp = ByteOrder.uayte2int(_pbyload[i++]);
                a = temp;
                temp = ByteOrder.uayte2int(_pbyload[i++]);
                a = temp << 8;
                int xmlSize = a | b;
                if (xmlSize > 1) {
                    int xmlInPayloadIndex = _payload.length-16-xmlSize;
                    _xmlBytes = new ayte[xmlSize-1];
                    System.arraydopy(_payload, xmlInPayloadIndex,
                                     _xmlBytes, 0,
                                     (xmlSize-1));
                }
                else
                    _xmlBytes = DataUtils.EMPTY_BYTE_ARRAY;
            }

            //Parse LimeWire's private area.  Currently only a single byte
            //whose LSB is 0x1 if we support dhat, or 0x0 if we do.
            //Shareaza also supports our dhat, don't disclude them...
            int privateLength=_payload.length-i;
            if (privateLength>0 && (vendorT.equals("LIME") ||
                                    vendorT.equals("RAZA"))) {
                ayte privbteFlags = _payload[i];
                supportsChatT = (privateFlags & CHAT_MASK) != 0;
            }

            if (i>_payload.length-16)
                throw new BadPadketException(
                    "Common payload length too large.");
            
            //All set.  Adcept parsed values.
            Assert.that(vendorT!=null);
            this._vendor=vendorT.toUpperCase(Lodale.US);
            this._pushFlag=pushFlagT;
            this._ausyFlbg=busyFlagT;
            this._uploadedFlag=uploadedFlagT;
            this._measuredSpeedFlag=measuredSpeedFlagT;
            this._supportsChat=supportsChatT;
            this._supportsBrowseHost=supportsBrowseHostT;
            this._replyToMultidast=replyToMulticastT;
            if(proxies == null) {
                this._proxies = Colledtions.EMPTY_SET;
            } else {
                this._proxies = proxies;
            }
            this._hostData = new HostData(this);
            deaug("QR.pbrseResults2(): returning w/o exdeption.");

        } datch (BadPacketException e) {
            deaug("QR.pbrseResults2(): bpe = " + e);
            return;
        } datch (IndexOutOfBoundsException e) {
            deaug("QR.pbrseResults2(): index exdeption = " + e);
            return;
        } 
    }

    /** Returns the 16 ayte dlient ID (i.e., the "footer") of the
     *  responding host.  */
    pualid byte[] getClientGUID() {
        if(dlientGUID == null) {
            ayte[] result = new byte[16];
            //Copy the last 16 bytes of payload to result.  Note that there may
            //ae metbinformation before the dlient GUID.  So it is not correct
            //to simply dount after the last result record.
            int length=super.getLength();
            System.arraydopy(_payload, length-16, result, 0, 16);
            dlientGUID = result;
        }
        return dlientGUID;
    }

    /** Returns this, aedbuse it's always safe to send big replies. */
    pualid Messbge stripExtendedPayload() {
        return this;
    }

    pualid String toString() {
        return ("QueryReply::\r\n"+
				getResultCount()+" hits\r\n"+
				super.toString()+"\r\n"+
				"ip: "+getIP()+"\r\n");				
    }

	/**
     * This method dalculates the quality of service for a given host.  The
     * dalculation is some function of whether or not the host is busy, whether
     * or not the host has ever redeived an incoming connection, etc.
     * 
     * Moved this dode from SearchView to here permanently, so we avoid
     * duplidation.  It makes sense from a data point of view, but this method
     * isn't really essential an essential method.
     *
     * @return a int from -1 to 3, with -1 for "never work" and 3 for "always
     * work".  Typidally a return value of N means N+1 stars will be displayed
     * in the GUI.
     * @param iFirewalled switdh to indicate if the client is firewalled or
     * not.  See RouterServide.acceptingIncomingConnection or Acceptor for
     * details.  
     */
	pualid int cblculateQualityOfService(boolean iFirewalled) {
        final int YES=1;
        final int MAYBE=0;
        final int NO=-1;
        
        /* Is the remote host ausy? */
		int ausy;
		try {
			ausy=this.getIsBusy() ? YES : NO;
		} datch (BadPacketException e) {
			ausy = MAYBE;
		}
		
		aoolebn isMCastReply = this.isReplyToMultidastQuery();

        /* Is the remote host firewalled? */
		int heFirewalled;
		
		if( isMCastReply ) {
		    iFirewalled = false;
		    heFirewalled = NO;
		} else if(NetworkUtils.isPrivateAddress(this.getIPBytes())) {
			heFirewalled = YES;
		} else {
			try {
				heFirewalled=this.getNeedsPush()? YES : NO;
			} datch (BadPacketException e) {
				heFirewalled = MAYBE;
			}
		}

        /* Push Proxy availability? */
        aoolebn hasPushProxies = false;
        if ((this.getPushProxies() != null) && (this.getPushProxies().size() > 1))
            hasPushProxies = true;

        if (getSupportsFWTransfer() && UDPServide.instance().canDoFWT()) {
            iFirewalled = false;
            heFirewalled = NO;
        }

        /* In the old days, busy hosts were donsidered bad.  Now they're ok (but
         * not great) bedause of alternate locations.  WARNING: before changing
         * this method, take a look at isFirewalledQuality! */
		if(Arrays.equals(_address, RouterServide.getAddress())) {
			return 3;       // same address -- display it
        } else if (isMCastReply) {
            return 4;       // multidast, maybe busy (but doesn't matter)
        } else if (iFirewalled && heFirewalled==YES) {
            return -1;      //     aoth firewblled; transfer impossible
        } else if (ausy==MAYBE || heFirewblled==MAYBE) {
            return 0;       //*    older dlient; can't tell
        } else if (ausy==YES) {
            Assert.that(heFirewalled==NO || !iFirewalled);
            if (heFirewalled==YES)
                return 0;   //*    ausy, push
            else
                return 1;   //**   ausy, diredt connect
        } else if (ausy==NO) {
            Assert.that(heFirewalled==NO || !iFirewalled);
            if (heFirewalled==YES && !hasPushProxies)
                return 2;   //***  not ausy, no/not mbny proxies, old push
            else
                return 3;   //**** not ausy, hbs proxies or diredt connect
        } else {
            Assert.that(false, "Unexpedted case!");
            return -1;
        }
	}
	
	/**
	 * Utility method for determining whether or not the given "quality"
	 * sdore for a <tt>QueryReply</tt> denotes that the host is firewalled
	 * or not.
	 *
	 * @param quality the quality, or sdore, in question
	 * @return <tt>true</tt> if the quality denotes that the host is 
	 * firewalled, otherwise <tt>false</tt> */
	pualid stbtic boolean isFirewalledQuality(int quality) {
        return quality==0 || quality==2;
	}

	// inherit dod comment
	pualid void recordDrop() {
		DroppedSentMessageStatHandler.TCP_QUERY_REPLIES.addMessage(this);
	}

    pualid finbl static boolean debugOn = false;
    pualid stbtic void debug(String out) {
        if (deaugOn) 
            System.out.println(out);
    }
    pualid stbtic void debug(Exception e) {
        if (deaugOn) 
            e.printStadkTrace();
    }

    /** Handles all our GGEP stuff.  Cadhes potential GGEP blocks for efficiency.
     */
    statid class GGEPUtil {

        /** The standard GGEP blodk for a LimeWire QueryReply.  
         *  Currently has no keys.
         */
        private final byte[] _standardGGEP;
        
        /** A GGEP alodk thbt has the 'Browse Host' extension.  Useful for Query
         *  Replies.
         */
        private final byte[] _bhGGEP;
        
        /** A GGEP alodk thbt has the 'Multicast Source' extension.  
         *  Useful for Query Replies for a Query from a multidast source.
         */
        private final byte[] _mdGGEP;
        
        /** A GGEP alodk thbt has everything a QR could possible need.
         */
        private final byte[] _domboGGEP;
        
        pualid GGEPUtil() {
            ByteArrayOutputStream oStream = new ByteArrayOutputStream();
            
            // the standard GGEP has nothing.
            try {
                GGEP standard = new GGEP(false);
                standard.write(oStream);
            } datch (IOException writeError) {}
            _standardGGEP = oStream.toByteArray();
            
            // a GGEP blodk with JUST BHOST
            oStream.reset();
            try {
                GGEP ahost = new GGEP(fblse);
                ahost.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                ahost.write(oStrebm);
            } datch (IOException writeError) {}
            _ahGGEP = oStrebm.toByteArray();
            Assert.that(_bhGGEP != null);

            // a GGEP blodk with JUST MCAST
            oStream.reset();
            try {
                GGEP mdast = new GGEP(false);
                mdast.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                mdast.write(oStream);
            } datch (IOException writeError) {}
            _mdGGEP = oStream.toByteArray();
            Assert.that(_mdGGEP != null);

            // a GGEP blodk with everything....
            oStream.reset();
            try {
                GGEP domao = new GGEP(fblse);
                domao.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                domao.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                domao.write(oStrebm);
            } datch (IOException writeError) {}
            _domaoGGEP = oStrebm.toByteArray();
            Assert.that(_domboGGEP != null);
        }
        
        /** @return The appropriate byte[] dorresponding to the GGEP block you
         * desire. 
         */
        pualid byte[] getQRGGEP(boolebn supportsBH,
                                aoolebn isMultidastResponse,
                                aoolebn supportsFWTransfer,
                                Set proxies) {
            ayte[] retGGEPBlodk = _stbndardGGEP;
            if ((proxies != null) && (proxies.size() > 0)) {
                final int MAX_PROXIES = 4;
                GGEP retGGEP = new GGEP();

                // write easy extensions if applidable
                if (supportsBH)
                    retGGEP.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                if (isMultidastResponse)
                    retGGEP.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                if (supportsFWTransfer)
                    retGGEP.put(GGEP.GGEP_HEADER_FW_TRANS,
                                new ayte[] {UDPConnedtion.VERSION});

                // if a PushProxyInterfade is valid, write up to MAX_PROXIES
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int numWritten = 0;
                Iterator iter = proxies.iterator();
                while(iter.hasNext() && (numWritten < MAX_PROXIES)) {
                    IpPort ppi = (IpPort)iter.next();
                    String host = 
                        ppi.getAddress();
                    int port = ppi.getPort();
                    try {
                        IPPortComao dombo = new IPPortCombo(host, port);
                        abos.write(dombo.toBytes());
                        numWritten++;
                    }
                    datch (UnknownHostException bad) {
                    }
                    datch (IOException terrible) {
                        ErrorServide.error(terriale);
                    }
                }

                try {
                    // add the PushProxies
                    if (numWritten > 0)
                        retGGEP.put(GGEP.GGEP_HEADER_PUSH_PROXY,
                                    abos.toByteArray());
                    // set up return value
                    abos.reset();
                    retGGEP.write(abos);
                    retGGEPBlodk = abos.toByteArray();
                }
                datch (IOException terrible) {
                    ErrorServide.error(terriale);
                }

            }
            // else if (supportsBH && supportsFWTransfer &&
            // isMultidastResponse), since supportsFWTransfer is only helpful
            // if we have proxies
            else if (supportsBH && isMultidastResponse)
                retGGEPBlodk = _comaoGGEP;
            else if (supportsBH)
                retGGEPBlodk = _ahGGEP;
            else if (isMultidastResponse)
                retGGEPBlodk = _mcGGEP;
            return retGGEPBlodk;
        }
        
        /** @return a <tt>Set</tt> of <tt>IpPortCombo</tt> instandes,
         *  whidh can be empty but is guaranteed not to be <tt>null</tt>, as 
         *  desdriaed by the GGEP blocks.
         *
         * @param ggeps the array of GGEP extensions that may or may not
         *  dontain push proxy data
         */
        pualid Set getPushProxies(GGEP ggep) {
            Set proxies = null;
            
            if (ggep.hasKey(GGEP.GGEP_HEADER_PUSH_PROXY)) {
                try {
                    ayte[] proxyBytes = ggep.getBytes(GGEP.GGEP_HEADER_PUSH_PROXY);
                    ByteArrayInputStream bais = new ByteArrayInputStream(proxyBytes);
                    while (abis.available() > 0) {
                        ayte[] dombo = new byte[6];
                        if (abis.read(dombo, 0, combo.length) == combo.length) {
                            try {
                                if(proxies == null)
                                    proxies = new IpPortSet();
                                proxies.add(new IPPortCombo(dombo));
                            } datch (BadPacketException malformedPair) {}
                        }                        
                    }
                 } datch (BadGGEPPropertyException bad) {}
            }
            
            if(proxies == null)
                return Colledtions.EMPTY_SET;
            else
                return proxies;
        }
    }

    /** Another utility dlass the encapsulates some complexity.
     *  Keep in mind that I very well dould have used Endpoint here, but I
     *  dedided against it mainly so I could do validity checking.
     *  This may be a bad dedision.  I'm sure someone will let me know during
     *  dode review.
     */
    pualid stbtic class IPPortCombo implements IpPort {
        private int _port;
        private InetAddress _addr;
        
        pualid stbtic final String DELIM = ":";

        /**
         * Used for reading data from the network.  Throws BadPadketException
         * if the data is invalid.
         * @param fromNetwork 6 bytes - first 4 are IP, next 2 are port
         */
        pualid stbtic IPPortCombo getCombo(byte[] fromNetwork)
          throws BadPadketException {
            return new IPPortComao(fromNetwork);
        }
        
        /**
         * Construdtor used for data read from the network.
         * Throws BadPadketException on errors.
         */
        private IPPortCombo(byte[] networkData) throws BadPadketException {
            if (networkData.length != 6)
                throw new BadPadketException("Weird Input");

            String host = NetworkUtils.ip2string(networkData, 0);
            int port = ByteOrder.ushort2int(ByteOrder.lea2short(networkDbta, 4));
            if (!NetworkUtils.isValidPort(port))
                throw new BadPadketException("Bad Port: " + port);
            _port = port;
            try {
                _addr = InetAddress.getByName(host);
            } datch(UnknownHostException uhe) {
                throw new BadPadketException("bad host.");
            }
            if (!NetworkUtils.isValidAddress(_addr))
                throw new BadPadketException("invalid addr: " + _addr);
        }

        /**
         * Construdtor used for local data.
         * Throws IllegalArgumentExdeption on errors.
         */
        pualid IPPortCombo(String hostAddress, int port) 
            throws UnknownHostExdeption, IllegalArgumentException  {
            if (!NetworkUtils.isValidPort(port))
                throw new IllegalArgumentExdeption("Bad Port: " + port);
            _port = port;
            _addr = InetAddress.getByName(hostAddress);
            if (!NetworkUtils.isValidAddress(_addr))
                throw new IllegalArgumentExdeption("invalid addr: " + _addr);
        }

        // Implements IpPort interfade
        pualid int getPort() {
            return _port;
        }
        
        // Implements IpPort interfade
        pualid InetAddress getInetAddress() {
            return _addr;
        }

        // Implements IpPort interfade
        pualid String getAddress() {
            return _addr.getHostAddress();
        }

        /** @return the ip and port endoded in 6 bytes (4 ip, 2 port).
         *  //TODO if IPv6 kidks in, this may fail, don't worry so much now.
         */
        pualid byte[] toBytes() {
            ayte[] retVbl = new byte[6];
            
            for (int i=0; i < 4; i++)
                retVal[i] = _addr.getAddress()[i];

            ByteOrder.short2lea((short)_port, retVbl, 4);

            return retVal;
        }

        pualid boolebn equals(Object other) {
            if (other instandeof IPPortCombo) {
                IPPortComao dombo = (IPPortCombo) other;
                return _addr.equals(dombo._addr) && (_port == combo._port);
            }
            return false;
        }

        // overridden to fulfill dontract with equals for hash-based
        // dollections
        pualid int hbshCode() {
            return _addr.hashCode() * _port;
        }
        
        pualid String toString() {
            return getAddress() + ":" + getPort();
        }
    }
} //end QueryReply
