package com.limegroup.gnutella.xml;

import com.sun.java.util.collections.*;
import java.io.*;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.*;
import org.w3c.dom.*;
import com.limegroup.gnutella.*;


public class LimeXMLDocumentHelper{

    /**
     * TO be used when a Query Reply comes with a chunk of meta-data
     * we want to get LimeXMLDocuments out of it
     *<p>
     * returns null if the XML string does now parse.
     */
    public List getDocuments(String aggregrateXMLStr, 
                                                 int totalResponseCount){
        ArrayList retList = new ArrayList();
        if(aggregrateXMLStr==null || aggregrateXMLStr.equals(""))
            return retList;
        int startIndex=aggregrateXMLStr.indexOf
                                (XMLStringUtils.XML_DOC_START_IDENTIFIER);
        int endIndex = startIndex;
        String chunk = "";
        boolean finished= false;
        while(!finished){
            startIndex = endIndex;//nextRound
            if (startIndex == -1){
                finished = true;
                continue;
            }
            endIndex=aggregrateXMLStr.indexOf
            (XMLStringUtils.XML_DOC_START_IDENTIFIER,startIndex+1);
            if (endIndex > 0)
                chunk = aggregrateXMLStr.substring(startIndex, endIndex);
            else
                chunk = aggregrateXMLStr.substring(startIndex);        
            
            LimeXMLDocument[] docs = new LimeXMLDocument[totalResponseCount];
            Element rootElement = getDOMTree(chunk);
            if(rootElement==null){
                retList.add(null);
                continue;
            }
            //String schemaURI=getAttributeValue(rootElement,"schemaLocation");
            List children=LimeXMLUtils.getElements(rootElement.getChildNodes());
            //Note: each child corresponds to a LimeXMLDocument
            int z = children.size();
            for(int i=0;i<z;i++){
                Node currNode = (Node)children.get(i);
                String cIndex = getAttributeValue(currNode,"index");
                int currIndex= Integer.parseInt(cIndex);
                LimeXMLDocument currDoc=new LimeXMLDocument(currNode,rootElement);
                docs[currIndex]=currDoc;
            }
            retList.add( docs);
        }
        return retList;
    }
    
    /**
     * Breaks the passed xml document in aggregate form (where the root
     * element has multiple child nodes) to a list of xml documents
     * where the root node has got only one child. In other words it breaks
     * the multiple documents embedded in a single big document to respective
     * non-embedded documents
     * @param aggregateXMLStr string representing xml document in 
     * aggregate form
     * @return List (of LimeXMLDocument) of LimeXMlDocuments that we get 
     * after breaking the 
     * aggregate string. Returns null, if the aggregateXMLString is not a
     * valid xml
     */ 
    public static List /* LimeXMLDocument */ breakSingleSchemaAggregateString(
        String aggregrateXMLStr)
    {
        //get the root element of the aggregate string
        Element rootElement = getDOMTree(aggregrateXMLStr);

        //return null, if the passed xml couldnt be parsed
        if(rootElement==null)
            return null;
        
        //create a list to store the documents
        ArrayList docs = new ArrayList();
        
        //get the child nodes, each of which will be transformed to 
        //a separate document
        List children = LimeXMLUtils.getElements(rootElement.getChildNodes());
        
        //Iterate over the children
        for(Iterator iterator = children.iterator(); iterator.hasNext();)
        {
            //get the next child node
            Node currNode = (Node)iterator.next();
            //convert the subtree represented by the child node to an 
            //instance of LimeXMLDocument, and add to the list of documents
            docs.add(new LimeXMLDocument(currNode,rootElement));
        }
        
        //return the list of documents
        return docs;
    }
    
    
    /**
     * @param responses array is a set of responses. Sore have meta-data
     * some do not. 
     * The aggregrate string should reflect the indexes of the 
     * responses
     *<p>
     * If none of the responses have any metadata, this method returns an
     * empty string.
     */
    public String getAggregateString(Response[] responses){
        String agg = internalGetAggregateString(responses);
        return (agg);
    }
        

    private static final String SCHEMA_PRECEDE_STRING = "SchemaLocation";

    /** @return the schema uri for the given input xml.  may return null.
     */
    private String getSchemaURI(String xml) {
        String retString = null;
        int i = xml.indexOf(SCHEMA_PRECEDE_STRING);
        if (i > -1) {
            i += SCHEMA_PRECEDE_STRING.length();
            if (xml.charAt(i++) == '=') 
                if ((xml.charAt(i) == '\'') || ((xml.charAt(i) == '"'))) {
                    char delim = xml.charAt(i);
                    int begin = ++i;
                    while (xml.charAt(i) != delim)
                        i++;
                    int end = i;
                    retString = xml.substring(begin,end);
            
                }
        }
        return retString;
    }


    private String getSchemaURIXML(Element tree) {
        String retString="<"+tree.getNodeName()+" ";
        List attributes=LimeXMLUtils.getAttributes(tree.getAttributes());
        int z = attributes.size();
        for(int i=0;i<z;i++){
            Node att = (Node)attributes.get(i);
            String attName = att.getNodeName();//.toLowerCase();Sumeet
            String attVal = att.getNodeValue();
            retString = retString+attName+"=\""+attVal+"\" ";
        }
        retString = retString +">";//closed root
        return retString;
    }


    private HashMap _urisToLists = new HashMap();

    private void indexTree(String schemaURI, Element tree) {
        LinkedList trees = null;
        if (_urisToLists.containsKey(schemaURI)) 
            trees = (LinkedList) _urisToLists.remove(schemaURI);        
        else 
            trees = new LinkedList();
        trees.add(tree);
        _urisToLists.put(schemaURI, trees);
    }

    private String internalGetAggregateString(Response[] responses){ 
        _urisToLists.clear();
        
        final int len = responses.length;

        // since responses may have disparate xml, i need to group them while
        // remembering their index in the response array
        for (int i = 0; i < len; i++) {
            Response currResp = responses[i];
            String currXML = currResp.getMetadata();
            Element currTree = null;
            if ((currXML != null) && !currXML.equals(""))
                currTree = getDOMTree(currXML);
            if (currTree != null) {
                addIndex(currTree, i);
                String schemaURI = getSchemaURI(currXML);
                indexTree(schemaURI, currTree);
            }
        }

        // now get all the disparate xml from the hashmap, and construct a xml
        // string for each.
        StringBuffer retStringBuffer = new StringBuffer();
        Iterator keys = _urisToLists.keySet().iterator();
        while (keys.hasNext()) { // for each schema URI
            String currKey = (String) keys.next();
            List treeList = (List) _urisToLists.get(currKey);
            Iterator trees = treeList.iterator();
            boolean firstIter = true;

            while (trees.hasNext()) { // for each xml per schema URI
                Element currTree = (Element) trees.next();

                // open the outer on the first iteration...
                if (firstIter) {
                    retStringBuffer.append(XMLStringUtils.XML_VERSION_DELIM);
                    retStringBuffer.append(getSchemaURIXML(currTree));
                    firstIter = false;
                }

                // get current child info and append...
                Node currChild = getFirstNonTextChild(currTree);
                retStringBuffer.append(getNodeString(currChild));

                // upon the last iteration, close outer....
                if (!trees.hasNext()) 
                    retStringBuffer.append("</"+
                                           currTree.getNodeName()+
                                           ">");
            }
        }

        return retStringBuffer.toString();
    }
    
    private String getNodeString(Node node){

        StringBuffer retStringB = new StringBuffer();

        // start everything off....
        retStringB.append("<"+node.getNodeName());

        //deal with attributes
        List attributes=LimeXMLUtils.getAttributes(node.getAttributes());
        int z = attributes.size();
        for(int i=0;i<z;i++){
            Node att = (Node)attributes.get(i);
            String attName = att.getNodeName().toLowerCase();
            String attVal = LimeXMLUtils.encodeXML(att.getNodeValue());
            retStringB.append(" "+attName+"=\""+attVal+"\"");
        }

        // deal with children
        if (node.hasChildNodes()) {
            // i have children, so don't close off this node...
            retStringB.append(">");
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength();i++) {
                Node child = (Node)children.item(i);
                if (child.getNodeType() == Node.TEXT_NODE)
                    retStringB.append(((Text)child).getData());
                if (child.getNodeType() == Node.ELEMENT_NODE)
                    retStringB.append(getNodeString(child));
            }
        }

        if (node.hasChildNodes())
            retStringB.append("</"+node.getNodeName()+">");
        else 
            retStringB.append("/>");
            
        return retStringB.toString();
        //TODO2:review and test
    }
        
    private void addIndex(Node node, int childIndex){
        //find the only child of node - called newLeaf
        Element newLeaf = getFirstNonTextChild(node);
        //then  add the index as the attribute of newLeaf
        if(newLeaf == null)
            return;
        String ind = ""+childIndex;//cast it!
        newLeaf.setAttribute("index", ind);
        //then add newLeaf as a child of parent.
    }

     private Element getFirstNonTextChild(Node node){
           List children = LimeXMLUtils.getElements(node.getChildNodes());
           int z = children.size();
           for (int i=0;i<z;i++){
                Node n = (Node)children.get(i);
                if(n.getNodeType() == Node.ELEMENT_NODE)
                      return (Element)n;
           }
           return null;
     }
    
        
    private static Element getDOMTree(String aggrigateXMLStr){
        InputSource source=new InputSource(new StringReader(aggrigateXMLStr));
        DOMParser parser = new DOMParser();
        Document root = null;
        try{            
            parser.parse(source);
        }catch(Exception e){
            e.printStackTrace();
            //could not parse XML well
            return null;
        }
        root = parser.getDocument();
        //get the schemaURI
        Element rootElement = root.getDocumentElement();
        return rootElement;
    }

    /**
     * Takes a DOMElement, and a string that is part of the attribute
     * we are looking for, and returns the value of that attribute
     */
    private String getAttributeValue(Node element,String targetName){
        String lowerTargetName = targetName.toLowerCase();
        List atts=LimeXMLUtils.getAttributes(element.getAttributes());
        String retString="";
        int z = atts.size();        
        for(int i=0;i<z;i++){
            Node att = (Node)atts.get(i);
            String lowerAttName = att.getNodeName().toLowerCase();            
            if(lowerAttName.indexOf(lowerTargetName)>=0)
                retString=att.getNodeValue();
        }
        return retString;
    }

    private static final boolean debugOn = true;
    public static void debug(String out) {
        if (debugOn) 
            System.out.println(out);
    }


    /*
    public static void main(String argv[]) {
        LimeXMLDocumentHelper help = new LimeXMLDocumentHelper();
        
        Response[] resps = new Response[5];

        resps[0] = new Response(0, 100, "File 1");
        resps[1] = new Response(1, 200, "File 2", "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio bitrate=\"192\" genre=\"Blues\"/></audios>");
        resps[2] = new Response(0, 300, "File 3");
        resps[3] = new Response(3, 400, "File 4", "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio bitrate=\"128\" genre=\"Country\"/></audios>");
        resps[4] = new Response(0, 500, "File 5");
        
        for (int i = 0; i < resps.length; i++)
            debug("resps["+i+"].metadata = " +
                  resps[i].getMetadata());
        
        String xmlCollectionString = help.getAggregateString(resps);
        debug("Aggregate String (no disparates) = " + xmlCollectionString); 

        resps = new Response[10];
        resps[0] = new Response(0, 100, "File 1");
        resps[1] = new Response(1, 200, "File 2", "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio bitrate=\"192\" genre=\"Blues\"/></audios>");
        resps[2] = new Response(0, 300, "File 3");
        resps[3] = new Response(3, 400, "File 4", "<?xml version=\"1.0\"?><backlash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdot.xsd\"><story><comments>Duh!</comments><author>Susheel</author></story></backlash>");
        resps[4] = new Response(0, 500, "File 5");
        resps[5] = new Response(1, 200, "File 6", "<?xml version=\"1.0\"?><radioStations xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/radiolocator.xsd\"><radioStation format=\"Blues\" city=\"New York\"/></radioStations>");
        resps[6] = new Response(1, 200, "File 7", "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio bitrate=\"160\" genre=\"Chamber Music\"/></audios>");
        resps[7] = new Response(1, 200, "File 8", "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio bitrate=\"170\" genre=\"Pop\"/></audios>");
        resps[8] = new Response(1, 200, "File 9", "<?xml version=\"1.0\"?><radioStations xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/radiolocator.xsd\"><radioStation format=\"Classic Rock=\"></radioStation></radioStations>");
        resps[9] = new Response(1, 200, "File 10", "<?xml version=\"1.0\"?><backlash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdot.xsd\"><story><image>J. Lo</image><title>Oops, I did it Again!</title></story></backlash>");
        
        for (int i = 0; i < resps.length; i++)
            debug("resps["+i+"].metadata = " +
                  resps[i].getMetadata());

        System.out.println("----->");
        
        xmlCollectionString = help.getAggregateString(resps);
        debug("Aggregate String (disparates) = " + xmlCollectionString); 



        debug("--------------------------------");
        
        List madeXMLList = 
        help.getDocuments("<?xml version=\"1.0\"?><backlash><story index=\"0\"><title>Susheel</title></story><story index=\"4\"><author>Daswani</author><section>News</section></story></backlash>", 6);
        LimeXMLDocument[] madeXML = (LimeXMLDocument[])madeXMLList.get(0);
        for (int i = 0; (i < 6) && (madeXML != null); i++)
            if (madeXML[i] != null) {
                madeXML[i].setSchemaURI("http://www.limewire.com/schemas/slashdot.xsd");
                debug("mock-xml["+i+"] = " + 
                      madeXML[i].getXMLString());
            }
        
        debug("--------------------------------");
        
        List retrievedXMLList = help.getDocuments(xmlCollectionString,
                                                  resps.length);
        Iterator arrays = retrievedXMLList.iterator();
        while (arrays.hasNext()) {
            LimeXMLDocument[] retrievedXML = (LimeXMLDocument[]) arrays.next();
            if (retrievedXML == null)
                debug("unexpected!");
            for (int i = 0; (i < resps.length) && (retrievedXML != null); i++)
                if (retrievedXML[i] != null) 
                    debug("ret-xml["+i+"] = " + 
                          retrievedXML[i].getXMLString());

        }
    }
    */
}



