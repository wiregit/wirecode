package com.limegroup.gnutella.downloader;

import java.util.Collection;
import java.util.Iterator;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.DownloadSettings;

/**
 * A class that ranks sources for a download. 
 * 
 * It uses a factory pattern to provide the best ranker based on system
 * conditions.
 */
public abstract class SourceRanker {

    public void addToPool(Collection hosts) {
        for (Iterator iter = hosts.iterator(); iter.hasNext();) {
            RemoteFileDesc host = (RemoteFileDesc) iter.next();
            addToPool(host);
        }
    }
    
    /**
     * @param host the host that the ranker should consider
     */
    public abstract void addToPool(RemoteFileDesc host);
	
    /**
     * @return whether the ranker has any more potential sources
     */
	public abstract boolean hasMore();
    
    /**
     * @return the source that should be tried next
     */
    public abstract RemoteFileDesc getBest();
    
    /**
     * @return the collection of hosts that can be shared with other rankers
     */
    protected abstract Collection getShareableHosts();
    
    /**
     * stops the ranker.
     */
    public abstract void stop();
    
    /**
     * @return a ranker appropriate for our system's capabilities.
     */
    public static SourceRanker getAppropriateRanker() {
        if (RouterService.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue())
            return new PingRanker();
        else 
            return new LegacyRanker();
    }
    
    /**
     * @param original the current ranker that we use
     * @return the ranker that should be used
     */
    public static SourceRanker getAppropriateRanker(SourceRanker original) {
        SourceRanker better;
        if (RouterService.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue()) {
            if (original instanceof PingRanker)
                return original;
            better = new PingRanker();
            better.addToPool(original.getShareableHosts());
        }else {
            if (original instanceof LegacyRanker)
                return original;
            better = new LegacyRanker();
            better.addToPool(original.getShareableHosts());
        }
        
        return better;
    }
}
