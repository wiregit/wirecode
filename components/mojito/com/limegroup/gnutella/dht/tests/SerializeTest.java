package com.limegroup.gnutella.dht.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.zip.GZIPOutputStream;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.routing.RoutingTable;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class SerializeTest {
    
    private static InetSocketAddress addr = new InetSocketAddress("localhost",3000);

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
//        testSerializeStats();
        testSerializeRT();
    }
    
    public static void testSerializePatriciaTrie() throws Exception{
        PatriciaTrie trie = new PatriciaTrie();
        for(int i = 0; i < 300; i++) {
            KUID nodeId = KUID.createRandomNodeID();
            ContactNode node = new ContactNode(nodeId, new InetSocketAddress(i));
            trie.put(nodeId, node);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzout = new GZIPOutputStream(baos);
        ObjectOutputStream out = new ObjectOutputStream(gzout);
        
        out.writeObject(trie);
        out.close();
        
        System.out.println("Size: " + baos.toByteArray().length);
        
        //System.out.println(new String(baos.toByteArray()));
    }
    
    public static void testSerializeStats() throws Exception{
        DHT dht = new DHT();
        try {
            dht.bind(addr);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        dht.start();
        
        Thread.sleep(3*1000);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzout = new GZIPOutputStream(baos);
        OutputStreamWriter out = new OutputStreamWriter(baos);
        
        dht.getContext().getDHTStats().dumpStats(out, false);
        System.out.println("Size: " + baos.toByteArray().length);
        System.out.println(new String(baos.toByteArray()));
        
        StringWriter writer = new StringWriter();
        dht.getContext().getDHTStats().dumpStats(writer,false);
        System.out.println(writer.toString());
        System.exit(0);
    }
    
    public static void testSerializeRT() throws Exception{
        DHT dht = new DHT();
        try {
            dht.bind(addr);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        dht.start();
        
//        Thread.sleep(3*1000);
        RoutingTable rtable = dht.getContext().getRouteTable();
        for (int i = 0; i < 5000; i++) {
            ContactNode node = new ContactNode(KUID.createRandomNodeID(),new InetSocketAddress(i));
            rtable.add(node,false);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzout = new GZIPOutputStream(baos);
        OutputStreamWriter out = new OutputStreamWriter(gzout);
        
        dht.getContext().getDHTStats().dumpRouteTable(out);
        out.flush();
        baos.flush();
        out.close();
        System.out.println(new String(baos.toByteArray()));
        System.out.println("Size: " + baos.toByteArray().length);
        System.exit(0);
    }

}
