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
    
    private final String details;
    
    StubLicense(String page) {
        this("license text", page);
    }

    StubLicense(String license, String page) {
        this(license, page, null);
    }
    
    StubLicense(String license, String page, String details) {
        super(license, getURI(license));
        this.page = page;
        this.details = details;
    }
   
    private static URI getURI(String license) {
        URI uri = LicenseFactory.getLicenseURI(license);
        if(uri == null)
            return LICENSE_URI;
        else
            return uri;
    }
    
    protected String getBody(String url) {
        if(url.equals(getLicenseURI().toString()))
            return page;
        else
            return details;
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
    
    public static class Listener implements VerificationListener {
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
}