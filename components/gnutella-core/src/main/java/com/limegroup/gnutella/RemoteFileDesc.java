package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.xml.*;
import org.xml.sax.*;

/**
 * A reference to a single file on a remote machine.  In this respect
 * RemoteFileDesc is similar to a URL, but it contains Gnutella-
 * specific data as well, such as the server's 16-byte GUID.
 */
public class RemoteFileDesc implements Serializable {
    private static final long serialVersionUID = 6619479308616716538L;

	private final String _host;
	private final int _port;
	private final String _filename; 
	private final long _index;
	private final byte[] _clientGUID;
	private final int _speed;
	private final int _size;
	private final boolean _chatEnabled;
    private final int _quality;
    private final LimeXMLDocument _xmlDoc;
	private final Set _urns;
	private final boolean _browseHostEnabled;

	/**
	 * Constant for an empty, unmodifiable <tt>Set</tt>.  This is necessary
	 * because Collections.EMPTY_SET is not serializable in the collections
	 * 1.1 implementation.
	 */
	private static final Set EMPTY_SET = 
		Collections.unmodifiableSet(new HashSet());

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
	 */
	public RemoteFileDesc(String host, int port, long index, String filename,
						  int size, byte[] clientGUID, int speed, 
						  boolean chat, int quality, boolean browseHost, 
						  LimeXMLDocument xmlDoc, Set urns) {
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
		_xmlDoc = xmlDoc;
		if(urns == null) {
			_urns = EMPTY_SET;
		}
		else {
			_urns = Collections.unmodifiableSet(urns);
		}
	}


	/* Accessor Methods */
	public final String getHost() {return _host;}
	public final int getPort() {return _port;}
	public final long getIndex() {return _index;}
	public final int getSize() {return _size;}
	public final String getFileName() {return _filename;}
	public final byte[] getClientGUID() {return _clientGUID;}
	public final int getSpeed() {return _speed;}	
	public final boolean chatEnabled() {return _chatEnabled;}
	public final boolean browseHostEnabled() {return _browseHostEnabled;}
    public final int getQuality() {return _quality;}

	/**
	 * Returns the <tt>LimeXMLDocument</tt> for this <tt>RemoteFileDesc</tt>, 
	 * which can be <tt>null</tt>.
	 *
	 * @return the <tt>LimeXMLDocument</tt> for this <tt>RemoteFileDesc</tt>, 
	 * which can be <tt>null</tt>.
	 */
    public final LimeXMLDocument getXMLDoc() {
		return _xmlDoc;
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
			if(urn.isSHA1()) {
				return urn;
			}
		}

		return null;
	}

	public final boolean isPrivate() {
		if (_host == null) return true;
		Endpoint e = new Endpoint(_host, _port);
		return e.isPrivateAddress();
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
	 */
    public boolean equals(Object o) {
		if(o == this) return true;
        if (! (o instanceof RemoteFileDesc))
            return false;
        RemoteFileDesc other=(RemoteFileDesc)o;        
		return ((_host == null ? other._host == null : 
				 _host.equals(other._host)) &&
				(_port == other._port) &&
				(_filename == null ? other._filename == null :
				 _filename.equals(other._filename)) &&
				(_index == other._index) &&
				(_clientGUID == null ? other._clientGUID == null :
				 Arrays.equals(_clientGUID, other._clientGUID)) &&
				(_speed == other._speed) &&
				(_size == other._size) &&
				(_xmlDoc == null ? other._xmlDoc == null :
				 _xmlDoc.equals(other._xmlDoc)) &&
				(_urns == null ? other._urns == null :
				 _urns.equals(other._urns)));		
    }

    public String toString() {
        return  "<"+getHost()+":"+getPort()+", "
               +getFileName()+"/"+getSize()+", "
               +getSpeed()+">";
    }
}
