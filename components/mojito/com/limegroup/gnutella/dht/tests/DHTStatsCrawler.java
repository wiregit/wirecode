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
import java.util.Set;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.event.StatsListener;
import de.kapsi.net.kademlia.handler.ResponseHandler;
import de.kapsi.net.kademlia.handler.response.StatsResponseHandler;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.request.FindNodeRequest;
import de.kapsi.net.kademlia.messages.request.StatsRequest;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.messages.response.StatsResponse;

public class DHTStatsCrawler implements Runnable, ResponseHandler {
    
    /** number of concurrent findNodes */
    private static final int alpha = 20;
    
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
    
    final Object lock = new Object();
    
    private int numResponses;
    
    private int timeouts;
    
    private Writer statsWriter;
    
    private Writer dbWriter;
    
    private Writer rtWriter;
    
    private boolean finished;
    
    //TODO remove:
    private int addedToQuery;
    

    /**
     * Crawl the DHT: Contact everynode in the network asking for the closest node to their own nodeId
     * 
     * @param args
     */
    public static void main(String[] args) {
        
        if (args.length == 0) {
            System.err.println("DHTStatsCrawler <port> <bootstrap host> <bootstrap port> <outputFileName>");
            System.exit(-1);
        }
        
        int port = Integer.parseInt(args[0]);
        InetSocketAddress dst = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
        
        Writer statsWriter;
        BufferedWriter dbWriter;
        BufferedWriter rtWriter;
        try {
            File statsFile = new File(args[3]);
            statsWriter = new BufferedWriter(new FileWriter(statsFile));
            File dbFile = new File(args[4]);
            dbWriter = new BufferedWriter(new FileWriter(statsFile));
            File rtFile = new File(args[5]);
            rtWriter = new BufferedWriter(new FileWriter(statsFile));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

//        statsWriter = new OutputStreamWriter(System.out); 
        DHTStatsCrawler crawler = new DHTStatsCrawler(port, dst, statsWriter, dbWriter, rtWriter);
        crawler.run();
        StatsGatherer gatherer = crawler.new StatsGatherer(statsWriter);
        new Thread(gatherer).run();
    }

    public DHTStatsCrawler(int localPort, 
            InetSocketAddress bootStrap, 
            Writer statsWriter, 
            Writer dbWriter, 
            Writer rtWriter) {
        
        this.bootstrap = bootStrap;
        this.statsWriter = statsWriter;
        this.dbWriter = dbWriter;
        this.rtWriter = rtWriter;
        DHT dht = new DHT();
        try {
            dht.bind(new InetSocketAddress(localPort));
            
            dht.setName("DHT-Crawler");
            dht.setFirewalled(true);
            dht.start();
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
                public void response(ResponseMessage response, long time) {
                    startCrawl(response.getSourceNodeID());
                }

                public void timeout(KUID nodeId, SocketAddress address, RequestMessage request, long time) {
                    System.out.println("Crawl failed: bootstrap host dead");
                    System.exit(0);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void startCrawl(KUID nodeId) {
        try {
            toQuery.add(new ContactNode(nodeId, bootstrap));
            FindNodeRequest req = context.getMessageFactory().createFindNodeRequest(bootstrap, context.getLocalNodeID());
            //and the opposite
            FindNodeRequest req2 = context.getMessageFactory().createFindNodeRequest(bootstrap, context.getLocalNodeID().invert());
            numReq += 2;
            context.getMessageDispatcher().send(bootstrap, req, this);
            context.getMessageDispatcher().send(bootstrap, req2, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleResponse(ResponseMessage message, long time) throws IOException {
        --numReq;
        ++numResponses;
        responses.add(message.getSource());

        synchronized (lock) {
            lock.notify();
        }
        FindNodeResponse response = (FindNodeResponse) message;
        Collection values = response.getValues();
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            ContactNode node = (ContactNode)it.next();
            KUID nodeID = node.getNodeID();
            if(!nodeID.equals(context.getLocalNodeID()) && 
                    !queried.contains(nodeID)) {
                boolean added = toQuery.add(node);
                if(added) {
                    ++addedToQuery;
                }
            }
        }
        sendQueries();
        
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        --numReq;
        ++timeouts;
        sendQueries();
    }

    
    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        try {
            handleTimeout(nodeId, dst, message, -1L);
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

    public long timeout() {
        return 30*1000L;
    }
    
    
    public void addTime(long time) {
    }
    
    public long time() {
        return 0L;
    }

    public void sendQueries() throws IOException{
        if(toQuery.size() == 0 && numReq == 0) {
            long delay = System.currentTimeMillis() - this.time; 
            System.out.println("Crawl finished after "+delay+" ms");
            System.out.println("Responses size: "+numResponses+", timeouts: "+timeouts);
            System.out.println("Added to query: " + addedToQuery);
            System.out.println("Queried: " + queried.size());
            synchronized (lock) { 
                finished = true; 
            }
        } else {
            for (Iterator iter = toQuery.iterator(); iter.hasNext();) {
                if(numReq > alpha) break;
                ContactNode node = (ContactNode) iter.next();
                iter.remove();
                if(queried.contains(node.getNodeID())) {
                    continue;
                }
                queried.add(node.getNodeID());

                FindNodeRequest req = context.getMessageFactory().createFindNodeRequest(node.getSocketAddress(), node.getNodeID());
                //and the other side
                FindNodeRequest req2 = context.getMessageFactory().createFindNodeRequest(node.getSocketAddress(), node.getNodeID().invert());
                numReq+=2;
                context.getMessageDispatcher().send(node, req, this);
                context.getMessageDispatcher().send(node, req2, this);
            }
        }
    }
    
    private class StatsGatherer implements Runnable, StatsListener {
        
        /** number of concurrent stats requests */
        private static final int beta = 5;
        
        private int numReq;
        
        private Set toQuery = new HashSet();
        
        private Set queried = new HashSet();
        
        private Writer statsWriter;
        
        public StatsGatherer(Writer statsWriter) {
            this.statsWriter = statsWriter;
        }
        
        public void run() {
            while(true) {
                synchronized(lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    for (Iterator iter = responses.iterator(); iter.hasNext();) {
                        ContactNode node = (ContactNode) iter.next();
                        if(!queried.contains(node)) {
                            toQuery.add(node);
                        }
                    }
                    responses.clear();

                    for (Iterator iter = toQuery.iterator(); iter.hasNext();) { 
                        if(numReq > beta) { 
                            break; 
                        } else {  
                            ContactNode node = (ContactNode) iter.next(); 
                            StatsRequest req = context.getMessageFactory().createStatsRequest(node.getSocketAddress(), 
                                    new byte[0], StatsRequest.DB); 
                            try { 
                                iter.remove(); 
                                queried.add(node);
                                ++numReq; 
                                StatsResponseHandler handler = new StatsResponseHandler(context);
                                handler.addStatsListener(this);
                                context.getMessageDispatcher().send(node.getSocketAddress(), req, handler); 
                            } catch (IOException e) { 
                                e.printStackTrace(); 
                            }
                        }
                    }
                }
            }
        }
        
        public void response(ResponseMessage response, long time) {
            numReq--;
            
            try {
                ContactNode node = response.getSource();
                String statistics = ((StatsResponse)response).getStatistics();
                
                synchronized(statsWriter) {
                    statsWriter.write("Node: " + node.getNodeID() + ", " + node.getSocketAddress());
                    statsWriter.write("\n"+statistics+"\n");
                    statsWriter.flush();
                }
                
                synchronized(lock) { 
                    if(finished && toQuery.size() == 0 && numReq == 0) { 
                        finish(); 
                    } 
                    lock.notify(); 
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void timeout(KUID nodeId, SocketAddress address, RequestMessage request, long time) {
            ++timeouts; 
            numReq--;
            if(finished)
                return;
            try {
                synchronized(statsWriter) {
                    System.out.println("Timeout from node :"+nodeId);
                    statsWriter.write("Node: "+nodeId + ", "+address+" TIMEOUT");
                    statsWriter.flush();
                }

                synchronized(lock) { 
                    if(finished && toQuery.size() == 0 && numReq == 0) { 
                        finish(); 
                    } 
                    lock.notify(); 
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        private void finish() { 
            synchronized(statsWriter) { 
                try { 
                    statsWriter.close(); 
                    System.exit(0); 
                } catch (IOException e) { 
                    e.printStackTrace(); 
                } 
            } 
        } 
    }
}
