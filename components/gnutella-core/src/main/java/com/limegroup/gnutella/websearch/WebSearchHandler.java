package com.limegroup.gnutella.websearch;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import java.io.*;


public class WebSearchHandler {
    private static final String HOST = 
        "http://us01.xmlsearch.findwhat.com/bin/findwhat.dll?getresults";
    
    private static final String BASE = "&base=";

    private static final String SRCH = "&mt=";

    //TODO1 this needs to change
    private static final String AFF_ID = "&aff_id="+1000;

    private static final String FIL = "&fl=0";//no filtering
    
    private static final String ENC = "&df=0";


    private static final String IP = "&ip_addr=";


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
    }

}

