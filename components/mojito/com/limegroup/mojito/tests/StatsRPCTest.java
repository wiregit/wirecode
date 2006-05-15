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
import java.net.SocketAddress;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.DHT;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.StatsListener;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.request.StatsRequest;
import com.limegroup.mojito.messages.response.StatsResponse;


public class StatsRPCTest {

    
    
    /**
     * @param args
     */
    public static void main(String[] args) {

        int port = 4000;
        try {
            System.out.println("Starting stats server");
            DHT dht = new DHT("DHT-StatsServer");
            SocketAddress sac = new InetSocketAddress("localhost",port); 
            dht.bind(sac);
            dht.start();
            System.out.println("Stats server is ready");
            System.out.println("Starting Node");
            SocketAddress sac2 = new InetSocketAddress("localhost",port+1);
            KUID nodeID = KUID.createRandomNodeID(sac2);
            final DHT dht2 = new DHT("DHT-2");
            dht2.bind(sac2,nodeID);
            dht2.start();
            
            dht2.bootstrap(sac);
            
            StatsListener listener = new StatsListener() {
                public void response(ResponseMessage response, long time) {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append("*** Stats to ").append(response.getSource())
                        .append(" succeeded in ").append(time).append("ms\n");
                    buffer.append(((StatsResponse)response).getStatistics());
                    System.out.println(buffer.toString());
                }

                public void timeout(KUID nodeId, SocketAddress address, RequestMessage request, long time) {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append("*** Stats to ").append(ContactNode.toString(nodeId, address))
                        .append(" failed with timeout");
                    System.out.println(buffer.toString());
                }
            };
            
            dht.stats(sac2, StatsRequest.STATS, listener);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
