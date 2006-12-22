package com.limegroup.gnutella.licenses;

import org.limewire.service.ErrorService;

class StubWeedLicense extends WeedLicense {
    
    private final String page;
    
    StubWeedLicense(String page) {
        this("1", "2", page);
    }
        
    StubWeedLicense(String cid, String vid, String page) {
        super(buildURI(cid, vid));
        this.page = page;
    }
    
    protected String getBodyFromURL(String url) {
        return page;
    }
    
    public void verify(VerificationListener listener) {
        VerificationListener waiter = new Listener(listener);
        synchronized(waiter) {
            super.verify(waiter);
            try {
                waiter.wait();
            } catch(InterruptedException ie) {
                ErrorService.error(ie);
            }
        }
    }
}