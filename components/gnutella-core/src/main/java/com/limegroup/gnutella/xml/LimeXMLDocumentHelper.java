package com.limegroup.gnutella.xml;

import java.util.*;
import java.io.*;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.*;
import org.w3c.dom.*;
import com.limegroup.gnutella.Response;



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
    public String getAggrigrateString(Response[] responses){
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
        for(int i=initIndex+1;i<len;i++){//look at the rest of the responses
            String xmlStr = responses[i].getMetadata();
            Element currTree = getDOMTree(xmlStr);
            if(currTree!=null)
                addChildNode(firstTree,currTree,i);
        }
        LimeXMLDocument d = new LimeXMLDocument(firstTree);//special const.
        return d.getXMLString();        
    }
    
    private void addChildNode(Node parent, Node newChild, int childIndex){
        //find the only child of newChild - called newLeaf
        Element newLeaf = (Element)newChild.getFirstChild();
        //then  add the index as the attribute of newLeaf
        String ind = ""+childIndex;//cast it!
        newLeaf.setAttribute("index", ind);
        //then add newLeaf as a child of parent.
        parent.appendChild((Node)newLeaf);
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


}
