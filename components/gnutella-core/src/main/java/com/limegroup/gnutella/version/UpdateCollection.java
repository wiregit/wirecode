package com.limegroup.gnutella.version;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.List;
import java.util.LinkedList;
import java.io.StringReader;
import java.io.IOException;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.DOMException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


class UpdateCollection {
    
    private static final Log LOG = LogFactory.getLog(UpdateCollection.class);
    
    /**
     * The id of this UpdateCollection.
     */
    private long collectionId = -1;
    
    /**
     * The timestamp of this collection.
     */
    private long collectionTimestamp = -1;
    
    /**
     * The list of UpdateData's in this collection.
     */
    private List updateDataList = new LinkedList();
    
    /**
     * Ensure that this is only created by using the factory constructor.
     */
    private UpdateCollection() {}
    
    /**
     * A string rep of the collection.
     */
    public String toString() {
        return "Update Collection, id: " + collectionId + ", timestamp: " + collectionTimestamp +
               ", data: " + updateDataList;
    }
    
    /**
     * Gets the id of this UpdateCollection.
     */
    long getId() {
        return collectionId;
    }
    
    /**
     * Gets the timestamp.
     */
    long getTimestamp() {
        return collectionTimestamp;
    }
    
    /**
     * Gets the UpdateData objects.
     */
    List getUpdateData() {
        return updateDataList;
    }

    /**
     * Constructs and returns a new UpdateCollection that corresponds
     * to the elements in the XML.
     */
    static UpdateCollection create(String xml) {
        UpdateCollection collection = new UpdateCollection();
        collection.parse(xml);
        return collection;
    }
    
    /**
     * Parses the XML and fills in the data of this collection.
     */
    private void parse(String xml) {
        DOMParser parser = new DOMParser();
        InputSource is = new InputSource(new StringReader(xml));
        try {
            parser.parse(is);
        } catch(IOException ioe) {
            LOG.error("Unable to parse: " + xml, ioe);
            return;
        } catch(SAXException sax) {
            LOG.error("Unable to parse: " + xml, sax);
            return;
        }
        
        parseDocumentElement(parser.getDocument().getDocumentElement());
    }
    
    /**
     * Parses the document element.
     */
    private void parseDocumentElement(Node doc) {
        // Ensure the document element is the 'update' element.
        if(!"update".equals(doc.getNodeName()))
            return;
        
        // Parse the 'id' & 'timestamp' attributes.
        NamedNodeMap attr = doc.getAttributes();
        
        // we MUST have an id.
        String idText = getAttributeText(attr, "id");
        if(idText == null) {
            LOG.error("No id attribute.");
            return;
        }
        
        try {
            collectionId = Long.parseLong(idText);
        } catch(NumberFormatException nfe) {
            LOG.error("Couldn't get collection id from: " + idText, nfe);
            return;
        }
        
        // Parse the optional 'timestamp' attribute.
        String timestampText = getAttributeText(attr, "timestamp");
        if(timestampText != null) {
            try {
                collectionTimestamp = Long.parseLong(timestampText);
            } catch(NumberFormatException nfe) {
                LOG.warn("Couldn't get timestamp from: " + timestampText, nfe);
            }
        }
        
        NodeList children = doc.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = (Node)children.item(i);
            if("msg".equals(child.getNodeName()))
                parseMsgItem(child);
        }
    }
    
    /**
     * Parses a single msg item.
     */
    private void parseMsgItem(Node msg) {
        UpdateData data = new UpdateData();
        
        // parse the 'for' & 'os' attributes -- MUST have both.
        NamedNodeMap attr = msg.getAttributes();
        String forText = getAttributeText(attr, "for");
        String osText = getAttributeText(attr, "os");
        if(forText == null || osText == null) {
            LOG.error("no for or os attribute.");
            return;
        }
            
        try {
            data.setOldestVersion(new Version(forText));
        } catch(VersionFormatException vfe) {
            LOG.error("Unable to create version from: " + forText, vfe);
            return;
        }
        
        data.setOS(OS.createFromList(osText));
        
        
        NodeList children = msg.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = (Node)children.item(i);
            if("lang".equals(child.getNodeName()))
                parseLangItem((UpdateData)data.clone(), child);
        }
    }
    
    /**
     * Parses a single lang item.
     */
    private void parseLangItem(UpdateData data, Node lang) {
        // Parse the id & url & current attributes -- all MUST exist.
        NamedNodeMap attr = lang.getAttributes();
        String idText = getAttributeText(attr, "id");
        String urlText = getAttributeText(attr, "url");
        String currentText = getAttributeText(attr, "current");
        String updateText = lang.getTextContent();
        
        if(idText == null || urlText == null || currentText == null || updateText == null) {
            LOG.error("no id, url, current, or text.");
            return;
        }
            
        data.setLanguage(idText);
        
        try {
            data.setUpdateURI(new URI(urlText.toCharArray()));
        } catch(URIException exc) {
            LOG.error("Unable to create uri from: " + urlText);
            return;
        }
        
        try {
            data.setUpdateVersion(new Version(currentText));
        } catch(VersionFormatException vfe) {
            LOG.error("Unable to create version from: " + currentText, vfe);
            return;
        }
        
        data.setUpdateText(updateText);
        
        // A-Okay -- we've got a good UpdateData.
        updateDataList.add(data);
    }
    
    /**
     * Gets the text from an attribute map.
     */
    private String getAttributeText(NamedNodeMap map, String attr) {
        Node node = map.getNamedItem(attr);
        if(node != null)
            return node.getNodeValue();
        else
            return null;
    }
}