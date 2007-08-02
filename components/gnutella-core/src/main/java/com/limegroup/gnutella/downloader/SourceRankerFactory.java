package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.settings.DownloadSettings;

public class SourceRankerFactory {
    
    private final NetworkManager networkManager;
    
    public SourceRankerFactory(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /**
     * @return a ranker appropriate for our system's capabilities.
     */
    public SourceRanker getAppropriateRanker() {
        if (networkManager.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue())
            return new PingRanker(networkManager);
        else 
            return new LegacyRanker();
    }

    /**
     * @param original the current ranker that we use
     * @return the ranker that should be used.  If different than the current one,
     * the current one is stopped.
     */
    public SourceRanker getAppropriateRanker(SourceRanker original) {
        if(original == null)
            return getAppropriateRanker();
        
        SourceRanker better;
        if (networkManager.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue()) {
            if (original instanceof PingRanker)
                return original;
            better = new PingRanker(networkManager);
        }else {
            if (original instanceof LegacyRanker)
                return original;
            better = new LegacyRanker();
        }
        
        better.setMeshHandler(original.getMeshHandler());
        better.addToPool(original.getShareableHosts());
        original.stop();
        return better;
    }
}
