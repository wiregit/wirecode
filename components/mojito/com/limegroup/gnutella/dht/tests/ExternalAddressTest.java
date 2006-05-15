package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.handler.request.PingRequestHandler;
import de.kapsi.net.kademlia.io.MessageDispatcher;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.response.PingResponse;

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
