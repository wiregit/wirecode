package com.limegroup.gnutella.xml;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.List;
import com.limegroup.gnutella.util.NameValue;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Element;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * @author  Sumeet Thadani
 * @version
 * A LimeXMLDocument is basically a hashmap that maps a
 * Names of fields to the values as per a XML document.
 */
public class LimeXMLDocument{

    //TODO2: Need to build in the ability to work with multiple instances
    //of some fields. 
    
    protected Map fieldToValue;
    protected String schemaUri;
    
    //constructor
    public LimeXMLDocument(String XMLString) {
        DOMParser parser = new DOMParser();
        //TODO2: make sure that the schema actually validates documents
        //documentBuilderFactory.setValidating(true);
        //documentBuilderFactory.setNamespaceAware(true);
        InputSource doc = new InputSource(new StringReader(XMLString));
        Document document = null;
        try{
            parser.parse(doc);
        }catch(SAXException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        document = parser.getDocument();
        createMap(document);
        makeSchemaURI(document);
    }
    
    private void makeSchemaURI(Document doc){
        List attributes = DOMUtils.getAttributes(doc.getAttributes());
        int size = attributes.size();
        for(int i=0; i< size; i++){
            Node att = (Node)attributes.get(i);
            String attName = att.getNodeName();
            String lowerAttName = attName.toLowerCase();
            if (lowerAttName.indexOf("schemalocation") >= 0){
                schemaUri = att.getNodeValue();
                break;
            }
        }
    }

    /**
     * Returns the unique identifier which identifies the schema this XML
     * document conforms to
     * @return the unique identifier which identifies the schema this XML
     * document conforms to
     */
    public String getSchemaURI(){
        return schemaUri;
    }
    
    
    public LimeXMLDocument(File f) {
        DOMParser parser = new DOMParser();
        //TODO2: make sure that the schema actually validates documents
        //documentBuilderFactory.setValidating(true);
        //documentBuilderFactory.setNamespaceAware(true);
        InputSource doc = null;
        try{
            doc = new InputSource(new FileInputStream(f));
        }catch (FileNotFoundException ee){
            ee.printStackTrace();
        }
        Document document = null;
        try{
            parser.parse(doc);
        }catch (SAXException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        document=parser.getDocument();
        createMap(document);
    }
    
    private void createMap(Document doc) {
        fieldToValue = new HashMap();
        Element docElement = doc.getDocumentElement();
        doAllChildren(docElement,"");
    }

    private void doAllChildren (Node currNode, String parentName){
        if (!currNode.hasChildNodes()){ //base case
            doEntry(currNode, parentName);
            return;
        }
        String currString = doEntry(currNode,parentName);
        List children = DOMUtils.getElements(currNode.getChildNodes());
        int size = children.size();
        for(int i=0; i< size; i++){            
            Node child = (Node)children.get(i);
            doAllChildren(child,currString);
        }
    }

    private String doEntry(Node currNode, String parentName){
        String currTag;
        if(!parentName.equals(""))
            currTag =parentName+XMLStringUtils.DELIMITER+currNode.getNodeName();
        else
            currTag = currNode.getNodeName();
            
        if (currNode.getNodeType() == Node.CDATA_SECTION_NODE)
            System.out.println("this node has type  "+ currNode.getNodeType());

        Element currElement = (Element)currNode;
        String nodeValue = DOMUtils.getText(currElement.getChildNodes());
        nodeValue = nodeValue.trim();
        if (nodeValue != null && !nodeValue.equals(""))
            fieldToValue.put(currTag, nodeValue);
        //We only want 
        //add the attributes
        List attributes = DOMUtils.getAttributes(currNode.getAttributes());
        int size = attributes.size();
        for(int i=0; i< size; i++){
            Node att = (Node)attributes.get(i);
            String attName = att.getNodeName();
            String attString = 
            currTag + XMLStringUtils.DELIMITER+attName+XMLStringUtils.DELIMITER;
            String attValue = att.getNodeValue();
            fieldToValue.put(attString,attValue);
        }
        return currTag;
    }
    /**
     * Returns a List <NameValue>, where each name-value corresponds to a
     * Canonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     * <p>
     * Canonicalization:
     * <p>
     * So as to preserve the structure, Structure.Field will be represented as
     * Structure__Field (Double Underscore is being used as a delimiter to
     * represent the structure).
     *<p>
     * In case of multiple structured values with same name,
     * as might occur while using + or * in the regular expressions in schema,
     * those should be represented as using the array index using the __
     * notation (withouth the square brackets)
     * for e.g. myarray[0].name ==> myarray__0__name
     *
     * attribute names for an element in the XML schema should be postfixed 
     * with __ (double underscore).
     * So element.attribute ==> element__attribute__
     *
     * @return a List <NameValue>, where each name-value corresponds to a
     * canonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     */
    public List getNameValueList() {
        int size = fieldToValue.size();
        Iterator keys = fieldToValue.keySet().iterator();
        List retList = new ArrayList();
        for(int i=0; i< size; i++){
            String name = (String)keys.next();
            String value  = (String)fieldToValue.get(name);
            NameValue namVal = new NameValue(name,value);
            retList.add(namVal);
        }
        return retList;
    }

    //Unit Tester
    /*
      public static void main(String args[]){
      //File f = new File("C:/down/xerces-1_3_1/data","personal-schema.xml");
      /*
        Runtime rt = Runtime.getRuntime();
        long mem = rt.totalMemory()- rt.freeMemory();
        System.out.println("Sumeet : Used memory is "+mem);
        File f = new File("C:/home/etc/xml","all-books-pub.xml");
        LimeXMLDocument l = new LimeXMLDocument(f);
        List list = l.getNameValueList();
        int size = list.size();
        for (int i =0; i< size; i++){
        NameValue a = (NameValue)list.get(i);
        String name = a.getName();
        String value = (String)a.getValue();
        System.out.println("Sumeet : name "+name);
        System.out.println("Sumeet : value "+value);
        }
        }
    */
}


