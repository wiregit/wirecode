package com.limegroup.gnutella;

import java.net.Socket;

import org.limewire.nio.NBThrottle;
import org.limewire.nio.Throttle;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.udpconnect.UDPConnection;

public class BandwidthManager {

	private final Throttle UP_TCP, DOWN_TCP, UP_UDP;
	
	public BandwidthManager() {
		UP_TCP = new NBThrottle(true,0);
		DOWN_TCP = new NBThrottle(false,0);
		UP_UDP = new NBThrottle(true, 0);
	}
	
	public void applyRate() {
		applyDownloadRate();
		applyUploadRate();
	}
	
	private void applyDownloadRate() {
		float downloadRate = Float.MAX_VALUE;
		int downloadThrottle = DownloadSettings.DOWNLOAD_SPEED.getValue();
		
		if ( downloadThrottle < 100 ) {
			downloadRate = ((downloadThrottle/100.f)*
					(ConnectionSettings.CONNECTION_SPEED.getValue()/8.f))*1024.f;
		}
		DOWN_TCP.setRate(downloadRate);
	}
	
	public void applyUploadRate() {
		UP_TCP.setRate(RouterService.getUploadManager().getUploadSpeed());
		UP_UDP.setRate(RouterService.getUploadManager().getUploadSpeed());
	}
	
	public Throttle getReadThrottle() {
	    applyDownloadRate();
        return DOWN_TCP;
    }
    
    public Throttle getWriteThrottle() {
        applyUploadRate();
        return UP_TCP;
	}
    
    public Throttle getWriteThrottle(Socket socket) {
        applyUploadRate();
        return (socket instanceof UDPConnection) ? UP_UDP : UP_TCP;
    }

}
