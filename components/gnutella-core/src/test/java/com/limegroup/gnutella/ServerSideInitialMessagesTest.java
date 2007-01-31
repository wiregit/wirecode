package com.limegroup.gnutella;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 *  Tests that an Ultrapeer and Leaf correctly send and parse some initial messages
 *
 */
public final class ServerSideInitialMessagesTest extends ServerSideTestCase {
    protected static int TIMEOUT = 2000;

    private List /*of Messsage*/ _queue;

    public ServerSideInitialMessagesTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideInitialMessagesTest.class);
    }    
   
    public static Integer numUPs() {
        return new Integer(1);
    }

    public static Integer numLeaves() {
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
    public void testInitialPeerMessages() throws Exception{
        Connection up=ULTRAPEER[0];
        Thread.sleep( 11*1000 );
        parseWaitingMessages( up );

        // Check that initial messages are sent & received correctly
        Message mCapVM=getFirstMessageOfTypeFromQueue( CapabilitiesVM.class );
        Message mVendS=getFirstMessageOfTypeFromQueue( MessagesSupportedVendorMessage.class );
        Message mReset=getFirstMessageOfTypeFromQueue( ResetTableMessage.class );
        Message mPatch=getFirstMessageOfTypeFromQueue( PatchTableMessage.class );
        
        //  UP may support PONG CACHING so we don't send it an initial ping.
        Message mPingR=getFirstMessageOfTypeFromQueue( PingRequest.class );
        
        assertNotNull( mCapVM );
        assertNotNull( mVendS );
        assertNotNull( mReset );
        assertNotNull( mPatch );
        //  see above
        // assertNotNull( mPingR );
        
        assertEquals( "Peer messages queue not empty" + _queue, 0, _queue.size() );
    }
    // ------------------------------------------------------
    public void testInitialLeafMessages() throws Exception {
        Connection leaf=LEAF[0];
        
        Thread.sleep( 5*1000 );
        parseWaitingMessages( leaf );

        // Check that initial messages are sent & received correctly
        Message mCapvm=getFirstMessageOfTypeFromQueue( CapabilitiesVM.class );
        Message mVendS=getFirstMessageOfTypeFromQueue( MessagesSupportedVendorMessage.class );
        
        //  Leaf may support PONG CACHING so we don't send it an initial ping.
        Message mqePingR=getFirstMessageOfTypeFromQueue( PingRequest.class );
        
        assertNotNull( mCapvm );
        assertNotNull( mVendS );
        //  See above
        // assertNotNull( mqePingR );
        
        assertEquals( "Leaf messages queue not empty" + _queue, 0, _queue.size() );
    }
    // ======================================================
    
    //  Utility functions   
    private final void sendF(Connection c, Message m) throws Exception {
        c.send(m);
        c.flush();
    }
    
    private void parseWaitingMessages( Connection con ) throws Exception {
        try {
            Message m = null;
            while((m = con.receive( 300 )) !=null ) {
                _queue.add( m );
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



