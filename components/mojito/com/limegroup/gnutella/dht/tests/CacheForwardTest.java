/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.net.InetSocketAddress;

import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.settings.RouteTableSettings;

public class CacheForwardTest {

    public void testCacheForward() {
        KademliaSettings.setReplicationParameter(20);
        RouteTableSettings.setMaxLiveNodeFailures(3);
       
        DHT originalRequesterDHT = new DHT();
        
        DHT firstStorer = new DHT();
        
        DHT secondStorer = new DHT();
        try {
            originalRequesterDHT.bind(new InetSocketAddress("localhost",3000));
            firstStorer.bind(new InetSocketAddress("localhost",3001));
            secondStorer.bind(new InetSocketAddress("localhost",3002));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        new Thread(originalRequesterDHT,"DHT-1").start();
        new Thread(firstStorer,"DHT-2").start();
        
        try {
            firstStorer.bootstrap(originalRequesterDHT.getSocketAddress(),null);
            byte[] valueID = firstStorer.getLocalNode().getNodeID().getBytes();
            //replace with first bits of first storer to make sure it lands there first
            originalRequesterDHT.put(KUID.createValueID(valueID),"test".getBytes("UTF-8"),null);
            Thread.sleep(5000);
            new Thread(secondStorer,"DHT-3").start();
            secondStorer.bootstrap(firstStorer.getSocketAddress(),null);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        new CacheForwardTest().testCacheForward();
    }
}
