package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;
import com.sun.java.util.collections.*;
import junit.framework.*;

/**
 * Test the public methods of the <tt>FileDesc</tt> class.
 */
public final class RouteTableTest extends BaseTestCase {

	public RouteTableTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(RouteTableTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public void testLegacy() throws Exception {
        RouteTable rt=null;
        byte[] g1=new byte[16]; g1[0]=(byte)1;
        byte[] g2=new byte[16]; g2[0]=(byte)2;
        byte[] g3=new byte[16]; g3[0]=(byte)3;
        byte[] g4=new byte[16]; g4[0]=(byte)4;
        ReplyHandler c1=new StubReplyHandler();
        ReplyHandler c2=new StubReplyHandler();
        ReplyHandler c3=new StubReplyHandler();
        ReplyHandler c4=new StubReplyHandler();

        //1. Test time replacement policy (glass box).
        int MSECS=1250;
        rt=new RouteTable(MSECS/1000, Integer.MAX_VALUE);
        rt.routeReply(g1, c1);
        rt.routeReply(g2, c2);                    //old, new:
        rt.routeReply(g3, c3);                    //{}, {g1, g2, g3}
        assertSame(c1, rt.getReplyHandler(g1));
        assertSame(c2, rt.getReplyHandler(g2));
        assertSame(c3, rt.getReplyHandler(g3, 0, (short) 0).getReplyHandler());
        assertNull(rt.getReplyHandler(g4, 0, (short) 0));
        try { Thread.sleep(MSECS); } catch (InterruptedException e) { }
        assertNotNull(rt.tryToRouteReply(g4, c4));  //{g1, g2, g3}, {g4}
        assertSame(c1, rt.getReplyHandler(g1));
        rt.routeReply(g1, c1);                    //{g2, g3}, {g1, g4}
        assertSame(c1, rt.getReplyHandler(g1));
        assertSame(c2, rt.getReplyHandler(g2, 0, (short) 0).getReplyHandler());
        assertSame(c3, rt.getReplyHandler(g3));
        assertSame(c4, rt.getReplyHandler(g4));
        try { Thread.sleep(MSECS); } catch (InterruptedException e) { }
        rt.routeReply(g2, c3);                     //{g1, g4}, {g2}
        debug(rt.toString());
        assertSame(c1, rt.getReplyHandler(g1));
        assertSame(c3, rt.getReplyHandler(g2));
        assertNull(rt.getReplyHandler(g3));
        assertSame(c4, rt.getReplyHandler(g4));
        try { Thread.sleep(MSECS); } catch (InterruptedException e) { }
        assertNull(rt.tryToRouteReply(g2, c2));  //{g2}, {}}
        assertNull(rt.getReplyHandler(g1));
        assertSame(c3, rt.getReplyHandler(g2));
        assertNull(rt.getReplyHandler(g3));
        assertNull(rt.getReplyHandler(g4));
        rt.routeReply(g2, c2);                      //{}, {g2}
        assertNull(rt.getReplyHandler(g1));
        assertSame(c2, rt.getReplyHandler(g2));
        assertNull(rt.getReplyHandler(g3));
        assertNull(rt.getReplyHandler(g4));

        //1.5 Test space replacment (hard upper bounds)
        rt=new RouteTable(Integer.MAX_VALUE, 1);
        rt.routeReply(g1, c1);
        rt.routeReply(g2, c2);                      //{g1}, {g2}
        assertSame(c1, rt.getReplyHandler(g1));
        assertSame(c2, rt.getReplyHandler(g2));
        rt.routeReply(g3, c3);                      //{g3}, {g2}
        assertNull(rt.getReplyHandler(g1));
        assertSame(c2, rt.getReplyHandler(g2));
        assertSame(c3, rt.getReplyHandler(g3));
        rt.routeReply(g4, c4);
        assertNull(rt.getReplyHandler(g1));
        assertNull(rt.getReplyHandler(g2));
        assertSame(c3, rt.getReplyHandler(g3));
        assertSame(c4, rt.getReplyHandler(g4));

        //2. Test routing/re-routing abilities...with glass box tests.
        rt=new RouteTable(1000, Integer.MAX_VALUE);
        //test wrap-around
        PrivilegedAccessor.setValue(rt, "_nextID", 
                                    new Integer(Integer.MAX_VALUE));  
        assertNotNull(rt.tryToRouteReply(g1, c1));         //g1->c1
        assertNull(rt.tryToRouteReply(g1, c2));
        assertSame(c1, rt.getReplyHandler(g1));

        assertNotNull(rt.tryToRouteReply(g2, c2));         //g2->c2
        assertSame(c2, rt.getReplyHandler(g2));
        rt.routeReply(g2, c3);                           //g2->c3
        assertSame(c1, rt.getReplyHandler(g1));
        assertSame(c3, rt.getReplyHandler(g2));

        rt.removeReplyHandler(c1);                       //g1->null
        rt.removeReplyHandler(c3);                       //g2->null
        assertNull(rt.getReplyHandler(g1, 0, (short) 0));
        assertNull(rt.getReplyHandler(g2));
        assertNull(rt.tryToRouteReply(g1, c1));   
        assertNull(rt.tryToRouteReply(g2, c3));
        assertNull(rt.getReplyHandler(g1));
        assertNull(rt.getReplyHandler(g2));
        assertEquals(1, (getMap(rt, "_handlerMap")).size());           //g2 only
        assertEquals(1, (getMap(rt, "_idMap")).size());

        //Test that _idMap/_handlerMap don't grow without bound.
        rt=new RouteTable(1000, Integer.MAX_VALUE);
        assertNotNull(rt.tryToRouteReply(g1, c1));
        assertNotNull(rt.tryToRouteReply(g2, c1));
        assertNotNull(rt.tryToRouteReply(g3, c1));
        assertNotNull(rt.tryToRouteReply(g4, c1));
        assertNull(rt.tryToRouteReply(g4, c1));
        assertSame(c1, rt.getReplyHandler(g1));
        assertSame(c1, rt.getReplyHandler(g2));
        assertSame(c1, rt.getReplyHandler(g3));
        assertSame(c1, rt.getReplyHandler(g4));
        assertEquals(1, (getMap(rt, "_handlerMap")).size());
        assertEquals(1, (getMap(rt, "_idMap")).size());


        //3. Test reply counting logic.
        RouteTable.ReplyRoutePair rrp=null;
        rt=new RouteTable(MSECS/1000, Integer.MAX_VALUE);
        assertNotNull(rt.tryToRouteReply(g1, c1));  //g1 -> <c1, 0>
        rrp=rt.getReplyHandler(g1, 5, (short) 0);            //g1 -> <c1, 0+5>
        assertSame(c1, rrp.getReplyHandler());
        assertEquals(0, rrp.getBytesRouted());
        rrp=rt.getReplyHandler(g1, 1, (short) 0);            //g1 -> <c1, 5+1>
        assertSame(c1, rrp.getReplyHandler());
        assertEquals(5, rrp.getBytesRouted());
        rt.routeReply(g1, c2);                    //g1 -> <c2, 6>
        rrp=rt.getReplyHandler(g1, 2, (short) 0);            //g1 -> <c2, 6+2>
        assertSame(c2, rrp.getReplyHandler());
        assertEquals("Reply bytes", 6, 
                   rrp.getBytesRouted());
                   
        assertEquals(1, (getMap(rt, "_newMap")).size());
        assertEquals(0, (getMap(rt, "_oldMap")).size());
        try { Thread.sleep(MSECS); } catch (InterruptedException e) { }
        PrivilegedAccessor.invokeMethod(rt, "purge", null);
        assertEquals(0, (getMap(rt, "_newMap")).size());
        assertEquals(1, (getMap(rt, "_oldMap")).size());
        rrp=rt.getReplyHandler(g1, 3, (short) 0);            //g1 -> <c2, 8+3>
        assertSame(c2, rrp.getReplyHandler());
        assertEquals(8, rrp.getBytesRouted());
        rt.routeReply(g1, c3);                    //g1 -> <c3, 11>
        assertEquals(1, (getMap(rt, "_newMap")).size());
        assertEquals(0, (getMap(rt, "_oldMap")).size());
        rrp=rt.getReplyHandler(g1, 10, (short) 0);            //g1 -> <c3, 11+10>
        assertSame(c3, rrp.getReplyHandler());
        assertEquals(11, rrp.getBytesRouted());
        rt.removeReplyHandler(c3);
        assertNull(rt.getReplyHandler(g1,0, (short) 0));                    
    }


    public void testTTLAdditions() {
        RouteTable rt=null;
        int MSECS=1000;
        rt=new RouteTable(MSECS/1000, Integer.MAX_VALUE);
        byte[] g1=new byte[16]; g1[0]=(byte)1;
        byte[] g2=new byte[16]; g2[0]=(byte)2;
        byte[] g3=new byte[16]; g3[0]=(byte)3;
        byte[] g4=new byte[16]; g4[0]=(byte)4;
        ReplyHandler c1=new StubReplyHandler();
        ReplyHandler c2=new StubReplyHandler();
        ReplyHandler c3=new StubReplyHandler();
        ReplyHandler c4=new StubReplyHandler();

        // test setTTL and getAndSetTTL
        rt.setTTL(rt.tryToRouteReply(g2, c2), (byte)2);
        rt.setTTL(rt.tryToRouteReply(g1, c1), (byte)3);
        try {
            rt.setTTL(null, (byte) 1);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected) {}
        try {
            rt.setTTL(rt.tryToRouteReply(g3, c3), (byte) 0);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected) {}
        try {
            rt.setTTL(new ResultCounterImpl(), (byte) 1);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected) {}
        try {
            rt.getAndSetTTL(g3, (byte)0, (byte)2);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected) {}
        try {
            rt.getAndSetTTL(g2, (byte)2, (byte)2);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected) {}
        try {
            assertTrue(rt.getAndSetTTL(g1, (byte)3, (byte)4));
        }
        catch (IllegalArgumentException expected) {
            assertTrue(false);
        }
        try {
            assertTrue(rt.getAndSetTTL(g1, (byte)4, (byte)5));
        }
        catch (IllegalArgumentException expected) {
            assertTrue(false);
        }
        try {
            assertTrue(!rt.getAndSetTTL(g2, (byte)3, (byte)5));
        }
        catch (IllegalArgumentException expected) {
            assertTrue(false);
        }
    }

    
    private Map getMap(RouteTable rt, String mapName) 
        throws IllegalAccessException, NoSuchFieldException {
        return (Map) PrivilegedAccessor.getValue(rt, mapName);
    }

    private static final boolean debugOn = false;
    private static final void debug(Exception e) {
        if (debugOn)
            e.printStackTrace();
    }
    private static final void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }


    private static class ResultCounterImpl 
        implements com.limegroup.gnutella.search.ResultCounter {

        public int getNumResults() {
            return 0;
        }
    }

    private static class StubReplyHandler implements ReplyHandler {
        public boolean isOpen() {
            return true;
        }
        public void handlePingReply(PingReply pingReply, 
                                    ReplyHandler receivingConnection) {
        }
        public void handlePushRequest(PushRequest pushRequest, 
                                      ReplyHandler receivingConnection) {
        }
        public void handleQueryReply(QueryReply queryReply, 
                                     ReplyHandler receivingConnection) {
        }
        public int getNumMessagesReceived() {
            return 0;
        }
        public void countDroppedMessage() {
        }
        public Set getDomains() {
            return null;
        }
        public boolean isPersonalSpam(Message m) {
            return false;
        }
        public boolean isOutgoing() {
            return false;
        }
        public boolean isKillable() {
            return false;
        }
        public boolean isSupernodeClientConnection() {
            return false;
        }
        public boolean isLeafConnection() {
            return false;
        }
        public boolean isHighDegreeConnection() {
            return false;
        }

        public boolean isUltrapeerQueryRoutingConnection() {
            return false;
        }
    }

}
