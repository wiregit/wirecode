package com.limegroup.gnutella;

import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.BaseTestCase;

public class BusyLeafQRTUpdateTest extends BaseTestCase {
    private ManagedConnectionCountQRT peer; 
    private ManagedConnectionCountQRT leaf;
    private ConnectionManagerCountQRT cm;
    private MessageRouter mr;
    
    public static void globalSetUp() throws Exception {
        new RouterService( new ActivityCallbackStub() );
    }
    
    public void setUp() throws Exception {
        cm=new ConnectionManagerCountQRT();
        
        peer=new ManagedConnectionCountQRT();
        leaf=new ManagedConnectionCountQRT();
        
        peer.setPeerConnection(true);
        leaf.setPeerConnection(false);
        
        cm.addStubOvrConnection(peer);
        cm.addStubOvrConnection(leaf);
        
        mr=RouterService.getMessageRouter();
        PrivilegedAccessor.setValue( MessageRouter.class, "_manager", cm);
        PrivilegedAccessor.setValue( MessageRouter.class, "_fileManager", new FileManagerStub() );
        PrivilegedAccessor.setValue( RouterService.class, "manager", cm);
    }
    

    /**
     * Start of BusyLeafQRTUpdateTest suite of tests
     * 
     * @throws Exception
     */
	public void testBusyLeafNoticed() throws Exception {
        leaf.setBusyEnoughToTriggerQRTRemoval(true);
        
        //  Should't work for >TEST_MIN_BUSY_LEAF_TIME seconds
        assertFalse( cm.isAnyBusyLeafTriggeringQRTUpdate() ); 

        waitForSeconds();
        
        //  Should work, since >TEST_MIN_BUSY_LEAF_TIME seconds have elapsed
        assertTrue( cm.isAnyBusyLeafTriggeringQRTUpdate() ); 
        
        //  Shouldn't work, prior one should have cleared the busy flag.
        assertFalse( cm.isAnyBusyLeafTriggeringQRTUpdate() );
    }
    
    public void testBusyPeerNotNoticed() throws Exception {
         peer.setBusyEnoughToTriggerQRTRemoval(true);
        
        //  Should't work at all...
        assertFalse( cm.isAnyBusyLeafTriggeringQRTUpdate() ); 

        waitForSeconds();
         
        //  Shouldn't work still
        assertFalse( cm.isAnyBusyLeafTriggeringQRTUpdate() );
    }
    
    public void testNonBusyLastHop() throws Exception {
        //  No busy leaves
        forwardQueryRouteTablesCaller();
        assertTrue( 1==getTotalNumberOfLeavesQRTsIncluded() );
        cm.clearConnectionQRTStatus();
        waitForSeconds();
        
        //  Still no busy leaves
        forwardQueryRouteTablesCaller();
        assertTrue( 1==getTotalNumberOfLeavesQRTsIncluded() );
    }
    
    public void testBusyLastHop() throws Exception {
        //  No busy leaves
        forwardQueryRouteTablesCaller();
        assertTrue( 1==getTotalNumberOfLeavesQRTsIncluded() );
        cm.clearConnectionQRTStatus();
        
        leaf.setBusyEnoughToTriggerQRTRemoval(true);
        waitForSeconds();
        
        //  A busy leaf should have been noticed
        forwardQueryRouteTablesCaller();
        assertTrue( 0==getTotalNumberOfLeavesQRTsIncluded() );
    }
    
    public void forwardQueryRouteTablesCaller() throws Exception {
        PrivilegedAccessor.invokeMethod(mr, "forwardQueryRouteTables", null );
    }
    
    
    /**
     * Shortcut to waiting for awhile
     *
     */
    public void waitForSeconds() throws Exception {
        waitForSeconds( (int)(ManagedConnectionCountQRT.TEST_MIN_BUSY_LEAF_TIME/1000) + 5, true );
    }
    public void waitForSeconds( int seconds, boolean print ) throws Exception {
        if( print )
            System.out.println("Starting to sleep for " + seconds + " seconds...");
        
        for (int i = 0; i < seconds; i++) {
            if( print && ((seconds<=5) || (i%5)==0) )
                System.out.println( (seconds-i) + " secs..." );
            Thread.sleep(1000);
        }
        if( print )
            System.out.print("Done sleeping...");
    }
    
    /**
     * Check to see how many leaves' tables would be included in the LastHop QRT
     * @return number of leaf tables which would be included in a LastHop QRT
     */
    private int getTotalNumberOfLeavesQRTsIncluded() {
        int leaves=0;
        
        Iterator it=cm.getConnections().iterator();
        
        while( it.hasNext() ){
            if( ((ManagedConnectionCountQRT)it.next())._qrtIncluded )
                leaves++;
        }
        
        return leaves;
    }
    
    /**
     * The local stub for testing busy leaf QRT updating functionality
     * 
     * Attaches ManagedConnectionStubs to this, and performs superclass related 
     * functionality on them
     * 
     * Also, adds method clearConnectionQRTStatus(), which loops through connections 
     * and clears the per-connection flag which is used to indicate whether or not a 
     * connection's QRT table was included in the LastHop QRT.
     */
    static class ConnectionManagerCountQRT extends ConnectionManagerStub {
       
        public void addStubOvrConnection( ManagedConnectionCountQRT mcso ) throws Exception {
            mcso._managerStub=this;
            PrivilegedAccessor.invokeMethod( 
                    this, "connectionInitializing",  
                    new Object[] { mcso }, new Class[] { Connection.class } );

            PrivilegedAccessor.invokeMethod( 
                    this, "connectionInitialized",  
                    new Object[] { mcso }, new Class[] { ManagedConnection.class } );
        }
        
        /**
         * Loop through connections and clear each one's "QRT Included" flag 
         */
        public void clearConnectionQRTStatus() {
            Iterator it=getConnections().iterator();
            while( it.hasNext() ) {
                ManagedConnectionCountQRT mc=((ManagedConnectionCountQRT)it.next());
                if( mc._qrtIncluded ) {
                    mc._qrtIncluded=false;
                }
            }
        }

//        public boolean isSupernode() {
//            return true;    //  All tests in BusyLeafQRTTest class assume this...
//        }
        
    }
    
    /**
     * The local stub for testing busy leaf connections
     */
    static class ManagedConnectionCountQRT extends ManagedConnectionStub {      
        
        private static final long TEST_MIN_BUSY_LEAF_TIME=1000*5;
        public ManagedConnectionCountQRT() {
            try {
                PrivilegedAccessor.setValue(ManagedConnection.class, "MIN_BUSY_LEAF_TIME", new Long(TEST_MIN_BUSY_LEAF_TIME));                
                PrivilegedAccessor.setValue(this, "softMaxHops", new Integer(-1));
            } catch (Exception e) {
                ErrorService.error( e );
            }
        }
        
//        private boolean _isBusy=false;
        private boolean _isPeer=false;
        
        public ConnectionManagerCountQRT _managerStub=null;
        
        public long getNextQRPForwardTime() {
            return 0l;
        }        
        
        public void incrementNextQRPForwardTime(long curTime) {
        }

        public boolean isUltrapeerQueryRoutingConnection() {
            return _isPeer;
        }
        
        public boolean isSupernodeClientConnection() {
            return !_isPeer;
        }
        
        public boolean isClientSupernodeConnection() {
            return _isPeer;
        }
        
        public boolean isQueryRoutingEnabled() {
            return _isPeer;
        }
        
        public void send(Message m) {}

        public void setBusyEnoughToTriggerQRTRemoval( boolean busy ) throws Exception{
            PrivilegedAccessor.setValue(this, "softMaxHops", new Integer( ((busy)?(0):(-1)) ) );
            
            setBusyTime(busy);
            if( _managerStub!=null && busy )
                _managerStub.setAnyLeafHasBecomeBusy(busy);
        }
        
        public void setPeerConnection( boolean peer ){
            _isPeer = peer;
        }

        public boolean _qrtIncluded=false;
        public QueryRouteTable getQueryRouteTableReceived() {
            _qrtIncluded = true;
            return super.getQueryRouteTableReceived();
        }
    }
    
    /**
     * JUnit crap...
     */
    public BusyLeafQRTUpdateTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BusyLeafQRTUpdateTest.class);
    }
    
    
}



