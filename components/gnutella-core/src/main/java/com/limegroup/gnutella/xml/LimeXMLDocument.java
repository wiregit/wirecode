package com.limegroup.gnutella.xml;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.licenses.CCConstants;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.LicenseConstants;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.metadata.WeedInfo;
import com.limegroup.gnutella.metadata.WRMXML;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;


/**
 * @author  Sumeet Thadani
 * A LimeXMLDocument is basically a hashmap that maps a
 * Names of fields to the values as per a XML document.
 */
public class LimeXMLDocument implements Serializable {
    
    private static final Log LOG = LogFactory.getLog(LimeXMLDocument.class);

    public static final String XML_ID_ATTRIBUTE = "identifier__";
    public static final String XML_ACTION_ATTRIBUTE = "action__";
    public static final String XML_INDEX_ATTRIBUTE = "index__";
    public static final String XML_LICENSE_ATTRIBUTE = "license__";
    public static final String XML_LICENSE_TYPE_ATTRIBUTE = "licensetype__";
    
    /**
     * The current version of LimeXMLDocuments.
     *
     * Increment this number as features are added which require
     * reparsing documents on disk.
     */
    private static final int CURRENT_VERSION = 2;

	/**
	 * Cached hash code for this instance.
	 */
	private volatile transient int hashCode = 0;

    /** For backwards compatibility with downloads.dat. */
    private static final long serialVersionUID = 7396170507085078485L;

    //TODO2: Need to build in the ability to work with multiple instances
    //of some fields. 
    
    /**
     * Map of canonical attribute name -> value.
     */
    private Map fieldToValue = new HashMap();
    
    /**
     * The schema of this LimeXMLDocument.
     */
    private String schemaUri;
    
    /**
     * The cached string of attributes.
     */
    private transient String attributeString;

    /** 
     * The file this is related to.  Can be null if pure meta-data.
     */
    private transient File fileId;
    
    /**
     * The action that this doc has.
     */
    private transient String action;
    
    /**
     * The version of this LimeXMLDocument.
     */
    private int version = CURRENT_VERSION;
    boolean isCurrent() { return version == CURRENT_VERSION; }
    void setCurrent() { version = CURRENT_VERSION; }
    
    /**
     * Cached list of keywords.  Because keywords are only filled up
     * upon construction, they can be cached upon retrieval.
     */
    private transient List CACHED_KEYWORDS = null;
    
    /** The kind of license this has. */
    private transient int licenseType = LicenseConstants.NO_LICENSE;

    /**
     * Constructs a LimeXMLDocument with the given string.
     */
    public LimeXMLDocument(String xml)
      throws SAXException, SchemaNotFoundException, IOException {
        if(xml==null || xml.equals(""))
            throw new SAXException("null or empty string");

        InputSource doc = new InputSource(new StringReader(xml));
        XMLParsingUtils.ParseResult result = XMLParsingUtils.parse(doc);
        if (result.isEmpty())
            throw new IOException("No element present");
        if (result.schemaURI == null)
            throw new SchemaNotFoundException("no schema");

        this.fieldToValue = (Map)result.get(0);
        this.schemaUri = result.schemaURI;
        setFields(result.canonicalKeyPrefix);
        
        if(!isValid())
            throw new IOException("Invalid XML: " + xml);
    }

    /**
     * Constructs a new LimeXMLDocument
     * @param map Map with keys in canonicalized
     * form and corresponding values that will be used to create the 
     * new instance
     * @param schemaURI The schema URI for the LimeXMLDocument to be
     * created
     */    
    LimeXMLDocument(Map map, String schemaURI, String keyPrefix) 
      throws IOException {
        if(map.isEmpty())
            throw new IllegalArgumentException("empty map");

        this.schemaUri = schemaURI;
        this.fieldToValue = map;
        fieldToValue.remove(keyPrefix + XML_ID_ATTRIBUTE); // remove id.
        setFields(keyPrefix);
        
        if(!isValid())
            throw new IOException("invalid doc! "+map+" \nschema uri: "+schemaURI);
        
    }

    /**
     * Constructs a new LimeXMLDocument
     * @param nameValueList List (of Map.Entry) of fieldnames (in canonicalized
     * form) and corresponding values that will be used to create the 
     * new instance
     * @param schemaURI The schema URI for the LimeXMLDocument to be
     * created
     */
    public LimeXMLDocument(Collection nameValueList, String schemaURI) {
        if(nameValueList.isEmpty())
            throw new IllegalArgumentException("empty list");

        //set the schema URI
        this.schemaUri = schemaURI;
                
        //iterate over the passed list of fieldnames & values
        for(Iterator i = nameValueList.iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            String name = (String)next.getKey();
            Object value = next.getValue();
            fieldToValue.put(name.trim(), value);
        }
        
        // scan for action/id/etc..
        scanFields();
        
        if(!isValid())
            throw new IllegalArgumentException("Invalid Doc!");
    }
    
    /**
     * Determines whether or not this LimeXMLDocument is valid.
     */
    boolean isValid() {
        // no schemaURI or the schemaURI doesn't map to a LimeXMLSchema
        if(schemaUri == null || getSchema() == null)
            return false;

        // no valid attributes.
        if(getAttributeString().length() == 0)
            return false;
            
        return true;
    }

    /**
     * Reads the object and initializes transient fields.
     */
    private void readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        scanFields();
    }

    /**
     * Returns the number of fields this document has.
     */
    public int getNumFields() {
        return fieldToValue.size();
    }

    /**
     * Returns all the non-numeric fields in this.  These are
     * not necessarily QRP keywords.  For example, one of the
     * elements of the returned list may be "Some comment-blah".
     * QRP code may want to split this into the QRP keywords
     * "Some", "comment", and "blah".
     *
     * Indivisible keywords are not returned.  To retrieve those,
     * use getIndivisibleKeywords().  Indivisible keywords are
     * those which QRP will not split up.
     */
    public List getKeyWords() {
        if( CACHED_KEYWORDS != null )
            return CACHED_KEYWORDS;

        List retList = new ArrayList();
        Iterator iter = fieldToValue.keySet().iterator();
        while(iter.hasNext()){
            String currKey = (String) iter.next();
            String val = (String) fieldToValue.get(currKey);
            if(val != null && !val.equals("") && !isIndivisible(currKey)) {
                try {
                    Double.parseDouble(val); // will trigger NFE.
                } catch(NumberFormatException ignored) {
                    retList.add(val);
                }
            }
        }
        CACHED_KEYWORDS = retList;
        return retList;
    }

    /**
     * Returns all the indivisible keywords for entry into QRP tables.
     */
    public List getKeyWordsIndivisible() {
        return LicenseConstants.getIndivisible(licenseType);
    }

    /**
     * Determines if this keyword & value is indivisible
     * (thus making QRP not split it).
     */
    private boolean isIndivisible(String currKey) {
        //the license-type is always indivisible.
        //note that for weed licenses, this works because getKeyWordsIndivisible
        //is returning a list of only 'WeedInfo.LAINFO'.  the content-id & version-id
        //are essentially lost & ignored.
        return currKey.endsWith(XML_LICENSE_TYPE_ATTRIBUTE);
    }    

    /**
     * Returns the unique identifier which identifies the schema this XML
     * document conforms to
     */
    public String getSchemaURI() {
        return schemaUri;
    }
    
    /**
     * Returns the LimeXMLSchema associated with this XML document.
     */
    public LimeXMLSchema getSchema() {
        return LimeXMLSchemaRepository.instance().getSchema(schemaUri);
    }
    
    /**
     * Returns the description of the schema URI.
     */
    public String getSchemaDescription() {
        LimeXMLSchema schema = getSchema();
        if(schema != null)
            return schema.getDescription();
        else
            return LimeXMLSchema.getDisplayString(schemaUri);
    }
    
    /**
     * Returns the name of the file that the data in this XML document 
     * corresponds to. If the meta-data does not correspond to any file
     * in the file system, this method will rerurn a null.
     */
    public File getIdentifier() {
        return fileId;
    }
    
    /**
     * Sets the identifier.
     */
    public void setIdentifier(File id) {
        fileId = id;
    }

    /**
     * Returns the action corresponding with this LimeXMLDocument.
     */
    public String getAction() {
        if(action == null)
            return "";
        else
            return action;
    }

    /**
     * Returns a Set of Map.Entry, where each key-value corresponds to a
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
     * @return a Set of Map.Entry, where each key-value corresponds to a
     * canonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     */
    public Set getNameValueSet() {
        return fieldToValue.entrySet();
    }
    
    /**
     * Returns a set of the names within this LimeXMLDocument.
     */
    public Set getNameSet() {
        return fieldToValue.keySet();
    }
    
    /**
     * Returns a collection of the values of this LimeXMLDocument.
     */
    public Collection getValueList() {
        return fieldToValue.values();
    }
    
    /**
     * Determines if a license exists that this LimeXMLDocument knows about.
     */
    public boolean isLicenseAvailable() {
        return licenseType != LicenseConstants.NO_LICENSE;
    }
    
    /**
     * Returns a string that can be used to verify if this license is valid.
     */
    public String getLicenseString() {
        if(isLicenseAvailable()) {
            String licenseStringSuffix = getVerifyableLicenseElement(licenseType);
            if (licenseStringSuffix == null)
                return null;
            for(Iterator i = fieldToValue.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry next = (Map.Entry)i.next();
                String key = (String)next.getKey();
                if (key.endsWith(licenseStringSuffix))
                    return (String)next.getValue();
            }
        }
        return null;
    }
    
    private static String getVerifyableLicenseElement(int type) {
        if (type == LicenseConstants.CC_LICENSE)
            return LimeXMLDocument.XML_LICENSE_ATTRIBUTE;
        if (LicenseConstants.isDRMLicense(type))
            return LimeXMLDocument.XML_LICENSE_TYPE_ATTRIBUTE;
        return null;
    }
    
    /**
     * Returns the license.
     */
    public License getLicense() {
        String license = getLicenseString();
        if(license != null)
            return LicenseFactory.create(license);
        else
            return null;
    }

    /**
     * Returns a list of attributes and their values in the same order
     * as is in the schema.
     */
    public List getOrderedNameValueList() {
        String[] fNames = getSchema().getCanonicalizedFieldNames();
        List retList = new ArrayList(fNames.length);
        for (int i = 0; i < fNames.length; i++) {
            String name = fNames[i].trim();
            Object value = fieldToValue.get(name);
            if (value != null)
                retList.add(new NameValue(name, value));
        }
            
        return retList;
    }
    
    /**
     * Returns the value associated with this canonicalized fieldname.
     */
    public String getValue(String fieldName) {
        return (String)fieldToValue.get(fieldName);
    }
    
    /**
     * Constructs an XML string from this document.
     */
    public String getXMLString() {
        StringBuffer fullXML = new StringBuffer();
        LimeXMLDocumentHelper.buildXML(fullXML, getSchema(), getAttributeString() + "/>");
        return fullXML.toString();
    }
    
    /**
     * Returns the attribute string with the given index.
     *
     * For example, this will return:
     *   <thing att1="value1" att2="value2" att3="value3" index="4"/>
     */
    public String getAttributeStringWithIndex(int i) {
        String attributes = getAttributeString();
        return attributes + " index=\"" + i + "\"/>";
    }
    
    /**
     * Returns the attribute string. THIS IS NOT A FULL XML ELEMENT.
     * It is purposely left unclosed so an index can easily be inserted.
     */
    private String getAttributeString() {
        if(attributeString == null)
            attributeString = constructAttributeString();
        return attributeString;
    }
    
    /**
     * Constructs the open-ended XML that contains the attributes.
     * This is purposely open-ended so that an index can easily be
     * inserted.
     * If no attributes exist, this returns an empty string,
     * to easily be marked as invalid.
     */
    private String constructAttributeString() {
        List attributes = getOrderedNameValueList();
        if(attributes.isEmpty())
            return ""; // invalid.
            
        StringBuffer tag = new StringBuffer();
        String root = getSchema().getRootXMLName();
        String type = getSchema().getInnerXMLName();
        String canonicalKey = root + "__" + type + "__";
        tag.append("<");
        tag.append(type);

        for(Iterator i = attributes.iterator(); i.hasNext(); ) {
            NameValue nv = (NameValue)i.next();
            String name = XMLStringUtils.getLastField(canonicalKey, nv.getName());
            if(name == null)
                continue;
            // Construct: ' attribute="value"'
            tag.append(" ");
            tag.append(name);
            tag.append("=\"");
            tag.append(LimeXMLUtils.encodeXML((String)nv.getValue()));
            tag.append("\"");
        }
        
        return tag.toString();
    }

	/**
	 * Overrides equals to check for equality of all xml document fields.
	 *
	 * @param o the object to compare
	 * @return <tt>true</tt> if the objects are equal, <tt>false</tt>
	 *  otherwise
	 */
	public boolean equals(Object o) {
		if(o == this)
		    return true;
		if(o == null)
		    return false;
		if(!(o instanceof LimeXMLDocument))
		    return false;

		LimeXMLDocument xmlDoc = (LimeXMLDocument)o;
		return ((schemaUri == null ? xmlDoc.schemaUri == null :
				 schemaUri.equals(xmlDoc.schemaUri)) &&
				(fileId == null ? xmlDoc.fileId == null :
				 fileId.equals(xmlDoc.fileId)) &&
				(action == null ? xmlDoc.action == null :
				 action.equals(xmlDoc.action)) &&
				(fieldToValue == null ? xmlDoc.fieldToValue == null : 
				 fieldToValue.equals(xmlDoc.fieldToValue)));
	}

	/**
	 * Overrides <tt>Object.hashCode</tt> to satisfy the contract for
	 * hashCode, given that we're overriding equals.
	 *
	 * @return a hashcode for this object for use in hash-based collections
	 */
	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			if(fieldToValue != null)
				result = 37*result + fieldToValue.hashCode();
			if(schemaUri != null)
				result = 37*result + schemaUri.hashCode();
			if(fileId != null)
				result = 37*result + fileId.hashCode();
			if(action != null)
				result = 37*result + action.hashCode();
			hashCode = result;
		} 
		return hashCode;
	}
	
	/**
	 * Returns the XML identifier for the string.
	 */
	public String toString() {
	    return getXMLString();
    }
    
    /**
     * Looks in the fields for the ACTION, IDENTIFIER, and INDEX, and a license.
     * Action is stored, index & identifier are removed.
     */
    private void scanFields() {
        String canonicalKey = getCanonicalKey(getNameValueSet());
        if(canonicalKey == null)
            return;

        setFields(canonicalKey);
        fieldToValue.remove(canonicalKey + XML_INDEX_ATTRIBUTE);
        fieldToValue.remove(canonicalKey + XML_ID_ATTRIBUTE);
    }
    
    /**
     * Stores whether or not an action or CC license are in this LimeXMLDocument.
     */
    private void setFields(String prefix) {
        // store action.
        action = (String)fieldToValue.get(prefix + XML_ACTION_ATTRIBUTE);

        // deal with updating license_type based on the license
        String license = (String)fieldToValue.get(prefix + XML_LICENSE_ATTRIBUTE);
        String type = (String)fieldToValue.get(prefix + XML_LICENSE_TYPE_ATTRIBUTE);
        
        if(LOG.isDebugEnabled())
            LOG.debug("type: " + type);
        
        // Do specific stuff on licenseType for various licenses.
        // CC licenses require that the 'license' field has the CC_URI_PREFIX & CC_URL_INDICATOR
        // somewhere.  Weed licenses require that the 'license type' field has WeedInfo.LINFO,
        // a content id & a version id.
        licenseType = LicenseConstants.determineLicenseType(license, type);
        if (licenseType == LicenseConstants.CC_LICENSE)
            fieldToValue.put(prefix + XML_LICENSE_TYPE_ATTRIBUTE, CCConstants.CC_URI_PREFIX);

        if(LOG.isDebugEnabled())
            LOG.debug("Fields after setting: " + fieldToValue);
    }
    
    /**
     * Derives a canonicalKey from a collection of Map.Entry's.
     */
    private String getCanonicalKey(Collection entries) {
        if(entries.isEmpty())
            return null;
        Map.Entry firstEntry = (Map.Entry)entries.iterator().next();
        String firstKey = (String)firstEntry.getKey();
        
        // The canonicalKey is always going to be x__x__<other stuff here>
        int idx = firstKey.indexOf(XMLStringUtils.DELIMITER);
        idx = firstKey.indexOf(XMLStringUtils.DELIMITER, idx+1);
        // not two delimiters? can't find the canonicalKey
        if(idx == -1)
            return null;
            
        // 2 == XMLStringUtils.DELIMITER.length()
        return firstKey.substring(0, idx + 2);
    }
}

