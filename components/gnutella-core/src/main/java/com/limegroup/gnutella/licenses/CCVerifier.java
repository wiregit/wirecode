package com.limegroup.gnutella.licenses;

import java.io.StringReader;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.util.CommonUtils;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * A concrete implementation of a verifier, for Creative Commons licenses.
 */
/* package private */ class CCVerifier implements Verifier {
    
    private static final Log LOG = LogFactory.getLog(CCVerifier.class);
    
    private static final ProcessingQueue VQUEUE = new ProcessingQueue("CCVerifier");
    
    /**
     * The current state of this verifier.
     */
    private int state = NOTHING;
    
    /**
     * The URI where verification will be performed.
     */
    private final URI uri;
    
    /**
     * The license string.
     */
    private final String license;
    
    /**
     * The URL for the license.
     */
    private URL licenseURL;
    
    /**
     * The URN of this verifier.
     */
    private URN expectedURN;
    
    /**
     * A list of permissions that are permitted.
     */
    private List permitted;
    
    /**
     * A list of permissions that are probhibited.
     */
    private List prohibited;
    
    /**
     * A list of permissions that are required.
     */
    private List required;
    
    /**
     * Constructs a new CCVerifier.
     */
    CCVerifier(String license, URI uri) {
        this.license = license;
        this.uri = uri;
    }
    
    public boolean isVerified() { return state == VERIFIED; }
    public boolean isVerifying() { return state == VERIFYING; }
    public boolean isVerificationDone() { return state > VERIFYING; }
    public String getLicense() { return license; }
    public URL getLicenseURL() { return licenseURL; }
    public URN getExpectedURN() { return expectedURN; }
    
    public URL getURL() {
        try {
            return new URL(uri.toString());
        } catch(MalformedURLException murl) {
            // should not happen, because the URI was checked prior to construction.
            ErrorService.error(murl);
            return null;
        }
    }
    
    /**
     * Builds a description of this license based on what is permitted,
     * probibited, and required.
     */
    public String getVerifiedDescrption() {
        StringBuffer sb = new StringBuffer();
        if(permitted != null && !permitted.isEmpty()) {
            sb.append("Permitted: ");
            for(Iterator i = permitted.iterator(); i.hasNext(); )
                sb.append(i.next().toString());
        }
        if(prohibited != null && !prohibited.isEmpty()) {
            if(sb.length() != 0)
                sb.append("\n");
            sb.append("Prohibited: ");
            for(Iterator i = prohibited.iterator(); i.hasNext(); )
                sb.append(i.next().toString());
        }
        if(required != null && !required.isEmpty()) {
            if(sb.length() != 0)
                sb.append("\n");
            sb.append("Required: ");
            for(Iterator i = required.iterator(); i.hasNext(); )
                sb.append(i.next().toString());
        }
        
        if(sb.length() == 0)
            sb.append("Permissions unknown.");
        
        return sb.toString();
    }
    
    /**
     * Starts verification of the license.
     *
     * The listener is notified when verification is finished.
     */
    public void verify(VerificationCallback listener) {
        VQUEUE.add(new VImpl(listener));
    }
    
    /**
     * Verifies the body of the verification page.
     */
    protected boolean doVerification(String body) {
        // look for two rdf:RDF's.
        int startRDF = body.indexOf("rdf:RDF");
        if(startRDF >= body.length() - 1)
            return false;
            
        int endRDF = body.indexOf("rdf:RDF", startRDF+1);
        if(startRDF == -1 || endRDF == -1)
            return false;
        
        // okay, now we know there's a start & end, find the opening <
        // and closing >, get that substring, and do a DOM parsing.
        startRDF = body.lastIndexOf('<', startRDF);
        endRDF = body.indexOf('>', endRDF);
        if(startRDF == -1 || endRDF == -1)
            return false;
        
        // Alright, we got where the rdf is at!
        String rdf = body.substring(startRDF, endRDF + 1);
        DOMParser parser = new DOMParser();
        InputSource is = new InputSource(new StringReader(rdf));
        try {
            parser.parse(is);
        } catch (IOException ioe) {
            LOG.debug("IOX parsing RDF", ioe);
            return false;
        } catch (SAXException saxe) {
            LOG.debug("SAX parsing RDF", saxe);
            return false;
        }
        
        Document doc = parser.getDocument();
        Node workItem = doc.getElementsByTagName("Work").item(0);
        Node licenseItem = doc.getElementsByTagName("License").item(0);
        // good enough for today.
        return true;
    }
    
    /**
     * Runnable that actually does the verification.
     */
    private class VImpl implements Runnable {
        private final VerificationCallback vc;
        
        VImpl(VerificationCallback listener) {
            vc = listener;
        }
        
        public void run() {
            HttpClient client = HttpClientManager.getNewClient();
            GetMethod get = new GetMethod(uri.toString());
            get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
            try {
                HttpClientManager.executeMethodRedirecting(client, get);
                String body = get.getResponseBodyAsString();
                if(body == null || body.trim().equals(""))
                    state = VERIFY_FAILED;
                else if(!doVerification(body))
                    state = VERIFY_FAILED;
                else
                    state = VERIFIED;
            } catch(IOException ioe) {
                state = VERIFY_FAILED;
            } finally {
                get.releaseConnection();
            }
            
            vc.verificationCompleted(CCVerifier.this);
        }
    }   
}