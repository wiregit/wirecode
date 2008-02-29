package org.limewire.xmpp;

import org.jivesoftware.smack.packet.IQ;

public class Ping extends IQ {
    
    public Ping() {
    }
    
    public String getChildElementXML() {
        return "<ping xmlns=\"jabber:iq:ping\"/>";
    }
}
