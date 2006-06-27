package com.limegroup.gnutella.downloader;

import java.util.Collection;

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

    /**
     * The mesh handler to inform when altlocs fail
     */
    protected MeshHandler meshHandler;

    /**
     * @param hosts a collection of remote hosts to rank
     * @return if we didn't know about at least one of the hosts
     */
    public boolean addToPool(Collection<? extends RemoteFileDesc> hosts) {
        boolean ret = false;
        for(RemoteFileDesc host : hosts) {
            if (addToPool(host))
                ret = true;
        }
        return ret;
    }
    
    /**
     * @param host the host that the ranker should consider
     * @return if we did not already know about this host
     */
    public abstract boolean addToPool(RemoteFileDesc host);
	
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
    protected abstract Collection<RemoteFileDesc> getShareableHosts();
    
    /**
     * @return the number of hosts this ranker knows about
     */
    public abstract int getNumKnownHosts();
    
    /**
     * @return the ranker knows about at least one potential source that is
     * not currently busy
     */
    public synchronized boolean hasNonBusy() {
        return getNumKnownHosts() > getNumBusyHosts();
    }

    /**
     * @return the number of busy hosts the ranker knows about
     */
    public synchronized int getNumBusyHosts() {
        int ret = 0;
        long now = System.currentTimeMillis();
        for(RemoteFileDesc rfd : getPotentiallyBusyHosts()) {
            if (rfd.isBusy(now))
                ret++;
        }
        return ret;
    }
    
    /**
     * @return how much time we should wait before at least one host
     * will become non-busy
     */
    public synchronized int calculateWaitTime() {
        if (!hasMore())
            return 0;
        
        // waitTime is in seconds
        int waitTime = Integer.MAX_VALUE;
        long now = System.currentTimeMillis();
        for(RemoteFileDesc rfd : getPotentiallyBusyHosts()) {
            if (!rfd.isBusy(now))
                continue;
            waitTime = Math.min(waitTime, rfd.getWaitTime(now));
        }
        
        // waitTime was in seconds
        return (waitTime*1000);
    }
    
    protected abstract Collection<RemoteFileDesc> getPotentiallyBusyHosts();
    
    /**
     * stops the ranker, clearing any state
     */
    public synchronized void stop() {
        clearState();
        meshHandler = null;
    }
    
    protected void clearState() {}
    
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
     * @return the ranker that should be used.  If different than the current one,
     * the current one is stopped.
     */
    public static SourceRanker getAppropriateRanker(SourceRanker original) {
        if(original == null)
            return getAppropriateRanker();
        
        SourceRanker better;
        if (RouterService.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue()) {
            if (original instanceof PingRanker)
                return original;
            better = new PingRanker();
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

    /** sets the Mesh handler if any */
    public synchronized void setMeshHandler(MeshHandler handler) {
        meshHandler = handler;
    }
    
    /** 
     * @return the Mesh Handler, if any
     */
    public synchronized MeshHandler getMeshHandler() {
        return meshHandler;
    }
}
