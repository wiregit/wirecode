package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.IpPort;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RouterService;

/**
 * An implementation of PushProxiesDHTValue for the localhost
 */
class PushProxiesValueForSelf extends AbstractPushProxiesValue {
    
    private static final long serialVersionUID = -3222117316287224578L;
    private final PushEndpoint self;
    private final NetworkManager networkManager;
    
    public PushProxiesValueForSelf(NetworkManager networkManager, PushEndpointFactory pushEndpointFactory) {
        super(AbstractPushProxiesValue.VERSION);
        this.networkManager = networkManager;
        this.self = pushEndpointFactory.createForSelf();
    }

    public boolean isEmpty() {
        return false;
    }

    public DHTValueType getValueType() {
        return AbstractPushProxiesValue.PUSH_PROXIES;
    }

    public Version getVersion() {
        return AbstractPushProxiesValue.VERSION;
    }

    public byte[] getValue() {
        return AbstractPushProxiesValue.serialize(this);
    }

    public void write(OutputStream out) throws IOException {
        out.write(getValue());
    }

    public byte[] getGUID() {
        return RouterService.getMyGUID();
    }
    
    public byte getFeatures() {
        return self.getFeatures();
    }

    public int getFwtVersion() {
        return self.supportsFWTVersion();
    }
    
    public int getPort() {
        return networkManager.getPort();
    }

    public Set<? extends IpPort> getPushProxies() {
        return self.getProxies();
    }
    
    public BitNumbers getTLSInfo() {
        return AbstractPushProxiesValue.getNumbersFromProxies(getPushProxies());
    }
}