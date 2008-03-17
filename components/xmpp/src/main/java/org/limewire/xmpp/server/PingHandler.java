package org.limewire.xmpp.server;

import java.util.List;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.tree.DefaultElement;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

public class PingHandler extends IQHandler {
    private List<JID> ultrapeers;
    /**
     * Create a basic module with the given name.
     *
     * @param moduleName The name for the module or null to use the default
     */
    public PingHandler(String moduleName, List<JID> ultrapeers) {
        super(moduleName);
        this.ultrapeers = ultrapeers;
    }

    public IQ handleIQ(IQ packet) throws UnauthorizedException {
        Element pingReply = new DefaultElement(new QName("ping", new Namespace("", "jabber:iq:ping")));
        synchronized (ultrapeers) {            
            for(JID ultrapeer : ultrapeers) {
                DefaultElement ultraPeerElem = new DefaultElement("ultrapeer", new Namespace("", "jabber:iq:ping"));
                ultraPeerElem.setText(ultrapeer.toString());
                pingReply.add(ultraPeerElem);
            }
        }
        IQ networkModeResult = packet.createResultIQ(packet);
        networkModeResult.setChildElement(pingReply);
        return networkModeResult;
    }

    public IQHandlerInfo getInfo() {
        return new IQHandlerInfo("ping", "jabber:iq:ping");
    }
}
