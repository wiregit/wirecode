package com.limegroup.gnutella.websearch;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.http.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import java.io.*;
import org.apache.xerces.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;


public class WebSearchHandler {
    private static final String HOST = 
        "http://sales.limewire.com/cgi-bin/echoJunk.cgi?";
        //"http://us01.xmlsearch.findwhat.com/bin/findwhat.dll?getresults";

    private static final String BASE = "&base=";

    private static final String SRCH = "&mt=";

    //TODO1 this needs to change
    private static final String AFF_ID = "&aff_id="+1000;

    private static final String FIL = "&fl=0";//no filtering
    
    private static final String ENC = "&df=0";


    private static final String IP = "&ip_addr=";
    
    private DOMParser parser;

    public WebSearchHandler() {
        parser = new DOMParser();
    }


    public void search(String searchString, int searchIndex) {
        //TODO1: Find a way to get the base parameter from the GUI or store it
        //somewhere else
        //1. Create the search url
        String ip = NetworkUtils.ip2string(RouterService.getAddress());
        String urlStr = HOST+BASE+0+SRCH+searchString+AFF_ID+FIL+ENC+IP+ip;
        //2. Make a HttpClient and send the GET Request
        GetMethod getMethod = new GetMethod(urlStr);
        getMethod.addRequestHeader("Cache-Control", "no-cache");
        getMethod.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
        HttpClient client = HttpClientManager.getNewClient();
        byte[] response = null;
        //TODO1: remove these asserts, they are really for debugging for now. 
        try {
            client.executeMethod(getMethod);
            response = getMethod.getResponseBody();
            if(response == null)
                Assert.that(false, "server is not responding correctly");
        } catch (IOException iox) {
            Assert.that(false, "server is not responding correctly");
        }
        System.out.println("Sumeet: got xml response:\n"+new String(response));
        parseAndDisplay(response);
    }

    private void parseAndDisplay(byte[] serverResponse) {
        try {
        InputSource inputSource = 
        new InputSource(new ByteArrayInputStream(serverResponse));
        parser.parse(inputSource);
        Document document = parser.getDocument();
        Element resultElement = document.getDocumentElement();
        //attributes of the result element.  

        //Note: This tab has the following attributes status for if the result
        //is valid, records for the total number of results, first for the index
        //of the first result of the set of all results, last for the index of
        //the last result and searchrequest for the query string.

        //TODO1: All the above attributes have to be parsed correctly for the
        //proper parsing and display of results

        String v;
        v = 
        LimeXMLUtils.getAttributeValue(resultElement.getAttributes(),"status");
        boolean valid = "OK".equalsIgnoreCase(v);
        Assert.that(valid,"server not responding correctly");//TODO:remove
        v = 
        LimeXMLUtils.getAttributeValue(resultElement.getAttributes(),"last");
        int numReplies = Integer.parseInt(v);
        
        NodeList results = resultElement.getChildNodes();
        for(int i=0; i<numReplies; i++) {
            //Note: these nodes have the following fields:
            //title, url, desc, bidprice, clickurl
            Node currResult = results.item(i);
            NodeList resultAttribs = currResult.getChildNodes();
            String title = null;
            String desc = null;
            String bidPrice = null;
            String clickURL = null;
            for(int j=0; j < resultAttribs.getLength(); j++) {
                Node currAttrib = resultAttribs.item(j);
                String nodeName = currAttrib.getNodeName();
                String val = LimeXMLUtils.getText(currAttrib.getChildNodes()); 
                if("title".equalsIgnoreCase(nodeName))
                    title = val;
                else if("description".equalsIgnoreCase(nodeName))
                    desc = val;
                else if("bidprice".equalsIgnoreCase(nodeName))
                    bidPrice = val;
                else if("clickurl".equalsIgnoreCase(nodeName))
                    clickURL = val;
            }
            //create a new WebResult with these attributes
            System.out.println(title+", "+desc+", "+bidPrice+", "+clickURL);
            WebResult result = new WebResult(title, desc, bidPrice, clickURL);
            //TODO: add these WebResults to the GUI
            RouterService.getCallback().addWebResult(result);
        }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

