package com.limegroup.gnutella.downloader;

import java.util.Collection;
import java.util.Iterator;

import com.limegroup.gnutella.util.IpPort;

/**
 * A class that ranks sources for a download. 
 * 
 * It uses a factory pattern to provide the best ranker based on system
 * conditions.
 */
public abstract class SourceRanker {

    /**
     * a null object
     */
    public static final SourceRanker EMPTY_RANKER = new EmptyRanker();
    
    public SourceRanker() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public void addToPool(Collection hosts) {
        for (Iterator iter = hosts.iterator(); iter.hasNext();) {
            IpPort host = (IpPort) iter.next();
            addToPool(host);
        }
    }
    
    public abstract void addToPool(IpPort host);
	
	public abstract boolean hasMore();
    
    public abstract IpPort getBest();
    
    /**
     * @return a ranker appropriate for our system's capabilities.
     */
    public static SourceRanker getAppropriateRanker() {
        // for example, if we can't receive solicited UDP we'd use a 
        // ranker that uses the current logic in ManagedDownloader.removeBest
        // which would be implemented in ClassicRanker or similar.
        return new LegacyRanker();
    }
    
    private static class EmptyRanker extends SourceRanker {
        public void addToPool(IpPort host){}
        public IpPort getBest() {
            return null;
        }
		public boolean hasMore(){return false;}
    }

}
