package com.limegroup.gnutella.xml;

import java.util.*;
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
    public LimeXMLDocument[] getDocuments(String aggrigateXMLStr, 
                                                 int totalResponseCount){
        
        LimeXMLDocument[] docs = new LimeXMLDocument[totalResponseCount];
        Element rootElement = getDOMTree(aggrigateXMLStr);
        if(rootElement==null)
            return null;
        //String schemaURI = getAttributeValue(rootElement,"schemaLocation");
        List children = LimeXMLUtils.getElements(rootElement.getChildNodes());
        //Note: each child corresponds to a LimeXMLDocument
        int z = children.size();
        for(int i=0;i<z;i++){
            Node currNode = (Node)children.get(i);
            String cIndex = getAttributeValue(currNode,"index");
            int currIndex= Integer.parseInt(cIndex);
            LimeXMLDocument currDoc=new LimeXMLDocument(currNode,rootElement);
            docs[currIndex]=currDoc;
        }
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
    public String getAggregrateString(Response[] responses){
        String agg = internalGetAggregrateString(responses);
        return (XMLStringUtils.XML_VERSION_DELIM+agg);
    }
        


    private String internalGetAggregrateString(Response[] responses){ 
        //get the first DOMTree(with only 1 child)
        int len = responses.length;
        int initIndex=-1;
        Element firstTree=null;
        boolean responseValid = false;
        while(initIndex<len && !responseValid){
            initIndex = getFirstRichResponse(initIndex+1,responses);        
            if(initIndex==-1)//reached the end with no meta-data
                return "";
            Response first = responses[initIndex];
            String XML = first.getMetadata();//we know its there
            firstTree = getDOMTree(XML);
            if(firstTree != null)
                responseValid = true;
        }
        if(!responseValid)//we could not find a single valid XML response
            return "";
        addIndex(firstTree,initIndex);
        String retString="<"+firstTree.getNodeName()+" ";
        List attributes=LimeXMLUtils.getAttributes(firstTree.getAttributes());
        int z = attributes.size();
        for(int i=0;i<z;i++){
            Node att = (Node)attributes.get(i);
            String attName = att.getNodeName();//.toLowerCase();Sumeet
            String attVal = att.getNodeValue();
            retString = retString+attName+"=\""+attVal+"\" ";
        }
        retString = retString +">";//closed root
        //add the child of the firstTree
        Node child = firstTree.getFirstChild();//only child
        retString = retString+getNodeString(child);
        for(int i=initIndex+1;i<len;i++){//look at the rest of the responses
            String xmlStr = responses[i].getMetadata();
            Element currTree = null;
            if(xmlStr!=null && !xmlStr.equals(""))                
               currTree = getDOMTree(xmlStr);
            if(currTree!=null){
                addIndex(currTree,i);
                Node currChild = currTree.getFirstChild();//only child
                retString = retString+getNodeString(currChild);
            }
        }
        retString = retString+"</"+firstTree.getNodeName()+">";
        return retString;
    }
    
    private String getNodeString(Node node){
        String retString="<"+node.getNodeName();
        //deal with attributes
        List attributes=LimeXMLUtils.getAttributes(node.getAttributes());
        int z = attributes.size();
        for(int i=0;i<z;i++){
            Node att = (Node)attributes.get(i);
            String attName = att.getNodeName().toLowerCase();
            String attVal = att.getNodeValue();
            retString = retString+" "+attName+"=\""+attVal+"\"";
        }
        String val = node.getNodeValue();
        List children=LimeXMLUtils.getElements(node.getChildNodes());
        int y=children.size();
        boolean hasValue;
        if(val!=null && !val.equals(""))
            hasValue = true;
        else 
            hasValue= false;
        if(y==0 && !hasValue)
            return retString+"/>";
        retString = retString+">";//close root
        if(hasValue)
            retString = retString+val;
        for(int i=0;i<y;i++){
            Node child = (Node)children.get(i);
            retString = retString + getNodeString(child);
        }
        retString = retString+"</"+node.getNodeName()+">";
        return retString;
        //TODO2:review and test
    }
        
    private void  addIndex(Node node, int childIndex){
        //find the only child of node - called newLeaf
        Element newLeaf = (Element)node.getFirstChild();
        //then  add the index as the attribute of newLeaf
        String ind = ""+childIndex;//cast it!
        newLeaf.setAttribute("index", ind);
        //then add newLeaf as a child of parent.
    }

    private int getFirstRichResponse (int initIndex,Response[] responses){
        Response res=null;
        int len = responses.length;
        int i=initIndex;
        for(;i<len;i++){
            res = responses[i];
            if(res.getMetadata()!=null && !res.getMetadata().equals(""))
                break;
        }
        if(res.getMetadata()== null || res.getMetadata().equals("")){
            return -1;//meaning we reached the end of the loop
            //w/o any rich res
        }
        return i;
    }
        
    private Element getDOMTree(String aggrigateXMLStr){
        InputSource source=new InputSource(new StringReader(aggrigateXMLStr));
        DOMParser parser = new DOMParser();
        Document root = null;
        try{            
            parser.parse(source);
        }catch(Exception e){
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

    public static void debug(String out) {
        if (true) 
            System.out.println(out);
    }
 
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
        
        String xmlCollectionString = help.getAggregrateString(resps);
        debug("Aggregate String = " + xmlCollectionString); 

        debug("--------------------------------");
        
        LimeXMLDocument[] madeXML = 
        help.getDocuments("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio index=\"0\" bitrate=\"181\" genre=\"Rock\"/><audio index=\"5\" bitrate=\"176\" genre=\"Pop\" title=\"Bigmouth Strikes Again\"/></audios>", 6);
        for (int i = 0; (i < 6) && (madeXML != null); i++)
            if (madeXML[i] != null)
                debug("mock-xml["+i+"] = " + 
                      madeXML[i].getXMLString());
        
        /*
        debug("--------------------------------");
        
        LimeXMLDocument[] retrievedXML = help.getDocuments(xmlCollectionString,
                                                           resps.length);
        for (int i = 0; (i < resps.length) && (retrievedXML != null); i++)
            if (retrievedXML[i] != null)
                debug("ret-xml["+i+"] = " + 
                      retrievedXML[i].getXMLString());
 
        // ASSERTIONS
 
        {
            
            // make sure getAggregates worked
            //Assert.that(xmlCollectionString.equals("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio index=\"1\" bitrate=\"192\" genre=\"Blues\"/><audio index=\"3\" bitrate=\"128\" genre=\"Country\"/></audios>"));
            
            // make sure that getDocuments worked
            Assert.that(retrievedXML[0] == null);
            Assert.that(retrievedXML[2] == null);
            Assert.that(retrievedXML[4] == null);
            Assert.that((retrievedXML[1].getXMLString()).equals("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio bitrate=192 genre=\"Blues\"/></audios>"));
            Assert.that((retrievedXML[3].getXMLString()).equals("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio bitrate=128 genre=\"Country\"/></audios>"));
            
            // some more assertions...
            Assert.that(madeXML[1] == null);
            Assert.that(madeXML[2] == null);
            Assert.that(madeXML[3] == null);
            Assert.that(madeXML[4] == null);
            Assert.that((madeXML[0].getXMLString()).equals("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio bitrate=181 genre=\"Rock\"/></audios>"));
            Assert.that((madeXML[5].getXMLString()).equals("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\"><audio bitrate=176 genre=\"Pop\" title=\"Bigmouth Strikes Again\"/></audios>"));
 
        }
        */
 
    }


}

