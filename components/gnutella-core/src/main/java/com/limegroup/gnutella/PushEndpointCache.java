package com.limegroup.gnutella;

import java.util.Set;

import org.limewire.io.IpPort;

public interface PushEndpointCache {

    void clear();
    
    CachedPushEndpoint getCached(GUID guid);    
    
    /**
     * Overwrites the current known push proxies for the host specified
     * by the GUID, using the the proxies as written in the httpString.
     * 
     * @param guid the guid whose proxies to overwrite
     * @param httpString comma-separated list of proxies and possible proxy features
     */
    public void overwriteProxies(byte [] guid, String httpString);

    /**
     * Overwrites any stored proxies for the host specified by the guid.
     * 
     * @param guid the guid whose proxies to overwrite
     * @param newSet the proxies to overwrite with
     */
    public void overwriteProxies(byte[] guid, Set<? extends IpPort> newSet);

    /**
     * updates the external address of all PushEndpoints for the given guid
     */
    public void setAddr(byte [] guid, IpPort addr);

    /**
     * updates the features of all PushEndpoints for the given guid 
     */
    public void setFeatures(byte [] guid, int features);

    /**
     * Sets the fwt version supported for all PEs pointing to the
     * given client guid.
     */
    public void setFWTVersionSupported(byte[] guid, int version);

    /**
     * Updates the PushEndpoint to match what is in the cache.
     * If there is nothing in the cache, the cache is set to match this endpoint.
     * If the endpoint is valid, the proxies in it are added to those already cached.
     * If it is invalid, the proxies are removed from the cached version.
     */
    public GUID updateProxiesFor(GUID guid, PushEndpoint pushEndpoint, boolean valid);

    public interface CachedPushEndpoint {
        /**
         * Adds or removes the given set of ip ports depending on <code>add</code>.
         * 
         * @param add if false removes <code>proxies</code> otherwise adds them
         */
        void updateProxies(Set<? extends IpPort> proxies, boolean add);
        /**
         * Sets a new set of proxies overwriting the exiting one. 
         */
        void overwriteProxies(Set<? extends IpPort> proxies) ;        
        Set<IpPort> getProxies();        
        int getFeatures();        
        int getFWTVersion();        
        void setFeatures(int features);        
        void setFWTVersion(int version);        
        void setIpPort(IpPort addr);        
        IpPort getIpPort();        
        GUID getGuid();
    }


}