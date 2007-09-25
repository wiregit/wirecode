package com.limegroup.gnutella.licenses;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.ProviderHacks;


class StubCCLicense extends CCLicense {


    private static final URI LICENSE_URI;
    
    static {
        URI uri = null;
        try {
            uri = new URI("http://1.2.3.4/page".toCharArray());
        } catch(URIException muri) {
        }
        LICENSE_URI = uri;
    }    
    
    private final String page;
    
    private final String details;
    
    StubCCLicense(String page) {
        this("license text", page);
    }

    StubCCLicense(String license, String page) {
        this(license, page, null);
    }
    
    StubCCLicense(String license, String page, String details) {
        super(license, getURI(license), ProviderHacks.getLicenseCache());
        this.page = page;
        this.details = details;
    }
   
    private static URI getURI(String license) {
        URI uri = LicenseFactoryImpl.getLicenseURI(license);
        if(uri == null)
            return LICENSE_URI;
        else
            return uri;
    }
    
    protected String getBodyFromURL(String url) {
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
}