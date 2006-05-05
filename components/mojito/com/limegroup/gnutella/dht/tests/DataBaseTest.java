package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValue;

public class DataBaseTest {

    private static InetSocketAddress addr = new InetSocketAddress("localhost",3000);
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        DHT dht = new DHT();
        try {
            dht.bind(addr);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        new Thread(dht,"DHT").start();
        testRemoveValueDB(dht.getContext());
    }
    
    public static void testRemoveValueDB(Context context) {
        Database db = context.getDatabase();
        KUID key = KUID.createValueID(KUID.createUnknownID().getBytes());
        byte[] value;
        try {
            value = "test".getBytes("UTF-8");
            KUID nodeId = KUID.createRandomNodeID();
            KeyValue keyValue = KeyValue.createRemoteKeyValue(key,value,nodeId,addr,null);
            db.add(keyValue);
            System.out.println(db.toString());
            keyValue = KeyValue.createRemoteKeyValue(key,new byte[0],nodeId,addr,null);
            db.add(keyValue);
            System.out.println(db.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
