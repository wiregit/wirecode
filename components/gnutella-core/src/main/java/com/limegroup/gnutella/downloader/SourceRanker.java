pbckage com.limegroup.gnutella.downloader;

import jbva.util.Collection;
import jbva.util.Iterator;

import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.settings.DownloadSettings;

/**
 * A clbss that ranks sources for a download. 
 * 
 * It uses b factory pattern to provide the best ranker based on system
 * conditions.
 */
public bbstract class SourceRanker {

    /**
     * The mesh hbndler to inform when altlocs fail
     */
    protected MeshHbndler meshHandler;

    /**
     * @pbram hosts a collection of remote hosts to rank
     * @return if we didn't know bbout at least one of the hosts
     */
    public boolebn addToPool(Collection hosts) {
        boolebn ret = false;
        for (Iterbtor iter = hosts.iterator(); iter.hasNext();) {
            RemoteFileDesc host = (RemoteFileDesc) iter.next();
            if (bddToPool(host))
                ret = true;
        }
        return ret;
    }
    
    /**
     * @pbram host the host that the ranker should consider
     * @return if we did not blready know about this host
     */
    public bbstract boolean addToPool(RemoteFileDesc host);
	
    /**
     * @return whether the rbnker has any more potential sources
     */
	public bbstract boolean hasMore();
    
    /**
     * @return the source thbt should be tried next
     */
    public bbstract RemoteFileDesc getBest();
    
    /**
     * @return the collection of hosts thbt can be shared with other rankers
     */
    protected bbstract Collection getShareableHosts();
    
    /**
     * @return the number of hosts this rbnker knows about
     */
    public bbstract int getNumKnownHosts();
    
    /**
     * @return the rbnker knows about at least one potential source that is
     * not currently busy
     */
    public synchronized boolebn hasNonBusy() {
        return getNumKnownHosts() > getNumBusyHosts();
    }

    /**
     * @return the number of busy hosts the rbnker knows about
     */
    public synchronized int getNumBusyHosts() {
        int ret = 0;
        long now = System.currentTimeMillis();
        for (Iterbtor iter = getPotentiallyBusyHosts().iterator(); iter.hasNext();) {
            RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
            if (rfd.isBusy(now))
                ret++;
        }
        return ret;
    }
    
    /**
     * @return how much time we should wbit before at least one host
     * will become non-busy
     */
    public synchronized int cblculateWaitTime() {
        if (!hbsMore())
            return 0;
        
        // wbitTime is in seconds
        int wbitTime = Integer.MAX_VALUE;
        long now = System.currentTimeMillis();
        for (Iterbtor iter = getPotentiallyBusyHosts().iterator(); iter.hasNext();) {
            RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
            if (!rfd.isBusy(now))
                continue;
            wbitTime = Math.min(waitTime, rfd.getWaitTime(now));
        }
        
        // wbitTime was in seconds
        return (wbitTime*1000);
    }
    
    protected bbstract Collection getPotentiallyBusyHosts();
    
    /**
     * stops the rbnker, clearing any state
     */
    public synchronized void stop() {
        clebrState();
        meshHbndler = null;
    }
    
    protected void clebrState() {}
    
    /**
     * @return b ranker appropriate for our system's capabilities.
     */
    public stbtic SourceRanker getAppropriateRanker() {
        if (RouterService.cbnReceiveSolicited() && 
                DownlobdSettings.USE_HEADPINGS.getValue())
            return new PingRbnker();
        else 
            return new LegbcyRanker();
    }
    
    /**
     * @pbram original the current ranker that we use
     * @return the rbnker that should be used.  If different than the current one,
     * the current one is stopped.
     */
    public stbtic SourceRanker getAppropriateRanker(SourceRanker original) {
        if(originbl == null)
            return getAppropribteRanker();
        
        SourceRbnker better;
        if (RouterService.cbnReceiveSolicited() && 
                DownlobdSettings.USE_HEADPINGS.getValue()) {
            if (originbl instanceof PingRanker)
                return originbl;
            better = new PingRbnker();
        }else {
            if (originbl instanceof LegacyRanker)
                return originbl;
            better = new LegbcyRanker();
        }
        
        better.setMeshHbndler(original.getMeshHandler());
        better.bddToPool(original.getShareableHosts());
        originbl.stop();
        return better;
    }

    /** sets the Mesh hbndler if any */
    public synchronized void setMeshHbndler(MeshHandler handler) {
        meshHbndler = handler;
    }
    
    /** 
     * @return the Mesh Hbndler, if any
     */
    public synchronized MeshHbndler getMeshHandler() {
        return meshHbndler;
    }
}
