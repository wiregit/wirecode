package com.limegroup.gnutella;

/**
 * This is a wrapper class for information pertaining to a specific
 * file on a remote host.
 *
 * @author rsoule
 * @file RemoteFileDesc.java
 */

import java.io.Serializable;
import com.sun.java.util.collections.Comparator;
import com.sun.java.util.collections.Comparable;
import com.limegroup.gnutella.gui.GUIConstants;
import com.sun.java.util.collections.Arrays;

public class RemoteFileDesc implements Comparable, Serializable {

	/**
	 * RemoteFileDesc's have a priority associated with 
	 * them that will determine the order in which they 
	 * will be downloaded in the SmartDownloader.  
	 *
	 * There are some rules to the priority that i will 
	 * outline here:
	 *
	 * 1) A non-private file will always be attempted
	 *    before a private file.
	 * 2) A faster connection will be attempted before 
	 *    a slower connection.
	 * 3) No file will be attempted more than the 
	 *    MAX_NUMBER_ATTEMPTS.
	 *
	 */

	/* speed priorities */
	public static final int PRIVATE_MODEM_PRIORITY       = 10;
	public static final int PRIVATE_CABLE_PRIORITY       = 8;
	public static final int PRIVATE_T1_PRIORITY          = 7;
	public static final int PRIVATE_T3_PRIORITY          = 6;

	public static final int PUBLIC_MODEM_PRIORITY        = 5;
	public static final int PUBLIC_CABLE_PRIORITY        = 3;
	public static final int PUBLIC_T1_PRIORITY           = 2;
	public static final int PUBLIC_T3_PRIORITY           = 1;

	/* how many times a download will be attempted */

	private String _host;
	private int _port;
	private String _filename; 
	private int _index;
	private byte[] _clientGUID;
	private int _speed;
	private int _size;

	private int _priority;  
	private int _speed_priority;  
	private int _numAttempts;  

	private boolean _chatEnabled;

	/** 
	 * @param host the host's ip
	 * @param port the host's port
	 * @param index the index of the file that the client sent
	 * @param filename the name of the file
	 * @param clientGUID the unique identifier of the client
	 * @param speed the speed of the connection
	 */
	public RemoteFileDesc(String host, int port, int index, String filename,
						  int size, byte[] clientGUID, int speed, 
						  boolean chat) {
		
		_numAttempts = 0;
		_speed = speed;
		_host = host;
		_port = port;
		_index = index;
		_filename = filename;
		_size = size;
		_clientGUID = clientGUID;
		_chatEnabled = chat;
		calculateSpeedPriority();
		calculatePriority();
	}

	public void print() {
		//  System.out.println(_filename);
//  		System.out.println("    _priority:    " + _priority);
//  		System.out.println("    _numAttempts: " + _numAttempts);
//  		System.out.println("    _speed      : " + _speed);

	}

	public int compareTo(Object obj2) {

		RemoteFileDesc rdf2;
		try {
			rdf2 = (RemoteFileDesc)obj2;
		}
		catch (ClassCastException e) {
			// System.out.println("Class cast exception");
			return 0;  // probably want to go ahead and throw this?
		}

		if ( calculatePriority() > rdf2.calculatePriority() ) {
			return -1;
		}
		else if ( calculatePriority() < rdf2.calculatePriority() ) {
			return 1;
		}

		return 0;  // they are the same

	}



	/* Accessor Methods */
	public String getHost() {return _host;}
	public int getPort() {return _port;}
	public int getIndex() {return _index;}
	public int getSize() {return _size;}
	public String getFileName() {return _filename;}
	public byte[] getClientGUID() {return _clientGUID;}
	public int getSpeed() {return _speed;}

	public int getPriority() {return _priority;}

	public void setHost(String h) {_host = h;}
	public void setPost(int p) {_port = p;}
	public void setIndex(int i) {_index = i;}
	public void setSize(int s) {_size = s;}
	public void setFileName(String name) {_filename = name;}
	public void setClientGUID(byte[] b) {_clientGUID = b;}
	public void setSpeed(int s) {_speed = s;}

	public int getNumAttempts() {return _numAttempts;}
	public void setNumAttempts(int n) {_numAttempts = n;}
	public void incrementNumAttempts() {_numAttempts++;}
	
	public boolean chatEnabled() {return _chatEnabled;}

	public boolean isPrivate() {
		// System.out.println("host: " + _host);
		if (_host == null) return true;
		Endpoint e = new Endpoint(_host, _port);
		return e.isPrivateAddress();
	}

	public int calculatePriority() {
		_priority = _speed_priority + (_numAttempts * 10) ;
		return _priority;
	}

	public int calculateSpeedPriority() {

		if (isPrivate()) {
			if (_speed <= SpeedConstants.MODEM_SPEED_INT) {
				_speed_priority = PRIVATE_MODEM_PRIORITY;
			}
			else if (_speed <= SpeedConstants.CABLE_SPEED_INT) {
				_speed_priority = PRIVATE_CABLE_PRIORITY;
			}
			else if (_speed <= SpeedConstants.T1_SPEED_INT) {
				_speed_priority = PRIVATE_T1_PRIORITY;
			}
			else if (_speed <= SpeedConstants.T3_SPEED_INT) {
				_speed_priority = PRIVATE_T3_PRIORITY;
			}
		}
		
		else {
			if (_speed <= GUIConstants.MODEM_SPEED_INT) {
				_speed_priority = PUBLIC_MODEM_PRIORITY;
			}
			else if (_speed <= GUIConstants.CABLE_SPEED_INT) {
				_speed_priority = PUBLIC_CABLE_PRIORITY;
			}
			else if (_speed <= GUIConstants.T1_SPEED_INT) {
				_speed_priority = PUBLIC_T1_PRIORITY;
			}
			else if (_speed <= GUIConstants.T3_SPEED_INT) {
				_speed_priority = PUBLIC_T3_PRIORITY;
			}
		}
		
		return _speed_priority;
	}
	
	public static class RemoteFileDescComparator 
		implements Comparator {

        /**
         * Primary key: connection speed
         * (Private/public addresses are dealt with through other means.)
         */
		public int compare(Object obj1, Object obj2) {

			RemoteFileDesc rdf1=(RemoteFileDesc)obj1;;
			RemoteFileDesc rdf2=(RemoteFileDesc)obj2;;
            return rdf1.getSpeed()-rdf2.getSpeed();
		}
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
