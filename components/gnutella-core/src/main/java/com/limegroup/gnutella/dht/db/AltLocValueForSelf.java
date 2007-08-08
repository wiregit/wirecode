package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.security.TigerTree;
import com.limegroup.gnutella.settings.SSLSettings;

/**
 * An AltLocValue for the localhost
 */
class AltLocValueForSelf extends AbstractAltLocValue {
    
    private static final long serialVersionUID = 8101291047246461600L;
    
    private final long fileSize;
    
    private final byte[] ttroot;
    
    private final NetworkManager networkManager;    
    
    public AltLocValueForSelf(long fileSize, byte[] ttroot, NetworkManager networkManager) {
        super(AbstractAltLocValue.VERSION);
        
        if (fileSize < 0L) {
            throw new IllegalArgumentException("Illegal fileSize: " + fileSize);
        }
        
        if (ttroot != null && ttroot.length != TigerTree.HASHSIZE) {
            throw new IllegalArgumentException("Illegal ttroot length: " + ttroot.length);
        }
        
        this.fileSize = fileSize;
        this.ttroot = ttroot;
        this.networkManager = networkManager;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getValue()
     */
    public byte[] getValue() {
        return AbstractAltLocValue.serialize(this);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#write(java.io.OutputStream)
     */
    public void write(OutputStream out) throws IOException {
        out.write(getValue());
    }

    @Override
    public int getPort() {
        return networkManager.getPort();
    }
    
    @Override
    public byte[] getGUID() {
        return ProviderHacks.getApplicationServices().getMyGUID();
    }
    
    @Override
    public boolean isFirewalled() {
        return !networkManager.acceptedIncomingConnection();
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public byte[] getRootHash() {
        return ttroot;
    }
    
    @Override
    public boolean supportsTLS() {
        return SSLSettings.isIncomingTLSEnabled();
    }
}