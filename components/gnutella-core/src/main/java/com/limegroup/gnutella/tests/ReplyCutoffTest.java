package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.tests.stubs.*;
import com.limegroup.gnutella.gui.*;

import java.util.Properties;
import com.sun.java.util.collections.*;
import java.io.*;

/**
 * Out-of-process test to check that runaway query replies are not routed.
 */
public class ReplyCutoffTest {
    /** MessageRouter.MAX_REPLY_ROUTE_BYTES.  Intentionally duplicated. */
    static final int MAX_REPLY_ROUTE_BYTES=50000; 
    static final int PORT=6347;
    static final int TIMEOUT=2000;
    static Connection c1;
    static Connection c2;

    public static void main(String args[]) {
        System.out.println(
            "Please make sure you are not running anything on port "+PORT+".\n"
           +"If this test fails, try disabling connection watchdogs");

        //Bring up application.  This code stolen from AcceptLimitTest.
        //TODO: this is useful for testing.  Factor this.
        SettingsManager settings=SettingsManager.instance();
        settings.setPort(PORT);
        settings.setDirectories(new File[0]);
        settings.setUseQuickConnect(false);
        settings.setQuickConnectHosts(new String[0]);
        settings.setConnectOnStartup(false);
        settings.setEverSupernodeCapable(true);
        settings.setDisableSupernodeMode(false);
        settings.setForceSupernodeMode(false);
        settings.setKeepAlive(6);
        ActivityCallback callback=new ActivityCallbackStub();
        FileManager files=new FileManagerStub();
        CountingMessageRouter router=new CountingMessageRouter();
        RouterService rs=new RouterService(callback,
                                           router,
                                           files,
                                           new DummyAuthenticator());
        rs.initialize();
        rs.clearHostCatcher();
        try {
            rs.setKeepAlive(6);
        } catch (BadConnectionSettingException e) { 
            e.printStackTrace();
            Assert.that(false); 
        }

        try {
            c1=new Connection("localhost", PORT);
            c1.initialize();
            c2=new Connection("localhost", PORT);
            c2.initialize();
        } catch (IOException e) {
            System.out.println("Couldn't establish connections.");
            System.exit(1);
        }

        try {
            testRouteCutoff();
            testRouteToMe(rs, router);
        } catch (BadPacketException e) {
            Assert.that(false, "Received bad packet");
        } catch (IOException e) {
            Assert.that(false, "Socket closed");
        }
    }

    private static void testRouteCutoff() throws IOException, BadPacketException {
        System.out.println("-Testing reply cutoff");
        //1. Send query from c1->HOST->c2
        QueryRequest query=new QueryRequest((byte)3, 0, "test ReplyCutoffTest");
        c1.send(query);
        c1.flush();

        int bytesRead=0;
        int messageLength=0;
        //The following steps are interleaved to prevent buffers from
        //overflowing.  Note that the loop exits from the break statement during
        //normal operation, not the while condition.
        while (bytesRead<=2*MAX_REPLY_ROUTE_BYTES) { 
            //2. Send replies from c2->HOST->c1
            QueryReply reply=new QueryReply(query.getGUID(), 
                                            (byte)5,
                                            6346, new byte[4],
                                            0, new Response[0],
                                            new byte[16]);
            messageLength=reply.getTotalLength();
            c2.send(reply);
            c2.flush();
            
            //3. Count replies read from c1.
            try {
                Message m=c1.receive(TIMEOUT);
                if (m instanceof QueryReply) 
                    bytesRead+=m.getTotalLength();
            } catch (InterruptedIOException e) {
                break;
            }
        }            

        Assert.that(bytesRead<=MAX_REPLY_ROUTE_BYTES, 
                    "Too many bytes: "+bytesRead);
        Assert.that(bytesRead+messageLength
                        > MAX_REPLY_ROUTE_BYTES, 
                    "Too few bytes: "+bytesRead);
    }

    private static void testRouteToMe(RouterService rs, 
                                      CountingMessageRouter router)
                                      throws IOException {
        System.out.println("-Testing replies to me (no cutoff)");
        //1. Initiate query
        byte[] guid=rs.newQueryGUID();
        rs.query(guid, "test", 0, null);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { }

        //2. Send back results
        int bytesSent=0;
        while (bytesSent<=2*MAX_REPLY_ROUTE_BYTES) {
            QueryReply reply=new QueryReply(guid,
                                            (byte)5,
                                            6346, new byte[4],
                                            0, new Response[0],
                                            new byte[16]);
            bytesSent+=reply.getTotalLength();
            c1.send(reply);
            c1.flush();
        }
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { }
        Assert.that(router.getReplyBytes()==bytesSent,
            "Got "+router.getReplyBytes()+" bytes instead of "+bytesSent);
    }
}

class CountingMessageRouter extends MessageRouterStub {
    private int _bytes=0;

    protected synchronized void handleQueryReplyForMe(
            QueryReply queryReply, ManagedConnection receivingConnection) {
        _bytes+=queryReply.getTotalLength();                
    }

    public int getReplyBytes() { 
        return _bytes;
    }
}
