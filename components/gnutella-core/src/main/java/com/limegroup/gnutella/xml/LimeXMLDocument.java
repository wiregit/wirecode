package com.limegroup.gnutella.xml;

import com.sun.java.util.collections.*;
import java.util.StringTokenizer;
import java.util.Locale;
import java.io.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.URN;
import org.apache.xerces.parsers.DOMParser;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.mp3.ID3Reader;
import org.xml.sax.*;
import org.w3c.dom.*;


/**
 * @author  Sumeet Thadani
 * A LimeXMLDocument is basically a hashmap that maps a
 * Names of fields to the values as per a XML document.
 */
public class LimeXMLDocument implements Serializable {

    public static final String XML_ID_ATTRIBUTE_STRING = "identifier";
    public static final String XML_HEADER = "<?xml version=\"1.0\"?>";

    /** The parser to be used for all XML Docs.  No use carrying one per
     *  instance.  Instances should synchronize on use.
     */
    private static DOMParser parser = new DOMParser();

	/**
	 * Cached hash code for this instance.
	 */
	private volatile transient int hashCode = 0;

    /** For backwards compatibility with downloads.dat. */
    static final long serialVersionUID = 7396170507085078485L;

    //TODO2: Need to build in the ability to work with multiple instances
    //of some fields. 
    
    private final Map fieldToValue = new TreeMap(new StringComparator());
    private String schemaUri;
    private String xmlString;//this is what is sent back on the wire.
    /** 
     * Field corresponds to the name of the file for which this
     * meta-data corresponds to. It can be null if the data is pure meta-data
     */
    private String identifier;
    private String action="";
    
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
        this.xmlString = ripIdentifier(XMLStr);
    }
    
    
    public LimeXMLDocument(Node node, Node rootElement) throws 
    SchemaNotFoundException, IOException {
        grabDocInfo(rootElement,true);
        grabDocInfo(node,false);
        createMap(node,rootElement.getNodeName());
    }

    /**
     * Constructs a new LimeXMLDocument
     * @param nameValueList List (of NameValue) of fieldnames (in canonicalized
     * form) and corresponding values that will be used to create the 
     * new instance
     * @param schemaURI The schema URI for the LimeXMLDocument to be
     * created
     */
    public LimeXMLDocument(List nameValueList, String schemaURI){
       
        //set the schema URI
        this.schemaUri = schemaURI;
                
        //iterate over the passed list of fieldnames & values
        for(Iterator iterator = nameValueList.iterator();
            iterator.hasNext();){
            //get the next pair
            NameValue nameValue = (NameValue)iterator.next();
            
            //update the field to value map
            fieldToValue.put(nameValue.getName().trim(), nameValue.getValue());
        }
    }
    

    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // we may want to do special stuff in the future....
        in.defaultReadObject();
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
        //TODO1: make sure that the schema actually validates documents
        //documentBuilderFactory.setValidating(true);
        //documentBuilderFactory.setNamespaceAware(true);
        Document document = null;
        synchronized(this.parser) {
            parser.parse(doc);
            document=parser.getDocument();
        }
        Element docElement = document.getDocumentElement();
        grabDocInfo(docElement,true);
        Node child=docElement.getFirstChild();
        grabDocInfo(child,false);//not root
        createMap(docElement);
    }

    /**
     * There are two possible places for important document information 
     * may be stored. 
     * 1. it may be in the root element. In which case the root variable
     * should be set as true. In this case all the important doc info will
     * be remembered, and any non-doc info present as attributes will
     * be put into the hashmap
     * <p>
     * The other possible place is at the (only) child of the root element.
     * If this is the node that is being passed. then root variable should
     * be set to false. In this case. Just the doc info is grabbed. The
     * non-doc info of a non-doc Node is not even looked at in this method.
     * <p> 
     * For this method to be effective, keep the following points in mind.
     * The doc info mey be either in the root alone or in both the root and
     * the only child of root. If it is only in root - call this method on 
     * root, and create map on root.
     * <p>
     * If the doc-info is spread out b/w the root and the child. First
     * call this method with root, then call it with child, and then 
     * call createMap wht root. This will ensure no loss of data
     */
    private void grabDocInfo(Node docElement,boolean root)
        throws SchemaNotFoundException, IOException {
                        
        //Element docElement = doc.getDocumentElement();
        if(docElement==null)
            throw new IOException("null doc element");
        NamedNodeMap nnm = docElement.getAttributes();
        if( nnm == null )
            throw new IOException("invalid doc element -- null namednodemap");

        List attributes=LimeXMLUtils.getAttributes(nnm);
        int size = attributes.size();
        for(int i=0; i< size; i++){
            Node att = (Node)attributes.get(i);
            String attName = att.getNodeName();
            String lowerAttName = attName.toLowerCase(Locale.US);
            if (lowerAttName.indexOf("schemalocation") >= 0)
                schemaUri = att.getNodeValue();
            else if (lowerAttName.indexOf(XML_ID_ATTRIBUTE_STRING) >= 0) {
                identifier = att.getNodeValue();
                //This indentifier corresponds to the ED of the response in
                //the system. Remove this attribute from the node. We are not 
                //interested in it anymore
                Element e = (Element)docElement;
                e.removeAttribute(attName);
            }
            else if (lowerAttName.indexOf("action") >= 0)
                action = att.getNodeValue();
            else if(lowerAttName.indexOf("index") >= 0){
                //This index corresponds to the index of the response in
                //the QR. Remove this attribute from the node. We are not 
                //interested in it anymore
                Element e = (Element)docElement;
                e.removeAttribute(attName);
            }
            else{//these are attributes that have a value
                if(root){
                    String canonicalizedAttName= docElement.getNodeName()+
                    XMLStringUtils.DELIMITER+att.getNodeName()+
                    XMLStringUtils.DELIMITER;                
                    fieldToValue.put(canonicalizedAttName.trim(),
                                     att.getNodeValue().trim());
                }
            }
        }
        if(schemaUri == null)//we cannot have a doc with out a schema
            throw new SchemaNotFoundException();
        //Note: However if the identifier is null it just implies that
        // the meta data is not associated with any file!
        //Similarly, if the action is null, it just means there is no
        //action associated with this Document. 
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

    private void createMap(Node docElement) {
        //Element docElement = doc.getDocumentElement();
        doAllChildren(docElement,"");
    }

    private void createMap(Node docElement, String parent){
        doAllChildren(docElement,parent);
    }

    private void doAllChildren (Node currNode, String parentName){
        if (!currNode.hasChildNodes()){ //base case
            doEntry(currNode, parentName);
            return;
        }
        String currString = doEntry(currNode,parentName);
        List children = LimeXMLUtils.getElements(currNode.getChildNodes());
        int size = children.size();
        for(int i=0; i< size; i++){            
            Node child = (Node)children.get(i);
            doAllChildren(child,currString);
        }
    }

    private String doEntry(Node currNode, String parentName){
        String currTag;
        if(!parentName.equals(""))
            currTag=parentName+XMLStringUtils.DELIMITER+currNode.getNodeName();
        else
            currTag = currNode.getNodeName();

        //if (currNode.getNodeType() == Node.CDATA_SECTION_NODE)
        //  System.out.println("this node has type  "+ currNode.getNodeType());

        Element currElement = (Element)currNode;
        String nodeValue = LimeXMLUtils.getText(currElement.getChildNodes());
        if (nodeValue != null && !nodeValue.equals(""))
            fieldToValue.put(currTag.trim(), nodeValue.trim());
        //add the attributes only if its not the first level
        if(!parentName.equals("")){
            List attribs=LimeXMLUtils.getAttributes(currNode.getAttributes());
            int size = attribs.size();
            for(int i=0; i< size; i++){
                Node att = (Node)attribs.get(i);
                String attName = att.getNodeName();
                String attString=currTag+XMLStringUtils.DELIMITER
                         +attName+XMLStringUtils.DELIMITER;
                String attValue = att.getNodeValue();
                fieldToValue.put(attString.trim(),
                                 attValue.trim());
            }
        }
        return currTag;
    }

    /**
     * Returns the unique identifier which identifies the schema this XML
     * document conforms to
     */
    public String getSchemaURI(){
        return schemaUri;
    }    
    
    /**
     * Returns the name of the file that the data in this XML document 
     * corresponds to. If the meta-data does not correspond to any file
     * in the file system, this method will rerurn a null.
     */
    public String getIdentifier(){
        return identifier;
    }
    
    public String getAction(){
        return action;
    }


    /**
     * Returns a List <NameValue>, where each name-value corresponds to a
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
     * @return a List <NameValue>, where each name-value corresponds to a
     * canonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     */
    public List getNameValueList() {
        int size = fieldToValue.size();
        Iterator keys = fieldToValue.keySet().iterator();
        List retList = new ArrayList();
        for(int i=0; i< size; i++){
            String name = (String)keys.next();
            String value  = (String)fieldToValue.get(name);
            NameValue namVal = new NameValue(name,value);
            retList.add(namVal);
        }
        return retList;
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

        if (schemaUri != null) {
            LimeXMLSchemaRepository schemaDB = 
            LimeXMLSchemaRepository.instance();
            LimeXMLSchema schema = schemaDB.getSchema(schemaUri);

            if (schema != null) {
                String[] fNames = schema.getCanonicalizedFieldNames();        
                
                for (int i = 0; i < fNames.length; i++) {
                    Object retObj = fieldToValue.get(fNames[i].trim());
                    if (retObj != null)
                        retList.add(new NameValue(fNames[i].trim(),
                                                  retObj));
                }
            }
            else
                throw new SchemaNotFoundException();
        }
        else
            throw new SchemaNotFoundException();
            
        return retList;
    }


    public String getValue(String fieldName){
        String retValue = null;
        fieldName = fieldName.trim();
        retValue = (String)fieldToValue.get(fieldName);
        /** The following HACK is necessitated by my omission of
         *  getGenreString() in the new ID3Editor.readDocument() method.
         *  That method has been fixed, but certain clients still have issues.
         *  So we need to turn any numbers into the appropriate genre...
         */
        if (fieldName.equals("audios__audio__genre__") && (retValue != null)) {
            try {
                short index = Short.parseShort(retValue);
                retValue = ID3Reader.getGenreString(index);
                fieldToValue.put(fieldName, retValue);
            }
            catch (NumberFormatException ignored) {
                // the string is fine, it is a valid genre...
            }
        }
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
        if (xmlString == null) {
            // derive xml...
            xmlString = constructXML(getOrderedNameValueList(),schemaUri);
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


    /*
    //Unit Tester    
    public static void main(String args[]){
        //File f = new File("C:/down/xerces-1_3_1/data","personal-schema.xml");
        
        Runtime rt = Runtime.getRuntime();
        long mem = rt.totalMemory()- rt.freeMemory();
        System.out.println("Sumeet : Used memory is "+mem);
        File f = new File("C:/home/etc/xml","junk.xml");
        LimeXMLDocument l = null;
        try{            
            String buffer = "";
            String xmlStruct = "";
            BufferedReader br = new BufferedReader(new FileReader(f));
            while(buffer!=null){
                buffer=br.readLine();
                if (buffer!=null)
                    xmlStruct = xmlStruct+buffer;
                xmlStruct = xmlStruct.trim();
            }
            l = new LimeXMLDocument(xmlStruct);
        }catch(Exception e){
            e.printStackTrace();
        }
        List list = l.getNameValueList();
        int size = list.size();
        for (int i =0; i< size; i++){
            NameValue a = (NameValue)list.get(i);
            String name = a.getName();
            String value = (String)a.getValue();
            System.out.println("Sumeet : name "+name);
            System.out.println("Sumeet : value "+value);
       }
        ripIDTest();
        debugTest1();
    }
    
    public static void debugTest1() {
        String s="<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio type=\"song\" album=\" \" genre=\" \"/></audios>";
        try {
            LimeXMLDocument d = new LimeXMLDocument(s);
            System.out.println("Document d = "+d);
            String v = d.getValue("audios__audio__type__");
            System.out.println("Type val = |"+v+"|");
            v = d.getValue("audios__audio__album__");
            System.out.println("Album val = |"+v+"|");
            v = d.getValue("audios__audio__genre__");
            System.out.println("Genre val = |"+v+"|");
        } catch (Exception e ) {
            e.printStackTrace();
        }
    }
    
    static void ripIDTest() {
    final String xml = "<?xml version=\"1.0\"?><backslash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdotNews.xsd\"><story identifier=\"robbie hranac\"><image>J. Lo</image><title>Oops, I did it Again!</title></story></backslash>";
    
    Assert.that(ripIdentifier(xml).equals("<?xml version=\"1.0\"?><backslash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdotNews.xsd\"><story ><image>J. Lo</image><title>Oops, I did it Again!</title></story></backslash>"));
    }
    
    */

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
        //encode the URI
        uri = LimeXMLUtils.encodeXML(uri);
        int size = namValList.size();
        String first="";
        String last = "";
        String prevString = "";
        ArrayList tagsToClose = new ArrayList();
        boolean prevAtt=false;
        boolean rootAtts;//if there are root attributes besides identifier,URI
        if (namValList.size() == 0)
            return "";
        NameValue nv = (NameValue)namValList.get(0);
        String n = nv.getName();
        //if this string contains 2 sets of __ and the second set it at the 
        //end then we know it that the root has attributes.
        boolean end = n.endsWith(XMLStringUtils.DELIMITER);
        StringTokenizer tok = new StringTokenizer(n,XMLStringUtils.DELIMITER);
        int c = tok.countTokens();
        //System.out.println("Sumeet: "+n+","+c);
        if(end && c==2)
            rootAtts = true;
        else 
            rootAtts = false;
        for(int i=0; i< size; i++){
            NameValue namevalue = (NameValue)namValList.get(i);
            String currString = namevalue.getName();
            String value=LimeXMLUtils.encodeXML((String)namevalue.getValue());
            List currFields = XMLStringUtils.split(currString);
            int commonCount = 0;
            List prevFields = null;            
            boolean attribute = false;            
            if (currString.endsWith(XMLStringUtils.DELIMITER))
                attribute = true;
            if(prevAtt && !attribute)//previous was attribute and this is not
                first = first+">";
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
                    last = last + "</"+closeStr+">";
                }
            }
            if(!last.equals("")){
                first = first + last;
                last = "";
            }
            //deal with parents
            for(int j=commonCount; j<z-1; j++){
                String str = (String)currFields.get(j);
                first = first+"<"+str;
                if(i==0 && j==0 && !rootAtts){
                    first=first+" xsi:noNamespaceSchemaLocation=\""+uri+"\">";
                }
                else if(i==0 && j==0 && rootAtts){
                    first=first+" xsi:noNamespaceSchemaLocation=\""+uri+"\"";
                }
                if( (!attribute) && ( j>0 || i > 0))
                    first = first+">";
                tagsToClose.add(str);
            }
            String curr=(String)currFields.get(z-1);//get last=current one
            if(!attribute)
                first = first + "<"+curr+">"+value+"</"+curr+">";
            else{
                first = first+" "+curr+"=\""+value+"\""; 
                if(i==size-1)
                    first= first+">";
            }
            prevString = currString;
            prevAtt = attribute;                
        }
        //close remaining tags
        int stillPending = tagsToClose.size();
        for(int l=stillPending-1;l>=0;l--){
            String tag = (String)tagsToClose.remove(l);
            first = first + "</"+tag+">";
        }
        first = XML_HEADER+first;
        return first;
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

