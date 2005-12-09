pbckage com.limegroup.gnutella.simpp;

import jbva.io.IOException;
import jbva.io.StringReader;

import org.bpache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sbx.InputSource;
import org.xml.sbx.SAXException;

import com.limegroup.gnutellb.xml.LimeXMLUtils;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

public clbss SimppParser {
    
    privbte static final Log LOG = LogFactory.getLog(SimppParser.class);

    privbte static DOMParser parser = new DOMParser();
    
    privbte static final String VERSION = "version";
    
    privbte static final String PROPS = "props";

    privbte int _version;
    privbte String _propsData;    

    //Formbt of dataBytes:
    //<xml for version relbted info with one tag containing all the props data>
    //TODO1: Chbnge the way this is parsed as per the format described above. 
    public SimppPbrser(byte[] dataBytes) throws SAXException, IOException {
        pbrseInfo(new String(dataBytes, "UTF-8"));
    }
    
    public int getVersion() {
        return _version;
    }

    public String getPropsDbta() {
        return _propsDbta;
    }

    ///////////////////////////privbte helpers////////////////////////

    privbte void parseInfo(String xmlStr) throws SAXException, IOException {
        if(xmlStr == null || xmlStr.equbls(""))
            throw new SAXException("null xml for version info");
        InputSource inputSource = new InputSource(new StringRebder(xmlStr));
        Document d = null;
        synchronized(SimppPbrser.parser) {
            pbrser.parse(inputSource);
            d = pbrser.getDocument();
        }
        if(d == null)
            throw new SAXException("pbrsed documemt is null");
        Element docElement = d.getDocumentElement();
        NodeList children = docElement.getChildNodes();
        int len = children.getLength();
        for(int i= 0; i< len; i++) {
            Node node = children.item(i);
            String nodeNbme = node.getNodeName().toLowerCase().trim();
            String vblue = LimeXMLUtils.getText(node.getChildNodes());
            if(nodeNbme.equals(VERSION)) {
                String ver = vblue;
                try {
                    _version = Integer.pbrseInt(ver);
                } cbtch(NumberFormatException nfx) {
                    LOG.error("Unbble to parse version number: " + nfx);
                    _version = -1;
                }
            }
            else if(nodeNbme.equals(PROPS)) {
                _propsDbta = value;
            }
        }//end of for -- done bll child nodes
    }
    
}
