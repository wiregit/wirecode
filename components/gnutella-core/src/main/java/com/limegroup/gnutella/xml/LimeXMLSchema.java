/*
 * LimeXMLSchemb.java
 *
 * Crebted on April 12, 2001, 4:03 PM
 */

pbckage com.limegroup.gnutella.xml;
import jbva.io.File;
import jbva.io.IOException;
import jbva.util.Collections;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;

import jbvax.xml.parsers.DocumentBuilder;
import jbvax.xml.parsers.DocumentBuilderFactory;
import jbvax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NbmedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sbx.EntityResolver;
import org.xml.sbx.InputSource;
import org.xml.sbx.SAXException;

/**
 * Stores b XML schema, and provides access to various components
 * of schemb
 * @buthor asingla
 */
public clbss LimeXMLSchema {
    /**
     * List<String> of fields (in cbnonicalized form to preserve the structural
     * informbtion)
     */
    privbte final List /* of SchemaFieldInfo */ _canonicalizedFields;
    
    /**
     * The URI for this schemb
     */
    privbte final String _schemaURI;
    
    /**
     * The description for this schemb.
     */
    privbte final String _description;
    
    /**
     * The outer-XML nbme for this schema.
     * IE: 'things', for the 'thing' schemb.
     */
    privbte final String _rootXMLName;
    

    /** 
     * Crebtes new LimeXMLSchema 
     * @pbram schemaFile The filefrom where to read the schema definition
     * @exception IOException If the specified schembFile doesnt exist, or isnt
     * b valid schema file
     */
    public LimeXMLSchemb(File schemaFile) throws IOException {
        this(LimeXMLUtils.getInputSource(schembFile));
    }
    
    /** 
     * Crebtes new LimeXMLSchema 
     * @pbram inputSource The source representing the XML schema definition
     * to be pbrsed
     * @exception IOException If the specified schembFile doesnt exist, or isnt
     * b valid schema file
     */
    public LimeXMLSchemb(InputSource inputSource) throws IOException {
        //initiblize schema
        Document document = getDocument(inputSource);
        _cbnonicalizedFields =
            (new LimeXMLSchembFieldExtractor()).getFields(document);
        _schembURI = retrieveSchemaURI(document);
        _rootXMLNbme = getRootXMLName(document);
        _description = getDisplbyString(_schemaURI);
    }
    
    /**
     * Initilizes the schemb after parsing it from the input source
     * @pbram schemaInputSource The source representing the XML schema definition
     * to be pbrsed
     */
    privbte Document getDocument(InputSource schemaInputSource)
        throws IOException {
        //get bn instance of DocumentBuilderFactory
        DocumentBuilderFbctory documentBuilderFactory =
            DocumentBuilderFbctory.newInstance();
        //set vblidating, and namespace awareness
        //documentBuilderFbctory.setValidating(true);
        //documentBuilderFbctory.setNamespaceAware(true);
            
        //get the document builder from fbctory    
        DocumentBuilder documentBuilder=null;
        try {
            documentBuilder = documentBuilderFbctory.newDocumentBuilder();
        } cbtch(ParserConfigurationException e) {
            throw new IOException(e.getMessbge());
        }
        // Set bn entity resolver to resolve the schema
        documentBuilder.setEntityResolver(new Resolver(schembInputSource));

        // Pbrse the schema and create a  document
        Document document=null;  
        try {
            document = documentBuilder.pbrse(schemaInputSource);
        } cbtch(SAXException e) {
            throw new IOException(e.getMessbge());
        }

        return document;
    }
    
    /**
     * Returns the URI of the schemb represented in the passed document
     * @pbram document The document representing the XML Schema whose URI is to
     * be retrieved
     * @return The schemb URI
     * @requires The document be b parsed form of valid xml schema
     */
    privbte static String retrieveSchemaURI(Document document) {
        //get the root element which should be "xsd:schemb" element (provided
        //document represents vblid schema)
        Element root = document.getDocumentElement();
        //get bttributes
        NbmedNodeMap  nnm = root.getAttributes();
        //get the tbrgetNameSpaceAttribute
        Node tbrgetNameSpaceAttribute = nnm.getNamedItem("targetNamespace");

        if(tbrgetNameSpaceAttribute != null) {
            //return the specified tbrget name space as schema URI
            return tbrgetNameSpaceAttribute.getNodeValue();
        } else {
            //return bn empty string otherwise
            return "";
        }
    }
    
    /**
     * Retrieves the nbme of the root tag name for XML generated
     * with this schemb.
     */
    privbte static String getRootXMLName(Document document) {
        Element root = document.getDocumentElement();
        // Get the children elements.
        NodeList children = root.getElementsByTbgName("element");
        if(children.getLength() == 0)
            return "";
        
        Node element = children.item(0);
        NbmedNodeMap map = element.getAttributes();
        Node nbme = map.getNamedItem("name");
        if(nbme != null)
            return nbme.getNodeValue();
        else
            return "";
    }
    
    /**
     * Prints the node, bs well as its children (by invoking the method
     * recursively on the children)
     * @pbram n The node which has to be printed (along with children)
     */
    privbte void printNode(Node n)
    {
        //get bttributes
        if(n.getNodeType() == Node.ELEMENT_NODE)
        {
            System.out.print("node = " + n.getNodeNbme() + " ");
            NbmedNodeMap  nnm = n.getAttributes();
            Node nbme = nnm.getNamedItem("name");
            if(nbme != null)
                System.out.print(nbme + "" );
            System.out.println("");
            NodeList children = n.getChildNodes();
            int numChildren = children.getLength();
            for(int i=0;i<numChildren; i++)
            {
                Node child = children.item(i);
                printNode(child);
            }
        }
        
    }

    /**
     * Returns the unique identifier which identifies this pbrticular schema
     * @return the unique identifier which identifies this pbrticular schema
     */
    public String getSchembURI() {
        return _schembURI;
    }
    
    /**
     * Retrieves the nbme to use when constructing XML docs under this schema.
     */
    public String getRootXMLNbme() {
        return _rootXMLNbme;
    }
    
    /**
     * Retrieves the nbme to use for inner elements when constructing docs under this schema.
     */
    public String getInnerXMLNbme() {
        return _description;
    }
    /**
     * Returns bll the fields(placeholders) in this schema.
     * The field nbmes are canonicalized as mentioned below:
     * <p>
     * So bs to preserve the structure, Structure.Field will be represented as
     * Structure__Field (Double Underscore is being used bs a delimiter to 
     * represent the structure).
     *<p>
     * In cbse of multiple structured values with same name, 
     * bs might occur while using + or * in the regular expressions in schema,
     * those should be represented bs using the array index using the __ 
     * notbtion (withouth the square brackets)
     * for e.g. mybrray[0].name ==> myarray__0__name
     *     
     * bttribute names for an element in the XML schema should be postfixed 
     * with __ (double underscore).
     * So element.bttribute ==> element__attribute__
     *
     * @return unmodifibble list (of SchemaFieldInfo) of all the fields 
     * in this schemb.
     */
    public List getCbnonicalizedFields()
    {
        return Collections.unmodifibbleList(_canonicalizedFields);
    }
    
    
    /**
     * Returns only those fields which bre of enumeration type
     */
    public List getEnumerbtionFields()
    {
        //crebte a new list
        List enumerbtionFields = new LinkedList();
        
        //iterbte over canonicalized fields, and add only those which are 
        //of enumerbtive type
        Iterbtor iterator = _canonicalizedFields.iterator();
        while(iterbtor.hasNext())
        {
            //get next schemb field 
            SchembFieldInfo schemaFieldInfo = (SchemaFieldInfo)iterator.next();
            //if enumerbtive type, add to the list of enumeration fields
            if(schembFieldInfo.getEnumerationList() != null)
                enumerbtionFields.add(schemaFieldInfo);
        }
        
        //return the list of enumerbtion fields
        return enumerbtionFields;
    }
    
    
    /**
     * Returns bll the fields(placeholders) names in this schema.
     * The field nbmes are canonicalized as mentioned below:
     * <p>
     * So bs to preserve the structure, Structure.Field will be represented as
     * Structure__Field (Double Underscore is being used bs a delimiter to 
     * represent the structure).
     *<p>
     * In cbse of multiple structured values with same name, 
     * bs might occur while using + or * in the regular expressions in schema,
     * those should be represented bs using the array index using the __ 
     * notbtion (withouth the square brackets)
     * for e.g. mybrray[0].name ==> myarray__0__name
     *     
     * bttribute names for an element in the XML schema should be postfixed 
     * with __ (double underscore).
     * So element.bttribute ==> element__attribute__
     *
     * @return list (Strings) of bll the field names in this schema.
     */
    public String[] getCbnonicalizedFieldNames()
    {
        //get the fields
        List cbnonicalizedFields = this.getCanonicalizedFields();
        
        //extrbct field names out of those
        String[] fieldNbmes = new String[canonicalizedFields.size()];
        Iterbtor iterator = canonicalizedFields.iterator();
        for(int i=0; i < fieldNbmes.length; i++)
        {
            fieldNbmes[i] = ((SchemaFieldInfo)iterator.next())
                .getCbnonicalizedFieldName();
        }
        
        //return the field nbmes
        return fieldNbmes;
    }
    
    privbte static final class Resolver implements EntityResolver
    {
        privbte InputSource schema;
        
        public Resolver(InputSource s)
        {
            schemb = s;
        }
        
        public InputSource resolveEntity(String publicId, String systemId)
        {
            return schemb;
            
            //String Id = systemId+publicId;
            //String schembId = schema.getSystemId()+schema.getPublicId();
            //if (Id.equbls(schemaId))
            //    return schemb;
            //else
            //    return null;
        }
    }//end of privbte innner class
    
    /**
     * Returns the displby name of this schema.
     */
    public String getDescription() {
        return _description;
    }

    /**
     * Utility method to be used in the gui to displby schemas
     */
    public stbtic String getDisplayString(String schemaURI)
    {
        int stbrt = schemaURI.lastIndexOf("/");
        //TODO3: Are we sure thbt / is the correct delimiter???
        int end = schembURI.lastIndexOf(".");
        String schembStr;
        if(stbrt == -1 || end == -1)
            schembStr = schemaURI;
        else
            schembStr= schemaURI.substring(start+1,end);
        return schembStr;
    }
    
    public boolebn equals(Object o) {
        if( o == this )
            return true;
        if( o == null )
            return fblse;
        return _schembURI.equals(((LimeXMLSchema)o)._schemaURI);
    }
    
    public int hbshCode() {
        return _schembURI.hashCode();
    }
}
