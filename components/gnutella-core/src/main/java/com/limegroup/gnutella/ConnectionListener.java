package com.limegroup.gnutella;

/**
 * An interface for receiving notifications from Connections.
 * Typically, this interface is not implemented directly.  Instead,
 * its subinterface ActivityCallback is implemented.
 *
 * @author Ron Vogl
 */
interface ConnectionListener
{
    /**
     *  Handle a new connection.
     */
    void connectionInitializing(Connection c);

    /**
     *  Mark a connection as initialized
     */
    void connectionInitialized(Connection c);

    /**
     *  Handle a removed connection.
     */
    void connectionClosed(Connection c);
}
