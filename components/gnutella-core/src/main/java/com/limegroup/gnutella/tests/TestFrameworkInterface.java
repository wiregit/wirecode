package com.limegroup.gnutella.tests;

import java.rmi.*;

/**
 * RMI Interface that is used to control the functions of the Test Framework
 * Clients.
 */
public interface TestFrameworkInterface extends Remote
{
    public static final String SERVICE_NAME = "TestFrameworkClient";

    /**
     * Connect to another test framework client.
     */
    public void connect(String host, int port) throws RemoteException;

    /**
     * Disconnect from another test framework client.  Returns true if 
     * successfully disconnected.
     */
    public boolean disconnectFrom(String host, int port) throws RemoteException;

    /**
     * Disconnects from all test framework clients.
     */
    public void disconnect() throws RemoteException;

    /**
     * Prints all the active connections (both incoming and outgoing).
     */
    public String[] getCurrentConnections() throws RemoteException;

    /**
     * Returns the number of current connections.
     */
    public int getNumOfConnections() throws RemoteException;

    /**
     * Returns network statistics such as number of cached hosts, number of
     * reserve cache hosts, pings received, pings sent, etc.
     */
    public int[] getNetworkStatistics() throws RemoteException;
}
