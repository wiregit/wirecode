package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.routing.*;
import junit.framework.*;

/**
 * Tests CompositeQueue, which provides message dropping and reorder for flow
 * control purposes.
 */
public class CompositeQueueTest extends TestCase {
    public CompositeQueueTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(CompositeQueueTest.class);
    }    
   
    CompositeQueue queue;
    public void setUp() {
        CompositeQueue.QUEUE_TIME=1000;
        queue=new CompositeQueue();
    }

    ////////////////////////////////  Tests ///////////////////////////////
    
    public void testEmpty() {        
        assertEquals(0, queue.size());
        assertEquals(null, queue.removeNext());
        assertEquals(0, queue.resetDropped());
    }

    public void testMessageDrop() {
        //Add too many pings
        PingRequest[] pings=new PingRequest[1000];
        for (int i=0; i<pings.length; i++) {
            pings[i]=new PingRequest((byte)3);
            queue.add(pings[i]);
        }

        //Make sure we got 1000, 999, ..., 901
        for (int i=0; i<CompositeQueue.QUEUE_SIZE; i++) {
            Message m=queue.removeNext();
            assertEquals(pings[pings.length-i-1], m);
        }
        assertEquals(null, queue.removeNext());
        assertEquals(pings.length-CompositeQueue.QUEUE_SIZE, 
                     queue.resetDropped());
    }

    public void testMessageReorder() {
        //This is the most important test in our suite. TODO: simplify this by
        //breaking it into subparts, e.g., to test that twice as many replies as
        //queries are sent, that replies are ordered by GUID volume, that
        //queries with the same hops are LIFO, etc.  Also, we should use "==" to
        //check the values from removeBest().

        Message m=null;

        //1. Buffer tons of messages.  Must put a watchdog pong in first to avoid
        //any complications from the _priority optimization in add(..).
        queue.add(new PingReply(new byte[16], (byte)1, 6342, new byte[4], 0, 0));
        queue.add(new QueryRequest((byte)5, 0, "test"));
        m=new PingRequest((byte)5);
        m.hop();
        queue.add(m);
        m=new QueryReply(new byte[16], (byte)5, 6340, new byte[4], 0, 
                         new Response[0], new byte[16]);
        m.setPriority(30000);
        queue.add(m);
        queue.add(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6340));
        queue.add(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6341));
        queue.add(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6342));
        m=new QueryReply(new byte[16], (byte)5, 6341, new byte[4], 0, 
                         new Response[0], new byte[16]);
        queue.add(m);
        m=new PingReply(new byte[16], (byte)5, 6343, new byte[4], 0, 0);
        m.hop();  m.hop();  m.hop();
        queue.add(m);
        queue.add(new ResetTableMessage(1024, (byte)2));
        m=new PingReply(new byte[16], (byte)3, 6340, new byte[4], 0, 0);
        m.hop();
        m.hop();
        queue.add(m);
        m=new QueryReply(new byte[16], (byte)5, 6342, new byte[4], 0, 
                         new Response[0], new byte[16]);
        m.setPriority(1000);
        queue.add(m);
        queue.add(new PatchTableMessage((short)1, (short)2, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 0, 5));
        queue.add(new PingRequest((byte)2));
        queue.add(new PatchTableMessage((short)2, (short)2, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 5, 9));
        queue.add(new QueryRequest((byte)5, 0, "test2"));
        m=new QueryRequest((byte)5, 0, "test far");
        m.hop();
        queue.add(m);
               
        //2. Now we let the messages pass through, as if the receiver's window
        //became non-zero.  Buffers look this before emptying:
        //  WATCHDOG: pong/6342 ping
        //  PUSH: x/6340 x/6341 x/6342
        //  QUERY_REPLY: 6340/3 6342/1000 6341/0 (highest priority)
        //  QUERY: "test far"/1, "test"/0, "test2"/0
        //  PING_REPLY: x/6340 x/6343
        //  PING: x
        //  OTHER: reset patch1 patch2

        //3. Read them...now in different order!
        m=queue.removeNext(); //watchdog ping
        assertTrue("Unexpected message: "+m, m instanceof PingRequest);
        assertTrue("Unexpected message: "+m, m.getHops()==0);  

        m=queue.removeNext(); //push        
        assertTrue("Unexpected message: "+m, m instanceof PushRequest);
        assertTrue(((PushRequest)m).getPort()==6342);

        m=queue.removeNext(); //push
        assertTrue("Unexpected message: "+m, m instanceof PushRequest);
        assertTrue(((PushRequest)m).getPort()==6341);

        m=queue.removeNext(); //push
        assertTrue("Unexpected message: "+m, m instanceof PushRequest);
        assertTrue(((PushRequest)m).getPort()==6340);

        m=queue.removeNext(); //reply/6341 (high priority)
        assertTrue(m instanceof QueryReply);
        assertEquals(6341, ((QueryReply)m).getPort());

        m=queue.removeNext(); //reply/6342 (medium priority)
        assertTrue(m instanceof QueryReply);
        assertTrue(((QueryReply)m).getPort()==6342);

        
        m=queue.removeNext(); //query "test2"/0
        assertTrue(m instanceof QueryRequest);
        assertTrue("Got: "+m, ((QueryRequest)m).getQuery().equals("test2"));

        m=queue.removeNext(); //reply 6343
        assertTrue(m instanceof PingReply);
        assertTrue(((PingReply)m).getPort()==6343);

        m=queue.removeNext(); //ping
        assertTrue(m instanceof PingRequest);
        assertTrue(m.getHops()>0);

        m=queue.removeNext(); //QRP reset
        assertTrue(m instanceof ResetTableMessage);

        

        m=queue.removeNext(); //watchdog pong/6342
        assertTrue(m instanceof PingReply);
        assertTrue(((PingReply)m).getPort()==6342);
        assertTrue(m.getHops()==0);  //watchdog response pong

        m=queue.removeNext(); //reply/6340
        assertTrue(m instanceof QueryReply);
        assertTrue(((QueryReply)m).getPort()==6340);

        m=queue.removeNext(); //query "test"/0
        assertTrue(m instanceof QueryRequest);
        assertTrue(((QueryRequest)m).getQuery().equals("test"));

        m=queue.removeNext(); //reply 6340
        assertTrue(m instanceof PingReply);
        assertTrue(m.toString(), ((PingReply)m).getPort()==6340);

        m=queue.removeNext(); //QRP patch1
        assertTrue(m instanceof PatchTableMessage);
        assertTrue(((PatchTableMessage)m).getSequenceNumber()==1);


        m=queue.removeNext(); //query "test"/0
        assertTrue(m instanceof QueryRequest);
        assertTrue(((QueryRequest)m).getQuery().equals("test far"));

        m=queue.removeNext(); //QRP patch2
        assertTrue(m instanceof PatchTableMessage);
        assertTrue(((PatchTableMessage)m).getSequenceNumber()==2);
    }


    public void testMessageReorder2() { 
        //tail...<wrap>...head
        queue.add(hopped(new PingRequest((byte)4)));
        queue.add(new QueryRequest((byte)3, 0, "a"));
        assertTrue(queue.removeNext() instanceof PingRequest);
        assertTrue(queue.removeNext() instanceof QueryRequest);
        assertEquals(null, queue.removeNext());
        assertEquals(0, queue.size());
    }

    public void testMessageReorder3() {
        // head...tail
        queue.add(new QueryRequest((byte)3, 0, "a"));
        queue.add(hopped(new PingRequest((byte)5)));
        assertTrue(queue.removeNext() instanceof QueryRequest);
        assertTrue(queue.removeNext() instanceof PingRequest);
        assertEquals(null, queue.removeNext());
    }

    public void testMessageReorder4() {
        //tail...<wrap>...head
        //  WATCHDOG: ping
        //  PUSH:
        //  QUERY_REPLY: reply
        //  QUERY: query
        //  PING_REPLY: 
        //  PING: 
        //  OTHER: reset
        queue.add(new QueryRequest((byte)3, 0, "a"));
        queue.add(new PingRequest((byte)1));
        queue.add(new QueryReply(new byte[16], (byte)5, 6341, new byte[4], 0, 
                                new Response[0], new byte[16]));
        queue.add(new ResetTableMessage(1024, (byte)2));
        Message m=queue.removeNext();
        assertTrue("Got: "+m, m instanceof QueryRequest);
        m=queue.removeNext();
        assertTrue("Got: "+m, m instanceof ResetTableMessage);
        m=queue.removeNext();
        assertTrue("Got: "+m, m instanceof PingRequest);
        m=queue.removeNext();
        assertTrue("Got: "+m, m instanceof QueryReply);
        assertEquals(null, queue.removeNext());
    }

    public void testDropTimedout() {        
        assertEquals(1000, CompositeQueue.QUEUE_TIME);
        //Drop one message
        queue.add(new QueryRequest((byte)3, 0, "0"));   
        sleep(1200);
        queue.add(new QueryRequest((byte)3, 0, "1200"));        
        Message m=(QueryRequest)queue.removeNext();
        assertTrue(m instanceof QueryRequest);
        assertTrue(((QueryRequest)m).getQuery().equals("1200"));
        assertEquals(null, queue.removeNext());
        assertEquals(1, queue.resetDropped());

        //Drop many messages
        queue.add(new QueryRequest((byte)3, 0, "0"));   
        sleep(300);
        queue.add(new QueryRequest((byte)3, 0, "300"));        
        sleep(300);
        queue.add(new QueryRequest((byte)3, 0, "600"));        
        sleep(500);
        queue.add(new QueryRequest((byte)3, 0, "1100"));
        sleep(900);
        queue.add(new QueryRequest((byte)3, 0, "2000"));
        m=queue.removeNext();
        assertTrue(m!=null);
        assertTrue(m instanceof QueryRequest);
        assertTrue(((QueryRequest)m).getQuery().equals("2000"));
        m=queue.removeNext();
        assertTrue(m!=null);
        assertTrue(m instanceof QueryRequest);
        assertTrue(((QueryRequest)m).getQuery().equals("1100"));
        assertEquals(null, queue.removeNext());
        assertEquals(3, queue.resetDropped());
    }


    ///////////////////////////// Helpers ////////////////////////////

    private static Message hopped(Message m) {
        m.hop();
        return m;
    }

    private static void sleep(long msecs) {
        try { Thread.sleep(msecs); } catch (InterruptedException ignored) { }
    }

}
