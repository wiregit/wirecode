package com.limegroup.gnutella.updates;

import org.apache.xerces.parsers.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import java.io.*;

public class UpdateFileParser {
    
    //initilaize this once per class. 
    private static DOMParser parser = new DOMParser();
    
    /**
     * For the first release the only value we need is the new version.
     * As we add more data to the update file, we can have the structure be a 
     * hashmap, and add getter and setter methods.
     */
    private String newVersion=null;
    
    private String updateMessage=null;

    private boolean usingLocale = true;

    public UpdateFileParser(String xml) throws SAXException, IOException {
        if(xml==null || xml.equals(""))
            throw new SAXException(new Exception());
        InputSource inputSource = new InputSource(new StringReader(xml));
        Document d = null;
        synchronized(this.parser) {
            parser.parse(inputSource);
            d = parser.getDocument();
        }
        if(d==null)//problems parsing?
            throw new SAXException(new Exception());
        populateValues(d);
    }
    
    private void populateValues(Document doc) {
        Element docElement = doc.getDocumentElement();
        //Note: We are assuming that the XML structure will have no attributes.
        //only child elements. We can make this assumption because we are the
        //XML is generated right here in house at LimeWire.
        NodeList children = docElement.getChildNodes();
        int len = children.getLength();
        for(int i=0; i<len; i++) { //this versions only looks for version
            Node node = children.item(i);
            String name = node.getNodeName().toLowerCase().trim();
            if(name.equals("version")) 
                newVersion = LimeXMLUtils.getText(node.getChildNodes());
            else if(name.equals("message"))
                updateMessage = getLocaleSpecificMessage(node);
        }
    }
    
    /**
     * Looks at the child nodes of node, and tries to find the value of the
     * message based on the language specified in limewire.props
     * If there is no string for the message in that langauge, returns the
     * string in English.
     * <p>
     * If we were not able to find the string as per the language preference,
     * we set the value of usingLocale to false. 
     */
    private String getLocaleSpecificMessage(Node node) {
        String locale = SettingsManager.instance().getLanguage().toLowerCase();
        String defaultMessage=null;
        String localeMessage=null;
        NodeList children = node.getChildNodes();
        int len = children.getLength();
        for(int i=0 ; i<len ; i++) {
            Node n = children.item(i);
            String name = n.getNodeName().toLowerCase().trim();
            if(name.equals("en"))
                defaultMessage = LimeXMLUtils.getText(n.getChildNodes());
            else if(name.equals(locale)) 
                localeMessage = LimeXMLUtils.getText(n.getChildNodes());
        }
        Assert.that(defaultMessage!=null,"bad xml file signed by LimeWire");
        //check if we should send back en or locale
        if(locale.equals("en"))
            return defaultMessage;
        if(localeMessage!=null)  //we have a proper string to return
            return localeMessage;
        usingLocale = false;
        return defaultMessage;        
    }

    /**
     * @return the value of new version we parsed out of XML. Can return null.
     */ 
    public String getVersion() {
        return newVersion;
    }
    
    /**
     * @return true if the message was picked up as per the locale, else false
     */
    public boolean usesLocale() {
        return usingLocale;
    }
    
    /**
     * @return the message to show the user.
     */
    public String getMessage() {
        return updateMessage;
    }
}
