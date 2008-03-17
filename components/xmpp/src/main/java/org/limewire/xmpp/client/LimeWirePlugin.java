package org.limewire.xmpp.client;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.*;
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

import java.io.File;
import java.util.*;

public class LimeWirePlugin {
    private static final String LW_SERVICE_NS = "http://www.limewire.org/";
    private static final String LW_SERVICE_NAME = "limewire";

    static {
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(final XMPPConnection connection) {
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(LW_SERVICE_NS);
                SearchListener searchListener = new SearchListener(connection, new File("C:\\data\\"));
                SearchResultListener searcheResultListener = new SearchResultListener();
                connection.addPacketListener(searchListener, searchListener.getPacketFilter());
                connection.addPacketListener(searcheResultListener, searcheResultListener.getPacketFilter());
                ProviderManager.getInstance().addIQProvider("search", "jabber:iq:lw-search", SearchResult.getIQProvider());
                ProviderManager.getInstance().addIQProvider("search-results", "jabber:iq:lw-search-results", SearchResult.getIQProvider());

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
    }
}
