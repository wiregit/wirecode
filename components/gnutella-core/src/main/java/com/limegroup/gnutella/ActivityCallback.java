package com.limegroup.gnutella;

/**
 *  Interface for connection events to be fed from the router into the 
 *  visual world.
 */
public interface ActivityCallback
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
    public void addConnection(Connection c, int type, int status);

    /**
     *  Handle a removed connection.
     */
    public void removeConnection(Connection c);

    /**
     *  Change the status of a connection
     */
    public void updateConnection(Connection c, int status);

    /**
     *  Add a known host/port
     */
    public void knownHost(Endpoint e);

    /**
     *  Add a query reply to a query screen
     */
    public void handleQueryReply( QueryReply qr );

    /**
     *  Add a query string to the monitor screen
     */
    public void handleQueryString( String query );

    /**
     * A miscellaneous error not fitting in any other category.
     */
    public void error(String message);
}
