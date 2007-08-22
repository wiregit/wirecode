package com.limegroup.gnutella.dht.db;

import java.io.Serializable;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.IpPort;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;

public interface PushProxiesValue extends DHTValue, Serializable  {

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getValueType()
     */
    public DHTValueType getValueType();

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getVersion()
     */
    public Version getVersion();

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#size()
     */
    public int size();

    /**
     * The Client ID of the Gnutella Node
     */
    public byte[] getGUID();

    /**
     * The Port number of the Gnutella Node
     */
    public int getPort();

    /**
     * The supported features of the Gnutella Node
     */
    public byte getFeatures();

    /**
     * The version of the firewalls-to-firewall 
     * transfer protocol
     */
    public int getFwtVersion();

    /**
     * A Set of Push Proxies of the Gnutella Node
     */
    public Set<? extends IpPort> getPushProxies();

    /**
     * @return BitNumbers for tls status of push proxies,
     * null if none are tls-capable.
     */
    public BitNumbers getTLSInfo();

}