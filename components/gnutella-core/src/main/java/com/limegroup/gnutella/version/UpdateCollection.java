package com.limegroup.gnutella.version;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
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
    private int collectionId = -1;
    
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
    int getId() {
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
     * Gets the UpdateData that is relevant to us.
     * Returns null if there is no relevant update.
     */
    UpdateData getUpdateDataFor(Version currentV, String lang, boolean currentPro,
                                int currentStyle, Version currentJava) {
        UpdateData englishMatch = null;
        UpdateData exactMatch = null;
        
        // Iterate through them till we find an acceptable version.
        // Remember for the 'English' and 'Exact' match --
        // If we got an exact, use that.  Otherwise, use English.
        for(Iterator i = updateDataList.iterator(); i.hasNext(); ) {
            UpdateData next = (UpdateData)i.next();
            if(next.isAllowed(currentV, currentPro, currentStyle, currentJava)) {
                if(lang.equals(next.getLanguage())) {
                    exactMatch = next;
                    break;
                } else if("en".equals(next.getLanguage())) {
                    englishMatch = next;
                }
            }
        }
        
        if(exactMatch == null)
            return englishMatch;
        else
            return exactMatch;
    }

    /**
     * Constructs and returns a new UpdateCollection that corresponds
     * to the elements in the XML.
     */
    static UpdateCollection create(String xml) {
        if(LOG.isTraceEnabled())
            LOG.trace("Parsing Update XML: " + xml);

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
            collectionId = Integer.parseInt(idText);
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
     *
     * The elements this parses are:
     *      from     -- OPTIONAL (defaults to 0.0.0)
     *      to       -- OPTIONAL (defaults to the 'for' value)
     *      for      -- REQUIRED
     *      pro      -- OPTIONAL (see free)
     *      free     -- OPTIONAL (if both pro & free are missing, both default to true.
                                  otherwise they defaults to false.  any non-null value == true)
     *      url      -- REQUIRED
     *      style    -- REQUIRED (accepts a number only.)
     *      javafrom -- OPTIONAL (see javato)
     *      javato   -- OPTIONAL (if both are missing, all ranges are valid.  if one is missing, defaults to above or below that.)
     *      os       -- OPTIONAL (defaults to '*' -- accepts a comma delimited list.)
     *
     * If any values exist but error while parsing, the entire block is considered
     * invalid and ignored.
     */
    private void parseMsgItem(Node msg) {
        UpdateData data = new UpdateData();
        
        NamedNodeMap attr = msg.getAttributes();
        String fromV = getAttributeText(attr, "from");
        String toV = getAttributeText(attr, "to");
        String forV = getAttributeText(attr, "for");
        String pro = getAttributeText(attr, "pro");
        String free = getAttributeText(attr, "free");
        String url = getAttributeText(attr, "url");
        String style = getAttributeText(attr, "style");
        String javaFrom = getAttributeText(attr, "javafrom");
        String javaTo = getAttributeText(attr, "javato");
        String os = getAttributeText(attr, "os");
        
        if(forV == null || url == null || style == null) {
            LOG.error("Missing required for, url, or style.");
            return;
        }
        
        if(fromV == null)
            fromV = "0.0.0";
        if(toV == null)
            toV = forV;

        try {
            data.setFromVersion(new Version(fromV));
            data.setToVersion(new Version(toV));
            data.setForVersion(new Version(forV));
            if(javaFrom != null)
                data.setFromJava(new Version(javaFrom));
            if(javaTo != null)
                data.setToJava(new Version(javaTo));
        } catch(VersionFormatException vfe) {
            LOG.error("Invalid version", vfe);
            return;
        }
        
        if(pro == null && free == null) {
            data.setPro(true);
            data.setFree(true);
        } else {
            data.setPro(pro != null);
            data.setFree(free != null);
        }
        
        data.setUpdateURL(url);
        
        try {
            data.setStyle(Integer.parseInt(style));
        } catch(NumberFormatException nfe) {
            LOG.error("Invalid style", nfe);
            return;
        }
        
        if(os == null)
            os = "*";
        data.setOSList(OS.createFromList(os));
        
        
        NodeList children = msg.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = (Node)children.item(i);
            if("lang".equals(child.getNodeName()))
                parseLangItem((UpdateData)data.clone(), child);
        }
    }
    
    /**
     * Parses a single lang item.
     *
     * Accepts attributes 'id', 'button1', and 'button2'.
     * 'id' is REQUIRD.  others are optional.
     * REQUIRES a text content inside.
     */
    private void parseLangItem(UpdateData data, Node lang) {
        // Parse the id & url & current attributes -- all MUST exist.
        NamedNodeMap attr = lang.getAttributes();
        String id = getAttributeText(attr, "id");
        String button1 = getAttributeText(attr, "button1");
        String button2 = getAttributeText(attr, "button2");
        String title = getAttributeText(attr, "title");
        String msg = lang.getTextContent();
        
        if(id == null || msg == null || msg.equals("")) {
            LOG.error("Missing id or message.");
            return;
        }
            
        data.setLanguage(id);
        data.setButton1Text(button1);
        data.setButton2Text(button2);
        data.setUpdateText(msg);
        data.setUpdateTitle(title);
        
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