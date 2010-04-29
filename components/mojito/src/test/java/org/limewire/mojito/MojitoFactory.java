package org.limewire.mojito;

import java.io.IOException;

import org.limewire.mojito2.Context;
import org.limewire.mojito2.DHT;
import org.limewire.mojito2.io.DatagramTransport;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.DefaultMessageFactory;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.RouteTableImpl;
import org.limewire.mojito2.storage.DatabaseImpl;

public class MojitoFactory {
    
    private MojitoFactory() {}
    
    public static DHT createDHT(String name) throws IOException {
        MessageFactory messageFactory 
            = new DefaultMessageFactory();
        
        DatabaseImpl database = new DatabaseImpl();
        RouteTable routeTable = new RouteTableImpl();
        
        return new Context(name, messageFactory, 
                routeTable, database);
    }
    
    public static DHT createDHT(String name, int port) throws IOException {
        DHT dht = createDHT(name);
        bind(dht, port);
        return dht;
    }
    
    public static Transport bind(DHT dht, int port) throws IOException {
        Transport transport = new DatagramTransport(
                port, dht.getMessageFactory());
        dht.bind(transport);
        return transport;
    }
}
