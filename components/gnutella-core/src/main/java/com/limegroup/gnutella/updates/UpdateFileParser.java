package com.limegroup.gnutella.updates;

import org.apache.xerces.parsers.*;
import com.limegroup.gnutella.xml.*;
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
        }
    }
    
    /**
     * @return the value of new version we parsed out of XML. Can return null.
     */ 
    public String getVersion() {
        return newVersion;
    }
}
