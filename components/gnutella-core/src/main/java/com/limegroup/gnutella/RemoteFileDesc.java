package com.limegroup.gnutella;



import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.security.SecureMessage;
import org.limewire.util.GenericsUtils;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.downloader.URLRemoteFileDesc;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A reference to a single file on a remote machine.  In this respect
 * RemoteFileDesc is similar to a URL, but it contains Gnutella-
 * specific data as well, such as the server's 16-byte GUID.<p>
 *
 * This class is serialized to disk as part of the downloads.dat file.  Hence
 * you must be very careful before making any changes.  Changing the
 * types of fields is DISALLOWED.  Deleting fields is not recommended,
 * as it can cause problems in the future if the field is re-added. 
 * Adding field a F is acceptable as long as the
 * readObject() method of this initializes F to a reasonable value when
 * reading from older files where the fields are not present.  This is exactly
 * what we do with _urns and _browseHostEnabled.  On the other hand, older
 * version of LimeWire will simply discard any extra fields F if reading from a
 * newer serialized file.  
 */
public class RemoteFileDesc implements IpPort, Connectable, Serializable, FileDetails {
    
    private static final Log LOG = LogFactory.getLog(RemoteFileDesc.class);
    
    private static final long serialVersionUID = 6619479308616716538L;
    
    private static final int COPY_INDEX = Integer.MAX_VALUE;

    /** bogus IP we assign to RFDs whose real ip is unknown */
    public static final String BOGUS_IP = "1.1.1.1";
    
    /** Typed reference to an empty list of RemoteFileDescs. */ 
    public static final List<RemoteFileDesc> EMPTY_LIST = Collections.emptyList(); 
    
	private final String _host;
	private final int _port;
	private final String _filename; 
	private final long _index;
	private final byte[] _clientGUID;
	private final int _speed;
    @Deprecated
	private final int _size;
	private final boolean _chatEnabled;
    private final int _quality;
    private final boolean _replyToMulticast;

    /**
     *  RemoteFileDesc can only be constructed with a single piece of metadata.
     *  However, historically RemoteFileDesc stored an array of metadata.  Hence
     *  we must be prepared to read this data from a serialized downloads.dat
     *  file.  In other words, _xmlDocs is typically null or a single non-null
     *  element, unless this was deserialized from an older version.  
	 * 
	 *  INVARIANT: _xmlDocs != null -> _xmlDocs.length != 0
	 */
    private LimeXMLDocument[] _xmlDocs;
	private Set<URN>  _urns;

    /**
     * Boolean indicating whether or not the remote host has browse host 
     * enabled.
     */
	private boolean _browseHostEnabled;

    private boolean _firewalled;
    private String _vendor;
    
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
    public transient boolean _tlsCapable;
    
    /**
     * The <tt>PushEndpoint</tt> for this RFD.
     * if null, the rfd is not behind a push proxy.
     */
    private transient PushEndpoint _pushAddr;
		

    /**
     * The list of available ranges.
     * This is NOT SERIALIZED.
     */
    private transient IntervalSet _availableRanges = null;
    
    /**
     * The last known queue status of the remote host
     * negative values mean free slots
     */
    private transient int _queueStatus = Integer.MAX_VALUE;
    
    /**
     * The number of times this download has failed while attempting
     * to transfer data.
     */
    private transient int _failedCount = 0;

    /**
     * The earliest time to retry this host in milliseconds since 01-01-1970
     */
    private transient volatile long _earliestRetryTime = 0;

    /**
     * The cached hash code for this RFD.
     */
    private transient int _hashCode = 0;

    /**
     * Whether or not THEX retrieval has failed with this host.
     */
    private transient boolean _THEXFailed = false;

    /**
     * The cached RemoteHostData for this rfd.
     */
    private transient RemoteHostData _hostData = null;
    
    /**
     * Whether or not this RFD is/was used for downloading.
     */
    private transient volatile boolean _isDownloading = false;
    
    /**
     * The creation time of this file.
     */
    private transient long _creationTime;
    
    /**
     * Whether to serialize the push proxies
     */
    private transient volatile boolean _serializeProxies = false;
	
	/**
	 * the spam rating of this rfd.
	 */
	private transient float _spamRating = 0.f;
    
    /** the security of this RemoteFileDesc. */
    private transient int _secureStatus = SecureMessage.INSECURE;
    
    private transient volatile long longSize;
    /**
     * A map of various properties we want to serialize.  Currently we use
     * this object only during de/serialization, but we keep it cached if we
     * ever create one
     */
    private Map<String, Serializable> propertiesMap; 
    
    /** A list of keys of properties inserted into the propertiesMap. */
    private static enum RFDProperties {
        PUSH_ADDR, CONNECT_TYPE, LONG_SIZE;
    }
    
    /**
     * Constructs a new RemoteFileDesc exactly like the other one,
     * but with a different remote host.
     *
     * It is okay to use the same internal structures
     * for URNs because the Set is immutable.
     */
    public RemoteFileDesc(RemoteFileDesc rfd, IpPort ep) {
        this( ep.getAddress(),              // host
              ep.getPort(),                 // port
              COPY_INDEX,                   // index (unknown)
              rfd.getFileName(),            // filename
              rfd.getSize(),                // filesize
              rfd.getClientGUID(),          // client GUID
              0,                            // speed
              false,                        // chat capable
              2,                            // quality
              false,                        // browse hostable
              rfd.getXMLDocument(),         // xml doc
              rfd.getUrns(),                // urns
              false,                        // reply to MCast
              false,                        // is firewalled
              AlternateLocation.ALT_VENDOR, // vendor
              IpPort.EMPTY_SET,             // push proxies
              rfd.getCreationTime(),        // creation time
              0,                            // firewalled transfer
              null,                         // no PE cause not firewalled
          ep instanceof Connectable ? 
      ((Connectable)ep).isTLSCapable() : false // TLS capable if ep is.
             );
    }
    
    /**
     * Constructs a new RemoteFileDesc exactly like the other one,
     * but with a different push proxy host.  Will be handy when processing
     * head pongs.
     */
    public RemoteFileDesc(RemoteFileDesc rfd, PushEndpoint pe){
    	this( pe.getAddress(),                // host - ignored
                pe.getPort(),                 // port -ignored
                COPY_INDEX,                   // index (unknown)
                rfd.getFileName(),            // filename
                rfd.getSize(),                // filesize
                DataUtils.EMPTY_GUID,         // guid
                rfd.getSpeed(),               // speed
                false,                        // chat capable
                rfd.getQuality(),             // quality
                false,                        // browse hostable
                rfd.getXMLDocument(),         // xml doc
                rfd.getUrns(),                // urns
                false,                        // reply to MCast
                true,                         // is firewalled
                AlternateLocation.ALT_VENDOR, // vendor
                null,                         // push proxies
                rfd.getCreationTime(),	      // creation time
                0,                            // firewalled transfer
                pe,                           // use existing PE
                false);                       // not TLS capable (they connect to us anyway)
    }

	/** 
     * Constructs a new RemoteFileDesc with metadata.
     *
	 * @param host the host's ip
	 * @param port the host's port
	 * @param index the index of the file that the client sent
	 * @param filename the name of the file
	 * @param size the completed size of this file
	 * @param clientGUID the unique identifier of the client
	 * @param speed the speed of the connection
	 * @param chat true if the location is chattable
	 * @param quality the quality of the connection, where 0 is the
     *  worst and 3 is the best.  (This is the same system as in the
     *  GUI but on a 0 to N-1 scale.)
	 * @param browseHost specifies whether or not the remote host supports
	 *  browse host
	 * @param xmlDoc the <tt>LimeXMLDocument</tt> for the response
	 * @param urns the <tt>Set</tt> of <tt>URN</tt>s for the file
	 * @param replyToMulticast true if its from a reply to a multicast query
	 * @param firewalled true if the host is firewalled
	 * @param vendor the vendor of the remote host
	 * @param proxies the push proxies for this host
	 * @param createTime the network-wide creation time of this file
	 * @param tlsCapable true if the remote host supports TLS
	 * @throws <tt>IllegalArgumentException</tt> if any of the arguments are
	 *  not valid
     * @throws <tt>NullPointerException</tt> if the host argument is 
     *  <tt>null</tt> or if the file name is <tt>null</tt>
	 */
	public RemoteFileDesc(String host, int port, long index, String filename,
						  long size, byte[] clientGUID, int speed, 
						  boolean chat, int quality, boolean browseHost, 
						  LimeXMLDocument xmlDoc, Set<? extends URN> urns,
						  boolean replyToMulticast, boolean firewalled, 
                          String vendor, 
                          Set<? extends IpPort> proxies, long createTime, boolean tlsCapable) {
        this(host, port, index, filename, size, clientGUID, speed, chat,
             quality, browseHost, xmlDoc, urns, replyToMulticast, firewalled,
             vendor, proxies, createTime, 0, null, tlsCapable);
    }

	/** 
     * Constructs a new RemoteFileDesc with metadata.
     *
	 * @param host the host's ip
	 * @param port the host's port
	 * @param index the index of the file that the client sent
	 * @param filename the name of the file
	 * @param clientGUID the unique identifier of the client
	 * @param speed the speed of the connection
	 * @param chat true if the location is chattable
	 * @param quality the quality of the connection, where 0 is the
     *  worst and 3 is the best.  (This is the same system as in the
     *  GUI but on a 0 to N-1 scale.)
	 * @param browseHost specifies whether or not the remote host supports
	 *  browse host
	 * @param xmlDoc the <tt>LimeXMLDocument</tt> for the response
	 * @param urns the <tt>Set</tt> of <tt>URN</tt>s for the file
	 * @param replyToMulticast true if its from a reply to a multicast query
	 * @param tlsCapable true if the host supports a TLS connection
	 * @param xmlDocs the array of XML documents pertaining to this file
	 * @throws <tt>IllegalArgumentException</tt> if any of the arguments are
	 *  not valid
     * @throws <tt>NullPointerException</tt> if the host argument is 
     *  <tt>null</tt> or if the file name is <tt>null</tt>
	 */
	public RemoteFileDesc(String host, int port, long index, String filename,
						  long size, byte[] clientGUID, int speed, 
						  boolean chat, int quality, boolean browseHost, 
						  LimeXMLDocument xmlDoc, Set<? extends URN> urns,
						  boolean replyToMulticast, boolean firewalled, 
                          String vendor, 
                          Set<? extends IpPort> proxies, long createTime, 
                          int FWTVersion, boolean tlsCapable) {
		this(host,
             port,
             index,
             filename,
             size,
             clientGUID,
             speed,
             chat,
             quality,
             browseHost,
             xmlDoc,
             urns, 
             replyToMulticast,
             firewalled,
             vendor, 
             proxies,
             createTime,
             FWTVersion, 
             null, // this will create a PE to house the data if the host is firewalled
             tlsCapable); 
	}
	
    /** Constructs a RemoteFileDesc using the given PushEndpoint. */
	public RemoteFileDesc(String host, int port, long index, String filename,
	        			long size,int speed,boolean chat, int quality, boolean browseHost,
	        			LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
	        			boolean firewalled, String vendor,long createTime,
	        			PushEndpoint pe) {
        this(host,port,index,filename,size,null,speed,chat,quality,browseHost,xmlDoc,urns,
                replyToMulticast,firewalled,vendor,null,createTime,0,pe, false); // use exising pe
    }
    
    /**
     * Actual constructor.  If the firewalled flag is set and a PE object is passed it is used, if 
     * no PE object is passed a new one is created.
     */
    private RemoteFileDesc (String host, int port, long index, String filename,
            long size, byte[] clientGUID, int speed,boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            boolean firewalled, String vendor, Set<? extends IpPort> proxies, long createTime,
            int FWTVersion, PushEndpoint pe, boolean tlsCapable) {
	    
	    if(!NetworkUtils.isValidPort(port)) {
			throw new IllegalArgumentException("invalid port: "+port);
		} 
		if((speed & 0xFFFFFFFF00000000L) != 0) {
			throw new IllegalArgumentException("invalid speed: "+speed);
		} 
		if(filename == null) {
			throw new NullPointerException("null filename");
		}
		if(filename.equals("")) {
			throw new IllegalArgumentException("cannot accept empty string file name");
		}
		if((size < 0) ) {
			throw new IllegalArgumentException("invalid size: "+size);
		}
		if((index & 0xFFFFFFFF00000000L) != 0) {
			throw new IllegalArgumentException("invalid index: "+index);
		}
        if(host == null) {
            throw new NullPointerException("null host");
        }
        
	    _speed = speed;
		_host = host;
		_port = port;
		_index = index;
		_filename = filename;
		_size = size <= Integer.MAX_VALUE ? (int) size : -1;
        longSize = size;
        _firewalled = firewalled;
		
		if (firewalled) {
            if (pe != null) 
                _pushAddr = pe;
            else {
                try {
                    _pushAddr = new PushEndpoint(clientGUID,proxies,
                        PushEndpoint.PLAIN, FWTVersion, 
                        new IpPortImpl(_host,_port));
                }catch (UnknownHostException uhe) {
                    throw new IllegalArgumentException(uhe);
                }
            }
            
            _clientGUID = _pushAddr.getClientGUID();
        } else 
            _clientGUID = clientGUID;
        
        
		_chatEnabled = chat;
        _quality = quality;
		_browseHostEnabled = browseHost;
		_replyToMulticast = replyToMulticast;
        _vendor = vendor;
        _creationTime = createTime;
        _tlsCapable = tlsCapable;
        
        if(xmlDoc!=null) //not strictly needed
            _xmlDocs = new LimeXMLDocument[] {xmlDoc};
        else
            _xmlDocs = null;
        
		if(urns == null) {
			_urns = Collections.emptySet();
		} else {
			_urns = Collections.unmodifiableSet(urns);
		}
        
        _http11 = ( !_urns.isEmpty() );
	}

    private void readObject(ObjectInputStream stream) 
		throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        //Older downloads.dat files do not have _urns, so _urns will be null
        //(the default Java value).  Hence we also initialize
        //_browseHostEnabled.  See class overview for more details.
        if(_urns == null) {
            _urns = Collections.emptySet();
            _browseHostEnabled= false;
        } else {
            // According to some bug reports, it seems that the
            // Urn Set has some java.io.Files inserted into it.
            // Here we check for this case and remove the offending object.
            Set<URN> newUrns = GenericsUtils.scanForSet(_urns,
                                                        URN.class,
                                                        GenericsUtils.ScanMode.NEW_COPY_REMOVED,
                                                        UrnSet.class);
            
            // if it was converted or recreated while scanning,
            // ensure it's unmodifiable.
            if(_urns != newUrns)
                _urns = Collections.unmodifiableSet(newUrns);
        }
                
		// preserve the invariant that the LimeXMLDocument array either be
		// null or have at least one element
		if(_xmlDocs != null && _xmlDocs.length == 0) {
			_xmlDocs = null;
		}
        // http11 must be set manually, because older clients did not have this
        // field but did have urns.
        _http11 = ( _http11 || !_urns.isEmpty() );
        
        // if we saved any properties, read them now
        if (propertiesMap != null) {
            Boolean tlsCapable = (Boolean)propertiesMap.get(RFDProperties.CONNECT_TYPE.name());
            if(tlsCapable != null)
                _tlsCapable = tlsCapable.booleanValue();
            
            String http = (String)propertiesMap.get(RFDProperties.PUSH_ADDR.name());
            // try the older serialized name if it didn't have the newer one.
            if(http == null)
                http = (String)propertiesMap.get("_pushAddr");
            if (http != null) {
                try {
                    _pushAddr = new PushEndpoint(http);
                    if (!_firewalled) {
                        Assert.silent(false, "deserialized RFD had PE but wasn't firewalled, "+this+" "+_pushAddr);
                        _firewalled = true;
                    }
                } catch (IOException iox) {}
            }
            
            Long size64 = (Long)propertiesMap.get(RFDProperties.LONG_SIZE.name());
            if (size64 == null)
                longSize = _size;
            else
                longSize = size64.longValue();
            // erase the map so it's reconstructed with the most recent
            // values upon the first write.
            propertiesMap = null;
        }
    }
    
    public void setSerializeProxies() {
        _serializeProxies = true;
    }
    
    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (longSize > Integer.MAX_VALUE) {
            initPropertiesMap();
            propertiesMap.put(RFDProperties.LONG_SIZE.name(), longSize);
        } 
        if(_tlsCapable) {
            initPropertiesMap();
            propertiesMap.put(RFDProperties.CONNECT_TYPE.name(), Boolean.TRUE);
        }
        
        if (_serializeProxies && _pushAddr != null) {
            initPropertiesMap();
            // this will also update the PE in case it changed since last serialization
            propertiesMap.put(RFDProperties.PUSH_ADDR.name(), _pushAddr.httpStringValue());
        }
        
        stream.defaultWriteObject();
    }
    
    private void initPropertiesMap() {
        if(propertiesMap == null)
            propertiesMap = new HashMap<String, Serializable>();
    }
    
    /** Returns true if the host supports TLS. */
    public boolean isTLSCapable() {
        return _tlsCapable;
    }
    
    /** Sets whether or not this host is TLS capable. */
    public void setTLSCapable(boolean tlsCapable) {
        _tlsCapable = tlsCapable;
    }
    
    /** 
     * Accessor for HTTP11.
     *
     * @return Whether or not we think this host supports HTTP11.
     */
    public boolean isHTTP11() {
        return _http11;
    }
    
    /**
     * Mutator for HTTP11.  Should be set after connecting.
     */
    public void setHTTP11(boolean http11) {
        _http11 = http11;
    }
    
    /**
     * Returns true if this is a partial source
     */
    public boolean isPartialSource() {
        return (_availableRanges != null);
    }
    
    /**
     * @return whether this rfd points to myself.
     */
    public boolean isMe() {
        return needsPush() ? 
                Arrays.equals(_clientGUID,RouterService.getMyGUID()) :
                    NetworkUtils.isMe(getHost(),getPort());
    }
    /**
     * Accessor for the available ranges.
     */
    public IntervalSet getAvailableRanges() {
        return _availableRanges.clone();
    }

    /**
     * Mutator for the available ranges.
     */
    public void setAvailableRanges(IntervalSet availableRanges) {
        this._availableRanges = availableRanges;
    }
    
    /**
     * updates the push address of the rfd to a new one.
     * This should be done only to update the set of push proxies,
     * features or FWT capability.
     */
    public void setPushAddress(PushEndpoint pe) {
        if (!Arrays.equals(pe.getClientGUID(),this._clientGUID))
                throw new IllegalArgumentException("different clientGUID");
        this._pushAddr=pe;
    }
    
    /**
     * Returns the current failed count.
     */
    public int getFailedCount() {
        return _failedCount;
    }
    
    /**
     * Increments the failed count by one.
     */
    public void incrementFailedCount() {
        _failedCount++;
    }
    
    /**
     * Resets the failed count back to zero.
     */
    public void resetFailedCount() {
        _failedCount = 0;
    }
    
    /**
     * Determines whether or not this RemoteFileDesc was created
     * from an alternate location.
     */
    public boolean isFromAlternateLocation() {
        return "ALT".equals(_vendor);
    }
    
    /**
     * @return true if this host is still busy and should not be retried
     */
    public boolean isBusy() {
        return isBusy(System.currentTimeMillis());
    }
    
    public boolean isBusy(long now) {
        return now < _earliestRetryTime;
    }

    /**
     * @return time to wait until this host will be ready to be retried
     * in seconds
     */
    public int getWaitTime(long now) {
        return (isBusy(now) ? 
                (int) (_earliestRetryTime - now)/1000 + 1:
                0 );
    }

    /**
     * Mutator for _earliestRetryTime. 
     * @param seconds number of seconds to wait before retrying
     */
    public void setRetryAfter(int seconds) {
        if(LOG.isDebugEnabled())
            LOG.debug("setting retry after to be [" + seconds + 
                      "] seconds for " + this);        
        _earliestRetryTime = System.currentTimeMillis() + seconds*1000;
    }
    
    /**
     * The creation time of this file.
     */
    public long getCreationTime() {
        return _creationTime;
    }

	/**
     * @return Returns the _THEXFailed.
     */
    public boolean hasTHEXFailed() {
        return _THEXFailed;
    }

    /**
     * Having THEX with this host is no good. We can get our THEX from anybody,
     * so we won't bother again. 
     */
    public void setTHEXFailed() {
        _THEXFailed = true;
    }
    
    /**
     * Sets this RFD as downloading.
     */
    public void setDownloading(boolean dl) {
        _isDownloading = dl;
    }
    
    /**
     * Determines if this RFD is downloading.
     *
     * @return whether or not this is downloading
     */
    public boolean isDownloading() { return _isDownloading; }

	/**
	 * Accessor for the host ip with this file.
	 *
	 * @return the host ip with this file
	 */
	public final String getHost() {return _host;}

	/**
	 * Accessor for the port of the host with this file.
	 *
	 * @return the file name for the port of the host
	 */
	public final int getPort() {return _port;}

	/**
	 * Accessor for the index this file, which can be <tt>null</tt>.
	 *
	 * @return the file name for this file, which can be <tt>null</tt>
	 */
	public final long getIndex() {return _index;}

	/**
	 * Accessor for the size in bytes of this file.
	 *
	 * @return the size in bytes of this file
	 */
	public final long getSize() {return longSize;}
	
	public final long getFileSize() { return longSize; }

	/**
	 * Accessor for the file name for this file, which can be <tt>null</tt>.
	 *
	 * @return the file name for this file, which can be <tt>null</tt>
	 */
	public final String getFileName() {return _filename;}

	/**
	 * Accessor for the client guid for this file, which can be <tt>null</tt>.
	 *
	 * @return the client guid for this file, which can be <tt>null</tt>
	 */
	public final byte[] getClientGUID() {return _clientGUID;}

	/**
	 * Accessor for the speed of the host with this file, which can be 
	 * <tt>null</tt>.
	 *
	 * @return the speed of the host with this file, which can be 
	 *  <tt>null</tt>
	 */
	public final int getSpeed() {return _speed;}	
    
    public final String getVendor() {return _vendor;}

	public final boolean isChatEnabled() {return _chatEnabled;}
	public final boolean isBrowseHostEnabled() {return _browseHostEnabled;}

	/**
	 * Returns the "quality" of the remote file in terms of firewalled status,
	 * whether or not the remote host has open slots, etc.
	 * 
	 * @return the current "quality" of the remote file in terms of the 
	 *  determined likelihood of the request succeeding
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
        if (_xmlDocs==null)
            return null;
        else
            return _xmlDocs[0];  //can be null
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

	/**
	 * Returns an <tt>URL</tt> instance for this <tt>RemoteFileDesc</tt>.
	 *
	 * @return an <tt>URL</tt> instance for this <tt>RemoteFileDesc</tt>
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
	
    /**
     * Determines whether or not this RFD was a reply to a multicast query.
     *
     * @return <tt>true</tt> if this RFD was in reply to a multicast query,
     *  otherwise <tt>false</tt>
     */
	public final boolean isReplyToMulticast() {
	    return _replyToMulticast;
    }

    /**
     * Determines whether or not this host reported a private address.
     *
     * @return <tt>true</tt> if the address for this host is private,
     *  otherwise <tt>false</tt>.  If the address is unknown, returns
     *  <tt>true</tt>
     *
     * TODO:: use InetAddress in this class for the host so that we don't 
     * have to go through the process of creating one each time we check
     * it it's a private address
     */
	public final boolean isPrivate() {
        return NetworkUtils.isPrivateAddress(_host);
	}
    
    public boolean isFirewalled() {
        return _firewalled;
    }

    /**
     * Accessor for the <tt>Set</tt> of <tt>PushProxyInterface</tt>s for this
     * file -- can be empty, but is guaranteed to be non-null.
     *
     * @return the <tt>Set</tt> of proxy hosts that will accept push requests
     *  for this host -- can be empty
     */
    public final Set<? extends IpPort> getPushProxies() {
    	if (_pushAddr!=null)
    		return _pushAddr.getProxies();
    	else
    		return Collections.emptySet();
    }

    /**
     * @return whether this RFD supports firewall-to-firewall transfer.
     * For this to be true we need to have some push proxies, indication that
     * the host supports FWT and we need to know that hosts' external address.
     */
    public final boolean supportsFWTransfer() {
        
        if (_host.equals(BOGUS_IP) ||
                !NetworkUtils.isValidAddress(_host) || 
                NetworkUtils.isPrivateAddress(_host))
            return false;
        
        return _pushAddr == null ? false : _pushAddr.supportsFWTVersion() > 0;
    }

    /**
     * Creates the _hostData lazily and uses as necessary
     */ 
    public final RemoteHostData getRemoteHostData() {
        if(_hostData == null)
            _hostData = new RemoteHostData(_host, _port, _clientGUID);
        return _hostData;
    }

    /**
     * @return true if I am not a multicast host and have a hash.
     * also, if I am firewalled I must have at least one push proxy,
     * otherwise my port and address need to be valid.
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
                !NetworkUtils.isPrivateAddress(_host) &&
                NetworkUtils.isValidAddress(_host);
        
        return ret;
    }
    
    /**
     * 
     * @return whether a push should be sent tho this rfd.
     */
    public boolean needsPush() {
        
        //if replying to multicast, do a push.
        if ( isReplyToMulticast() )
            return true;
        //Return true if rfd is private or unreachable
        if (isPrivate()) {
            // Don't do a push for magnets in case you are in a private network.
            // Note to Sam: This doesn't mean that isPrivate should be true.
            if (this instanceof URLRemoteFileDesc) 
                return false;
            else  // Otherwise obey push rule for private rfds.
                return true;
        }
        else if (!NetworkUtils.isValidPort(getPort()))
            return true;
        
        else return isFirewalled();
    }
    
    /**
     * 
     * @return the push address.
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
    public boolean equals(Object o) {
		if(o == this) return true;
        if (! (o instanceof RemoteFileDesc))
            return false;
        RemoteFileDesc other=(RemoteFileDesc)o;
        if (! (nullEquals(_host, other._host) && (_port==other._port)) )
            return false;

        if (longSize != other.longSize)
            return false;
        
        if ( (_clientGUID ==null) != (other._clientGUID==null) )
            return false;
        
        if ( _clientGUID!= null &&
                ! ( Arrays.equals(_clientGUID,other._clientGUID)))
            return false;

        if (_urns.isEmpty() && other._urns.isEmpty())
            return nullEquals(_filename, other._filename);
        else
            return _urns.equals(other._urns);
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
	public int hashCode() {
	   if(_hashCode == 0) {
            int result = 17;
            result = (37* result)+_host.hashCode();
            result = (37* result)+_port;
			result = (int)((37* result)+longSize);
            result = (37* result)+_urns.hashCode();
            if (_clientGUID!=null)
                result = (37* result)+(new GUID(_clientGUID)).hashCode();
            _hashCode = result;
        }
		return _hashCode;
	}

    public String toString() {
        return  ("<"+getHost()+":"+getPort()+", "
				 +getFileName().toLowerCase()+">");
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
    
    public void setQueueStatus(int status) {
        _queueStatus = status;
    }
    
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
	
	public void setSpamRating(float rating) {
		_spamRating = rating;
	}
	
	public float getSpamRating() {
		return _spamRating;
	}

    public int getSecureStatus() {
        return _secureStatus;
    }

    public void setSecureStatus(int secureStatus) {
        this._secureStatus = secureStatus;
    }
}
