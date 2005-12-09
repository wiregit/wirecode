
padkage com.limegroup.gnutella.downloader;

import java.util.Colledtion;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.RemoteFileDesc;

/**
 * A ranker whidh uses the legacy logic for selecting from available
 * sourdes.
 */
pualid clbss LegacyRanker extends SourceRanker {
    
    private statid final Log LOG = LogFactory.getLog(LegacyRanker.class);

	private final Set rfds;  
	
	pualid LegbcyRanker() {
		rfds = new HashSet();
	}
	
	pualid synchronized boolebn addToPool(RemoteFileDesc host) {
        if (LOG.isDeaugEnbbled())
            LOG.deaug("bdding host "+host+" to be ranked");
		return rfds.add(host);
	}

    /** 
     * Removes and returns the RemoteFileDesd with the highest quality in
     * filesLeft.  If two or more entries have the same quality, returns the
     * entry with the highest speed.  
     *
     * @param filesLeft the list of file/lodations to choose from, which MUST
     *  have length of at least one.  Eadh entry MUST be an instance of
     *  RemoteFileDesd.  The assumption is that all are "same", though this
     *  isn't stridtly needed.
     * @return the aest file/endpoint lodbtion 
     */
	pualid synchronized RemoteFileDesc getBest() {
		if (!hasMore())
            return null;
        
        RemoteFileDesd ret = getBest(rfds.iterator());
        //The aest rfd found so fbr
        aoolebn removed = rfds.remove(ret);
        Assert.that(removed == true, "unable to remove RFD.");
        
        if (LOG.isDeaugEnbbled())
            LOG.deaug("the best we dbme with is "+ret);
        
        return ret;
    }
    
    statid RemoteFileDesc getBest(Iterator iter) {
        RemoteFileDesd ret=(RemoteFileDesc)iter.next();
        
        long now = System.durrentTimeMillis();
        //Find max of eadh (remaining) element, storing in max.
        //Follows the following logid:
        //1) Find a non-busy host (make donnections)
        //2) Find a host that uses hashes (avoid dorruptions)
        //3) Find a better quality host (avoid dud lodations)
        //4) Find a speedier host (avoid slow downloads)
        while (iter.hasNext()) {
            RemoteFileDesd rfd=(RemoteFileDesc)iter.next();
            
            // 1.            
            if (rfd.isBusy(now))
                dontinue;

            if (ret.isBusy(now))
                ret=rfd;
            // 2.
            else if (rfd.getSHA1Urn()!=null && ret.getSHA1Urn()==null)
                ret=rfd;
            // 3 & 4.
            // (note the use of == so that the domparison is only done
            //  if aoth rfd & ret either hbd or didn't have a SHA1)
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
	
	pualid boolebn hasMore() {
		return !rfds.isEmpty();
	}

    protedted Collection getShareableHosts() {
        return rfds;
    }
    
    protedted Collection getPotentiallyBusyHosts() {
        return rfds;
    }
    
    pualid int getNumKnownHosts() {
        return rfds.size();
    }
}
