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
 * <li>Cant's use IDREF 
 * </li>
 * <li> might have problems if same field name is used in two different
 * contexts in the schema document (attribute names are no problem)
 * </li>
 * <li>Will work only if schema is valid. If schema is invalid (has errors),
 * the result may be unpredictable 
 * </li>
 * <li> Doesn't resolve references to other schemas
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
    private static Map _nameFieldTypeSetMap = null;
    
    /**
     * A dummy name to be used when there's no name for a field
     */
    private static final String DUMMY = "DUMMY";
    
    /**
     * Set of primitive types (as per XML Schema specifications)
     */
    private static final Set PRIMITIVE_TYPES;
    
    /**
     * A counter to generate unique number which can be appened to strings
     * to form unique strings
     */
    private static int _uniqueCount = 1;
    
    /**
     * The last autogenerated name for 'complexType' element
     */
    private static String _lastUniqueComplexTypeName = "";
    
    /**
     * The field names that are referenced/used from some other field
     * (ie which can not be root element)
     */
    private static Set _referencedNames = null;
    
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
        PRIMITIVE_TYPES.add("xsi:NUMTOKEN");
        PRIMITIVE_TYPES.add("NUMTOKEN");
        PRIMITIVE_TYPES.add("xsi:Qname");
        PRIMITIVE_TYPES.add("Qname");
    }

    /**
     * Returns all the field names in the passed document. The fields returned
     * are in canonicalized form.
     * @param document The XML Schema documnet from where to extract fields
     * @requires The document be a valid XML Schema without any errors
     * @return A list containing all the field names in the passed document. 
     * The fields returned
     * are in canonicalized form.
     * @see XMLStringUtils for more information on canonicalized form
     */
    public synchronized static List getFields(Document document)
    {
        try
        {
            //reInitialize fields(internal datastructures)
            reInitializeMemberFields();
            
            //traverse the document
            Element root = document.getDocumentElement();
            traverse(root);
            
            //now get the root element below <xsd:schema>
            String rootElementName = getRootElementName();
            
            List fieldNames = new LinkedList(); 
            fillWithFieldNames(fieldNames, 
                (FieldTypeSet)_nameFieldTypeSetMap.get(rootElementName),
                rootElementName);
            
            System.out.println("fields: " + fieldNames);
            
            return fieldNames;
        }
        catch(NullPointerException npe)
        {
            return new LinkedList();
        }
    }
    
    private static void reInitializeMemberFields()
    {
        _nameFieldTypeSetMap = new HashMap();
        _uniqueCount = 1;
        _lastUniqueComplexTypeName = "";
        _referencedNames = new HashSet(); 
    }
    
    private static void  fillWithFieldNames(List fieldNames,
        FieldTypeSet fieldTypeSet,
        final String prefix)
    {
        
        //get the iterator over the elements in the fieldtypeSet
        Iterator iterator = fieldTypeSet.iterator();

        while(iterator.hasNext())
        {
            FieldTypePair fieldTypePair = (FieldTypePair)iterator.next();
            //get the field type set corresponding to this field pair's type
            FieldTypeSet newFieldTypeSet 
                = (FieldTypeSet)_nameFieldTypeSetMap.get(
                fieldTypePair.getType());
            String field = fieldTypePair.getField();
            if(newFieldTypeSet == null)
            {
                if(!isDummy(field))
                {
                    fieldNames.add(prefix 
                        + XMLStringUtils.DELIMITER + field);
                }
                else
                {
                    fieldNames.add(prefix);
                }
            }
            else
            {
                if(!isDummy(field))
                {
                    fillWithFieldNames(fieldNames,newFieldTypeSet,
                        prefix + XMLStringUtils.DELIMITER
                        + field);
                }
                else
                {
                    fillWithFieldNames(fieldNames,newFieldTypeSet,prefix);
                }
            }
        }
    }
    
    private static boolean isDummy(String field)
    {
        if(field.trim().equals(DUMMY))
            return true;
    
        return false;
    }
    
    
    /**
     * Returns the root element below <xsd:schema>
     */
    private static String getRootElementName()
    {
        //get the set of keys in _nameFieldTypeSetMap
        //one of this is the root element
        Set possibleRoots = ((HashMap)((HashMap)_nameFieldTypeSetMap).clone()).keySet();
        
        //Iterate over set of _referencedNames
        //and remove those from possibleRoots
        Iterator iterator = _referencedNames.iterator();
        while(iterator.hasNext())
        {
            //remove from set of possibleRoots
            possibleRoots.remove(iterator.next());
        }
        
        //return the first element in the set
        Iterator possibleRootsIterator = possibleRoots.iterator();
        return (String)possibleRootsIterator.next();
    }
    

    /**
     * Traverses the given node as well as its children and fills in the
     * datastructures (_nameFieldTypeSetMap, _referencedNames etc) using
     * the information gathered
     * @param n The node which has to be traveresed (along with its children)
     * @modifies this
     */
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
    }
    
    
    /**
     * Processes the 'complexType' tag
     * @param n The node having 'complexType' tag 
     */
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
            processChildOfComplexType(child,fieldTypeSet);
        }
        
        //add mapping to _nameFieldTypeSetMap
        _nameFieldTypeSetMap.put(name, fieldTypeSet);     
        
        //also add to the _referencedNames
        _referencedNames.add(name);
    }
    
    /**
     * Processes the child of a 'complexType' element
     * @param n The child to be processed
     * @param fieldTypeSet The set to which information related to the child
     * is to be put
     * @modifies fieldTypeSet
     */
    private static void processChildOfComplexType(Node n, 
        FieldTypeSet fieldTypeSet)
    {
            //get the name of the node
            String nodeName = n.getNodeName();
            
            //if element
            if(isElementTag(nodeName))
            {
                processChildElementTag(n,fieldTypeSet);
            }
            else if(isAttributeTag(nodeName))
            {
                processChildAttributeTag(n,fieldTypeSet);
            }
            else
            {
                //get the child nodes of this node, and process them
                NodeList children = n.getChildNodes();
                int numChildren = children.getLength();
                for(int i=0;i<numChildren; i++)
                {
                    Node child = children.item(i);
                    processChildOfComplexType(child,fieldTypeSet);
                }
            }
    }
    
    /**
     * Processes the child that has the "element' tag
     * @param n The node whose child needs to be processed
     * @param fieldTypeSet The set to which information related to the child
     * is to be put
     * @modifies fieldTypeSet
     */
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
            
            //add mapping to fieldTypeSet
            fieldTypeSet.add(new FieldTypePair(refName, refName));
            
            //also add the refName to set of _referencedNames
            _referencedNames.add(refName);
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
           fieldTypeSet.add(new FieldTypePair(name, removeNameSpace(typeName)));   
        }

    }
    
    /**
     * Removes the namespace part from the passed string
     * @param typeName The string whose namespace part is to be removed
     * @return The string after removing the namespace part (if present).
     * For eg If the passed string was "ns:type", the returned value will
     * be "type"
     */
    private static String removeNameSpace(String typeName)
    {
        //if no namespace part
        if(typeName.indexOf(':') == -1)
        {
            //return the original string
            return typeName;
        }
        else 
        {
            //return the part of the string without namespace
            return typeName.substring(typeName.indexOf(':') + 1);
        }
        
    }
    
    /**
     * Processes the attribute child element
     * @param n The node whose child needs to be processed
     * @param fieldTypeSet The set to which information related to the child
     * is to be put
     * @modifies fieldTypeSet
     */
    private static void processChildAttributeTag(Node n, FieldTypeSet fieldTypeSet)
    {
        //get attributes
        NamedNodeMap attributes = n.getAttributes();
        
        //get name attribute
        Node nameAttribute = attributes.getNamedItem("name");
        Node typeAttribute = attributes.getNamedItem("type");
        if(nameAttribute == null || typeAttribute == null)
        {
            //cant do much, return
            return;
        }
       
        //get field and type names
        //append DELIMITER after name of the attribute (as per convention
        //@see XMLStringUtils
        String name = nameAttribute.getNodeValue() + XMLStringUtils.DELIMITER;
        String typeName = typeAttribute.getNodeValue();
       
        //add mapping to fieldTypeMap
        fieldTypeSet.add(new FieldTypePair(name, removeNameSpace(typeName)));   
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
//            //TODO anu
//            //check if we need to do this
//            processElementTagWithoutNameAttribute(n, attributes);
//            return;
        }
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
        }
        
       //add mapping to _nameFieldTypeSetMap
       addToFieldTypeSetMap(name, typeName); 
    }
    
//    private static void processElementTagWithoutNameAttribute(Node n, 
//            NamedNodeMap attributes)
//    {
//        //get "ref" attribute
//        Node refAttribute = attributes.getNamedItem("ref");
//        
//        if(refAttribute == null)
//        {
//            //return, cant do anything
//            //anu check if something else can be done
//            return;
//        }
//        
//        //get the ref name
//        String refName = refAttribute.getNodeValue();
//        
//        //TODO anu think
////        //add mapping to _nameFieldTypeSetMap
////        addToFieldTypeSetMap(name, typeName, typeName); 
//    }
    
    
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
        fieldTypeSet.add(new FieldTypePair(DUMMY, removeNameSpace(typeName)));
        
        //add mapping to _nameFieldTypeSetMap
        _nameFieldTypeSetMap.put(name, fieldTypeSet);
        
        //add type name to the referenced names set
        _referencedNames.add(removeNameSpace(typeName));
    }
    
    private static boolean isElementTag(String tag)
    {
        if(tag.trim().equals("element") || tag.trim().equals("xsd:element"))
            return true;
        return false;
    }
    
    
/**
 * A Set <FieldTypePair> of fields and corresponding types
 */   
private static class FieldTypeSet
{
    private Set /* of FieldTypePair */ _elements = new HashSet();
   
    /**
     * Add the given fieldType pair to the set of elements
     * @param fieldTypePair the field-type pair to be added
     */
    public void add(FieldTypePair fieldTypePair)
    {
        //add to the _elements
        _elements.add(fieldTypePair);
    }

    /**
     * Returns an iterator over the elements
     */
    public Iterator iterator()
    {
       return _elements.iterator(); 
    }
    
    /**
     * Return string representation of all the elements
     */
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
