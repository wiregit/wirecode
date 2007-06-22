package com.limegroup.gnutella.altlocs;

import java.io.IOException;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.URN;

public class DirectDHTAltLoc extends DirectAltLoc {

    private final long fileSize;
    
    private final byte[] ttroot;
    
    public DirectDHTAltLoc(IpPort address, URN sha1, long fileSize, byte[] ttroot) throws IOException {
        super(address, sha1);
        
        this.fileSize = fileSize;
        this.ttroot = ttroot;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public byte[] getRootHash() {
        return ttroot;
    }
}
