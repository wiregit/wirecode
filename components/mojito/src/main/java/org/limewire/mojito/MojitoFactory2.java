package org.limewire.mojito;

import org.limewire.mojito.db.impl.DatabaseImpl;
import org.limewire.mojito.io.MessageDispatcher2.Transport;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.routing.impl.RouteTableImpl;
import org.limewire.mojito.settings.ContextSettings;

public class MojitoFactory2 {

    private static final String NAME = "DHT";
    
    public static MojitoDHT2 createDHT(Transport transport) {
        
        DatabaseImpl database = new DatabaseImpl();
        RouteTable routeTable = new RouteTableImpl();
        
        LocalContact localhost = (LocalContact)routeTable.getLocalNode();
        
        localhost.setVendor(ContextSettings.getVendor());
        localhost.setVersion(ContextSettings.getVersion());
        localhost.setFirewalled(false);
        
        return new Context2(NAME, transport, routeTable, database);
    }
}
