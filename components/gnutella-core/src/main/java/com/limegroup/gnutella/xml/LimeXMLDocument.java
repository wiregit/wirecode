package com.limegroup.gnutella.xml;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.metadata.MP3MetaData;
import com.limegroup.gnutella.util.NameValue;

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
    public static final String XML_HEADER = "<?xml version=\"1.0\"?>";

	/**
	 * Cached hash code for this instance.
	 */
	private volatile transient int hashCode = 0;

    /** For backwards compatibility with downloads.dat. */
    private static final long serialVersionUID = 7396170507085078485L;

    //TODO2: Need to build in the ability to work with multiple instances
    //of some fields. 
    
    private Map fieldToValue = new HashMap();
    private String schemaUri;
    private String xmlString;//this is what is sent back on the wire.

    /** 
     * The file this is related to.  Can be null if pure meta-data.
     */
    private transient File fileId;
    
    /**
     * The action that this doc has.
     */
    private transient String action;
    
    /**
     * Indicator that the LimeXMLDocument was created after
     * LimeWire began to understand id3v2 data.
     * Older LimeXMLDocuments are deserialized with this as false.
     *
     * MUST NOT BE FINAL, or else readObject won't mark it as false.
     */
    private boolean supportsID3v2 = true;
    public boolean supportsID3v2() { return supportsID3v2; }
    
    /**
     * Cached list of keywords.  Because keywords are only filled up
     * upon construction, they can be cached upon retrieval.
     */
    private transient List CACHED_KEYWORDS = null;

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
        this.action = (String)fieldToValue.get(result.canonicalKeyPrefix + XML_ACTION_ATTRIBUTE);
        // if we were able to remove the identifier, regen the XML string.
        if(fieldToValue.remove(result.canonicalKeyPrefix + XML_ID_ATTRIBUTE) != null)
            getXMLString();
        else
            this.xmlString = xml.trim();

        if(xmlString.equals(""))
            throw new SAXException("empty after identifier ripped");
    }

    /**
     * Constructs a new LimeXMLDocument
     * @param map Map with keys in canonicalized
     * form and corresponding values that will be used to create the 
     * new instance
     * @param schemaURI The schema URI for the LimeXMLDocument to be
     * created
     */    
    LimeXMLDocument(Map map, String schemaURI, String keyPrefix) {
        if(map.isEmpty())
            throw new IllegalArgumentException("empty map");

        this.schemaUri = schemaURI;
        this.fieldToValue = map;
        this.action = (String)fieldToValue.get(keyPrefix + XML_ACTION_ATTRIBUTE);
        fieldToValue.remove(keyPrefix + XML_ID_ATTRIBUTE); // remove id.
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
        //set the schema URI
        this.schemaUri = schemaURI;
        if(nameValueList.isEmpty())
            throw new IllegalArgumentException("empty list");
                
        //iterate over the passed list of fieldnames & values
        for(Iterator i = nameValueList.iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            String name = (String)next.getKey();
            Object value = next.getValue();
            fieldToValue.put(name.trim(), value);
        }
        
        // scan for action/id/etc..
        scanFields();        
        
        // Verify that the data read from the collection had valid info
        // for this schema.
        try {
            if(getXMLString().equals(""))
                throw new IllegalArgumentException("invalid collection data.");
        } catch(SchemaNotFoundException snfe) {
            throw new IllegalArgumentException(snfe.getMessage());
        }
    }
    
    /**
     * Determines whether or not this LimeXMLDocument is valid.
     */
    boolean isValid() {
        try {
            return !getXMLString().equals("") && !fieldToValue.isEmpty();
        } catch(SchemaNotFoundException snfe) {
            return false;
        }
    }

    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // we may want to do special stuff in the future....
        in.defaultReadObject();
        scanFields();
        
        // make sure any spaces are removed.
        if(xmlString != null)
            xmlString = xmlString.trim();
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
     */
    public List getKeyWords() {
        if( CACHED_KEYWORDS != null )
            return CACHED_KEYWORDS;

        List retList = new ArrayList();
        Iterator iter = fieldToValue.values().iterator();
        while(iter.hasNext()){
            boolean number = true;//reset
            String val = (String)iter.next();
            try{
                new Double(val); // will trigger NFE.
            }catch(NumberFormatException e){
                number = false;
            }
            if(!number && (val != null) && (!val.equals("")))
                retList.add(val);
        }
        CACHED_KEYWORDS = retList;
        return retList;
    }

    /**
     * Returns the unique identifier which identifies the schema this XML
     * document conforms to
     */
    public String getSchemaURI(){
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

    /** This method is only guaranteed to work if getSchemaURI() returns a 
     *  non-null value.
     *  @return a List <NameValue>, where each name-value corresponds to a 
     *  canonicalized field name (placeholder), and its corresponding value in
     *  the XML Document.  This list is ORDERED according to the schema URI of
     *  this document.  
     *  @exception SchemaNotFoundException Thrown if you called this without 
     *  providing a valid XML schema.  So please make sure your schema is ok.
     */
    public List getOrderedNameValueList() throws SchemaNotFoundException {
        List retList = new LinkedList();
        
        if( schemaUri == null )
            throw new SchemaNotFoundException("no schema given.");
            
        LimeXMLSchema schema =
            LimeXMLSchemaRepository.instance().getSchema(schemaUri);
        
        if( schema == null )
            throw new SchemaNotFoundException("invalid schema: " + schemaUri);

        String[] fNames = schema.getCanonicalizedFieldNames();
        for (int i = 0; i < fNames.length; i++) {
            Object retObj = fieldToValue.get(fNames[i].trim());
            if (retObj != null)
                retList.add(new NameValue(fNames[i].trim(),
                                          retObj));
        }
            
        return retList;
    }
    
    /**
     * A faster version of getValue, does no trimming or comparison.
     */
    public String getValueFast(final String field) {
        return (String)fieldToValue.get(field);
    }

    public String getValue(String fieldName) {
        String retValue = null;
        fieldName = fieldName.trim();
        retValue = (String)fieldToValue.get(fieldName);
        return retValue;
    }
    

    /**
     * @return the XML to be sent on the wire.
     * @exception SchemaNotFoundException if the schema is invalid.
     */
    public String getXMLString() throws SchemaNotFoundException {        
        // generate the XML
        if (xmlString == null || xmlString.equals(""))
            xmlString = constructXML(getOrderedNameValueList(),schemaUri).trim();

        return xmlString;
    }

    /**
     * finds the structure of the document by looking at the names of 
     * the keys in the NameValue List and creates an XML string.
     * <p>
     * The name value list must have the correct ordering. 
     * <p>
     * The values are converted into the correect encoding as per the 
     * XML specifications. So the caller of this method need not 
     * pre-encode the special XML characters into the values. 
     */
    private String constructXML(List namValList, String uri){
        if (namValList.size() == 0)
            return "";

        //encode the URI
        //uri = LimeXMLUtils.encodeXML(uri);
        
        StringBuffer xml = new StringBuffer();
        // add the initial XML header.
        xml.append(XML_HEADER);
        
        String canonicalKey = getCanonicalKey(namValList);
        List split = XMLStringUtils.split(canonicalKey);
        if(split.size() != 2)
            return "";
        String root = (String)split.get(0);
        String type = (String)split.get(1);
            
        // Construct: '<things><thing '
        xml.append("<");
        xml.append(root);
        xml.append(">");
        xml.append("<");
        xml.append(type);

        for(Iterator i = namValList.iterator(); i.hasNext(); ) {
            NameValue next = (NameValue)i.next();
            String name = getLastField(canonicalKey, next.getName());
            if(name == null)
                continue;
            // Construct: ' attribute="value"'
            xml.append(" ");
            xml.append(name);
            xml.append("=\"");
            xml.append(LimeXMLUtils.encodeXML((String)next.getValue()));
            xml.append("\"");
        }
        
        // Construct: ' /></things>'
        xml.append(" />");
        xml.append("</");
        xml.append(root);
        xml.append(">");
        
        return xml.toString();
    }

    private static int getCommonCount(List currFields, List prevFields){
        int retValue =0;
        int smaller;
        if (currFields.size() < prevFields.size())
            smaller = currFields.size();
        else 
            smaller = prevFields.size();

        for(int i=0; i<smaller; i++){
            if(currFields.get(i).equals(prevFields.get(i)))
                retValue++;
            else//stop counting and get outta here
                break;
        }
        return retValue;
    }

	/**
	 * Overrides equals to check for equality of all xml document fields.
	 *
	 * @param o the object to compare
	 * @return <tt>true</tt> if the objects are equal, <tt>false</tt>
	 *  otherwise
	 */
	public boolean equals(Object o) {
		if(o == this) return true;
		if(o == null) return false;
		if(!(o instanceof LimeXMLDocument)) return false;
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
	    try {
	        return getXMLString();
	    } catch(SchemaNotFoundException snfe) {
	        return "no schema.";
        }
    }
    
    /**
     * Looks in the fields for the ACTION, IDENTIFIER, and INDEX.
     * Action is stored, index & identifier are removed.
     */
    private void scanFields() {
        String canonicalKey = getCanonicalKey(getNameValueSet());
        if(canonicalKey == null)
            return;

        action = (String)fieldToValue.get(canonicalKey + XML_ACTION_ATTRIBUTE);
        boolean removed = false;
        removed |= fieldToValue.remove(canonicalKey + XML_INDEX_ATTRIBUTE) != null;
        removed |= fieldToValue.remove(canonicalKey + XML_ID_ATTRIBUTE) != null;
        
        // regenerate the XML string.
        if(xmlString != null && removed) {
            if(LOG.isDebugEnabled())
                LOG.debug("Reconstructing XML, old: " + xmlString);
            xmlString = null;
            try {
                getXMLString();
                if(LOG.isDebugEnabled())
                    LOG.debug("Reconstructed XML, new: " + xmlString);
            } catch(SchemaNotFoundException ignored) {
                LOG.warn("No schema!", ignored);
            }
        }   
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
    
    /**
     * Derives the last field name from a given name.
     * With input "things__thing__field__", this will return "field".
     */
    private String getLastField(String canonicalKey, String full) {
        //      things__thing__field__
        //      ^                   ^
        //     idx                 idx2
        
        int idx = full.indexOf(canonicalKey);
        if(idx == -1 || idx != 0)
            return null;
            
        int length = canonicalKey.length();
        int idx2 = full.indexOf(XMLStringUtils.DELIMITER, length);
        if(idx2 == -1)
            return null;
        
        return full.substring(length, idx2);
    }
}

