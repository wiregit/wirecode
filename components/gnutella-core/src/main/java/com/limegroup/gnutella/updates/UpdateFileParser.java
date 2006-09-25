package com.limegroup.gnutella.updates;

import java.io.IOException;
import java.io.StringReader;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.xml.LimeXMLUtils;

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

    private long timestamp;

    public UpdateFileParser(String xml) throws SAXException, IOException {
        if(xml==null || xml.equals(""))
            throw new SAXException("xml is null or empty string");
        timestamp = -1l;
        InputSource inputSource = new InputSource(new StringReader(xml));
        Document d = null;
        synchronized(parser) {
            parser.parse(inputSource);
            d = parser.getDocument();
        }
        if(d==null)//problems parsing?
            throw new SAXException("document is null");
        populateValues(d);
    }
    
    private void populateValues(Document doc) throws IOException {
        Element docElement = doc.getDocumentElement();
        //Note: We are assuming that the XML structure will have no attributes.
        //only child elements. We can make this assumption because we are the
        //XML is generated right here in house at LimeWire.
        NodeList children = docElement.getChildNodes();
        int len = children.getLength();
        for(int i=0; i<len; i++) { //parse the nodes.
            Node node = children.item(i);
            String name = node.getNodeName().toLowerCase().trim();
            if(name.equals("version")) 
                newVersion = LimeXMLUtils.getText(node.getChildNodes());
            else if(name.equals("message"))
                updateMessage = getLocaleSpecificMessage(node);
            else if(name.equals("timestamp")) {
                try {
                    timestamp = 
                    Long.parseLong(LimeXMLUtils.getText(node.getChildNodes()));
                } catch (NumberFormatException nfx) {
                    throw new IOException();
                }
            }
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
        String locale = ApplicationSettings.LANGUAGE.getValue().toLowerCase();
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
    
    public long getTimestamp() {
        return timestamp;
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
