package com.limegroup.gnutella.simpp;

import org.apache.xerces.parsers.*;
import org.xml.sax.*;
import com.limegroup.gnutella.xml.*;
import org.w3c.dom.*;
import java.io.*;

public class SimppParser {

    private static DOMParser parser = new DOMParser();
    
    private static final String VERSION = "version";

    private int _version;
    private String _propsData;    

    //Format of dataBytes:
    //<xml for version related info>|<xml for props>
    public SimppParser(byte[] dataBytes) throws IOException {
        //TODO1: Is using embedded XML a better way to go?
        int sepIndex = SimppDataVerifier.findSeperator(dataBytes);
        byte[] versionBytes = new byte[sepIndex];
        System.arraycopy(dataBytes, 0, versionBytes, 0, sepIndex);
        String tmp = new String(versionBytes, "UTF-8");
        parseVersionInfo(tmp);
        byte[] propsBytes = new byte[dataBytes.length-1-sepIndex];
        System.arraycopy(dataBytes, sepIndex+1, propsBytes, 0, 
                                                   dataBytes.length-1-sepIndex);
        _propsData = new String(propsBytes, "UTF-8");
    }
    
    public int getVersion() {
        return _version;
    }

    public String getPropsData() {
        return _propsData;
    }

    ///////////////////////////private helpers////////////////////////

    private void parseVersionInfo(String xmlStr) {
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
            if(nodeName.equals(VERSION)) {
                String ver = LimeXMLUtils.getText(node.getChildNodes());
                try {
                    _version = Integer.parseInt(ver);
                } catch(NumberFormatException nfx) {
                    _version = -1;
                }
            }
        }
    }
    
}
