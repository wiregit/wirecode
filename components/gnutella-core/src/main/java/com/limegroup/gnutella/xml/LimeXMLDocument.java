/*
 * XMLDocument.java
 *
 * Created on April 16, 2001, 12:09 PM
 */

package com.limegroup.gnutella.xml;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import com.limegroup.gnutella.util.NameValue;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * @author  Sumeet Thadani
 * @version
 * A LimeXMLDocument is basically a hashmap that maps a
 * Names of fields to the values as per a XML document.
 */
public class LimeXMLDocument{
    
    private Map fieldToValue;
    
    //constructor
    public LimeXMLDocument(String XMLString){
        DocumentBuilderFactory documentBuilderFactory = 
        DocumentBuilderFactory.newInstance();
        
        documentBuilderFactory.setValidating(true);
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder =null;
        try{
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        }catch(ParserConfigurationException e){
            e.printStackTrace();
        }
        InputSource doc = new InputSource(new StringReader(XMLString));
        Document document = null;
        try{
            document = documentBuilder.parse(doc);
        }catch(SAXException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    
    //
    public LimeXMLDocument(File f){
        String XMLString="";
        String buffer="";
        String xmlStruct="";
        InputSource schema;
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            while(buffer!=null){
                buffer=br.readLine();
                if (buffer!=null){
                    buffer=buffer.trim();
                    xmlStruct = xmlStruct+buffer;
                }
            }
            xmlStruct = xmlStruct.trim();
        }catch(IOException e){
            e.printStackTrace();
        }
        //this(xmlStruct);
        DocumentBuilderFactory documentBuilderFactory = 
        DocumentBuilderFactory.newInstance();
        
        documentBuilderFactory.setValidating(true);
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder=null;
        try{
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        }catch(ParserConfigurationException e){
            e.printStackTrace();
        }
        InputSource doc = new InputSource(new StringReader(XMLString));
        Document document = null;
        try{
            document = documentBuilder.parse(doc);
        }catch (SAXException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        
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
        //TODO
        //return an instance of ArrayList <NameValue>
        return null;
    }

    //Unit Tester
    public static void main(String args[]){
        File f = new File("C:/home/etc/xml","all-books-gen.xml");
        LimeXMLDocument l = new LimeXMLDocument(f);
    }
}
