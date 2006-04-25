package com.limegroup.gnutella.dht.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.dht.statistics.DHTNodeStat;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.BootstrapListener;
import de.kapsi.net.kademlia.event.FindNodeListener;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.handler.ResponseHandler;
import de.kapsi.net.kademlia.handler.response.PingResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.FindNodeRequest;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.util.CollectionUtils;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class DHTStatsCrawler implements Runnable, ResponseHandler {
    
    private static final int alpha = 10;
    
    /** Trie of <KUID,ContactNode> from whom we got replies */
    private Set responses = new HashSet();
    
    /** Trie of <KUID,ContactNode> to query */
    private Set toQuery = new HashSet();
    
    /** Set of queried KUIDs */
    private Set queried = new HashSet();

    private final Context context;
    
    private int numReq;
    
    private long time;
    
    private InetSocketAddress bootstrap;
    

    /**
     * Crawl the DHT: Contact everynode in the network asking for the closest node to their own nodeId
     * 
     * @param args
     */
    public static void main(String[] args) {
        
        if (args.length == 0) {
            System.err.println("PlanetLab <port> <bootstrap host> <bootstrap port> <outputFileName>");
            System.exit(-1);
        }
        
        int port = Integer.parseInt(args[0]);
        InetSocketAddress dst = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
        BufferedWriter statsWriter;
        try {
            File statsFile = new File(args[3]);
            statsWriter = new BufferedWriter(new FileWriter(statsFile));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        DHTStatsCrawler crawler = new DHTStatsCrawler(port, dst, statsWriter);
        crawler.run();
    }

    public DHTStatsCrawler(int localPort, InetSocketAddress bootStrap, Writer statsWriter) {
        this.bootstrap = bootStrap;
        DHT dht = new DHT();
        try {
            dht.bind(new InetSocketAddress(localPort));
            Thread t = new Thread(dht, "DHT-Crawler");
            t.setDaemon(false);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        this.context = dht.getContext();
    }
    
    public void run() {
        time = System.currentTimeMillis();
        queried.add(context.getLocalNodeID());
        try {
            context.ping(bootstrap, new PingListener() {
                public void pingSuccess(KUID nodeId, SocketAddress address, long time) {
                    startCrawl(nodeId);
                }

                public void pingTimeout(KUID nodeId, SocketAddress address) {
                    System.out.println("Crawl failed: bootstrap host dead");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void startCrawl(KUID nodeId) {
        try {
            toQuery.add(new ContactNode(nodeId,bootstrap));
            FindNodeRequest req = context.getMessageFactory().createFindNodeRequest(context.getLocalNodeID());
            ++numReq;
            context.getMessageDispatcher().send(bootstrap, req, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleResponse(KUID nodeId, SocketAddress src, Message message, long time) throws IOException {
        --numReq;
        responses.add(new ContactNode(nodeId,src));
        FindNodeResponse response = (FindNodeResponse) message;
        Collection values = response.getValues();
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            ContactNode node = (ContactNode)it.next();
            KUID nodeID = node.getNodeID();
            if(!nodeID.equals(context.getLocalNodeID()) && 
                    !queried.contains(nodeID)) {
                boolean added = toQuery.add(node);
            }
        }
        sendQueries();
        
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, long time) throws IOException {
        --numReq;
        sendQueries();
    }

    public long timeout() {
        return 10*1000L;
    }
    
    public void sendQueries() throws IOException{
        if(toQuery.size() == 0 && numReq == 0) {
            long delay = System.currentTimeMillis() - this.time; 
            System.out.println("Crawl finished after "+delay+" ms");
            System.out.println("Responses size: "+responses.size());
            System.exit(0);
        } else {
            for (Iterator iter = toQuery.iterator(); iter.hasNext();) {
                if(numReq > alpha) break;
                ContactNode node = (ContactNode) iter.next();
                iter.remove();
                if(queried.contains(node.getNodeID())) {
                    continue;
                }
                queried.add(node.getNodeID());
                FindNodeRequest req = context.getMessageFactory().createFindNodeRequest(node.getNodeID());
                ++numReq;
                context.getMessageDispatcher().send(node.getSocketAddress(), req, this);
            }
        }
    }
}
