package com.limegroup.gnutella.version;

import org.limewire.io.URNImpl;

public interface DownloadInformation {
    
    public URNImpl getUpdateURN();
    public String getTTRoot();
    public String getUpdateCommand();
    public String getUpdateFileName();
    public long getSize();
    
}