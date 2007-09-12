package com.limegroup.gnutella.licenses;

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
    
}