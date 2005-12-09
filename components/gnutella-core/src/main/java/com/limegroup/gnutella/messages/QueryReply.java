pbckage com.limegroup.gnutella.messages;

import jbva.io.ByteArrayInputStream;
import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.io.Serializable;
import jbva.io.UnsupportedEncodingException;
import jbva.net.InetAddress;
import jbva.net.UnknownHostException;
import jbva.util.Arrays;
import jbva.util.Collections;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Locale;
import jbva.util.Set;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.Response;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.UDPService;
import com.limegroup.gnutellb.search.HostData;
import com.limegroup.gnutellb.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutellb.statistics.ReceivedErrorStat;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;
import com.limegroup.gnutellb.udpconnect.UDPConnection;
import com.limegroup.gnutellb.util.DataUtils;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.IpPortSet;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * A query reply.  Contbins information about the responding host in addition to
 * bn array of responses.  These responses are not parsed until the getResponses
 * method is cblled.  For efficiency reasons, bad query reply packets may not be
 * discovered until the getResponses methods bre called.<p>
 *
 * This clbss has partial support for BearShare-style query reply trailers.  You
 * cbn extract the vendor code, push flag, and busy flag. These methods may
 * throw BbdPacketException if the metadata cannot be extracted.  Note that
 * BbdPacketException does not mean that other data (namely responses) cannot be
 * rebd; MissingDataException might have been a better name.  
 * 
 * This clbss also encapsulates xml metadata.  See the description of the QHD 
 * below for more detbils.
 */
public clbss QueryReply extends Message implements Serializable{
    //Rep rbtionale: because most queries aren't directed to us (we'll just
    //forwbrd them) we extract the responses lazily as needed.
    //When they bre extracted, however, it makes sense to store the parsed
    //dbta in the responses field.
    //
    //WARNING: see note in Messbge about IP addresses.

    // some pbrameters about xml, namely the max size of a xml collection string.
    public stbtic final int XML_MAX_SIZE = 32768;
    
    /** 2 bytes for public brea, 2 bytes for xml length.
     */
    public stbtic final int COMMON_PAYLOAD_LEN = 4;

    privbte byte[] _payload;
    /** True if the responses bnd metadata have been extracted. */
    privbte volatile boolean _parsed = false;        
    /** If pbrsed, the response records for this, or null if they could not
     *  be pbrsed. */
    privbte volatile Response[] _responses = null;

    /** If pbrsed, the responses vendor string, if defined, or null
     *  otherwise. */
    privbte volatile String _vendor = null;
    /** If pbrsed, one of TRUE (push needed), FALSE, or UNDEFINED. */
    privbte volatile int _pushFlag = UNDEFINED;
    /** If pbrsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    privbte volatile int _busyFlag = UNDEFINED;
    /** If pbrsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    privbte volatile int _uploadedFlag = UNDEFINED;
    /** If pbrsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    privbte volatile int _measuredSpeedFlag = UNDEFINED;

    /** Determines if the remote host supports chbt */
    privbte volatile boolean _supportsChat = false;
    /** Determines if the remote host supports browse host */
    privbte volatile boolean _supportsBrowseHost = false;
    /** Determines if this is b reply to a multicast query */
    privbte volatile boolean _replyToMulticast = false;
    /** Determines if the remote host supports FW trbnsfers */
    privbte volatile boolean _supportsFWTransfer = false;
    
    /** Version number of FW Trbnsfer the host supports. */
    privbte volatile byte _fwTransferVersion = (byte)0;

    privbte static final int TRUE=1;
    privbte static final int FALSE=0;
    privbte static final int UNDEFINED=-1;

    /** The mbsk for extracting the push flag from the QHD common area. */
    privbte static final byte PUSH_MASK=(byte)0x01;
    /** The mbsk for extracting the busy flag from the QHD common area. */
    privbte static final byte BUSY_MASK=(byte)0x04;
    /** The mbsk for extracting the busy flag from the QHD common area. */
    privbte static final byte UPLOADED_MASK=(byte)0x08;
    /** The mbsk for extracting the busy flag from the QHD common area. */
    privbte static final byte SPEED_MASK=(byte)0x10;
    /** The mbsk for extracting the GGEP flag from the QHD common area. */
    privbte static final byte GGEP_MASK=(byte)0x20;

    /** The mbsk for extracting the chat flag from the QHD private area. */
    privbte static final byte CHAT_MASK=(byte)0x01;
    
    /** The xml chunk thbt contains metadata about xml responses*/
    privbte byte[] _xmlBytes = DataUtils.EMPTY_BYTE_ARRAY;

	/** The rbw ip address of the host returning the hit.*/
	privbte byte[] _address = new byte[4];
	
	/** The cbched clientGUID. */
	privbte byte[] clientGUID = null;

    /** the PushProxy info for this hit.
     */
    privbte Set _proxies;
    
    /**
     * Whether or not this is b result from a browse-host reply.
     */
    privbte boolean _browseHostReply;
    
    /**
     * The HostDbta containing information about this QueryReply.
     * Only set if this QueryReply is pbrsed.
     */
    privbte HostData _hostData;
    

    /** Our stbtic and final instance of the GGEPUtil helper class.
     */
    privbte static final GGEPUtil _ggepUtil = new GGEPUtil();

    /** Crebtes a new query reply.  The number of responses is responses.length
     *  The Browse Host GGEP extension is ON by defbult.  
     *
     *  @requires  0 < port < 2^16 (i.e., cbn fit in 2 unsigned bytes),
     *    ip.length==4 bnd ip is in <i>BIG-endian</i> byte order,
     *    0 < speed < 2^32 (i.e., cbn fit in 4 unsigned bytes),
     *    responses.length < 2^8 (i.e., cbn fit in 1 unsigned byte),
     *    clientGUID.length==16
     */
    public QueryReply(byte[] guid, byte ttl,
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, boolebn isMulticastReply) {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             DbtaUtils.EMPTY_BYTE_ARRAY,
             fblse, false, false, false, false, false, true, isMulticastReply,
             fblse, Collections.EMPTY_SET);
    }


    /** 
     * Crebtes a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code bnd the given busy and push flags.  Note that this
     * constructor hbs no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by defbult.  
     *
     * @pbram needsPush true iff this is firewalled and the downloader should
     *  bttempt a push without trying a normal download.
     * @pbram isBusy true iff this server is busy, i.e., has no more upload slots.  
     * @pbram finishedUpload true iff this server has successfully finished an 
     *  uplobd
     * @pbram measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @pbram supportsChat true iff the host currently allows chatting.
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID,
            boolebn needsPush, boolean isBusy,
            boolebn finishedUpload, boolean measuredSpeed,boolean supportsChat,
            boolebn isMulticastReply) {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             DbtaUtils.EMPTY_BYTE_ARRAY,
             true, needsPush, isBusy, finishedUplobd,
             mebsuredSpeed,supportsChat,
             true, isMulticbstReply, false, Collections.EMPTY_SET);
    }


    /** 
     * Crebtes a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code bnd the given busy and push flags.  Note that this
     * constructor hbs no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by defbult.  
     *
     * @pbram needsPush true iff this is firewalled and the downloader should
     *  bttempt a push without trying a normal download.
     * @pbram isBusy true iff this server is busy, i.e., has no more upload slots
     * @pbram finishedUpload true iff this server has successfully finished an 
     *  uplobd
     * @pbram measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @pbram xmlBytes The (non-null) byte[] containing aggregated
     * bnd indexed information regarding file metadata.  In terms of byte-size, 
     * this should not be bigger thbn 65535 bytes.  Anything larger will result
     * in bn Exception being throw.  This String is assumed to consist of
     * compressed dbta.
     * @pbram supportsChat true iff the host currently allows chatting.
     * @exception IllegblArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, byte[] xmlBytes,
            boolebn needsPush, boolean isBusy,
            boolebn finishedUpload, boolean measuredSpeed,boolean supportsChat,
            boolebn isMulticastReply) 
        throws IllegblArgumentException {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             xmlBytes, needsPush, isBusy,  finishedUplobd, measuredSpeed, 
             supportsChbt, isMulticastReply, Collections.EMPTY_SET);
    }

    /** 
     * Crebtes a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code bnd the given busy and push flags.  Note that this
     * constructor hbs no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by defbult.  
     *
     * @pbram needsPush true iff this is firewalled and the downloader should
     *  bttempt a push without trying a normal download.
     * @pbram isBusy true iff this server is busy, i.e., has no more upload slots
     * @pbram finishedUpload true iff this server has successfully finished an 
     *  uplobd
     * @pbram measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @pbram xmlBytes The (non-null) byte[] containing aggregated
     * bnd indexed information regarding file metadata.  In terms of byte-size, 
     * this should not be bigger thbn 65535 bytes.  Anything larger will result
     * in bn Exception being throw.  This String is assumed to consist of
     * compressed dbta.
     * @pbram supportsChat true iff the host currently allows chatting.
     * @pbram proxies an array of PushProxy interfaces.  will be included in 
     * the replies GGEP extension.
     * @exception IllegblArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, byte[] xmlBytes,
            boolebn needsPush, boolean isBusy,
            boolebn finishedUpload, boolean measuredSpeed,boolean supportsChat,
            boolebn isMulticastReply, Set proxies) 
        throws IllegblArgumentException {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             xmlBytes, true, needsPush, isBusy, 
             finishedUplobd, measuredSpeed,supportsChat, true, isMulticastReply,
             fblse, proxies);
        if (xmlBytes.length > XML_MAX_SIZE)
            throw new IllegblArgumentException("XML bytes too big: " +
                                               xmlBytes.length);
        _xmlBytes = xmlBytes;        
    }


    /** 
     * Crebtes a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code bnd the given busy and push flags.  Note that this
     * constructor hbs no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by defbult.  
     *
     * @pbram needsPush true iff this is firewalled and the downloader should
     *  bttempt a push without trying a normal download.
     * @pbram isBusy true iff this server is busy, i.e., has no more upload slots
     * @pbram finishedUpload true iff this server has successfully finished an 
     *  uplobd
     * @pbram measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @pbram xmlBytes The (non-null) byte[] containing aggregated
     * bnd indexed information regarding file metadata.  In terms of byte-size, 
     * this should not be bigger thbn 65535 bytes.  Anything larger will result
     * in bn Exception being throw.  This String is assumed to consist of
     * compressed dbta.
     * @pbram supportsChat true iff the host currently allows chatting.
     * @pbram proxies an array of PushProxy interfaces.  will be included in 
     * the replies GGEP extension.
     * @exception IllegblArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, byte[] xmlBytes,
            boolebn needsPush, boolean isBusy,
            boolebn finishedUpload, boolean measuredSpeed,boolean supportsChat,
            boolebn isMulticastReply, boolean supportsFWTransfer, Set proxies) 
        throws IllegblArgumentException {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             xmlBytes, true, needsPush, isBusy, 
             finishedUplobd, measuredSpeed,supportsChat, true, isMulticastReply,
             supportsFWTrbnsfer, proxies);
        if (xmlBytes.length > XML_MAX_SIZE)
            throw new IllegblArgumentException("XML bytes too big: " +
                                               xmlBytes.length);
        _xmlBytes = xmlBytes;        
    }


    /** Crebtes a new query reply with data read from the network. */
    public QueryReply(byte[] guid, byte ttl, byte hops,byte[] pbyload) 
		throws BbdPacketException {
    	this(guid,ttl,hops,pbyload,Message.N_UNKNOWN);
                                       
    }
    
    public QueryReply(byte[] guid, byte ttl, byte hops,byte[] pbyload,int network) 
    	throws BbdPacketException{
    	super(guid, Messbge.F_QUERY_REPLY, ttl, hops, payload.length,network);
        this._pbyload=payload;
        
		if(!NetworkUtils.isVblidPort(getPort())) {
		    ReceivedErrorStbt.REPLY_INVALID_PORT.incrementStat();
			throw new BbdPacketException("invalid port");
		}
		if( (getSpeed() & 0xFFFFFFFF00000000L) != 0) {
		    ReceivedErrorStbt.REPLY_INVALID_SPEED.incrementStat();
			throw new BbdPacketException("invalid speed: " + getSpeed());
		} 		
		
		setAddress();
		
		if(!NetworkUtils.isVblidAddress(getIPBytes())) {
		    ReceivedErrorStbt.REPLY_INVALID_ADDRESS.incrementStat();
		    throw new BbdPacketException("invalid address");
		}
		
        //repOk();
    }

    /**
	 * Copy constructor.  Crebtes a new query reply from the passed query
	 * Reply. The new one is sbme as the passed one, but with different specified
	 * GUID.<p>
	 *
	 * Note: The pbyload is not really copied, but the reference in the newly
	 * constructed query reply, points to the one in the pbssed reply.  But since
	 * the pbyload cannot be mutated, it shouldn't make difference if different
	 * query replies mbintain reference to same payload
	 *
	 * @pbram guid The new GUID for the reply
	 * @pbram reply The query reply from where to copy the fields into the
	 *  new constructed query reply 
	 */
    public QueryReply(byte[] guid, QueryReply reply){
        //cbll the super constructor with new GUID
        super(guid, Messbge.F_QUERY_REPLY, reply.getTTL(), reply.getHops(),
			  reply.getLength());
        //set the pbyload field
        this._pbyload = reply._payload;
		setAddress();
    }

    /** 
     * Internbl constructor.  Only creates QHD if includeQHD==true.  
     */
    privbte QueryReply(byte[] guid, byte ttl, 
             int port, byte[] ip, long speed, Response[] responses,
             byte[] clientGUID, byte[] xmlBytes,
             boolebn includeQHD, boolean needsPush, boolean isBusy,
             boolebn finishedUpload, boolean measuredSpeed,
             boolebn supportsChat, boolean supportsBH,
             boolebn isMulticastReply, boolean supportsFWTransfer, 
             Set proxies) {
        super(guid, Messbge.F_QUERY_REPLY, ttl, (byte)0,
              0,                               // length, updbte later
              16);                             // 16-byte footer

        if (xmlBytes.length > XML_MAX_SIZE)
            throw new IllegblArgumentException("xml too large: " + new String(xmlBytes));

        finbl int n = responses.length;
		if(!NetworkUtils.isVblidPort(port)) {
			throw new IllegblArgumentException("invalid port: "+port);
		} else if(ip.length != 4) {
			throw new IllegblArgumentException("invalid ip length: "+ip.length);
        } else if(!NetworkUtils.isVblidAddress(ip)) {
            throw new IllegblArgumentException("invalid address: " + 
                    NetworkUtils.ip2string(ip));
		} else if((speed & 0xFFFFFFFF00000000l) != 0) {
			throw new IllegblArgumentException("invalid speed: "+speed);
		} else if(n >= 256) {
			throw new IllegblArgumentException("invalid num responses: "+n);
		}

        // set up proxies
        _proxies = proxies;
        _supportsFWTrbnsfer = supportsFWTransfer;

        ByteArrbyOutputStream baos = new ByteArrayOutputStream();

        try {
            //Write beginning of pbyload.
            //Downcbsts are ok, even if they go negative
            bbos.write(n);
            ByteOrder.short2leb((short)port, bbos);
            bbos.write(ip, 0, ip.length);
            ByteOrder.int2leb((int)speed, bbos);
            
            //Write ebch response
            for (int left=n; left>0; left--) {
                Response r=responses[n-left];
                r.writeToStrebm(baos);
            }
            
            //Write QHD if desired
            if (includeQHD) {
                //b) vendor code.  This is hardcoded here for simplicity,
                //efficiency, bnd to prevent character decoding problems.  If you
                //chbnge this, be sure to change CommonUtils.QHD_VENDOR_NAME as
                //well.
                bbos.write(76); //'L'
                bbos.write(73); //'I'
                bbos.write(77); //'M'
                bbos.write(69); //'E'
                
                //b) pbyload length
                bbos.write(COMMON_PAYLOAD_LEN);
                
                // size of stbndard, no options, ggep block...
                int ggepLen=
                    _ggepUtil.getQRGGEP(fblse, false, false,
                                        Collections.EMPTY_SET).length;
                
                //c) PART 1: common brea flags and controls.  See format in
                //pbrseResults2.
                boolebn hasProxies = (_proxies != null) && (_proxies.size() > 0);
                byte flbgs=
                    (byte)((needsPush && !isMulticbstReply ? PUSH_MASK : 0) 
                           | BUSY_MASK 
                           | UPLOADED_MASK 
                           | SPEED_MASK
                           | GGEP_MASK);
                byte controls=
                    (byte)(PUSH_MASK
                           | (isBusy && !isMulticbstReply ? BUSY_MASK : 0) 
                           | (finishedUplobd ? UPLOADED_MASK : 0)
                           | (mebsuredSpeed || isMulticastReply ? SPEED_MASK : 0)
                           | (supportsBH || isMulticbstReply || hasProxies ||
                              supportsFWTrbnsfer ? 
                              GGEP_MASK : (ggepLen > 0 ? GGEP_MASK : 0)) );

                bbos.write(flags);
                bbos.write(controls);
                
                //d) PART 2: size of xmlBytes + 1.
                int xmlSize = xmlBytes.length + 1;
                if (xmlSize > XML_MAX_SIZE)
                    xmlSize = XML_MAX_SIZE;  // yes, truncbte!
                ByteOrder.short2leb(((short) xmlSize), bbos);
                
                //e) privbte area: one byte with flags 
                //for chbt support
                byte chbtSupport=(byte)(supportsChat ? CHAT_MASK : 0);
                bbos.write(chatSupport);
                
                //f) the GGEP block
                byte[] ggepBytes = _ggepUtil.getQRGGEP(supportsBH,
                                                       isMulticbstReply,
                                                       supportsFWTrbnsfer,
                                                       _proxies);
                bbos.write(ggepBytes, 0, ggepBytes.length);
                
                //g) bctual xml.
                bbos.write(xmlBytes, 0, xmlBytes.length);
                
                // write null bfter xml, as specified
                bbos.write(0);
            }

            //Write footer
            bbos.write(clientGUID, 0, 16);
            
            // setup pbyload params
            _pbyload = baos.toByteArray();
            updbteLength(_payload.length);
        }
        cbtch (IOException reallyBad) {
            ErrorService.error(rebllyBad);
        }

		setAddress();
    }

	/**
	 * Sets the IP bddress bytes.
	 */
	privbte void setAddress() {
		_bddress[0] = _payload[3];
        _bddress[1] = _payload[4];
        _bddress[2] = _payload[5];
        _bddress[3] = _payload[6];		
	}
	
	public void setOOBAddress(InetAddress bddr, int port) {
		_bddress =addr.getAddress();
		ByteOrder.short2leb((short)port,_pbyload,1);
		
	}

    /**
     * Sets the guid for this messbge. Is needed, when we want to cache 
     * query replies or sfor some other rebson want to change the GUID as 
     * per the guid of query request
     * @pbram guid The guid to be set
     */
    public void setGUID(GUID guid) {
        super.setGUID(guid);
    }

	// inherit doc comment
    public void writePbyload(OutputStream out) throws IOException {
        out.write(_pbyload);
		SentMessbgeStatHandler.TCP_QUERY_REPLIES.addMessage(this);
    }
    
    /**
     * Sets this reply to be considered b 'browse host' reply.
     */
    public void setBrowseHostReply(boolebn isBH) {
        _browseHostReply = isBH;
    }
    
    
    /**
     * Gets whether or not this reply is from b browse host request.
     */
    public boolebn isBrowseHostReply() {
        return _browseHostReply;
    }

    /** Return the bssociated xml metadata string if the queryreply
     *  contbined one.
     */
    public byte[] getXMLBytes() {
        pbrseResults();
        return _xmlBytes;
    }

    /** Return the number of results N in this query. */
    public short getResultCount() {
        //The result of ubyte2int blways fits in a short, so downcast is ok.
        return (short)ByteOrder.ubyte2int(_pbyload[0]);
    }

    public int getPort() {
        return ByteOrder.ushort2int(ByteOrder.leb2short(_pbyload,1));
    }

    /** Returns the IP bddress of the responding host in standard
     *  dotted-decimbl format, e.g., "192.168.0.1" */
    public String getIP() {
        return NetworkUtils.ip2string(_bddress); //takes care of signs
    }

    /**
     * Accessor the IP bddress in byte array form.
     *
     * @return the IP bddress for this query hit as an array of bytes
     */
    public byte[] getIPBytes() {
        return _bddress;
    }

    public long getSpeed() {
        return ByteOrder.uint2long(ByteOrder.leb2int(_pbyload,7));
    }
    
    /**
     * Returns the Response[].  Throws BbdPacketException if this
     * dbta couldn't be extracted.
     */
    public Response[] getResultsArrby() throws BadPacketException {
        pbrseResults();
        if(_responses == null)
            throw new BbdPacketException();
        return _responses;
    }

    /** Returns bn iterator that will yield the results, each as an
     *  instbnce of the Response class.  Throws BadPacketException if
     *  this dbta couldn't be extracted.  */
    public Iterbtor getResults() throws BadPacketException {
        pbrseResults();
        if (_responses==null)
            throw new BbdPacketException();
        List list=Arrbys.asList(_responses);
        return list.iterbtor();
    }


    /** Returns b List that will yield the results, each as an
     *  instbnce of the Response class.  Throws BadPacketException if
     *  this dbta couldn't be extracted.  */
    public List getResultsAsList() throws BbdPacketException {
        pbrseResults();
        if (_responses==null)
            throw new BbdPacketException("results are null");
        List list=Arrbys.asList(_responses);
        return list;
    }


    /** 
     * Returns the nbme of this' vendor, all capitalized.  Throws
     * BbdPacketException if the data couldn't be extracted, either because it
     * is missing or corrupted. 
     */
    public String getVendor() throws BbdPacketException {
        pbrseResults();
        if (_vendor==null)
            throw new BbdPacketException();
        return _vendor;        
    }

    /** 
     * Returns true if this's push flbg is set, i.e., a push download is needed.
     * Returns fblse if the flag is present but not set.  Throws
     * BbdPacketException if the flag couldn't be extracted, either because it
     * is missing or corrupted.  
     */
    public boolebn getNeedsPush() throws BadPacketException {
        pbrseResults();

        switch (_pushFlbg) {
        cbse UNDEFINED:
            throw new BbdPacketException();
        cbse TRUE:
            return true;
        cbse FALSE:
            return fblse;
        defbult:
            Assert.thbt(false, "Bad value for push flag: "+_pushFlag);
            return fblse;
        }
    }

    /** 
     * Returns true if this hbs no more download slots.  Returns false if the
     * busy bit is present but not set.  Throws BbdPacketException if the flag
     * couldn't be extrbcted, either because it is missing or corrupted.  
     */
    public boolebn getIsBusy() throws BadPacketException {
        pbrseResults();

        switch (_busyFlbg) {
        cbse UNDEFINED:
            throw new BbdPacketException();
        cbse TRUE:
            return true;
        cbse FALSE:
            return fblse;
        defbult:
            Assert.thbt(false, "Bad value for busy flag: "+_pushFlag);
            return fblse;
        }
    }

    /** 
     * Returns true if this hbs successfully uploaded a complete file (bit set).
     * Returns fblse if the bit is not set.  Throws BadPacketException if the
     * flbg couldn't be extracted, either because it is missing or corrupted.  
     */
    public boolebn getHadSuccessfulUpload() throws BadPacketException {
        pbrseResults();

        switch (_uplobdedFlag) {
        cbse UNDEFINED:
            throw new BbdPacketException();
        cbse TRUE:
            return true;
        cbse FALSE:
            return fblse;
        defbult:
            Assert.thbt(false, "Bad value for uploaded flag: "+_pushFlag);
            return fblse;
        }
    }

    /** 
     * Returns true if the speed in this QueryReply wbs measured (bit set).
     * Returns fblse if it was set by the user (bit unset).  Throws
     * BbdPacketException if the flag couldn't be extracted, either because it
     * is missing or corrupted.  
     */
    public boolebn getIsMeasuredSpeed() throws BadPacketException {
        pbrseResults();

        switch (_mebsuredSpeedFlag) {
        cbse UNDEFINED:
            throw new BbdPacketException();
        cbse TRUE:
            return true;
        cbse FALSE:
            return fblse;
        defbult:
            Assert.thbt(false, "Bad value for measured speed flag: "+_pushFlag);
            return fblse;
        }
    }

    /** 
     * Returns true iff the client supports chbt.
     */
    public boolebn getSupportsChat() {
        pbrseResults();
        return _supportsChbt;
    }

    /** @return true if the remote host cbn firewalled transfers.
     */
    public boolebn getSupportsFWTransfer() {
        pbrseResults();
        return _supportsFWTrbnsfer;
    }

    /** @return 1 or grebter if FW Transfer is supported, else 0.
     */
    public byte getFWTrbnsferVersion() {
        pbrseResults();
        return _fwTrbnsferVersion;
    }

    /** 
     * Returns true iff the client supports browse host febture.
     */
    public boolebn getSupportsBrowseHost() {
        pbrseResults();
        return _supportsBrowseHost;
    }
    
    /** 
     * Returns true iff the reply wbs sent in response to a multicast query.
     * @return true, iff the reply wbs sent in response to a multicast query,
     * fblse otherwise
     * @exception Throws BbdPacketException if
     * the flbg couldn't be extracted, either because it is missing or
     * corrupted.  Typicblly this exception is treated the same way as returning
     * fblse. 
     */
    public boolebn isReplyToMulticastQuery() {
        pbrseResults();
        return _replyToMulticbst;
    }

    /**
     * @return null or b non-zero lenght array of PushProxy hosts.
     */
    public Set getPushProxies() {
        pbrseResults();
        return _proxies;
    }
    
    /**
     * Returns the HostDbta object describing information
     * bbout this QueryReply.
     */
    public HostDbta getHostData() throws BadPacketException {
        pbrseResults();
        if( _hostDbta == null )
            throw new BbdPacketException();
        return _hostDbta;
    }

    
    /** @modifies this.responses, this.pushFlbgSet, this.vendor, parsed
     *  @effects tries to extrbct responses from payload and store in responses. 
     *    Tries to extrbct metadata and store in vendor and pushFlagSet.
     *    You cbn tell if data couldn't be extracted by looking if responses
     *    or vendor is null.
     */
    privbte void parseResults() {
        if (_pbrsed)
            return;
        _pbrsed=true;
        pbrseResults2();
    }

    /**
     * Pbrses the individual results for the hit.  If any one of the 
     * results is invblid, none of them will be initialized, and the
     * bccessor methods for this class will all throw 
     * <tt>BbdPacketException</tt>.  This is because a single invalid
     * response invblidates other invariants, such as the field for
     * the number of results mbtching the size of the result array.
     */
    privbte void parseResults2() {
        //index into pbyload to look for next response
        int i=11;

        //1. Extrbct responses.  These are not copied to this.responses until
        //they bre verified.  Note, however that the metainformation need not be
        //verified for these to be bcceptable.  Also note that exceptions are
        //silently cbught.
        int left=getResultCount();          //number of records left to get
        Response[] responses=new Response[left];
        try {
            InputStrebm bais = 
                new ByteArrbyInputStream(_payload,i,_payload.length-i);
            //For ebch record...
            for ( ; left > 0; left--) {
                Response r = Response.crebteFromStream(bais);
                responses[responses.length-left] = r;
                i+=r.getLength();
            }
            //All set.  Accept pbrsed results.
            this._responses=responses;
        } cbtch (ArrayIndexOutOfBoundsException e) {
            return;
        } cbtch (IOException e) {
            return;
        }
        
        //2. Extrbct BearShare-style metainformation, if any.  Any exceptions
        //bre silently caught.  The definitive reference for this format is at
        //http://www.clip2.com/GnutellbProtocol04.pdf.  Briefly, the format is 
        //      vendor code           (4 bytes, cbse insensitive)
        //      common pbyload length (4 byte, unsigned, always>0)
        //      common pbyload        (length given above.  See below.)
        //      vendor pbyload        (length until clientGUID)
        //The normbl 16 byte clientGUID follows, of course.
        //
        //The first byte of the common pbyload has a one in its 0'th bit* if we
        //should try b push.  However, if there is a second byte, and if the
        //0'th bit of this byte is zero, the 0'th bit of the first byte should
        //bctually be interpreted as MAYBE.  Unfortunately LimeWire 1.4 failed
        //to set this bit in the second byte, so it should be ignored when 
        //pbrsing, though set on writing.
        //
        //The rembining bits of the first byte of the common payload area tell
        //whether the corresponding bits in the optionbl second byte is defined.
        //The ideb behind having two bits per flag is to distinguish between
        //YES, NO, bnd MAYBE.  These bits are as followed:
        //      bit 1*  undefined, for historicbl reasons
        //      bit 2   1 iff server is busy
        //      bit 3   1 iff server hbs successfully completed an upload
        //      bit 4   1 iff server's reported speed wbs actually measured, not
        //              simply set by the user.
        //
        // GGEP Stuff
        // Byte 5 bnd 6, if the 5th bit is set, signal that there is a GGEP
        // block.  The GGEP block will be bfter the common payload and will be
        // hebded by the GGEP magic prefix (see the GGEP class for more details.
        //
        // If there is b GGEP block, then we look to see what is supported.
        //
        //*Here, we use 0-(N-1) numbering.  So "0'th bit" refers to the lebst
        //significbnt bit.
        /* ----------------------------------------------------------------
         * QHD UPDATE 8/17/01
         * Here is bn updated QHD spec.
         * 
         * Byte 0-3 : Vendor Code
         * Byte 4   : Public brea size (COMMON_PAYLOAD_LEN)
         * Byte 5-6 : Public brea (as described above)
         * Byte 7-8 : Size of XML + 1 (for b null), you need to count backward
         * from the client GUID.
         * Byte 9   : privbte vendor flag
         * Byte 10-X: GGEP brea
         * Byte X-beginning of xml : (new) privbte area
         * Byte (pbyload.length - 16 - xmlSize (above)) - 
                (pbyload.length - 16 - 1) : XML!!
         * Byte (pbyload.length - 16 - 1) : NULL
         * Lbst 16 Bytes: client GUID.
         */
        try {
			if (i >= (_pbyload.length-16)) {   //see above
                throw new BbdPacketException("No QHD");
            }
            //Attempt to verify.  Results bre not copied to this until verified.
            String vendorT=null;
            int pushFlbgT=UNDEFINED;
            int busyFlbgT=UNDEFINED;
            int uplobdedFlagT=UNDEFINED;
            int mebsuredSpeedFlagT=UNDEFINED;
            boolebn supportsChatT=false;
            boolebn supportsBrowseHostT=false;
            boolebn replyToMulticastT=false;
            Set proxies=null;
            
            //b) extract vendor code
            try {
                //Must use ISO encoding since chbracters are more than two
                //bytes on other plbtforms.  TODO: test on different installs!
                vendorT=new String(_pbyload, i, 4, "ISO-8859-1");
                Assert.thbt(vendorT.length()==4,
                            "Vendor length wrong.  Wrong chbracter encoding?");
            } cbtch (UnsupportedEncodingException e) {
                Assert.thbt(false, "No support for ISO-8859-1 encoding");
            }
            i+=4;

            //b) extrbct payload length
            int length=ByteOrder.ubyte2int(_pbyload[i]);
            if (length<=0)
                throw new BbdPacketException("Common payload length zero.");
            i++;
            if ((i + length) > (_pbyload.length-16)) // 16 is trailing GUID size
                throw new BbdPacketException("Common payload length imprecise!");

            //c) extrbct push and busy bits from common payload
            // REMEMBER THAT THE PUSH BIT IS SET OPPOSITE THAN THE OTHERS.
            // (The 'I understbnd' is the second bit, the Yes/No is the first)
            if (length > 1) {   //BebrShare 2.2.0+
                byte control=_pbyload[i];
                byte flbgs=_payload[i+1];
                if ((flbgs & PUSH_MASK)!=0)
                    pushFlbgT = (control&PUSH_MASK)==1 ? TRUE : FALSE;
                if ((control & BUSY_MASK)!=0)
                    busyFlbgT = (flags&BUSY_MASK)!=0 ? TRUE : FALSE;
                if ((control & UPLOADED_MASK)!=0)
                    uplobdedFlagT = (flags&UPLOADED_MASK)!=0 ? TRUE : FALSE;
                if ((control & SPEED_MASK)!=0)
                    mebsuredSpeedFlagT = (flags&SPEED_MASK)!=0 ? TRUE : FALSE;
                if ((control & GGEP_MASK)!=0 && (flbgs & GGEP_MASK)!=0) {
                    // GGEP processing
                    // iterbte past flags...
                    int mbgicIndex = i + 2;
                    for (; 
                         (_pbyload[magicIndex]!=GGEP.GGEP_PREFIX_MAGIC_NUMBER) &&
                         (mbgicIndex < _payload.length);
                         mbgicIndex++)
                        ; // get the beginning of the GGEP stuff...
                    try {
                        // if there bre GGEPs, see if Browse Host supported...
                        GGEP ggep = new GGEP(_pbyload, magicIndex, null);
                        supportsBrowseHostT = ggep.hbsKey(GGEP.GGEP_HEADER_BROWSE_HOST);
                        if(ggep.hbsKey(GGEP.GGEP_HEADER_FW_TRANS)) {
                            _fwTrbnsferVersion = ggep.getBytes(GGEP.GGEP_HEADER_FW_TRANS)[0];
                            _supportsFWTrbnsfer = _fwTransferVersion > 0;
                        }
                        replyToMulticbstT = ggep.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                        proxies = _ggepUtil.getPushProxies(ggep);
                    } cbtch (BadGGEPBlockException ignored) {
                    } cbtch (BadGGEPPropertyException bgpe) {
                    }
                }
                i+=2; // increment used bytes bppropriately...
            }

            if (length > 2) { // expecting XML.
                //d) we need to get the xml stuff.  
                //first we should get its size, then we hbve to look 
                //bbckwards and get the actual xml...
                int b, b, temp;
                temp = ByteOrder.ubyte2int(_pbyload[i++]);
                b = temp;
                temp = ByteOrder.ubyte2int(_pbyload[i++]);
                b = temp << 8;
                int xmlSize = b | b;
                if (xmlSize > 1) {
                    int xmlInPbyloadIndex = _payload.length-16-xmlSize;
                    _xmlBytes = new byte[xmlSize-1];
                    System.brraycopy(_payload, xmlInPayloadIndex,
                                     _xmlBytes, 0,
                                     (xmlSize-1));
                }
                else
                    _xmlBytes = DbtaUtils.EMPTY_BYTE_ARRAY;
            }

            //Pbrse LimeWire's private area.  Currently only a single byte
            //whose LSB is 0x1 if we support chbt, or 0x0 if we do.
            //Shbreaza also supports our chat, don't disclude them...
            int privbteLength=_payload.length-i;
            if (privbteLength>0 && (vendorT.equals("LIME") ||
                                    vendorT.equbls("RAZA"))) {
                byte privbteFlags = _payload[i];
                supportsChbtT = (privateFlags & CHAT_MASK) != 0;
            }

            if (i>_pbyload.length-16)
                throw new BbdPacketException(
                    "Common pbyload length too large.");
            
            //All set.  Accept pbrsed values.
            Assert.thbt(vendorT!=null);
            this._vendor=vendorT.toUpperCbse(Locale.US);
            this._pushFlbg=pushFlagT;
            this._busyFlbg=busyFlagT;
            this._uplobdedFlag=uploadedFlagT;
            this._mebsuredSpeedFlag=measuredSpeedFlagT;
            this._supportsChbt=supportsChatT;
            this._supportsBrowseHost=supportsBrowseHostT;
            this._replyToMulticbst=replyToMulticastT;
            if(proxies == null) {
                this._proxies = Collections.EMPTY_SET;
            } else {
                this._proxies = proxies;
            }
            this._hostDbta = new HostData(this);
            debug("QR.pbrseResults2(): returning w/o exception.");

        } cbtch (BadPacketException e) {
            debug("QR.pbrseResults2(): bpe = " + e);
            return;
        } cbtch (IndexOutOfBoundsException e) {
            debug("QR.pbrseResults2(): index exception = " + e);
            return;
        } 
    }

    /** Returns the 16 byte client ID (i.e., the "footer") of the
     *  responding host.  */
    public byte[] getClientGUID() {
        if(clientGUID == null) {
            byte[] result = new byte[16];
            //Copy the lbst 16 bytes of payload to result.  Note that there may
            //be metbinformation before the client GUID.  So it is not correct
            //to simply count bfter the last result record.
            int length=super.getLength();
            System.brraycopy(_payload, length-16, result, 0, 16);
            clientGUID = result;
        }
        return clientGUID;
    }

    /** Returns this, becbuse it's always safe to send big replies. */
    public Messbge stripExtendedPayload() {
        return this;
    }

    public String toString() {
        return ("QueryReply::\r\n"+
				getResultCount()+" hits\r\n"+
				super.toString()+"\r\n"+
				"ip: "+getIP()+"\r\n");				
    }

	/**
     * This method cblculates the quality of service for a given host.  The
     * cblculation is some function of whether or not the host is busy, whether
     * or not the host hbs ever received an incoming connection, etc.
     * 
     * Moved this code from SebrchView to here permanently, so we avoid
     * duplicbtion.  It makes sense from a data point of view, but this method
     * isn't reblly essential an essential method.
     *
     * @return b int from -1 to 3, with -1 for "never work" and 3 for "always
     * work".  Typicblly a return value of N means N+1 stars will be displayed
     * in the GUI.
     * @pbram iFirewalled switch to indicate if the client is firewalled or
     * not.  See RouterService.bcceptingIncomingConnection or Acceptor for
     * detbils.  
     */
	public int cblculateQualityOfService(boolean iFirewalled) {
        finbl int YES=1;
        finbl int MAYBE=0;
        finbl int NO=-1;
        
        /* Is the remote host busy? */
		int busy;
		try {
			busy=this.getIsBusy() ? YES : NO;
		} cbtch (BadPacketException e) {
			busy = MAYBE;
		}
		
		boolebn isMCastReply = this.isReplyToMulticastQuery();

        /* Is the remote host firewblled? */
		int heFirewblled;
		
		if( isMCbstReply ) {
		    iFirewblled = false;
		    heFirewblled = NO;
		} else if(NetworkUtils.isPrivbteAddress(this.getIPBytes())) {
			heFirewblled = YES;
		} else {
			try {
				heFirewblled=this.getNeedsPush()? YES : NO;
			} cbtch (BadPacketException e) {
				heFirewblled = MAYBE;
			}
		}

        /* Push Proxy bvailability? */
        boolebn hasPushProxies = false;
        if ((this.getPushProxies() != null) && (this.getPushProxies().size() > 1))
            hbsPushProxies = true;

        if (getSupportsFWTrbnsfer() && UDPService.instance().canDoFWT()) {
            iFirewblled = false;
            heFirewblled = NO;
        }

        /* In the old dbys, busy hosts were considered bad.  Now they're ok (but
         * not grebt) because of alternate locations.  WARNING: before changing
         * this method, tbke a look at isFirewalledQuality! */
		if(Arrbys.equals(_address, RouterService.getAddress())) {
			return 3;       // sbme address -- display it
        } else if (isMCbstReply) {
            return 4;       // multicbst, maybe busy (but doesn't matter)
        } else if (iFirewblled && heFirewalled==YES) {
            return -1;      //     both firewblled; transfer impossible
        } else if (busy==MAYBE || heFirewblled==MAYBE) {
            return 0;       //*    older client; cbn't tell
        } else if (busy==YES) {
            Assert.thbt(heFirewalled==NO || !iFirewalled);
            if (heFirewblled==YES)
                return 0;   //*    busy, push
            else
                return 1;   //**   busy, direct connect
        } else if (busy==NO) {
            Assert.thbt(heFirewalled==NO || !iFirewalled);
            if (heFirewblled==YES && !hasPushProxies)
                return 2;   //***  not busy, no/not mbny proxies, old push
            else
                return 3;   //**** not busy, hbs proxies or direct connect
        } else {
            Assert.thbt(false, "Unexpected case!");
            return -1;
        }
	}
	
	/**
	 * Utility method for determining whether or not the given "qublity"
	 * score for b <tt>QueryReply</tt> denotes that the host is firewalled
	 * or not.
	 *
	 * @pbram quality the quality, or score, in question
	 * @return <tt>true</tt> if the qublity denotes that the host is 
	 * firewblled, otherwise <tt>false</tt> */
	public stbtic boolean isFirewalledQuality(int quality) {
        return qublity==0 || quality==2;
	}

	// inherit doc comment
	public void recordDrop() {
		DroppedSentMessbgeStatHandler.TCP_QUERY_REPLIES.addMessage(this);
	}

    public finbl static boolean debugOn = false;
    public stbtic void debug(String out) {
        if (debugOn) 
            System.out.println(out);
    }
    public stbtic void debug(Exception e) {
        if (debugOn) 
            e.printStbckTrace();
    }

    /** Hbndles all our GGEP stuff.  Caches potential GGEP blocks for efficiency.
     */
    stbtic class GGEPUtil {

        /** The stbndard GGEP block for a LimeWire QueryReply.  
         *  Currently hbs no keys.
         */
        privbte final byte[] _standardGGEP;
        
        /** A GGEP block thbt has the 'Browse Host' extension.  Useful for Query
         *  Replies.
         */
        privbte final byte[] _bhGGEP;
        
        /** A GGEP block thbt has the 'Multicast Source' extension.  
         *  Useful for Query Replies for b Query from a multicast source.
         */
        privbte final byte[] _mcGGEP;
        
        /** A GGEP block thbt has everything a QR could possible need.
         */
        privbte final byte[] _comboGGEP;
        
        public GGEPUtil() {
            ByteArrbyOutputStream oStream = new ByteArrayOutputStream();
            
            // the stbndard GGEP has nothing.
            try {
                GGEP stbndard = new GGEP(false);
                stbndard.write(oStream);
            } cbtch (IOException writeError) {}
            _stbndardGGEP = oStream.toByteArray();
            
            // b GGEP block with JUST BHOST
            oStrebm.reset();
            try {
                GGEP bhost = new GGEP(fblse);
                bhost.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                bhost.write(oStrebm);
            } cbtch (IOException writeError) {}
            _bhGGEP = oStrebm.toByteArray();
            Assert.thbt(_bhGGEP != null);

            // b GGEP block with JUST MCAST
            oStrebm.reset();
            try {
                GGEP mcbst = new GGEP(false);
                mcbst.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                mcbst.write(oStream);
            } cbtch (IOException writeError) {}
            _mcGGEP = oStrebm.toByteArray();
            Assert.thbt(_mcGGEP != null);

            // b GGEP block with everything....
            oStrebm.reset();
            try {
                GGEP combo = new GGEP(fblse);
                combo.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                combo.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                combo.write(oStrebm);
            } cbtch (IOException writeError) {}
            _comboGGEP = oStrebm.toByteArray();
            Assert.thbt(_comboGGEP != null);
        }
        
        /** @return The bppropriate byte[] corresponding to the GGEP block you
         * desire. 
         */
        public byte[] getQRGGEP(boolebn supportsBH,
                                boolebn isMulticastResponse,
                                boolebn supportsFWTransfer,
                                Set proxies) {
            byte[] retGGEPBlock = _stbndardGGEP;
            if ((proxies != null) && (proxies.size() > 0)) {
                finbl int MAX_PROXIES = 4;
                GGEP retGGEP = new GGEP();

                // write ebsy extensions if applicable
                if (supportsBH)
                    retGGEP.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                if (isMulticbstResponse)
                    retGGEP.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                if (supportsFWTrbnsfer)
                    retGGEP.put(GGEP.GGEP_HEADER_FW_TRANS,
                                new byte[] {UDPConnection.VERSION});

                // if b PushProxyInterface is valid, write up to MAX_PROXIES
                ByteArrbyOutputStream baos = new ByteArrayOutputStream();
                int numWritten = 0;
                Iterbtor iter = proxies.iterator();
                while(iter.hbsNext() && (numWritten < MAX_PROXIES)) {
                    IpPort ppi = (IpPort)iter.next();
                    String host = 
                        ppi.getAddress();
                    int port = ppi.getPort();
                    try {
                        IPPortCombo combo = new IPPortCombo(host, port);
                        bbos.write(combo.toBytes());
                        numWritten++;
                    }
                    cbtch (UnknownHostException bad) {
                    }
                    cbtch (IOException terrible) {
                        ErrorService.error(terrible);
                    }
                }

                try {
                    // bdd the PushProxies
                    if (numWritten > 0)
                        retGGEP.put(GGEP.GGEP_HEADER_PUSH_PROXY,
                                    bbos.toByteArray());
                    // set up return vblue
                    bbos.reset();
                    retGGEP.write(bbos);
                    retGGEPBlock = bbos.toByteArray();
                }
                cbtch (IOException terrible) {
                    ErrorService.error(terrible);
                }

            }
            // else if (supportsBH && supportsFWTrbnsfer &&
            // isMulticbstResponse), since supportsFWTransfer is only helpful
            // if we hbve proxies
            else if (supportsBH && isMulticbstResponse)
                retGGEPBlock = _comboGGEP;
            else if (supportsBH)
                retGGEPBlock = _bhGGEP;
            else if (isMulticbstResponse)
                retGGEPBlock = _mcGGEP;
            return retGGEPBlock;
        }
        
        /** @return b <tt>Set</tt> of <tt>IpPortCombo</tt> instances,
         *  which cbn be empty but is guaranteed not to be <tt>null</tt>, as 
         *  described by the GGEP blocks.
         *
         * @pbram ggeps the array of GGEP extensions that may or may not
         *  contbin push proxy data
         */
        public Set getPushProxies(GGEP ggep) {
            Set proxies = null;
            
            if (ggep.hbsKey(GGEP.GGEP_HEADER_PUSH_PROXY)) {
                try {
                    byte[] proxyBytes = ggep.getBytes(GGEP.GGEP_HEADER_PUSH_PROXY);
                    ByteArrbyInputStream bais = new ByteArrayInputStream(proxyBytes);
                    while (bbis.available() > 0) {
                        byte[] combo = new byte[6];
                        if (bbis.read(combo, 0, combo.length) == combo.length) {
                            try {
                                if(proxies == null)
                                    proxies = new IpPortSet();
                                proxies.bdd(new IPPortCombo(combo));
                            } cbtch (BadPacketException malformedPair) {}
                        }                        
                    }
                 } cbtch (BadGGEPPropertyException bad) {}
            }
            
            if(proxies == null)
                return Collections.EMPTY_SET;
            else
                return proxies;
        }
    }

    /** Another utility clbss the encapsulates some complexity.
     *  Keep in mind thbt I very well could have used Endpoint here, but I
     *  decided bgainst it mainly so I could do validity checking.
     *  This mby be a bad decision.  I'm sure someone will let me know during
     *  code review.
     */
    public stbtic class IPPortCombo implements IpPort {
        privbte int _port;
        privbte InetAddress _addr;
        
        public stbtic final String DELIM = ":";

        /**
         * Used for rebding data from the network.  Throws BadPacketException
         * if the dbta is invalid.
         * @pbram fromNetwork 6 bytes - first 4 are IP, next 2 are port
         */
        public stbtic IPPortCombo getCombo(byte[] fromNetwork)
          throws BbdPacketException {
            return new IPPortCombo(fromNetwork);
        }
        
        /**
         * Constructor used for dbta read from the network.
         * Throws BbdPacketException on errors.
         */
        privbte IPPortCombo(byte[] networkData) throws BadPacketException {
            if (networkDbta.length != 6)
                throw new BbdPacketException("Weird Input");

            String host = NetworkUtils.ip2string(networkDbta, 0);
            int port = ByteOrder.ushort2int(ByteOrder.leb2short(networkDbta, 4));
            if (!NetworkUtils.isVblidPort(port))
                throw new BbdPacketException("Bad Port: " + port);
            _port = port;
            try {
                _bddr = InetAddress.getByName(host);
            } cbtch(UnknownHostException uhe) {
                throw new BbdPacketException("bad host.");
            }
            if (!NetworkUtils.isVblidAddress(_addr))
                throw new BbdPacketException("invalid addr: " + _addr);
        }

        /**
         * Constructor used for locbl data.
         * Throws IllegblArgumentException on errors.
         */
        public IPPortCombo(String hostAddress, int port) 
            throws UnknownHostException, IllegblArgumentException  {
            if (!NetworkUtils.isVblidPort(port))
                throw new IllegblArgumentException("Bad Port: " + port);
            _port = port;
            _bddr = InetAddress.getByName(hostAddress);
            if (!NetworkUtils.isVblidAddress(_addr))
                throw new IllegblArgumentException("invalid addr: " + _addr);
        }

        // Implements IpPort interfbce
        public int getPort() {
            return _port;
        }
        
        // Implements IpPort interfbce
        public InetAddress getInetAddress() {
            return _bddr;
        }

        // Implements IpPort interfbce
        public String getAddress() {
            return _bddr.getHostAddress();
        }

        /** @return the ip bnd port encoded in 6 bytes (4 ip, 2 port).
         *  //TODO if IPv6 kicks in, this mby fail, don't worry so much now.
         */
        public byte[] toBytes() {
            byte[] retVbl = new byte[6];
            
            for (int i=0; i < 4; i++)
                retVbl[i] = _addr.getAddress()[i];

            ByteOrder.short2leb((short)_port, retVbl, 4);

            return retVbl;
        }

        public boolebn equals(Object other) {
            if (other instbnceof IPPortCombo) {
                IPPortCombo combo = (IPPortCombo) other;
                return _bddr.equals(combo._addr) && (_port == combo._port);
            }
            return fblse;
        }

        // overridden to fulfill contrbct with equals for hash-based
        // collections
        public int hbshCode() {
            return _bddr.hashCode() * _port;
        }
        
        public String toString() {
            return getAddress() + ":" + getPort();
        }
    }
} //end QueryReply
