/*
 * LimeXMLSchema.java
 *
 * Created on April 12, 2001, 4:03 PM
 */

package com.limegroup.gnutella.xml;
import java.util.*;
import java.io.*;

import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;

/**
 * Stores a XML schema, and provides access to various components
 * of schema
 * @author asingla
 * @version
 */
public class LimeXMLSchema 
{
    /**
     * List<String> of elements (in canonicalized form to preserve the structural
     * information)
     */
    List /* of String */ canonicalizedElements = new LinkedList();

    /** 
     * Creates new LimeXMLSchema 
     * @param schemaFile The filefrom where to read the schema definition
     * @exception IOException If the specified schemaFile doesnt exist, or isnt
     * a valid schema file
     */
    public LimeXMLSchema(File schemaFile) throws IOException
    {
        this(LimeXMLUtils.getInputSource(schemaFile));
    }
    
    //TODO anu
    //this constructor may not be needed
    public LimeXMLSchema()
    {
    }
    
    /** 
     * Creates new LimeXMLSchema 
     * @param inputSource The source representing the XML schema definition
     * to be parsed
     * @exception IOException If the specified schemaFile doesnt exist, or isnt
     * a valid schema file
     */
    public LimeXMLSchema(InputSource inputSource) throws IOException
    {
        //initialize schema
        initializeSchema(inputSource);
    }
    
    /**
     * Initilizes the schema after parsing it from the input source
     * @param schemaInputSource The source representing the XML schema definition
     * to be parsed
     */
    private void initializeSchema(InputSource schemaInputSource)
    {
        //get an instance of DocumentBuilderFactory
        DocumentBuilderFactory documentBuilderFactory =
            DocumentBuilderFactory.newInstance();
        //set validating, and namespace awareness
        //documentBuilderFactory.setValidating(true);
        //documentBuilderFactory.setNamespaceAware(true);
            
        //get the document builder from factory    
        DocumentBuilder documentBuilder=null;
        try
        {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        }
        catch(ParserConfigurationException e)
        {
            e.printStackTrace();
            return;
        }
        // Set an entity resolver to resolve the schema
        documentBuilder.setEntityResolver(new Resolver(schemaInputSource));

        // Parse the schema and create a  document
        Document document=null;  
        try
        {
            document = documentBuilder.parse(schemaInputSource);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return;
        }
        
        //print some of the elements
        Element root = document.getDocumentElement();
        printNode(root);
        
        //get the fields
        LimeXMLSchemaFieldExtractor.getFields(document);
        
//        String rootName = root.getTagName();
//        System.out.println("root = " + rootName);
//        NodeList children = root.getChildNodes();
//        int numChildren = children.getLength();
//        System.out.println("no of children "+numChildren);
//        for(int i=0;i<numChildren; i++)
//        {
//            Node n = children.item(i);
//            String childName = n.getNodeName();
//            System.out.println("child Name is "+childName);
//        }
        
        
        
        //TODO anu
            
    }
    
    private void printNode(Node n)
    {
        System.out.print("node = " + n.getNodeName() + " ");
        
        //get attributes
        NamedNodeMap  nnm = n.getAttributes();
        Node name = nnm.getNamedItem("name");
        if(name != null)
            System.out.print(name + "" );
        System.out.println("");
        NodeList children = n.getChildNodes();
        int numChildren = children.getLength();
        for(int i=0;i<numChildren; i++)
        {
            Node child = children.item(i);
            printNode(child);
	    }
    }
    
    /**
     * Returns the unique identifier which identifies this particular schema
     * @return the unique identifier which identifies this particular schema
     */
    public String getSchemaIdentifier()
    {
        //TODO anu remove
        return "schemas/gen_books.xsd";
        //end remove
        
        //TODO
        //return null;
        
    }
    
    /**
     * Returns all the field names (placeholders) in this schema.
     * The field names are canonicalized as mentioned below:
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
     * @return all the field names (placeholders) in this schema.
     */
    public String[] getCanonicalizedFieldNames()
    {
        //TODO anu remove
        String[] result =
            {
                "gen_book_info__Title__",
                "gen_book_info__Author__",
                "gen_book_info__NumChapters__",
                "gen_book_info__Genre__"
            };
            return result;
        
        //end remove
        
        //TODO
        //return null;
    }
    
    public static void Test()
    {
        try
        {
            LimeXMLSchema schema = new LimeXMLSchema(new File(
                LimeXMLProperties.instance().getXMLSchemaDir() 
                + File.separator
                + "gen_books.xsd"));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args)
    {
        Test();
    }
    
    
    private static final class Resolver implements EntityResolver
    {
        private InputSource schema;
        
        public Resolver(InputSource s)
        {
            schema = s;
        }
        
        public InputSource resolveEntity(String publicId, String systemId)
        {
            String Id = systemId+publicId;
            String schemaId = schema.getSystemId()+schema.getPublicId();
            if (Id.equals(schemaId))
                return schema;
            else
                return null;
        }
    }//end of private innner class
    
    
}
