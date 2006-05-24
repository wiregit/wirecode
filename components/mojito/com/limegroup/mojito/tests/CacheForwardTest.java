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

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;


public class CacheForwardTest {

    public void testCacheForward() {
        MojitoDHT originalRequesterDHT = new MojitoDHT("DHT-1");
        
        MojitoDHT firstStorer = new MojitoDHT("DHT-2");
        
        MojitoDHT secondStorer = new MojitoDHT("DHT-3");
        try {
            originalRequesterDHT.bind(new InetSocketAddress("localhost",3000));
            firstStorer.bind(new InetSocketAddress("localhost",3001));
            secondStorer.bind(new InetSocketAddress("localhost",3002));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        originalRequesterDHT.start();
        firstStorer.start();
        
        try {
            firstStorer.bootstrap(originalRequesterDHT.getSocketAddress(),null);
            byte[] valueID = firstStorer.getLocalNode().getNodeID().getBytes();
            //replace with first bits of first storer to make sure it lands there first
            originalRequesterDHT.put(KUID.createValueID(valueID),"test".getBytes("UTF-8"),null);
            Thread.sleep(1000);
            //try the normal store forward
            secondStorer.start();
            secondStorer.bootstrap(firstStorer.getSocketAddress(),null);
            Thread.sleep(5000);
            //now change instanceID and retry -- should store forward again
            secondStorer.stop();
            Thread.sleep(1000);
            secondStorer.bind(new InetSocketAddress("localhost",3002));
            secondStorer.start();
            secondStorer.bootstrap(firstStorer.getSocketAddress(),null);
            Thread.sleep(10000);
            //now contact host with same instanceID -- should not store forward
            System.out.println("Second storer will send ping!");
            secondStorer.getContext().ping(firstStorer.getLocalNode());
            Thread.sleep(10000);
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
