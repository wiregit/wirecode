package com.limegroup.gnutella.licenses;

    
class Callback implements VerificationListener {
    public boolean completed = false;
    
    public void licenseVerified(License license) {
        completed = true;
    }
}