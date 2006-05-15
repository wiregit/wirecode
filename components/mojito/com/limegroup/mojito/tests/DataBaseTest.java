/*
 * Mojito Distributed Hash Tabe (DHT)
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
 
package com.limegroup.mojito.tests;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHT;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.KeyValue;


public class DataBaseTest {

    private static InetSocketAddress addr = new InetSocketAddress("localhost",3000);
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        DHT dht = new DHT();
        try {
            dht.bind(addr);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        dht.start();
        testRemoveValueDB(dht.getContext());
    }
    
    public static void testRemoveValueDB(Context context) {
        Database db = context.getDatabase();
        KUID key = KUID.createValueID(KUID.createUnknownID().getBytes());
        byte[] value;
        try {
            value = "test".getBytes("UTF-8");
            KUID nodeId = KUID.createRandomNodeID();
            KeyValue keyValue = KeyValue.createRemoteKeyValue(key,value,nodeId,addr,null);
            db.add(keyValue);
            System.out.println(db.toString());
            keyValue = KeyValue.createRemoteKeyValue(key,new byte[0],nodeId,addr,null);
            db.add(keyValue);
            System.out.println(db.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
