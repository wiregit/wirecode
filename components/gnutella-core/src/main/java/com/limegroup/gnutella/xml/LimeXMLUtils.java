/*
 * XMLUtils.java
 *
 * Created on April 30, 2001, 4:51 PM
 */

package com.limegroup.gnutella.xml;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import com.limegroup.gnutella.Response;

import org.xml.sax.InputSource;

/**
 * Contains utility methods
 * @author  asingla
 */
public class LimeXMLUtils
{
    /**
     * Returns an instance of InputSource after reading the file, and trimming
     * the extraneous white spaces.
     * @param file The file from where to read
     * @return The instance of InpiutSource created from the passed file
     * @exception IOException If file doesnt get opened or other I/O problems
     */
    public static InputSource getInputSource(File file) throws IOException
    {
        //open the file, read it, and derive the structure, store internally
        StringBuffer sb = new StringBuffer();
        InputSource inputSource;
        String line = "";
     
        //open the file
        BufferedReader br = new BufferedReader(new FileReader(file));
        while(line != null)
        {
            //read a line from file
            line = br.readLine();
            if(line != null)
            {
                //append the line (along with the newline that got removed)
                sb.append(line + "\n");
            }
        }
      
        //get & return the input source
        return new InputSource(new StringReader(sb.toString()));
    }
    
    /**
     * Returns an instance of org.w3c.dom.Document after parsing the
     * passed xml file
     * @param file The file from where to read
     * @return The instance of org.w3c.dom.Document after parsing the
     * passed xml file
     * @exception IOException If file doesnt get opened or other I/O problems
     * @exception ParserConfigurationException if problem in getting parser
     * @exception SAXException If any problem in parsing
     */
    public static Document getDocument(File file) throws IOException, 
        ParserConfigurationException, SAXException
    {
        //get an input source out of it for parsing
        InputSource inputSource = 
            LimeXMLUtils.getInputSource(file);

        //get a document builder
        DocumentBuilder documentBuilder = 
            DocumentBuilderFactory.newInstance().newDocumentBuilder();

        // Parse the xml file and create a  document
        Document document = documentBuilder.parse(inputSource);
        
        //return the document
        return document;
    }
    
    
    /**
     * Returns the value of the specified attribute
     * @param attributes attribute nodes in which to search for the 
     * specified attribute
     * @param soughtAttribute The attribute whose value is sought
     * @return the value of the specified attribute, or null if the specified
     * attribute doesnt exist in the passed set of attributes
     */
    public static String getAttributeValue(NamedNodeMap  attributes, String
        soughtAttribute)
    {
        //get the required attribute node
        Node requiredNode = attributes.getNamedItem(soughtAttribute);
        
        //if the attribute node is null, return null
        if(requiredNode == null)
            return null;
        
        //get the value of the required attribute, and return that
        return requiredNode.getNodeValue();
    }
    
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
    
    /**
     * Creates a Response instance from the passed xml string
     */
    public static Response createResponse(String xml)
    {
        //create a new response using default values and return it
        return new Response(
            LimeXMLProperties.DEFAULT_NONFILE_INDEX,
                xml.length(), "xml result", xml);
    }
    
}
