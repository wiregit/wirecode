package com.limegroup.gnutella.simpp;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.limegroup.gnutella.xml.LimeXMLUtils;

public class SimppParser {
    
    private static final Log LOG = LogFactory.getLog(SimppParser.class);

    private static final String VERSION = "version";
    
    private static final String PROPS = "props";

    private int _version;
    private String _propsData;    

    //Format of dataBytes:
    //<xml for version related info with one tag containing all the props data>
    //TODO1: Change the way this is parsed as per the format described above. 
    public SimppParser(byte[] dataBytes) throws SAXException, IOException {
        parseInfo(new String(dataBytes, "UTF-8"));
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
        Document d = null;
        try {
        	DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        	InputSource inputSource = new InputSource(new StringReader(xmlStr));
            builder.setErrorHandler(new LogErrorHandler());
        	d = builder.parse(inputSource);
        } catch (ParserConfigurationException ouch) {
        	return;
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
                    LOG.error("Unable to parse version number: " + nfx);
                    _version = -1;
                }
            }
            else if(nodeName.equals(PROPS)) {
                _propsData = value;
            }
        }//end of for -- done all child nodes
    }
    
    private static final class LogErrorHandler implements ErrorHandler {
        public void error(SAXParseException exception) throws SAXException {
            LOG.error("Parse error", exception);
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            LOG.error("Parse fatal error", exception);
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            LOG.error("Parse warning", exception);
        }
    }    
    
}
