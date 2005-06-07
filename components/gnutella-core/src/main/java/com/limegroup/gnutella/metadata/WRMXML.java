package com.limegroup.gnutella.metadata;

import java.io.StringReader;
import java.io.IOException;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.DOMException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * An encapsulation of the XML that describes Windows Media's
 * extended content encryption object.
 *
 * Construction will always succeed, but the object may be invalid.
 * Consult WRMXML.isValid() to see if the given XML was valid.
 */
public class WRMXML {
    
    public static final String PROTECTED = "licensed: ";
    
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
    
    protected String _securityversion, _cid, _lainfo, _kid, _checksum;
    protected String _hashalgorithm, _signalgorithm, _signatureValue;
    protected Node _documentNode;
    
    /**
     * Parses the given XML & constructs a WRMXML object out of it.
     */
    WRMXML(String xml) {
        parse(xml);
    }
    
    /**
     * Constructs a WRMXML object out of the given document.
     */
    WRMXML(Node documentNode) {
        parseDocument(documentNode);
    }
    
    /**
     * Determines is this WRMXML is well formed.
     * If it is not, no other methods are considered valid.
     */
    public boolean isValid() {
        return _documentNode != null &&
               _lainfo != null &&
               _hashalgorithm != null &&
               _signalgorithm != null &&
               _signatureValue != null;
    }
    
    public String getSecurityVersion() { return _securityversion; }
    public String getCID() { return _cid; }
    public String getLAInfo() { return _lainfo; }
    public String getKID() { return _kid; }
    public String getHashAlgorithm() { return _hashalgorithm; }
    public String getSignAlgorithm() { return _signalgorithm; }
    public String getSignatureValue() { return _signatureValue; }
    public String getChecksum() { return _checksum; }
    
    
    /** Parses the content encryption XML. */
    protected void parse(String xml) {
        DOMParser parser = new DOMParser();
        InputSource is = new InputSource(new StringReader(xml));
        try {
            parser.parse(is);
        } catch (IOException ioe) {
            return;
        } catch (SAXException saxe) {
            return;
        }
        
        parseDocument(parser.getDocument().getDocumentElement());
    }
    
    /** Parses through the given document node. */
    protected void parseDocument(Node node) {
        _documentNode = node;
        if(!_documentNode.getNodeName().equals("WRMHEADER"))
            return;
        
        NodeList children = _documentNode.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = (Node)children.item(i);
            parseNode(child.getNodeName(), child);
        }
    }
    
    /**
     * Parses a node.
     */
    protected void parseNode(String nodeName, Node data) {
        NodeList children = data.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = (Node)children.item(i);
            parseAttributes(nodeName, child);
                        
            String name = child.getNodeName();            
            String value = LimeXMLUtils.getTextContent(child);
            if(value == null)
                continue;
            value = value.trim();
            if(value.equals(""))
                continue;
                
            parseChild(nodeName, name, null, value);
        }
    }
    
    /**
     * Parses the attributes of a given node.
     */
    protected void parseAttributes(String parentNodeName, Node child) {
        NamedNodeMap nnm = child.getAttributes();
        String name = child.getNodeName();
        for(int i = 0; i < nnm.getLength(); i++) {
            Node attribute = nnm.item(i);
            String attrName = attribute.getNodeName();
            String attrValue = attribute.getNodeValue();
            if(attrValue == null)
                continue;
            attrValue = attrValue.trim();
            if(attrValue.equals(""))
                continue;
            parseChild(parentNodeName, name, attrName, attrValue);
        }
    }
    
    
    /** Parses a child of the data node. */
    protected void parseChild(String nodeName, String name, String attribute, String value) {
        if(nodeName.equals("DATA")) {
            if(attribute != null)
                return;            
            if(name.equals("SECURITYVERSION"))
                _securityversion = value;
            else if(name.equals("CID"))
                _cid = value;
            else if(name.equals("LAINFO"))
                _lainfo = value;
            else if(name.equals("KID"))
                _kid = value;
            else if(name.equals("CHECKSUM"))
                _checksum = value;
        } else if(nodeName.equals("SIGNATURE")) {
            if(name.equals("HASHALGORITHM") && "type".equals(attribute))
                _hashalgorithm = value;
            else if(name.equals("SIGNALGORITHM") && "type".equals(attribute))
                _signalgorithm = value;
            else if(name.equals("VALUE") && attribute == null)
                _signatureValue = value;
        }
    }
    
}