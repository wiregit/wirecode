package org.limewire.mojito2;

import org.limewire.mojito.db.impl.DatabaseImpl;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.routing.impl.RouteTableImpl;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.MessageFactory;

public class MojitoFactory2 {

    private static final String NAME = "DHT";
    
    public static DHT createDHT(Transport transport, MessageFactory messageFactory) {
        return createDHT(transport, messageFactory, ContextSettings.getVendor(), 
                ContextSettings.getVersion(), false);
    }
    
    public static DHT createDHT(Transport transport, MessageFactory messageFactory,
            Vendor vendor, Version version, boolean firewalled) {
        
        DatabaseImpl database = new DatabaseImpl();
        RouteTable routeTable = new RouteTableImpl();
        
        LocalContact localhost = (LocalContact)routeTable.getLocalNode();
        
        localhost.setVendor(vendor);
        localhost.setVersion(version);
        localhost.setFirewalled(firewalled);
        
        return new Context(NAME, transport, messageFactory, routeTable, database);
    }
}
