package com.limegroup.gnutella;

/**
 *  Interface for connection events to be fed from the router into the 
 *  visual world.
 */
public interface ConnectionCallback
{	
	/**  Flag an outgoing connection type */
	public static final int CONNECTION_OUTGOING = 1;
	/**  Flag an incoming connection type */
	public static final int CONNECTION_INCOMING = 2;

	/**  The connection is being setup */
	public static final int STATUS_CONNECTING = 1;
	/**  The connection is established */
	public static final int STATUS_CONNECTED = 2;

    /**
     *  Handle a new connection.
     */
    public void addConnection(String host, int port, int type, int status);

    /**
     *  Handle a removed connection.
     */
    public void removeConnection(String host, int port);

    /**
     *  Change the status of a connection
     */
    public void updateConnection(String host, int port, int status);

}
