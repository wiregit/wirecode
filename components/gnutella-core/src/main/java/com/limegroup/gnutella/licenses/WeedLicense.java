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
import com.limegroup.gnutella.xml.LimeXMLUtils;

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
 * A concrete implementation of a License, for Weed licenses.
 */
class WeedLicense implements License, Serializable, Cloneable {
    
    private static final Log LOG = LogFactory.getLog(WeedLicense.class);
    
    private static final ProcessingQueue VQUEUE = new ProcessingQueue("WeedLicense");
    
    private static final long serialVersionUID = 1230497157539025753L;
    
    /**
     * Whether or not this license has been verified.
     */
    private transient int verified = UNVERIFIED;
    
    /**
     * The URI where verification will be performed.
     */
    private transient URI licenseLocation;
    
    /**
     * The last time this license was verified.
     */
    private long lastVerifiedTime;
    
    /** The artist. */
    private String artist;
    
    /** The title. */
    private String title;
    
    /** The price. */
    private String price;
    
    /** Whether or not the license is valid. */
    private boolean valid;
    
    /** The license name. */
    private transient String licenseName;
    
    /**
     * Constructs a new WeedLicense.
     */
    WeedLicense(URI uri, String name) {
        this.licenseLocation = uri;
        this.licenseName = name;
    }
    
    public boolean isVerifying() {
        return verified == VERIFYING;
    }
    
    public boolean isVerified() {
        return verified == VERIFIED;
    }
    
    public String getLicenseName() { return licenseName; }
    
    public String getLicense() {
        return "Play, Buy, Share";
    }
    
    /**
     * Retrieves the license deed for the given URN.
     */
    public URL getLicenseDeed(URN urn) {
        try {
            return new URL("http://weedshare.com/company/policies/summary_usage_rights.aspx");
        } catch(MalformedURLException murl) {
            return null;
        }
    }
    
    public long getLastVerifiedTime() {
        return lastVerifiedTime;
    }
    
    public URI getLicenseURI() {
        return licenseLocation;
    }
        
    /**
     * Determines if the Weed License is valid.
     */
    public boolean isValid(URN urn) {
        return valid;
    }
    
    /**
     * Returns a new WeedLicense with a different URI.
     */
    public License copy(String license, URI licenseURI, String name) {
        WeedLicense newL = null;
        try {
            newL = (WeedLicense)clone();
            newL.licenseLocation = licenseURI;
            newL.licenseName = name;
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
        return "Artist: " + artist + "\n"
             + "Title: " + title + "\n"
             + "Price: " + price;
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

    /** Clears prior validation information. */    
    private void clear() {
        valid = false;
        artist = null;
        title = null;
        price = null;
    }

    /**
     * Retrieves the body of a URL.
     *
     * Returns null if the page could not be found.
     */
    protected String getBody(String url) {
        url += "&data=1";
        
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
    
    /**
     * Parses the XML sent back from the Weed server.
     * The XML should look like:
     *  <WeedVerifyData>
	 *       <Status>Verified</Status>
	 *       <Artist>Roger Joseph Manning, Jr.</Artist>
	 *       <Title>What You Don't Know About the Girl</Title>
	 *       <Price>1.2500</Price>
     *  </WeedVerifyData>
     */
    protected void doVerification(String xml) {
        if(LOG.isTraceEnabled())
            LOG.trace("Attempting to verify: " + xml);

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
        
        Node doc = parser.getDocument().getDocumentElement();
        if(!doc.getNodeName().equals("WeedVerifyData"))
            return;
        
        NodeList children = doc.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = (Node)children.item(i);
            String name = child.getNodeName();
            String value = LimeXMLUtils.getTextContent(child);
            if(name == null || value == null)
                continue;
                
            if(name.equals("Status"))
                valid = value.equals("Verified");
            else if(name.equals("Artist"))
                artist = value;
            else if(name.equals("Title"))
                title = value;
            else if(name.equals("Price"))
                price = value;
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
            verified = VERIFIED;
            LicenseCache.instance().addVerifiedLicense(WeedLicense.this);
            if(vc != null)
                vc.licenseVerified(WeedLicense.this);
        }
    }
}
