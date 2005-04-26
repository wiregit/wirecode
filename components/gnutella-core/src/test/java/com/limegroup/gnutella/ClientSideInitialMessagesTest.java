package com.limegroup.gnutella;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class ClientSideInitialMessagesTest extends ClientSideTestCase {
    protected static int TIMEOUT = 2000;

    private List /*of Messsage*/ _queue;

    public ClientSideInitialMessagesTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideInitialMessagesTest.class);
    }    
   
    public static Integer numUPs() {
        return new Integer(1);
    }
    
    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    
    public static void setUpQRPTables() {}
    
    public void setUp() {
        _queue=new ArrayList(10);
    }

    // BEGIN TESTS
    // ======================================================
    public void testInitialUpMessages() throws Exception {
        Connection up=testUP[0];
        
        Thread.sleep( 15*1000 );
        parseWaitingMessages( up );
        
        // Check that initial messages are setn & received correctly
        Message mqeReset=getFirstMessageOfTypeFromQueue( ResetTableMessage.class );
        Message mqePatch=getFirstMessageOfTypeFromQueue( PatchTableMessage.class );
        //  Host supports Pong Caching, so an initial Ping isn't sent
//    Message mqePingR=getFirstMessageOfTypeFromQueue( PingRequest.class );
        
        assertTrue( mqeReset!=null );
        assertTrue( mqePatch!=null );
        //  See Above
//    assertTrue( mqePingR!=null );
        
        assertEquals( "UP messages queue not empty" + _queue, 0, _queue.size() );
    }
    // ======================================================
    
    //  Utility functions   
    private final void sendF(Connection c, Message m) throws Exception {
        c.send(m);
        c.flush();
    }
    
    private void parseWaitingMessages( Connection con ) throws Exception {
        try {
            Message m=con.receive( 100 );
            
            while( m!=null ) {
                _queue.add( m );
                m=con.receive(100);
            }
        } catch (InterruptedIOException ie) {
        }
    }

    private Message getFirstMessageOfTypeFromQueue( Class type ) {
        ListIterator li=_queue.listIterator();
        Message m=null;
        
        while( li.hasNext() ) {
            Message n=(Message)li.next();
            
            if( type.isInstance(n) ) {
                m = n;
                li.remove();
                break;
            }
        }
            
        return m;
    }
    


}
