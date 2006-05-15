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
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHT;
import com.limegroup.mojito.handler.request.PingRequestHandler;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.response.PingResponse;


public class ExternalAddressTest {
    
    public void testSettingExternalAddress() throws Exception {
        DHT dht1 = new DHT();
        dht1.bind(new InetSocketAddress(2000));
        Context context1 = dht1.getContext();
        
        dht1.setName("DHT-1");
        dht1.start();
        
        DHT dht2 = new DHT();
        dht2.bind(new InetSocketAddress(3000));
        Context context3 = dht2.getContext();
        
        dht2.setName("DHT-3");
        dht2.start();
        
        Thread.sleep(3000);
        
        PingRequestHandlerStub pingHandlerStub = new PingRequestHandlerStub(context3);
        Field pingHandler = MessageDispatcher.class.getDeclaredField("pingHandler");
        pingHandler.setAccessible(true);
        pingHandler.set(context3.getMessageDispatcher(), pingHandlerStub);
        
        //NetworkSettings.ALLOW_MULTIPLE_NODES.setValue(true);
        
        // BEGIN
        pingHandlerStub.externalAddress = new InetSocketAddress("10.254.0.251", 3000);
        context1.ping(new InetSocketAddress("localhost", 3000), null);
        Thread.sleep(1000);
        System.out.println(pingHandlerStub.externalAddress.equals(context1.getSocketAddress())); // false
        
        pingHandlerStub.externalAddress = new InetSocketAddress("127.0.0.1", 2000);
        context1.ping(new InetSocketAddress("localhost", 3000), null);
        Thread.sleep(1000);
        System.out.println(pingHandlerStub.externalAddress.equals(context1.getSocketAddress())); // false
        
        pingHandlerStub.externalAddress = new InetSocketAddress("10.254.0.251", 2000);
        context1.ping(new InetSocketAddress("localhost", 3000), null);
        Thread.sleep(1000);
        System.out.println(pingHandlerStub.externalAddress.equals(context1.getSocketAddress())); // true
        
        dht1.stop();
        dht2.stop();
    }
    
    public static void main(String[] args) throws Exception {
        new ExternalAddressTest().testSettingExternalAddress();
    }
    
    private static class PingRequestHandlerStub extends PingRequestHandler {
        
        public SocketAddress externalAddress;
        
        public PingRequestHandlerStub(Context context) {
            super(context);
        }

        public void handleRequest(RequestMessage message) throws IOException {
            
            SocketAddress addr = externalAddress != null ? externalAddress : message.getSourceAddress();

            System.out.println("Received Ping from " + message.getSourceAddress());
            System.out.println("Going to tell " + message.getSourceAddress() + " that its external address is " + addr);
            
            PingResponse pong = context.getMessageFactory()
                    .createPingResponse(message, addr);
    
            context.getMessageDispatcher().send(message.getSource(), pong);
        }        
    }
}
