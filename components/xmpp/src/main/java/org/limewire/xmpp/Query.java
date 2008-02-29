package org.limewire.xmpp;

import java.util.List;

import org.jivesoftware.smack.packet.IQ;

public class Query extends IQ {
    public static final int DEFAULT_TTL = 3;
    
    private int ttl = DEFAULT_TTL;
    private int hops = 0;
    private List<String> keywords;
    // TODO exact word / phrase match
    
    public Query(List<String> keywords) {
        this.keywords = keywords;
    }
    
    public String getChildElementXML() {
        return "<query xmlns=\"jabber:iq:query\"><hops>" + hops + "</hops>" + "<ttl>" + ttl + "</ttl>" + getKeywordsElement(keywords) + "</query>";
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
