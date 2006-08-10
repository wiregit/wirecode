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
import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;


public class CacheForwardTest extends BaseTestCase {
    
    public CacheForwardTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(CacheForwardTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testCacheForward() {
        MojitoDHT originalRequesterDHT = MojitoFactory.createDHT("DHT-1");
        
        MojitoDHT firstStorer = MojitoFactory.createDHT("DHT-2");
        
        MojitoDHT secondStorer = MojitoFactory.createDHT("DHT-3");
        try {
            originalRequesterDHT.bind(new InetSocketAddress("localhost", 3000));
            firstStorer.bind(new InetSocketAddress("localhost", 3001));
            secondStorer.bind(new InetSocketAddress("localhost", 3002));
        } catch (IOException e) {
            fail(e);
        }
        
        originalRequesterDHT.start();
        firstStorer.start();
        
        try {
            firstStorer.bootstrap(originalRequesterDHT.getContactAddress());
            
            // 
            byte[] valueID = firstStorer.getLocalNodeID().getBytes();
            //replace with first bits of first storer to make sure it lands there first
            originalRequesterDHT.put(KUID.createValueID(valueID), "test".getBytes("UTF-8"));
            Thread.sleep(1000);
            
            //try the normal store forward
            secondStorer.start();
            secondStorer.bootstrap(firstStorer.getContactAddress());

            //now change instanceID and retry -- should store forward again
            secondStorer.stop();
            Thread.sleep(1000);
            secondStorer.bind(new InetSocketAddress("localhost", 3002));
            secondStorer.start();
            secondStorer.bootstrap(firstStorer.getContactAddress());
            
            Thread.sleep(10000);
            
            //now contact host with same instanceID -- should not store forward
            System.out.println("Second storer will send ping!");
            ((Context)secondStorer).ping(((Context)firstStorer).getLocalNode());
            Thread.sleep(10000);
            
        } catch (IOException e) {
            fail(e);
        } catch (InterruptedException iex) {
            fail(iex);
        }
    }
}
