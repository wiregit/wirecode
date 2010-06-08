package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.io.InputStream;

import org.limewire.mojito.routing.BucketNode;
import org.limewire.mojito.routing.LocalContact;
import org.limewire.mojito.routing.RemoteContact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTableImpl;
import org.limewire.util.ConverterObjectInputStream;

/**
 * An utility class to read serialized {@link RouteTable}s.
 */
class MojitoObjectInputStream extends ConverterObjectInputStream {

    public MojitoObjectInputStream(InputStream in) throws IOException {
        super(in);
        
        addLookup("org.limewire.mojito.routing.impl.LocalContact", 
                LocalContact.class.getName());
        
        addLookup("org.limewire.mojito.routing.impl.RemoteContact", 
                RemoteContact.class.getName());
        
        addLookup("org.limewire.mojito.routing.impl.RouteTableImpl", 
                RouteTableImpl.class.getName());
        
        addLookup("org.limewire.mojito.routing.impl.BucketNode", 
                BucketNode.class.getName());
    }
}
