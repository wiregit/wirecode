package com.limegroup.gnutella.licenses;

import java.io.StringReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;

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
 * A concrete implementation of a License, for Creative Commons licenses.
 */
class CCLicense implements License, Serializable, Cloneable {
    
    private static final Log LOG = LogFactory.getLog(CCLicense.class);
    
    private static final ProcessingQueue VQUEUE = new ProcessingQueue("CCLicense");
    
    private static final long serialVersionUID = 8213994964631107858L;
    
    /**
     * Whether or not this license has been verified.
     */
    private transient int verified = UNVERIFIED;
    
    /**
     * Whether or not this license is valid.
     */
    private boolean valid;
    
    /**
     * The last time this license was verified.
     */
    private long lastVerifiedTime;
    
    /**
     * The URI where verification will be performed.
     */
    private URI licenseLocation;
    
    /**
     * The license string.
     */
    private transient String license;
    
    /**
     * The URL for the license.
     */
    private URL licenseURL;
    
    /**
     * The URN of this License.
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
     * Constructs a new CCLicense.
     */
    CCLicense(String license, URI uri) {
        this.license = license;
        this.licenseLocation = uri;
    }
    
    public boolean isVerifying() { return verified == VERIFYING; }
    public boolean isVerified() { return verified == VERIFIED; }
    public String getLicense() { return license; }
    public URL getLicenseDeed() { return licenseURL == null ? guessLicenseDeed() : licenseURL; }
    public long getLastVerifiedTime() { return lastVerifiedTime; }
    public URI getLicenseURI() { return licenseLocation; }

    /**
     * Attempts to guess what the license URI is from the license text.
     */    
    private URL guessLicenseDeed() {
        return CCConstants.guessLicenseDeed(license);
    }
        
    /**
     * Determines if the CC License is valid with this URN.
     */
    public boolean isValid(URN urn) {
        if(!valid)
            return false;
        if(expectedURN == null || urn == null)
            return true;

        return expectedURN.equals(urn);
    }
    
    /**
     * Returns a CCLicense exactly like this, except
     * with a different license string.
     */
    public License copy(String license) {
        CCLicense newL = null;
        try {
            newL = (CCLicense)clone();
            newL.license = license;
        } catch(CloneNotSupportedException error) {
            ErrorService.error(error);
        }
        return newL;
    }
    
    /**
     * Assume that all serialized licenses were verified.
     * (Otherwise they wouldn't have been serialized.
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        verified = VERIFIED;
    }
    
    /**
     * Builds a description of this license based on what is permitted,
     * probibited, and required.
     */
    public String getLicenseDescription() {
        StringBuffer sb = new StringBuffer();
        if(permitted != null && !permitted.isEmpty()) {
            sb.append("Permitted: ");
            for(Iterator i = permitted.iterator(); i.hasNext(); ) {
                sb.append(i.next().toString());
                if(i.hasNext())
                    sb.append(", ");
            }
        }
        if(prohibited != null && !prohibited.isEmpty()) {
            if(sb.length() != 0)
                sb.append("\n");
            sb.append("Prohibited: ");
            for(Iterator i = prohibited.iterator(); i.hasNext(); ) {
                sb.append(i.next().toString());
                if(i.hasNext())
                    sb.append(", ");
            }
        }
        if(required != null && !required.isEmpty()) {
            if(sb.length() != 0)
                sb.append("\n");
            sb.append("Required: ");
            for(Iterator i = required.iterator(); i.hasNext(); ) {
                sb.append(i.next().toString());
                if(i.hasNext())
                    sb.append(", ");
            }
        }
        
        if(sb.length() == 0)
            sb.append("Permissions unknown.");
        
        return sb.toString();
    }
    
    /**
     * Erases all data associated with a verification.
     */
    private void clear() {
        valid = false;
        licenseURL = null;
        expectedURN = null;
        if(permitted != null)
            permitted.clear();
        if(prohibited != null)
            prohibited.clear();
        if(required != null)
            required.clear();
    }
    
    /**
     * Starts verification of the license.
     *
     * The listener is notified when verification is finished.
     */
    public void verify(VerificationListener listener) {
        verified = VERIFYING;
        clear();
        VQUEUE.add(new Verifier(listener));
    }

    /**
     * Retrieves the body of a page that has RDF embedded in it.
     *
     * Returns null if the page could not be found.
     */
    protected String getBody() {
        HttpClient client = HttpClientManager.getNewClient();
        GetMethod get = new GetMethod(licenseLocation.toString());
        get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
        try {
            HttpClientManager.executeMethodRedirecting(client, get);
            return get.getResponseBodyAsString();
        } catch(IOException ioe) {
            LOG.warn("Can't contact license server: " + licenseLocation, ioe);
            return null;
        } finally {
            get.releaseConnection();
        }
    }
    
    
    ///// VERIFICATION CODE ///
    
    /**
     * Verifies the body of the verification page.
     */
    protected boolean doVerification(String body) {
        if(LOG.isTraceEnabled())
            LOG.trace("Attempting to verify: " + body);
        
        if(body == null || body.trim().equals(""))
            return false;
        
        // look for two rdf:RDF's.
        int startRDF = body.indexOf("<rdf:RDF");
        if(startRDF >= body.length() - 1)
            return false;
            
        int endRDF = body.indexOf("rdf:RDF", startRDF+6);
        if(startRDF == -1 || endRDF == -1)
            return false;
        
        // okay, now we know there's a start & end, 
        // get that substring, and do a DOM parsing.
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
            LOG.debug("IOX parsing RDF\n" + rdf, ioe);
            return false;
        } catch (SAXException saxe) {
            LOG.debug("SAX parsing RDF\n" + rdf, saxe);
            return false;
        }
        
        Node doc = parser.getDocument().getDocumentElement();
        NodeList children = doc.getChildNodes();
        boolean workPassed = false;
        for(int i = 0; i < children.getLength(); i++) {
            Node child = (Node)children.item(i);
            if(child.getNodeName().equals("Work"))
                workPassed |= parseWorkItem(child);
            else if(child.getNodeName().equals("License"))
                parseLicenseItem(child);
        }
            
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
                LOG.warn("Bad URN value: " + value, ioe);
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
                LOG.warn("Bad License URL: " + value, murl);
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
                if(!permissions.contains(permission)) {
                    permissions.add(permission);
                    if(LOG.isDebugEnabled())
                        LOG.debug("Added permission: " + permission);
                } else {
                    if(LOG.isWarnEnabled())
                        LOG.warn("Duplicate permission: " + permission + "!");
                }
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
    private class Verifier implements Runnable {
        private final VerificationListener vc;
        
        Verifier(VerificationListener listener) {
            vc = listener;
        }
        
        public void run() {
            valid = doVerification(getBody());
            lastVerifiedTime = System.currentTimeMillis();
            verified = VERIFIED;
            LicenseCache.instance().addVerifiedLicense(CCLicense.this);
            if(vc != null)
                vc.licenseVerified(CCLicense.this);
        }
    }
}
