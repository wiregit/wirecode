package com.limegroup.gnutella.metadata;

import java.io.StringReader;
import java.io.IOException;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.DOMException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * Encapsulates information about Weedified files.
 * See http://www.weedshare.com.
 */
public class WeedInfo {
    
    private static final Log LOG = LogFactory.getLog(WeedInfo.class);
    
    public static final String LAINFO = "http://www.shmedlic.com/license/3play.aspx";
    public static final String LDIST  = "Shared Media Licensing, Inc.";
    public static final String LURL   = "http://www.shmedlic.com/";
    
    private String _versionId, _contentId;
    private String _licenseDate, _licenseDistributor, _licenseDistributorURL;
    private String _publishDate;
    private String _contentDistributor, _contentDistributorURL;
    private String _price, _collection, _description, _copyright;
    private String _artistURL, _author, _title;
    private String _lainfo;
    
    /**
     * Constructs a new WeedInfo based off the given XML.
     * If the XML is malformed, or is not a Weed file, throws an IllegalArgumentException.
     */
    public WeedInfo(String xml) throws IllegalArgumentException {
        //The XML should look something like:
        //<WRMHEADER version="2.0.0.0">
        //    <DATA>
        //        <VersionID>0000000000001370651</VersionID>
        //        <ContentID>214324</ContentID>
        //        <ice9>ice9</ice9>
        //        <License_Date></License_Date>
        //        <License_Distributor_URL>http://www.shmedlic.com/</License_Distributor_URL>
        //        <License_Distributor>Shared Media Licensing, Inc.</License_Distributor>
        //        <Publish_Date>4/14/2005 4:13:50 PM</Publish_Date>
        //        <Content_Distributor_URL>http://www.presidentsrock.com</Content_Distributor_URL>
        //        <Content_Distributor>PUSA Inc.</Content_Distributor>
        //        <Price>0.9900</Price>
        //        <Collection>Love Everybody</Collection>
        //        <Description></Description>
        //        <Copyright>2004 PUSA Inc.</Copyright>
        //        <Artist_URL>http://www.presidentsrock.com</Artist_URL>
        //        <Author>The Presidents of the United States of America</Author>
        //        <Title>Love Everybody</Title>
        //        <SECURITYVERSION>2.2</SECURITYVERSION>
        //        <CID>o9miGn4Z0k2gUeHhN9VxTA==</CID>
        //        <LAINFO>http://www.shmedlic.com/license/3play.aspx</LAINFO>
        //        <KID>ERVOYkZ8qkWZ75OQw9ihnA==</KID>
        //        <CHECKSUM>t1ZpoYJF2w==</CHECKSUM>
        //    </DATA>
        //    <SIGNATURE>
        //        <HASHALGORITHM type="SHA"></HASHALGORITHM>
        //        <SIGNALGORITHM type="MSDRM"></SIGNALGORITHM>
        //        <VALUE>XZkWZWCq919yum!bBGdxvnpiS38npAqAofxT8AkegyJ27zTlb9v4gA==</VALUE>
        //    </SIGNATURE>
        //</WRMHEADER> 
        
        parse(xml);
        
        if(!LAINFO.equals(_lainfo))
            throw new IllegalArgumentException("Invalid LAINFO: " + _lainfo);
        if(!LURL.equals(_licenseDistributorURL))
            throw new IllegalArgumentException("Invalid LURL: " + _licenseDistributorURL);
        if(!LDIST.equals(_licenseDistributor))
            throw new IllegalArgumentException("Invalid LDIST: " + _licenseDistributor);
    }
    
    public String getVersionId() { return _versionId; }
    public String getContentId() { return _contentId; }
    public String getLicenseDate() { return _licenseDate; }
    public String getLicenseDistributorURL() { return _licenseDistributor; }
    public String getLicenseDistributor() { return _licenseDistributorURL; }
    public String getPublishDate() { return _publishDate; }
    public String getContentDistributor() { return _contentDistributor; }
    public String getContentDistrubutorURL() { return _contentDistributorURL; }
    public String getPrice() { return _price; }
    public String getCollection() { return _collection; }
    public String getDescription() { return _description; }
    public String getAuthor() { return _author; }
    public String getArtistURL() { return _artistURL; }
    public String getTitle() { return _title; }
    public String getLAInfo()    { return _lainfo; }
    
    
    /** Parses the content encryption XML looking for Weed info. */
    private void parse(String xml) {
        DOMParser parser = new DOMParser();
        InputSource is = new InputSource(new StringReader(xml));
        try {
            parser.parse(is);
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        } catch (SAXException saxe) {
            throw new IllegalArgumentException(saxe);
        }
        
        Node doc = parser.getDocument().getDocumentElement();
        if(doc.getNodeName().equals("WRMHEADER")) {
            NodeList children = doc.getChildNodes();
            for(int i = 0; i < children.getLength(); i++) {
                Node child = (Node)children.item(i);
                if(child.getNodeName().equals("DATA")) {
                    parseData(child);
                    break;
                }
            }
        }
    }
    
    /** Parses the data element, looking for all weed info. */
    private void parseData(Node data) {
        NodeList children = data.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = (Node)children.item(i);
            String name = child.getNodeName();
            String value = LimeXMLUtils.getTextContent(child);
            if(name.equals("ContentID"))
                _contentId = value;
            else if(name.equals("License_Date"))
                _licenseDate = value;
            else if(name.equals("License_Distributor"))
                _licenseDistributor = value;
            else if(name.equals("License_Distributor_URL"))
                _licenseDistributorURL = value;
            else if(name.equals("Publish_Date"))
                _publishDate = value;
            else if(name.equals("Content_Distributor"))
                _contentDistributor = value;
            else if(name.equals("Content_Distributor_URL"))
                _contentDistributorURL = value;
            else if(name.equals("Price"))
                _price = value;
            else if(name.equals("Collection"))
                _collection = value;
            else if(name.equals("Description"))
                _description = value;
            else if(name.equals("Copyright"))
                _copyright = value;
            else if(name.equals("Artist_URL"))
                _artistURL = value;
            else if(name.equals("Author"))
                _author = value;
            else if(name.equals("Title"))
                _title = value;
            else if(name.equals("LAINFO"))
                _lainfo = value;
        }
    }
}