package com.limegroup.gnutella.downloader;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.security.SecureMessage;
import org.limewire.util.Objects;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RemoteHostData;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A default implementation for {@link RemoteFileDesc}.
 */
class RemoteFileDescImpl implements RemoteFileDesc {
    
    private static final Log LOG = LogFactory.getLog(RemoteFileDesc.class);

    private final String _host;
	private final int _port;
	private final String _filename; 
	private final long _index;
	private final byte[] _clientGUID;
	private final int _speed;
	private final boolean _chatEnabled;
    private final int _quality;
    private final boolean _replyToMulticast;
    private final LimeXMLDocument _xmlDoc;
	private final Set<URN>  _urns;

    /**
     * Boolean indicating whether or not the remote host has browse host 
     * enabled.
     */
	private final boolean _browseHostEnabled;

    private final boolean _firewalled;
    private final String _vendor;
    
    /**
     * Whether or not the remote host supports HTTP/1.1
     * This is purposely NOT IMMUTABLE.  Before we connect,
     * we can only assume the remote host supports HTTP/1.1 by
     * looking at the set of URNs.  If any exist, we assume
     * HTTP/1.1 is supported (because URNs were added to Gnutella
     * after HTTP/1.1).  Once we connect, this value is set to
     * be whatever the host reports in the response line.
     *
     * When deserializing, this value may be wrong for older download.dat
     * files.  (Older versions will always set this to false, because
     * the field did not exist.)  To counter that, when deserializing,
     * if this is false, we set it to true if any URNs are present.
     */
    private boolean _http11;
    
    /** True if this host is TLS capable. */
    private boolean _tlsCapable;
    
    /**
     * The <tt>PushEndpoint</tt> for this RFD.
     * if null, the rfd is not behind a push proxy.
     */
    private final PushEndpoint _pushAddr;
		

    /**
     * The list of available ranges.
     * This is NOT SERIALIZED.
     */
    private IntervalSet _availableRanges = null;
    
    /**
     * The last known queue status of the remote host
     * negative values mean free slots
     */
    private int _queueStatus = Integer.MAX_VALUE;
    
    /**
     * The number of times this download has failed while attempting
     * to transfer data.
     */
    private int _failedCount = 0;

    /**
     * The earliest time to retry this host in milliseconds since 01-01-1970
     */
    private volatile long _earliestRetryTime = 0;

    /**
     * The cached hash code for this RFD.
     */
    private int _hashCode = 0;

    /**
     * Whether or not THEX retrieval has failed with this host.
     */
    private boolean _THEXFailed = false;

    /**
     * The cached RemoteHostData for this rfd.
     */
    private RemoteHostData _hostData = null;
    
    /**
     * Whether or not this RFD is/was used for downloading.
     */
    private volatile boolean _isDownloading = false;
    
    /**
     * The creation time of this file.
     */
    private final long _creationTime;
    
    /** Whether to serialize the push proxies */
    private volatile boolean _serializeProxies = false;
	
	/**
	 * the spam rating of this rfd.
	 */
	private float _spamRating = 0.f;
    
    /** the security of this RemoteFileDesc. */
    private int _secureStatus = SecureMessage.INSECURE;
    
    private final long _size;
    
    private final NetworkInstanceUtils networkInstanceUtils;
    private static final DownloadStatsTracker STATS_TRACKER_STUB = new DownloadStatsTrackerStub();

    /**
     * Actual constructor.  If the firewalled flag is set and a PE object is passed it is used, if 
     * no PE object is passed a new one is created.
     */
    RemoteFileDescImpl (String host, int port, long index, String filename,
            long size, byte[] clientGUID, int speed,boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            boolean firewalled, String vendor, Set<? extends IpPort> proxies, long createTime,
            int FWTVersion, PushEndpoint pe, boolean tlsCapable, boolean http11,
            NetworkInstanceUtils networkInstanceUtils) {
        Objects.nonNull(filename, "filename");
        Objects.nonNull(host, "host");
        if (!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("invalid port: " + port);
        if ((speed & 0xFFFFFFFF00000000L) != 0)
            throw new IllegalArgumentException("invalid speed: " + speed);
        if (filename.equals(""))
            throw new IllegalArgumentException("cannot accept empty string file name");
        if (size < 0 || size > MAX_FILE_SIZE)
            throw new IllegalArgumentException("invalid size: " + size);
        if ((index & 0xFFFFFFFF00000000L) != 0)
            throw new IllegalArgumentException("invalid index: " + index);
        
        _speed = speed;
		_host = host;
		_port = port;
		_index = index;
		_filename = filename;
        _size = size;
        _firewalled = firewalled;
		_pushAddr = pe;
		_clientGUID = clientGUID;
		_chatEnabled = chat;
        _quality = quality;
		_browseHostEnabled = browseHost;
		_replyToMulticast = replyToMulticast;
        _vendor = vendor;
        _creationTime = createTime;
        _tlsCapable = tlsCapable;
        _xmlDoc = xmlDoc;
        _http11 = http11;
        _urns = Collections.unmodifiableSet(urns);
        this.networkInstanceUtils = networkInstanceUtils;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#setSerializeProxies()
     */
    public void setSerializeProxies() {
        _serializeProxies = true;
    }
    
    /** Returns true if the host supports TLS. */
    public boolean isTLSCapable() {
        return _tlsCapable;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#setTLSCapable(boolean)
     */
    public void setTLSCapable(boolean tlsCapable) {
        _tlsCapable = tlsCapable;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isHTTP11()
     */
    public boolean isHTTP11() {
        return _http11;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#setHTTP11(boolean)
     */
    public void setHTTP11(boolean http11) {
        _http11 = http11;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isPartialSource()
     */
    public boolean isPartialSource() {
        return (_availableRanges != null);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isMe(byte[])
     */
    public boolean isMe(byte[] myClientGUID) {
        return needsPush() ? 
                Arrays.equals(_clientGUID, myClientGUID) :
                    networkInstanceUtils.isMe(getHost(),getPort());
    }
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getAvailableRanges()
     */
    public IntervalSet getAvailableRanges() {
        return _availableRanges.clone();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#setAvailableRanges(org.limewire.collection.IntervalSet)
     */
    public void setAvailableRanges(IntervalSet availableRanges) {
        this._availableRanges = availableRanges;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getFailedCount()
     */
    public int getFailedCount() {
        return _failedCount;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#incrementFailedCount()
     */
    public void incrementFailedCount() {
        _failedCount++;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#resetFailedCount()
     */
    public void resetFailedCount() {
        _failedCount = 0;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isFromAlternateLocation()
     */
    public boolean isFromAlternateLocation() {
        return "ALT".equals(_vendor);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isBusy()
     */
    public boolean isBusy() {
        return isBusy(System.currentTimeMillis());
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isBusy(long)
     */
    public boolean isBusy(long now) {
        return now < _earliestRetryTime;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getWaitTime(long)
     */
    public int getWaitTime(long now) {
        return (isBusy(now) ? 
                (int) (_earliestRetryTime - now)/1000 + 1:
                0 );
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#setRetryAfter(int)
     */
    public void setRetryAfter(int seconds) {
        if(LOG.isDebugEnabled())
            LOG.debug("setting retry after to be [" + seconds + 
                      "] seconds for " + this);        
        _earliestRetryTime = System.currentTimeMillis() + seconds*1000;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getCreationTime()
     */
    public long getCreationTime() {
        return _creationTime;
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#hasTHEXFailed()
     */
    public boolean hasTHEXFailed() {
        return _THEXFailed;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#setTHEXFailed()
     */
    public void setTHEXFailed() {
        _THEXFailed = true;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#setDownloading(boolean)
     */
    public void setDownloading(boolean dl) {
        _isDownloading = dl;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isDownloading()
     */
    public boolean isDownloading() { return _isDownloading; }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getHost()
     */
	public final String getHost() {return _host;}

	/**
	 * Accessor for the port of the host with this file.
	 *
	 * @return the file name for the port of the host
	 */
	public final int getPort() {return _port;}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getIndex()
     */
	public final long getIndex() {return _index;}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getSize()
     */
	public final long getSize() {return _size;}
	
	public final long getFileSize() { return _size; }

	/**
	 * Accessor for the file name for this file, which can be <tt>null</tt>.
	 *
	 * @return the file name for this file, which can be <tt>null</tt>
	 */
	public final String getFileName() {return _filename;}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getClientGUID()
     */
	public final byte[] getClientGUID() {return _clientGUID;}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getSpeed()
     */
	public final int getSpeed() {return _speed;}	
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getVendor()
     */
    public final String getVendor() {return _vendor;}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isChatEnabled()
     */
	public final boolean isChatEnabled() {return _chatEnabled;}
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isBrowseHostEnabled()
     */
	public final boolean isBrowseHostEnabled() {return _browseHostEnabled;}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getQuality()
     */
    public final int getQuality() {return _quality;}

	/**
	 * Returns the <tt>LimeXMLDocument</tt> for this <tt>RemoteFileDesc</tt>, 
	 * which can be <tt>null</tt>.
	 *
	 * @return the <tt>LimeXMLDocument</tt> for this <tt>RemoteFileDesc</tt>, 
	 * which can be <tt>null</tt>.
	 */
    public final LimeXMLDocument getXMLDocument() {
        return _xmlDoc;
	}

	/**
	 * Accessor for the <tt>Set</tt> of URNs for this <tt>RemoteFileDesc</tt>.
	 *
	 * @return the <tt>Set</tt> of URNs for this <tt>RemoteFileDesc</tt>
	 */
	public final Set<URN> getUrns() {
		return _urns;
	}

	/**
	 * Accessor for the SHA1 URN for this <tt>RemoteFileDesc</tt>.
	 *
	 * @return the SHA1 <tt>URN</tt> for this <tt>RemoteFileDesc</tt>, or 
	 *  <tt>null</tt> if there is none
	 */
	public final URN getSHA1Urn() {
        for(URN urn : _urns) {
			// defensively check against null values added.
			if(urn == null) continue;
			if(urn.isSHA1()) {
				return urn;
			}
		}
		return null;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getUrl()
     */
	public URL getUrl() {
		try {
			String fileName = "";
			URN urn = getSHA1Urn();
			if(urn == null) {
				fileName = "/get/"+_index+"/"+_filename;
			} else {
				fileName = HTTPConstants.URI_RES_N2R+urn.httpStringValue();
			}
			return new URL("http", _host, _port, fileName);
		} catch(MalformedURLException e) {
			return null;
		}
	}
	
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isReplyToMulticast()
     */
	public final boolean isReplyToMulticast() {
	    return _replyToMulticast;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isPrivate()
     */
	public final boolean isPrivate() {
        return networkInstanceUtils.isPrivateAddress(_host);
	}
    
    public boolean isFirewalled() {
        return _firewalled;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getPushProxies()
     */
    public final Set<? extends IpPort> getPushProxies() {
    	if (_pushAddr!=null)
    		return _pushAddr.getProxies();
    	else
    		return Collections.emptySet();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#supportsFWTransfer()
     */
    public final boolean supportsFWTransfer() {
        
        if (_host.equals(BOGUS_IP) ||
                !NetworkUtils.isValidAddress(_host) || 
                networkInstanceUtils.isPrivateAddress(_host))
            return false;
        
        return _pushAddr == null ? false : _pushAddr.getFWTVersion() > 0;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getRemoteHostData()
     */ 
    public final RemoteHostData getRemoteHostData() {
        if(_hostData == null)
            _hostData = new RemoteHostData(_host, _port, _clientGUID);
        return _hostData;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#isAltLocCapable()
     */
    public final boolean isAltLocCapable() {
        boolean ret = getSHA1Urn() != null &&
               !_replyToMulticast;
        
        if (_firewalled)
        	ret = ret && 
				_pushAddr!=null &&
				_pushAddr.getProxies().size() > 0;
		else
             ret= ret &&  
			    NetworkUtils.isValidPort(_port) &&
                !networkInstanceUtils.isPrivateAddress(_host) &&
                NetworkUtils.isValidAddress(_host);
        
        return ret;
    }

    public boolean needsPush() {
        return needsPush(STATS_TRACKER_STUB);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#needsPush()
     */
    public boolean needsPush(DownloadStatsTracker statsTracker) {
        
        //if replying to multicast, do a push.
        if ( isReplyToMulticast() ) {
            statsTracker.increment(DownloadStatsTracker.PushReason.MULTICAST_REPLY);
            return true;
        }
        //Return true if rfd is private or unreachable
        if (isPrivate()) {
            // Don't do a push for magnets in case you are in a private network.
            // Note to Sam: This doesn't mean that isPrivate should be true.
            if (this instanceof UrlRemoteFileDescImpl) 
                return false;
            else  {// Otherwise obey push rule for private rfds.
                statsTracker.increment(DownloadStatsTracker.PushReason.PRIVATE_NETWORK);
                return true;
            }
        }
        else if (!NetworkUtils.isValidPort(getPort())) {
            statsTracker.increment(DownloadStatsTracker.PushReason.INVALID_PORT);
            return true;
        }
        
        else {
            boolean isFirewalled = isFirewalled();
            if(isFirewalled) {
                statsTracker.increment(DownloadStatsTracker.PushReason.FIREWALL);
            }
            return isFirewalled;
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getPushAddr()
     */
    public PushEndpoint getPushAddr() {
    	return _pushAddr;
    }

	/**
	 * Overrides <tt>Object.equals</tt> to return instance equality
	 * based on the equality of all <tt>RemoteFileDesc</tt> fields.
	 *
	 * @return <tt>true</tt> if all of fields of this 
	 *  <tt>RemoteFileDesc</tt> instance are equal to all of the 
	 *  fields of the specified object, and <tt>false</tt> if this
	 *  is not the case, or if the specified object is not a 
	 *  <tt>RemoteFileDesc</tt>.
	 *
	 * Dynamic values such as _http11, and _availableSources
	 * are not checked here, as they can change and still be considered
	 * the same "remote file".
	 * 
	 * The _host field may be equal for many firewalled locations; 
	 * therefore it is necessary that we distinguish those by their 
	 * client GUIDs
	 */
    @Override
    public boolean equals(Object o) {
		if(o == this) return true;
        if (! (o instanceof RemoteFileDesc))
            return false;
        RemoteFileDesc other=(RemoteFileDesc)o;
        if (! (nullEquals(_host, other.getHost()) && (_port==other.getPort())) )
            return false;

        if (_size != other.getSize())
            return false;
        
        if ( (_clientGUID ==null) != (other.getClientGUID()==null) )
            return false;
        
        if ( _clientGUID!= null &&
                ! ( Arrays.equals(_clientGUID,other.getClientGUID())))
            return false;

        if (_urns.isEmpty() && other.getUrns().isEmpty())
            return nullEquals(_filename, other.getFileName());
        else
            return _urns.equals(other.getUrns());
    }
    
    private boolean nullEquals(Object one, Object two) {
        return one == null ? two == null : one.equals(two);
    }

	/**
	 * Overrides the hashCode method of Object to meet the contract of 
	 * hashCode.  Since we override equals, it is necessary to also 
	 * override hashcode to ensure that two "equal" RemoteFileDescs
	 * return the same hashCode, less we unleash unknown havoc on the
	 * hash-based collections.
	 *
	 * @return a hash code value for this object
	 */
	@Override
    public int hashCode() {
	   if(_hashCode == 0) {
            int result = 17;
            result = (37* result)+_host.hashCode();
            result = (37* result)+_port;
			result = (int)((37* result)+_size);
            result = (37* result)+_urns.hashCode();
            if (_clientGUID!=null)
                result = (37* result)+(new GUID(_clientGUID)).hashCode();
            _hashCode = result;
        }
		return _hashCode;
	}

    @Override
    public String toString() {
        return  ("<"+getHost()+":"+getPort()+", "
				 +getFileName() + ">");
    }

    public String getAddress() {
        return getHost();
    }

    public InetAddress getInetAddress() {
        try {
            return InetAddress.getByName(getAddress());
        }catch(UnknownHostException bad){}
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#setQueueStatus(int)
     */
    public void setQueueStatus(int status) {
        _queueStatus = status;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getQueueStatus()
     */
    public int getQueueStatus() {
        return _queueStatus;
    }

	public InetSocketAddress getInetSocketAddress() {
		InetAddress addr = getInetAddress();
		if (addr != null) {
			return new InetSocketAddress(addr, getPort());
		}
		return null;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#setSpamRating(float)
     */
	public void setSpamRating(float rating) {
		_spamRating = rating;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getSpamRating()
     */
	public float getSpamRating() {
		return _spamRating;
	}

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#getSecureStatus()
     */
    public int getSecureStatus() {
        return _secureStatus;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#setSecureStatus(int)
     */
    public void setSecureStatus(int secureStatus) {
        this._secureStatus = secureStatus;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.RemoteFileDesc#toMemento()
     */
    public RemoteHostMemento toMemento() {
        return new RemoteHostMemento(_host, _port, _filename, _index, _clientGUID, _speed,
                _size, _chatEnabled, _quality, _replyToMulticast, xmlString(), _urns,
                _browseHostEnabled, _firewalled, _vendor, _http11, _tlsCapable, pushAddrString()); 
    }
    
    private String xmlString() {
        if(_xmlDoc == null)
            return null;
        else
            return _xmlDoc.getXMLString();
    }
    
    private String pushAddrString() {
        if(_serializeProxies && _pushAddr != null)
            return _pushAddr.httpStringValue();
        else
            return null;
    }
    
    private static class DownloadStatsTrackerStub implements DownloadStatsTracker {
        public Object inspect() {
                return "this is a stub";
            }

            public void successfulDirectConnect() {}

            public void failedDirectConnect() {}

            public void successfulPushConnect() {}

            public void failedPushConnect() {}

            public void increment(DownloadStatsTracker.PushReason reason) {}    
    }
}
