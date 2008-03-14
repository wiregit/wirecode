package org.limewire.xmpp.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.limewire.xmpp.client.commands.BrowseCommand;
import org.limewire.xmpp.client.commands.CommandDispatcher;
import org.limewire.xmpp.client.commands.DownloadCommand;
import org.limewire.xmpp.client.commands.SearchCommand;

public class AuthenticatedClient {
    public static void main(String [] args) throws XMPPException, ClassNotFoundException, UnsupportedLookAndFeelException, IllegalAccessException, InstantiationException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        XMPPConnection.DEBUG_ENABLED = true;
        
        XMPPConnection conn = new XMPPConnection(getConnectionConfig(args[0]));
        conn.connect();
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conn);
        serviceDiscoveryManager.addFeature("http://www.limewire.org/search");
        
        conn.login(args[1], args[2], "limewire");
        
        Roster roster = conn.getRoster();

        HashSet<String> limewireClients = new HashSet<String>();
        for (RosterEntry rosterEntry : roster.getEntries()) {
            Iterator<Presence> presences = roster.getPresences(rosterEntry.getUser());
            while(presences.hasNext()) {
                Presence presence = presences.next();
                try {
                    if (serviceDiscoveryManager.discoverInfo(presence.getFrom()).containsFeature("http://www.limewire.org/search")) {
                        limewireClients.add(presence.getFrom());
                        System.out.println("found lw client: " + presence.getFrom());
                    }
                } catch (XMPPException exception) {
                    //exception.printStackTrace();
                }
            }
        }
            

//        HashSet<JID> limewireClients = new HashSet<JID>();
//        conn.addPacketListener(new DiscoInfoListener(limewireClients), new AndFilter(new DiscoInfoFilter(), new IQTypeFilter(IQ.Type.RESULT)));
//        
//        conn.addPacketWriterListener(new DiscoInfoListener(), new AndFilter(new DiscoInfoFilter(), new IQTypeFilter(IQ.Type.RESULT)));

        
        
//        NetworkMode mode = new NetworkMode(NetworkMode.Mode.LEAF);
//        mode.setType(IQ.Type.SET);
//        conn.sendPacket(mode);
//        
//        Ping ping = new Ping();
//        ping.setType(IQ.Type.GET);
//        conn.sendPacket(ping);

        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.add(new DownloadCommand(conn));
        //dispatcher.add(new InitiateChatCommand(conn));
        //dispatcher.add(new RosterCommand(conn));
        dispatcher.add(new SearchCommand(conn, limewireClients));
        //dispatcher.add(new SendMessageCommand(conn));
        dispatcher.add(new BrowseCommand(conn));
        Thread t = new Thread(dispatcher);
        t.setDaemon(false);
        t.start();
    }

    private static ConnectionConfiguration getConnectionConfig(String serviceName) {
        if(serviceName.equals("gtalk")) {
            return new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
        } /*else if(serviceName.equals("yahoo")) {
            return new ConnectionConfiguration("chat.live.yahoo.com", 5222, "yahoo.com");
        } else if(serviceName.equals("aol")) {
            return new ConnectionConfiguration("xmpp.oscar.aol.com", 5222, "aol.com");
        } */else {
            throw new IllegalArgumentException("unknown service: " + serviceName + ". Supported values are: gtalk");//, yahoo, aol");
        }
    }
    
    static class DiscoInfoFilter extends PacketTypeFilter {
        public DiscoInfoFilter() {
            super(DiscoverInfo.class);
        }
    }

    static class DiscoInfoListener implements PacketListener {
        private boolean incoming;
        private Set<String> limewireClients;

        DiscoInfoListener() {
            incoming = false;    
        }
        
        DiscoInfoListener(Set<String> limewireClients){
            incoming = true;
            this.limewireClients = limewireClients;
        }
        
        public void processPacket(Packet packet) {
            DiscoverInfo discoInfo = (DiscoverInfo)packet;
            if(incoming) {
                if(discoInfo.containsFeature("http://www.limewire.org/search")) {
                    limewireClients.add(discoInfo.getFrom());
                }
            } else {
                discoInfo.addFeature("http://www.limewire.org/search");
            }
        }
    }
}
