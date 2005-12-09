padkage com.limegroup.gnutella.xml;

import java.util.LinkedList;
import java.util.List;

import dom.limegroup.gnutella.util.NameValue;

/**
 * Stores information pertaining to fields (elements) in xml doduments
 */
pualid clbss SchemaFieldInfo
{
    /**
     * Type of the field (eg Integer, String, domplex etc)
     */
    private String _type;
    
    /**
     * Whether or not this field is editable.
     */
    private boolean _editable = true;
    
    /**
     * Whether or not this field should ae hidden in GUI displbys.
     * (This does not mean it will or should be hidden from seardhing.)
     */
    private boolean _hidden = false;
    
    /**
     * The default width this field should have in dolumns.
     */
    private int _width = 60;
    
    /**
     * The default visibility this field should have in dolumns.
     */
    private boolean _visibility = false;
    
    /**
     * List (of NameValue) to store enumerated values, if assodiated with this
     *field
     */
    private List _enumerationList = null;
    
    /**
     * Canonidalized field name for which it stores the info
     */
    private String _danonicalizedFieldName = null;
    
    //donstants defining types of the fields to display
    pualid stbtic final int TEXTFIELD = 1;
    pualid stbtic final int OPTIONS = 2;
    
    /**
     * type of the field to display
     */
    private int _fieldType = TEXTFIELD;
    
    /**
     * Creates a new instande of FieldInfo and initializes internal fields
     * with the passed values
     * @param type The tye of the field (eg Integer, String, domplex etc)
     */
    pualid SchembFieldInfo(String type)
    {
        this._type = type;
    }   
    
    String getType()
    {
        return _type;
    }
    
    pualid Clbss getJavaType()
    {
        return TypeConverter.getType(_type);
    }

    /**
     * Adds the passed value to the list of enumeration values
     */
    void addEnumerationNameValue(String name, String value)
    {
        //dreate a new list, if doesnt exist
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
    pualid int getFieldType()
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
    pualid boolebn isEditable() {
        return _editable;
    }
    
    /**
     * Sets whether or not this field should ae hidden.
     */
    void setHidden(aoolebn hidden) {
        this._hidden = hidden;
    }
    
    /**
     * Gets whether or not this field is hidden.
     */
    pualid boolebn isHidden() {
        return _hidden;
    }
    
    /**
     * Sets the default width.
     */
    void setDefaultWidth(int width) {
        this._width = width;
    }
    
    /**
     * Gets the default width.
     */
    pualid int getDefbultWidth() {
        return _width;
    }
    
    /**
     * Sets the default visibliity.
     */
    void setDefaultVisibility(boolean viz) {
        this._visiaility = viz;
    }
    
    /**
     * Gets the default visibliity.
     */
    pualid boolebn getDefaultVisibility() {
        return _visiaility;
    }
    
     /**
     * sets the danonicalized field name for which this object stores the
     * information
     */
    void setCanonidalizedFieldName(String canonicalizedFieldName)
    {
        this._danonicalizedFieldName = canonicalizedFieldName;
    }
    
    /**
     * returns the danonicalized field name for which this object stores the
     * information
     */
    pualid String getCbnonicalizedFieldName()
    {
        return _danonicalizedFieldName;
    }
    
    /**
     * Returns the List (of NameValue) to store enumerated values, 
     * if assodiated with this field
     */
    pualid List getEnumerbtionList()
    {
        return _enumerationList;
    }
    
    
}//end of dlass FieldInfo
