package com.limegroup.gnutella.xml;

import java.util.*;
import java.io.*;
import com.limegroup.gnutella.util.NameValue;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.*;
import org.w3c.dom.*;


/**
 * @author  Sumeet Thadani
 * A LimeXMLDocument is basically a hashmap that maps a
 * Names of fields to the values as per a XML document.
 */
public class LimeXMLDocument{

    //TODO2: Need to build in the ability to work with multiple instances
    //of some fields. 
    
    protected Map fieldToValue;
    protected String schemaUri;
    //protected String XMLString;//this is what is sent back on the wire.
    /** 
     * Field corresponds to the name of the file for which this
     * meta-data corresponds to. It can be null if the data is pure meta-data
     */
    protected String identifier;
    protected String action="";

    //constructor
    public LimeXMLDocument(String XMLString) throws SAXException, 
                                        SchemaNotFoundException, IOException{
        InputSource doc = new InputSource(new StringReader(XMLString));
        //this.XMLString = XMLString;
        initialize(doc);
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
        
        //initialize the field mapping map
        fieldToValue = new HashMap();
        
        //iterate over the passed list of fieldnames & values
        for(Iterator iterator = nameValueList.iterator();
            iterator.hasNext();){
            //get the next pair
            NameValue nameValue = (NameValue)iterator.next();
            
            //update the field to value map
            fieldToValue.put(nameValue.getName(), nameValue.getValue());
        }
    }
    
    private void initialize(InputSource doc) throws SchemaNotFoundException,
                            IOException, SAXException {
        DOMParser parser = new DOMParser();
        //TODO1: make sure that the schema actually validates documents
        //documentBuilderFactory.setValidating(true);
        //documentBuilderFactory.setNamespaceAware(true);
        Document document = null;
        parser.parse(doc);
        document=parser.getDocument();
        grabDocInfo(document);
        createMap(document);
    }

    private void grabDocInfo(Document doc) throws SchemaNotFoundException{
        Element docElement = doc.getDocumentElement();
        List attributes=LimeXMLUtils.getAttributes(docElement.getAttributes());
        int size = attributes.size();
        for(int i=0; i< size; i++){
            Node att = (Node)attributes.get(i);
            String attName = att.getNodeName();
            String lowerAttName = attName.toLowerCase();
            if (lowerAttName.indexOf("schemalocation") >= 0)
                schemaUri = att.getNodeValue();
            else if (lowerAttName.indexOf("identifier") >= 0)
                identifier = att.getNodeValue();
            else if (lowerAttName.indexOf("action") >= 0)
                action = att.getNodeValue();
        }
        if(schemaUri == null)//we cannot have a doc with out a schema
            throw new SchemaNotFoundException();
        //Note: However if the identifier is null it just implies that
        // the meta data is not associated with any file!
        //Similarly, if the action is null, it just means there is no
        //action associated with this Document. 
    }

    private void createMap(Document doc) {
        fieldToValue = new HashMap();
        Element docElement = doc.getDocumentElement();
        doAllChildren(docElement,"");
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
        nodeValue = nodeValue.trim();
        if (nodeValue != null && !nodeValue.equals(""))
            fieldToValue.put(currTag, nodeValue);
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
                fieldToValue.put(attString,attValue);
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

    public String getValue(String fieldName){
        String value = (String)fieldToValue.get(fieldName);
        return value;
    }
    
    /**
     * Returns an XML string that will be re-created as this document 
     * when it is re-assembled in another machine. 
     */

    public String getXMLString() {        
        //return XMLString;
        String ret = constructXML(getNameValueList(),schemaUri);
        return ret;
    }
    
    public String getXMLStringWithIdentifier(){
        String ret = constructXML(getNameValueList(),schemaUri);
        //Insert the identifier name in the xmlString
        int index = ret.indexOf(">");//end of the header string
        if (index < 0)
            return ret;  // do not insert anything if not valid xml
        index = ret.indexOf(">",++index);//index of end of root element
        String first = ret.substring(0,index);
        String last = ret.substring(index);
        String middle = " identifier=\""+identifier+"\"";
        ret = first+middle+last;
        return ret;
    }

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
    }
    
    public static String constructXML(List namValList, String uri){
        //OK so we have a list of all the populated fields. In correct order
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
            String value = (String)namevalue.getValue();
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
        first = "<?xml version=\"1.0\"?>"+first;
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
    
}


