package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import java.util.Properties;
import com.sun.java.util.collections.*;
import java.io.*;

/**
 * Out-of-process test to check whether ultrapeers handle query routing, normal
 * routing, routing of marked pongs, etc.
 */
public class UltrapeerTester {
    static final int PORT=6347;
    static final int TIMEOUT=500;
    static Connection leaf;
    static Connection ultrapeer;
    static Connection old;

    public static void main(String args[]) {
        System.out.println(
            "Please make sure you have an ultrapeer with incoming slots, \n"
           +"running on port "+PORT+" of localhost, with no connections\n");
        
        try {
            connect();
            testBroadcastFromLeaf();  //also tests replies, pushes
            testBroadcastFromOld();
            testBroadcastFromOldToLeaf();
            testPingBroadcast();      //also tests replies
            testBigPingBroadcast();
            testMisroutedPong();
            testUltrapeerPong();
            testNullQueryURNRequest();
            testDropAndDuplicate();   //must be last; closes old
            shutdown();
        } catch (IOException e) { 
            System.err.println("Mysterious IOException:");
            e.printStackTrace();
        } catch (BadPacketException e) { 
            System.err.println("Mysterious bad packet:");
            e.printStackTrace();
        }
        
        System.out.println("Done");
    }

    private static void connect() throws IOException {
        //1. unrouted 0.4 connection
        old=new Connection("localhost", PORT);
        old.initialize();

        //2. unrouted ultrapeer connection
        ultrapeer=new Connection("localhost", PORT, 
                                            new UltrapeerProperties(),
                                            new EmptyResponder(),
                                            false);
        ultrapeer.initialize();
        
        //3. routed leaf, with route table for "test"
        leaf=new Connection("localhost", PORT, 
                                       new LeafProperties(),
                                       new EmptyResponder(),
                                       false);
        leaf.initialize();
        QueryRouteTable qrt=new QueryRouteTable();
        qrt.add("test");
        for (Iterator iter=qrt.encode(null); iter.hasNext(); ) {
            leaf.send((RouteTableMessage)iter.next());
        }
    }

    private static void testBroadcastFromLeaf() 
             throws IOException, BadPacketException {
        System.out.println(
            "-Testing normal broadcast from leaf, with replies and pushes");
        drain(old);
        drain(ultrapeer);

        //1. Check that query broadcasted to old and ultrapeer
        QueryRequest qr=new QueryRequest((byte)7, 0, "crap");
        leaf.send(qr);
        leaf.flush();
        
        Message m=old.receive(TIMEOUT);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("crap"));
        Assert.that(m.getHops()==(byte)1); //used to be not decremented
        Assert.that(m.getTTL()==(byte)6);
      
        m=ultrapeer.receive(TIMEOUT);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("crap"));
        Assert.that(m.getHops()==(byte)1); //used to be not decremented
        Assert.that(m.getTTL()==(byte)6);

        //2. Check that replies are routed back.
        drain(leaf);
        Response response1=new Response(0l, 0l, "response1.txt");
        byte[] guid1=GUID.makeGuid();
        QueryReply reply1=new QueryReply(qr.getGUID(),
                                         (byte)2,
                                         6346,
                                         new byte[4],
                                         56,
                                         new Response[] {response1},
                                         guid1);
        old.send(reply1);
        old.flush();

        QueryReply replyRead=(QueryReply)leaf.receive(TIMEOUT);
        Assert.that(Arrays.equals(guid1, replyRead.getClientGUID()));

        drain(leaf);
        Response response2=new Response(0l, 0l, "response2.txt");
        byte[] guid2=GUID.makeGuid();
        QueryReply reply2=new QueryReply(qr.getGUID(),
                                         (byte)2,
                                         6346,
                                         new byte[4],
                                         56,
                                         new Response[] {response1},
                                         guid2);
        ultrapeer.send(reply2);
        ultrapeer.flush();

        replyRead=(QueryReply)leaf.receive(TIMEOUT);
        Assert.that(Arrays.equals(guid2, replyRead.getClientGUID()));

        //3. Check that pushes are routed (not broadcast)
        drain(old);
        drain(ultrapeer);
        PushRequest push1=new PushRequest(GUID.makeGuid(),
                                          (byte)2,
                                          guid1,
                                          0, new byte[4],
                                          6346);
        leaf.send(push1);
        leaf.flush();
        PushRequest pushRead=(PushRequest)old.receive(TIMEOUT);
        Assert.that(pushRead.getIndex()==0);
        Assert.that(! drain(ultrapeer));

        PushRequest push2=new PushRequest(GUID.makeGuid(),
                                          (byte)2,
                                          guid2,
                                          1, new byte[4],
                                          6346);
        leaf.send(push2);
        leaf.flush();
        pushRead=(PushRequest)ultrapeer.receive(TIMEOUT);
        Assert.that(pushRead.getIndex()==1);
        Assert.that(! drain(old));        

        //4. Check that queries can re-route push routes
        drain(leaf);
        drain(old);
        ultrapeer.send(reply1);
        ultrapeer.flush();
        replyRead=(QueryReply)leaf.receive(TIMEOUT);
        Assert.that(Arrays.equals(guid1, replyRead.getClientGUID()));
        PushRequest push3=new PushRequest(GUID.makeGuid(),
                                          (byte)2,
                                          guid1,
                                          3, new byte[4],
                                          6346);
        leaf.send(push3);
        leaf.flush();
        pushRead=(PushRequest)ultrapeer.receive(TIMEOUT);
        Assert.that(pushRead.getIndex()==3);
        Assert.that(! drain(old));
    }

    /** This test makes sure that query's with no query string but with
     *  specified urn's get through to leaves, etc.
     */ 
    private static void testNullQueryURNRequest() 
        throws IOException, BadPacketException {
        System.out.println("-Testing null query string with non-null URN" +
                           " QR routering.");
        
        // drain all connections
        drain(ultrapeer);
        drain(leaf);
        drain(old);

        // make sure it gets through with all combinations of one sender and two
        // receivers.
        urnTest(ultrapeer, old, leaf);
        urnTest(old, leaf, ultrapeer);
        urnTest(leaf, ultrapeer, old);        
    }


    private static void urnTest(Connection sndr, Connection rcv1,
                                Connection rcv2) 
        throws IOException, BadPacketException {
        // make urns...
        Set currUrnSet = new HashSet();
        Set currUrnTypeSet = new HashSet();
        for(int j = 0; j < HugeTestUtils.URNS.length; j++) {
            currUrnSet.add(HugeTestUtils.URNS[j]);
            currUrnTypeSet.add(HugeTestUtils.URNS[j].getUrnType());
        }

        // build the null QR
        GUID guid = new GUID(GUID.makeGuid());
        QueryRequest qr = new QueryRequest(guid.bytes(),
                                           (byte)7, 0, "", "", false,
                                           currUrnTypeSet, currUrnSet);
        
        // send the QR - FROM sndr
        sndr.send(qr);
        sndr.flush();

        // did recv1 get everything a-ok?
        QueryRequest reqRead=(QueryRequest)rcv1.receive(TIMEOUT);
        Assert.that(Arrays.equals(guid.bytes(), reqRead.getGUID()));
        Assert.that(reqRead.getQueryUrns() != null);
        Assert.that(currUrnSet.equals(reqRead.getQueryUrns()));

        // did recv2 get everything a-ok?
        reqRead=(QueryRequest)rcv2.receive(TIMEOUT);
        Assert.that(Arrays.equals(guid.bytes(), reqRead.getGUID()));
        Assert.that(reqRead.getQueryUrns() != null);
        Assert.that(currUrnSet.equals(reqRead.getQueryUrns()));
    }


    private static void testBroadcastFromOld() 
             throws IOException, BadPacketException {
        System.out.println("-Testing normal broadcast from old connnection"
                           +", no forwarding to leaf");
        drain(ultrapeer);
        drain(leaf);

        QueryRequest qr=new QueryRequest((byte)7, 0, "crap");
        old.send(qr);
        old.flush();
              
        Message m=ultrapeer.receive(TIMEOUT);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("crap"));
        Assert.that(m.getHops()==(byte)1); 
        Assert.that(m.getTTL()==(byte)6);

        Assert.that(! drain(leaf));
    }

    private static void testBroadcastFromOldToLeaf() 
             throws IOException, BadPacketException {
        System.out.println("-Testing normal broadcast from old connnection"
                           +", with forwarding to leaf");
        drain(ultrapeer);
        drain(leaf);

        QueryRequest qr=new QueryRequest((byte)7, 0, "test");
        old.send(qr);
        old.flush();
              
        Message m=ultrapeer.receive(TIMEOUT);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("test"));
        Assert.that(m.getHops()==(byte)1); 
        Assert.that(m.getTTL()==(byte)6);

        m=leaf.receive(TIMEOUT);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("test"));
        Assert.that(m.getHops()==(byte)1); 
        Assert.that(m.getTTL()==(byte)6);
    }

    private static void testPingBroadcast() 
             throws IOException, BadPacketException {
        System.out.println("-Testing ping broadcast from old connnection"
                           +", no forwarding to leaf, with reply");
        drain(old);
        drain(leaf);

        //Send ping
        Message m=new PingRequest((byte)7);
        ultrapeer.send(m);
        ultrapeer.flush();
              
        m=old.receive(TIMEOUT);
        Assert.that(m instanceof PingRequest);
        Assert.that(m.getHops()==(byte)1); 
        Assert.that(m.getTTL()==(byte)6);

        Assert.that(! drain(leaf));

        //Send reply
        drain(ultrapeer);        
        PingReply pong=new PingReply(m.getGUID(),
                                     (byte)7,
                                     6344,
                                     new byte[4],
                                     3, 7);
        old.send(pong);
        old.flush();
        for (int i=0; i<10; i++) {
            PingReply pongRead=(PingReply)ultrapeer.receive(TIMEOUT);
            if (pongRead.getPort()==pong.getPort())
                return;
        }
        Assert.that(false, "Pong wasn't routed");
    }

    private static void testBigPingBroadcast() 
             throws IOException, BadPacketException {
        System.out.println("-Testing big ping broadcast from leaf connnection"
                           +", no payload forwarding to old, with big reply");
        drain(old);
        drain(ultrapeer);

        //1a. Send big ping (not GGEP...which should be ok)
        byte[] payload= new byte[16];
        byte c = 65; //'A'
        for(int i=0;i<16;i++, c++)
            payload[i] = c;
        
        Message m=new PingRequest(GUID.makeGuid(), (byte)7, (byte)0,payload);
        leaf.send(m);
        leaf.flush();
            
        //1b. Make sure ultrapeer gets it with payload.
        m=ultrapeer.receive(TIMEOUT);
        PingRequest ping = null;
        try{
            ping = (PingRequest)m;
        }catch(ClassCastException cce){
            cce.printStackTrace();
            Assert.that(false,"Big ping not created properly on old client");
        }
        Assert.that(m.getHops()==(byte)1); 
        Assert.that(m.getTTL()==(byte)6);
        Assert.that(m.getLength()==16);
        //lets make sure the payload got there OK
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try{
            ping.write(stream);
        }catch(IOException ioe){
            ioe.printStackTrace();
            Assert.that(false, "error while writing payload in old client");
        }
        byte[] b = stream.toByteArray();
        //get rid of bytes 0-22(inclusive) ie the header

        String out = new String(b,23,b.length-23);

        Assert.that(out.equals
                    ("ABCDEFGHIJKLMNOP"),"wrong payload in old client "+out);
        
        //1c. Make sure old gets it wihtout payload.
        m=old.receive(TIMEOUT);
        ping = null;
        try{
            ping = (PingRequest)m;
        }catch(ClassCastException cce){
            cce.printStackTrace();
            Assert.that(false,"Big ping not created properly on old client");
        }
        Assert.that(m.getHops()==(byte)1); 
        Assert.that(m.getTTL()==(byte)6);
        Assert.that(m.getLength()==0);


        //2a. Send reply from ultrapeer
        //create payload for big pong
        byte[] payload2 = new byte[14+2];
        //add the port
        payload2[0] = 0x0F;
        payload2[1] = 0x00;//port 

        payload2[2] = 0x10;
        payload2[3] = 0x10;
        payload2[4] = 0x10;
        payload2[5] = 0x10;//ip = 16.16.16.16

        payload2[6] = 0x0F;//
        payload2[7] = 0x00;//
        payload2[8] = 0x00;//
        payload2[9] = 0x00;//15 files shared

        payload2[10] = 0x0F;//
        payload2[11] = 0x00;//
        payload2[12] = 0x00;//
        payload2[13] = 0x00;//15 KB
        //OK Now for the big pong part
        payload2[14] = (byte) 65;
        payload2[15] = (byte) 66;

        drain(leaf);       

        PingReply pong=new PingReply(m.getGUID(),
                                     (byte)7,
                                     (byte)0,
                                     payload2);
        ultrapeer.send(pong);
        ultrapeer.flush();

        //2b. Make sure leaf reads it.
        PingReply ourPong = null;
        for (int i=0; i<10; i++) {
            PingReply pongRead=(PingReply)leaf.receive(TIMEOUT);
            if (pongRead.getPort()==pong.getPort()){
                ourPong = pongRead;
                break;
            }
        }
        Assert.that(ourPong != null, "Pong wasn't routed");
        //Lets check that the pong came back in good shape
        Assert.that(ourPong.getPort() == 15, "wrong port");
        String ip = ourPong.getIP();
        Assert.that(ip.equals("16.16.16.16"),"wrong IP");
        Assert.that(ourPong.getFiles() == 15, "wrong files");
        Assert.that(ourPong.getKbytes() == 15, "Wrong share size");
        stream = new ByteArrayOutputStream();
        try{
            ourPong.write(stream);
        }catch(IOException ioe){
            ioe.printStackTrace();
            Assert.that(false, "problem with writing out big pong");
        }
        byte[] op = stream.toByteArray();
        byte[] big = new byte[2];
        big[0] = op[op.length-2];
        big[1] = op[op.length-1];
        out = "";//reset
        out = new String(big);
        Assert.that(out.equals("AB"), "Big part of pong lost");
        //come this far means its OK
        //System.out.println("Passed");
    }




    private static void testMisroutedPong() 
             throws IOException, BadPacketException {
        System.out.println("-Testing misrouted normal pong"
                           +", not forwarded to leaf");
        drain(old);
        drain(leaf);

        Message m=new PingReply(GUID.makeGuid(), 
                                (byte)7, 6399, new byte[4], 
                                0, 0, false);                                
        ultrapeer.send(m);
        ultrapeer.flush();
              
        Assert.that(! drain(old));
        Assert.that(! drain(leaf));
    }

    private static void testUltrapeerPong() 
             throws IOException, BadPacketException {
        System.out.println("-Testing misrouted ultrapeer pong"
                           +", forwarded to leaf");
        drain(old);
        drain(leaf);

        byte[] guid=GUID.makeGuid();
        Message m=new PingReply(guid, 
                                (byte)7, 6399, new byte[4], 
                                0, 0, true);                                
        ultrapeer.send(m);
        ultrapeer.flush();
              
        m=leaf.receive(TIMEOUT);
        Assert.that(m instanceof PingReply);
        Assert.that(((PingReply)m).getPort()==6399);        

        Assert.that(! drain(old));
    }


    private static void testDropAndDuplicate() 
             throws IOException, BadPacketException {
        System.out.println("-Testing that duplicates are dropped "
                           +"when original connection closed");
        drain(old);
        drain(ultrapeer);

        //Send query request from leaf, received by ultrapeer (and old)
        QueryRequest qr=new QueryRequest((byte)7, 0, "crap");
        leaf.send(qr);
        leaf.flush();
        
        Message m=ultrapeer.receive(TIMEOUT);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("crap"));
        Assert.that(m.getHops()==(byte)1); //used to be not decremented
        Assert.that(m.getTTL()==(byte)6);

        //After closing leaf (give it some time to clean up), make sure
        //duplicate query is dropped.
        drain(ultrapeer);
        leaf.close();
        try { Thread.sleep(200); } catch (InterruptedException e) { }
        old.send(qr);
        old.flush();
        Assert.that(!drain(ultrapeer));
    }

    /** Tries to receive any outstanding messages on c 
     *  @return true if this got a message */
    private static boolean drain(Connection c) throws IOException {
        boolean ret=false;
        while (true) {
            try {
                Message m=c.receive(TIMEOUT);
                ret=true;
                //System.out.println("Draining "+m+" from "+c);
            } catch (InterruptedIOException e) {
                return ret;
            } catch (BadPacketException e) {
            }
        }
    }

    private static void shutdown() throws IOException {
        System.out.println("\nShutting down.");
        leaf.close();
        ultrapeer.close();
        old.close();
    }
}

class LeafProperties extends Properties {
    public LeafProperties() {
        put(ConnectionHandshakeHeaders.X_QUERY_ROUTING, "0.1");
        put(ConnectionHandshakeHeaders.X_SUPERNODE, "False");
        put(ConnectionHandshakeHeaders.GGEP, "0.5");
    }
}

class UltrapeerProperties extends Properties {
    public UltrapeerProperties() {
        put(ConnectionHandshakeHeaders.X_QUERY_ROUTING, "0.1");
        put(ConnectionHandshakeHeaders.X_SUPERNODE, "true");
        put(ConnectionHandshakeHeaders.GGEP, "1.0");  //just for fun
    }
}


class EmptyResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException {
        return new HandshakeResponse(new Properties());
    }
}
