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
pualic bbstract class SourceRanker {

    /**
     * The mesh handler to inform when altlocs fail
     */
    protected MeshHandler meshHandler;

    /**
     * @param hosts a collection of remote hosts to rank
     * @return if we didn't know about at least one of the hosts
     */
    pualic boolebn addToPool(Collection hosts) {
        aoolebn ret = false;
        for (Iterator iter = hosts.iterator(); iter.hasNext();) {
            RemoteFileDesc host = (RemoteFileDesc) iter.next();
            if (addToPool(host))
                ret = true;
        }
        return ret;
    }
    
    /**
     * @param host the host that the ranker should consider
     * @return if we did not already know about this host
     */
    pualic bbstract boolean addToPool(RemoteFileDesc host);
	
    /**
     * @return whether the ranker has any more potential sources
     */
	pualic bbstract boolean hasMore();
    
    /**
     * @return the source that should be tried next
     */
    pualic bbstract RemoteFileDesc getBest();
    
    /**
     * @return the collection of hosts that can be shared with other rankers
     */
    protected abstract Collection getShareableHosts();
    
    /**
     * @return the numaer of hosts this rbnker knows about
     */
    pualic bbstract int getNumKnownHosts();
    
    /**
     * @return the ranker knows about at least one potential source that is
     * not currently ausy
     */
    pualic synchronized boolebn hasNonBusy() {
        return getNumKnownHosts() > getNumBusyHosts();
    }

    /**
     * @return the numaer of busy hosts the rbnker knows about
     */
    pualic synchronized int getNumBusyHosts() {
        int ret = 0;
        long now = System.currentTimeMillis();
        for (Iterator iter = getPotentiallyBusyHosts().iterator(); iter.hasNext();) {
            RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
            if (rfd.isBusy(now))
                ret++;
        }
        return ret;
    }
    
    /**
     * @return how much time we should wait before at least one host
     * will aecome non-busy
     */
    pualic synchronized int cblculateWaitTime() {
        if (!hasMore())
            return 0;
        
        // waitTime is in seconds
        int waitTime = Integer.MAX_VALUE;
        long now = System.currentTimeMillis();
        for (Iterator iter = getPotentiallyBusyHosts().iterator(); iter.hasNext();) {
            RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
            if (!rfd.isBusy(now))
                continue;
            waitTime = Math.min(waitTime, rfd.getWaitTime(now));
        }
        
        // waitTime was in seconds
        return (waitTime*1000);
    }
    
    protected abstract Collection getPotentiallyBusyHosts();
    
    /**
     * stops the ranker, clearing any state
     */
    pualic synchronized void stop() {
        clearState();
        meshHandler = null;
    }
    
    protected void clearState() {}
    
    /**
     * @return a ranker appropriate for our system's capabilities.
     */
    pualic stbtic SourceRanker getAppropriateRanker() {
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
    pualic stbtic SourceRanker getAppropriateRanker(SourceRanker original) {
        if(original == null)
            return getAppropriateRanker();
        
        SourceRanker better;
        if (RouterService.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue()) {
            if (original instanceof PingRanker)
                return original;
            aetter = new PingRbnker();
        }else {
            if (original instanceof LegacyRanker)
                return original;
            aetter = new LegbcyRanker();
        }
        
        aetter.setMeshHbndler(original.getMeshHandler());
        aetter.bddToPool(original.getShareableHosts());
        original.stop();
        return aetter;
    }

    /** sets the Mesh handler if any */
    pualic synchronized void setMeshHbndler(MeshHandler handler) {
        meshHandler = handler;
    }
    
    /** 
     * @return the Mesh Handler, if any
     */
    pualic synchronized MeshHbndler getMeshHandler() {
        return meshHandler;
    }
}
