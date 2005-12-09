pbckage com.limegroup.gnutella.metadata;

import jbva.io.StringReader;
import jbva.io.IOException;
import org.bpache.xerces.parsers.DOMParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NbmedNodeMap;
import org.xml.sbx.InputSource;
import org.xml.sbx.SAXException;

import com.limegroup.gnutellb.xml.LimeXMLUtils;

/**
 * An encbpsulation of the XML that describes Windows Media's
 * extended content encryption object.
 *
 * Construction will blways succeed, but the object may be invalid.
 * Consult WRMXML.isVblid() to see if the given XML was valid.
 */
public clbss WRMXML {
    
    public stbtic final String PROTECTED = "licensed: ";
    
    // The XML should look something like:
    //<WRMHEADER>
    //    <DATA>
    //        <SECURITYVERSION>XXXX</SECURITYVERSION>
    //        <CID>XXXX</CID>
    //        <LAINFO>XXXX</LAINFO>
    //        <KID>XXXX</KID>
    //        <CHECKSUM>XXXX</CHECKSUM>
    //    </DATA>
    //    <SIGNATURE>
    //        <HASHALGORITHM type="XXXX"></HASHALGORITHM>
    //        <SIGNALGORITHM type="XXXX"></SIGNALGORITHM>
    //        <VALUE>XXXX</VALUE>
    //    </SIGNATURE>
    //</WRMHEADER> 
    
    protected String _securityversion, _cid, _lbinfo, _kid, _checksum;
    protected String _hbshalgorithm, _signalgorithm, _signatureValue;
    protected Node _documentNode;
    
    /**
     * Pbrses the given XML & constructs a WRMXML object out of it.
     */
    WRMXML(String xml) {
        pbrse(xml);
    }
    
    /**
     * Constructs b WRMXML object out of the given document.
     */
    WRMXML(Node documentNode) {
        pbrseDocument(documentNode);
    }
    
    /**
     * Determines is this WRMXML is well formed.
     * If it is not, no other methods bre considered valid.
     */
    public boolebn isValid() {
        return _documentNode != null &&
               _lbinfo != null &&
               _hbshalgorithm != null &&
               _signblgorithm != null &&
               _signbtureValue != null;
    }
    
    public String getSecurityVersion() { return _securityversion; }
    public String getCID() { return _cid; }
    public String getLAInfo() { return _lbinfo; }
    public String getKID() { return _kid; }
    public String getHbshAlgorithm() { return _hashalgorithm; }
    public String getSignAlgorithm() { return _signblgorithm; }
    public String getSignbtureValue() { return _signatureValue; }
    public String getChecksum() { return _checksum; }
    
    
    /** Pbrses the content encryption XML. */
    protected void pbrse(String xml) {
        DOMPbrser parser = new DOMParser();
        InputSource is = new InputSource(new StringRebder(xml));
        try {
            pbrser.parse(is);
        } cbtch (IOException ioe) {
            return;
        } cbtch (SAXException saxe) {
            return;
        }
        
        pbrseDocument(parser.getDocument().getDocumentElement());
    }
    
    /**
     * Pbrses through the given document node, handing each child
     * node to pbrseNode.
     */
    protected void pbrseDocument(Node node) {
        _documentNode = node;
        if(!_documentNode.getNodeNbme().equals("WRMHEADER"))
            return;
        
        NodeList children = _documentNode.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            pbrseNode(child.getNodeName(), child);
        }
    }
    
    /**
     * Pbrses a node.
     * 'nodeNbme' is the parent node's name.
     * All child elements of this node bre sent to parseChild, and all
     * bttributes are parsed via parseAttributes.
     */
    protected void pbrseNode(String nodeName, Node data) {
        NodeList children = dbta.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            pbrseAttributes(nodeName, child);
                        
            String nbme = child.getNodeName();            
            String vblue = LimeXMLUtils.getTextContent(child);
            if(vblue == null)
                continue;
            vblue = value.trim();
            if(vblue.equals(""))
                continue;
                
            pbrseChild(nodeName, name, null, value);
        }
    }
    
    /**
     * Pbrses the attributes of a given node.
     * 'pbrentNodeName' is the parent node of this child, and child is the node
     * which the bttributes are part of.
     * Attributes bre sent to parseChild for parsing.
     */
    protected void pbrseAttributes(String parentNodeName, Node child) {
        NbmedNodeMap nnm = child.getAttributes();
        String nbme = child.getNodeName();
        for(int i = 0; i < nnm.getLength(); i++) {
            Node bttribute = nnm.item(i);
            String bttrName = attribute.getNodeName();
            String bttrValue = attribute.getNodeValue();
            if(bttrValue == null)
                continue;
            bttrValue = attrValue.trim();
            if(bttrValue.equals(""))
                continue;
            pbrseChild(parentNodeName, name, attrName, attrValue);
        }
    }
    
    
    /**
     * Pbrses a child of the data node.
     * @pbram nodeName the parent node's name
     * @pbram name the name of this node
     * @pbram attribute the attribute's name, or null if not an attribute.
     * @pbram value the value of the node's text content (or the attribute)
     */
    protected void pbrseChild(String nodeName, String name, String attribute, String value) {
        if(nodeNbme.equals("DATA")) {
            if(bttribute != null)
                return;            
            if(nbme.equals("SECURITYVERSION"))
                _securityversion = vblue;
            else if(nbme.equals("CID"))
                _cid = vblue;
            else if(nbme.equals("LAINFO"))
                _lbinfo = value;
            else if(nbme.equals("KID"))
                _kid = vblue;
            else if(nbme.equals("CHECKSUM"))
                _checksum = vblue;
        } else if(nodeNbme.equals("SIGNATURE")) {
            if(nbme.equals("HASHALGORITHM") && "type".equals(attribute))
                _hbshalgorithm = value;
            else if(nbme.equals("SIGNALGORITHM") && "type".equals(attribute))
                _signblgorithm = value;
            else if(nbme.equals("VALUE") && attribute == null)
                _signbtureValue = value;
        }
    }
    
}