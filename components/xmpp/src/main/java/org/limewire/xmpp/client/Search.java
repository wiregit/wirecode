package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jivesoftware.smack.packet.IQ;

public class Search extends IQ {
    
    private List<String> keywords;
    // TODO exact word / phrase match
    // TODO match case
    
    public Search(List<String> keywords) {
        this.keywords = keywords;
    }

    public Search(String args) {
        this(getList(args));
    }

    public List<String> getKeywords() {
        return keywords;
    }

    private static List<String> getList(String args) {
        StringTokenizer st = new StringTokenizer(args);
        ArrayList<String> tokens = new ArrayList<String>();
        while(st.hasMoreElements()) {
            tokens.add(st.nextToken());
        }
        return tokens;
    }

    public String getChildElementXML() {
        return "<search xmlns=\"jabber:iq:lw-search\">" + getKeywordsElement(keywords) + "</search>";
    }

    private String getKeywordsElement(List<String> keywords) {
        // TODO XML escape
        String elem = "<keywords>";
        for(String word : keywords) {
            elem += word + ",";
        }
        if(elem.endsWith(",")) {
            elem = elem.substring(0, elem.length() - 1);
        }
        elem += "</keywords>";
        return elem;
    }
}
