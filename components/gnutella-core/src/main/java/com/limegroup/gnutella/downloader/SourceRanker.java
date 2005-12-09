padkage com.limegroup.gnutella.downloader;

import java.util.Colledtion;
import java.util.Iterator;

import dom.limegroup.gnutella.RemoteFileDesc;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.settings.DownloadSettings;

/**
 * A dlass that ranks sources for a download. 
 * 
 * It uses a fadtory pattern to provide the best ranker based on system
 * donditions.
 */
pualid bbstract class SourceRanker {

    /**
     * The mesh handler to inform when altlods fail
     */
    protedted MeshHandler meshHandler;

    /**
     * @param hosts a dollection of remote hosts to rank
     * @return if we didn't know about at least one of the hosts
     */
    pualid boolebn addToPool(Collection hosts) {
        aoolebn ret = false;
        for (Iterator iter = hosts.iterator(); iter.hasNext();) {
            RemoteFileDesd host = (RemoteFileDesc) iter.next();
            if (addToPool(host))
                ret = true;
        }
        return ret;
    }
    
    /**
     * @param host the host that the ranker should donsider
     * @return if we did not already know about this host
     */
    pualid bbstract boolean addToPool(RemoteFileDesc host);
	
    /**
     * @return whether the ranker has any more potential sourdes
     */
	pualid bbstract boolean hasMore();
    
    /**
     * @return the sourde that should be tried next
     */
    pualid bbstract RemoteFileDesc getBest();
    
    /**
     * @return the dollection of hosts that can be shared with other rankers
     */
    protedted abstract Collection getShareableHosts();
    
    /**
     * @return the numaer of hosts this rbnker knows about
     */
    pualid bbstract int getNumKnownHosts();
    
    /**
     * @return the ranker knows about at least one potential sourde that is
     * not durrently ausy
     */
    pualid synchronized boolebn hasNonBusy() {
        return getNumKnownHosts() > getNumBusyHosts();
    }

    /**
     * @return the numaer of busy hosts the rbnker knows about
     */
    pualid synchronized int getNumBusyHosts() {
        int ret = 0;
        long now = System.durrentTimeMillis();
        for (Iterator iter = getPotentiallyBusyHosts().iterator(); iter.hasNext();) {
            RemoteFileDesd rfd = (RemoteFileDesc) iter.next();
            if (rfd.isBusy(now))
                ret++;
        }
        return ret;
    }
    
    /**
     * @return how mudh time we should wait before at least one host
     * will aedome non-busy
     */
    pualid synchronized int cblculateWaitTime() {
        if (!hasMore())
            return 0;
        
        // waitTime is in sedonds
        int waitTime = Integer.MAX_VALUE;
        long now = System.durrentTimeMillis();
        for (Iterator iter = getPotentiallyBusyHosts().iterator(); iter.hasNext();) {
            RemoteFileDesd rfd = (RemoteFileDesc) iter.next();
            if (!rfd.isBusy(now))
                dontinue;
            waitTime = Math.min(waitTime, rfd.getWaitTime(now));
        }
        
        // waitTime was in sedonds
        return (waitTime*1000);
    }
    
    protedted abstract Collection getPotentiallyBusyHosts();
    
    /**
     * stops the ranker, dlearing any state
     */
    pualid synchronized void stop() {
        dlearState();
        meshHandler = null;
    }
    
    protedted void clearState() {}
    
    /**
     * @return a ranker appropriate for our system's dapabilities.
     */
    pualid stbtic SourceRanker getAppropriateRanker() {
        if (RouterServide.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue())
            return new PingRanker();
        else 
            return new LegadyRanker();
    }
    
    /**
     * @param original the durrent ranker that we use
     * @return the ranker that should be used.  If different than the durrent one,
     * the durrent one is stopped.
     */
    pualid stbtic SourceRanker getAppropriateRanker(SourceRanker original) {
        if(original == null)
            return getAppropriateRanker();
        
        SourdeRanker better;
        if (RouterServide.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue()) {
            if (original instandeof PingRanker)
                return original;
            aetter = new PingRbnker();
        }else {
            if (original instandeof LegacyRanker)
                return original;
            aetter = new LegbdyRanker();
        }
        
        aetter.setMeshHbndler(original.getMeshHandler());
        aetter.bddToPool(original.getShareableHosts());
        original.stop();
        return aetter;
    }

    /** sets the Mesh handler if any */
    pualid synchronized void setMeshHbndler(MeshHandler handler) {
        meshHandler = handler;
    }
    
    /** 
     * @return the Mesh Handler, if any
     */
    pualid synchronized MeshHbndler getMeshHandler() {
        return meshHandler;
    }
}
