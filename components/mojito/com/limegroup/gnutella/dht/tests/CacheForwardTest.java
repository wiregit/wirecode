package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.net.InetSocketAddress;

import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.settings.RouteTableSettings;

public class CacheForwardTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        KademliaSettings.setReplicationParameter(20);
        RouteTableSettings.setMaxNodeFailures(3);
       
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
            byte[] valueID = secondStorer.getLocalNode().getNodeID().getBytes();
            //replace with first bits of first storer to make sure it lands there first
            originalRequesterDHT.put(KUID.createValueID(valueID),"test".getBytes("UTF-8"));
            Thread.sleep(3000);
            new Thread(secondStorer,"DHT-3").start();
            secondStorer.bootstrap(firstStorer.getSocketAddress(),null);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
        

    }

}
