package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.*;
import java.net.*;

/**
 * A reference to a single file on a remote machine.  In this respect
 * RemoteFileDesc is similar to a URL, but it contains Gnutella-
 * specific data as well, such as the server's 16-byte GUID.<p>
 *
 * This class is serialized to disk as part of the downloads.dat file.  Hence
 * you must be very careful before making any changes.  Deleting or changing the
 * types of fields is DISALLOWED.  Adding field a F is acceptable as long as the
 * readObject() method of this initializes F to a reasonable value when
 * reading from older files where the fields are not present.  This is exactly
 * what we do with _urns and _browseHostEnabled.  On the other hand, older
 * version of LimeWire will simply discard any extra fields F if reading from a
 * newer serialized file.  
 */
public class RemoteFileDesc implements Serializable {
    private static final long serialVersionUID = 6619479308616716538L;
    
    private static final int COPY_INDEX = Integer.MAX_VALUE;

	private final String _host;
	private final int _port;
	private final String _filename; 
	private final long _index;
	private final byte[] _clientGUID;
	private final int _speed;
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
	private Set /* of URN*/  _urns;

    /**
     * Boolean indicating whether or not the remote host has browse host 
     * enabled.
     */
	private boolean _browseHostEnabled;

    private boolean _firewalled;
    private String _vendor;
    private long _timestamp;
    
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
    
    /**
     * The <tt>Set</tt> of proxies for this host -- can be empty.
     */
    private transient Set _proxies;
		
    /**
     * The List of available ranges.  Should not be serialized.
     * This is not an IntervalSet for a reason:
     * We do not want to compact overlapping ranges into a single range,
     * because the remote host may not understand them as such.
     * We must act off the ranges as they're listed to us.
     * For LimeWires, the ranges will always be as compact as possible,
     * but for other vendors this is unknown.
     *
     * This is NOT SERIALIZED.
     */
    private transient List _availableRanges = null;
    
    /**
     * The number of times this download has failed while attempting
     * to transfer data.
     */
    private transient int _failedCount = 0;
    
    /**
     * Constructs a new RemoteFileDesc exactly like the other one,
     * but with a different remote host.
     *
     * It is okay to use the same internal structures
     * for URNs because the Set is immutable.
     */
    public RemoteFileDesc(RemoteFileDesc rfd, Endpoint ep) {
        this( ep.getHostname(),             // host
              ep.getPort(),                 // port
              COPY_INDEX,                   // index (unknown)
              rfd.getFileName(),            // filename
              rfd.getSize(),                // filesize
              DataUtils.EMPTY_GUID,         // client GUID
              0,                            // speed
              false,                        // chat capable
              2,                            // quality
              false,                        // browse hostable
              rfd.getXMLDoc(),              // xml doc
              rfd.getUrns(),                // urns
              false,                        // reply to MCast
              false,                        // is firewalled
              AlternateLocation.ALT_VENDOR, // vendor
              System.currentTimeMillis(),   // timestamp
              DataUtils.EMPTY_SET );        // push proxies
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
     * @param xmlDocs the array of XML documents pertaining to this file
	 * @param browseHost specifies whether or not the remote host supports
	 *  browse host
	 * @param xmlDoc the <tt>LimeXMLDocument</tt> for the response
	 * @param urns the <tt>Set</tt> of <tt>URN</tt>s for the file
	 * @param replyToMulticast true if its from a reply to a multicast query
	 *
	 * @throws <tt>IllegalArgumentException</tt> if any of the arguments are
	 *  not valid
     * @throws <tt>NullPointerException</tt> if the host argument is 
     *  <tt>null</tt> or if the file name is <tt>null</tt>
	 */
	public RemoteFileDesc(String host, int port, long index, String filename,
						  int size, byte[] clientGUID, int speed, 
						  boolean chat, int quality, boolean browseHost, 
						  LimeXMLDocument xmlDoc, Set urns,
						  boolean replyToMulticast, boolean firewalled, 
                          String vendor, long timestamp,
                          Set proxies) {
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
		if((size & 0xFFFFFFFF00000000L) != 0) {
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
		_size = size;
		_clientGUID = clientGUID;
		_chatEnabled = chat;
        _quality = quality;
		_browseHostEnabled = browseHost;
		_replyToMulticast = replyToMulticast;
        _firewalled = firewalled;
        _vendor = vendor;
        _timestamp = timestamp;
        if(proxies == null) {
            _proxies = DataUtils.EMPTY_SET;
        } else {
            _proxies = Collections.unmodifiableSet(proxies);
        }
        if(xmlDoc!=null) //not strictly needed
            _xmlDocs = new LimeXMLDocument[] {xmlDoc};
        else
            _xmlDocs = null;
		if(urns == null) {
			_urns = DataUtils.EMPTY_SET;
		}
		else {
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
            _urns = DataUtils.EMPTY_SET;
            _browseHostEnabled= false;
        }
        if(_proxies == null) {
            _proxies = DataUtils.EMPTY_SET;
        }
		// preserve the invariant that the LimeXMLDocument array either be
		// null or have at least one element
		if(_xmlDocs != null && _xmlDocs.length == 0) {
			_xmlDocs = null;
		}
        // http11 must be set manually, because older clients did not have this
        // field but did have urns.
        _http11 = ( _http11 || !_urns.isEmpty() );
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
     * Accessor for the available ranges.
     */
    public List getAvailableRanges() {
        return _availableRanges;
    }

    /**
     * Mutator for the available ranges.
     */
    public void setAvailableRanges(List availableRanges) {
        this._availableRanges = availableRanges;
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
	public final int getSize() {return _size;}

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

	public final boolean chatEnabled() {return _chatEnabled;}
	public final boolean browseHostEnabled() {return _browseHostEnabled;}

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
    public final LimeXMLDocument getXMLDoc() {
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
	public final Set getUrns() {
		return _urns;
	}

	/**
	 * Accessor for the SHA1 URN for this <tt>RemoteFileDesc</tt>.
	 *
	 * @return the SHA1 <tt>URN</tt> for this <tt>RemoteFileDesc</tt>, or 
	 *  <tt>null</tt> if there is none
	 */
	public final URN getSHA1Urn() {
		Iterator iter = _urns.iterator(); 
		while(iter.hasNext()) {
			URN urn = (URN)iter.next();
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

    /**
     * Accessor for the <tt>Set</tt> of <tt>PushProxyInterface</tt>s for this
     * file -- can be empty, but is guaranteed to be non-null.
     *
     * @return the <tt>Set</tt> of proxy hosts that will accept push requests
     *  for this host -- can be empty
     */
    public final Set getPushProxies() {
        return _proxies;
    }

    /**
     * @return true if I am not (firewalled, multicast host, have private IP)
     *         and i do have a valid port & address.
     */
    public final boolean isAltLocCapable() {
        return getSHA1Urn() != null &&
               !_replyToMulticast &&
               !_firewalled &&
               NetworkUtils.isValidPort(_port) &&
               !NetworkUtils.isPrivateAddress(_host) &&
               NetworkUtils.isValidAddress(_host);
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
	 * Dynamic values such as _http11, _proxies and _availableSources
	 * are not checked here, as they can change and still be considered
	 * the same "remote file".
	 */
    public boolean equals(Object o) {
		if(o == this) return true;
        if (! (o instanceof RemoteFileDesc))
            return false;
        RemoteFileDesc other=(RemoteFileDesc)o;        
        return nullEquals(_host, other._host) &&
               _port == other._port &&
               nullEquals(_filename, other._filename) &&
               _index == other._index &&
               byteArrayEquals(_clientGUID, other._clientGUID) &&
               _speed == other._speed &&
               _size == other._size &&
               nullEquals(getXMLDoc(), other.getXMLDoc()) &&
               nullEquals(_urns, other._urns);
    }
    
    private boolean nullEquals(Object one, Object two) {
        return one == null ? two == null : one.equals(two);
    }
    
    private boolean byteArrayEquals(byte[] one, byte[] two) {
        return one == null ? two == null : Arrays.equals(one, two);
    }

	//TODO:: ADD HASHCODE OVERRIDE

    public String toString() {
        return  ("<"+getHost()+":"+getPort()+", "
				 +getFileName()+">");
    }
}
