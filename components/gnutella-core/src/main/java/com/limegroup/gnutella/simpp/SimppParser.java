package com.limegroup.gnutella.simpp;

import org.apache.xerces.parsers.*;
import org.xml.sax.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.*;
import org.w3c.dom.*;
import java.io.*;

public class SimppParser {

    private static DOMParser parser = new DOMParser();
    
    private static final String VERSION = "version";
    
    private static final String PROPS = "props";

    private int _version;
    private String _propsData;    

    //Format of dataBytes:
    //<xml for version related info with one tag containing all the props data>
    //TODO1: Change the way this is parsed as per the format described above. 
    public SimppParser(byte[] dataBytes) throws SAXException, IOException {
        String tmp = null;
        try {
            tmp = new String(dataBytes, "UTF-8");
        } catch (UnsupportedEncodingException uex) {
            ErrorService.error(uex);
        }
        parseInfo(tmp);
    }
    
    public int getVersion() {
        return _version;
    }

    public String getPropsData() {
        return _propsData;
    }

    ///////////////////////////private helpers////////////////////////

    private void parseInfo(String xmlStr) throws SAXException, IOException {
        if(xmlStr == null || xmlStr.equals(""))
            throw new SAXException("null xml for version info");
        InputSource inputSource = new InputSource(new StringReader(xmlStr));
        Document d = null;
        synchronized(this.parser) {
            parser.parse(inputSource);
            d = parser.getDocument();
        }
        if(d == null)
            throw new SAXException("parsed documemt is null");
        Element docElement = d.getDocumentElement();
        NodeList children = docElement.getChildNodes();
        int len = children.getLength();
        for(int i= 0; i< len; i++) {
            Node node = children.item(i);
            String nodeName = node.getNodeName().toLowerCase().trim();
            String value = LimeXMLUtils.getText(node.getChildNodes());
            if(nodeName.equals(VERSION)) {
                String ver = value;
                try {
                    _version = Integer.parseInt(ver);
                } catch(NumberFormatException nfx) {
                    _version = -1;
                }
            }
            else if(nodeName.equals(PROPS)) {
                _propsData = value;
            }
        }//end of for -- done all child nodes
    }
    
}
