package com.limegroup.gnutella.websearch;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;


public class WebSearchHandler {
    private static final String HOST = 
        "http://us01/xmlsearch.findwhat.com/bin/findwhat.dll?getresults";
    
    private static final String BASE = "&base=";

    private static final String SRCH = "&mt=";

    //TODO1 this needs to change
    private static final String AFF_ID = "&aff_id="+1000;

    private static final String FIL = "&fl=0";//no filtering
    
    private static final String ENC = "&df=1";


    private static final String IP = 
        "&ip_addr="+NetworkUtils.ip2string(RouterService.getAddress());


    public void search(String searchString, int searchIndex) {
        //TODO1: Find a way to get the base parameter from the GUI or store it
        //somewhere else
        String urlStr = HOST+BASE+0+SRCH+searchString+AFF_ID+FIL+ENC+IP;
        //TODO1: make a http client connection with this url and parse the
        //results. I will need to do this after I recreate this branch
        System.out.println(urlStr);
    }

}
