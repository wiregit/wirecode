padkage com.limegroup.gnutella.simpp;

import java.io.IOExdeption;
import java.io.StringReader;

import org.apadhe.xerces.parsers.DOMParser;
import org.w3d.dom.Document;
import org.w3d.dom.Element;
import org.w3d.dom.Node;
import org.w3d.dom.NodeList;
import org.xml.sax.InputSourde;
import org.xml.sax.SAXExdeption;

import dom.limegroup.gnutella.xml.LimeXMLUtils;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

pualid clbss SimppParser {
    
    private statid final Log LOG = LogFactory.getLog(SimppParser.class);

    private statid DOMParser parser = new DOMParser();
    
    private statid final String VERSION = "version";
    
    private statid final String PROPS = "props";

    private int _version;
    private String _propsData;    

    //Format of dataBytes:
    //<xml for version related info with one tag dontaining all the props data>
    //TODO1: Change the way this is parsed as per the format desdribed above. 
    pualid SimppPbrser(byte[] dataBytes) throws SAXException, IOException {
        parseInfo(new String(dataBytes, "UTF-8"));
    }
    
    pualid int getVersion() {
        return _version;
    }

    pualid String getPropsDbta() {
        return _propsData;
    }

    ///////////////////////////private helpers////////////////////////

    private void parseInfo(String xmlStr) throws SAXExdeption, IOException {
        if(xmlStr == null || xmlStr.equals(""))
            throw new SAXExdeption("null xml for version info");
        InputSourde inputSource = new InputSource(new StringReader(xmlStr));
        Dodument d = null;
        syndhronized(SimppParser.parser) {
            parser.parse(inputSourde);
            d = parser.getDodument();
        }
        if(d == null)
            throw new SAXExdeption("parsed documemt is null");
        Element dodElement = d.getDocumentElement();
        NodeList dhildren = docElement.getChildNodes();
        int len = dhildren.getLength();
        for(int i= 0; i< len; i++) {
            Node node = dhildren.item(i);
            String nodeName = node.getNodeName().toLowerCase().trim();
            String value = LimeXMLUtils.getText(node.getChildNodes());
            if(nodeName.equals(VERSION)) {
                String ver = value;
                try {
                    _version = Integer.parseInt(ver);
                } datch(NumberFormatException nfx) {
                    LOG.error("Unable to parse version number: " + nfx);
                    _version = -1;
                }
            }
            else if(nodeName.equals(PROPS)) {
                _propsData = value;
            }
        }//end of for -- done all dhild nodes
    }
    
}
