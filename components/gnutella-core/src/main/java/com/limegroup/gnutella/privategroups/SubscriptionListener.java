package com.limegroup.gnutella.privategroups;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.limegroup.gnutella.gui.tabs.BuddylistTab;


/**
 * Class that listens for requests to subscribe to current user
 */
public class SubscriptionListener implements PacketListener{

    private static final Log LOG = LogFactory.getLog(PGRPClientImpl.class);
    private static Vector<Object> subscriptionListener = new Vector();
    
    public void processPacket(Packet packet) {
        LOG.debug("SubscriptionListener: processPacket");
        if(packet instanceof Presence){
            //start gui window to ask user by notifying listeners
            for(Object listener: subscriptionListener){
                LOG.debug("notify listener: " + listener);
                if(listener instanceof BuddylistTab)
                    ((BuddylistTab)listener).handleUserSubscription(packet);
            }
        }
    }

    public static void registerListener(Object o){
        
        LOG.debug("SubscriptionListener: registerListener");
        subscriptionListener.add(o);
    }
    
    public static void unregisterListener(Object o){
        LOG.debug("SubscriptionListener: unregisterListener");
        subscriptionListener.remove(o);
    }
}
