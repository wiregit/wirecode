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
import com.limegroup.gnutella.util.NameValue;

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
    
    /**
     * Reads all the bytes from the passed input stream till end of stream
     * reached.
     * @param in The input stream to read from
     * @return array of bytes read
     * @exception IOException If any I/O exception occurs while reading data
     */
    public static byte[] readFully(InputStream in) throws IOException
    {
        //create a new byte array stream to store the read data
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        
        //read the bytes till EOF
        byte[] buffer = new byte[1024];
        int bytesRead;
        while((bytesRead = in.read(buffer)) != -1)
        {
            //append the bytes read to the byteArray buffer
            byteArray.write(buffer,0,bytesRead);
        }
        
        //return the bytes read
        return byteArray.toByteArray();
    }
    
    /**
     * It converts the passed aggregate XML Document (where root node 
     * has multiple children of same type, to represent multiple results)
     * to individual documents (in each of which the root node 
     * will have only one child). So, in a way, the aggreate document 
     * containing multiple results will get converted to multiple individual
     * result documents
     * @param aggregateXMLDocument The Aggregate XML document that needs to 
     * be broken inti simpler documents
     * @return Array of individual documents after breaking the aggregate
     * document
     */
    public static LimeXMLDocument[] convertAggregateToParts(
        LimeXMLDocument aggregateXMLDocument)
    {
        return null;
    }
    
    /**
     * Compares the queryDoc with the replyDoc and finds out if the
     * replyDoc is a match for the queryDoc
     * @param queryDoc The query Document
     * @param replyDoc potential reply Document
     * @return true if the replyDoc is a match for the queryDoc, false
     * otherwise
     */
    public static boolean match(LimeXMLDocument replyDoc, 
        LimeXMLDocument queryDoc){
        if(replyDoc==null)
            return false;
        //First find the names of all the fields in the query
        List queryNameValues = queryDoc.getNameValueList();
        int size = queryNameValues.size();
        List fieldNames = new ArrayList(size);
        for(int i=0; i<size; i++){
            NameValue nameValue = (NameValue)queryNameValues.get(i);
            String fieldName = nameValue.getName();
            fieldNames.add(fieldName);
        }
        //compare these fields with the current reply document
        //List currDocNameValues = replyDoc.getNameValueList();
        int matchCount=0;//number of matches
        int nullCount=0;//num of fields which are in query but null in ReplyDoc
        for(int j=0; j< size; j++){
            String currFieldName = (String)fieldNames.get(j);
            String queryValue = queryDoc.getValue(currFieldName);
            String replyDocValue = replyDoc.getValue(currFieldName);
            if(replyDocValue == null)
                nullCount++;
            else if(replyDocValue.equalsIgnoreCase(queryValue))
                matchCount++;
        }
        //The metric of a correct match is that whatever fields are specified
        //in the query must have perfect match with the fields in the reply
        //unless the reply has a null for that feild, in which case we are OK 
        // with letting it slide. But if there is even one mismatch
        // we are going to return false.
        //We make an exception for queries of size i field. Here 
        // there must be a 100% match
        if(size > 1){
            if( ( (nullCount+matchCount)/size ) < 1)
                return false;
            return true;
        }
        else if (size == 1){
            if(matchCount/size <1)
                return false;
            return true;
        }
        //this should never happen - size >0
        return false;
    }
    
}
