package com.limegroup.gnutella.xml;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.Writer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;


/**
 * Utility methods for stripping irrelevant information out of DOM Documents
 *
 * @author Ron Vogl
 */
class DOMUtils{
    /**
     * Extracts only the Element nodes from a NodeList.  This is useful when
     * the DTD guarantees that the node list's parent contains only elements.
     * Unfortunately, the node list can contain comments and whitespace.
     */
    public static List getElements(NodeList nodeList) {
        List elements = new ArrayList(nodeList.getLength());
        for(int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE)
                elements.add(node);
        }
        return elements;
    }
    
    public static List getAttributes(NamedNodeMap nodeMap){
        List attributes = new ArrayList(nodeMap.getLength());
        for (int i = 0; i< nodeMap.getLength(); i++){
            Node node = nodeMap.item(i);
            attributes.add(node);
        }
        return attributes;
    }

    /**
     * Collapses a list of CDATASection, Text, and predefined EntityReference
     * nodes into a single string.  If the list contains other types of nodes,
     * those other nodes are ignored.
     */
    public static String getText(NodeList nodeList) {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            switch(node.getNodeType()) {
                case Node.CDATA_SECTION_NODE :
                case Node.TEXT_NODE :
                    buffer.append(node.getNodeValue());
                    break;
                case Node.ENTITY_REFERENCE_NODE :
                    if(node.getNodeName().equals("amp"))
                        buffer.append('&');
                    else if(node.getNodeName().equals("lt"))
                        buffer.append('<');
                    else if(node.getNodeName().equals("gt"))
                        buffer.append('>');
                    else if(node.getNodeName().equals("apos"))
                        buffer.append('\'');
                    else if(node.getNodeName().equals("quot"))
                        buffer.append('"');
                    // Any other entity references are ignored
                    break;
                default :
                    // All other nodes are ignored
             }
         }
         return buffer.toString();
    }

    /**
     * Writes <CODE>string</CODE> into writer, escaping &, ', ", <, and >
     * with the XML excape strings.
     */
    public static void writeEscapedString(Writer writer, String string)
        throws IOException {
        for(int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if(c == '<')
                writer.write("&lt;");
            else if(c == '>')
                writer.write("&gt;");
            else if(c == '&')
                writer.write("&amp;");
            else if(c == '\'')
                writer.write("&apos;");
            else if(c == '"')
                writer.write("&quot;");
            else
		writer.write(c);
        }
    }
}
