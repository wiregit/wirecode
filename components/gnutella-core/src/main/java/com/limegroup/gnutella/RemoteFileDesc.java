pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.io.ObjectInputStream;
import jbva.io.ObjectOutputStream;
import jbva.io.Serializable;
import jbva.net.InetAddress;
import jbva.net.InetSocketAddress;
import jbva.net.MalformedURLException;
import jbva.net.URL;
import jbva.net.UnknownHostException;
import jbva.util.Arrays;
import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Map;
import jbva.util.Set;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.altlocs.AlternateLocation;
import com.limegroup.gnutellb.downloader.URLRemoteFileDesc;
import com.limegroup.gnutellb.http.HTTPConstants;
import com.limegroup.gnutellb.util.DataUtils;
import com.limegroup.gnutellb.util.IntervalSet;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.IpPortImpl;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.xml.LimeXMLDocument;

/**
 * A reference to b single file on a remote machine.  In this respect
 * RemoteFileDesc is similbr to a URL, but it contains Gnutella-
 * specific dbta as well, such as the server's 16-byte GUID.<p>
 *
 * This clbss is serialized to disk as part of the downloads.dat file.  Hence
 * you must be very cbreful before making any changes.  Deleting or changing the
 * types of fields is DISALLOWED.  Adding field b F is acceptable as long as the
 * rebdObject() method of this initializes F to a reasonable value when
 * rebding from older files where the fields are not present.  This is exactly
 * whbt we do with _urns and _browseHostEnabled.  On the other hand, older
 * version of LimeWire will simply discbrd any extra fields F if reading from a
 * newer seriblized file.  
 */
public clbss RemoteFileDesc implements IpPort, Serializable, FileDetails {
    
    privbte static final Log LOG = LogFactory.getLog(RemoteFileDesc.class);
    
    privbte static final long serialVersionUID = 6619479308616716538L;
    
    privbte static final int COPY_INDEX = Integer.MAX_VALUE;

    /** bogus IP we bssign to RFDs whose real ip is unknown */
    public stbtic final String BOGUS_IP = "1.1.1.1";
    
	privbte final String _host;
	privbte final int _port;
	privbte final String _filename; 
	privbte final long _index;
	privbte final byte[] _clientGUID;
	privbte final int _speed;
	privbte final int _size;
	privbte final boolean _chatEnabled;
    privbte final int _quality;
    privbte final boolean _replyToMulticast;

    /**
     *  RemoteFileDesc cbn only be constructed with a single piece of metadata.
     *  However, historicblly RemoteFileDesc stored an array of metadata.  Hence
     *  we must be prepbred to read this data from a serialized downloads.dat
     *  file.  In other words, _xmlDocs is typicblly null or a single non-null
     *  element, unless this wbs deserialized from an older version.  
	 * 
	 *  INVARIANT: _xmlDocs != null -> _xmlDocs.length != 0
	 */
    privbte LimeXMLDocument[] _xmlDocs;
	privbte Set /* of URN*/  _urns;

    /**
     * Boolebn indicating whether or not the remote host has browse host 
     * enbbled.
     */
	privbte boolean _browseHostEnabled;

    privbte boolean _firewalled;
    privbte String _vendor;
    privbte long _timestamp;
    
    /**
     * Whether or not the remote host supports HTTP/1.1
     * This is purposely NOT IMMUTABLE.  Before we connect,
     * we cbn only assume the remote host supports HTTP/1.1 by
     * looking bt the set of URNs.  If any exist, we assume
     * HTTP/1.1 is supported (becbuse URNs were added to Gnutella
     * bfter HTTP/1.1).  Once we connect, this value is set to
     * be whbtever the host reports in the response line.
     *
     * When deseriblizing, this value may be wrong for older download.dat
     * files.  (Older versions will blways set this to false, because
     * the field did not exist.)  To counter thbt, when deserializing,
     * if this is fblse, we set it to true if any URNs are present.
     */
    privbte boolean _http11;
    
    /**
     * The <tt>PushEndpoint</tt> for this RFD.
     * if null, the rfd is not behind b push proxy.
     */
    privbte transient PushEndpoint _pushAddr;
		

    /**
     * The list of bvailable ranges.
     * This is NOT SERIALIZED.
     */
    privbte transient IntervalSet _availableRanges = null;
    
    /**
     * The lbst known queue status of the remote host
     * negbtive values mean free slots
     */
    privbte transient int _queueStatus = Integer.MAX_VALUE;
    
    /**
     * The number of times this downlobd has failed while attempting
     * to trbnsfer data.
     */
    privbte transient int _failedCount = 0;

    /**
     * The ebrliest time to retry this host in milliseconds since 01-01-1970
     */
    privbte transient volatile long _earliestRetryTime = 0;

    /**
     * The cbched hash code for this RFD.
     */
    privbte transient int _hashCode = 0;

    /**
     * Whether or not THEX retrievbl has failed with this host.
     */
    privbte transient boolean _THEXFailed = false;

    /**
     * The cbched RemoteHostData for this rfd.
     */
    privbte transient RemoteHostData _hostData = null;
    
    /**
     * Whether or not this RFD is/wbs used for downloading.
     */
    privbte transient volatile boolean _isDownloading = false;
    
    /**
     * The crebtion time of this file.
     */
    privbte transient long _creationTime;
    
    /**
     * Whether to seriblize the push proxies
     */
    privbte transient volatile boolean _serializeProxies = false;
    
    /**
     * A mbp of various properties we want to serialize.  Currently we use
     * this object only during de/seriblization, but we keep it cached if we
     * ever crebte one
     */
    privbte Map propertiesMap; 
    
    /**
     * Constructs b new RemoteFileDesc exactly like the other one,
     * but with b different remote host.
     *
     * It is okby to use the same internal structures
     * for URNs becbuse the Set is immutable.
     */
    public RemoteFileDesc(RemoteFileDesc rfd, IpPort ep) {
        this( ep.getAddress(),              // host
              ep.getPort(),                 // port
              COPY_INDEX,                   // index (unknown)
              rfd.getFileNbme(),            // filename
              rfd.getSize(),                // filesize
              rfd.getClientGUID(),          // client GUID
              0,                            // speed
              fblse,                        // chat capable
              2,                            // qublity
              fblse,                        // browse hostable
              rfd.getXMLDocument(),              // xml doc
              rfd.getUrns(),                // urns
              fblse,                        // reply to MCast
              fblse,                        // is firewalled
              AlternbteLocation.ALT_VENDOR, // vendor
              System.currentTimeMillis(),   // timestbmp
              Collections.EMPTY_SET,        // push proxies
              rfd.getCrebtionTime(),       // creation time
              0,                            // firewblled transfer
              null);                       // no PE cbuse not firewalled
    }
    
    /**
     * Constructs b new RemoteFileDesc exactly like the other one,
     * but with b different push proxy host.  Will be handy when processing
     * hebd pongs.
     */
    public RemoteFileDesc(RemoteFileDesc rfd, PushEndpoint pe){
    	this( pe.getAddress(),              // host - ignored
                pe.getPort(),                 // port -ignored
                COPY_INDEX,                   // index (unknown)
                rfd.getFileNbme(),            // filename
                rfd.getSize(),                // filesize
                DbtaUtils.EMPTY_GUID,         // guid
                rfd.getSpeed(),                            // speed
                fblse,                        // chat capable
                rfd.getQublity(),                            // quality
                fblse,                        // browse hostable
                rfd.getXMLDocument(),              // xml doc
                rfd.getUrns(),                // urns
                fblse,                        // reply to MCast
                true,                        // is firewblled
                AlternbteLocation.ALT_VENDOR, // vendor
                System.currentTimeMillis(),   // timestbmp
                null,
                rfd.getCrebtionTime(),	// creation time
                0,
                pe);                // use existing PE
    }

	/** 
     * Constructs b new RemoteFileDesc with metadata.
     *
	 * @pbram host the host's ip
	 * @pbram port the host's port
	 * @pbram index the index of the file that the client sent
	 * @pbram filename the name of the file
	 * @pbram size the completed size of this file
	 * @pbram clientGUID the unique identifier of the client
	 * @pbram speed the speed of the connection
     * @pbram chat true if the location is chattable
     * @pbram quality the quality of the connection, where 0 is the
     *  worst bnd 3 is the best.  (This is the same system as in the
     *  GUI but on b 0 to N-1 scale.)
	 * @pbram browseHost specifies whether or not the remote host supports
	 *  browse host
	 * @pbram xmlDoc the <tt>LimeXMLDocument</tt> for the response
	 * @pbram urns the <tt>Set</tt> of <tt>URN</tt>s for the file
	 * @pbram replyToMulticast true if its from a reply to a multicast query
	 * @pbram firewalled true if the host is firewalled
	 * @pbram vendor the vendor of the remote host
	 * @pbram timestamp the time this RemoteFileDesc was instantiated
	 * @pbram proxies the push proxies for this host
	 * @pbram createTime the network-wide creation time of this file
	 * @throws <tt>IllegblArgumentException</tt> if any of the arguments are
	 *  not vblid
     * @throws <tt>NullPointerException</tt> if the host brgument is 
     *  <tt>null</tt> or if the file nbme is <tt>null</tt>
	 */
	public RemoteFileDesc(String host, int port, long index, String filenbme,
						  int size, byte[] clientGUID, int speed, 
						  boolebn chat, int quality, boolean browseHost, 
						  LimeXMLDocument xmlDoc, Set urns,
						  boolebn replyToMulticast, boolean firewalled, 
                          String vendor, long timestbmp,
                          Set proxies, long crebteTime) {
        this(host, port, index, filenbme, size, clientGUID, speed, chat,
             qublity, browseHost, xmlDoc, urns, replyToMulticast, firewalled,
             vendor, timestbmp, proxies, createTime, 0, null);
    }

	/** 
     * Constructs b new RemoteFileDesc with metadata.
     *
	 * @pbram host the host's ip
	 * @pbram port the host's port
	 * @pbram index the index of the file that the client sent
	 * @pbram filename the name of the file
	 * @pbram clientGUID the unique identifier of the client
	 * @pbram speed the speed of the connection
     * @pbram chat true if the location is chattable
     * @pbram quality the quality of the connection, where 0 is the
     *  worst bnd 3 is the best.  (This is the same system as in the
     *  GUI but on b 0 to N-1 scale.)
     * @pbram xmlDocs the array of XML documents pertaining to this file
	 * @pbram browseHost specifies whether or not the remote host supports
	 *  browse host
	 * @pbram xmlDoc the <tt>LimeXMLDocument</tt> for the response
	 * @pbram urns the <tt>Set</tt> of <tt>URN</tt>s for the file
	 * @pbram replyToMulticast true if its from a reply to a multicast query
	 *
	 * @throws <tt>IllegblArgumentException</tt> if any of the arguments are
	 *  not vblid
     * @throws <tt>NullPointerException</tt> if the host brgument is 
     *  <tt>null</tt> or if the file nbme is <tt>null</tt>
	 */
	public RemoteFileDesc(String host, int port, long index, String filenbme,
						  int size, byte[] clientGUID, int speed, 
						  boolebn chat, int quality, boolean browseHost, 
						  LimeXMLDocument xmlDoc, Set urns,
						  boolebn replyToMulticast, boolean firewalled, 
                          String vendor, long timestbmp,
                          Set proxies, long crebteTime, 
                          int FWTVersion) {
		this(host,port,index,filenbme,size, clientGUID,speed,chat,quality,browseHost,
                xmlDoc, urns, replyToMulticbst, firewalled,vendor,timestamp,proxies,
                crebteTime, FWTVersion, null); // create pe if firewalled
	}
	
	public RemoteFileDesc(String host, int port, long index, String filenbme,
	        			int size,int speed,boolebn chat, int quality, boolean browseHost,
	        			LimeXMLDocument xmlDoc, Set urns, boolebn replyToMulticast,
	        			boolebn firewalled, String vendor,long timestamp,long createTime,
	        			PushEndpoint pe) {
        this(host,port,index,filenbme,size,null,speed,chat,quality,browseHost,xmlDoc,urns,
                replyToMulticbst,firewalled,vendor,timestamp,null,createTime,0,pe); // use exising pe
    }
    
    /**
     * Actubl constructor.  If the firewalled flag is set and a PE object is passed it is used, if 
     * no PE object is pbssed a new one is created. 
     */
    privbte RemoteFileDesc (String host, int port, long index, String filename,
            int size, byte[] clientGUID, int speed,boolebn chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set urns, boolebn replyToMulticast,
            boolebn firewalled, String vendor,long timestamp,Set proxies, long createTime,
            int FWTVersion, PushEndpoint pe) {
	    
	    if(!NetworkUtils.isVblidPort(port)) {
			throw new IllegblArgumentException("invalid port: "+port);
		} 
		if((speed & 0xFFFFFFFF00000000L) != 0) {
			throw new IllegblArgumentException("invalid speed: "+speed);
		} 
		if(filenbme == null) {
			throw new NullPointerException("null filenbme");
		}
		if(filenbme.equals("")) {
			throw new IllegblArgumentException("cannot accept empty string file name");
		}
		if((size & 0xFFFFFFFF00000000L) != 0) {
			throw new IllegblArgumentException("invalid size: "+size);
		}
		if((index & 0xFFFFFFFF00000000L) != 0) {
			throw new IllegblArgumentException("invalid index: "+index);
		}
        if(host == null) {
            throw new NullPointerException("null host");
        }
        
	    _speed = speed;
		_host = host;
		_port = port;
		_index = index;
		_filenbme = filename;
		_size = size;
        _firewblled = firewalled;
		
		if (firewblled) {
            if (pe != null) 
                _pushAddr = pe;
            else {
                try {
                    _pushAddr = new PushEndpoint(clientGUID,proxies,
                        PushEndpoint.PLAIN, FWTVersion, 
                        new IpPortImpl(_host,_port));
                }cbtch (UnknownHostException uhe) {
                    throw new IllegblArgumentException("invalid host");
                }
            }
            
            _clientGUID = _pushAddr.getClientGUID();
        } else 
            _clientGUID = clientGUID;
        
        
		_chbtEnabled = chat;
        _qublity = quality;
		_browseHostEnbbled = browseHost;
		_replyToMulticbst = replyToMulticast;
        _vendor = vendor;
        _timestbmp = timestamp;
        _crebtionTime = createTime;
		
	if(xmlDoc!=null) //not strictly needed
            _xmlDocs = new LimeXMLDocument[] {xmlDoc};
        else
            _xmlDocs = null;
		if(urns == null) {
			_urns = Collections.EMPTY_SET;
		}
		else {
			_urns = Collections.unmodifibbleSet(urns);
		}
        _http11 = ( !_urns.isEmpty() );
	}

    privbte void readObject(ObjectInputStream stream) 
		throws IOException, ClbssNotFoundException {
        strebm.defaultReadObject();
        //Older downlobds.dat files do not have _urns, so _urns will be null
        //(the defbult Java value).  Hence we also initialize
        //_browseHostEnbbled.  See class overview for more details.
        if(_urns == null) {
            _urns = Collections.EMPTY_SET;
            _browseHostEnbbled= false;
        } else {
            // It seems thbt the Urn Set has some java.io.Files
            // inserted into it. See:
            // http://bugs.limewire.com:8080/bugs/sebrching.jsp?disp1=l&disp2=c&disp3=o&disp4=j&l=141&c=188&m=694_223
            // Here we check for this cbse and remove the offending object.
            HbshSet newUrns = null;
            Iterbtor iter = _urns.iterator();
            while(iter.hbsNext()) {
                Object next = iter.next();
                if(!(next instbnceof URN)) {
                    if(newUrns == null) {
                        newUrns = new HbshSet();
                        newUrns.bddAll(_urns);
                    }
                    newUrns.remove(next);
                }
            }
            if(newUrns != null) {
                _urns = Collections.unmodifibbleSet(newUrns);
            }
        }
                
		// preserve the invbriant that the LimeXMLDocument array either be
		// null or hbve at least one element
		if(_xmlDocs != null && _xmlDocs.length == 0) {
			_xmlDocs = null;
		}
        // http11 must be set mbnually, because older clients did not have this
        // field but did hbve urns.
        _http11 = ( _http11 || !_urns.isEmpty() );
        
        // if we sbved any properties, read them now
        if (propertiesMbp != null) {
            String http = (String)propertiesMbp.get("_pushAddr");
            if (http != null) {
                try {
                    _pushAddr = new PushEndpoint(http);
                    if (!_firewblled) {
                        Assert.silent(fblse, "deserialized RFD had PE but wasn't firewalled, "+this+" "+_pushAddr);
                        _firewblled = true;
                    }
                } cbtch (IOException iox) {}
            }
            // currently, we do not need the mbp to exist during the life of the object
            // since we will not seriblize pe unless told so this lifetime
            propertiesMbp = null;
        }
    }
    
    public void setSeriblizeProxies() {
        _seriblizeProxies = true;
    }
    
    privbte void writeObject(ObjectOutputStream stream) throws IOException {
        if (_seriblizeProxies && _pushAddr != null) {
            if (propertiesMbp == null)
                propertiesMbp = new HashMap();
            
            // this will blso update the PE in case it changed since last serialization
            propertiesMbp.put("_pushAddr",_pushAddr.httpStringValue());
        }
        strebm.defaultWriteObject();
    }
    
    /** 
     * Accessor for HTTP11.
     *
     * @return Whether or not we think this host supports HTTP11.
     */
    public boolebn isHTTP11() {
        return _http11;
    }
    
    /**
     * Mutbtor for HTTP11.  Should be set after connecting.
     */
    public void setHTTP11(boolebn http11) {
        _http11 = http11;
    }
    
    /**
     * Returns true if this is b partial source
     */
    public boolebn isPartialSource() {
        return (_bvailableRanges != null);
    }
    
    /**
     * @return whether this rfd points to myself.
     */
    public boolebn isMe() {
        return needsPush() ? 
                Arrbys.equals(_clientGUID,RouterService.getMyGUID()) :
                    NetworkUtils.isMe(getHost(),getPort());
    }
    /**
     * Accessor for the bvailable ranges.
     */
    public IntervblSet getAvailableRanges() {
        return (IntervblSet)_availableRanges.clone();
    }

    /**
     * Mutbtor for the available ranges.
     */
    public void setAvbilableRanges(IntervalSet availableRanges) {
        this._bvailableRanges = availableRanges;
    }
    
    /**
     * updbtes the push address of the rfd to a new one.
     * This should be done only to updbte the set of push proxies,
     * febtures or FWT capability.
     */
    public void setPushAddress(PushEndpoint pe) {
        if (!Arrbys.equals(pe.getClientGUID(),this._clientGUID))
                throw new IllegblArgumentException("different clientGUID");
        this._pushAddr=pe;
    }
    
    /**
     * Returns the current fbiled count.
     */
    public int getFbiledCount() {
        return _fbiledCount;
    }
    
    /**
     * Increments the fbiled count by one.
     */
    public void incrementFbiledCount() {
        _fbiledCount++;
    }
    
    /**
     * Resets the fbiled count back to zero.
     */
    public void resetFbiledCount() {
        _fbiledCount = 0;
    }
    
    /**
     * Determines whether or not this RemoteFileDesc wbs created
     * from bn alternate location.
     */
    public boolebn isFromAlternateLocation() {
        return "ALT".equbls(_vendor);
    }
    
    /**
     * @return true if this host is still busy bnd should not be retried
     */
    public boolebn isBusy() {
        return isBusy(System.currentTimeMillis());
    }
    
    public boolebn isBusy(long now) {
        return now < _ebrliestRetryTime;
    }

    /**
     * @return time to wbit until this host will be ready to be retried
     * in seconds
     */
    public int getWbitTime(long now) {
        return (isBusy(now) ? 
                (int) (_ebrliestRetryTime - now)/1000 + 1:
                0 );
    }

    /**
     * Mutbtor for _earliestRetryTime. 
     * @pbram seconds number of seconds to wait before retrying
     */
    public void setRetryAfter(int seconds) {
        if(LOG.isDebugEnbbled())
            LOG.debug("setting retry bfter to be [" + seconds + 
                      "] seconds for " + this);        
        _ebrliestRetryTime = System.currentTimeMillis() + seconds*1000;
    }
    
    /**
     * The crebtion time of this file.
     */
    public long getCrebtionTime() {
        return _crebtionTime;
    }

	/**
     * @return Returns the _THEXFbiled.
     */
    public boolebn hasTHEXFailed() {
        return _THEXFbiled;
    }

    /**
     * Hbving THEX with this host is no good. We can get our THEX from anybody,
     * so we won't bother bgain. 
     */
    public void setTHEXFbiled() {
        _THEXFbiled = true;
    }
    
    /**
     * Sets this RFD bs downloading.
     */
    public void setDownlobding(boolean dl) {
        _isDownlobding = dl;
    }
    
    /**
     * Determines if this RFD is downlobding.
     *
     * @return whether or not this is downlobding
     */
    public boolebn isDownloading() { return _isDownloading; }

	/**
	 * Accessor for the host ip with this file.
	 *
	 * @return the host ip with this file
	 */
	public finbl String getHost() {return _host;}

	/**
	 * Accessor for the port of the host with this file.
	 *
	 * @return the file nbme for the port of the host
	 */
	public finbl int getPort() {return _port;}

	/**
	 * Accessor for the index this file, which cbn be <tt>null</tt>.
	 *
	 * @return the file nbme for this file, which can be <tt>null</tt>
	 */
	public finbl long getIndex() {return _index;}

	/**
	 * Accessor for the size in bytes of this file.
	 *
	 * @return the size in bytes of this file
	 */
	public finbl int getSize() {return _size;}
	
	public finbl long getFileSize() { return _size; }

	/**
	 * Accessor for the file nbme for this file, which can be <tt>null</tt>.
	 *
	 * @return the file nbme for this file, which can be <tt>null</tt>
	 */
	public finbl String getFileName() {return _filename;}

	/**
	 * Accessor for the client guid for this file, which cbn be <tt>null</tt>.
	 *
	 * @return the client guid for this file, which cbn be <tt>null</tt>
	 */
	public finbl byte[] getClientGUID() {return _clientGUID;}

	/**
	 * Accessor for the speed of the host with this file, which cbn be 
	 * <tt>null</tt>.
	 *
	 * @return the speed of the host with this file, which cbn be 
	 *  <tt>null</tt>
	 */
	public finbl int getSpeed() {return _speed;}	
    
    public finbl String getVendor() {return _vendor;}

	public finbl boolean chatEnabled() {return _chatEnabled;}
	public finbl boolean browseHostEnabled() {return _browseHostEnabled;}

	/**
	 * Returns the "qublity" of the remote file in terms of firewalled status,
	 * whether or not the remote host hbs open slots, etc.
	 * 
	 * @return the current "qublity" of the remote file in terms of the 
	 *  determined likelihood of the request succeeding
	 */
    public finbl int getQuality() {return _quality;}

	/**
	 * Returns the <tt>LimeXMLDocument</tt> for this <tt>RemoteFileDesc</tt>, 
	 * which cbn be <tt>null</tt>.
	 *
	 * @return the <tt>LimeXMLDocument</tt> for this <tt>RemoteFileDesc</tt>, 
	 * which cbn be <tt>null</tt>.
	 */
    public finbl LimeXMLDocument getXMLDocument() {
        if (_xmlDocs==null)
            return null;
        else
            return _xmlDocs[0];  //cbn be null
	}

	/**
	 * Accessor for the <tt>Set</tt> of URNs for this <tt>RemoteFileDesc</tt>.
	 *
	 * @return the <tt>Set</tt> of URNs for this <tt>RemoteFileDesc</tt>
	 */
	public finbl Set getUrns() {
		return _urns;
	}

	/**
	 * Accessor for the SHA1 URN for this <tt>RemoteFileDesc</tt>.
	 *
	 * @return the SHA1 <tt>URN</tt> for this <tt>RemoteFileDesc</tt>, or 
	 *  <tt>null</tt> if there is none
	 */
	public finbl URN getSHA1Urn() {
		Iterbtor iter = _urns.iterator(); 
		while(iter.hbsNext()) {
			URN urn = (URN)iter.next();
			// defensively check bgainst null values added.
			if(urn == null) continue;
			if(urn.isSHA1()) {
				return urn;
			}
		}
		return null;
	}

	/**
	 * Returns bn <tt>URL</tt> instance for this <tt>RemoteFileDesc</tt>.
	 *
	 * @return bn <tt>URL</tt> instance for this <tt>RemoteFileDesc</tt>
	 */
	public URL getUrl() {
		try {
			String fileNbme = "";
			URN urn = getSHA1Urn();
			if(urn == null) {
				fileNbme = "/get/"+_index+"/"+_filename;
			} else {
				fileNbme = HTTPConstants.URI_RES_N2R+urn.httpStringValue();
			}
			return new URL("http", _host, _port, fileNbme);
		} cbtch(MalformedURLException e) {
			return null;
		}
	}
	
    /**
     * Determines whether or not this RFD wbs a reply to a multicast query.
     *
     * @return <tt>true</tt> if this RFD wbs in reply to a multicast query,
     *  otherwise <tt>fblse</tt>
     */
	public finbl boolean isReplyToMulticast() {
	    return _replyToMulticbst;
    }

    /**
     * Determines whether or not this host reported b private address.
     *
     * @return <tt>true</tt> if the bddress for this host is private,
     *  otherwise <tt>fblse</tt>.  If the address is unknown, returns
     *  <tt>true</tt>
     *
     * TODO:: use InetAddress in this clbss for the host so that we don't 
     * hbve to go through the process of creating one each time we check
     * it it's b private address
     */
	public finbl boolean isPrivate() {
        return NetworkUtils.isPrivbteAddress(_host);
	}
    
    public boolebn isFirewalled() {
        return _firewblled;
    }

    /**
     * Accessor for the <tt>Set</tt> of <tt>PushProxyInterfbce</tt>s for this
     * file -- cbn be empty, but is guaranteed to be non-null.
     *
     * @return the <tt>Set</tt> of proxy hosts thbt will accept push requests
     *  for this host -- cbn be empty
     */
    public finbl Set getPushProxies() {
    	if (_pushAddr!=null)
    		return _pushAddr.getProxies();
    	else
    		return Collections.EMPTY_SET;
    }

    /**
     * @return whether this RFD supports firewbll-to-firewall transfer.
     * For this to be true we need to hbve some push proxies, indication that
     * the host supports FWT bnd we need to know that hosts' external address.
     */
    public finbl boolean supportsFWTransfer() {
        
        if (_host.equbls(BOGUS_IP) ||
                !NetworkUtils.isVblidAddress(_host) || 
                NetworkUtils.isPrivbteAddress(_host))
            return fblse;
        
        return _pushAddr == null ? fblse : _pushAddr.supportsFWTVersion() > 0;
    }

    /**
     * Crebtes the _hostData lazily and uses as necessary
     */ 
    public finbl RemoteHostData getRemoteHostData() {
        if(_hostDbta == null)
            _hostDbta = new RemoteHostData(_host, _port, _clientGUID);
        return _hostDbta;
    }

    /**
     * @return true if I bm not a multicast host and have a hash.
     * blso, if I am firewalled I must have at least one push proxy,
     * otherwise my port bnd address need to be valid.
     */
    public finbl boolean isAltLocCapable() {
        boolebn ret = getSHA1Urn() != null &&
               !_replyToMulticbst;
        
        if (_firewblled)
        	ret = ret && 
				_pushAddr!=null &&
				_pushAddr.getProxies().size() > 0;
		else
             ret= ret &&  
			    NetworkUtils.isVblidPort(_port) &&
                !NetworkUtils.isPrivbteAddress(_host) &&
                NetworkUtils.isVblidAddress(_host);
        
        return ret;
    }
    
    /**
     * 
     * @return whether b push should be sent tho this rfd.
     */
    public boolebn needsPush() {
        
        //if replying to multicbst, do a push.
        if ( isReplyToMulticbst() )
            return true;
        //Return true if rfd is privbte or unreachable
        if (isPrivbte()) {
            // Don't do b push for magnets in case you are in a private network.
            // Note to Sbm: This doesn't mean that isPrivate should be true.
            if (this instbnceof URLRemoteFileDesc) 
                return fblse;
            else  // Otherwise obey push rule for privbte rfds.
                return true;
        }
        else if (!NetworkUtils.isVblidPort(getPort()))
            return true;
        
        else return isFirewblled();
    }
    
    /**
     * 
     * @return the push bddress.
     */
    public PushEndpoint getPushAddr() {
    	return _pushAddr;
    }

	/**
	 * Overrides <tt>Object.equbls</tt> to return instance equality
	 * bbsed on the equality of all <tt>RemoteFileDesc</tt> fields.
	 *
	 * @return <tt>true</tt> if bll of fields of this 
	 *  <tt>RemoteFileDesc</tt> instbnce are equal to all of the 
	 *  fields of the specified object, bnd <tt>false</tt> if this
	 *  is not the cbse, or if the specified object is not a 
	 *  <tt>RemoteFileDesc</tt>.
	 *
	 * Dynbmic values such as _http11, and _availableSources
	 * bre not checked here, as they can change and still be considered
	 * the sbme "remote file".
	 * 
	 * The _host field mby be equal for many firewalled locations; 
	 * therefore it is necessbry that we distinguish those by their 
	 * client GUIDs
	 */
    public boolebn equals(Object o) {
		if(o == this) return true;
        if (! (o instbnceof RemoteFileDesc))
            return fblse;
        RemoteFileDesc other=(RemoteFileDesc)o;
        if (! (nullEqubls(_host, other._host) && (_port==other._port)) )
            return fblse;

        if (_size != other._size)
            return fblse;
        
        if ( (_clientGUID ==null) != (other._clientGUID==null) )
            return fblse;
        
        if ( _clientGUID!= null &&
                ! ( Arrbys.equals(_clientGUID,other._clientGUID)))
            return fblse;

        if (_urns.isEmpty() && other._urns.isEmpty())
            return nullEqubls(_filename, other._filename);
        else
            return urnSetEqubls(_urns, other._urns);
    }
    
    privbte boolean nullEquals(Object one, Object two) {
        return one == null ? two == null : one.equbls(two);
    }
    
    privbte boolean urnSetEquals(Set one, Set two) {
        for (Iterbtor iter = one.iterator(); iter.hasNext(); ) {
            if (two.contbins(iter.next())) {
                return true;
            }
        }
        return fblse;
    }

    privbte boolean byteArrayEquals(byte[] one, byte[] two) {
        return one == null ? two == null : Arrbys.equals(one, two);
    }

	/**
	 * Overrides the hbshCode method of Object to meet the contract of 
	 * hbshCode.  Since we override equals, it is necessary to also 
	 * override hbshcode to ensure that two "equal" RemoteFileDescs
	 * return the sbme hashCode, less we unleash unknown havoc on the
	 * hbsh-based collections.
	 *
	 * @return b hash code value for this object
	 */
	public int hbshCode() {
	   if(_hbshCode == 0) {
            int result = 17;
            result = (37* result)+_host.hbshCode();
            result = (37* result)+_port;
			result = (37* result)+_size;
            result = (37* result)+_urns.hbshCode();
            if (_clientGUID!=null)
                result = (37* result)+(new GUID(_clientGUID)).hbshCode();
            _hbshCode = result;
        }
		return _hbshCode;
	}

    public String toString() {
        return  ("<"+getHost()+":"+getPort()+", "
				 +getFileNbme().toLowerCase()+">");
    }

    public String getAddress() {
        return getHost();
    }

    public InetAddress getInetAddress() {
        try {
            return InetAddress.getByNbme(getAddress());
        }cbtch(UnknownHostException bad){}
        return null;
    }
    
    public void setQueueStbtus(int status) {
        _queueStbtus = status;
    }
    
    public int getQueueStbtus() {
        return _queueStbtus;
    }

	public InetSocketAddress getSocketAddress() {
		InetAddress bddr = getInetAddress();
		if (bddr != null) {
			return new InetSocketAddress(bddr, getPort());
		}
		return null;
	}
}
