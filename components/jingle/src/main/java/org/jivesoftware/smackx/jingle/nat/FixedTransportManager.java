package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smackx.jingle.JingleSession;

public class FixedTransportManager extends JingleTransportManager{

    private final String ip;
    private final int port;

    public FixedTransportManager(String ip, int port){
        this.ip = ip;
        this.port = port;
    }

    protected TransportResolver createResolver(JingleSession session) {
        return new FixedResolver(ip, port);
    }
}
