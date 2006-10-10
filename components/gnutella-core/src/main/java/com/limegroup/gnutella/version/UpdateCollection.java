package com.limegroup.gnutella.version;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.EncodingUtils;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * An abstraction for the update XML.
 * Contains the ID & timestamp of the message, as well as the list
 * of UpdateData information for individual messages.
 */
class UpdateCollection {
    
    private static final Log LOG = LogFactory.getLog(UpdateCollection.class);
    
    
    private static DocumentBuilder parser;
    static {
    	try {
    		parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    	} catch (ParserConfigurationException bad) {
    		ErrorService.error(bad);
    		parser = null;
    	}
    }
    
    /**
     * The id of this UpdateCollection.
     */
    private int collectionId = Integer.MIN_VALUE;
    
    /**
     * The timestamp of this collection.
     */
    private long collectionTimestamp = -1;
    
    /**
     * The list of UpdateData's in this collection.
     */
    private List<UpdateData> updateDataList = new LinkedList<UpdateData>();
    
    /**
     * The list of DownloadDatas in this collection.
     */
    private List<DownloadInformation> downloadDataList = new LinkedList<DownloadInformation>();
    
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
    List<UpdateData> getUpdateData() {
        return updateDataList;
    }
    
    /**
     * Gets all updates that have information so we can download them.
     */
    List<DownloadInformation> getUpdatesWithDownloadInformation() {
        return Collections.unmodifiableList(downloadDataList);
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
        for(UpdateData next : updateDataList) {
            if(next.isAllowed(currentV, currentPro, currentStyle, currentJava)) {
                if(lang.equals(next.getLanguage())) {
                    exactMatch = next;
                    break;
                } else if("en".equals(next.getLanguage()) && englishMatch == null) {
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
    	Document d;
        InputSource is = new InputSource(new StringReader(xml));
        try {
            d = parser.parse(is);
        } catch(IOException ioe) {
            LOG.error("Unable to parse: " + xml, ioe);
            return;
        } catch(SAXException sax) {
            LOG.error("Unable to parse: " + xml, sax);
            return;
        }
        
        parseDocumentElement(d.getDocumentElement());
    }
    
    /**
     * Parses the document element.
     *
     * This requires that the element be "update" and has the attribute 'id'.
     * The 'timestamp' attribute is checked (but is optional), as are child 'msg'
     * elements.
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
            Node child = children.item(i);
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
     * The below elements are necessary for downloading the update in the network.
     *      urn      -- The BITPRINT of the download
     *      ucommand -- The command to run to invoke the update.
     *      uname    -- The filename on disk the update should have.
     *      size     -- The size of the update when completed.
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
        String updateURN = getAttributeText(attr, "urn");
        String updateCommand = getAttributeText(attr, "ucommand");
        String updateName = getAttributeText(attr, "uname");
        String fileSize = getAttributeText(attr, "size");
        
        if(updateURN != null) {
            try {
                URN urn = URN.createSHA1Urn(updateURN);
                String tt = URN.getTigerTreeRoot(updateURN);
                data.setUpdateURN(urn);
                data.setUpdateTTRoot(tt);
            } catch(IOException ignored) {
                LOG.warn("Invalid bitprint urn: " + updateURN, ignored);
            }
        }
        
        data.setUpdateCommand(updateCommand);
        data.setUpdateFileName(updateName);
        
        if(fileSize != null) {
            try {
                data.setUpdateSize(Integer.parseInt(fileSize));
            } catch(NumberFormatException nfe) {
                LOG.warn("Invalid size: " + fileSize);
            }
        }
        
        // if this has enough information for downloading, add it to the list of potentials.
        if(data.getUpdateURN() != null && data.getUpdateFileName() != null && data.getSize() != 0) {
            if (LOG.isDebugEnabled())
                LOG.debug("Adding new download data item: " + data);
            downloadDataList.add(data);
        }
        
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
        
        // Update the URL to contain the correct pro & language.
        if(url.indexOf('?') == -1)
            url += "?";
        else
            url += "&";
        url += "pro="   + CommonUtils.isPro() + 
               "&lang=" + encode(ApplicationSettings.getLanguage()) +
               "&lv="   + encode(CommonUtils.getLimeWireVersion()) +
               "&jv="   + encode(CommonUtils.getJavaVersion()) +
               "&os="   + encode(CommonUtils.getOS()) +
               "&osv="  + encode(CommonUtils.getOSVersion());
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
            Node child = children.item(i);
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
        String msg = LimeXMLUtils.getTextContent(lang);
        
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
    
    /**
     * Converts a string into url encoding.
     */
    private String encode(String unencoded) {
        return EncodingUtils.encode(unencoded);
    }
}