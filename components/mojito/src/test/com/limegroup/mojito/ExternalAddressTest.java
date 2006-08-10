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
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.handler.request.PingRequestHandler;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;


public class ExternalAddressTest extends BaseTestCase {
    
    public ExternalAddressTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(ExternalAddressTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSettingExternalAddress() throws Exception {
        MojitoDHT dht1 = MojitoFactory.createDHT("DHT-1");
        dht1.bind(new InetSocketAddress(2000));
        dht1.start();
        
        MojitoDHT dht2 = MojitoFactory.createDHT("DHT-2");
        dht2.bind(new InetSocketAddress(3000));
        dht2.start();
        
        Thread.sleep(3000);
        
        PingRequestHandlerStub pingHandlerStub = new PingRequestHandlerStub((Context)dht2);
        Field pingHandler = MessageDispatcher.class.getDeclaredField("pingHandler");
        pingHandler.setAccessible(true);
        pingHandler.set(((Context)dht2).getMessageDispatcher(), pingHandlerStub);
        
        // BEGIN
        
        /*
         * First Ping: Accept any IP:Port
         */
        pingHandlerStub.externalAddress = new InetSocketAddress("10.254.0.251", 3000);
        dht1.ping(new InetSocketAddress("localhost", 3000));
        Thread.sleep(1000);
        assertEquals(pingHandlerStub.externalAddress, dht1.getContactAddress());
        
        /*
         * After 1st Ping: Two Nodes must say the same IP:Port
         */
        pingHandlerStub.externalAddress = new InetSocketAddress("www.google.com", 1234);
        dht1.ping(new InetSocketAddress("localhost", 3000));
        Thread.sleep(1000);
        assertNotEquals(pingHandlerStub.externalAddress, dht1.getContactAddress());
        
        pingHandlerStub.externalAddress = new InetSocketAddress("www.limewire.com", 80);
        dht1.ping(new InetSocketAddress("localhost", 3000));
        Thread.sleep(1000);
        assertNotEquals(pingHandlerStub.externalAddress, dht1.getContactAddress());
        
        /*
         * But Now!
         */
        pingHandlerStub.externalAddress = new InetSocketAddress("www.limewire.com", 80);
        dht1.ping(new InetSocketAddress("localhost", 3000));
        Thread.sleep(1000);
        assertEquals(pingHandlerStub.externalAddress, dht1.getContactAddress());
        
        dht1.stop();
        dht2.stop();
    }
    
    private static class PingRequestHandlerStub extends PingRequestHandler {
        
        public SocketAddress externalAddress;
        
        public PingRequestHandlerStub(Context context) {
            super(context);
        }

        public void handleRequest(RequestMessage message, SocketAddress src) throws IOException {
            
            Contact node = message.getContact();
            SocketAddress addr = externalAddress != null ? externalAddress : node.getContactAddress();

            System.out.println("Received Ping from " + node.getContactAddress());
            System.out.println("Going to tell " + node.getContactAddress() + " that its external address is " + addr);
            
            PingResponse pong = context.getMessageHelper()
                    .createPingResponse(message, addr);
    
            context.getMessageDispatcher().send(node, pong);
        }
    }
}
