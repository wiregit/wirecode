package com.limegroup.gnutella.dht.db;

import java.util.Set;

import org.limewire.io.GUID;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;

class PushEndpointCacheStub implements PushEndpointCache {

    @Override
    public void clear() {
    }

    @Override
    public PushEndpoint getCached(GUID guid) {
        return null;
    }

    @Override
    public PushEndpoint getPushEndpoint(GUID guid) {
        return null;
    }

    @Override
    public void overwriteProxies(byte[] guid, Set<? extends IpPort> newSet) {
    }

    @Override
    public void overwriteProxies(byte[] guid, String httpString) {
    }

    @Override
    public void removePushProxy(byte[] bytes, IpPort pushProxy) {
    }

    @Override
    public void setAddr(byte[] guid, IpPort addr) {
    }

    @Override
    public void setFWTVersionSupported(byte[] guid, int version) {
    }

    @Override
    public GUID updateProxiesFor(GUID guid, PushEndpoint pushEndpoint, boolean valid) {
        return null;
    }
}
