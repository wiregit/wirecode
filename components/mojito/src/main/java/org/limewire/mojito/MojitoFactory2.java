package org.limewire.mojito;

import org.limewire.mojito.db.impl.DatabaseImpl;
import org.limewire.mojito.io.MessageDispatcher2.Transport;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.routing.impl.RouteTableImpl;
import org.limewire.mojito.settings.ContextSettings;

public class MojitoFactory2 {

    private static final String NAME = "DHT";
    
    public static MojitoDHT2 createDHT(Transport transport) {
        return createDHT(transport, ContextSettings.getVendor(), 
                ContextSettings.getVersion(), false);
    }
    
    public static MojitoDHT2 createDHT(Transport transport, 
            Vendor vendor, Version version, boolean firewalled) {
        
        DatabaseImpl database = new DatabaseImpl();
        RouteTable routeTable = new RouteTableImpl();
        
        LocalContact localhost = (LocalContact)routeTable.getLocalNode();
        
        localhost.setVendor(vendor);
        localhost.setVersion(version);
        localhost.setFirewalled(firewalled);
        
        return new Context2(NAME, transport, routeTable, database);
    }
}
