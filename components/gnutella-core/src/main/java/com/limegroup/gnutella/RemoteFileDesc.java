package com.limegroup.gnutella;

/**
 * This is a wrapper class for information pertaining to a specific
 * file on a remote host.
 *
 * @author rsoule
 * @file RemoteFileDesc.java
 */

public class RemoteFileDesc {

	private Endpoint _endpoint;
	private String _filename; 
	private int _index;
	private byte[] _clientGUID;
	private int _priority;

	/** 
	 * @param endpoint lsakjdflsjdf
	 * @param index laksjd
	 */
	public RemoteFileDesc(Endpoint endpoint, int index, String filename,
						  byte[] clientGUID, int priority) {
		
		_endpoint = endpoint;
		_index = index;
		_filename = filename;
		_clientGUID = clientGUID;
		_priority = priority;
	}

	/* Accessor Methods */
	public Endpoint getEndpoint() {return _endpoint;}
	public int getIndex() {return _index;}
	public String getFileName() {return _filename;}
	public byte[] getClientGUID() {return _clientGUID;}
	public int getPriority() {return _priority;}

	public void setEndpoint(Endpoint e) {_endpoint = e;}
	public void setIndex(int i) {_index = i;}
	public void setFileName(String name) {_filename = name;}
	public void setClientGUID(byte[] b) {_clientGUID = b;}
	public void setPriority(int p) {_priority = p;}

}
