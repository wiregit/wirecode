package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.guess.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.downloader.*;
import com.bitzi.util.*;

import junit.framework.*;
import java.util.Properties;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class ClientSideOOBRequeryTest 
    extends com.limegroup.gnutella.util.BaseTestCase {
    private static final int PORT=6669;
    private static final int TIMEOUT=3000;
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    private static Connection[] testUPs = new Connection[4];
    private static RouterService rs;

    private static MyActivityCallback callback;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket[] UDP_ACCESS;

    public ClientSideOOBRequeryTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideOOBRequeryTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void doSettings() {
        ConnectionSettings.PORT.setValue(PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		ConnectionSettings.NUM_CONNECTIONS.setValue(0);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;mp3");
        // get the resource file for com/limegroup/gnutella
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        File mp3 = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/mp3/mpg1layIII_0h_58k-VBRq30_frame1211_44100hz_joint_XingTAG_sample.mp3");
        // now move them to the share dir
        CommonUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        CommonUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
        CommonUtils.copy(mp3, new File(_sharedDir, "metadata.mp3"));
        // make sure results get through
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(-2);
    }        
    
    public static void globalSetUp() throws Exception {
        doSettings();

        callback=new MyActivityCallback();
        rs=new RouterService(callback);
        assertEquals("unexpected port",
            PORT, ConnectionSettings.PORT.getValue());
        rs.start();
        rs.clearHostCatcher();
        rs.connect();
        Thread.sleep(1000);
        assertEquals("unexpected port",
            PORT, ConnectionSettings.PORT.getValue());
    }        
    
    public void setUp() throws Exception  {
        doSettings();
    }
     
     private static Connection connect(RouterService rs, int port, 
                                       boolean ultrapeer) 
         throws Exception {
         ServerSocket ss=new ServerSocket(port);
         rs.connectToHostAsynchronously("127.0.0.1", port);
         Socket socket = ss.accept();
         ss.close();
         
         socket.setSoTimeout(3000);
         InputStream in=socket.getInputStream();
         String word=readWord(in);
         if (! word.equals("GNUTELLA"))
             throw new IOException("Bad word: "+word);
         
         HandshakeResponder responder;
         if (ultrapeer) {
             responder = new UltrapeerResponder();
         } else {
             responder = new OldResponder();
         }
         Connection con = new Connection(socket, responder);
         con.initialize();
         replyToPing(con, ultrapeer);
         return con;
     }
     
     /**
      * Acceptor.readWord
      *
      * @modifies sock
      * @effects Returns the first word (i.e., no whitespace) of less
      *  than 8 characters read from sock, or throws IOException if none
      *  found.
      */
     private static String readWord(InputStream sock) throws IOException {
         final int N=9;  //number of characters to look at
         char[] buf=new char[N];
         for (int i=0 ; i<N ; i++) {
             int got=sock.read();
             if (got==-1)  //EOF
                 throw new IOException();
             if ((char)got==' ') { //got word.  Exclude space.
                 return new String(buf,0,i);
             }
             buf[i]=(char)got;
         }
         throw new IOException();
     }

     private static void replyToPing(Connection c, boolean ultrapeer) 
             throws Exception {
        // respond to a ping iff one is given.
        Message m = null;
        byte[] guid;
        try {
            while (!(m instanceof PingRequest)) {
                m = c.receive(500);
            }
            guid = ((PingRequest)m).getGUID();            
        } catch(InterruptedIOException iioe) {
            //nothing's coming, send a fake pong anyway.
            guid = new GUID().bytes();
        }
        
        Socket socket = (Socket)PrivilegedAccessor.getValue(c, "_socket");
        PingReply reply = 
        PingReply.createExternal(guid, (byte)7,
                                 socket.getLocalPort(), 
                                 ultrapeer ? ultrapeerIP : oldIP,
                                 ultrapeer);
        reply.hop();
        c.send(reply);
        c.flush();
     }

    ///////////////////////// Actual Tests ////////////////////////////

    // MUST RUN THIS TEST FIRST
    public void testNoDownloadQueryDonePurge() throws Exception {
        DatagramPacket pack = null;
        UDP_ACCESS = new DatagramSocket[10];
        for (int i = 0; i < UDP_ACCESS.length; i++)
            UDP_ACCESS[i] = new DatagramSocket();

        for (int i = 0; i < testUPs.length; i++) {
            testUPs[i] = connect(rs, 6355+i, true);
            assertTrue("should be open", testUPs[i].isOpen());
            assertTrue("should be up -> leaf",
                testUPs[i].isSupernodeClientConnection());
            drain(testUPs[i], 100);
            // OOB client side needs server side leaf guidance
            testUPs[i].send(MessagesSupportedVendorMessage.instance());
            testUPs[i].flush();
        }

        // first we need to set up GUESS capability
        // ----------------------------------------
        // set up solicited UDP support
        {
            drainAll();
            PingReply pong = 
                PingReply.create(GUID.makeGuid(), (byte) 4,
                                 UDP_ACCESS[0].getLocalPort(), 
                                 InetAddress.getLocalHost().getAddress(), 
                                 10, 10, true, 900, true);
            testUPs[0].send(pong);
            testUPs[0].flush();

            // wait for the ping request from the test UP
            UDP_ACCESS[0].setSoTimeout(10000);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS[0].receive(pack);
            }
            catch (IOException bad) {
               fail("Did not get ping", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            PingRequest ping = (PingRequest) Message.read(in);
            
            // send the pong in response to the ping
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pong = PingReply.create(ping.getGUID(), (byte) 4,
                                    UDP_ACCESS[0].getLocalPort(), 
                                    InetAddress.getLocalHost().getAddress(), 
                                    10, 10, true, 900, true);
            pong.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      pack.getAddress(), pack.getPort());
            UDP_ACCESS[0].send(pack);
        }

        // set up unsolicited UDP support
        {
            // resend this to start exchange
            testUPs[0].send(MessagesSupportedVendorMessage.instance());
            testUPs[0].flush();

            byte[] cbGuid = null;
            int cbPort = -1;
            while (cbGuid == null) {
                try {
                    Message m = testUPs[0].receive(TIMEOUT);
                    if (m instanceof UDPConnectBackVendorMessage) {
                        UDPConnectBackVendorMessage udp = 
                            (UDPConnectBackVendorMessage) m;
                        cbGuid = udp.getConnectBackGUID().bytes();
                        cbPort = udp.getConnectBackPort();
                    }
                }
                catch (Exception ie) {
                    fail("did not get the UDP CB message!", ie);
                }
            }

            // ok, now just do a connect back to the up so unsolicited support
            // is all set up
            PingRequest pr = new PingRequest(cbGuid, (byte) 1, (byte) 0);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pr.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUPs[0].getInetAddress(), cbPort);
            UDP_ACCESS[0].send(pack);
        }

        // you also have to set up TCP incoming....
        {
            Socket sock = null;
            OutputStream os = null;
            try {
                sock = Sockets.connect(InetAddress.getLocalHost().getHostAddress(), 
                                       PORT, 12);
                os = sock.getOutputStream();
                os.write("\n\n".getBytes());
            } catch (IOException ignored) {
            } catch (SecurityException ignored) {
            } catch (Throwable t) {
                ErrorService.error(t);
            } finally {
                if(sock != null)
                    try { sock.close(); } catch(IOException ignored) {}
                if(os != null)
                    try { os.close(); } catch(IOException ignored) {}
            }
        }        

        // ----------------------------------------

        Thread.sleep(250);
        // we should now be guess capable and tcp incoming capable....
        assertTrue(rs.isGUESSCapable());
        assertTrue(rs.acceptedIncomingConnection());
        
        keepAllAlive(testUPs);
        // clear up any messages before we begin the test.
        drainAll();

        Message m = null;

        byte[] guid = rs.newQueryGUID();
        rs.query(guid, "whatever");
        // i need to pretend that the UI is showing the user the query still
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            (QueryRequest) getFirstInstanceOfMessageType(testUPs[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // ok, the leaf is sending OOB queries - good stuff, now we should send
        // a lot of results back and make sure it buffers the bypassed OOB ones
        for (int i = 0; i < testUPs.length; i++) {
            Response[] res = new Response[200];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10+j+i, 10+j+i, "whatever "+ j + i);
            m = new QueryReply(qr.getGUID(), (byte) 1, 6355, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);
            testUPs[i].send(m);
            testUPs[i].flush();
        }
        
        // wait for processing
        Thread.sleep(2000);

        for (int i = 0; i < UDP_ACCESS.length; i++) {
            ReplyNumberVendorMessage vm = 
                new ReplyNumberVendorMessage(new GUID(qr.getGUID()), i+1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUPs[0].getInetAddress(), PORT);
            UDP_ACCESS[i].send(pack);
        }

        // wait for processing
        Thread.sleep(1000);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(1, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNotNull(endpoints);
            assertEquals(UDP_ACCESS.length, endpoints.size());
        }

        {
            // now we should make sure MessageRouter clears the map
            rs.stopQuery(new GUID(qr.getGUID()));
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(0, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNull(endpoints);
        }
        callback.clearGUID();
    }


    public void testDownloadDoneQueryDonePurge() throws Exception {

        keepAllAlive(testUPs);
        // clear up any messages before we begin the test.
        drainAll();

        // luckily there is hacky little way to go through the download paces -
        // download from yourself :) .

        Message m = null;

        byte[] guid = rs.newQueryGUID();
        rs.query(guid, "berkeley");
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            (QueryRequest) getFirstInstanceOfMessageType(testUPs[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // just return ONE real result and the rest junk
        Response resp = null;
        QueryReply reply = null;
        {
            // get a correct response object
            QueryRequest qrTemp = QueryRequest.createQuery("berkeley");
            testUPs[0].send(qrTemp);
            testUPs[0].flush();

            reply = (QueryReply) getFirstInstanceOfMessageType(testUPs[0],
                                                               QueryReply.class);
            assertNotNull(reply);
            resp = (Response) (reply.getResultsAsList()).get(0);

        }
        assertNotNull(reply);
        assertNotNull(resp);
        Response[] res = new Response[] { resp };

        // this isn't really needed but just for completeness send it back to 
        // the test Leaf
        m = new QueryReply(guid, (byte) 1, PORT, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        testUPs[0].send(m);
        testUPs[0].flush();

        // send back a lot of results via TCP so you konw the UDP one will be
        // bypassed
        for (int i = 0; i < testUPs.length; i++) {
            res = new Response[75];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10+j+i, 10+j+i, "berkeley "+ j + i);
            m = new QueryReply(guid, (byte) 1, testUPs[0].getListeningPort(), 
                               myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);
            testUPs[i].send(m);
            testUPs[i].flush();
        }

        // allow for processing
        Thread.sleep(3000);

        {
            // now we should make sure MessageRouter has not bypassed anything
            // yet
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(0, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNull(endpoints);
        }
        
        // send back a UDP response and make sure it was saved in bypassed...
        {
            ReplyNumberVendorMessage vm = 
                new ReplyNumberVendorMessage(new GUID(guid), 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                                     baos.toByteArray().length,
                                                     testUPs[0].getInetAddress(),
                                                     PORT);
            UDP_ACCESS[0].send(pack);
        }

        // allow for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(1, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(guid));
            assertNotNull(endpoints);
            assertEquals(1, endpoints.size());
        }
        
        // now do the download, wait for it to finish, and then bypassed results
        // should be empty again
        RemoteFileDesc rfd = resp.toRemoteFileDesc(reply.getHostData());
        
        assertFalse("file should not be saved yet", 
            new File( _savedDir, "berkeley.txt").exists());
        assertTrue("file should be shared",
            new File(_sharedDir, "berkeley.txt").exists());
        
        rs.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        callback.clearGUID();
        
        // sleep to make sure the download starts 
        Thread.sleep(5000);
        
        assertTrue("file should saved", 
            new File( _savedDir, "berkeley.txt").exists());
        assertTrue("file should be shared",
            new File(_sharedDir, "berkeley.txt").exists());

        {
            // now we should make sure MessageRouter clears the map
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(0, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNull(endpoints);
        }

    }


    public void testQueryAliveNoPurge() throws Exception {

        keepAllAlive(testUPs);
        // clear up any messages before we begin the test.
        drainAll();

        // luckily there is hacky little way to go through the download paces -
        // download from yourself :) .

        Message m = null;

        byte[] guid = rs.newQueryGUID();
        rs.query(guid, "berkeley");
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            (QueryRequest) getFirstInstanceOfMessageType(testUPs[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // just return ONE real result and the rest junk
        Response resp = null;
        QueryReply reply = null;
        {
            // get a correct response object
            QueryRequest qrTemp = QueryRequest.createQuery("berkeley");
            testUPs[0].send(qrTemp);
            testUPs[0].flush();

            reply = (QueryReply) getFirstInstanceOfMessageType(testUPs[0],
                                                               QueryReply.class);
            assertNotNull(reply);
            resp = (Response) (reply.getResultsAsList()).get(0);

        }
        assertNotNull(reply);
        assertNotNull(resp);
        Response[] res = new Response[] { resp };

        // this isn't really needed but just for completeness send it back to 
        // the test Leaf
        m = new QueryReply(guid, (byte) 1, PORT, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        testUPs[0].send(m);
        testUPs[0].flush();

        // send back a lot of results via TCP so you konw the UDP one will be
        // bypassed
        for (int i = 0; i < testUPs.length; i++) {
            res = new Response[75];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10+j+i, 10+j+i, "berkeley "+ j + i);
            m = new QueryReply(guid, (byte) 1, testUPs[0].getListeningPort(), 
                               myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);
            testUPs[i].send(m);
            testUPs[i].flush();
        }

        // allow for processing
        Thread.sleep(3000);

        {
            // now we should make sure MessageRouter has not bypassed anything
            // yet
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(0, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNull(endpoints);
        }
        
        // send back a UDP response and make sure it was saved in bypassed...
        {
            ReplyNumberVendorMessage vm = 
                new ReplyNumberVendorMessage(new GUID(guid), 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                                     baos.toByteArray().length,
                                                     testUPs[0].getInetAddress(),
                                                     PORT);
            UDP_ACCESS[0].send(pack);
        }

        // allow for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(1, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(guid));
            assertNotNull(endpoints);
            assertEquals(1, endpoints.size());
        }
        
        // now do the download, wait for it to finish, and then bypassed results
        // should not be empty since the query is still alive
        RemoteFileDesc rfd = resp.toRemoteFileDesc(reply.getHostData());
        
        assertFalse("file should not be saved yet", 
            new File( _savedDir, "berkeley.txt").exists());
        assertTrue("file should be shared",
            new File(_sharedDir, "berkeley.txt").exists());
        
        rs.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        
        // sleep to make sure the download starts 
        Thread.sleep(5000);
        
        assertTrue("file should saved", 
            new File( _savedDir, "berkeley.txt").exists());
        assertTrue("file should be shared",
            new File(_sharedDir, "berkeley.txt").exists());

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(1, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(guid));
            assertNotNull(endpoints);
            assertEquals(1, endpoints.size());
        }
        
        rs.stopQuery(new GUID(guid));

        {
            // now we should make sure MessageRouter clears the map
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(0, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNull(endpoints);
        }

    }


    public void testDownloadProgressQueryDoneNoPurge() 
        throws Exception {

        keepAllAlive(testUPs);
        // clear up any messages before we begin the test.
        drainAll();

        // luckily there is hacky little way to go through the download paces -
        // download from yourself :) .

        Message m = null;

        byte[] guid = rs.newQueryGUID();
        rs.query(guid, "metadata");
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            (QueryRequest) getFirstInstanceOfMessageType(testUPs[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // just return ONE real result and the rest junk
        Response resp = null;
        QueryReply reply = null;
        {
            // get a correct response object
            QueryRequest qrTemp = QueryRequest.createQuery("metadata");
            testUPs[0].send(qrTemp);
            testUPs[0].flush();

            reply = (QueryReply) getFirstInstanceOfMessageType(testUPs[0],
                                                               QueryReply.class);
            assertNotNull(reply);
            resp = (Response) (reply.getResultsAsList()).get(0);

        }
        assertNotNull(reply);
        assertNotNull(resp);
        Response[] res = new Response[] { resp };

        // this isn't really needed but just for completeness send it back to 
        // the test Leaf
        m = new QueryReply(guid, (byte) 1, PORT, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        testUPs[0].send(m);
        testUPs[0].flush();

        // send back a lot of results via TCP so you konw the UDP one will be
        // bypassed
        for (int i = 0; i < testUPs.length; i++) {
            res = new Response[75];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10+j+i, 10+j+i, "berkeley "+ j + i);
            m = new QueryReply(guid, (byte) 1, testUPs[0].getListeningPort(), 
                               myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);
            testUPs[i].send(m);
            testUPs[i].flush();
        }

        // allow for processing
        Thread.sleep(3000);

        {
            // now we should make sure MessageRouter has not bypassed anything
            // yet
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(0, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNull(endpoints);
        }
        
        // send back a UDP response and make sure it was saved in bypassed...
        {
            ReplyNumberVendorMessage vm = 
                new ReplyNumberVendorMessage(new GUID(guid), 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                                     baos.toByteArray().length,
                                                     testUPs[0].getInetAddress(),
                                                     PORT);
            UDP_ACCESS[0].send(pack);
        }

        // allow for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(1, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(guid));
            assertNotNull(endpoints);
            assertEquals(1, endpoints.size());
        }
        
        // now do the download, wait for it to finish, and then bypassed results
        // should be empty again
        RemoteFileDesc rfd = resp.toRemoteFileDesc(reply.getHostData());
        
        assertFalse("file should not be saved yet", 
            new File( _savedDir, "metadata.mp3").exists());
        assertTrue("file should be shared",
            new File(_sharedDir, "metadata.mp3").exists());
        
        rs.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        UploadSettings.UPLOAD_SPEED.setValue(5);

        rs.stopQuery(new GUID(guid));
        callback.clearGUID();

        {
            // download still in progress, don't purge
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(1, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(guid));
            assertNotNull(endpoints);
            assertEquals(1, endpoints.size());
        }

        UploadSettings.UPLOAD_SPEED.setValue(100);

        // sleep to make sure the download starts 
        Thread.sleep(10000);
        
        assertTrue("file should saved", 
            new File( _savedDir, "metadata.mp3").exists());
        assertTrue("file should be shared",
            new File(_sharedDir, "metadata.mp3").exists());

        {
            // now we should make sure MessageRouter clears the map
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(0, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNull(endpoints);
        }

    }


    public void testBusyDownloadLocatesSources() throws Exception {

        keepAllAlive(testUPs);
        // clear up any messages before we begin the test.
        drainAll();

        DatagramPacket pack = null;

        Message m = null;

        byte[] guid = rs.newQueryGUID();
        rs.query(guid, "whatever");
        // i need to pretend that the UI is showing the user the query still
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            (QueryRequest) getFirstInstanceOfMessageType(testUPs[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // ok, the leaf is sending OOB queries - good stuff, now we should send
        // a lot of results back and make sure it buffers the bypassed OOB ones
        for (int i = 0; i < testUPs.length; i++) {
            Response[] res = new Response[200];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10+j+i, 10+j+i, "whatever "+ j + i);
            m = new QueryReply(qr.getGUID(), (byte) 1, 6355, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);
            testUPs[i].send(m);
            testUPs[i].flush();
        }

        // create a test uploader and send back that response
        final int UPLOADER_PORT = 10000;
        TestUploader uploader = new TestUploader("whatever", UPLOADER_PORT);
        uploader.setBusy(true);
        URN urn =
        URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ");
        Set urns = new HashSet();
        urns.add(urn);
        RemoteFileDesc rfd = new RemoteFileDesc("127.0.0.1", UPLOADER_PORT, 1, 
                                                "whatever", 10, GUID.makeGuid(),
                                                1, false, 3, false, null, 
                                                urns, false, false, 
                                                "LIME", 0, new HashSet());

        // wait for processing
        Thread.sleep(1500);

        for (int i = 0; i < UDP_ACCESS.length; i++) {
            ReplyNumberVendorMessage vm = 
                new ReplyNumberVendorMessage(new GUID(qr.getGUID()), i+1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUPs[0].getInetAddress(), PORT);
            UDP_ACCESS[i].send(pack);
        }

        // wait for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(1, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNotNull(endpoints);
            assertEquals(UDP_ACCESS.length, endpoints.size());
        }
        
        long currTime = System.currentTimeMillis();
        Downloader downloader = 
            rs.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        
        Thread.sleep(1000);
        assertEquals(Downloader.ITERATIVE_GUESSING, downloader.getState());

        // we should start getting guess queries on all UDP ports, actually
        // querykey requests
        for (int i = 0; i < UDP_ACCESS.length; i++) {
            boolean gotPing = false;
            while (!gotPing) {
                try {
                    byte[] datagramBytes = new byte[1000];
                    pack = new DatagramPacket(datagramBytes, 1000);
                    UDP_ACCESS[i].setSoTimeout(10000); // may need to wait
                    UDP_ACCESS[i].receive(pack);
                    InputStream in = new ByteArrayInputStream(pack.getData());
                    m = Message.read(in);
                    m.hop();
                    if (m instanceof PingRequest)
                        gotPing = ((PingRequest) m).isQueryKeyRequest();
                }
                catch (InterruptedIOException iioe) {
                    assertTrue("was successful for " + i,
                               false);
                }
            }
        }

        Thread.sleep((UDP_ACCESS.length * 1000) - 
                     (System.currentTimeMillis() - currTime));

        assertEquals(Downloader.WAITING_FOR_RETRY, downloader.getState());

        callback.clearGUID();
        downloader.stop();

        Thread.sleep(1000);

        {
            // now we should make sure MessageRouter clears the map
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(0, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNull(endpoints);
        }
    }


    public void testDownloadFinishes() throws Exception {

        keepAllAlive(testUPs);
        // clear up any messages before we begin the test.
        drainAll();

        DatagramPacket pack = null;

        Message m = null;

        byte[] guid = rs.newQueryGUID();
        rs.query(guid, "whatever");
        // i need to pretend that the UI is showing the user the query still
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            (QueryRequest) getFirstInstanceOfMessageType(testUPs[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // ok, the leaf is sending OOB queries - good stuff, now we should send
        // a lot of results back and make sure it buffers the bypassed OOB ones
        for (int i = 0; i < testUPs.length; i++) {
            Response[] res = new Response[200];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10+j+i, 10+j+i, "whatever "+ j + i);
            m = new QueryReply(qr.getGUID(), (byte) 1, 6355, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);
            testUPs[i].send(m);
            testUPs[i].flush();
        }

        // create a test uploader and send back that response
        final int UPLOADER_PORT = 10000;
        TestUploader uploader = new TestUploader("whatever", UPLOADER_PORT);
        uploader.setBusy(true);
        URN urn = TestFile.hash();
        Set urns = new HashSet();
        urns.add(urn);
        RemoteFileDesc rfd = new RemoteFileDesc("127.0.0.1", UPLOADER_PORT, 1, 
                                                "whatever", 10, GUID.makeGuid(),
                                                1, false, 3, false, null, 
                                                urns, false, false, 
                                                "LIME", 0, new HashSet());

        // wait for processing
        Thread.sleep(1500);

        // just do it for 1 UDP guy
        {
            ReplyNumberVendorMessage vm = 
                new ReplyNumberVendorMessage(new GUID(qr.getGUID()), 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUPs[0].getInetAddress(), PORT);
            UDP_ACCESS[0].send(pack);
        }

        // wait for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(1, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNotNull(endpoints);
            assertEquals(1, endpoints.size());
        }
        
        long currTime = System.currentTimeMillis();
        Downloader downloader = 
            rs.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        
        Thread.sleep(100);
        assertEquals(Downloader.ITERATIVE_GUESSING, downloader.getState());

        // we should get a query key request
        {
            boolean gotPing = false;
            while (!gotPing) {
                try {
                    byte[] datagramBytes = new byte[1000];
                    pack = new DatagramPacket(datagramBytes, 1000);
                    UDP_ACCESS[0].setSoTimeout(10000); // may need to wait
                    UDP_ACCESS[0].receive(pack);
                    InputStream in = new ByteArrayInputStream(pack.getData());
                    m = Message.read(in);
                    m.hop();
                    if (m instanceof PingRequest)
                        gotPing = ((PingRequest) m).isQueryKeyRequest();
                }
                catch (InterruptedIOException iioe) {
                    assertTrue(false);
                }
            }
        }

        // send back a query key
        QueryKey qk = QueryKey.getQueryKey(InetAddress.getLocalHost(),
                                           PORT);
        {
            byte[] ip = new byte[] {(byte)127, (byte) 0, (byte) 0, (byte) 1};
            PingReply pr = 
                PingReply.createQueryKeyReply(GUID.makeGuid(), (byte) 1,
                                              UDP_ACCESS[0].getLocalPort(),
                                              ip, 10, 10, false, qk);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pr.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUPs[0].getInetAddress(),
                                      PORT);
            UDP_ACCESS[0].send(pack);
        }

        Thread.sleep(500);

        // ensure that it gets into the OnDemandUnicaster
        {
            // now we should make sure MessageRouter retains the key
            Map _queryKeys = 
            (Map) PrivilegedAccessor.getValue(OnDemandUnicaster.class,
                                              "_queryKeys");
            assertNotNull(_queryKeys);
            assertEquals(1, _queryKeys.size());
        }

        { // confirm a URN query
            boolean gotQuery = false;
            while (!gotQuery) {
                try {
                    byte[] datagramBytes = new byte[1000];
                    pack = new DatagramPacket(datagramBytes, 1000);
                    UDP_ACCESS[0].setSoTimeout(10000); // may need to wait
                    UDP_ACCESS[0].receive(pack);
                    InputStream in = new ByteArrayInputStream(pack.getData());
                    m = Message.read(in);
                    if (m instanceof QueryRequest) {
                        QueryRequest qReq = (QueryRequest) m;
                        Set queryURNs = qReq.getQueryUrns();
                        gotQuery = queryURNs.contains(urn);
                        if (gotQuery)
                            gotQuery = qk.equals(qReq.getQueryKey());
                    }
                }
                catch (InterruptedIOException iioe) {
                    assertTrue(false);
                }
            }
        }

        Thread.sleep(1000 - (System.currentTimeMillis() - currTime));
        assertEquals(Downloader.WAITING_FOR_RETRY, downloader.getState());
        // purge front end of query
        callback.clearGUID();

        // create a new Uploader to service the download
        TestUploader uploader2 = new TestUploader("whatever", UPLOADER_PORT+1);
        uploader2.setRate(100);

        { // send back a query request, the TestUploader should service upload
            rfd = new RemoteFileDesc("127.0.0.1", UPLOADER_PORT+1, 1, 
                                     "whatever", 10, GUID.makeGuid(),
                                     1, false, 3, false, null, 
                                     urns, false, false, 
                                     "LIME", 0, new HashSet());
            Response[] res = new Response[] { new Response(10, 10, "whatever") };
            m = new QueryReply(qr.getGUID(), (byte) 1, UPLOADER_PORT+1, myIP(), 
                               0, res, GUID.makeGuid(), new byte[0], false, 
                               false, true, true, false, false, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUPs[0].getInetAddress(),
                                      PORT);
            UDP_ACCESS[0].send(pack);
        }

        // after a while, the download should finish, the bypassed results
        // should be discarded
        Thread.sleep(10000);
        assertEquals(Downloader.COMPLETE, downloader.getState());

        {
            // now we should make sure MessageRouter clears the map
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(0, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNull(endpoints);
        }
    }


    public void testUsesCachedQueryKeys() throws Exception {

        keepAllAlive(testUPs);
        // clear up any messages before we begin the test.
        drainAll();

        DatagramPacket pack = null;

        Message m = null;

        byte[] guid = rs.newQueryGUID();
        rs.query(guid, "whatever");
        // i need to pretend that the UI is showing the user the query still
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            (QueryRequest) getFirstInstanceOfMessageType(testUPs[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // ok, the leaf is sending OOB queries - good stuff, now we should send
        // a lot of results back and make sure it buffers the bypassed OOB ones
        for (int i = 0; i < testUPs.length; i++) {
            Response[] res = new Response[200];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10+j+i, 10+j+i, "whatever "+ j + i);
            m = new QueryReply(qr.getGUID(), (byte) 1, 6355, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);
            testUPs[i].send(m);
            testUPs[i].flush();
        }

        // create a test uploader and send back that response
        final int UPLOADER_PORT = 10000;
        TestUploader uploader = new TestUploader("whatever", UPLOADER_PORT);
        uploader.setBusy(true);
        URN urn =
        URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ");
        Set urns = new HashSet();
        urns.add(urn);
        RemoteFileDesc rfd = new RemoteFileDesc("127.0.0.1", UPLOADER_PORT, 1, 
                                                "whatever", 10, GUID.makeGuid(),
                                                1, false, 3, false, null, 
                                                urns, false, false, 
                                                "LIME", 0, new HashSet());

        // wait for processing
        Thread.sleep(1500);

        // send back ReplyNumberVMs that should be bypassed
        for (int i = 0; i < UDP_ACCESS.length; i++) {
            ReplyNumberVendorMessage vm = 
                new ReplyNumberVendorMessage(new GUID(qr.getGUID()), i+1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUPs[0].getInetAddress(), PORT);
            UDP_ACCESS[i].send(pack);
        }

        // wait for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(1, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNotNull(endpoints);
            assertEquals(UDP_ACCESS.length, endpoints.size());
        }
        
        // Prepopulate Query Keys
        QueryKey qk = QueryKey.getQueryKey(InetAddress.getLocalHost(),
                                           PORT);
        for (int i = 0; i < UDP_ACCESS.length; i++) {
            byte[] ip = new byte[] {(byte)127, (byte) 0, (byte) 0, (byte) 1};
            PingReply pr = 
                PingReply.createQueryKeyReply(GUID.makeGuid(), (byte) 1,
                                              UDP_ACCESS[i].getLocalPort(),
                                              ip, 10, 10, false, qk);
            pr.hop();
            OnDemandUnicaster.handleQueryKeyPong(pr);

        }

        // confirm download will try to GUESS
        long currTime = System.currentTimeMillis();
        Downloader downloader = 
            rs.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        
        Thread.sleep(1000);
        assertEquals(Downloader.ITERATIVE_GUESSING, downloader.getState());

        // we should start getting guess queries on all UDP ports
        for (int i = 0; i < UDP_ACCESS.length; i++) {
            boolean gotQuery = false;
            while (!gotQuery) {
                try {
                    byte[] datagramBytes = new byte[1000];
                    pack = new DatagramPacket(datagramBytes, 1000);
                    UDP_ACCESS[i].setSoTimeout(10000); // may need to wait
                    UDP_ACCESS[i].receive(pack);
                    InputStream in = new ByteArrayInputStream(pack.getData());
                    m = Message.read(in);
                    if (m instanceof QueryRequest) {
                        QueryRequest qReq = (QueryRequest) m;
                        Set queryURNs = qReq.getQueryUrns();
                        gotQuery = queryURNs.contains(urn);
                        if (gotQuery)
                            gotQuery = qk.equals(qReq.getQueryKey());
                    }
                }
                catch (InterruptedIOException iioe) {
                    assertTrue("was successful for " + i,
                               false);
                }
            }
        }

        Thread.sleep((UDP_ACCESS.length * 1000) - 
                     (System.currentTimeMillis() - currTime));

        assertEquals(Downloader.WAITING_FOR_RETRY, downloader.getState());

        callback.clearGUID();
        downloader.stop();

        Thread.sleep(1000);

        {
            // now we should make sure MessageRouter clears the map
            Map _bypassedResults = 
                (Map) PrivilegedAccessor.getValue(rs.getMessageRouter(),
                                                  "_bypassedResults");
            assertNotNull(_bypassedResults);
            assertEquals(0, _bypassedResults.size());
            Set endpoints = (Set) _bypassedResults.get(new GUID(qr.getGUID()));
            assertNull(endpoints);
        }
    }


    //////////////////////////////////////////////////////////////////

    private void drainAll() throws Exception {
        drainAll(testUPs);
    }
    
    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

    private static final boolean DEBUG = false;
    
    static void debug(String message) {
        if(DEBUG) 
            System.out.println(message);
    }

    private static class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing) throws IOException {
            Properties props = new UltrapeerHeaders("127.0.0.1"); 
            props.put(HeaderNames.X_DEGREE, "42");           
            return HandshakeResponse.createResponse(props);
        }
    }


    private static class OldResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing) throws IOException {
            Properties props=new Properties();
            return HandshakeResponse.createResponse(props);
        }
    }

    public static class MyActivityCallback extends ActivityCallbackStub {
        public GUID aliveGUID = null;

        public void setGUID(GUID guid) { aliveGUID = guid; }
        public void clearGUID() { aliveGUID = null; }

        public boolean queryIsAlive(GUID guid) {
            if (aliveGUID != null)
                return (aliveGUID.equals(guid));
            return false;
        }
    }


}
