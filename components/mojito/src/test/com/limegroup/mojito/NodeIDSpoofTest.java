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
 
package com.limegroup.mojito;

import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.settings.RouteTableSettings;


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
        RouteTableSettings.MIN_RECONNECTION_TIME.setValue(0);
        
        MojitoDHT bootstrap = null, original = null, spoofer = null;
        
        try {
            bootstrap = new MojitoDHT("Bootstrap Node");
            bootstrap.bind(new InetSocketAddress("localhost", PORT));
            bootstrap.start();
            
            // The original Node
            KUID nodeId = KUID.createRandomNodeID();
            
            original = new MojitoDHT("OriginalDHT");
            MojitoHelper.setNodeID(original, nodeId);
            original.bind(new InetSocketAddress(PORT+1));
            original.start();
            original.bootstrap(bootstrap.getSocketAddress());
            
            // The spoofer Node
            spoofer = new MojitoDHT("Spoofer Node");
            MojitoHelper.setNodeID(spoofer, nodeId);
            spoofer.bind(new InetSocketAddress(PORT+2));
            spoofer.start();
            spoofer.bootstrap(bootstrap.getSocketAddress());
        
            Context context = MojitoHelper.getContext(bootstrap);
            List<ContactNode> nodes = context.getRouteTable().getAllNodes();
            for(ContactNode node : nodes) {
                assertNotEquals(spoofer.getSocketAddress(), 
                        node.getSocketAddress());
            }
        } finally {
            if (bootstrap != null) {
                bootstrap.stop();
            }
            
            if (original != null) {
                original.stop();
            }
            
            if (spoofer != null) {
                spoofer.stop();
            }
        }
        
        Thread.sleep(3000);
    }
    
    public void testReplace() throws Exception {
        RouteTableSettings.MIN_RECONNECTION_TIME.setValue(0);
        NetworkSettings.MAX_TIMEOUT.setValue(100L);
        
        MojitoDHT bootstrap = null, original = null, replacement = null;
        
        try {
            bootstrap = new MojitoDHT("Bootstrap-DHT");
            bootstrap.bind(new InetSocketAddress(PORT));
            bootstrap.start();
            
            // The original Node
            KUID nodeID = KUID.createRandomNodeID();
            
            original = new MojitoDHT("OriginalDHT");
            MojitoHelper.setNodeID(original, nodeID);
            original.bind(new InetSocketAddress(PORT+1));
            original.start();
            original.bootstrap(new InetSocketAddress("localhost", PORT));
            original.stop();
            Thread.sleep(3000);
            
            // The spoofer Node
            replacement = new MojitoDHT("ReplacementDHT");
            MojitoHelper.setNodeID(replacement, nodeID);
            replacement.bind(new InetSocketAddress(PORT+2));
            replacement.start();
            replacement.bootstrap(new InetSocketAddress("localhost", PORT));
            
            Thread.sleep(4L * NetworkSettings.MAX_TIMEOUT.getValue());
            
            Context context = MojitoHelper.getContext(bootstrap);
            List<ContactNode> nodes = context.getRouteTable().getAllNodes();
            
            boolean contains = false;
            for(ContactNode node : nodes) {
                if (node.getSocketAddress()
                        .equals(replacement.getSocketAddress())) {
                    contains = true;
                    break;
                }
            }
            
            assertTrue("Bootstrap Node does not have the new Node in its RT!", contains);
            
        } finally {
            
            if (bootstrap != null) {
                bootstrap.stop();
            }
            
            if (original != null) {
                original.stop();
            }
            
            if (replacement != null) {
                replacement.stop();
            }
        }
        Thread.sleep(3000);
    }
}
