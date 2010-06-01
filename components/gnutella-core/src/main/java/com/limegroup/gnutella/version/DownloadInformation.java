package com.limegroup.gnutella.version;

import org.limewire.io.URN;

public interface DownloadInformation {
    
    public URN getUpdateURN();
    public String getTTRoot();
    public String getUpdateCommand();
    public String getUpdateFileName();
    public long getSize();
    
}