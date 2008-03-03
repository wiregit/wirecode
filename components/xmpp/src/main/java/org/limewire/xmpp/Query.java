package org.limewire.xmpp;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jivesoftware.smack.packet.IQ;

public class Query extends IQ {
    public static final int DEFAULT_TTL = 3;
    
    private int ttl = DEFAULT_TTL;
    private int hops = 0;
    private List<String> keywords;
    // TODO exact word / phrase match
    // TODO match case
    
    public Query(List<String> keywords) {
        this.keywords = keywords;
    }

    public Query(String args) {
        this(getList(args));
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
        return "<query xmlns=\"jabber:iq:lw-query\"><hops>" + hops + "</hops>" + "<ttl>" + ttl + "</ttl>" + getKeywordsElement(keywords) + "</query>";
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
