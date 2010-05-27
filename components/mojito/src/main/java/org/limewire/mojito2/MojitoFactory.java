package org.limewire.mojito2;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.limewire.mojito2.io.DatagramTransport;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.DefaultMessageFactory;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.routing.LocalContact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.RouteTableImpl;
import org.limewire.mojito2.storage.DatabaseImpl;

public class MojitoFactory {
    
    private MojitoFactory() {}
    
    public static MojitoDHT createDHT(String name) {
        MessageFactory messageFactory 
            = new DefaultMessageFactory();
        
        DatabaseImpl database = new DatabaseImpl();
        RouteTable routeTable = new RouteTableImpl();
        
        DefaultDHT context = new DefaultDHT(name, messageFactory, 
                routeTable, database);
        
        return new DefaultMojitoDHT(context);
    }
    
    public static MojitoDHT createDHT(String name, int port) throws IOException {
        MojitoDHT dht = createDHT(name);
        bind(dht, port);
        return dht;
    }
    
    public static Transport bind(DHT dht, int port) throws IOException {
        Transport transport = new DatagramTransport(port);
        dht.bind(transport);
        
        ((LocalContact)dht.getLocalNode()).setContactAddress(
                new InetSocketAddress("localhost", port));
        
        return transport;
    }
}
