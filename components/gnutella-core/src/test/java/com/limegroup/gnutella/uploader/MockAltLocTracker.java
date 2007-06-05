/**
 * 
 */
package com.limegroup.gnutella.uploader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.http.AltLocTracker;

class MockAltLocTracker extends AltLocTracker {
    private List<AlternateLocation> addedLocs = new ArrayList<AlternateLocation>();
    
    private Collection<DirectAltLoc> nextSetOfAltsToSend = null;
    private Collection<PushAltLoc> nextSetOfPushAltsToSend = null;

    public MockAltLocTracker() {
        super(HugeTestUtils.SHA1);
    }
    
    public MockAltLocTracker(URN urn) {
        super(urn);
    }

    @Override
    public void addLocation(AlternateLocation al) {
        addedLocs.add(al);
        super.addLocation(al);
    }
    
    public List<AlternateLocation> getAddedLocs() {
        return addedLocs;
    }

    @Override
    public Collection<DirectAltLoc> getNextSetOfAltsToSend() {
        if(nextSetOfAltsToSend == null)
            return super.getNextSetOfAltsToSend();
        else
            return nextSetOfAltsToSend;
    }

    @Override
    public Collection<PushAltLoc> getNextSetOfPushAltsToSend() {
        if(nextSetOfPushAltsToSend == null)
            return super.getNextSetOfPushAltsToSend();
        else
            return nextSetOfPushAltsToSend;
    }

    public void setNextSetOfAltsToSend(Collection<DirectAltLoc> nextSetOfAlts) {
        this.nextSetOfAltsToSend = nextSetOfAlts;
    }

    public void setNextSetOfPushAltsToSend(Collection<PushAltLoc> nextSetOfPushAlts) {
        this.nextSetOfPushAltsToSend = nextSetOfPushAlts;
    }
    
    
}