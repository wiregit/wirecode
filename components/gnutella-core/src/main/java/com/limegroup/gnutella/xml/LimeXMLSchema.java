/*
 * LimeXMLSchema.java
 *
 * Created on April 12, 2001, 4:03 PM
 */

package com.limegroup.gnutella.xml;
import com.sun.java.util.collections.*;
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
import org.xml.sax.*;

/**
 * Stores a XML schema, and provides access to various components
 * of schema
 * @author asingla
 */
public class LimeXMLSchema 
{
    /**
     * List<String> of fields (in canonicalized form to preserve the structural
     * information)
     */
    private List /* of SchemaFieldInfo */ _canonicalizedFields 
        = new ArrayList();
    
    /**
     * The URI for this schema
     */
    private String _schemaURI = null;

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
        throws IOException
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
            //e.printStackTrace();
            throw new IOException("" + e);
        }
        // Set an entity resolver to resolve the schema
        documentBuilder.setEntityResolver(new Resolver(schemaInputSource));

        // Parse the schema and create a  document
        Document document=null;  
        try
        {
            document = documentBuilder.parse(schemaInputSource);
        }
        catch(SAXException e) {
            throw new IOException("" + e);
        }
        
        //print some of the elements
//        Element root = document.getDocumentElement();
//        printNode(root);
        
        //get the fields
        _canonicalizedFields = 
            (new LimeXMLSchemaFieldExtractor()).getFields(document);
        
        //also get the schema URI
        _schemaURI = retrieveSchemaURI(document);
        
        //return
        return;
    }
    
    /**
     * Returns the URI of the schema represented in the passed document
     * @param document The document representing the XML Schema whose URI is to
     * be retrieved
     * @return The schema URI
     * @requires The document be a parsed form of valid xml schema
     */
    private static String retrieveSchemaURI(Document document)
    {
        //get the root element which should be "xsd:schema" element (provided
        //document represents valid schema)
        Element root = document.getDocumentElement();
        //get attributes
        NamedNodeMap  nnm = root.getAttributes();
        //get the targetNameSpaceAttribute
        Node targetNameSpaceAttribute = nnm.getNamedItem("targetNamespace");

        if(targetNameSpaceAttribute != null)
        {
            //return the specified target name space as schema URI
            return targetNameSpaceAttribute.getNodeValue();
        }
        else
        {
            //return an empty string otherwise
            return "";
        }
    }
    
    /**
     * Prints the node, as well as its children (by invoking the method
     * recursively on the children)
     * @param n The node which has to be printed (along with children)
     */
    private void printNode(Node n)
    {
        //get attributes
        if(n.getNodeType() == Node.ELEMENT_NODE)
        {
            System.out.print("node = " + n.getNodeName() + " ");
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
        
    }
    
    /**
     * Returns the unique identifier which identifies this particular schema
     * @return the unique identifier which identifies this particular schema
     */
    public String getSchemaURI()
    {
        return _schemaURI;
    }
    
    /**
     * Returns all the fields(placeholders) in this schema.
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
     * @return unmodifiable list (of SchemaFieldInfo) of all the fields 
     * in this schema.
     */
    public List getCanonicalizedFields()
    {
        return Collections.unmodifiableList(_canonicalizedFields);
    }
    
    
    /**
     * Returns only those fields which are of enumeration type
     */
    public List getEnumerationFields()
    {
        //create a new list
        List enumerationFields = new LinkedList();
        
        //iterate over canonicalized fields, and add only those which are 
        //of enumerative type
        Iterator iterator = _canonicalizedFields.iterator();
        while(iterator.hasNext())
        {
            //get next schema field 
            SchemaFieldInfo schemaFieldInfo = (SchemaFieldInfo)iterator.next();
            //if enumerative type, add to the list of enumeration fields
            if(schemaFieldInfo.getEnumerationList() != null)
                enumerationFields.add(schemaFieldInfo);
        }
        
        //return the list of enumeration fields
        return enumerationFields;
    }
    
    /**
     * Returns Mapping from FieldName => (EnumerativeValue => Mapped Value)
     * (String ==> Map (String => String))
     */
    public Map getDefaultFieldEnumerativeValueMap()
    {
        //create a new Map
        Map fieldEnumerativeValueMap = new HashMap();
        
        //iterate over canonicalized fields, and add mappings for 
        //only those which are of enumerative type
        Iterator iterator = _canonicalizedFields.iterator();
        while(iterator.hasNext())
        {
            //get next schema field 
            SchemaFieldInfo schemaFieldInfo = (SchemaFieldInfo)iterator.next();
            //get the enumerativeValueMap
            Map enumerativeValueMap = 
                schemaFieldInfo.getDefaultEnumerativeValueMap();
            //if the map is not null (i.e. enumerations exist for this field,
            //add the mapping
            if(enumerativeValueMap != null)
            {
                fieldEnumerativeValueMap.put(
                    schemaFieldInfo.getCanonicalizedFieldName(), 
                    enumerativeValueMap);
            }
        }
        
        //return the mappings
        return fieldEnumerativeValueMap;
    }
    
    
    /**
     * Returns all the fields(placeholders) names in this schema.
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
     * @return list (Strings) of all the field names in this schema.
     */
    public String[] getCanonicalizedFieldNames()
    {
        //get the fields
        List canonicalizedFields = this.getCanonicalizedFields();
        
        //extract field names out of those
        String[] fieldNames = new String[canonicalizedFields.size()];
        Iterator iterator = canonicalizedFields.iterator();
        for(int i=0; i < fieldNames.length; i++)
        {
            fieldNames[i] = ((SchemaFieldInfo)iterator.next())
                .getCanonicalizedFieldName();
        }
        
        //return the field names
        return fieldNames;
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
            return schema;
            
            //String Id = systemId+publicId;
            //String schemaId = schema.getSystemId()+schema.getPublicId();
            //if (Id.equals(schemaId))
            //    return schema;
            //else
            //    return null;
        }
    }//end of private innner class

    /**
     * Utility method to be used in the gui to display schemas
     */
    public static String getDisplayString(String schemaURI)
    {
        int start = schemaURI.lastIndexOf("/");
        //TODO3: Are we sure that / is the correct delimiter???
        int end = schemaURI.lastIndexOf(".");
        String schemaStr;
        if(start == -1 || end == -1)
            schemaStr = schemaURI;
        else
            schemaStr= schemaURI.substring(start+1,end);
        return schemaStr;
    }
}
