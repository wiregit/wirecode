package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.request.PingRequestHandler;
import de.kapsi.net.kademlia.io.MessageDispatcher;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.response.PingResponse;
import de.kapsi.net.kademlia.settings.NetworkSettings;

public class ExternalAddressTest {
    
    public void testSettingExternalAddress() throws Exception {
        DHT dht1 = new DHT();
        dht1.bind(new InetSocketAddress(2000));
        Context context1 = dht1.getContext();
        new Thread(dht1, "DHT-1").start();
        
        DHT dht2 = new DHT();
        dht2.bind(new InetSocketAddress(3000));
        Context context3 = dht2.getContext();
        new Thread(dht2, "DHT-3").start();
        
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
        System.out.println(pingHandlerStub.externalAddress.equals(context1.getExternalSocketAddress())); // false
        
        pingHandlerStub.externalAddress = new InetSocketAddress("127.0.0.1", 2000);
        context1.ping(new InetSocketAddress("localhost", 3000), null);
        Thread.sleep(1000);
        System.out.println(pingHandlerStub.externalAddress.equals(context1.getExternalSocketAddress())); // false
        
        pingHandlerStub.externalAddress = new InetSocketAddress("10.254.0.251", 2000);
        context1.ping(new InetSocketAddress("localhost", 3000), null);
        Thread.sleep(1000);
        System.out.println(pingHandlerStub.externalAddress.equals(context1.getExternalSocketAddress())); // true
        
        dht1.close();
        dht2.close();
    }
    
    public static void main(String[] args) throws Exception {
        new ExternalAddressTest().testSettingExternalAddress();
    }
    
    private static class PingRequestHandlerStub extends PingRequestHandler {
        
        public SocketAddress externalAddress;
        
        public PingRequestHandlerStub(Context context) {
            super(context);
        }

        public void handleRequest(KUID nodeId, SocketAddress src, 
                Message message) throws IOException {
            
            SocketAddress addr = externalAddress != null ? externalAddress : src;
            
            System.out.println("Received Ping from " + src);
            System.out.println("Going to tell " + src + " that its external address is " + addr);
            
            PingResponse pong = context.getMessageFactory()
                    .createPingResponse(message.getMessageID(), addr);
    
            context.getMessageDispatcher().send(src, pong, null);
        }        
    }
}
