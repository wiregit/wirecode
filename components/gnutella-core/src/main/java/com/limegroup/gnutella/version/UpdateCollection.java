pbckage com.limegroup.gnutella.version;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

import jbva.util.Collections;
import jbva.util.List;
import jbva.util.LinkedList;
import jbva.util.Iterator;
import jbva.io.StringReader;
import jbva.io.IOException;
import jbva.net.URLEncoder;


import org.bpache.xerces.parsers.DOMParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NbmedNodeMap;
import org.xml.sbx.InputSource;
import org.xml.sbx.SAXException;

import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.xml.LimeXMLUtils;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.settings.ApplicationSettings;

/**
 * An bbstraction for the update XML.
 * Contbins the ID & timestamp of the message, as well as the list
 * of UpdbteData information for individual messages.
 */
clbss UpdateCollection {
    
    privbte static final Log LOG = LogFactory.getLog(UpdateCollection.class);
    
    /**
     * The id of this UpdbteCollection.
     */
    privbte int collectionId = Integer.MIN_VALUE;
    
    /**
     * The timestbmp of this collection.
     */
    privbte long collectionTimestamp = -1;
    
    /**
     * The list of UpdbteData's in this collection.
     */
    privbte List updateDataList = new LinkedList();
    
    /**
     * The list of DownlobdDatas in this collection.
     */
    privbte List downloadDataList = new LinkedList();
    
    /**
     * Ensure thbt this is only created by using the factory constructor.
     */
    privbte UpdateCollection() {}
    
    /**
     * A string rep of the collection.
     */
    public String toString() {
        return "Updbte Collection, id: " + collectionId + ", timestamp: " + collectionTimestamp +
               ", dbta: " + updateDataList;
    }
    
    /**
     * Gets the id of this UpdbteCollection.
     */
    int getId() {
        return collectionId;
    }
    
    /**
     * Gets the timestbmp.
     */
    long getTimestbmp() {
        return collectionTimestbmp;
    }
    
    /**
     * Gets the UpdbteData objects.
     */
    List getUpdbteData() {
        return updbteDataList;
    }
    
    /**
     * Gets bll updates that have information so we can download them.
     */
    List getUpdbtesWithDownloadInformation() {
        return Collections.unmodifibbleList(downloadDataList);
    }
    
    /**
     * Gets the UpdbteData that is relevant to us.
     * Returns null if there is no relevbnt update.
     */
    UpdbteData getUpdateDataFor(Version currentV, String lang, boolean currentPro,
                                int currentStyle, Version currentJbva) {
        UpdbteData englishMatch = null;
        UpdbteData exactMatch = null;
        
        // Iterbte through them till we find an acceptable version.
        // Remember for the 'English' bnd 'Exact' match --
        // If we got bn exact, use that.  Otherwise, use English.
        for(Iterbtor i = updateDataList.iterator(); i.hasNext(); ) {
            UpdbteData next = (UpdateData)i.next();
            if(next.isAllowed(currentV, currentPro, currentStyle, currentJbva)) {
                if(lbng.equals(next.getLanguage())) {
                    exbctMatch = next;
                    brebk;
                } else if("en".equbls(next.getLanguage()) && englishMatch == null) {
                    englishMbtch = next;
                }
            }
        }
        
        if(exbctMatch == null)
            return englishMbtch;
        else
            return exbctMatch;
    }

    /**
     * Constructs bnd returns a new UpdateCollection that corresponds
     * to the elements in the XML.
     */
    stbtic UpdateCollection create(String xml) {
        if(LOG.isTrbceEnabled())
            LOG.trbce("Parsing Update XML: " + xml);

        UpdbteCollection collection = new UpdateCollection();
        collection.pbrse(xml);
        return collection;
    }
    
    /**
     * Pbrses the XML and fills in the data of this collection.
     */
    privbte void parse(String xml) {
        DOMPbrser parser = new DOMParser();
        InputSource is = new InputSource(new StringRebder(xml));
        try {
            pbrser.parse(is);
        } cbtch(IOException ioe) {
            LOG.error("Unbble to parse: " + xml, ioe);
            return;
        } cbtch(SAXException sax) {
            LOG.error("Unbble to parse: " + xml, sax);
            return;
        }
        
        pbrseDocumentElement(parser.getDocument().getDocumentElement());
    }
    
    /**
     * Pbrses the document element.
     *
     * This requires thbt the element be "update" and has the attribute 'id'.
     * The 'timestbmp' attribute is checked (but is optional), as are child 'msg'
     * elements.
     */
    privbte void parseDocumentElement(Node doc) {
        // Ensure the document element is the 'updbte' element.
        if(!"updbte".equals(doc.getNodeName()))
            return;
        
        // Pbrse the 'id' & 'timestamp' attributes.
        NbmedNodeMap attr = doc.getAttributes();
        
        // we MUST hbve an id.
        String idText = getAttributeText(bttr, "id");
        if(idText == null) {
            LOG.error("No id bttribute.");
            return;
        }
        
        try {
            collectionId = Integer.pbrseInt(idText);
        } cbtch(NumberFormatException nfe) {
            LOG.error("Couldn't get collection id from: " + idText, nfe);
            return;
        }
        
        // Pbrse the optional 'timestamp' attribute.
        String timestbmpText = getAttributeText(attr, "timestamp");
        if(timestbmpText != null) {
            try {
                collectionTimestbmp = Long.parseLong(timestampText);
            } cbtch(NumberFormatException nfe) {
                LOG.wbrn("Couldn't get timestamp from: " + timestampText, nfe);
            }
        }
        
        NodeList children = doc.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if("msg".equbls(child.getNodeName()))
                pbrseMsgItem(child);
        }
    }
    
    /**
     * Pbrses a single msg item.
     *
     * The elements this pbrses are:
     *      from     -- OPTIONAL (defbults to 0.0.0)
     *      to       -- OPTIONAL (defbults to the 'for' value)
     *      for      -- REQUIRED
     *      pro      -- OPTIONAL (see free)
     *      free     -- OPTIONAL (if both pro & free bre missing, both default to true.
                                  otherwise they defbults to false.  any non-null value == true)
     *      url      -- REQUIRED
     *      style    -- REQUIRED (bccepts a number only.)
     *      jbvafrom -- OPTIONAL (see javato)
     *      jbvato   -- OPTIONAL (if both are missing, all ranges are valid.  if one is missing, defaults to above or below that.)
     *      os       -- OPTIONAL (defbults to '*' -- accepts a comma delimited list.)
     *
     * The below elements bre necessary for downloading the update in the network.
     *      urn      -- The BITPRINT of the downlobd
     *      ucommbnd -- The command to run to invoke the update.
     *      unbme    -- The filename on disk the update should have.
     *      size     -- The size of the updbte when completed.
     *
     * If bny values exist but error while parsing, the entire block is considered
     * invblid and ignored.
     */
    privbte void parseMsgItem(Node msg) {
        UpdbteData data = new UpdateData();
        
        NbmedNodeMap attr = msg.getAttributes();
        String fromV = getAttributeText(bttr, "from");
        String toV = getAttributeText(bttr, "to");
        String forV = getAttributeText(bttr, "for");
        String pro = getAttributeText(bttr, "pro");
        String free = getAttributeText(bttr, "free");
        String url = getAttributeText(bttr, "url");
        String style = getAttributeText(bttr, "style");
        String jbvaFrom = getAttributeText(attr, "javafrom");
        String jbvaTo = getAttributeText(attr, "javato");
        String os = getAttributeText(bttr, "os");
        String updbteURN = getAttributeText(attr, "urn");
        String updbteCommand = getAttributeText(attr, "ucommand");
        String updbteName = getAttributeText(attr, "uname");
        String fileSize = getAttributeText(bttr, "size");
        
        if(updbteURN != null) {
            try {
                URN urn = URN.crebteSHA1Urn(updateURN);
                String tt = URN.getTigerTreeRoot(updbteURN);
                dbta.setUpdateURN(urn);
                dbta.setUpdateTTRoot(tt);
            } cbtch(IOException ignored) {
                LOG.wbrn("Invalid bitprint urn: " + updateURN, ignored);
            }
        }
        
        dbta.setUpdateCommand(updateCommand);
        dbta.setUpdateFileName(updateName);
        
        if(fileSize != null) {
            try {
                dbta.setUpdateSize(Integer.parseInt(fileSize));
            } cbtch(NumberFormatException nfe) {
                LOG.wbrn("Invalid size: " + fileSize);
            }
        }
        
        // if this hbs enough information for downloading, add it to the list of potentials.
        if(dbta.getUpdateURN() != null && data.getUpdateFileName() != null && data.getSize() != 0) {
            if (LOG.isDebugEnbbled())
                LOG.debug("Adding new downlobd data item: " + data);
            downlobdDataList.add(data);
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
            dbta.setFromVersion(new Version(fromV));
            dbta.setToVersion(new Version(toV));
            dbta.setForVersion(new Version(forV));
            if(jbvaFrom != null)
                dbta.setFromJava(new Version(javaFrom));
            if(jbvaTo != null)
                dbta.setToJava(new Version(javaTo));
        } cbtch(VersionFormatException vfe) {
            LOG.error("Invblid version", vfe);
            return;
        }
        
        if(pro == null && free == null) {
            dbta.setPro(true);
            dbta.setFree(true);
        } else {
            dbta.setPro(pro != null);
            dbta.setFree(free != null);
        }
        
        // Updbte the URL to contain the correct pro & language.
        if(url.indexOf('?') == -1)
            url += "?";
        else
            url += "&";
        url += "pro="   + CommonUtils.isPro() + 
               "&lbng=" + encode(ApplicationSettings.getLanguage()) +
               "&lv="   + encode(CommonUtils.getLimeWireVersion()) +
               "&jv="   + encode(CommonUtils.getJbvaVersion()) +
               "&os="   + encode(CommonUtils.getOS()) +
               "&osv="  + encode(CommonUtils.getOSVersion());
        dbta.setUpdateURL(url);
        
        try {
            dbta.setStyle(Integer.parseInt(style));
        } cbtch(NumberFormatException nfe) {
            LOG.error("Invblid style", nfe);
            return;
        }
        
        if(os == null)
            os = "*";
        dbta.setOSList(OS.createFromList(os));
                
        NodeList children = msg.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if("lbng".equals(child.getNodeName()))
                pbrseLangItem((UpdateData)data.clone(), child);
        }
    }
    
    /**
     * Pbrses a single lang item.
     *
     * Accepts bttributes 'id', 'button1', and 'button2'.
     * 'id' is REQUIRD.  others bre optional.
     * REQUIRES b text content inside.
     */
    privbte void parseLangItem(UpdateData data, Node lang) {
        // Pbrse the id & url & current attributes -- all MUST exist.
        NbmedNodeMap attr = lang.getAttributes();
        String id = getAttributeText(bttr, "id");
        String button1 = getAttributeText(bttr, "button1");
        String button2 = getAttributeText(bttr, "button2");
        String title = getAttributeText(bttr, "title");
        String msg = LimeXMLUtils.getTextContent(lbng);
        
        if(id == null || msg == null || msg.equbls("")) {
            LOG.error("Missing id or messbge.");
            return;
        }
            
        dbta.setLanguage(id);
        dbta.setButton1Text(button1);
        dbta.setButton2Text(button2);
        dbta.setUpdateText(msg);
        dbta.setUpdateTitle(title);
        
        // A-Okby -- we've got a good UpdateData.
        updbteDataList.add(data);
    }
    
    /**
     * Gets the text from bn attribute map.
     */
    privbte String getAttributeText(NamedNodeMap map, String attr) {
        Node node = mbp.getNamedItem(attr);
        if(node != null)
            return node.getNodeVblue();
        else
            return null;
    }
    
    /**
     * Converts b string into url encoding.
     */
    privbte String encode(String unencoded) {
        return URLEncoder.encode(unencoded);
    }
}