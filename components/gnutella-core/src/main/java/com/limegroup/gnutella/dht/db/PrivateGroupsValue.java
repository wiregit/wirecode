package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;

public interface PrivateGroupsValue extends DHTValue{

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
    
    public int getPublicKey();
    
    public byte[] getIPAddress();


}
