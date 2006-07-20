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
import java.security.KeyPair;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.ContactNode;
import com.limegroup.mojito.security.CryptoHelper;
import com.limegroup.mojito.settings.ContextSettings;
import com.limegroup.mojito.util.BucketUtils;

/**
 * 
 */
public class MojitoFactory {
    
    private static final Log LOG = LogFactory.getLog(MojitoFactory.class);
    
    private static final long SERIAL_VERSION_UID = 0;
    
    private static final String DEFAULT_NAME = "DHT";
    
    private MojitoFactory() {}
    
    public static MojitoDHT createDHT() {
        return createDHT(DEFAULT_NAME);
    }
    
    public static MojitoDHT createDHT(String name) {
        return create(name, null, false, CryptoHelper.createKeyPair());
    }
    
    public static MojitoDHT createFirewalledDHT() {
        return createFirewalledDHT(DEFAULT_NAME);
    }
    
    public static MojitoDHT createFirewalledDHT(String name) {
        return create(name, null, true, CryptoHelper.createKeyPair());
    }
    
    private static Context create(String name, Contact localNode, 
            boolean firewalled, KeyPair keyPair) {
        
        if (name == null) {
            name = DEFAULT_NAME;
        }
        
        if (localNode == null) {
            int vendor = ContextSettings.VENDOR.getValue();
            int version = ContextSettings.VERSION.getValue();
            
            KUID nodeId = KUID.createRandomNodeID();
            int instanceId = 0;
            
            localNode = ContactNode.createLocalContact(vendor, version, nodeId, instanceId);
            
            if (firewalled) {
                ((ContactNode)localNode).setFirewalled(true);
            }
        }

        Context context = new Context(name, localNode, keyPair);
        return context;
    }
    
    static void store(Context context, OutputStream out) throws IOException {
        store(context, out, true, true);
    }
    
    static void store(Context context, OutputStream out, 
            boolean storeRouteTable, boolean storeDatabase) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(out);
        
        synchronized (context) {
            // SERIAL_VERSION_UID
            oos.writeLong(SERIAL_VERSION_UID);
            
            // Time
            oos.writeLong(System.currentTimeMillis());
            
            // Name
            oos.writeObject(context.getName());
            
            // ContactNode
            Contact localNode = context.getLocalNode();
            oos.writeInt(localNode.getVendor());
            oos.writeShort(localNode.getVersion());
            oos.writeObject(localNode.getNodeID());
            oos.writeByte(localNode.getInstanceID());
            oos.writeBoolean(localNode.isFirewalled());
            
            if (localNode.isFirewalled()) {
                storeDatabase = false;
            }
            
            // Store the RouteTable
            oos.writeBoolean(storeRouteTable);
            if (storeRouteTable) {
                List<Contact> nodes = context.getRouteTable().getLiveContacts();
                
                KUID nodeId = localNode.getNodeID();
                for(Contact node : nodes) {
                    if (!node.getNodeID().equals(nodeId)) {
                        oos.writeObject(node);
                    }
                }
                
                // Terminator
                oos.writeObject(null); 
            }

            // Store the Database
            oos.writeBoolean(storeDatabase);
            boolean anyLocalKeyValue = false;
            
            if (storeDatabase) {
                Collection<KeyValue> keyValues = context.getDatabase().getValues();
                
                for(KeyValue keyValue : keyValues) {
                    if (!storeRouteTable 
                            && !keyValue.isLocalKeyValue()) {
                        continue;
                    }
                    
                    if (!anyLocalKeyValue 
                            && keyValue.isLocalKeyValue()) {
                        anyLocalKeyValue = true;
                    }
                    
                    oos.writeObject(keyValue);
                }
                
                // Terminator
                oos.writeObject(null);
            }
            
            // Store the KeyPair if there are any local KeyValues
            if (storeDatabase && anyLocalKeyValue) {
                oos.writeObject(context.getKeyPair());
            } else {
                oos.writeObject(null);
            }
        }
    }
    
    /**
     * Loads a Mojito DHT instance from the InputStream.
     */
    public static MojitoDHT load(InputStream in) 
            throws ClassNotFoundException, IOException {
        
        ObjectInputStream ois = new ObjectInputStream(in);
        
        // SERIAL_VERSION_UID
        long serialVersionUID = ois.readLong();
        
        // Time
        long time = ois.readLong();
        
        // Name
        String name = (String)ois.readObject();
        
        // ContactNode
        int vendor = ois.readInt();
        int version = ois.readUnsignedShort();
        KUID oldNodeId = (KUID)ois.readObject();
        int instanceId = ois.readUnsignedByte();
        boolean firewalled = ois.readBoolean();
        
        boolean timeout = (System.currentTimeMillis()-time) 
                            >= ContextSettings.NODE_ID_TIMEOUT.getValue();
        KUID nodeId;
        
        if (timeout) {
            if (LOG.isInfoEnabled()) {
                LOG.info(oldNodeId + " has timed out. Clearing database and rebuilding route table");
            }
            
            nodeId = KUID.createRandomNodeID();
            instanceId = 0;
        } else {
            nodeId = oldNodeId;
        }
        
        Contact local = ContactNode.createLocalContact(vendor, version, nodeId, instanceId);
        
        // Create an instance w/o a KeyPair for now (will set it later!)
        Context dht = create(name, local, firewalled, null);
        
        // Load the RouteTable
        boolean storeRouteTable = ois.readBoolean();
        if (storeRouteTable) {
            RouteTable routeTable = dht.getRouteTable();
            
            Contact node = null;
            
            while((node = (Contact)ois.readObject()) != null) {
                routeTable.add(node);
            }
            
            if(timeout) { 
                //we changed our local node ID! rebuild the table
                List<Contact> nodesList = routeTable.getContacts();
                routeTable.clear();
                //sort the nodes list (because rebuilding the table with the
                //new nodeID will probably evict some nodes)
                nodesList = BucketUtils.sortAliveToFailed(nodesList);
                //now insert everything again except the old local node
                for(Contact contact : nodesList) {
                    
                    if(!contact.getNodeID().equals(oldNodeId)) {
                        routeTable.add(contact);
                    }
                }
            }
        }
        
        // Load the Database
        boolean storeDatabase = ois.readBoolean();
        if (storeDatabase) {
            Database database = dht.getDatabase();
            
            KeyValue keyValue = null;
            while((keyValue = (KeyValue)ois.readObject()) != null) {
                if (!timeout || keyValue.isLocalKeyValue()) {
                    database.add(keyValue);
                }
            }
        }
        
        // Load the KeyPair. If null then create a new KeyPair!
        KeyPair keyPair = (KeyPair)ois.readObject();
        if (!storeDatabase || keyPair == null) {
            keyPair = CryptoHelper.createKeyPair();
        }
        dht.setKeyPair(keyPair);

        return dht;
    }
}
