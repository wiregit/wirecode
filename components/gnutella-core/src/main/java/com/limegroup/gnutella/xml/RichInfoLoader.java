package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.Assert;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.BufferedReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**The purpose of this class is to read all the files in the /etc/schema
 * directory. Once this is done we have to read all the meta-data from the 
 * xml files in the /etc directory.
 * @author Sumeet Thadani
 */

class RichInfoLoader {

    private Map uriSchemaMap;
    private List schemas;
    private List unmodifiableSchemas;

    //constructor
    public RichInfoLoader(){	
	uriSchemaMap = new HashMap();
	schemas = new ArrayList();
	unmodifiableSchemas = Collections.unmodifiableList(schemas);
	System.out.println("Sumeet: coming out of constructor");
    }
    
    /**
     * Clears all the data structures
     */
    public final void clear() {
        uriSchemaMap.clear();
        schemas.clear();
    }
    
    /** 
     * Takes a schema and the corresponding xml file and loads all the 
     * meta-data about all the files.
     */
    public void loadMetaData(File f){
	String buffer="";
	String xmlStruct="";
	InputSource schema;
	try {
	    BufferedReader br = new BufferedReader(new FileReader(f));
	    while(buffer!=null){
		buffer=br.readLine();
		if (buffer!=null){
		    buffer=buffer.trim();
		    xmlStruct = xmlStruct+buffer;
		}
	    }
	    xmlStruct.trim();
	}catch(IOException e){
	    e.printStackTrace();
	}
	System.out.println("Sumeet: starting loadMetaData");
	DocumentBuilderFactory documentBuilderFactory =
            DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setValidating(true);
	documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder=null;
        try{
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        }catch(ParserConfigurationException e){
            //throw new GMLParseException("Unable to create validating parser");
	    e.printStackTrace();
        }
	schema = new InputSource(new StringReader(xmlStruct));
        // Set an entity resolver to resolve the schema
	documentBuilder.setEntityResolver(new Resolver(schema));

        // Parse the schema and create a  document
        Document document=null;

	System.out.println("Sumeet: about to create blank document");
        try {
            document = documentBuilder.parse(schema);

        }catch(SAXException e){
            //throw new GMLParseException("SAX Exception\n" + e.toString());
	    e.printStackTrace();
        }catch(IOException e){
            //throw new GMLParseException("IO Exception\n" + e.toString());
	    e.printStackTrace();
        }
	//create a template_document from the schema. 
	//The template_document we create will serve as the 
	//request Element and the reply element
	System.out.println("Sumeet: got root element");
	Element root = document.getDocumentElement();
	String rootName = root.getTagName();
	System.out.println(rootName);
	NodeList children = root.getChildNodes();
	int numChildren = children.getLength();
	System.out.println("Sumeet: no of children "+numChildren);
	for(int i=0;i<numChildren; i++){
	    Node n = children.item(i);
	    String childName = n.getNodeName();
	    System.out.println("Sumeet: child Name is "+childName);
	}
    }
    
    private final class Resolver implements EntityResolver {
        private InputSource schema;

        public Resolver(InputSource s){
            schema = s;
        }
	
        public InputSource resolveEntity(String publicId, String systemId){
	    String Id = systemId+publicId;
	    String schemaId = schema.getSystemId()+schema.getPublicId();
	    if (Id.equals(schemaId))
                return schema;
            else
                return null;
        }
    }//end of private innner class

    public static void main(String args[]){
	//This method is the test code.
	RichInfoLoader r = new RichInfoLoader();
	r.test();
    }
    private void test(){
	File f = null;
	try{
	    f = new File(XMLProperties.instance().getXMLSchemaDir(),"gen-books.xsd");
	}catch(Exception e){
	    e.printStackTrace();
	}
	loadMetaData(f);
    }       
}









