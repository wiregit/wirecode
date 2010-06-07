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

/**
 * An utility class to create {@link MojitoDHT} instances.
 */
public class MojitoFactory {
    
    private MojitoFactory() {}
    
    /**
     * Creates and returns a {@link MojitoDHT} with the given name.
     */
    public static MojitoDHT createDHT(String name) {
        MessageFactory messageFactory 
            = new DefaultMessageFactory();
        
        DatabaseImpl database = new DatabaseImpl();
        RouteTable routeTable = new RouteTableImpl();
        
        return new DefaultMojitoDHT(name, messageFactory, 
                routeTable, database);
    }
    
    /**
     * Creates and returns a {@link MojitoDHT} that's bound to the given
     * port number.
     */
    public static MojitoDHT createDHT(String name, int port) throws IOException {
        MojitoDHT dht = createDHT(name);
        bind(dht, port);
        return dht;
    }
    
    /**
     * Binds the {@link MojitoDHT} to the given port number.
     */
    public static Transport bind(MojitoDHT dht, int port) throws IOException {
        Transport transport = new DatagramTransport(port);
        dht.bind(transport);
        
        // NOTE: We're doing this because we're very often doing 
        // something like this in our tests:
        //
        // dh1.ping(dht2.getContactAddress());
        //
        // It's being assumed that dht2 has a working address which 
        // it doesn't out of the box. To be more precise, it has a
        // working InetAddress but the port number is 0 (zero).
        dht.setContactAddress(new InetSocketAddress("localhost", port));
        
        return transport;
    }
}
