/*
 * LimeXMLSchemaFieldExtractor.java
 *
 * Created on May 1, 2001, 1:23 PM
 */

package com.limegroup.gnutella.xml;
import java.util.*;


import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

/**
 * Helper class to extract field names from a schema document
 * Note: This class is incomplete. It works only for subset of schemas. 
 * Some standard API should be used when available.
 *<p>
 * Some of Many Limitations:
 *<ul>
 * <li>Cant's use IDREF 
 *</li>
 *</ul>
 * @author  asingla
 * @version
 */
public class LimeXMLSchemaFieldExtractor
{
    
    /**
     * The map from names to corresponding FieldTypeSet
     */
    private static Map nameFieldTypeSetMap = new HashMap();
    
    private static final String DUMMY = "DUMMY";
    
    /**
     * Set of primitive types
     */
    private static final Set PRIMITIVE_TYPES;
    
    //initialize the static variables
    static
    {
        //create a new HashSet
        PRIMITIVE_TYPES = new HashSet();
        //fill it with primitive types
        PRIMITIVE_TYPES.add("xsi:string");
        PRIMITIVE_TYPES.add("string");
        PRIMITIVE_TYPES.add("xsi:boolean");
        PRIMITIVE_TYPES.add("boolean");
        PRIMITIVE_TYPES.add("xsi:float");
        PRIMITIVE_TYPES.add("float");
        PRIMITIVE_TYPES.add("xsi:double");
        PRIMITIVE_TYPES.add("double");
        PRIMITIVE_TYPES.add("xsi:decimal");
        PRIMITIVE_TYPES.add("decimal");
        PRIMITIVE_TYPES.add("xsi:timeDuration");
        PRIMITIVE_TYPES.add("timeDuration");
        PRIMITIVE_TYPES.add("xsi:recurringDuration");
        PRIMITIVE_TYPES.add("recurringDuration");
        PRIMITIVE_TYPES.add("xsi:binary");
        PRIMITIVE_TYPES.add("binary");
        PRIMITIVE_TYPES.add("xsi:uriReference");
        PRIMITIVE_TYPES.add("uriReference");
        PRIMITIVE_TYPES.add("xsi:ID");
        PRIMITIVE_TYPES.add("ID");
        PRIMITIVE_TYPES.add("xsi:IDREF");
        PRIMITIVE_TYPES.add("IDREF");
        PRIMITIVE_TYPES.add("xsi:ENTITY");
        PRIMITIVE_TYPES.add("ENTITY");
        PRIMITIVE_TYPES.add("xsi:Qname");
        PRIMITIVE_TYPES.add("Qname");
    }

    
    public static String[] getFields(Document document)
    {
        //traverse the document
        Element root = document.getDocumentElement();
        traverse(root);
        
        System.out.println("Map = " + nameFieldTypeSetMap);
        return null;
    }
    
    
    private static void traverse(Node n)
    {
        //get the name of the node
        String name = n.getNodeName();
        
        //if element
        if(isElementTag(name))
        {
           // processElementTag();
        }
        
        //get attributes
        NamedNodeMap  nnm = n.getAttributes();
        //Node name = nnm.getNamedItem("name");
        if(name != null)
            System.out.print(name + "");
        System.out.println("");
        NodeList children = n.getChildNodes();
        int numChildren = children.getLength();
        for(int i=0;i<numChildren; i++)
        {
            Node child = children.item(i);
            traverse(child);
	    }
    }
    
    
    private static boolean isElementTag(String tag)
    {
        if(tag.equals("element"))
            return true;
        return false;
    }
    
    
    
private static class FieldTypeSet
{
    boolean isReferenced = false;
    private Set /* of FieldTypePair */ _elements;
    
}

private static class Type
{
    Type _type = null;
}

/**
 * Stores the field and corresponding type
 */
private static class FieldTypePair
{
    /**
     * Name of the field
     */
    private String _field;
    
    /**
     * Reference to the type of the field (Is null, if the type is a basic
     * predefined type (like integer, float etc).
     */
    private Type _type;
    
    public FieldTypePair(String field, Type type)
    {
        this._field = field;
        this._type = type;
    }
    
    public String getField()
    {
        return _field;
    }
    
    public Type getType()
    {
        return _type;
    }
}
    
}
