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
 * <ul>
 * <li>Cant's use atleast the following tags: group, all 
 * </li>
 * <li>Will work only if schema is valid. If schema is invalid (has errors),
 * the result may be unpredictable 
 * </ul>
 * Its just a 'quick & dirty' approach to extract the field names. Whenever
 * available, a standard parser should be used for parsing schemas. It is 
 * beyond the scope of current project to implement a parser that works with
 * all the schemas.
 * @author  asingla
 * @version
 */
class LimeXMLSchemaFieldExtractor
{
    
    /**
     * The map from names to corresponding FieldTypeSet
     */
    private static Map _nameFieldTypeSetMap = new HashMap();
    
    private static final String DUMMY = "DUMMY";
    
    /**
     * Set of primitive types
     */
    private static final Set PRIMITIVE_TYPES;
    
    private static int _uniqueCount = 1;
    
    private static String _lastUniqueComplexTypeName = "";
    
    private static Set _referencedNames = new HashSet(); 
    
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
        try
        {
        //traverse the document
        Element root = document.getDocumentElement();
        traverse(root);
        }
        finally
        {
        System.out.println("Map = " + _nameFieldTypeSetMap);
        return null;
        }
    }
    
    
    private static void traverse(Node n)
    {
        //get the name of the node
        String name = n.getNodeName();
        
        //if element
        if(isElementTag(name))
        {
           processElementTag(n);
           
           //get and process children
            NodeList children = n.getChildNodes();
            int numChildren = children.getLength();
            for(int i=0;i<numChildren; i++)
            {
                Node child = children.item(i);
                traverse(child);
            }
        }
        else if(isComplexTypeTag(name))
        {
            System.out.println("processing complexType");
            processComplexTypeTag(n);
        }
        else
        {
             //get and process children
            NodeList children = n.getChildNodes();
            int numChildren = children.getLength();
            for(int i=0;i<numChildren; i++)
            {
                Node child = children.item(i);
                traverse(child);
            }
        }
            
        
//        //get attributes
//        NamedNodeMap  attributes = n.getAttributes();
//        //Node name = attributes.getNamedItem("name");
//        if(name != null)
//            System.out.print(name + "");
//        System.out.println("");
        

    }
    
    
    private static void processComplexTypeTag(Node n)
    {
        String name = _lastUniqueComplexTypeName;
        //get attributes
        NamedNodeMap  attributes = n.getAttributes();
        //get name attribute
        Node nameAttribute = attributes.getNamedItem("name");
        if(nameAttribute != null)
        {
            name = nameAttribute.getNodeValue();   
        }
        
        //get new fieldtype set
        FieldTypeSet fieldTypeSet = new FieldTypeSet();
        
        //get and process children
        NodeList children = n.getChildNodes();
        int numChildren = children.getLength();
        for(int i=0;i<numChildren; i++)
        {
            Node child = children.item(i);
            
            //get the name of the node
            String childNodeName = child.getNodeName();
            
            System.out.println("childNode = " + childNodeName);
            
            
            //if element
            if(isElementTag(childNodeName))
            {
                processChildElementTag(child,fieldTypeSet);
            }
            else if(isAttributeTag(childNodeName))
            {
                processChildAttributeTag(child,fieldTypeSet);
            }
        }
        
        //add mapping to _nameFieldTypeSetMap
        _nameFieldTypeSetMap.put(name, fieldTypeSet);        
    }
    
    private static void processChildElementTag(Node n, FieldTypeSet fieldTypeSet)
    {
         //get attributes
        NamedNodeMap  attributes = n.getAttributes();
        
        //get name attribute
        Node nameAttribute = attributes.getNamedItem("name");
        if(nameAttribute == null)
        {
            //get ref attribute
            Node refAttribute = attributes.getNamedItem("ref");
        
            if(refAttribute == null)
            {
                //return, cant do anything
                //anu check if something else can be done
                return;
            }

            //get the ref name
            String refName = refAttribute.getNodeValue();
            
            System.out.println("refName = " + refName);

            //TODO anu think
            //add mapping to fieldTypeSet
            fieldTypeSet.add(new FieldTypePair(refName, refName));
        }
        else
        {
            String name = nameAttribute.getNodeValue();

            //get type attribute
            Node typeAttribute = attributes.getNamedItem("type");
            String typeName;
            if(typeAttribute != null)
            {
                typeName = typeAttribute.getNodeValue();
            }
            else
            {
                typeName = getUniqueComplexTypeName();

                //also store it in _lastUniqueComplexTypeName for future use
                _lastUniqueComplexTypeName = typeName;
                
                //traverse children
                traverseChildren(n);
            }

           //add mapping to fieldTypeMap
           fieldTypeSet.add(new FieldTypePair(name, typeName));   
        }

    }
    
    private static void processChildAttributeTag(Node n, FieldTypeSet fieldTypeSet)
    {
         //get attributes
        NamedNodeMap  attributes = n.getAttributes();
        
        //get name attribute
        Node nameAttribute = attributes.getNamedItem("name");
        if(nameAttribute == null)
        {
            //get ref attribute
            Node refAttribute = attributes.getNamedItem("ref");
        
            if(refAttribute == null)
            {
                //return, cant do anything
                //anu check if something else can be done
                return;
            }

            //get the ref name
            String refName = refAttribute.getNodeValue();
            
            System.out.println("refName = " + refName);

            //TODO anu think
            //add mapping to fieldTypeSet
            fieldTypeSet.add(new FieldTypePair(refName, refName));
        }
        else
        {
            String name = nameAttribute.getNodeValue();

            //get type attribute
            Node typeAttribute = attributes.getNamedItem("type");
            String typeName;
            if(typeAttribute != null)
            {
                typeName = typeAttribute.getNodeValue();
            }
            else
            {
                typeName = getUniqueComplexTypeName();

                //also store it in _lastUniqueComplexTypeName for future use
                _lastUniqueComplexTypeName = typeName;
                
                //traverse children
                traverseChildren(n);
            }

           //add mapping to fieldTypeMap
           fieldTypeSet.add(new FieldTypePair(name, typeName));   
        }

    }
    
    private static void traverseChildren(Node n)
    {
        //get and process children
        NodeList children = n.getChildNodes();
        int numChildren = children.getLength();
        for(int i=0;i<numChildren; i++)
        {
            Node child = children.item(i);
            traverse(child);
        }
    }
    
    private static boolean isComplexTypeTag(String tag)
    {
        if(tag.trim().equals("complexType") || tag.trim().equals("xsd:complexType"))
            return true;
        return false;
    }
    
    private static boolean isAttributeTag(String tag)
    {
        if(tag.trim().equals("attribute") || tag.trim().equals("xsd:attribute"))
            return true;
        return false;
    }
    
    
    private static void processElementTag(Node n)
    {
        //get attributes
        NamedNodeMap  attributes = n.getAttributes();
        
        //get name attribute
        Node nameAttribute = attributes.getNamedItem("name");
        if(nameAttribute == null)
        {
            //TODO anu
            //check if we need to do this
            processElementTagWithoutNameAttribute(n, attributes);
            return;
        }
        String name = nameAttribute.getNodeValue();
//        System.out.println("name = " + name);
        
//        //get new fieldtype set
//        FieldTypeSet fieldTypeSet = new FieldTypeSet();
        
        //get type attribute
        Node typeAttribute = attributes.getNamedItem("type");
        String typeName;
        if(typeAttribute != null)
        {
            typeName = typeAttribute.getNodeValue();
//            //add to fieldTypeSet
//            fieldTypeSet.add(new FieldTypePair(DUMMY, typeName));
        }
        else
        {
//            processElementChildren(n, fieldTypeSet);
            
            typeName = getUniqueComplexTypeName();
            
            //also store it in _lastUniqueComplexTypeName for future use
            _lastUniqueComplexTypeName = typeName;
        }
        
       //add mapping to _nameFieldTypeSetMap
       addToFieldTypeSetMap(name, typeName); 
//        _nameFieldTypeSetMap.put(name, fieldTypeSet);
    }
    
    private static void processElementTagWithoutNameAttribute(Node n, 
            NamedNodeMap attributes)
    {
        //get "ref" attribute
        Node refAttribute = attributes.getNamedItem("ref");
        
        if(refAttribute == null)
        {
            //return, cant do anything
            //anu check if something else can be done
            return;
        }
        
        //get the ref name
        String refName = refAttribute.getNodeValue();
        
        //TODO anu think
//        //add mapping to _nameFieldTypeSetMap
//        addToFieldTypeSetMap(name, typeName, typeName); 
    }
    
    /**
     * @param n The node whose children need to be processed
     * @param fieldTypeSet The set to whichinformation related to children
     * is to be put
     */
    private static void processElementChildren(Node n, FieldTypeSet fieldTypeSet)
    {
        
    }
    
    
    /**
     * @modifies _uniqueCount
     */
    private static String getUniqueComplexTypeName()
    {
        return "COMPLEXTYPE___" + _uniqueCount++;
    }
    
    
    private static void addToFieldTypeSetMap(String name, String typeName)
    {
        //get new fieldtype set
        FieldTypeSet fieldTypeSet = new FieldTypeSet();
        fieldTypeSet.add(new FieldTypePair(DUMMY, typeName));
        
        //add mapping to _nameFieldTypeSetMap
        _nameFieldTypeSetMap.put(name, fieldTypeSet);
        
        //add type name to the referenced names set
        _referencedNames.add(typeName);
    }
    
    private static boolean isElementTag(String tag)
    {
        if(tag.trim().equals("element") || tag.trim().equals("xsd:element"))
            return true;
        return false;
    }
    
    
    
private static class FieldTypeSet
{
    private Set /* of FieldTypePair */ _elements = new HashSet();
    
    public void add(FieldTypePair fieldTypePair)
    {
        _elements.add(fieldTypePair);
    }
    
    public String toString()
    {
        return _elements.toString();
    }
    
}

//private static class Type
//{
//    Type _type = null;
//}

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
//    private Type _type;
      private String _type;
    
    public FieldTypePair(String field, String type)
    {
        this._field = field;
        this._type = type;
    }
    
    public String getField()
    {
        return _field;
    }
    
    public String getType()
    {
        return _type;
    }
    
    public String toString()
    {
        return "[" + _field + ":" + _type + "]";
    }
}
    
}
