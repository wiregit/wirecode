package org.limewire.xmpp;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

public class NetworkModeHandler extends IQHandler {
    public static final int MAX_SIZE = 32;
    private List<JID> leaves;
    private List<JID> ultrapeers;
    /**
     * Create a basic module with the given name.
     *
     * @param moduleName The name for the module or null to use the default
     */
    public NetworkModeHandler(String moduleName) {
        super(moduleName);
        leaves = new ArrayList<JID>();
        ultrapeers = new ArrayList<JID>();
        // dummy values
        ultrapeers.add(new JID("12345", "tjulien-pc", "up"));
        ultrapeers.add(new JID("678910", "tjulien-pc", "up"));
        ultrapeers.add(new JID("1112131415", "tjulien-pc", "up"));
    }
    
    public List<JID> getUltrapeers() {
        return ultrapeers;
    }

    public IQ handleIQ(IQ packet) throws UnauthorizedException {
        String mode = packet.getChildElement().getText();
        if(mode.equalsIgnoreCase("leaf")) {
            synchronized (leaves) {
                if(leaves.size() < MAX_SIZE) {
                    leaves.add(packet.getFrom());
                } else {
                    // TODO error   
                }
            }
        } else if(mode.equalsIgnoreCase("ultrapeer")){
            synchronized (ultrapeers) {
                if(ultrapeers.size() < MAX_SIZE) {
                    ultrapeers.add(packet.getFrom());
                } else {
                    // TODO error   
                } 
            }
        } else {
            // TODO error
        }
        IQ networkModeResult = packet.createResultIQ(packet);
        networkModeResult.setChildElement("network-mode", "jabber:iq:lw-network-mode");
        return networkModeResult;
    }

    public IQHandlerInfo getInfo() {
        return new IQHandlerInfo("network-mode", "jabber:iq:lw-network-mode");
    }
}
