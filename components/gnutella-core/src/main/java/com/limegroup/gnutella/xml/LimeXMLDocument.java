package com.limegroup.gnutella.xml;

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


/**
 * @author  Sumeet Thadani
 * A LimeXMLDocument is basically a hashmap that maps a
 * Names of fields to the values as per a XML document.
 */
public class LimeXMLDocument implements Serializable {

    public static final String XML_ID_ATTRIBUTE_STRING = "identifier";
    public static final String XML_HEADER = "<?xml version=\"1.0\"?>";

	/**
	 * Cached hash code for this instance.
	 */
	private volatile transient int hashCode = 0;

    /** For backwards compatibility with downloads.dat. */
    static final long serialVersionUID = 7396170507085078485L;

    //TODO2: Need to build in the ability to work with multiple instances
    //of some fields. 
    
    private Map fieldToValue = new HashMap();
    private String schemaUri;
    private String xmlString;//this is what is sent back on the wire.
    /** 
     * Field corresponds to the name of the file for which this
     * meta-data corresponds to. It can be null if the data is pure meta-data
     */
    private String identifier;
    private String action="";
    
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

    public void setIdentifier(String id) {
        identifier = id;
    }
    
    /**
     * This method is used by Lime Peer Server
     */
    public void setSchemaURI(String uri) {
        schemaUri = uri;
    }

    //constructor
    public LimeXMLDocument(String XMLStr) throws SAXException, 
                                        SchemaNotFoundException, IOException{
        if(XMLStr==null || XMLStr.equals(""))
            throw new SAXException("null or empty string");
        InputSource doc = new InputSource(new StringReader(XMLStr));
        initialize(doc);
        this.xmlString = ripIdentifier(XMLStr.trim());
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
    LimeXMLDocument(Map map, String schemaURI){
        this.schemaUri = schemaURI;
        if(map.keySet().isEmpty()) throw new IllegalArgumentException("empty map");
        fieldToValue = map;
    }

    /**
     * Constructs a new LimeXMLDocument
     * @param nameValueList List (of NameValue) of fieldnames (in canonicalized
     * form) and corresponding values that will be used to create the 
     * new instance
     * @param schemaURI The schema URI for the LimeXMLDocument to be
     * created
     */
    public LimeXMLDocument(Collection nameValueList, String schemaURI){
       
        //set the schema URI
        this.schemaUri = schemaURI;
        if(nameValueList.isEmpty())
            throw new IllegalArgumentException("empty list");
                
        //iterate over the passed list of fieldnames & values
        for(Iterator i = nameValueList.iterator(); i.hasNext(); ) {
            String name;
            Object value;
            Object next = i.next();
            if( next instanceof NameValue ) {
                name = ((NameValue)next).getName();
                value = ((NameValue)next).getValue();
            } else if( next instanceof Map.Entry ) {
                name = (String)((Map.Entry)next).getKey();
                value = ((Map.Entry)next).getValue();
            } else {
                throw new IllegalArgumentException("Invalid Collection");
            }
            //update the field to value map
            fieldToValue.put(name.trim(), value);
        }
        
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
        // make sure any spaces are removed.
        if(xmlString != null)
            xmlString = xmlString.trim();
        
        if (identifier!=null && fieldToValue!=null &&
        		LimeXMLUtils.isMP3File(identifier)) {
        	
        	String genre = (String) fieldToValue.get("audios__audio__genre__");
        	if (genre!=null)
        		try {
        			short index = Short.parseShort(genre);
        			genre = MP3MetaData.getGenreString(index);
        			fieldToValue.put("audios__audio__genre__", genre);
        		}
        		catch (NumberFormatException ignored) {
        			// the string is fine, it is a valid genre...
        		}
        }
    }
 

    /** expunges the 'identifier' tag from the xml string, if present....
     */
    private static String ripIdentifier(String xmlWithID) {
        String retString = xmlWithID;

        int indexOfID = xmlWithID.indexOf(XML_ID_ATTRIBUTE_STRING);
        if (indexOfID > -1) {
            final String quote = "\"";
            int indexOfEndQuote = xmlWithID.indexOf(quote, indexOfID+1);
            indexOfEndQuote = xmlWithID.indexOf(quote, indexOfEndQuote+1);
            String begin = xmlWithID.substring(0, indexOfID);
            String end = xmlWithID.substring(indexOfEndQuote+1);
            retString = begin + end;
        }
        if (retString.indexOf(XML_HEADER) < 0)
            retString = XML_HEADER + retString;
        return retString;
    }
    

    private void initialize(InputSource doc) throws SchemaNotFoundException,
                            IOException, SAXException {
        
        XMLParsingUtils.ParseResult result = XMLParsingUtils.parse(doc);
        
        if (result.canonicalAttributeMaps.isEmpty())
            throw new IOException("No element present");
        
        fieldToValue = (Map)result.canonicalAttributeMaps.get(0);
        schemaUri = result.schemaURI;
    }

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
     * 
     * TODO: this should return a File object.
     */
    public String getIdentifier(){
        return identifier;
    }
    
    public String getAction(){
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


    public String getValue(String fieldName){
        String retValue = null;
        fieldName = fieldName.trim();
        retValue = (String)fieldToValue.get(fieldName);
        return retValue;
    }
    

    /**
     * @return an XML string that will be re-created as this document 
     * when it is re-assembled in another machine. 
     * @exception SchemaNotFoundException DO NOT CALL THIS METHOD unless
     * you know that getSchemaURI() returns a valid xml schema.  Set it 
     * yourself with setSchemaURI().
     */
    public String getXMLString() throws SchemaNotFoundException {        
        if (xmlString == null || xmlString.equals("")) {
            // derive xml...
            xmlString = constructXML(getOrderedNameValueList(),schemaUri);
            xmlString = xmlString.trim();
        }
        return xmlString;
    }
    


    /**
     * @return an XML string that will be re-created as this document 
     * when it is re-assembled in another machine. this xml string contains
     * the filename identifier too (as an attribute).
     * @exception SchemaNotFoundException DO NOT CALL THIS METHOD unless
     * you know that getSchemaURI() returns a valid xml schema.  Set it 
     * yourself with setSchemaURI().
     */
    public String getXMLStringWithIdentifier() throws SchemaNotFoundException {
        String ret = getXMLString();
        //Insert the identifier name in the xmlString
        int index = ret.indexOf(">");//end of the header string
        if (index < 0)
            return ret;  // do not insert anything if not valid xml
        index = ret.indexOf(">",++index);//index of end of root element
        index = ret.indexOf(">",++index);//end of only child (plural form)
        String first = ret.substring(0,index);
        String last = ret.substring(index);
        String middle = " identifier=\""+identifier+"\"";
        ret = first+middle+last;
        return ret;
    }


    
    //Unit Tester    
//     public static void main(String args[]){
//         ripIDTest();
//         debugTest1();
//     }
    
//     public static void debugTest1() {
//         String s="<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio type=\"song\" album=\"hey\" genre=\"you\"/></audios>";
//         try {
//             LimeXMLDocument d = new LimeXMLDocument(s);
//             System.out.println("Document d = "+d);
//             String v = d.getValue("audios__audio__type__");
//             Assert.that(v.equals("song"));
//             v = d.getValue("audios__audio__album__");
//             Assert.that(v.equals("hey"));
//             v = d.getValue("audios__audio__genre__");
//             Assert.that(v.equals("you"));

//         } catch (Exception e ) {
//             e.printStackTrace();
//         }
//     }
    
//     static void ripIDTest() {
//     final String xml = "<?xml version=\"1.0\"?><backslash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdotNews.xsd\"><story identifier=\"robbie hranac\"><image>J. Lo</image><title>Oops, I did it Again!</title></story></backslash>";
    
//     Assert.that(ripIdentifier(xml).equals("<?xml version=\"1.0\"?><backslash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdotNews.xsd\"><story ><image>J. Lo</image><title>Oops, I did it Again!</title></story></backslash>"));
//     }
    

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
    public static String constructXML(List namValList, String uri){
        if (namValList.size() == 0)
            return "";

        //encode the URI
        uri = LimeXMLUtils.encodeXML(uri);
        int size = namValList.size();
        StringBuffer first = new StringBuffer();
        StringBuffer last = new StringBuffer();
        String prevString = "";
        ArrayList tagsToClose = new ArrayList();
        boolean prevAtt=false;
        boolean rootAtts;//if there are root attributes besides identifier,URI
        NameValue nv = (NameValue)namValList.get(0);
        String n = nv.getName();
        //if this string contains 2 sets of __ and the second set it at the 
        //end then we know it that the root has attributes.
        boolean end = n.endsWith(XMLStringUtils.DELIMITER);
        StringTokenizer tok = new StringTokenizer(n,XMLStringUtils.DELIMITER);
        int c = tok.countTokens();
        if(end && c==2)
            rootAtts = true;
        else 
            rootAtts = false;
        int i = 0;
        for(Iterator iter = namValList.iterator(); iter.hasNext(); i++) {
            NameValue namevalue = (NameValue)iter.next();
            String currString = namevalue.getName();
            String value=LimeXMLUtils.encodeXML((String)namevalue.getValue());
            List currFields = XMLStringUtils.split(currString);
            int commonCount = 0;
            List prevFields = null;            
            boolean attribute = false;            
            if (currString.endsWith(XMLStringUtils.DELIMITER))
                attribute = true;
            if(prevAtt && !attribute)//previous was attribute and this is not
                first.append(">");
            if (i > 0){
                prevFields = XMLStringUtils.split(prevString);
                commonCount = getCommonCount(currFields,prevFields);
            }        
            int z = currFields.size();
            //close any tags that need to be closed
            int numPending = tagsToClose.size();
            if(commonCount < numPending){//close some tags
                int closeCount = numPending-commonCount;
                int currClose = numPending-1;
                //close the last closeCount tags
                for(int k=0; k<closeCount; k++){
                    String closeStr=(String)tagsToClose.remove(currClose);
                    currClose--;
                    last.append("</" + closeStr + ">");
                }
            }
            if(last.length() != 0) {
                first.append(last);
                last.setLength(0);
            }
            //deal with parents
            for(int j = commonCount; j < z-1; j++) {
                String str = (String)currFields.get(j);
                first.append("<" + str);
                if( i == 0 && j == 0) {
                    first.append(" xsi:noNamespaceSchemaLocation=\""+uri+"\"");
                    if(!rootAtts)
                        first.append(">");
                } else if (!attribute) {
                    first.append(">");
                }
                tagsToClose.add(str);
            }
            String curr=(String)currFields.get(z-1);//get last=current one
            if(!attribute)
                first.append("<" + curr + ">" + value + "</" + curr + ">");
            else {
                first.append(" " + curr + "=\"" + value + "\"");
                if(i==size-1)
                    first.append(">");
            }
            prevString = currString;
            prevAtt = attribute;
        }

        //close remaining tags
        int stillPending = tagsToClose.size();
        for(int l = stillPending-1; l >= 0; l--) {
            String tag = (String)tagsToClose.remove(l);
            first.append("</" + tag + ">");
        }

        first.insert(0, XML_HEADER);
        return first.toString();
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
				(identifier == null ? xmlDoc.identifier == null :
				 identifier.equals(xmlDoc.identifier)) &&
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
			if(fieldToValue != null) {
				result = 37*result + fieldToValue.hashCode();
			}
			if(schemaUri != null) {
				result = 37*result + schemaUri.hashCode();
			}
			if(identifier != null) {
				result = 37*result + identifier.hashCode();
			}
			if(action != null) {
				result = 37*result + action.hashCode();
			}
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
}

