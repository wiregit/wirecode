package com.limegroup.gnutella.licenses;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.net.HttpClientManager;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * A base license class, implementing common functionality.
 */
public abstract class AbstractLicense implements MutableLicense, Serializable, Cloneable {
    
    private static final Log LOG = LogFactory.getLog(AbstractLicense.class);
    
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
    
    void setVerified(int verified) {
        this.verified = verified;
    }
    
    void setLastVerifiedTime(long lastVerifiedTime) {
        this.lastVerifiedTime = lastVerifiedTime;
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
     * Clears all internal state that could be set while verifying.
     */
    protected abstract void clear();
    
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
        get.addRequestHeader("User-Agent", LimeWireUtils.getHttpServer());
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
    protected abstract void parseDocumentNode(Node node, LicenseCache licenseCache);
    
    /**
     * Attempts to parse the given XML.
     * The actual handling of the XML is sent to parseDocumentNode,
     * which subclasses can implement as they see fit.
     *
     * If this is a request directly from our Verifier, 'liveData' is true.
     * Subclasses may use this to know where the XML data is coming from.
     */
    protected void parseXML(String xml, LicenseCache licenseCache) {
        if(xml == null)
            return;
        
        if(LOG.isTraceEnabled())
            LOG.trace("Attempting to parse: " + xml);

        // TODO propagate exceptions and handle in LicenseVerifier
        Document d;
        try {
        	DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        	InputSource is = new InputSource(new StringReader(xml));
            d = parser.parse(is);
        } catch (IOException ioe) {
            LOG.debug("IOX parsing XML\n" + xml, ioe);
            return;
        } catch (SAXException saxe) {
            LOG.debug("SAX parsing XML\n" + xml, saxe);
            return;
        } catch (ParserConfigurationException bad) {
        	LOG.debug("couldn't instantiate parser", bad);
        	return;
        }
        
        parseDocumentNode(d.getDocumentElement(), licenseCache);
    }

    public void verify(LicenseCache licenseCache) {
        setVerified(AbstractLicense.VERIFYING);
        clear();

        String body = getBody(getLicenseURI().toString());
        parseXML(body, licenseCache);
        setLastVerifiedTime(System.currentTimeMillis());
        setVerified(AbstractLicense.VERIFIED);
        
        licenseCache.addVerifiedLicense(this);
    }
    
}