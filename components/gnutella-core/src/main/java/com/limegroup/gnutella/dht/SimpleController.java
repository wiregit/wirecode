package com.limegroup.gnutella.dht;

import java.io.IOException;

import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.RouteTable.RouteTableEvent;
import org.limewire.mojito2.routing.RouteTable.RouteTableListener;

import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;

/**
 * 
 */
abstract class SimpleController extends AbstractController {

    protected final ConnectionServices connectionServices;
    
    public SimpleController(DHTMode mode, 
            Transport transport,
            NetworkManager networkManager,
            ConnectionServices connectionServices) {
        super(mode, transport, networkManager);
        
        this.connectionServices = connectionServices;
    }
    
    @Override
    public void start() throws IOException {
        super.start();
        
        // If we're an Ultrapeer we want to notify our firewalled
        // leafs about every new Contact we're adding to our RT.
        if (connectionServices.isActiveSuperNode()) {
            MojitoDHT dht = getMojitoDHT();
            RouteTable routeTable = dht.getRouteTable();
            routeTable.addRouteTableListener(new RouteTableListener() {
                @Override
                public void handleRouteTableEvent(RouteTableEvent event) {
                    processRouteTableEvent(event);
                }
            });
        }
    }
    
    /**
     * 
     */
    private void processRouteTableEvent(RouteTableEvent event) {
        switch (event.getEventType()) {
            case ADD_ACTIVE_CONTACT:
            case ADD_CACHED_CONTACT:
            case UPDATE_CONTACT:
                Contact node = event.getContact();
                if (!isLocalhost(node)) {
                    addContact(node);
                }
                break;
        }
    }
    
    /**
     * 
     */
    protected abstract void addContact(Contact contact);
}
