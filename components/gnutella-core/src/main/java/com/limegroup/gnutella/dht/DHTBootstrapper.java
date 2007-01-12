package com.limegroup.gnutella.dht;

import java.net.SocketAddress;

/**
 * The DHT Bootstrapper bootstraps this DHT node to the network
 */
public interface DHTBootstrapper {
    
    /**
     * Bootstraps this node to the network.
     */
    public void bootstrap();
    
    /**
     * Adds a host to the list of bootstrap hosts 
     * used to bootstrap to the network
     * 
     * @param hostAddress The <tt>SocketAddress</tt> of the bootstrap host
     */
    public void addBootstrapHost(SocketAddress hostAddress);
    
    /**
     * If the bootstrapper is waiting for nodes, pings this host 
     * in order to acquire DHT bootstrap hosts
     * 
     * @param hostAddress The <tt>SocketAddress</tt> of the host to ping
     */
    public void addPassiveNode (SocketAddress hostAddress);
    
    /**
     * Stops the bootstrap process
     */
    public void stop();
    
    /**
     * Returns wether or not the bootstrapper is waiting for 
     * nodes to bootstrap from.
     */
    public boolean isWaitingForNodes();
}
