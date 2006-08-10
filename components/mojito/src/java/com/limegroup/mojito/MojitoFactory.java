/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.limegroup.mojito;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.ContactNode;
import com.limegroup.mojito.settings.ContextSettings;

/**
 * 
 */
public class MojitoFactory {
    
    private static final Log LOG = LogFactory.getLog(MojitoFactory.class);
    
    private static final long VERSION_NUMBER = 0;
    
    private static final String DEFAULT_NAME = "DHT";
    
    private MojitoFactory() {}
    
    public static MojitoDHT createDHT() {
        return createDHT(DEFAULT_NAME);
    }
    
    public static MojitoDHT createDHT(String name) {
        return create(name, null, false);
    }
    
    public static MojitoDHT createFirewalledDHT() {
        return createFirewalledDHT(DEFAULT_NAME);
    }
    
    public static MojitoDHT createFirewalledDHT(String name) {
        return create(name, null, true);
    }
    
    private static Context create(String name, Contact localNode, boolean firewalled) {
        
        if (name == null) {
            name = DEFAULT_NAME;
        }
        
        if (localNode == null) {
            int vendor = ContextSettings.VENDOR.getValue();
            int version = ContextSettings.VERSION.getValue();
            
            KUID nodeId = KUID.createRandomNodeID();
            int instanceId = 0;
            
            localNode = ContactNode.createLocalContact(vendor, version, nodeId, instanceId, firewalled);
        }

        Context context = new Context(name, localNode);
        return context;
    }
    
    static void store(Context context, OutputStream out) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(out);
        
        synchronized (context) {
            // SERIAL_VERSION_UID
            oos.writeLong(VERSION_NUMBER);
            
            // Time
            oos.writeLong(System.currentTimeMillis());
            
            // Name
            oos.writeObject(context.getName());
            
            // Contact
            oos.writeObject(context.getLocalNode());
            
            // RouteTable
            oos.writeObject(context.getRouteTable());

            // Database
            oos.writeObject(context.getDatabase());
        }
    }
    
    /**
     * Loads a Mojito DHT instance from the InputStream.
     */
    public static MojitoDHT load(InputStream in) 
            throws ClassNotFoundException, IOException {
        
        ObjectInputStream ois = new ObjectInputStream(in);
        
        // Version Number
        @SuppressWarnings("unused")
        long versionNumber = ois.readLong();
        
        // Time
        long time = ois.readLong();
        
        // Name
        String name = (String)ois.readObject();
        
        // Contact
        Contact localNode = (Contact)ois.readObject();
        ((ContactNode)localNode).resetContactAddress();
        
        // RouteTable
        RouteTable routeTable = (RouteTable)ois.readObject();
        
        // Database
        Database database = (Database)ois.readObject();
        
        boolean timeout = false;
        if ((System.currentTimeMillis() - time) 
                >= ContextSettings.NODE_ID_TIMEOUT.getValue()) {
            timeout = true;
        }
        
        Context context = new Context(name, localNode, routeTable, database);
        
        if (timeout) {
            if (LOG.isInfoEnabled()) {
                LOG.info(localNode + " has timed out. Clearing Database and rebuilding RouteTable");
            }
            
            context.changeNodeID();
        }
        
        return context;
    }
}
