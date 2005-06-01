package com.limegroup.gnutella.licenses;

import java.io.Serializable;

import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.httpclient.URI;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.DOMException;
import org.xml.sax.InputSource;

/**
 * A concrete implementation of a License, for Weed licenses.
 */
class WeedLicense extends AbstractLicense {
    
    private static final Log LOG = LogFactory.getLog(WeedLicense.class);
    
    private static final long serialVersionUID = 1230497157539025753L;
    
    /** The artist. */
    private String artist;
    
    /** The title. */
    private String title;
    
    /** The price. */
    private String price;
    
    /** Whether or not the license is valid. */
    private boolean valid;
    
    /**
     * Constructs a new WeedLicense.
     */
    WeedLicense(URI uri) {
        super(uri);
    }
    
    /** There is no explicit license text for Weed files. */
    public String getLicense() { return null; }
    
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
        
    /**
     * Determines if the Weed License is valid.
     */
    public boolean isValid(URN urn) {
        return valid;
    }
    
    /**
     * Returns a new WeedLicense with a different URI.
     */
    public License copy(String license, URI licenseURI) {
        WeedLicense newL = null;
        try {
            newL = (WeedLicense)clone();
            newL.licenseLocation = licenseURI;
        } catch(CloneNotSupportedException error) {
            ErrorService.error(error);
        }
        return newL;
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

    /** Clears prior validation information. */    
    protected void clear() {
        valid = false;
        artist = null;
        title = null;
        price = null;
    }

    /**
     * Overriden to retrieve the body of data from a special URI.
     */
    protected String getBody(String url) {
        return super.getBody(url + "&data=1");
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
    protected void parseDocumentNode(Node doc, boolean liveData) {
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
}
