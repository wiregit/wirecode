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
        int MSECS=1000;
        rt=new RouteTable(MSECS/1000, Integer.MAX_VALUE);
        rt.routeReply(g1, c1);
        rt.routeReply(g2, c2);                    //old, new:
        rt.routeReply(g3, c3);                    //{}, {g1, g2, g3}
        assertTrue(rt.getReplyHandler(g1)==c1);
        assertTrue(rt.getReplyHandler(g2)==c2);
        assertTrue(rt.getReplyHandler(g3, 0, (short) 0).getReplyHandler()==c3);
        assertTrue(rt.getReplyHandler(g4, 0, (short) 0)==null);
        try { Thread.sleep(MSECS); } catch (InterruptedException e) { }
        assertTrue(rt.tryToRouteReply(g4, c4)!=null);  //{g1, g2, g3}, {g4}
        assertTrue(rt.getReplyHandler(g1)==c1);
        rt.routeReply(g1, c1);                    //{g2, g3}, {g1, g4}
        assertTrue(rt.getReplyHandler(g1)==c1);
        assertTrue(rt.getReplyHandler(g2, 0, (short) 0).getReplyHandler()==c2);
        assertTrue(rt.getReplyHandler(g3)==c3);
        assertTrue(rt.getReplyHandler(g4)==c4);
        try { Thread.sleep(MSECS); } catch (InterruptedException e) { }
        rt.routeReply(g2, c3);                     //{g1, g4}, {g2}
        debug(rt.toString());
        assertTrue(rt.getReplyHandler(g1)==c1);
        assertTrue(rt.getReplyHandler(g2)==c3);
        assertTrue(rt.getReplyHandler(g3)==null);
        assertTrue(rt.getReplyHandler(g4)==c4);
        try { Thread.sleep(MSECS); } catch (InterruptedException e) { }
        assertTrue(rt.tryToRouteReply(g2, c2)==null);  //{g2}, {}}
        assertTrue(rt.getReplyHandler(g1)==null);
        assertTrue(rt.getReplyHandler(g2)==c3);
        assertTrue(rt.getReplyHandler(g3)==null);
        assertTrue(rt.getReplyHandler(g4)==null);
        rt.routeReply(g2, c2);                      //{}, {g2}
        assertTrue(rt.getReplyHandler(g1)==null);
        assertTrue(rt.getReplyHandler(g2)==c2);
        assertTrue(rt.getReplyHandler(g3)==null);
        assertTrue(rt.getReplyHandler(g4)==null);

        //1.5 Test space replacment (hard upper bounds)
        rt=new RouteTable(Integer.MAX_VALUE, 1);
        rt.routeReply(g1, c1);
        rt.routeReply(g2, c2);                      //{g1}, {g2}
        assertTrue(rt.getReplyHandler(g1)==c1);
        assertTrue(rt.getReplyHandler(g2)==c2);
        rt.routeReply(g3, c3);                      //{g3}, {g2}
        assertTrue(rt.getReplyHandler(g1)==null);
        assertTrue(rt.getReplyHandler(g2)==c2);
        assertTrue(rt.getReplyHandler(g3)==c3);
        rt.routeReply(g4, c4);
        assertTrue(rt.getReplyHandler(g1)==null);
        assertTrue(rt.getReplyHandler(g2)==null);
        assertTrue(rt.getReplyHandler(g3)==c3);
        assertTrue(rt.getReplyHandler(g4)==c4);

        //2. Test routing/re-routing abilities...with glass box tests.
        rt=new RouteTable(1000, Integer.MAX_VALUE);
        //test wrap-around
        PrivilegedAccessor.setValue(rt, "_nextID", 
                                    new Integer(Integer.MAX_VALUE));  
        assertTrue(rt.tryToRouteReply(g1, c1)!=null);         //g1->c1
        assertTrue(rt.tryToRouteReply(g1, c2)==null);
        assertTrue(rt.getReplyHandler(g1)==c1);

        assertTrue(rt.tryToRouteReply(g2, c2)!=null);         //g2->c2
        assertTrue(rt.getReplyHandler(g2)==c2);
        rt.routeReply(g2, c3);                           //g2->c3
        assertTrue(rt.getReplyHandler(g1)==c1);
        assertTrue(rt.getReplyHandler(g2)==c3);

        rt.removeReplyHandler(c1);                       //g1->null
        rt.removeReplyHandler(c3);                       //g2->null
        assertTrue(rt.getReplyHandler(g1, 0, (short) 0)==null);
        assertTrue(rt.getReplyHandler(g2)==null);
        assertTrue(rt.tryToRouteReply(g1, c1)==null);   
        assertTrue(rt.tryToRouteReply(g2, c3)==null);
        assertTrue(rt.getReplyHandler(g1)==null);
        assertTrue(rt.getReplyHandler(g2)==null);
        assertTrue((getMap(rt, "_handlerMap")).size()==1);           //g2 only
        assertTrue((getMap(rt, "_idMap")).size()==1);

        //Test that _idMap/_handlerMap don't grow without bound.
        rt=new RouteTable(1000, Integer.MAX_VALUE);
        assertTrue(rt.tryToRouteReply(g1, c1)!=null);
        assertTrue(rt.tryToRouteReply(g2, c1)!=null);
        assertTrue(rt.tryToRouteReply(g3, c1)!=null);
        assertTrue(rt.tryToRouteReply(g4, c1)!=null);
        assertTrue(rt.tryToRouteReply(g4, c1)==null);
        assertTrue(rt.getReplyHandler(g1)==c1);
        assertTrue(rt.getReplyHandler(g2)==c1);
        assertTrue(rt.getReplyHandler(g3)==c1);
        assertTrue(rt.getReplyHandler(g4)==c1);
        assertTrue((getMap(rt, "_handlerMap")).size()==1);
        assertTrue((getMap(rt, "_idMap")).size()==1);


        //3. Test reply counting logic.
        RouteTable.ReplyRoutePair rrp=null;
        rt=new RouteTable(MSECS/1000, Integer.MAX_VALUE);
        assertTrue(rt.tryToRouteReply(g1, c1)!=null);  //g1 -> <c1, 0>
        rrp=rt.getReplyHandler(g1, 5, (short) 0);            //g1 -> <c1, 0+5>
        assertTrue(rrp.getReplyHandler()==c1);
        assertTrue(rrp.getBytesRouted()==0);
        rrp=rt.getReplyHandler(g1, 1, (short) 0);            //g1 -> <c1, 5+1>
        assertTrue(rrp.getReplyHandler()==c1);
        assertTrue(rrp.getBytesRouted()==5);
        rt.routeReply(g1, c2);                    //g1 -> <c2, 6>
        rrp=rt.getReplyHandler(g1, 2, (short) 0);            //g1 -> <c2, 6+2>
        assertTrue(rrp.getReplyHandler()==c2);
        assertTrue("Reply bytes: "+rrp.getBytesRouted(), 
                   rrp.getBytesRouted()==6);
                   
        assertTrue((getMap(rt, "_newMap")).size()==1);
        assertTrue((getMap(rt, "_oldMap")).size()==0);
        try { Thread.sleep(MSECS); } catch (InterruptedException e) { }
        PrivilegedAccessor.invokeMethod(rt, "purge", null);
        assertTrue((getMap(rt, "_newMap")).size()==0);
        assertTrue((getMap(rt, "_oldMap")).size()==1);
        rrp=rt.getReplyHandler(g1, 3, (short) 0);            //g1 -> <c2, 8+3>
        assertTrue(rrp.getReplyHandler()==c2);
        assertTrue(rrp.getBytesRouted()==8);
        rt.routeReply(g1, c3);                    //g1 -> <c3, 11>
        assertTrue((getMap(rt, "_newMap")).size()==1);
        assertTrue((getMap(rt, "_oldMap")).size()==0);
        rrp=rt.getReplyHandler(g1, 10, (short) 0);            //g1 -> <c3, 11+10>
        assertTrue(rrp.getReplyHandler()==c3);
        assertTrue(rrp.getBytesRouted()==11);
        rt.removeReplyHandler(c3);
        assertTrue(rt.getReplyHandler(g1,0, (short) 0)==null);                    
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
    }

}
