package com.limegroup.gnutella.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Stores information pertaining to fields (elements) in xml documents
 */
public class SchemaFieldInfo
{
    /**
     * Type of the field (eg Integer, String, complex etc)
     */
    private String _type;
    
    /**
     * List (of Strings) to store enumerated values, if associated with this
     *field
     */
    private List _enumerationList = null;
    
    /**
     * Canonicalized field name for which it stores the info
     */
    private String _canonicalizedFieldName = null;
    
    //constants defining types of the fields to display
    public static final int TEXTFIELD = 1;
    public static final int OPTIONS = 2;
    
    /**
     * type of the field to display
     */
    private int _fieldType = TEXTFIELD;
    
    /**
     * Creates a new instance of FieldInfo and initializes internal fields
     * with the passed values
     * @param type The tye of the field (eg Integer, String, complex etc)
     */
    public SchemaFieldInfo(String type)
    {
        this._type = type;
    }   
    
    String getType()
    {
        return _type;
    }
    
    public Class getJavaType()
    {
        return TypeConverter.getType(_type);
    }

    /**
     * Adds the passed value to the list of enumeration values
     */
    void addEnumerationValue(String value)
    {
        //create a new list, if doesnt exist
        if(_enumerationList == null)
            _enumerationList = new LinkedList();
        
        //add the value
        _enumerationList.add(value);
        
        //also set the field type to be OPTIONS
        _fieldType = OPTIONS;
    }
    
    /**
     * returns the type of the field to display
     */
    public int getFieldType()
    {
        return _fieldType;
    }
    
    
     /**
     * sets the canonicalized field name for which this object stores the
     * information
     */
    void setCanonicalizedFieldName(String canonicalizedFieldName)
    {
        this._canonicalizedFieldName = canonicalizedFieldName;
    }
    
    /**
     * returns the canonicalized field name for which this object stores the
     * information
     */
    public String getCanonicalizedFieldName()
    {
        return _canonicalizedFieldName;
    }
    
    /**
     * Returns the List (of Strings) to store enumerated values, 
     * if associated with this field
     */
    public List getEnumerationList()
    {
        return _enumerationList;
    }
    
    /**
     * Returns Mapping from EnumerativeValue => Mapped Value
     * (String => String). Returns null, if no enumerative values exist
     */
    public Map getDefaultEnumerativeValueMap()
    {
        //return null, if there are no enumerative values
        if(_enumerationList == null)
            return null;
        
        //else create a new Map
        Map enumerativeValueMap = new HashMap(
            (int)(_enumerationList.size() * 4.0/3.0 + 1),0.75f);
        
        //add the default mappings to the map
        Iterator iterator = _enumerationList.iterator();
        while(iterator.hasNext())
        {
            //get the next value
            String value = (String)iterator.next();
            //add the mapping
            enumerativeValueMap.put(value,value);
        }
        
        //return the mappings
        return enumerativeValueMap;
    }
    
    
}//end of class FieldInfo
