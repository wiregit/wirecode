package com.limegroup.gnutella;

import java.io.Serializable;
import com.sun.java.util.collections.Comparator;
import com.sun.java.util.collections.Comparable;
import com.sun.java.util.collections.Arrays;

/**
 * A reference to a single file on a remote machine.  In this respect
 * RemoteFileDesc is similar to a URL, but it contains Gnutella-
 * specific data as well, such as the server's 16-byte GUID.
 */
public class RemoteFileDesc implements Serializable {

	private String _host;
	private int _port;
	private String _filename; 
	private int _index;
	private byte[] _clientGUID;
	private int _speed;
	private int _size;
	private boolean _chatEnabled;
    private int _quality;

	/** 
	 * @param host the host's ip
	 * @param port the host's port
	 * @param index the index of the file that the client sent
	 * @param filename the name of the file
	 * @param clientGUID the unique identifier of the client
	 * @param speed the speed of the connection
     * @param quality the quality of the connection, where 0 is the
     *  worst and 3 is the best.  (This is the same system as in the
     *  GUI but on a 0 to N-1 scale.)
	 */
	public RemoteFileDesc(String host, int port, int index, String filename,
						  int size, byte[] clientGUID, int speed, 
						  boolean chat, int quality) {	   
		_speed = speed;
		_host = host;
		_port = port;
		_index = index;
		_filename = filename;
		_size = size;
		_clientGUID = clientGUID;
		_chatEnabled = chat;
        _quality = quality;
	}

	/* Accessor Methods */
	public String getHost() {return _host;}
	public int getPort() {return _port;}
	public int getIndex() {return _index;}
	public int getSize() {return _size;}
	public String getFileName() {return _filename;}
	public byte[] getClientGUID() {return _clientGUID;}
	public int getSpeed() {return _speed;}	
	public boolean chatEnabled() {return _chatEnabled;}
    public int getQuality() {return _quality;}

	public boolean isPrivate() {
		// System.out.println("host: " + _host);
		if (_host == null) return true;
		Endpoint e = new Endpoint(_host, _port);
		return e.isPrivateAddress();
	}


	/** Returns true iff o is a RemoteFileDesc with the same value as this.
     *  Priority and number of attempts is ignored in doing the comparison! */
    public boolean equals(Object o) {
        if (! (o instanceof RemoteFileDesc))
            return false;
        RemoteFileDesc other=(RemoteFileDesc)o;
        
        return _host.equals(other._host)
            && _port==other._port
            && _filename.equals(other._filename)
            && _index==other._index 
            && Arrays.equals(_clientGUID, other._clientGUID)
            && _speed==other._speed
            && _size==other._size;
    }

    public String toString() {
        return  "<"+getHost()+":"+getPort()+", "
               +getFileName()+"/"+getSize()+", "
               +getSpeed()+">";
    }
}
