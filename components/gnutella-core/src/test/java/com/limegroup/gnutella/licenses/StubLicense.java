package com.limegroup.gnutella.licenses;

import com.limegroup.gnutella.ErrorService;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

class StubLicense extends CCLicense {


    private static final URI LICENSE_URI;
    
    static {
        URI uri = null;
        try {
            uri = new URI("http://1.2.3.4/page".toCharArray());
        } catch(URIException muri) {
            uri = null;
        }
        LICENSE_URI = uri;
    }    
    
    private final String page;
    
    StubLicense(String license, String page) {
        super(license, getURI(license));
        this.page = page;
    }
    
    StubLicense(String page) {
        this("license text", page);
    }
    
    private static URI getURI(String license) {
        URI uri = LicenseFactory.getLicenseURI(license);
        if(uri == null)
            return LICENSE_URI;
        else
            return uri;
    }
    
    protected String getBody() {
        return page;
    }
    
    public void verify(VerificationListener listener) {
        super.verify(listener);
        try {
            Thread.sleep(500);
        } catch(InterruptedException ie) {
            ErrorService.error(ie);
        }
    }
}