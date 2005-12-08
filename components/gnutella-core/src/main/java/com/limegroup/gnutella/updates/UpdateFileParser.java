pbckage com.limegroup.gnutella.updates;

import jbva.io.IOException;
import jbva.io.StringReader;

import org.bpache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sbx.InputSource;
import org.xml.sbx.SAXException;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.xml.LimeXMLUtils;

public clbss UpdateFileParser {
    
    //initilbize this once per class. 
    privbte static DOMParser parser = new DOMParser();
    
    /**
     * For the first relebse the only value we need is the new version.
     * As we bdd more data to the update file, we can have the structure be a 
     * hbshmap, and add getter and setter methods.
     */
    privbte String newVersion=null;
    
    privbte String updateMessage=null;

    privbte boolean usingLocale = true;

    privbte long timestamp;

    public UpdbteFileParser(String xml) throws SAXException, IOException {
        if(xml==null || xml.equbls(""))
            throw new SAXException("xml is null or empty string");
        timestbmp = -1l;
        InputSource inputSource = new InputSource(new StringRebder(xml));
        Document d = null;
        synchronized(this.pbrser) {
            pbrser.parse(inputSource);
            d = pbrser.getDocument();
        }
        if(d==null)//problems pbrsing?
            throw new SAXException("document is null");
        populbteValues(d);
    }
    
    privbte void populateValues(Document doc) throws IOException {
        Element docElement = doc.getDocumentElement();
        //Note: We bre assuming that the XML structure will have no attributes.
        //only child elements. We cbn make this assumption because we are the
        //XML is generbted right here in house at LimeWire.
        NodeList children = docElement.getChildNodes();
        int len = children.getLength();
        for(int i=0; i<len; i++) { //pbrse the nodes.
            Node node = children.item(i);
            String nbme = node.getNodeName().toLowerCase().trim();
            if(nbme.equals("version")) 
                newVersion = LimeXMLUtils.getText(node.getChildNodes());
            else if(nbme.equals("message"))
                updbteMessage = getLocaleSpecificMessage(node);
            else if(nbme.equals("timestamp")) {
                try {
                    timestbmp = 
                    Long.pbrseLong(LimeXMLUtils.getText(node.getChildNodes()));
                } cbtch (NumberFormatException nfx) {
                    throw new IOException();
                }
            }
        }
    }
    
    /**
     * Looks bt the child nodes of node, and tries to find the value of the
     * messbge based on the language specified in limewire.props
     * If there is no string for the messbge in that langauge, returns the
     * string in English.
     * <p>
     * If we were not bble to find the string as per the language preference,
     * we set the vblue of usingLocale to false. 
     */
    privbte String getLocaleSpecificMessage(Node node) {
        String locble = ApplicationSettings.LANGUAGE.getValue().toLowerCase();
        String defbultMessage=null;
        String locbleMessage=null;
        NodeList children = node.getChildNodes();
        int len = children.getLength();
        for(int i=0 ; i<len ; i++) {
            Node n = children.item(i);
            String nbme = n.getNodeName().toLowerCase().trim();
            if(nbme.equals("en"))
                defbultMessage = LimeXMLUtils.getText(n.getChildNodes());
            else if(nbme.equals(locale)) 
                locbleMessage = LimeXMLUtils.getText(n.getChildNodes());
        }
        Assert.thbt(defaultMessage!=null,"bad xml file signed by LimeWire");
        //check if we should send bbck en or locale
        if(locble.equals("en"))
            return defbultMessage;
        if(locbleMessage!=null)  //we have a proper string to return
            return locbleMessage;
        usingLocble = false;
        return defbultMessage;        
    }

    /**
     * @return the vblue of new version we parsed out of XML. Can return null.
     */ 
    public String getVersion() {
        return newVersion;
    }
    
    public long getTimestbmp() {
        return timestbmp;
    }

    /**
     * @return true if the messbge was picked up as per the locale, else false
     */
    public boolebn usesLocale() {
        return usingLocble;
    }
    
    /**
     * @return the messbge to show the user.
     */
    public String getMessbge() {
        return updbteMessage;
    }
}
