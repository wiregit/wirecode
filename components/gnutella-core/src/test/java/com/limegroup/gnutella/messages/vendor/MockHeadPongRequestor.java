package com.limegroup.gnutella.messages.vendor;

import org.limewire.io.URNImpl;

public class MockHeadPongRequestor implements HeadPongRequestor {
    
    private byte features;
    private byte[] guid;
    private URNImpl urn;
    private boolean pongGGEPCapable;
    private boolean requestsAltLocs;
    private boolean requestsFWTOnlyPushLocs;
    private boolean requestsPushLocs;
    private boolean requestsRanges;

    public byte getFeatures() {
        return features;
    }

    public byte[] getGUID() {
        return guid;
    }

    public URNImpl getUrn() {
        return urn;
    }

    public boolean isPongGGEPCapable() {
        return pongGGEPCapable;        
    }

    public boolean requestsAltlocs() {
        return requestsAltLocs;
    }

    public boolean requestsFWTOnlyPushLocs() {
        return requestsFWTOnlyPushLocs;
    }

    public boolean requestsPushLocs() {
        return requestsPushLocs;
    }

    public boolean requestsRanges() {
        return requestsRanges;
    }

    public byte[] getGuid() {
        return guid;
    }

    public void setGuid(byte[] guid) {
        this.guid = guid;
    }

    public void setFeatures(byte features) {
        this.features = features;
    }

    public void setPongGGEPCapable(boolean pongGGEPCapable) {
        this.pongGGEPCapable = pongGGEPCapable;
    }

    public void setRequestsAltLocs(boolean requestsAltLocs) {
        this.requestsAltLocs = requestsAltLocs;
    }

    public void setRequestsFWTOnlyPushLocs(boolean requestsFWTOnlyPushLocs) {
        this.requestsFWTOnlyPushLocs = requestsFWTOnlyPushLocs;
    }

    public void setRequestsPushLocs(boolean requestsPushLocs) {
        this.requestsPushLocs = requestsPushLocs;
    }

    public void setRequestsRanges(boolean requestsRanges) {
        this.requestsRanges = requestsRanges;
    }

    public void setUrn(URNImpl urn) {
        this.urn = urn;
    }

}
