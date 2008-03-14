package org.limewire.xmpp.client;

import org.dom4j.Element;
import org.jivesoftware.smack.packet.IQ;

public class SearchResult extends IQ {
    private final Element [] childElements;

    public SearchResult(Element[] childElements) {
        this.childElements = childElements;
    }
    
    public String getChildElementXML() {
        return "<search-results xmlns=\"jabber:iq:lw-search-results\">" + getChildrenXML(childElements) + "</search-results>";
    }

    private String getChildrenXML(Element[] childElements) {
        StringBuilder children = new StringBuilder();
        for(Element child : childElements) {
            children.append(child.asXML());    
        }
        return children.toString();
    }
}
