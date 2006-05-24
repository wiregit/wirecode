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

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.settings.NetworkSettings;


public class NodeIDSpoofTest extends BaseTestCase {
    
    private static final int PORT = 3000;
    
    public NodeIDSpoofTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NodeIDSpoofTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSpoof() throws Exception {
        MojitoDHT bootstrap = new MojitoDHT("Bootstrap-DHT");
        bootstrap.bind(new InetSocketAddress(PORT));
        bootstrap.start();
        
        // The original Node
        KUID nodeID = KUID.createRandomNodeID(new InetSocketAddress(PORT+1));
        MojitoDHT original = new MojitoDHT("OriginalDHT");
        original.bind(new InetSocketAddress(PORT+1), nodeID);
        original.start();
        original.bootstrap(new InetSocketAddress("localhost", PORT));
        
        // The spoofer Node
        MojitoDHT spoofer = new MojitoDHT("SpooferDHT");
        spoofer.bind(new InetSocketAddress(PORT+2), nodeID);
        spoofer.start();
        spoofer.bootstrap(new InetSocketAddress("localhost", PORT));
        
        Context context = MojitoHelper.getContext(bootstrap);
        List nodes = context.getRouteTable().getAllNodes();
        
        System.out.println(nodes);
        
        for(Iterator it = nodes.iterator(); it.hasNext(); ) {
            ContactNode node = (ContactNode)it.next();
            
            assertNotEquals(spoofer.getSocketAddress(), 
                    node.getSocketAddress());
        }
        
        bootstrap.stop();
        original.stop();
        spoofer.stop();
        Thread.sleep(3000);
    }
    
    public void testReplace() throws Exception {
        MojitoDHT bootstrap = new MojitoDHT("Bootstrap-DHT");
        bootstrap.bind(new InetSocketAddress(PORT));
        bootstrap.start();
        
        // The original Node
        KUID nodeID = KUID.createRandomNodeID(new InetSocketAddress(PORT+1));
        MojitoDHT original = new MojitoDHT("OriginalDHT");
        original.bind(new InetSocketAddress(PORT+1), nodeID);
        original.start();
        original.bootstrap(new InetSocketAddress("localhost", PORT));
        original.stop();
        Thread.sleep(3000);
        
        // The spoofer Node
        MojitoDHT replacement = new MojitoDHT("ReplacementDHT");
        replacement.bind(new InetSocketAddress(PORT+2), nodeID);
        replacement.start();
        replacement.bootstrap(new InetSocketAddress("localhost", PORT));
        Thread.sleep(2L * NetworkSettings.MAX_TIMEOUT.getValue());
        
        Context context = MojitoHelper.getContext(bootstrap);
        List nodes = context.getRouteTable().getAllNodes();
        System.out.println(nodes);
        boolean contains = false;
        for(Iterator it = nodes.iterator(); it.hasNext(); ) {
            ContactNode node = (ContactNode)it.next();
            
            if (node.getSocketAddress()
                    .equals(replacement.getSocketAddress())) {
                contains = true;
                break;
            }
        }
        
        assertTrue("Bootstrap Node does not have the new Node in its RT!", contains);
        
        bootstrap.stop();
        original.stop();
        replacement.stop();
        Thread.sleep(3000);
    }
}
