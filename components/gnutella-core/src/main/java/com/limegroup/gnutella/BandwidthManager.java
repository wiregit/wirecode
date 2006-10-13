package com.limegroup.gnutella;

import com.limegroup.gnutella.io.NBThrottle;
import com.limegroup.gnutella.io.Throttle;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;

public class BandwidthManager {

	private final Throttle UP_TCP, DOWN_TCP;
	
	public BandwidthManager() {
		UP_TCP = new NBThrottle(true,0);
		DOWN_TCP = new NBThrottle(false,0);
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
		UP_TCP.setRate(UploadManager.getUploadSpeed());
	}
	
	public Throttle getReadThrottle() {
	    applyDownloadRate();
        return DOWN_TCP;
    }
    
    public Throttle getWriteThrottle() {
        applyUploadRate();
        return UP_TCP;
	}
}
