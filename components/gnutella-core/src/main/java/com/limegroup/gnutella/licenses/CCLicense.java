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
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

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
     * The URI where verification will be performed.
     */
    private transient URI licenseLocation;
    
    /**
     * The license string.
     */
    private transient String license;
    
    /**
     * The last time this license was verified.
     */
    private long lastVerifiedTime;    
    
    /**
     * The license information for each Work.
     */
    private Map /* URN -> Work */ allWorks;
    
    /**
     * Constructs a new CCLicense.
     */
    CCLicense(String license, URI uri) {
        this.license = license;
        this.licenseLocation = uri;
    }
    
    
    public boolean isVerifying() {
        return verified == VERIFYING;
    }
    
    public boolean isVerified() {
        return verified == VERIFIED;
    }
    
    public String getLicense() {
        return license;
    }
    
    /**
     * Retrieves the license deed for the given URN.
     */
    public URL getLicenseDeed(URN urn) {
        Work work = getWork(urn);
        if(work == null || work.licenseURL == null)
            return guessLicenseDeed();
        else
            return work.licenseURL;
    }
    
    public long getLastVerifiedTime() {
        return lastVerifiedTime;
    }
    
    public URI getLicenseURI() {
        return licenseLocation;
    }

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
        Work work = getWork(urn);
        if(work == null)
            return false;
            
        if(work.expectedURN == null || urn == null)
            return true;
            
        return work.expectedURN.equals(urn);
    }
    
    /**
     * Returns a CCLicense exactly like this, except
     * with a different license string.
     */
    public License copy(String license, URI licenseURI) {
        CCLicense newL = null;
        try {
            newL = (CCLicense)clone();
            newL.license = license;
            newL.licenseLocation = licenseURI;
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
    public String getLicenseDescription(URN urn) {
        List permitted = Collections.EMPTY_LIST;
        List prohibited = Collections.EMPTY_LIST;
        List required = Collections.EMPTY_LIST;
        Work work = getWork(urn);
        if(work != null) {
            permitted = work.permitted;
            prohibited = work.prohibited;
            required = work.required;
        }
        
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
        if(allWorks != null)
            allWorks.clear();
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
     * Retrieves the body of a URL.
     *
     * Returns null if the page could not be found.
     */
    protected String getBody(String url) {
        if(LOG.isTraceEnabled())
            LOG.trace("Contacting: " + url);
        
        HttpClient client = HttpClientManager.getNewClient();
        GetMethod get = new GetMethod(url);
        get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
        try {
            HttpClientManager.executeMethodRedirecting(client, get);
            return get.getResponseBodyAsString();
        } catch(IOException ioe) {
            LOG.warn("Can't contact license server: " + url, ioe);
            return null;
        } finally {
            get.releaseConnection();
        }
    }
    
    ///// WORK CODE ///
    
    
    /**
     * Adds the given work URN.
     */
    private void addWork(URN urn, String licenseURL) {
        URL url = null;
        try {
            url = new URL(licenseURL);
        } catch(MalformedURLException murl) {
            LOG.warn("Unable to make licenseURL out of: " + licenseURL, murl);
        }
        
        //See if we can refocus an existing licenseURL.
        Work work = getWork(urn);
        if(work != null) {
            if(LOG.isDebugEnabled())
                LOG.debug("Found existing work item for URN: " + urn);
            if(url != null) {
                URL guessed = guessLicenseDeed();
                if(guessed != null && guessed.equals(url)) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Updating license URL to be: " + url);
                    work.licenseURL = url;
                }
            }
                
            // Otherwise, not much else we can do.
            // We already have a Work for this URN and it has
            // a licenseURL already.
            return;
        }
        
        // There's no existing work for this item, so lets add one.
        work = new Work(urn, url);
        if(LOG.isDebugEnabled())
            LOG.debug("Adding new " + work);

        if(allWorks == null)
            allWorks = new HashMap();
        allWorks.put(urn, work); // it is fine if urn is null.
    }   
    
    /**
     * Locates a work for a given URN.
     */
    private Work getWork(URN urn) {
        if(allWorks == null)
            return null;
        
        // First see if there's a work that matches exactly.
        Work work = (Work)allWorks.get(urn);
        if(work != null)
            return work;
            
        // Okay, nothing matched.
        
        // If we want a specific URN, we can only give back the 'null' one.
        if(urn != null)
            return (Work)allWorks.get(null);
        
        // We must have wanted the null one.  Give back the first one we find.
        return (Work)allWorks.values().iterator().next();
    }
    
    /**
     * Locates all works that use the given License URL.
     */
    private List getWorksForLicenseURL(URL url) {
        if(allWorks == null || url == null)
            return Collections.EMPTY_LIST;
        
        List works = new LinkedList();
        for(Iterator i = allWorks.values().iterator(); i.hasNext(); ) {
            Work work = (Work)i.next();
            if(work.licenseURL != null && url.equals(work.licenseURL))
                works.add(work);
        }
        return works;
    }
    
    /**
     * A single work.
     */
    private static class Work implements Serializable {
        private static final long serialVersionUID =  -1719502030054241350L;
                
        URN expectedURN;
        URL licenseURL;
        List required;
        List permitted;
        List prohibited;
        
        // for de-serializing.
        Work() { }
        
        Work(URN urn, URL url) {
            expectedURN = urn;
            licenseURL = url;
        }
        
        boolean isDescriptionAvailable() {
            return required != null || permitted != null || prohibited != null;
        }
        
        public String toString() {
            return "work:: urn:" + expectedURN + ", license: " + licenseURL;
        }
    }   
    
    ///// VERIFICATION CODE ///
    
    /**
     * Locates RDF from a big string of HTML.
     */
    private String locateRDF(String body) {
        if(body == null || body.trim().equals(""))
            return null;
        
        // look for two rdf:RDF's.
        int startRDF = body.indexOf("<rdf:RDF");
        if(startRDF >= body.length() - 1)
            return null;
            
        int endRDF = body.indexOf("rdf:RDF", startRDF+6);
        if(startRDF == -1 || endRDF == -1)
            return null;
        
        // find the closing tag.
        endRDF = body.indexOf('>', endRDF);
        if(endRDF == -1)
            return null;
        
        // Alright, we got where the rdf is at!
        return body.substring(startRDF, endRDF + 1);
    }
    
    /**
     * Verifies the body of the verification page.
     */
    protected void doVerification(String body) {
        if(LOG.isTraceEnabled())
            LOG.trace("Attempting to verify: " + body);
            
        String rdf = locateRDF(body);
        if(rdf == null)
            return;

        DOMParser parser = new DOMParser();
        InputSource is = new InputSource(new StringReader(rdf));
        try {
            parser.parse(is);
        } catch (IOException ioe) {
            LOG.debug("IOX parsing RDF\n" + rdf, ioe);
            return;
        } catch (SAXException saxe) {
            LOG.debug("SAX parsing RDF\n" + rdf, saxe);
            return;
        }
        
        Node doc = parser.getDocument().getDocumentElement();
        NodeList children = doc.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = (Node)children.item(i);
            if(child.getNodeName().equals("Work"))
                parseWorkItem(child);
            else if(child.getNodeName().equals("License"))
                parseLicenseItem(child);
        }
            
        // so long as we found a valid work item, we're good.
        return;
    }
    
    /**
     * Ensures the 'work' item exists and retrieves the URN, if it exists.
     */
    protected void parseWorkItem(Node work) {
        if(LOG.isTraceEnabled())
            LOG.trace("Parsing work item.");
         
        // Get the URN of this Work item.   
        NamedNodeMap attributes = work.getAttributes();
        Node about = attributes.getNamedItem("rdf:about");
        URN expectedURN = null;
        if(about != null) {
            // attempt to create a SHA1 urn out of it.
            try {
                expectedURN = URN.createSHA1Urn(about.getNodeValue());
            } catch(IOException ioe) {}
        }
        
        // Get the license child element.
        NodeList children = work.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = (Node)children.item(i);
            if(child.getNodeName().equals("license")) {
                attributes = child.getAttributes();
                Node resource = attributes.getNamedItem("rdf:resource");
                // if we found a resource, attempt to add the Work.
                if(resource != null)
                    addWork(expectedURN, resource.getNodeValue());
            }
        }
        
        // other than it existing, nothing else needs to happen.
        return;
    }
    
    /**
     * Parses the 'license' item.
     */
    protected void parseLicenseItem(Node license) {
        if(LOG.isTraceEnabled())
            LOG.trace("Parsing license item.");
           
        // Get the license URL. 
        NamedNodeMap attributes = license.getAttributes();
        Node about = attributes.getNamedItem("rdf:about");
        List works = Collections.EMPTY_LIST;
        if(about != null) {
            String value = about.getNodeValue();
            try {
                works = getWorksForLicenseURL(new URL(value));
            } catch(MalformedURLException murl) {
                LOG.warn("Unable to create license URL for: " + value, murl);
            }
        }
        
        // Optimization:  If no works, exit early.
        if(!works.iterator().hasNext())
            return;
        
        List required = null;
        List prohibited = null;
        List permitted = null;
        
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
        
        // Okay, now iterate through each work and set the lists.
        for(Iterator i = works.iterator(); i.hasNext(); ) {
            Work work = (Work)i.next();
            if(LOG.isDebugEnabled())
                LOG.debug("Setting license details for " + work);
            work.required = required;
            work.prohibited = prohibited;
            work.permitted = permitted;
        }
        
        return;
    }
    
    /**
     * Adds a single permission to the list.
     */
    private void addPermission(List permissions, Node node) {
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
            LOG.warn("No resource item for permission.");
        } 
    }
    
    /**
     * Updates the license details, potentially retrieving information
     * from the licenseURL in each Work.
     */
    private void updateLicenseDetails() {
        if(allWorks == null)
            return;
        
        for(Iterator i = allWorks.values().iterator(); i.hasNext(); ) {
            Work work = (Work)i.next();
            if(!work.isDescriptionAvailable() && work.licenseURL != null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Updating licenseURL for work: " + work);
                
                String url = work.licenseURL.toExternalForm();
                // First see if we have cached details.
                Object details = LicenseCache.instance().getDetails(url);
                String body = null;
                if(details != null && details instanceof String) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Using cached details for url: " + url);
                    body = (String)details;
                } else {
                    body = getBody(url);
                    if(body != null)
                        LicenseCache.instance().addDetails(url, body);
                }
                
                if(body != null)
                    doVerification(body);
             }
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
            doVerification(getBody(licenseLocation.toString()));
            lastVerifiedTime = System.currentTimeMillis();
            updateLicenseDetails();
            verified = VERIFIED;
            LicenseCache.instance().addVerifiedLicense(CCLicense.this);
            if(vc != null)
                vc.licenseVerified(CCLicense.this);
        }
    }
}
