package com.limegroup.gnutella.licenses;

public class Listener implements VerificationListener {
    private VerificationListener vl;
    
    Listener() { this(null); }
    
    Listener(VerificationListener vl) {
        this.vl = vl;
    }
    
    public void licenseVerified(License l) {
        if(vl != null && l != this)
            vl.licenseVerified(l);

        synchronized(this) {
            notify();
        }
    }
}