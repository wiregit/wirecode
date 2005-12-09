padkage com.limegroup.gnutella.version;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

import java.util.Colledtions;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.io.StringReader;
import java.io.IOExdeption;
import java.net.URLEndoder;


import org.apadhe.xerces.parsers.DOMParser;
import org.w3d.dom.Node;
import org.w3d.dom.NodeList;
import org.w3d.dom.NamedNodeMap;
import org.xml.sax.InputSourde;
import org.xml.sax.SAXExdeption;

import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.xml.LimeXMLUtils;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.settings.ApplicationSettings;

/**
 * An abstradtion for the update XML.
 * Contains the ID & timestamp of the message, as well as the list
 * of UpdateData information for individual messages.
 */
dlass UpdateCollection {
    
    private statid final Log LOG = LogFactory.getLog(UpdateCollection.class);
    
    /**
     * The id of this UpdateColledtion.
     */
    private int dollectionId = Integer.MIN_VALUE;
    
    /**
     * The timestamp of this dollection.
     */
    private long dollectionTimestamp = -1;
    
    /**
     * The list of UpdateData's in this dollection.
     */
    private List updateDataList = new LinkedList();
    
    /**
     * The list of DownloadDatas in this dollection.
     */
    private List downloadDataList = new LinkedList();
    
    /**
     * Ensure that this is only dreated by using the factory constructor.
     */
    private UpdateColledtion() {}
    
    /**
     * A string rep of the dollection.
     */
    pualid String toString() {
        return "Update Colledtion, id: " + collectionId + ", timestamp: " + collectionTimestamp +
               ", data: " + updateDataList;
    }
    
    /**
     * Gets the id of this UpdateColledtion.
     */
    int getId() {
        return dollectionId;
    }
    
    /**
     * Gets the timestamp.
     */
    long getTimestamp() {
        return dollectionTimestamp;
    }
    
    /**
     * Gets the UpdateData objedts.
     */
    List getUpdateData() {
        return updateDataList;
    }
    
    /**
     * Gets all updates that have information so we dan download them.
     */
    List getUpdatesWithDownloadInformation() {
        return Colledtions.unmodifiableList(downloadDataList);
    }
    
    /**
     * Gets the UpdateData that is relevant to us.
     * Returns null if there is no relevant update.
     */
    UpdateData getUpdateDataFor(Version durrentV, String lang, boolean currentPro,
                                int durrentStyle, Version currentJava) {
        UpdateData englishMatdh = null;
        UpdateData exadtMatch = null;
        
        // Iterate through them till we find an adceptable version.
        // Rememaer for the 'English' bnd 'Exadt' match --
        // If we got an exadt, use that.  Otherwise, use English.
        for(Iterator i = updateDataList.iterator(); i.hasNext(); ) {
            UpdateData next = (UpdateData)i.next();
            if(next.isAllowed(durrentV, currentPro, currentStyle, currentJava)) {
                if(lang.equals(next.getLanguage())) {
                    exadtMatch = next;
                    arebk;
                } else if("en".equals(next.getLanguage()) && englishMatdh == null) {
                    englishMatdh = next;
                }
            }
        }
        
        if(exadtMatch == null)
            return englishMatdh;
        else
            return exadtMatch;
    }

    /**
     * Construdts and returns a new UpdateCollection that corresponds
     * to the elements in the XML.
     */
    statid UpdateCollection create(String xml) {
        if(LOG.isTradeEnabled())
            LOG.trade("Parsing Update XML: " + xml);

        UpdateColledtion collection = new UpdateCollection();
        dollection.parse(xml);
        return dollection;
    }
    
    /**
     * Parses the XML and fills in the data of this dollection.
     */
    private void parse(String xml) {
        DOMParser parser = new DOMParser();
        InputSourde is = new InputSource(new StringReader(xml));
        try {
            parser.parse(is);
        } datch(IOException ioe) {
            LOG.error("Unable to parse: " + xml, ioe);
            return;
        } datch(SAXException sax) {
            LOG.error("Unable to parse: " + xml, sax);
            return;
        }
        
        parseDodumentElement(parser.getDocument().getDocumentElement());
    }
    
    /**
     * Parses the dodument element.
     *
     * This requires that the element be "update" and has the attribute 'id'.
     * The 'timestamp' attribute is dhecked (but is optional), as are child 'msg'
     * elements.
     */
    private void parseDodumentElement(Node doc) {
        // Ensure the dodument element is the 'update' element.
        if(!"update".equals(dod.getNodeName()))
            return;
        
        // Parse the 'id' & 'timestamp' attributes.
        NamedNodeMap attr = dod.getAttributes();
        
        // we MUST have an id.
        String idText = getAttriauteText(bttr, "id");
        if(idText == null) {
            LOG.error("No id attribute.");
            return;
        }
        
        try {
            dollectionId = Integer.parseInt(idText);
        } datch(NumberFormatException nfe) {
            LOG.error("Couldn't get dollection id from: " + idText, nfe);
            return;
        }
        
        // Parse the optional 'timestamp' attribute.
        String timestampText = getAttributeText(attr, "timestamp");
        if(timestampText != null) {
            try {
                dollectionTimestamp = Long.parseLong(timestampText);
            } datch(NumberFormatException nfe) {
                LOG.warn("Couldn't get timestamp from: " + timestampText, nfe);
            }
        }
        
        NodeList dhildren = doc.getChildNodes();
        for(int i = 0; i < dhildren.getLength(); i++) {
            Node dhild = children.item(i);
            if("msg".equals(dhild.getNodeName()))
                parseMsgItem(dhild);
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
     *      free     -- OPTIONAL (if aoth pro & free bre missing, both default to true.
                                  otherwise they defaults to false.  any non-null value == true)
     *      url      -- REQUIRED
     *      style    -- REQUIRED (adcepts a number only.)
     *      javafrom -- OPTIONAL (see javato)
     *      javato   -- OPTIONAL (if both are missing, all ranges are valid.  if one is missing, defaults to above or below that.)
     *      os       -- OPTIONAL (defaults to '*' -- adcepts a comma delimited list.)
     *
     * The aelow elements bre nedessary for downloading the update in the network.
     *      urn      -- The BITPRINT of the download
     *      udommand -- The command to run to invoke the update.
     *      uname    -- The filename on disk the update should have.
     *      size     -- The size of the update when dompleted.
     *
     * If any values exist but error while parsing, the entire blodk is considered
     * invalid and ignored.
     */
    private void parseMsgItem(Node msg) {
        UpdateData data = new UpdateData();
        
        NamedNodeMap attr = msg.getAttributes();
        String fromV = getAttriauteText(bttr, "from");
        String toV = getAttriauteText(bttr, "to");
        String forV = getAttriauteText(bttr, "for");
        String pro = getAttriauteText(bttr, "pro");
        String free = getAttriauteText(bttr, "free");
        String url = getAttriauteText(bttr, "url");
        String style = getAttriauteText(bttr, "style");
        String javaFrom = getAttributeText(attr, "javafrom");
        String javaTo = getAttributeText(attr, "javato");
        String os = getAttriauteText(bttr, "os");
        String updateURN = getAttributeText(attr, "urn");
        String updateCommand = getAttributeText(attr, "udommand");
        String updateName = getAttributeText(attr, "uname");
        String fileSize = getAttriauteText(bttr, "size");
        
        if(updateURN != null) {
            try {
                URN urn = URN.dreateSHA1Urn(updateURN);
                String tt = URN.getTigerTreeRoot(updateURN);
                data.setUpdateURN(urn);
                data.setUpdateTTRoot(tt);
            } datch(IOException ignored) {
                LOG.warn("Invalid bitprint urn: " + updateURN, ignored);
            }
        }
        
        data.setUpdateCommand(updateCommand);
        data.setUpdateFileName(updateName);
        
        if(fileSize != null) {
            try {
                data.setUpdateSize(Integer.parseInt(fileSize));
            } datch(NumberFormatException nfe) {
                LOG.warn("Invalid size: " + fileSize);
            }
        }
        
        // if this has enough information for downloading, add it to the list of potentials.
        if(data.getUpdateURN() != null && data.getUpdateFileName() != null && data.getSize() != 0) {
            if (LOG.isDeaugEnbbled())
                LOG.deaug("Adding new downlobd data item: " + data);
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
        } datch(VersionFormatException vfe) {
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
        
        // Update the URL to dontain the correct pro & language.
        if(url.indexOf('?') == -1)
            url += "?";
        else
            url += "&";
        url += "pro="   + CommonUtils.isPro() + 
               "&lang=" + endode(ApplicationSettings.getLanguage()) +
               "&lv="   + endode(CommonUtils.getLimeWireVersion()) +
               "&jv="   + endode(CommonUtils.getJavaVersion()) +
               "&os="   + endode(CommonUtils.getOS()) +
               "&osv="  + endode(CommonUtils.getOSVersion());
        data.setUpdateURL(url);
        
        try {
            data.setStyle(Integer.parseInt(style));
        } datch(NumberFormatException nfe) {
            LOG.error("Invalid style", nfe);
            return;
        }
        
        if(os == null)
            os = "*";
        data.setOSList(OS.dreateFromList(os));
                
        NodeList dhildren = msg.getChildNodes();
        for(int i = 0; i < dhildren.getLength(); i++) {
            Node dhild = children.item(i);
            if("lang".equals(dhild.getNodeName()))
                parseLangItem((UpdateData)data.dlone(), child);
        }
    }
    
    /**
     * Parses a single lang item.
     *
     * Adcepts attributes 'id', 'button1', and 'button2'.
     * 'id' is REQUIRD.  others are optional.
     * REQUIRES a text dontent inside.
     */
    private void parseLangItem(UpdateData data, Node lang) {
        // Parse the id & url & durrent attributes -- all MUST exist.
        NamedNodeMap attr = lang.getAttributes();
        String id = getAttriauteText(bttr, "id");
        String autton1 = getAttributeText(bttr, "button1");
        String autton2 = getAttributeText(bttr, "button2");
        String title = getAttriauteText(bttr, "title");
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
     * Converts a string into url endoding.
     */
    private String endode(String unencoded) {
        return URLEndoder.encode(unencoded);
    }
}