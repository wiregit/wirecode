package com.limegroup.gnutella;

/**
 * This is a wrapper class for information pertaining to a specific
 * file on a remote host.
 *
 * @author rsoule
 * @file RemoteFileDesc.java
 */

public class RemoteFileDesc {

	private String _host;
	private int _port;
	private String _filename; 
	private int _index;
	private byte[] _clientGUID;
	private int _speed;
	private int _size;

	/** 
	 * @param host the host's ip
	 * @param port the host's port
	 * @param index the index of the file that the client sent
	 * @param filename the name of the file
	 * @param clientGUID the unique identifier of the client
	 * @param spped the speed of the connection
	 */
	public RemoteFileDesc(String host, int port, int index, String filename,
						  int size, byte[] clientGUID, int speed) {
		
		_host = host;
		_port = port;
		_index = index;
		_filename = filename;
		_size = size;
		_clientGUID = clientGUID;
		_speed = speed;
	}

	/* Accessor Methods */
	public String getHost() {return _host;}
	public int getPort() {return _port;}
	public int getIndex() {return _index;}
	public int getSize() {return _size;}
	public String getFileName() {return _filename;}
	public byte[] getClientGUID() {return _clientGUID;}
	public int getSpeed() {return _speed;}

	public void setHost(String h) {_host = h;}
	public void setPost(int p) {_port = p;}
	public void setIndex(int i) {_index = i;}
	public void setSize(int s) {_size = s;}
	public void setFileName(String name) {_filename = name;}
	public void setClientGUID(byte[] b) {_clientGUID = b;}
	public void setSpeed(int s) {_speed = s;}

}
