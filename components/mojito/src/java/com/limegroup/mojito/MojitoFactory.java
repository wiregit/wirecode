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
import com.limegroup.mojito.settings.ContextSettings;

/**
 * A Factory class to create or load MojitoDHTs
 */
public class MojitoFactory {
    
    private static final Log LOG = LogFactory.getLog(MojitoFactory.class);
    
    private static final long VERSION_NUMBER = 0;
    
    private static final String DEFAULT_NAME = "DHT";
    
    private MojitoFactory() {}
    
    /**
     * Creates a MojitoDHT with default settings
     */
    public static MojitoDHT createDHT() {
        return createDHT(DEFAULT_NAME);
    }
    
    /**
     * Creates a MojitoDHT with the given name
     */
    public static MojitoDHT createDHT(String name) {
        return create(name, false);
    }
    
    /**
     * Creates a MojitoDHT with the given name, vendor code and version
     */
    public static MojitoDHT createDHT(String name, int vendor, int version) {
        return create(name, vendor, version, false);
    }
    
    /**
     * Creates a firewalled MojitoDHT
     */
    public static MojitoDHT createFirewalledDHT() {
        return createFirewalledDHT(DEFAULT_NAME);
    }
    
    /**
     * Creates a firewalled MojitoDHT with the given name
     */
    public static MojitoDHT createFirewalledDHT(String name) {
        return create(name, true);
    }
    
    /**
     * Creates a firewalled MojitoDHT with the given name, vendor code and version
     */
    public static MojitoDHT createFirewalledDHT(String name, int vendor, int version) {
        return create(name, vendor, version, true);
    }
    
    /**
     * Creates a MojitoDHT with the given arguments
     */
    private static Context create(String name, boolean firewalled) {
        return create(name, 
                ContextSettings.VENDOR.getValue(), 
                ContextSettings.VERSION.getValue(),
                firewalled);
    }
    
    /**
     * Creates a MojitoDHT with the given arguments
     */
    private static Context create(String name, int vendor, int version, boolean firewalled) {
        
        if (name == null) {
            name = DEFAULT_NAME;
        }
        
        return new Context(name, vendor, version, firewalled);
    }
    
    static void store(Context context, OutputStream out) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(out);
        
        synchronized (context) {
            // VERSION_NUMBER
            oos.writeLong(VERSION_NUMBER);
            
            // Time
            oos.writeLong(System.currentTimeMillis());
            
            // Name
            oos.writeObject(context.getName());
            
            // RouteTable
            oos.writeObject(context.getRouteTable());

            // Database
            oos.writeObject(context.getDatabase());
        }
    }
    
    /**
     * Loads a serialized MojitoDHT instance from the given InputStream
     */
    public static MojitoDHT load(InputStream in) 
            throws ClassNotFoundException, IOException {
        
        return load(in, 
                ContextSettings.VENDOR.getValue(), 
                ContextSettings.VERSION.getValue());
    }
    
    /**
     * Loads a Mojito DHT instance from the InputStream.
     */
    public static MojitoDHT load(InputStream in, int vendor, int version) 
            throws ClassNotFoundException, IOException {
        
        ObjectInputStream ois = new ObjectInputStream(in);
        
        // Version Number
        @SuppressWarnings("unused")
        long versionNumber = ois.readLong();
        
        // Time
        long time = ois.readLong();
        
        // Name
        String name = (String)ois.readObject();
        
        // RouteTable
        RouteTable routeTable = (RouteTable)ois.readObject();
        
        // This happens if you do something stupid like:
        // MojitoDHT.getRouteTable().clear();
        // MojitoDHT.store(OutputStream);
        if (routeTable.size() == 0) {
            throw new IOException("The RouteTable is in an inconsistent state");
        }
        
        // Database
        Database database = (Database)ois.readObject();
        
        boolean timeout = false;
        if ((System.currentTimeMillis() - time) 
                >= ContextSettings.NODE_ID_TIMEOUT.getValue()) {
            timeout = true;
        }
        
        Context context = new Context(name, vendor, version, routeTable, database);
        
        if (timeout) {
            if (LOG.isInfoEnabled()) {
                LOG.info(context.getLocalNode() + " has timed out. Clearing Database and rebuilding RouteTable");
            }
            
            context.changeNodeID();
        }
        
        return context;
    }
}
