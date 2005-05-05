
package com.limegroup.gnutella.downloader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.util.IpPort;

/**
 * A ranker which uses the legacy logic for selecting from available
 * sources.
 */
public class LegacyRanker extends SourceRanker {
    
    private static final Log LOG = LogFactory.getLog(LegacyRanker.class);

	private final Set rfds;  
	
	LegacyRanker() {
		rfds = new HashSet();
	}
	
	public synchronized boolean addToPool(RemoteFileDesc host) {
        if (LOG.isDebugEnabled())
            LOG.debug("adding host "+host+" to be ranked",new Exception());
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
	public synchronized RemoteFileDesc getBest() {
		
        RemoteFileDesc ret = getBest(rfds.iterator());
        //The best rfd found so far
        boolean removed = rfds.remove(ret);
        Assert.that(removed == true, "unable to remove RFD.");
        
        if (LOG.isDebugEnabled())
            LOG.debug("the best we came with is "+ret);
        
        return ret;
    }
    
    static RemoteFileDesc getBest(Iterator iter) {
        RemoteFileDesc ret=(RemoteFileDesc)iter.next();

        //Find max of each (remaining) element, storing in max.
        //Follows the following logic:
        //1) Find a non-busy host (make connections)
        //2) Find a host that uses hashes (avoid corruptions)
        //3) Find a better quality host (avoid dud locations)
        //4) Find a speedier host (avoid slow downloads)
        while (iter.hasNext()) {
            RemoteFileDesc rfd=(RemoteFileDesc)iter.next();
            
            // 1.            
            if (rfd.isBusy())
                continue;

            if (ret.isBusy())
                ret=rfd;
            // 2.
            else if (rfd.getSHA1Urn()!=null && ret.getSHA1Urn()==null)
                ret=rfd;
            // 3 & 4.
            // (note the use of == so that the comparison is only done
            //  if both rfd & ret either had or didn't have a SHA1)
            else if ((rfd.getSHA1Urn()==null) == (ret.getSHA1Urn()==null)) {
                // 3.
                if (rfd.getQuality() > ret.getQuality())
                    ret=rfd;
                else if (rfd.getQuality() == ret.getQuality()) {
                    // 4.
                    if (rfd.getSpeed() > ret.getSpeed())
                        ret=rfd;
                }            
            }
        }
        
        return ret;
    }
	
	public boolean hasMore() {
		return !rfds.isEmpty();
	}

    protected Collection getShareableHosts() {
        return rfds;
    }
    
    protected Collection getPotentiallyBusyHosts() {
        return rfds;
    }
    
    public int getNumKnownHosts() {
        return rfds.size();
    }
    
    public void stop(){}
}
