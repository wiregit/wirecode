package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;
import java.net.*;
import java.util.Date;
import com.sun.java.util.collections.NoSuchElementException;

/**
 * Unit test for the HostCatcher, since it's too large and complicated
 * to fit into HostCatcher.main.  Syntax:
 *
 * <pre>
 * HostCatcherTest <EXPIRE_TIME> <RETRY_TIME> <CONNECT_TIME>
 * <pre>
 */
public class HostCatcherTest {
    /** The number of spurious wake-ups to tolerate. */
    private static final int WAIT_TIME=5000;

    /** Wraps the host and port into a dummy pong */
    static PingReply reply(String host, int port) {
        byte[] guid=new byte[16];
        byte[] ip=null;
        try {
            ip=InetAddress.getByName(host).getAddress();
        } catch (UnknownHostException e) {
            Assert.that(false);
        }
        return new PingReply(guid, (byte)3, port, ip, 0L, 0L);
    }
    private static PingReply reply(String host) {
        return reply(host, 6346);
    }

    /** 
     * Returns an endpoint from hc, swallowing InterruptedException. 
     */
    private static Endpoint waitForEndpoint(HostCatcher hc) {   
//          long start=(new Date()).getTime();
//          synchronized (hc) {
//              while (true) {
//                  try {
//                      return hc.getAnEndpoint();
//                  } catch (NoSuchElementException e) { }

//                  //Wait...if we haven't used up all the time.
//                  long now=(new Date()).getTime();
//                  long elapsed=now-start;
//                  if (elapsed > WAIT_TIME)
//                      Assert.that(false, "Timeout waiting for endpoint");
//                  try {
//                      //Nothing available
//                      hc.wait(WAIT_TIME-elapsed);
//                  } catch (InterruptedException e) {
//                      Assert.that(false, "InterruptedException");
//                  }
//              }
//          }
        try {
            return hc.getAnEndpoint();
        } catch (InterruptedException e) {
            Assert.that(false);
            return null;
        }
    }

    public static void main(String args[]) {
        int EXPIRE_TIME=0, RETRY_TIME=0, CONNECT_TIME=0;        
        try {
            EXPIRE_TIME=Integer.valueOf(args[0]).intValue();
            RETRY_TIME=Integer.valueOf(args[1]).intValue();
            CONNECT_TIME=Integer.valueOf(args[2]).intValue();
        } catch (Exception e) {
            System.err.println("Syntax: HostCatcherTest "
                +"<EXPIRE_TIME> <RETRY_TIME> <CONNECT_TIME>,");
            System.err.println(
                 "        where *TIME is the value given in HostCatcher");
            System.exit(1);
        }

        System.out.println(
            "WARNING: you may want to adjust timeouts to lower values");
        System.out.println(
            "WARNING: make sure HostCatcher.clear() interrupts threads");

        //Stubs galore
        ActivityCallback callback=new ActivityCallbackStub();
        Acceptor acceptor=new Acceptor(0, callback);
        MessageRouter router=new MessageRouterStub();
        ConnectionManager manager=new ConnectionManager(callback);
        ManagedConnection receiver=null;
        SettingsManager settings=SettingsManager.instance();
        settings.setKeepAlive(0);
        settings.setUseQuickConnect(true);
        HostCatcher hc=new HostCatcher(callback);
        hc.initialize(acceptor, manager);
        manager.initialize(router, hc);
        router.initialize(acceptor, manager, hc);
        Endpoint e=null, e2=null;
        TestPongCache cache=null;
        try {
            cache=new TestPongCache(6346);
        } catch (IOException exc) {
            System.err.println("Couldn't start pong cache.");
            System.exit(1);
        }
        cache.listen();
 
        //1. Quick-connect enabled but no quick connect hosts.  Should eventually
        //give normal pongs.
        System.out.println("****************** Test 1 *****************");
        hc.clear();
        settings.setQuickConnectHosts(new String[0]);
        hc.spy(reply("192.168.0.1"), receiver);
        hc.spy(reply("18.239.0.144"), receiver);
        hc.spy(reply("192.168.0.2"), receiver);
        e=waitForEndpoint(hc);
        Assert.that(e.getHostname().equals("18.239.0.144"));
        e=waitForEndpoint(hc);
        Assert.that(e.getHostname().startsWith("192.168.0"));
        e=waitForEndpoint(hc);
        Assert.that(e.getHostname().startsWith("192.168.0")); 

        //2.  Normal pong cache.
        System.out.println("****************** Test 2 *****************");
        cache.reset();
        hc.clear();
        settings.setQuickConnectHosts(new String[] {"localhost:6346"});
        //These should never be given out.
        hc.spy(reply("192.168.0.1"), receiver);
        hc.spy(reply("18.239.0.144"), receiver);
        hc.spy(reply("192.168.0.2"), receiver);
        //Contact server for first, but not for second
        e=waitForEndpoint(hc);
        Assert.that(e.getHostname().equals("0.0.0.0") 
                      || e.getHostname().equals("0.0.0.1")
                      || e.getHostname().equals("0.0.0.2"));
        e2=waitForEndpoint(hc);
        try {
            System.out.println(
                "HostCatcherTest: sleeping for a while.  Be patient, please.");
            Thread.sleep(EXPIRE_TIME/2);
        } catch (InterruptedException exc) { }
        Assert.that(e.getHostname().equals("0.0.0.0") 
                      || e.getHostname().equals("0.0.0.1")
                      || e.getHostname().equals("0.0.0.2"));
        Assert.that(! e2.getHostname().equals(e.getHostname()));
        //Timeout.  Contact server again.
        try {
            System.out.println(
                "HostCatcherTest: sleeping for a while.  Be patient, please.");
            Thread.sleep(EXPIRE_TIME+1000);
        } catch (InterruptedException exc) { }
        e=waitForEndpoint(hc);
        Assert.that(e.getHostname().equals("0.0.0.3") 
                    || e.getHostname().equals("0.0.0.4")
                    || e.getHostname().equals("0.0.0.5"));
        e2=waitForEndpoint(hc);
        Assert.that(e.getHostname().equals("0.0.0.3") 
                    || e.getHostname().equals("0.0.0.4")
                    || e.getHostname().equals("0.0.0.5"));
        Assert.that(! e2.getHostname().equals(e.getHostname()));

        //3. Unreachable pong cache.  (Use old points.)
        System.out.println("****************** Test 3 *****************");
        cache.reset();
        hc.clear();
        settings.setQuickConnectHosts(new String[] {"badserver:6346"});
        hc.spy(reply("192.168.0.1"), receiver);
        hc.spy(reply("18.239.0.144"), receiver);
        hc.spy(reply("192.168.0.2"), receiver);
        e=waitForEndpoint(hc);
        Assert.that(e.getHostname().equals("18.239.0.144"));
        e=waitForEndpoint(hc);
        Assert.that(e.getHostname().startsWith("192.168.0"));
        e=waitForEndpoint(hc);
        Assert.that(e.getHostname().startsWith("192.168.0")); 

        //4. First pong cache unreachable, second isn't.
        System.out.println("****************** Test 4 *****************");
        cache.reset();
        hc.clear();
        settings.setQuickConnectHosts(
            new String[] {"localhost:0", "localhost:6346"});
        //These should never be given out.
        hc.spy(reply("192.168.0.1"), receiver);
        hc.spy(reply("18.239.0.144"), receiver);
        hc.spy(reply("192.168.0.2"), receiver);
        //Unless timeout to localhost:0 is slow, this should work.
        e=waitForEndpoint(hc);
        Assert.that(e.getHostname().equals("0.0.0.0") 
                      || e.getHostname().equals("0.0.0.1")
                      || e.getHostname().equals("0.0.0.2"));
        e2=waitForEndpoint(hc);
    }
}

/**
 * A very simple (non-threaded) pong cache for testing.
 * Fake hosts 0.0.0.1, 0.0.0.2, 0.0.0.3, etc. are given out.
 */
class TestPongCache {
    private ServerSocket serverSocket;
    private int nextIndex=0;
    private int replies=3;

    /** Creates a new pong cache to listen on the following port. 
     *  Throws IOException if the port is not available.
     *  Doesn't actually start until listen() is called.
     */
    TestPongCache(int port) throws IOException {
        this.serverSocket=new ServerSocket(port);
    }

    /** Starts the listen thread, returning immediately. */
    public synchronized void listen() {
        Thread t=new ListenThread();
        t.setDaemon(true);
        t.start();
        Thread.yield();
    }

    /** Resets the indices given out. */
    public synchronized void reset() {
        this.nextIndex=0;
    }

    private class ListenThread extends Thread {
        public void run() {
            while (true) {
                //1. Accept incoming connection
                Connection c=null;
                try {
                    Socket socket=serverSocket.accept();
                    InputStream in=socket.getInputStream();
                    //Skip "GNUTELLA "
                    for (int i=0; i<9; i++) {
                        int ch=in.read();
                        if (ch==-1) throw new IOException();
                    }
                    c=new Connection(socket);
                    c.initialize();
                    //System.out.println("LT: got connection");
                } catch (IOException e) {
                    //System.out.println("LT: couldn't accept connection");
                    continue;
                }

                //2. Wait for ping...
                while (true) {
                    try {
                        Message m=c.receive();
                        //System.out.println("LT: read "+m);
                        if (! (m instanceof PingRequest))
                            continue;
                        
                        //...and send pongs                        
                        //System.out.println("LT: got ping");
                        for (int i=0; i<replies; i++) {
                            byte[] ip=null;
                            synchronized (TestPongCache.this) {
                                String host="0.0.0."+nextIndex;
                                nextIndex++;
                                try {
                                    ip=InetAddress.getByName(host).getAddress();
                                } catch (UnknownHostException e) {
                                    Assert.that(false);
                                }
                            }                            
                            PingReply reply=new PingReply(m.getGUID(),
                                                          (byte)3,
                                                          6346, ip,
                                                          0, 0);
                            c.send(reply);
                        }
                        c.flush();
                        break;
                    } catch (BadPacketException e) {
                        //System.out.println("LT: got bad packet");
                        Assert.that(false);
                    } catch (IOException e) {
                        //System.out.println("LT: connection closed while reading");
                        break;
                    }                        
                }

                //3. Close connection
                System.out.println("LT: closing connection");
                c.close();
            }
        }
    }
}

