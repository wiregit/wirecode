package com.limegroup.gnutella.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Response;


public final class LimeXMLDocumentHelper{

	/**
	 * Private constructor to ensure that this class can never be
	 * instantiated.
	 */
	private LimeXMLDocumentHelper() {}

    /**
     * TO be used when a Query Reply comes with a chunk of meta-data
     * we want to get LimeXMLDocuments out of it
     */
    public static List getDocuments(String aggregatedXML, 
                                    int totalResponseCount) {
        
        if(aggregatedXML==null || 
                aggregatedXML.equals("") ||
                totalResponseCount <= 0)
            return Collections.EMPTY_LIST;
        
        List results = new ArrayList();
        
        Iterator xmlDocumentsIterator = XMLParsingUtils.split(aggregatedXML).iterator();
        while(xmlDocumentsIterator.hasNext()) {
            
            String xmlDocument = (String)xmlDocumentsIterator.next();
            XMLParsingUtils.ParseResult parsingResult;
            try {
                parsingResult = XMLParsingUtils.parse(xmlDocument,totalResponseCount);
            } catch (SAXException sax) {
                continue;// bad xml, ignore
            } catch (IOException bad) {
                return Collections.EMPTY_LIST; // abort
            }
            
            String indexKey = parsingResult.canonicalKeyPrefix + "index__";
            LimeXMLDocument[] documents = new LimeXMLDocument[totalResponseCount];
            
            Iterator mapsIterator = parsingResult.iterator();
            while(mapsIterator.hasNext()) {
                
                Map map = (Map)mapsIterator.next();
                
                String sindex = (String)map.get(indexKey);
                if (sindex == null)
                    return Collections.EMPTY_LIST;
                
                int index = -1;
                try{
                    index = Integer.parseInt(sindex);
                }catch(NumberFormatException bad) { //invalid document
                    return Collections.EMPTY_LIST;
                }
                
                if (index >= documents.length || index < 0)
                    return Collections.EMPTY_LIST; // malicious document, can't trust it.
                
                documents[index] = new LimeXMLDocument(map,parsingResult.schemaURI);
                
            }
            results.add(documents);
        }
        return results;
    }
    /**
     * @param responses array is a set of responses. Some have meta-data
     * some do not. 
     * The aggregrate string should reflect the indexes of the 
     * responses
     *<p>
     * If none of the responses have any metadata, this method returns an
     * empty string.
     */
    public static String getAggregateString(Response[] responses){
        //this hashmap remembers the current state of the string for each uri
        HashMap uriToString /*LimeXMLSchemaURI -> String */ = new HashMap();
        for(int i=0; i< responses.length; i++) {
            LimeXMLDocument doc = responses[i].getDocument();
            if(doc==null)
                continue;
            aggregateResponse(uriToString, doc,i);
        }
        StringBuffer retStringB = new StringBuffer();

        //iterate over the map and close all the strings out.
        Iterator iter = uriToString.values().iterator();
        while(iter.hasNext()) {
            String str = ((StringBuffer)iter.next()).toString();
            int begin = str.indexOf("<",2);//index of opening outer(plural)
            int end = str.indexOf(" ",begin);
            retStringB.append(str);
            retStringB.append("</");
            retStringB.append(str.substring(begin+1,end));
            retStringB.append(">");
        }
        return retStringB.toString();
    }

    private static void aggregateResponse(HashMap uriToString, 
                                         LimeXMLDocument doc, int index) {
        if(doc == null)//response has no document
            return;
        String uri = doc.getSchemaURI();
        StringBuffer currStringB = (StringBuffer)uriToString.get(uri);

        if(currStringB == null) {//no entry so far
            //1. add the outer (plural form) - w/o the end
            String str = null;
            try {
                str = doc.getXMLString();
            } catch(SchemaNotFoundException e) {  
                return;//dont do anything to the map, just return
            }
            if(str==null || str.equals("")) 
                return;
            str = str.substring(0,str.lastIndexOf("<"));
            //2.append the index in the right place.
            int p = str.indexOf("<");//index of opening header
            Assert.that(p>=0, "LimeXMLDocument is broken");
            p = str.indexOf("<",p+1);//index of opening outer(plural) 
            Assert.that(p>=0, "LimeXMLDocument is broken");
            p = str.indexOf("<",p+1);//index of opening inner
            Assert.that(p>=0, "LimeXMLDocument is broken");
            int q = str.indexOf(">",p+1);//index of first closing tag
            int k = str.lastIndexOf("/",q-1);//is it tag closing inclusive?
            if(k!=-1 && p < k && k < q )//   "/" b/w open and close
                if(str.substring(k+1,q).trim().equals(""))
                    p=k;//white space only b/w / and >
                else
                    p=q;
            else
                p=q;
            String first = str.substring(0,p);
            String last = str.substring(p);
            StringBuffer strB = new StringBuffer(first.length() +
                                                 15 + last.length());
            // str = first+" index=\""+index+"\" "+last;
            strB.append(first);
            strB.append(" index=\"");
            strB.append(index);
            strB.append("\" ");
            strB.append(last);
            //3. add the entry in the map
            uriToString.put(uri, strB);
        }
        else {            
            //1.strip the plural form out of the xml string
            String str = null;
            try{
                str = doc.getXMLString();
            } catch(SchemaNotFoundException e) {
                return;//dont modify the string in the hashmap. just return
            }
            int begin = str.indexOf("<");//index of header
            Assert.that( begin != -1, str);
            begin = str.indexOf("<",begin+1);//index of outer(plural) tag-close
            Assert.that( begin != -1, str);
            begin = str.indexOf("<",begin+1);//index of begining of current tag
            Assert.that( begin != -1, str);            
            int end = str.lastIndexOf("<");
            Assert.that( end != -1, str);
            str = str.substring(begin,end);
            //2.insert the index 
            int p = str.indexOf(">");
            int q = str.lastIndexOf("/",p-1);
            if (q!=-1 && q < p) // 
                if(str.substring(q+1,p).trim().equals(""))
                   p = q;
            String first = str.substring(0,p);
            String last = str.substring(p);
            //  str = first+" index=\""+index+"\" "+last;
            //  currString = currString+str;
            currStringB.append(first);
            currStringB.append(" index=\"");
            currStringB.append(index);
            currStringB.append("\" ");
            currStringB.append(last);
            //3.concatinate the remaining xml to currString
            //4.put it back into the map
            uriToString.put(uri,currStringB);
        }
    }



}
