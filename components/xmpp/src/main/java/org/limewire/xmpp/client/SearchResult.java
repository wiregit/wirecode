package org.limewire.xmpp.client;

import org.jivesoftware.smack.packet.IQ;

public class SearchResult extends IQ {
    public String getChildElementXML() {
        return "<search xmlns=\"jabber:iq:lw-search-result\">" + getKeywordsElement(keywords) + "</search>";
    }
}
