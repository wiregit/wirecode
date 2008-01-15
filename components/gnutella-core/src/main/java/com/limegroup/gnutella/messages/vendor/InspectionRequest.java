package com.limegroup.gnutella.messages.vendor;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.Message;

public interface InspectionRequest extends Message {
    
    static final int VERSION = 1;

    public String[] getRequestedFields();

    public boolean requestsTimeStamp();

    public void setGUID(GUID g);

    public int getVersion();
    
    public byte [] getGUID();
    
    public IpPort getReturnAddress();
    
    public long getRoutableVersion();
    
    public boolean supportsEncoding();

}