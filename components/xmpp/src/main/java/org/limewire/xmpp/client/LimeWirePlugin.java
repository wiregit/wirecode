package org.limewire.xmpp.client;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.spark.plugin.Plugin;
import org.limewire.xmpp.client.commands.BrowseCommand;
import org.limewire.xmpp.client.commands.CommandDispatcher;
import org.limewire.xmpp.client.commands.DownloadCommand;
import org.limewire.xmpp.client.commands.SearchCommand;
import org.xmlpull.v1.XmlPullParser;

public class LimeWirePlugin implements Plugin {
    private static final String LW_SERVICE_NS = "http://www.limewire.org/";
    private static final String LW_SERVICE_NAME = "limewire";
    
    static {
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(final XMPPConnection connection) {
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(LW_SERVICE_NS);
                SearchListener searchListener = new SearchListener(connection, new File("C:\\Documents and Settings\\tjulien\\LimeWire Shared"));
                connection.addPacketListener(searchListener, searchListener.getPacketFilter());
                ProviderManager.getInstance().addIQProvider("search", "jabber:iq:lw-search", new SearchIQProvider());

                new Thread(new Runnable(){
                    public void run() {
                        // TODO HACK HACK
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        startCommandLineClient(connection);
                    }
                }).start();
                
            }
        });
    }

    private static void startCommandLineClient(XMPPConnection connection) {
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        //serviceDiscoveryManager.addFeature("http://www.limewire.org/search");

        Roster roster = connection.getRoster();

        HashSet<String> limewireClients = new HashSet<String>();
        for (RosterEntry rosterEntry : roster.getEntries()) {
            Iterator<Presence> presences = roster.getPresences(rosterEntry.getUser());
            while(presences.hasNext()) {
                Presence presence = presences.next();
                try {
                    if (serviceDiscoveryManager.discoverInfo(presence.getFrom()).containsFeature("http://www.limewire.org/")) {
                        limewireClients.add(presence.getFrom());
                        System.out.println("found lw client: " + presence.getFrom());
                    }
                } catch (XMPPException exception) {
                    //exception.printStackTrace();
                }
            }
        }

        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.add(new DownloadCommand(connection));
        //dispatcher.add(new InitiateChatCommand(conn));
        //dispatcher.add(new RosterCommand(conn));
        dispatcher.add(new SearchCommand(connection, limewireClients));
        //dispatcher.add(new SendMessageCommand(conn));
        dispatcher.add(new BrowseCommand(connection));
        Thread t = new Thread(dispatcher);
        t.setDaemon(false);
        t.start();
    }

    public void initialize() {
        //ProviderManager.getInstance().addIQProvider("search", "jabber:iq:lw-search", new SearchIQProvider());
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
            return new LimePacketExtension();
        }
    }

    private static class LimePacketExtension extends DefaultPacketExtension {
        //private final XmlPullParser parser;

        /*public LimePacketExtension(XmlPullParser parser) {
            this.parser = parser;
        }*/
        
        public LimePacketExtension() {
            super("search", "jabber:iq:lw-search");
        }

        public String getElementName() {
            return "search";
        }

        public String getNamespace() {
            return "jabber:iq:lw-search";
        }

        public String toXML() {
            return "<" + getElementName() + " xmlns=\"" + getNamespace() + "\" />";
        }
    }

    private static class SearchIQProvider implements IQProvider {
        public IQ parseIQ(XmlPullParser parser) throws Exception {
            parser.nextTag(); // keywords            
            return new Search(parser.nextText());
        }
    }
}
