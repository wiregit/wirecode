package org.limewire.xmpp;

import org.jivesoftware.smack.packet.IQ;

public class NetworkMode extends IQ {
    private final Mode mode;

    public enum Mode {LEAF, ULTRAPPER}
    
    public NetworkMode(Mode mode) {
        this.mode = mode;
    }
    
    public String getChildElementXML() {
        return "<network-mode xmlns=\"jabber:iq:lw-network-mode\">" + mode.toString() + "</network-mode>";
    }
}
