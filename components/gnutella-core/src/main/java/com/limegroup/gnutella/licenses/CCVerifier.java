package com.limegroup.gnutella.licenses;

import java.io.StringReader;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.DOMException;
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
    public String getVerifiedDescription() {
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
        if(!isVerifying() && !isVerificationDone()) {
            state = VERIFYING;
            VQUEUE.add(new VImpl(listener));
        }
    }

    /**
     * Retrieves the body of a page that has RDF embedded in it.
     *
     * Returns null if the page could not be found.
     */
    protected String getBody() {
        HttpClient client = HttpClientManager.getNewClient();
        GetMethod get = new GetMethod(uri.toString());
        get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
        try {
            HttpClientManager.executeMethodRedirecting(client, get);
            return get.getResponseBodyAsString();
        } catch(IOException ioe) {
            return null;
        } finally {
            get.releaseConnection();
        }
    }
    
    /**
     * Verifies the body of the verification page.
     */
    protected boolean doVerification(String body) {
        if(LOG.isTraceEnabled())
            LOG.trace("Attempting to verify: " + body);
        
        if(body == null || body.trim().equals(""))
            return false;
        
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
        NodeList workItems = doc.getElementsByTagName("Work");
        boolean workPassed = false;
        for(int i = 0; i < workItems.getLength(); i++)
            workPassed |= parseWorkItem(workItems.item(i));
        NodeList licenseItems = doc.getElementsByTagName("License");
        for(int i = 0; i < licenseItems.getLength(); i++)
            parseLicenseItem(licenseItems.item(i));
            
        // so long as we found a valid work item, we're good.
        return workPassed;
    }
    
    /**
     * Ensures the 'work' item exists and retrieves the URN, if it exists.
     */
    protected boolean parseWorkItem(Node work) {
        if(LOG.isTraceEnabled())
            LOG.trace("Parsing work item: " + work);
        
        // work MUST exist.
        if(work == null) {
            LOG.error("No work item, bailing");
            return false;
        }
            
        NamedNodeMap attributes = work.getAttributes();
        Node about = attributes.getNamedItem("rdf:about");
        if(about != null) {
            String value = about.getNodeValue();
            // attempt to create a SHA1 urn out of it.
            try {
                expectedURN = URN.createSHA1Urn(value);
                if(LOG.isDebugEnabled())
                    LOG.debug("Found URN: " + expectedURN);
            } catch(IOException ioe) {
                LOG.warn("Unable to create URN out of 'about' value", ioe);
            }
        } else if(LOG.isWarnEnabled()) {
            LOG.warn("No about item!");
        }
        
        // other than it existing, nothing else needs to happen.
        return true;
    }
    
    /**
     * Parses the 'license' item.
     */
    protected boolean parseLicenseItem(Node license) {
        if(LOG.isTraceEnabled())
            LOG.trace("Parsing license item: " + license);
        
        if(license == null)
            return false;
           
        // Get the license URL. 
        NamedNodeMap attributes = license.getAttributes();
        Node about = attributes.getNamedItem("rdf:about");
        if(about != null) {
            String value = about.getNodeValue();
            try {
                licenseURL = new URL(value);
                if(LOG.isDebugEnabled())
                    LOG.debug("Found licenseURL: " + licenseURL);
            } catch(MalformedURLException murl) {
                LOG.warn("Unable to get license URL", murl);
            }
        } else if(LOG.isWarnEnabled())
            LOG.warn("No about item!");
        
        // Get the 'permit', 'requires', and 'prohibits' values.
        NodeList children = license.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = (Node)children.item(i);
            String name = child.getNodeName();
            if(name.equalsIgnoreCase("requires")) {
                if(required == null)
                    required = new LinkedList();
                addPermission(required, child);
            } else if(name.equalsIgnoreCase("permits")) {
                if(permitted == null)
                    permitted = new LinkedList();
                addPermission(permitted, child);
            } else if(name.equalsIgnoreCase("prohibits")) {
                if(prohibited == null)
                    prohibited = new LinkedList();
                addPermission(prohibited, child);
            }
        }
        return true;
    }
    
    /**
     * Adds a single permission to the list.
     */
    private void addPermission(List permissions, Node node) {
        if(LOG.isTraceEnabled())
            LOG.trace("Adding permission from: " + node);
        
        NamedNodeMap attributes = node.getAttributes();
        Node resource = attributes.getNamedItem("rdf:resource");
        if(resource != null) {
            String value = resource.getNodeValue();
            int slash = value.lastIndexOf('/');
            if(slash != -1 && slash != value.length()-1) {
                String permission = value.substring(slash+1);
                permissions.add(permission);
                if(LOG.isDebugEnabled())
                    LOG.debug("Added permission: " + permission);
            } else if (LOG.isWarnEnabled()) {
                LOG.trace("Unable to find permission name: " + value);
            }
        } else if(LOG.isWarnEnabled()) {
            LOG.warn("No resource item in rdf namespace");
        } 
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
            String body = getBody();
            if(!doVerification(body))
                state = VERIFY_FAILED;
            else
                state = VERIFIED;
            
            if(vc != null)
                vc.verificationCompleted(CCVerifier.this);
        }
    }
}
