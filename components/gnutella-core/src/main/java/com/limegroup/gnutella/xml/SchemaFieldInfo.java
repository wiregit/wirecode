package com.limegroup.gnutella.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.util.NameValue;

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
     * Whether or not this field is editable.
     */
    private boolean _editable = true;
    
    /**
     * List (of NameValue) to store enumerated values, if associated with this
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
    void addEnumerationNameValue(String name, String value)
    {
        //create a new list, if doesnt exist
        if(_enumerationList == null)
            _enumerationList = new LinkedList();
        
        //add the value
        _enumerationList.add(new NameValue(name, value));
        
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
     * Sets whether or not this field is editable.
     */
    void setEditable(boolean editable) {
        this._editable = editable;
    }
    
    /**
     * Gets whether or not this is editable.
     */
    public boolean isEditable() {
        return _editable;
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
     * Returns the List (of NameValue) to store enumerated values, 
     * if associated with this field
     */
    public List getEnumerationList()
    {
        return _enumerationList;
    }
    
    
}//end of class FieldInfo
