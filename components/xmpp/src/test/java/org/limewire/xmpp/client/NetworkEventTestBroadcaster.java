package org.limewire.xmpp.client;

import com.limegroup.gnutella.NetworkManagerEvent;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventListener;

public class NetworkEventTestBroadcaster implements ListenerSupport<NetworkManagerEvent> {
    
    public final EventListenerList<NetworkManagerEvent> listeners =
        new EventListenerList<NetworkManagerEvent>();
    
    public void addListener(EventListener<NetworkManagerEvent> networkManagerEventEventListener) {
        listeners.addListener(networkManagerEventEventListener);
    }

    public boolean removeListener(EventListener<NetworkManagerEvent> networkManagerEventEventListener) {
        return listeners.removeListener(networkManagerEventEventListener);
    }
}
