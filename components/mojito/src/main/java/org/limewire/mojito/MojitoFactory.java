package org.limewire.mojito;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.limewire.mojito.io.DatagramTransport;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.message.DefaultMessageFactory;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTableImpl;
import org.limewire.mojito.storage.DatabaseImpl;

public class MojitoFactory {
    
    private MojitoFactory() {}
    
    public static MojitoDHT createDHT(String name) {
        MessageFactory messageFactory 
            = new DefaultMessageFactory();
        
        DatabaseImpl database = new DatabaseImpl();
        RouteTable routeTable = new RouteTableImpl();
        
        return new DefaultMojitoDHT(name, messageFactory, 
                routeTable, database);
    }
    
    public static MojitoDHT createDHT(String name, int port) throws IOException {
        MojitoDHT dht = createDHT(name);
        bind(dht, port);
        return dht;
    }
    
    public static Transport bind(MojitoDHT dht, int port) throws IOException {
        Transport transport = new DatagramTransport(port);
        dht.bind(transport);
        
        dht.setContactAddress(new InetSocketAddress("localhost", port));
        
        return transport;
    }
}
