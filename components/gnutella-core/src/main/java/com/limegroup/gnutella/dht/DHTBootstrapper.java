package com.limegroup.gnutella.dht;

import java.net.SocketAddress;

import com.limegroup.mojito.MojitoDHT;

public interface DHTBootstrapper {
    
    /**
     * Bootstraps the given dht to the network.
     * 
     * @param dht The MojitoDHT to bootstrap
     */
    public void bootstrap(MojitoDHT dht);
    
    /**
     * Adds a host to the list of bootstrap hosts 
     * used to bootstrap to the network
     * 
     * @param hostAddress The <tt>SocketAddress</tt> of the bootstrap host
     */
    public void addBootstrapHost(SocketAddress hostAddress);
    
    /**
     * Stops the bootstrap process
     */
    public void stop();
    
    /**
     * Returns wether or not the bootstrapper is waiting for 
     * nodes to bootstrap from.
     */
    public boolean isWaitingForNodes();
    
    /**
     * Returns wether the bootstrapper is currently bootstrapping from the 
     * persisted routing table.
     */
    public boolean isBootstrappingFromRT();
}