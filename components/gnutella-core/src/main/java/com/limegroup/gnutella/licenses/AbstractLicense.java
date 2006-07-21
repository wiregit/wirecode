package com.limegroup.gnutella.licenses;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.ObjectInputStream;

import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.util.CommonUtils;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.URI;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A base license class, implementing common functionality.
 */
abstract class AbstractLicense implements NamedLicense, Serializable, Cloneable {
    
    private static final Log LOG = LogFactory.getLog(AbstractLicense.class);
    
    /**
     * The queue that all license verification attempts are processed in.
     */
    private static final ProcessingQueue VQUEUE = new ProcessingQueue("LicenseVerifier");
    
    private static final long serialVersionUID = 6508972367931096578L;
    
    /** Whether or not this license has been verified. */
    protected transient int verified = UNVERIFIED;
    
    /** The URI where verification will be performed. */
    protected transient URI licenseLocation;
    
    /** The license name. */
    private transient String licenseName;
    
    /** The last time this license was verified. */
    private long lastVerifiedTime;
    
    /** Constructs a new AbstractLicense. */
    AbstractLicense(URI uri) {
        this.licenseLocation = uri;
    }
    
    public void setLicenseName(String name) { this.licenseName = name; }
    
    public boolean isVerifying() { return verified == VERIFYING; }
    public boolean isVerified() { return verified == VERIFIED; }
    public String getLicenseName() { return licenseName; }
    public URI getLicenseURI() { return licenseLocation; }
    public long getLastVerifiedTime() { return lastVerifiedTime; }
    
    /**
     * Assume that all serialized licenses were verified.
     * (Otherwise they wouldn't have been serialized.
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        verified = VERIFIED;
    }
    
    /**
     * Clears all internal state that could be set while verifying.
     */
    protected abstract void clear();
    
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
     * Retrieves the body of a URL from a webserver.
     *
     * Returns null if the page could not be found.
     */
    protected String getBody(String url) {
        return getBodyFromURL(url);
    }
    
    /**
     * Contacts the given URL and downloads returns the body of the
     * HTTP request.
     */
    protected String getBodyFromURL(String url) {
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
    
    /** Parses the document node of the XML. */
    protected abstract void parseDocumentNode(Node node, boolean liveData);
    
    /**
     * Attempts to parse the given XML.
     * The actual handling of the XML is sent to parseDocumentNode,
     * which subclasses can implement as they see fit.
     *
     * If this is a request directly from our Verifier, 'liveData' is true.
     * Subclasses may use this to know where the XML data is coming from.
     */
    protected void parseXML(String xml, boolean liveData) {
        if(xml == null)
            return;
        
        if(LOG.isTraceEnabled())
            LOG.trace("Attempting to parse: " + xml);

        DOMParser parser = new DOMParser();
        InputSource is = new InputSource(new StringReader(xml));
        try {
            parser.parse(is);
        } catch (IOException ioe) {
            LOG.debug("IOX parsing XML\n" + xml, ioe);
            return;
        } catch (SAXException saxe) {
            LOG.debug("SAX parsing XML\n" + xml, saxe);
            return;
        }
        
        parseDocumentNode(parser.getDocument().getDocumentElement(), liveData);
    }
    
    /**
     * Runnable that actually does the verification.
     * This will retrieve the body of a webpage from the licenseURI,
     * parse it, set the last verified time, and cache it in the LicenseCache.
     */
    private class Verifier implements Runnable {
        private final VerificationListener vc;
        
        Verifier(VerificationListener listener) {
            vc = listener;
        }
        
        public void run() {
            String body = getBody(getLicenseURI().toString());
            parseXML(body, true);
            lastVerifiedTime = System.currentTimeMillis();
            verified = VERIFIED;
            LicenseCache.instance().addVerifiedLicense(AbstractLicense.this);
            if(vc != null)
                vc.licenseVerified(AbstractLicense.this);
        }
    }
}