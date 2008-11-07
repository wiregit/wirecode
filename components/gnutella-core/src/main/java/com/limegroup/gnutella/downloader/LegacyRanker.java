
package com.limegroup.gnutella.downloader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RemoteFileDesc;

/**
 * A ranker which uses the legacy logic for selecting from available
 * sources.
 */
public class LegacyRanker extends AbstractSourceRanker {
    
    private static final Log LOG = LogFactory.getLog(LegacyRanker.class);

	private final Set<RemoteFileDescContext> rfds;  
	
	public LegacyRanker() {
		rfds = new HashSet<RemoteFileDescContext>();
	}
	
	@Override
    public synchronized boolean addToPool(RemoteFileDescContext host) {
        if (LOG.isDebugEnabled())
            LOG.debug("adding host "+host+" to be ranked", new Exception());
		return rfds.add(host);
	}

    /** 
     * Removes and returns the RemoteFileDesc with the highest quality in
     * filesLeft.  If two or more entries have the same quality, returns the
     * entry with the highest speed.  
     *
     * @param filesLeft the list of file/locations to choose from, which MUST
     *  have length of at least one.  Each entry MUST be an instance of
     *  RemoteFileDesc.  The assumption is that all are "same", though this
     *  isn't strictly needed.
     * @return the best file/endpoint location 
     */
	@Override
    public synchronized RemoteFileDescContext getBest() {
		if (!hasMore())
            return null;
        
        RemoteFileDescContext ret = getBest(rfds.iterator());
        //The best rfd found so far
        boolean removed = rfds.remove(ret);
        assert removed : "unable to remove RFD.";
        
        if (LOG.isDebugEnabled())
            LOG.debug("the best we came with is "+ret);
        
        return ret;
    }
    
    static RemoteFileDescContext getBest(Iterator<RemoteFileDescContext> iter) {
        RemoteFileDescContext ret= iter.next();
        
        long now = System.currentTimeMillis();
        //Find max of each (remaining) element, storing in max.
        //Follows the following logic:
        //1) Find a non-busy host (make connections)
        //2) Find a host that uses hashes (avoid corruptions)
        //3) Find a better quality host (avoid dud locations)
        //4) Find a speedier host (avoid slow downloads)
        while (iter.hasNext()) {
            // define in loop to reflect current selection of ret
            RemoteFileDesc retRfd = ret.getRemoteFileDesc();
            
            RemoteFileDescContext rfdContext= iter.next();
            RemoteFileDesc rfd = rfdContext.getRemoteFileDesc();
            
            // 1.            
            if (rfdContext.isBusy(now))
                continue;

            if (ret.isBusy(now))
                ret=rfdContext;
            // 2.
            else if (rfd.getSHA1Urn()!=null && retRfd.getSHA1Urn()==null)
                ret=rfdContext;
            // 3 & 4.
            // (note the use of == so that the comparison is only done
            //  if both rfd & ret either had or didn't have a SHA1)
            else if ((rfd.getSHA1Urn()==null) == (retRfd.getSHA1Urn()==null)) {
                // 3.
                if (rfd.getQuality() > retRfd.getQuality())
                    ret=rfdContext;
                else if (rfd.getQuality() == retRfd.getQuality()) {
                    // 4.
                    if (rfd.getSpeed() > retRfd.getSpeed())
                        ret=rfdContext;
                }            
            }
        }
        
        return ret;
    }
	
	@Override
    public boolean hasMore() {
		return !rfds.isEmpty();
	}

    @Override
    public Collection<RemoteFileDescContext> getShareableHosts() {
        return rfds;
    }
    
    @Override
    protected Collection<RemoteFileDescContext> getPotentiallyBusyHosts() {
        return rfds;
    }
    
    @Override
    public int getNumKnownHosts() {
        return rfds.size();
    }
}
