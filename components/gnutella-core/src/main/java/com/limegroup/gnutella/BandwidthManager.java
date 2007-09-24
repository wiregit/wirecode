package com.limegroup.gnutella;

import java.net.Socket;

import org.limewire.nio.Throttle;

public interface BandwidthManager {

	public void applyRate();

	public void applyUploadRate();
	
	public Throttle getReadThrottle();
        
    public Throttle getWriteThrottle();
    
    public Throttle getWriteThrottle(Socket socket);

}
