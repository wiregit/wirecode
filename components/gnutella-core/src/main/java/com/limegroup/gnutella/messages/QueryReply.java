package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.statistics.*;
import java.io.*;
import java.net.*;
import java.util.Locale;
import com.sun.java.util.collections.*;

/**
 * A query reply.  Contains information about the responding host in addition to
 * an array of responses.  These responses are not parsed until the getResponses
 * method is called.  For efficiency reasons, bad query reply packets may not be
 * discovered until the getResponses methods are called.<p>
 *
 * This class has partial support for BearShare-style query reply trailers.  You
 * can extract the vendor code, push flag, and busy flag. These methods may
 * throw BadPacketException if the metadata cannot be extracted.  Note that
 * BadPacketException does not mean that other data (namely responses) cannot be
 * read; MissingDataException might have been a better name.  
 * 
 * This class also encapsulates xml metadata.  See the description of the QHD 
 * below for more details.
 */
public class QueryReply extends Message implements Serializable{
    //Rep rationale: because most queries aren't directed to us (we'll just
    //forward them) we extract the responses lazily as needed.
    //When they are extracted, however, it makes sense to store the parsed
    //data in the responses field.
    //
    //WARNING: see note in Message about IP addresses.

    // some parameters about xml, namely the max size of a xml collection string.
    public static final int XML_MAX_SIZE = 32768;
    
    /** 2 bytes for public area, 2 bytes for xml length.
     */
    public static final int COMMON_PAYLOAD_LEN = 4;

    private byte[] _payload;
    /** True if the responses and metadata have been extracted. */
    private volatile boolean _parsed = false;        
    /** If parsed, the response records for this, or null if they could not
     *  be parsed. */
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
    /** If parsed, one of TRUE (client supports chat), FALSE, or UNDEFINED. */
    private volatile int _supportsChat = UNDEFINED;
     /** If parsed, one of TRUE (client supports browse host), 
      * FALSE, or UNDEFINED. */
    private volatile int _supportsBrowseHost = FALSE;
     /** If parsed, one of TRUE (reply sent in response to a multicast query), 
      * FALSE, or UNDEFINED. */
    private volatile int _replyToMulticast = FALSE;
    /** Boolean for whether or not the remote host supports Firewalled Transfer.
     *  This does not follow the usual example where an int takes on TRUE,
     *  FALSE, or UNDEFINED.  Assume it is false and set it to true if otherwise.
     */
    private volatile boolean _supportsFWTransfer = false;
    /** Version number of FW Transfer the host supports. */
    private volatile byte _fwTransferVersion = (byte)0;

    private static final int TRUE=1;
    private static final int FALSE=0;
    private static final int UNDEFINED=-1;

    /** The mask for extracting the push flag from the QHD common area. */
    private static final byte PUSH_MASK=(byte)0x01;
    /** The mask for extracting the busy flag from the QHD common area. */
    private static final byte BUSY_MASK=(byte)0x04;
    /** The mask for extracting the busy flag from the QHD common area. */
    private static final byte UPLOADED_MASK=(byte)0x08;
    /** The mask for extracting the busy flag from the QHD common area. */
    private static final byte SPEED_MASK=(byte)0x10;
    /** The mask for extracting the GGEP flag from the QHD common area. */
    private static final byte GGEP_MASK=(byte)0x20;

    /** The mask for extracting the chat flag from the QHD private area. */
    private static final byte CHAT_MASK=(byte)0x01;
    
    /** The xml chunk that contains metadata about xml responses*/
    private byte[] _xmlBytes = new byte[0];

	/** The raw ip address of the host returning the hit.*/
	private byte[] _address = new byte[4];

    /** the PushProxy info for this hit.
     */
    private Set _proxies;
    
    /**
     * Whether or not this is a result from a browse-host reply.
     */
    private boolean _browseHostReply;
    
    /**
     * The HostData containing information about this QueryReply.
     * Only set if this QueryReply is parsed.
     */
    private HostData _hostData;
    

    /** Our static and final instance of the GGEPUtil helper class.
     */
    private static final GGEPUtil _ggepUtil = new GGEPUtil();

    /** Creates a new query reply.  The number of responses is responses.length
     *  The Browse Host GGEP extension is ON by default.  
     *
     *  @requires  0 < port < 2^16 (i.e., can fit in 2 unsigned bytes),
     *    ip.length==4 and ip is in <i>BIG-endian</i> byte order,
     *    0 < speed < 2^32 (i.e., can fit in 4 unsigned bytes),
     *    responses.length < 2^8 (i.e., can fit in 1 unsigned byte),
     *    clientGUID.length==16
     */
    public QueryReply(byte[] guid, byte ttl,
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, boolean isMulticastReply) {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             DataUtils.EMPTY_BYTE_ARRAY,
             false, false, false, false, false, false, true, isMulticastReply,
             false, DataUtils.EMPTY_SET);
    }


    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by default.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots.  
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param supportsChat true iff the host currently allows chatting.
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID,
            boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,boolean supportsChat,
            boolean isMulticastReply) {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             DataUtils.EMPTY_BYTE_ARRAY,
             true, needsPush, isBusy, finishedUpload,
             measuredSpeed,supportsChat,
             true, isMulticastReply, false, DataUtils.EMPTY_SET);
    }


    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by default.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param xmlBytes The (non-null) byte[] containing aggregated
     * and indexed information regarding file metadata.  In terms of byte-size, 
     * this should not be bigger than 65535 bytes.  Anything larger will result
     * in an Exception being throw.  This String is assumed to consist of
     * compressed data.
     * @param supportsChat true iff the host currently allows chatting.
     * @exception IllegalArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, byte[] xmlBytes,
            boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,boolean supportsChat,
            boolean isMulticastReply) 
        throws IllegalArgumentException {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             xmlBytes, needsPush, isBusy,  finishedUpload, measuredSpeed, 
             supportsChat, isMulticastReply, DataUtils.EMPTY_SET);
    }

    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by default.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param xmlBytes The (non-null) byte[] containing aggregated
     * and indexed information regarding file metadata.  In terms of byte-size, 
     * this should not be bigger than 65535 bytes.  Anything larger will result
     * in an Exception being throw.  This String is assumed to consist of
     * compressed data.
     * @param supportsChat true iff the host currently allows chatting.
     * @param proxies an array of PushProxy interfaces.  will be included in 
     * the replies GGEP extension.
     * @exception IllegalArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, byte[] xmlBytes,
            boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,boolean supportsChat,
            boolean isMulticastReply, Set proxies) 
        throws IllegalArgumentException {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             xmlBytes, true, needsPush, isBusy, 
             finishedUpload, measuredSpeed,supportsChat, true, isMulticastReply,
             false, proxies);
        if (xmlBytes.length > XML_MAX_SIZE)
            throw new IllegalArgumentException("XML bytes too big: " +
                                               xmlBytes.length);
        _xmlBytes = xmlBytes;        
    }


    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by default.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param xmlBytes The (non-null) byte[] containing aggregated
     * and indexed information regarding file metadata.  In terms of byte-size, 
     * this should not be bigger than 65535 bytes.  Anything larger will result
     * in an Exception being throw.  This String is assumed to consist of
     * compressed data.
     * @param supportsChat true iff the host currently allows chatting.
     * @param proxies an array of PushProxy interfaces.  will be included in 
     * the replies GGEP extension.
     * @exception IllegalArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, byte[] xmlBytes,
            boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,boolean supportsChat,
            boolean isMulticastReply, boolean supportsFWTransfer, Set proxies) 
        throws IllegalArgumentException {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             xmlBytes, true, needsPush, isBusy, 
             finishedUpload, measuredSpeed,supportsChat, true, isMulticastReply,
             supportsFWTransfer, proxies);
        if (xmlBytes.length > XML_MAX_SIZE)
            throw new IllegalArgumentException("XML bytes too big: " +
                                               xmlBytes.length);
        _xmlBytes = xmlBytes;        
    }


    /** Creates a new query reply with data read from the network. */
    public QueryReply(byte[] guid, byte ttl, byte hops,byte[] payload) 
		throws BadPacketException {
    	this(guid,ttl,hops,payload,Message.N_UNKNOWN);
                                       
    }
    
    public QueryReply(byte[] guid, byte ttl, byte hops,byte[] payload,int network) 
    	throws BadPacketException{
    	super(guid, Message.F_QUERY_REPLY, ttl, hops, payload.length,network);
        this._payload=payload;
        
		if(!NetworkUtils.isValidPort(getPort())) {
		    ReceivedErrorStat.REPLY_INVALID_PORT.incrementStat();
			throw new BadPacketException("invalid port");
		}
		if( (getSpeed() & 0xFFFFFFFF00000000L) != 0) {
		    ReceivedErrorStat.REPLY_INVALID_SPEED.incrementStat();
			throw new BadPacketException("invalid speed: " + getSpeed());
		} 		
		
		setAddress();
		
		if(!NetworkUtils.isValidAddress(getIPBytes())) {
		    ReceivedErrorStat.REPLY_INVALID_ADDRESS.incrementStat();
		    throw new BadPacketException("invalid address");
		}
		
        //repOk();
    }

    /**
	 * Copy constructor.  Creates a new query reply from the passed query
	 * Reply. The new one is same as the passed one, but with different specified
	 * GUID.<p>
	 *
	 * Note: The payload is not really copied, but the reference in the newly
	 * constructed query reply, points to the one in the passed reply.  But since
	 * the payload cannot be mutated, it shouldn't make difference if different
	 * query replies maintain reference to same payload
	 *
	 * @param guid The new GUID for the reply
	 * @param reply The query reply from where to copy the fields into the
	 *  new constructed query reply 
	 */
    public QueryReply(byte[] guid, QueryReply reply){
        //call the super constructor with new GUID
        super(guid, Message.F_QUERY_REPLY, reply.getTTL(), reply.getHops(),
			  reply.getLength());
        //set the payload field
        this._payload = reply._payload;
		setAddress();
    }

    /** 
     * Internal constructor.  Only creates QHD if includeQHD==true.  
     */
    private QueryReply(byte[] guid, byte ttl, 
             int port, byte[] ip, long speed, Response[] responses,
             byte[] clientGUID, byte[] xmlBytes,
             boolean includeQHD, boolean needsPush, boolean isBusy,
             boolean finishedUpload, boolean measuredSpeed,
             boolean supportsChat, boolean supportsBH,
             boolean isMulticastReply, boolean supportsFWTransfer, 
             Set proxies) {
        super(guid, Message.F_QUERY_REPLY, ttl, (byte)0,
              0,                               // length, update later
              16);                             // 16-byte footer
        // you aren't going to send this.  it will throw an exception above in
        // the appropriate constructor....
        if (xmlBytes.length > XML_MAX_SIZE)
            return;  

        final int n = responses.length;
		if(!NetworkUtils.isValidPort(port)) {
			throw new IllegalArgumentException("invalid port: "+port);
		} else if(ip.length != 4) {
			throw new IllegalArgumentException("invalid ip length: "+ip.length);
        } else if(!NetworkUtils.isValidAddress(ip)) {
            throw new IllegalArgumentException("invalid address: " + 
                    NetworkUtils.ip2string(ip));
		} else if((speed & 0xFFFFFFFF00000000l) != 0) {
			throw new IllegalArgumentException("invalid speed: "+speed);
		} else if(n >= 256) {
			throw new IllegalArgumentException("invalid num responses: "+n);
		}

        // set up proxies
        _proxies = proxies;
        _supportsFWTransfer = supportsFWTransfer;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            //Write beginning of payload.
            //Downcasts are ok, even if they go negative
            baos.write(n);
            ByteOrder.short2leb((short)port, baos);
            baos.write(ip, 0, ip.length);
            ByteOrder.int2leb((int)speed, baos);
            
            //Write each response
            for (int left=n; left>0; left--) {
                Response r=responses[n-left];
                r.writeToStream(baos);
            }
            
            //Write QHD if desired
            if (includeQHD) {
                //a) vendor code.  This is hardcoded here for simplicity,
                //efficiency, and to prevent character decoding problems.  If you
                //change this, be sure to change CommonUtils.QHD_VENDOR_NAME as
                //well.
                baos.write(76); //'L'
                baos.write(73); //'I'
                baos.write(77); //'M'
                baos.write(69); //'E'
                
                //b) payload length
                baos.write(COMMON_PAYLOAD_LEN);
                
                // size of standard, no options, ggep block...
                int ggepLen=
                    _ggepUtil.getQRGGEP(false, false, false,
                                        DataUtils.EMPTY_SET).length;
                
                //c) PART 1: common area flags and controls.  See format in
                //parseResults2.
                boolean hasProxies = (_proxies != null) && (_proxies.size() > 0);
                byte flags=
                    (byte)((needsPush && !isMulticastReply ? PUSH_MASK : 0) 
                           | BUSY_MASK 
                           | UPLOADED_MASK 
                           | SPEED_MASK
                           | GGEP_MASK);
                byte controls=
                    (byte)(PUSH_MASK
                           | (isBusy && !isMulticastReply ? BUSY_MASK : 0) 
                           | (finishedUpload ? UPLOADED_MASK : 0)
                           | (measuredSpeed || isMulticastReply ? SPEED_MASK : 0)
                           | (supportsBH || isMulticastReply || hasProxies ||
                              supportsFWTransfer ? 
                              GGEP_MASK : (ggepLen > 0 ? GGEP_MASK : 0)) );

                baos.write(flags);
                baos.write(controls);
                
                //d) PART 2: size of xmlBytes + 1.
                int xmlSize = xmlBytes.length + 1;
                if (xmlSize > XML_MAX_SIZE)
                    xmlSize = XML_MAX_SIZE;  // yes, truncate!
                ByteOrder.short2leb(((short) xmlSize), baos);
                
                //e) private area: one byte with flags 
                //for chat support
                byte chatSupport=(byte)(supportsChat ? CHAT_MASK : 0);
                baos.write(chatSupport);
                
                //f) the GGEP block
                byte[] ggepBytes = _ggepUtil.getQRGGEP(supportsBH,
                                                       isMulticastReply,
                                                       supportsFWTransfer,
                                                       _proxies);
                baos.write(ggepBytes, 0, ggepBytes.length);
                
                //g) actual xml.
                baos.write(xmlBytes, 0, xmlBytes.length);
                
                // write null after xml, as specified
                baos.write(0);
            }

            //Write footer
            baos.write(clientGUID, 0, 16);
            
            // setup payload params
            _payload = baos.toByteArray();
            updateLength(_payload.length);
        }
        catch (IOException reallyBad) {
            ErrorService.error(reallyBad);
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
	
	public void setOOBAddress(InetAddress addr, int port) {
		_address =addr.getAddress();
		ByteOrder.short2leb((short)port,_payload,1);
		
	}

    /**
     * Sets the guid for this message. Is needed, when we want to cache 
     * query replies or sfor some other reason want to change the GUID as 
     * per the guid of query request
     * @param guid The guid to be set
     */
    public void setGUID(GUID guid) {
        super.setGUID(guid);
    }

	// inherit doc comment
    public void writePayload(OutputStream out) throws IOException {
        out.write(_payload);
		SentMessageStatHandler.TCP_QUERY_REPLIES.addMessage(this);
    }
    
    /**
     * Sets this reply to be considered a 'browse host' reply.
     */
    public void setBrowseHostReply(boolean isBH) {
        _browseHostReply = isBH;
    }
    
    
    /**
     * Gets whether or not this reply is from a browse host request.
     */
    public boolean isBrowseHostReply() {
        return _browseHostReply;
    }

    /** Return the associated xml metadata string if the queryreply
     *  contained one.
     */
    public byte[] getXMLBytes() {
        parseResults();
        return _xmlBytes;
    }

    /** Return the number of results N in this query. */
    public short getResultCount() {
        //The result of ubyte2int always fits in a short, so downcast is ok.
        return (short)ByteOrder.ubyte2int(_payload[0]);
    }

    public int getPort() {
        return ByteOrder.ubytes2int(ByteOrder.leb2short(_payload,1));
    }

    /** Returns the IP address of the responding host in standard
     *  dotted-decimal format, e.g., "192.168.0.1" */
    public String getIP() {
        return NetworkUtils.ip2string(_address); //takes care of signs
    }

    /**
     * Accessor the IP address in byte array form.
     *
     * @return the IP address for this query hit as an array of bytes
     */
    public byte[] getIPBytes() {
        return _address;
    }

    public long getSpeed() {
        return ByteOrder.ubytes2long(ByteOrder.leb2int(_payload,7));
    }
    
    /**
     * Returns the Response[].  Throws BadPacketException if this
     * data couldn't be extracted.
     */
    public Response[] getResultsArray() throws BadPacketException {
        parseResults();
        if(_responses == null)
            throw new BadPacketException();
        return _responses;
    }

    /** Returns an iterator that will yield the results, each as an
     *  instance of the Response class.  Throws BadPacketException if
     *  this data couldn't be extracted.  */
    public Iterator getResults() throws BadPacketException {
        parseResults();
        if (_responses==null)
            throw new BadPacketException();
        List list=Arrays.asList(_responses);
        return list.iterator();
    }


    /** Returns a List that will yield the results, each as an
     *  instance of the Response class.  Throws BadPacketException if
     *  this data couldn't be extracted.  */
    public List getResultsAsList() throws BadPacketException {
        parseResults();
        if (_responses==null)
            throw new BadPacketException("results are null");
        List list=Arrays.asList(_responses);
        return list;
    }


    /** 
     * Returns the name of this' vendor, all capitalized.  Throws
     * BadPacketException if the data couldn't be extracted, either because it
     * is missing or corrupted. 
     */
    public String getVendor() throws BadPacketException {
        parseResults();
        if (_vendor==null)
            throw new BadPacketException();
        return _vendor;        
    }

    /** 
     * Returns true if this's push flag is set, i.e., a push download is needed.
     * Returns false if the flag is present but not set.  Throws
     * BadPacketException if the flag couldn't be extracted, either because it
     * is missing or corrupted.  
     */
    public boolean getNeedsPush() throws BadPacketException {
        parseResults();

        switch (_pushFlag) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for push flag: "+_pushFlag);
            return false;
        }
    }

    /** 
     * Returns true if this has no more download slots.  Returns false if the
     * busy bit is present but not set.  Throws BadPacketException if the flag
     * couldn't be extracted, either because it is missing or corrupted.  
     */
    public boolean getIsBusy() throws BadPacketException {
        parseResults();

        switch (_busyFlag) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for busy flag: "+_pushFlag);
            return false;
        }
    }

    /** 
     * Returns true if this has successfully uploaded a complete file (bit set).
     * Returns false if the bit is not set.  Throws BadPacketException if the
     * flag couldn't be extracted, either because it is missing or corrupted.  
     */
    public boolean getHadSuccessfulUpload() throws BadPacketException {
        parseResults();

        switch (_uploadedFlag) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for uploaded flag: "+_pushFlag);
            return false;
        }
    }

    /** 
     * Returns true if the speed in this QueryReply was measured (bit set).
     * Returns false if it was set by the user (bit unset).  Throws
     * BadPacketException if the flag couldn't be extracted, either because it
     * is missing or corrupted.  
     */
    public boolean getIsMeasuredSpeed() throws BadPacketException {
        parseResults();

        switch (_measuredSpeedFlag) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for measured speed flag: "+_pushFlag);
            return false;
        }
    }

    /** 
     * Returns true iff the client supports chat.  Throws BadPacketException if
     * the flag couldn't be extracted, either because it is missing or
     * corrupted.  Typically this exception is treated the same way as returning
     * false.  
     */
    public boolean getSupportsChat() throws BadPacketException {
        parseResults();

        switch (_supportsChat) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for supportsChat: "+_supportsChat);
            return false;
        }
    }

    /** @return true if the remote host can firewalled transfers.
     */
    public boolean getSupportsFWTransfer() {
        parseResults();
        return _supportsFWTransfer;
    }

    /** @return 1 or greater if FW Transfer is supported, else 0.
     */
    public byte getFWTransferVersion() {
        parseResults();
        return _fwTransferVersion;
    }

    /** 
     * Returns true iff the client supports browse host feature.
     * @return true, if the client supports browse host feature,
     * false otherwise
     * @exception Throws BadPacketException if
     * the flag couldn't be extracted, either because it is missing or
     * corrupted.  Typically this exception is treated the same way as returning
     * false. 
     */
    public boolean getSupportsBrowseHost() throws BadPacketException {
        parseResults();

        switch (_supportsBrowseHost) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for supportsBrowseHost: "
                + _supportsBrowseHost);
            return false;
        }
    }
    
    /** 
     * Returns true iff the reply was sent in response to a multicast query.
     * @return true, iff the reply was sent in response to a multicast query,
     * false otherwise
     * @exception Throws BadPacketException if
     * the flag couldn't be extracted, either because it is missing or
     * corrupted.  Typically this exception is treated the same way as returning
     * false. 
     */
    public boolean isReplyToMulticastQuery() throws BadPacketException {
        parseResults();

        switch (_replyToMulticast) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for replyToMulticast: "
                + _replyToMulticast);
            return false;
        }
    }

    /**
     * @return null or a non-zero lenght array of PushProxy hosts.
     */
    public Set getPushProxies() {
        parseResults();
        return _proxies;
    }
    
    /**
     * Returns the HostData object describing information
     * about this QueryReply.
     */
    public HostData getHostData() throws BadPacketException {
        parseResults();
        if( _hostData == null )
            throw new BadPacketException();
        return _hostData;
    }

    
    /** @modifies this.responses, this.pushFlagSet, this.vendor, parsed
     *  @effects tries to extract responses from payload and store in responses. 
     *    Tries to extract metadata and store in vendor and pushFlagSet.
     *    You can tell if data couldn't be extracted by looking if responses
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
     * accessor methods for this class will all throw 
     * <tt>BadPacketException</tt>.  This is because a single invalid
     * response invalidates other invariants, such as the field for
     * the number of results matching the size of the result array.
     */
    private void parseResults2() {
        //index into payload to look for next response
        int i=11;

        //1. Extract responses.  These are not copied to this.responses until
        //they are verified.  Note, however that the metainformation need not be
        //verified for these to be acceptable.  Also note that exceptions are
        //silently caught.
        int left=getResultCount();          //number of records left to get
        Response[] responses=new Response[left];
        try {
            InputStream bais = 
                new ByteArrayInputStream(_payload,i,_payload.length-i);
            //For each record...
            for ( ; left > 0; left--) {
                Response r = Response.createFromStream(bais);
                responses[responses.length-left] = r;
                i+=r.getLength();
            }
            //All set.  Accept parsed results.
            this._responses=responses;
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        } catch (IOException e) {
            return;
        }
        
        //2. Extract BearShare-style metainformation, if any.  Any exceptions
        //are silently caught.  The definitive reference for this format is at
        //http://www.clip2.com/GnutellaProtocol04.pdf.  Briefly, the format is 
        //      vendor code           (4 bytes, case insensitive)
        //      common payload length (4 byte, unsigned, always>0)
        //      common payload        (length given above.  See below.)
        //      vendor payload        (length until clientGUID)
        //The normal 16 byte clientGUID follows, of course.
        //
        //The first byte of the common payload has a one in its 0'th bit* if we
        //should try a push.  However, if there is a second byte, and if the
        //0'th bit of this byte is zero, the 0'th bit of the first byte should
        //actually be interpreted as MAYBE.  Unfortunately LimeWire 1.4 failed
        //to set this bit in the second byte, so it should be ignored when 
        //parsing, though set on writing.
        //
        //The remaining bits of the first byte of the common payload area tell
        //whether the corresponding bits in the optional second byte is defined.
        //The idea behind having two bits per flag is to distinguish between
        //YES, NO, and MAYBE.  These bits are as followed:
        //      bit 1*  undefined, for historical reasons
        //      bit 2   1 iff server is busy
        //      bit 3   1 iff server has successfully completed an upload
        //      bit 4   1 iff server's reported speed was actually measured, not
        //              simply set by the user.
        //
        // GGEP Stuff
        // Byte 5 and 6, if the 5th bit is set, signal that there is a GGEP
        // block.  The GGEP block will be after the common payload and will be
        // headed by the GGEP magic prefix (see the GGEP class for more details.
        //
        // If there is a GGEP block, then we look to see what is supported.
        //
        //*Here, we use 0-(N-1) numbering.  So "0'th bit" refers to the least
        //significant bit.
        /* ----------------------------------------------------------------
         * QHD UPDATE 8/17/01
         * Here is an updated QHD spec.
         * 
         * Byte 0-3 : Vendor Code
         * Byte 4   : Public area size (COMMON_PAYLOAD_LEN)
         * Byte 5-6 : Public area (as described above)
         * Byte 7-8 : Size of XML + 1 (for a null), you need to count backward
         * from the client GUID.
         * Byte 9   : private vendor flag
         * Byte 10-X: GGEP area
         * Byte X-beginning of xml : (new) private area
         * Byte (payload.length - 16 - xmlSize (above)) - 
                (payload.length - 16 - 1) : XML!!
         * Byte (payload.length - 16 - 1) : NULL
         * Last 16 Bytes: client GUID.
         */
        try {
			if (i >= (_payload.length-16)) {   //see above
                throw new BadPacketException("No QHD");
            }
            //Attempt to verify.  Results are not copied to this until verified.
            String vendorT=null;
            int pushFlagT=UNDEFINED;
            int busyFlagT=UNDEFINED;
            int uploadedFlagT=UNDEFINED;
            int measuredSpeedFlagT=UNDEFINED;
            int supportsChatT=UNDEFINED;
            int supportsBrowseHostT=UNDEFINED;
            int replyToMulticastT=FALSE;
            Set proxies=null;
            
            //a) extract vendor code
            try {
                //Must use ISO encoding since characters are more than two
                //bytes on other platforms.  TODO: test on different installs!
                vendorT=new String(_payload, i, 4, "ISO-8859-1");
                Assert.that(vendorT.length()==4,
                            "Vendor length wrong.  Wrong character encoding?");
            } catch (UnsupportedEncodingException e) {
                Assert.that(false, "No support for ISO-8859-1 encoding");
            }
            i+=4;

            //b) extract payload length
            int length=ByteOrder.ubyte2int(_payload[i]);
            if (length<=0)
                throw new BadPacketException("Common payload length zero.");
            i++;
            if ((i + length) > (_payload.length-16)) // 16 is trailing GUID size
                throw new BadPacketException("Common payload length imprecise!");

            //c) extract push and busy bits from common payload
            // REMEMBER THAT THE PUSH BIT IS SET OPPOSITE THAN THE OTHERS.
            // (The 'I understand' is the second bit, the Yes/No is the first)
            if (length > 1) {   //BearShare 2.2.0+
                byte control=_payload[i];
                byte flags=_payload[i+1];
                if ((flags & PUSH_MASK)!=0)
                    pushFlagT = (control&PUSH_MASK)==1 ? TRUE : FALSE;
                if ((control & BUSY_MASK)!=0)
                    busyFlagT = (flags&BUSY_MASK)!=0 ? TRUE : FALSE;
                if ((control & UPLOADED_MASK)!=0)
                    uploadedFlagT = (flags&UPLOADED_MASK)!=0 ? TRUE : FALSE;
                if ((control & SPEED_MASK)!=0)
                    measuredSpeedFlagT = (flags&SPEED_MASK)!=0 ? TRUE : FALSE;
                if ((control & GGEP_MASK)!=0 && (flags & GGEP_MASK)!=0) {
                    // GGEP processing
                    // iterate past flags...
                    int magicIndex = i + 2;
                    for (; 
                         (_payload[magicIndex]!=GGEP.GGEP_PREFIX_MAGIC_NUMBER) &&
                         (magicIndex < _payload.length);
                         magicIndex++)
                        ; // get the beginning of the GGEP stuff...
                    GGEP[] ggepBlocks = null;
                    try {
                        // if there are GGEPs, see if Browse Host supported...
                        // TODO: stop using GGEP.read(2) - move to GGEP.read(3)
                        // or fix up GGEP.read(2)
                        ggepBlocks = GGEP.read(_payload, magicIndex);
                        if (_ggepUtil.allowsBrowseHost(ggepBlocks))
                            supportsBrowseHostT = TRUE;
                        _fwTransferVersion = 
                            _ggepUtil.getFWTransferVersion(ggepBlocks);
                        if (_fwTransferVersion > 0) _supportsFWTransfer = true;
                        if (_ggepUtil.replyToMulticastQuery(ggepBlocks))
                            replyToMulticastT = TRUE;
                        else
                            replyToMulticastT = FALSE;
                        proxies = _ggepUtil.getPushProxies(ggepBlocks);
                    }
                    catch (BadGGEPBlockException ignored) {
                    }
                }
                i+=2; // increment used bytes appropriately...
            }

            if (length > 2) { // expecting XML.
                //d) we need to get the xml stuff.  
                //first we should get its size, then we have to look 
                //backwards and get the actual xml...
                int a, b, temp;
                temp = ByteOrder.ubyte2int(_payload[i++]);
                a = temp;
                temp = ByteOrder.ubyte2int(_payload[i++]);
                b = temp << 8;
                int xmlSize = a | b;
                if (xmlSize > 1) {
                    int xmlInPayloadIndex = _payload.length-16-xmlSize;
                    _xmlBytes = new byte[xmlSize-1];
                    System.arraycopy(_payload, xmlInPayloadIndex,
                                     _xmlBytes, 0,
                                     (xmlSize-1));
                }
                else
                    _xmlBytes = DataUtils.EMPTY_BYTE_ARRAY;
            }

            //Parse LimeWire's private area.  Currently only a single byte
            //whose LSB is 0x1 if we support chat, or 0x0 if we do.
            //Shareaza also supports our chat, don't disclude them...
            int privateLength=_payload.length-i;
            if (privateLength>0 && (vendorT.equals("LIME") ||
                                    vendorT.equals("RAZA"))) {
                byte privateFlags = _payload[i];
                supportsChatT = (privateFlags&CHAT_MASK)!=0 ? TRUE : FALSE;
            }

            if (i>_payload.length-16)
                throw new BadPacketException(
                    "Common payload length too large.");
            
            //All set.  Accept parsed values.
            Assert.that(vendorT!=null);
            this._vendor=vendorT.toUpperCase(Locale.US);
            this._pushFlag=pushFlagT;
            this._busyFlag=busyFlagT;
            this._uploadedFlag=uploadedFlagT;
            this._measuredSpeedFlag=measuredSpeedFlagT;
            this._supportsChat=supportsChatT;
            this._supportsBrowseHost=supportsBrowseHostT;
            this._replyToMulticast=replyToMulticastT;
            if(proxies == null) {
                this._proxies = DataUtils.EMPTY_SET;
            } else {
                this._proxies = proxies;
            }
            this._hostData = new HostData(this);
            debug("QR.parseResults2(): returning w/o exception.");

        } catch (BadPacketException e) {
            debug("QR.parseResults2(): bpe = " + e);
            return;
        } catch (IndexOutOfBoundsException e) {
            debug("QR.parseResults2(): index exception = " + e);
            return;
        } 
    }

    /** Returns the 16 byte client ID (i.e., the "footer") of the
     *  responding host.  */
    public byte[] getClientGUID() {
        byte[] result=new byte[16];
        //Copy the last 16 bytes of payload to result.  Note that there may
        //be metainformation before the client GUID.  So it is not correct
        //to simply count after the last result record.
        int length=super.getLength();
        System.arraycopy(_payload, length-16, result, 0, 16);
        return result;
    }

    /** Returns this, because it's always safe to send big replies. */
    public Message stripExtendedPayload() {
        return this;
    }

    public String toString() {
        return ("QueryReply::\r\n"+
				getResultCount()+" hits\r\n"+
				super.toString()+"\r\n"+
				"ip: "+getIP()+"\r\n");				
    }

	/**
     * This method calculates the quality of service for a given host.  The
     * calculation is some function of whether or not the host is busy, whether
     * or not the host has ever received an incoming connection, etc.
     * 
     * Moved this code from SearchView to here permanently, so we avoid
     * duplication.  It makes sense from a data point of view, but this method
     * isn't really essential an essential method.
     *
     * @return a int from -1 to 3, with -1 for "never work" and 3 for "always
     * work".  Typically a return value of N means N+1 stars will be displayed
     * in the GUI.
     * @param iFirewalled switch to indicate if the client is firewalled or
     * not.  See RouterService.acceptingIncomingConnection or Acceptor for
     * details.  
     */
	public int calculateQualityOfService(boolean iFirewalled) {
        final int YES=1;
        final int MAYBE=0;
        final int NO=-1;
        
        /* Is the remote host busy? */
		int busy;
		try {
			busy=this.getIsBusy() ? YES : NO;
		} catch (BadPacketException e) {
			busy = MAYBE;
		}
		
		boolean isMCastReply;
		try {
		    isMCastReply = this.isReplyToMulticastQuery();
		} catch(BadPacketException e) {
		    isMCastReply = false;
		}		       

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
			} catch (BadPacketException e) {
				heFirewalled = MAYBE;
			}
		}

        /* Push Proxy availability? */
        boolean hasPushProxies = false;
        if ((this.getPushProxies() != null) && 
            (this.getPushProxies().size() > 1))
            hasPushProxies = true;
            
        if (getSupportsFWTransfer() && 
            UDPService.instance().canReceiveSolicited()) {
            iFirewalled = false;
            heFirewalled = NO;
        }

        /* In the old days, busy hosts were considered bad.  Now they're ok (but
         * not great) because of alternate locations.  WARNING: before changing
         * this method, take a look at isFirewalledQuality! */
		if(Arrays.equals(_address, RouterService.getAddress())) {
			return 3;       // same address -- display it
        } else if (isMCastReply) {
            return 4;       // multicast, maybe busy (but doesn't matter)
        } else if (iFirewalled && heFirewalled==YES) {
            return -1;      //     both firewalled; transfer impossible
        } else if (busy==MAYBE || heFirewalled==MAYBE) {
            return 0;       //*    older client; can't tell
        } else if (busy==YES) {
            Assert.that(heFirewalled==NO || !iFirewalled);
            if (heFirewalled==YES)
                return 0;   //*    busy, push
            else
                return 1;   //**   busy, direct connect
        } else if (busy==NO) {
            Assert.that(heFirewalled==NO || !iFirewalled);
            if (heFirewalled==YES && !hasPushProxies)
                return 2;   //***  not busy, no/not many proxies, old push
            else
                return 3;   //**** not busy, has proxies or direct connect
        } else {
            Assert.that(false, "Unexpected case!");
            return -1;
        }
	}
	
	/**
	 * Utility method for determining whether or not the given "quality"
	 * score for a <tt>QueryReply</tt> denotes that the host is firewalled
	 * or not.
	 *
	 * @param quality the quality, or score, in question
	 * @return <tt>true</tt> if the quality denotes that the host is 
	 * firewalled, otherwise <tt>false</tt> */
	public static boolean isFirewalledQuality(int quality) {
        return quality==0 || quality==2;
	}

	// inherit doc comment
	public void recordDrop() {
		DroppedSentMessageStatHandler.TCP_QUERY_REPLIES.addMessage(this);
	}

    public final static boolean debugOn = false;
    public static void debug(String out) {
        if (debugOn) 
            System.out.println(out);
    }
    public static void debug(Exception e) {
        if (debugOn) 
            e.printStackTrace();
    }

    /** Handles all our GGEP stuff.  Caches potential GGEP blocks for efficiency.
     */
    static class GGEPUtil {

        /** The standard GGEP block for a LimeWire QueryReply.  
         *  Currently has no keys.
         */
        private final byte[] _standardGGEP;
        
        /** A GGEP block that has the 'Browse Host' extension.  Useful for Query
         *  Replies.
         */
        private final byte[] _bhGGEP;
        
        /** A GGEP block that has the 'Multicast Source' extension.  
         *  Useful for Query Replies for a Query from a multicast source.
         */
        private final byte[] _mcGGEP;
        
        /** A GGEP block that has everything a QR could possible need.
         */
        private final byte[] _comboGGEP;
        
        public GGEPUtil() {
            ByteArrayOutputStream oStream = new ByteArrayOutputStream();
            
            // the standard GGEP has nothing.
            try {
                GGEP standard = new GGEP(false);
                standard.write(oStream);
            }
            catch (IOException writeError) {
            }
            _standardGGEP = oStream.toByteArray();
            
            // a GGEP block with JUST BHOST
            oStream.reset();
            try {
                GGEP bhost = new GGEP(false);
                bhost.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                bhost.write(oStream);
            }
            catch (IOException writeError) {
            }
            _bhGGEP = oStream.toByteArray();
            Assert.that(_bhGGEP != null);

            // a GGEP block with JUST MCAST
            oStream.reset();
            try {
                GGEP mcast = new GGEP(false);
                mcast.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                mcast.write(oStream);
            }
            catch (IOException writeError) {
            }
            _mcGGEP = oStream.toByteArray();
            Assert.that(_mcGGEP != null);

            // a GGEP block with everything....
            oStream.reset();
            try {
                GGEP combo = new GGEP(false);
                combo.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                combo.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                combo.write(oStream);
            }
            catch (IOException writeError) {
            }
            _comboGGEP = oStream.toByteArray();
            Assert.that(_comboGGEP != null);
        }
        
        /** @return The appropriate byte[] corresponding to the GGEP block you
         * desire. 
         */
        public byte[] getQRGGEP(boolean supportsBH,
                                boolean isMulticastResponse,
                                boolean supportsFWTransfer,
                                Set proxies) {
            byte[] retGGEPBlock = _standardGGEP;
            if ((proxies != null) && (proxies.size() > 0)) {
                final int MAX_PROXIES = 4;
                GGEP retGGEP = new GGEP();

                // write easy extensions if applicable
                if (supportsBH)
                    retGGEP.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                if (isMulticastResponse)
                    retGGEP.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                if (supportsFWTransfer)
                    retGGEP.put(GGEP.GGEP_HEADER_FW_TRANS,
                                new byte[] {UDPConnection.VERSION});

                // if a PushProxyInterface is valid, write up to MAX_PROXIES
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int numWritten = 0;
                Iterator iter = proxies.iterator();
                while(iter.hasNext() && (numWritten < MAX_PROXIES)) {
                    PushProxyInterface ppi = (PushProxyInterface)iter.next();
                    String host = 
                        ppi.getPushProxyAddress().getHostAddress();
                    int port = ppi.getPushProxyPort();
                    try {
                        IPPortCombo combo = new IPPortCombo(host, port);
                        baos.write(combo.toBytes());
                        numWritten++;
                    }
                    catch (UnknownHostException bad) {
                    }
                    catch (IOException terrible) {
                        ErrorService.error(terrible);
                    }
                }

                try {
                    // add the PushProxies
                    if (numWritten > 0)
                        retGGEP.put(GGEP.GGEP_HEADER_PUSH_PROXY,
                                    baos.toByteArray());
                    // set up return value
                    baos.reset();
                    retGGEP.write(baos);
                    retGGEPBlock = baos.toByteArray();
                }
                catch (IOException terrible) {
                    ErrorService.error(terrible);
                }

            }
            // else if (supportsBH && supportsFWTransfer &&
            // isMulticastResponse), since supportsFWTransfer is only helpful
            // if we have proxies
            else if (supportsBH && isMulticastResponse)
                retGGEPBlock = _comboGGEP;
            else if (supportsBH)
                retGGEPBlock = _bhGGEP;
            else if (isMulticastResponse)
                retGGEPBlock = _mcGGEP;
            return retGGEPBlock;
        }


        /** @return whether or not browse host support can be inferred from this
         * block of GGEPs.
         */
        public boolean allowsBrowseHost(GGEP[] ggeps) {
            boolean retBool = false;
            for (int i = 0; 
                 (ggeps != null) && (i < ggeps.length) && !retBool; 
                 i++) {
                Set headers = ggeps[i].getHeaders();
                retBool = headers.contains(GGEP.GGEP_HEADER_BROWSE_HOST);
            }
            return retBool;
        }

        /** @return whether or not FW Transfer support can be inferred from this
         *  block of GGEPs.
         */
        public boolean allowsFWTransfer(GGEP[] ggeps) {
            boolean retBool = false;
            for (int i = 0; 
                 (ggeps != null) && (i < ggeps.length) && !retBool; 
                 i++) {
                Set headers = ggeps[i].getHeaders();
                retBool = headers.contains(GGEP.GGEP_HEADER_FW_TRANS);
            }
            return retBool;
        }

        /** @return the version of FW Transfer supported by the host.  0
         *  if no support, else 1 or greater.
         */
        public byte getFWTransferVersion(GGEP[] ggeps) {
            byte retVersion = 0;
            for (int i = 0; 
                 (ggeps != null) && (i < ggeps.length); 
                 i++) {
                Set headers = ggeps[i].getHeaders();
                if (headers.contains(GGEP.GGEP_HEADER_FW_TRANS)) {
                    try {
                    byte[] bytes = ggeps[i].getBytes(GGEP.GGEP_HEADER_FW_TRANS);
                    if (bytes != null) {
                        retVersion = bytes[0];
                        break;
                    }
                    }
                    catch (BadGGEPPropertyException ignored) {}
                }
            }
            return retVersion;
        }

        /** @return whether or not it can be inferred that this reply is in
            response to a multicast query.
         */
        public boolean replyToMulticastQuery(GGEP[] ggeps) {
            boolean retBool = false;
            for (int i = 0; 
                 (ggeps != null) && (i < ggeps.length) && !retBool; 
                 i++) {
                Set headers = ggeps[i].getHeaders();
                retBool = headers.contains(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
            }
            return retBool;
        }

        
        /** @return a <tt>Set</tt> of <tt>PushProxyContainer</tt> instances,
         *  which can be empty but is guaranteed not to be <tt>null</tt>, as 
         *  described by the GGEP blocks.
         *
         * @param ggeps the array of GGEP extensions that may or may not
         *  contain push proxy data
         */
        public Set getPushProxies(GGEP[] ggeps) {
            Set proxies = new HashSet();
            
            for (int i = 0; (ggeps != null) && (i < ggeps.length); i++) {
                Set headers = ggeps[i].getHeaders();
                // if the block has a PUSH_PROXY value, get it, parse it,
                // and move to the next
                if (headers.contains(GGEP.GGEP_HEADER_PUSH_PROXY)) {
                    byte[] proxyBytes = null;
                    try {
                        proxyBytes = 
                            ggeps[i].getBytes(GGEP.GGEP_HEADER_PUSH_PROXY);
                    }
                    catch (BadGGEPPropertyException bad) {
                        // just ignore for now - we can't even get the vendor
                        // since this is a static method, will figure out
                        // something later
                        // TODO: add meaningful logging
                        continue;
                    }

                    ByteArrayInputStream bais = 
                        new ByteArrayInputStream(proxyBytes);
                    while (bais.available() > 0) {
                        byte[] combo = new byte[6];
                        if (bais.read(combo, 0, combo.length) == 
                            combo.length) {
                            try {
                                proxies.add(new PushProxyContainer(combo));
                            } catch (BadPacketException malformedPair) {}
                        }                        
                    }
                }
            }
            return proxies;
        }
    }

    /** A simple utility class for doling out PushProxy information.
     */
    public static class PushProxyContainer implements PushProxyInterface {
        IPPortCombo _combo;

        public PushProxyContainer(String hostAddress, int port) 
            throws UnknownHostException {
            _combo = new IPPortCombo(hostAddress, port);
        }

        public PushProxyContainer(byte[] fromNetwork)
            throws BadPacketException {
            _combo = IPPortCombo.getCombo(fromNetwork);
        }

        public int getPushProxyPort() {
            return _combo.getPort();
        }
        public InetAddress getPushProxyAddress() {
            return _combo.getInetAddress();
        }

        public boolean equals(Object other) {
            if(this == other) return true;
            if (other instanceof PushProxyContainer) {
                PushProxyContainer iface = (PushProxyContainer) other;
                return _combo.equals(iface._combo);
            }
            return false;
        }

        // overridden to fulfill contract with equals for hash-based
        // collections
        public int hashCode() {
            return _combo.hashCode() * 17;
        }
    }

    /** Another utility class the encapsulates some complexity.
     *  Keep in mind that I very well could have used Endpoint here, but I
     *  decided against it mainly so I could do validity checking.
     *  This may be a bad decision.  I'm sure someone will let me know during
     *  code review.
     */
    public static class IPPortCombo implements IpPort {
        private int _port;
        private InetAddress _addr;
        
        public static final String DELIM = ":";

        /**
         * Used for reading data from the network.  Throws BadPacketException
         * if the data is invalid.
         * @param fromNetwork 6 bytes - first 4 are IP, next 2 are port
         */
        public static IPPortCombo getCombo(byte[] fromNetwork)
          throws BadPacketException {
            return new IPPortCombo(fromNetwork);
        }
        
        /**
         * Constructor used for data read from the network.
         * Throws BadPacketException on errors.
         */
        private IPPortCombo(byte[] networkData) throws BadPacketException {
            if (networkData.length != 6)
                throw new BadPacketException("Weird Input");

            String host = NetworkUtils.ip2string(networkData, 0);
            int port = ByteOrder.ubytes2int(ByteOrder.leb2short(networkData, 4));
            if (!NetworkUtils.isValidPort(port))
                throw new BadPacketException("Bad Port: " + port);
            _port = port;
            try {
                _addr = InetAddress.getByName(host);
            } catch(UnknownHostException uhe) {
                throw new BadPacketException("bad host.");
            }
            if (!NetworkUtils.isValidAddress(_addr))
                throw new BadPacketException("invalid addr: " + _addr);
        }

        /**
         * Constructor used for local data.
         * Throws IllegalArgumentException on errors.
         */
        public IPPortCombo(String hostAddress, int port) 
            throws UnknownHostException, IllegalArgumentException  {
            if (!NetworkUtils.isValidPort(port))
                throw new IllegalArgumentException("Bad Port: " + port);
            _port = port;
            _addr = InetAddress.getByName(hostAddress);
            if (!NetworkUtils.isValidAddress(_addr))
                throw new IllegalArgumentException("invalid addr: " + _addr);
        }

        // Implements IpPort interface
        public int getPort() {
            return _port;
        }
        
        // Implements IpPort interface
        public InetAddress getInetAddress() {
            return _addr;
        }

        // Implements IpPort interface
        public String getAddress() {
            return _addr.getHostAddress();
        }

        /** @return the ip and port encoded in 6 bytes (4 ip, 2 port).
         *  //TODO if IPv6 kicks in, this may fail, don't worry so much now.
         */
        public byte[] toBytes() {
            byte[] retVal = new byte[6];
            
            for (int i=0; i < 4; i++)
                retVal[i] = _addr.getAddress()[i];

            ByteOrder.short2leb((short)_port, retVal, 4);

            return retVal;
        }

        public boolean equals(Object other) {
            if (other instanceof IPPortCombo) {
                IPPortCombo combo = (IPPortCombo) other;
                return _addr.equals(combo._addr) && (_port == combo._port);
            }
            return false;
        }

        // overridden to fulfill contract with equals for hash-based
        // collections
        public int hashCode() {
            return _addr.hashCode() * _port;
        }

    }
} //end QueryReply
