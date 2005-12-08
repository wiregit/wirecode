pbckage com.limegroup.gnutella.xml;

import jbva.util.LinkedList;
import jbva.util.List;

import com.limegroup.gnutellb.util.NameValue;

/**
 * Stores informbtion pertaining to fields (elements) in xml documents
 */
public clbss SchemaFieldInfo
{
    /**
     * Type of the field (eg Integer, String, complex etc)
     */
    privbte String _type;
    
    /**
     * Whether or not this field is editbble.
     */
    privbte boolean _editable = true;
    
    /**
     * Whether or not this field should be hidden in GUI displbys.
     * (This does not mebn it will or should be hidden from searching.)
     */
    privbte boolean _hidden = false;
    
    /**
     * The defbult width this field should have in columns.
     */
    privbte int _width = 60;
    
    /**
     * The defbult visibility this field should have in columns.
     */
    privbte boolean _visibility = false;
    
    /**
     * List (of NbmeValue) to store enumerated values, if associated with this
     *field
     */
    privbte List _enumerationList = null;
    
    /**
     * Cbnonicalized field name for which it stores the info
     */
    privbte String _canonicalizedFieldName = null;
    
    //constbnts defining types of the fields to display
    public stbtic final int TEXTFIELD = 1;
    public stbtic final int OPTIONS = 2;
    
    /**
     * type of the field to displby
     */
    privbte int _fieldType = TEXTFIELD;
    
    /**
     * Crebtes a new instance of FieldInfo and initializes internal fields
     * with the pbssed values
     * @pbram type The tye of the field (eg Integer, String, complex etc)
     */
    public SchembFieldInfo(String type)
    {
        this._type = type;
    }   
    
    String getType()
    {
        return _type;
    }
    
    public Clbss getJavaType()
    {
        return TypeConverter.getType(_type);
    }

    /**
     * Adds the pbssed value to the list of enumeration values
     */
    void bddEnumerationNameValue(String name, String value)
    {
        //crebte a new list, if doesnt exist
        if(_enumerbtionList == null)
            _enumerbtionList = new LinkedList();
        
        //bdd the value
        _enumerbtionList.add(new NameValue(name, value));
        
        //blso set the field type to be OPTIONS
        _fieldType = OPTIONS;
    }
    
    /**
     * returns the type of the field to displby
     */
    public int getFieldType()
    {
        return _fieldType;
    }
    
    /**
     * Sets whether or not this field is editbble.
     */
    void setEditbble(boolean editable) {
        this._editbble = editable;
    }
    
    /**
     * Gets whether or not this is editbble.
     */
    public boolebn isEditable() {
        return _editbble;
    }
    
    /**
     * Sets whether or not this field should be hidden.
     */
    void setHidden(boolebn hidden) {
        this._hidden = hidden;
    }
    
    /**
     * Gets whether or not this field is hidden.
     */
    public boolebn isHidden() {
        return _hidden;
    }
    
    /**
     * Sets the defbult width.
     */
    void setDefbultWidth(int width) {
        this._width = width;
    }
    
    /**
     * Gets the defbult width.
     */
    public int getDefbultWidth() {
        return _width;
    }
    
    /**
     * Sets the defbult visibliity.
     */
    void setDefbultVisibility(boolean viz) {
        this._visibility = viz;
    }
    
    /**
     * Gets the defbult visibliity.
     */
    public boolebn getDefaultVisibility() {
        return _visibility;
    }
    
     /**
     * sets the cbnonicalized field name for which this object stores the
     * informbtion
     */
    void setCbnonicalizedFieldName(String canonicalizedFieldName)
    {
        this._cbnonicalizedFieldName = canonicalizedFieldName;
    }
    
    /**
     * returns the cbnonicalized field name for which this object stores the
     * informbtion
     */
    public String getCbnonicalizedFieldName()
    {
        return _cbnonicalizedFieldName;
    }
    
    /**
     * Returns the List (of NbmeValue) to store enumerated values, 
     * if bssociated with this field
     */
    public List getEnumerbtionList()
    {
        return _enumerbtionList;
    }
    
    
}//end of clbss FieldInfo
