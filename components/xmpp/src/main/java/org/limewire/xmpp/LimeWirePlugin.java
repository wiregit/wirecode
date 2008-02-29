package org.limewire.xmpp;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.spark.plugin.Plugin;
import org.xmlpull.v1.XmlPullParser;

public class LimeWirePlugin implements Plugin {
    private static final String LW_SERVICE_NS = "http://www.limewire.org/";
    private static final String LW_SERVICE_NAME = "limewire";
    
    static {
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(XMPPConnection connection) {
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(LW_SERVICE_NS);
                connection.addPacketListener(new LimeWirePacketListener(), new LimeWirePacketFilter());
            }
        });
    }

    public void initialize() {
        ProviderManager.getInstance().addExtensionProvider(LW_SERVICE_NAME, LW_SERVICE_NS, new LimeExtensionProvider());
    }

    public void shutdown() {
        //ProviderManager.getInstance().removeExtensionProvider(LW_SERVICE_NAME, LW_SERVICE_NS);
    }

    public boolean canShutDown() {
        return true;
    }

    public void uninstall() {
        ProviderManager.getInstance().removeExtensionProvider(LW_SERVICE_NAME, LW_SERVICE_NS);
    }
    
    private static class LimeWirePacketFilter implements PacketFilter {
        public boolean accept(Packet packet) {
            return packet instanceof Message && packet.getExtension(LW_SERVICE_NAME, LW_SERVICE_NS) != null;
        }
    }
    
    private static class LimeWirePacketListener implements PacketListener {
        public void processPacket(Packet packet) {
            Message message = (Message)packet;
            LimePacketExtension packetExt = (LimePacketExtension)message.getExtension(LW_SERVICE_NAME, LW_SERVICE_NS);
        }
    }

    private static class LimeExtensionProvider implements PacketExtensionProvider {
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            return new LimePacketExtension(parser);
        }
    }

    private static class LimePacketExtension implements PacketExtension {
        private final XmlPullParser parser;

        public LimePacketExtension(XmlPullParser parser) {
            this.parser = parser;
        }

        public String getElementName() {
            return LW_SERVICE_NAME;
        }

        public String getNamespace() {
            return LW_SERVICE_NS;
        }

        public String toXML() {
            return "<" + getElementName() + " xmlns=\"" + getNamespace() + "\" />";
        }
    }
}
