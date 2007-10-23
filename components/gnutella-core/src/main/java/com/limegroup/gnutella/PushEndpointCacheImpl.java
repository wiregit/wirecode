package com.limegroup.gnutella;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.BitNumbers;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.http.HTTPUtils;

@Singleton
public class PushEndpointCacheImpl implements PushEndpointCache {
    
    /**
     * A mapping from GUID to a GUIDSetWrapper.  This is used to ensure
     * that all PE's will have access to the same PushProxies, even if
     * multiple PE's exist for a single GUID.  Because access to the proxies
     * is referenced from this GUID_PROXY_MAP, the PE will always receive the
     * most up-to-date set of proxies.
     *
     * Insertion to this map must be manually performed, to allow for temporary
     * PE objects that are used to update pre-existing ones.
     *
     * There is no explicit removal from the map -- the Weak nature of it will
     * automatically remove the key/value pairs when the key is garbage
     * collected.  For this reason, all PEs must reference the exact GUID
     * object that is stored in the map -- to ensure that the map will not GC
     * the GUID while it is still in use by a PE.
     *
     * The value is a GUIDSetWrapper (containing a WeakReference to the
     * GUID key as well as the Set of proxies) so that subsequent PEs can 
     * reference the true key object.  A WeakReference is used to allow
     * GC'ing to still work and the map to ultimately remove unused keys.
     */
    private final Map<GUID, PushEndpointCache.CachedPushEndpoint> GUID_PROXY_MAP = 
        Collections.synchronizedMap(new WeakHashMap<GUID, CachedPushEndpoint>());
    
    @Inject
    PushEndpointCacheImpl(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        backgroundExecutor.scheduleWithFixedDelay(new WeakCleaner(),30*1000,30*1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Overwrites the current known push proxies for the host specified
     * by the GUID, using the the proxies as written in the httpString.
     * 
     * @param guid the guid whose proxies to overwrite
     * @param httpString comma-separated list of proxies and possible proxy features
     */
    public void overwriteProxies(byte [] guid, String httpString) {
        Set<IpPort> newSet = new HashSet<IpPort>();
        StringTokenizer tok = new StringTokenizer(httpString,",");
        BitNumbers tlsProxies = null;
        while(tok.hasMoreTokens()) {
            String proxy = tok.nextToken().trim();
            // only read features when we haven't read proxies yet.
            if(newSet.size() == 0 && proxy.startsWith(PushEndpoint.PPTLS_HTTP)) {
                try {
                    String value = HTTPUtils.parseValue(proxy);
                    if(value != null) {
                        try {
                            tlsProxies = new BitNumbers(value);
                        } catch(IllegalArgumentException ignored) {}
                    }
                } catch(IOException invalid) {}
                continue;
            }
            
            boolean tlsCapable = tlsProxies != null && tlsProxies.isSet(newSet.size());
            try {
                newSet.add(NetworkUtils.parseIpPort(proxy, tlsCapable));
            } catch(IOException ohWell){
                tlsProxies = null; // unset, since index may be off.
            }
        }
    
        overwriteProxies(guid, newSet);
    }

    /**
     * Overwrites any stored proxies for the host specified by the guid.
     * 
     * @param guid the guid whose proxies to overwrite
     * @param newSet the proxies to overwrite with
     */
    public void overwriteProxies(byte[] guid, Set<? extends IpPort> newSet) {
        
        GUID g = new GUID(guid);
        CachedPushEndpoint wrapper ;
        synchronized(GUID_PROXY_MAP) {
            wrapper = GUID_PROXY_MAP.get(g);
            if (wrapper==null) {
                wrapper = new CachedPushEndpointImpl(g);
                GUID_PROXY_MAP.put(g, wrapper);
            }
            wrapper.overwriteProxies(newSet);
        }
    }

    /**
     * updates the external address of all PushEndpoints for the given guid
     */
    public void setAddr(byte [] guid, IpPort addr) {
        GUID g = new GUID(guid);
        CachedPushEndpoint current = getCached(g);
        if (current!=null)
            current.setIpPort(addr);
    }

    /**
     * updates the features of all PushEndpoints for the given guid 
     */
    public void setFeatures(byte [] guid,int features) {
    	GUID g = new GUID(guid);
    	CachedPushEndpoint current = getCached(g);
    	if (current!=null)
    		current.setFeatures(features);
    }

    /**
     * Sets the fwt version supported for all PEs pointing to the
     * given client guid.
     */
    public void setFWTVersionSupported(byte[] guid,int version){
    	GUID g = new GUID(guid);
    	CachedPushEndpoint current = getCached(g);
    	if (current!=null)
    		current.setFWTVersion(version);
    }

    public CachedPushEndpoint getCached(GUID guid) {
        synchronized(GUID_PROXY_MAP) {
            return GUID_PROXY_MAP.get(guid);
        }
    }

    public GUID updateProxiesFor(GUID guid, PushEndpoint pushEndpoint, boolean valid) {
        CachedPushEndpoint existing;
        GUID guidRef = null;
        
        synchronized(GUID_PROXY_MAP) {
            existing = GUID_PROXY_MAP.get(guid);

            // try to get a hard ref so that the mapping won't expire
            if (existing!=null)
                guidRef=existing.getGuid();         

            // if we do not have a mapping for this guid, or it just expired,
            // add a new one atomically
            // (we don't care about the proxies of the expired mapping)
            if (existing == null || guidRef==null) {                
                existing = new CachedPushEndpointImpl(guid, pushEndpoint.getFeatures(), pushEndpoint.supportsFWTVersion());
                if (valid)
                    existing.updateProxies(pushEndpoint.getProxies(), true);
                else
                    existing.updateProxies(IpPort.EMPTY_SET, true);
                
                GUID_PROXY_MAP.put(guid, existing);
                return guid;
            }
        }
        
        // if we got here, means we did have a mapping.  no need to
        // hold the map mutex when updating just the set
        existing.updateProxies(pushEndpoint.getProxies(), valid);
        return guidRef;
    }

    public void clear() {
        synchronized (GUID_PROXY_MAP) {
            GUID_PROXY_MAP.clear();
        }
    }
    
    private final class WeakCleaner implements Runnable {
        public void run() {
            GUID_PROXY_MAP.size();
        }
    }
    
    static class CachedPushEndpointImpl implements CachedPushEndpoint {
        private final WeakReference<GUID> _guidRef;
        private Set<IpPort> _proxies;
        private int _features,_fwtVersion;
        private IpPort _externalAddr;
        
        CachedPushEndpointImpl(GUID guid) {
            this(guid,0,0);
        }
        
        CachedPushEndpointImpl(GUID guid,int features, int version) {
            _guidRef = new WeakReference<GUID>(guid);
            _features=features;
            _fwtVersion=version;
        }
        
        public synchronized void updateProxies(Set<? extends IpPort> s, boolean add){
            Set<IpPort> existing = new IpPortSet();
            
            if (s == null)
                s = _proxies;
            
            if (_proxies!=null)
                existing.addAll(_proxies);
            
            if (add)
                existing.addAll(s);
            else
                existing.removeAll(s);
            
            overwriteProxies(existing);
        }
        
        public synchronized void overwriteProxies(Set<? extends IpPort> s) {
            _proxies = Collections.unmodifiableSet(s);
        }
        
        public synchronized Set<IpPort> getProxies() {
            return _proxies != null ? _proxies : IpPort.EMPTY_SET;
        }
        
        public synchronized int getFeatures() {
            return _features;
        }
        
        public synchronized int getFWTVersion() {
            return _fwtVersion;
        }
        
        public synchronized void setFeatures(int features) {
            _features=features;
        }
        
        public synchronized void setFWTVersion(int version){
            _fwtVersion=version;
        }
        
        public synchronized void setIpPort(IpPort addr) {
            _externalAddr = addr;
        }
        
        public synchronized IpPort getIpPort() {
            return _externalAddr;
        }
        
        public GUID getGuid() {
            return _guidRef.get();
        }
    }

}
