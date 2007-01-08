package com.limegroup.gnutella;

import java.util.Iterator;
import java.util.List;

import org.limewire.service.ErrorService;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class BusyLeafQRTUpdateTest extends LimeTestCase {
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
        
        peer.setLeafConnection(false);
        leaf.setLeafConnection(true);
        
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
        assertFalse( isAnyBusyLeafTriggeringQRTUpdate() ); 

        waitForSeconds();
        
        //  Should work, since >TEST_MIN_BUSY_LEAF_TIME seconds have elapsed
        assertTrue( isAnyBusyLeafTriggeringQRTUpdate() ); 
        
        //  UPDATE: Should still work, prior one should NOT have cleared the busy flag.
        assertTrue( isAnyBusyLeafTriggeringQRTUpdate() );
    }
    
    public void testBusyPeerNotNoticed() throws Exception {
         peer.setBusyEnoughToTriggerQRTRemoval(true);
        
        //  Should't work at all...
        assertFalse( isAnyBusyLeafTriggeringQRTUpdate() ); 

        waitForSeconds();
         
        //  Shouldn't work still
        assertFalse( isAnyBusyLeafTriggeringQRTUpdate() );
    }
    
    public void testNonBusyLastHop() throws Exception {
        //  No busy leaves
        forwardQueryRouteTablesCaller();
        assertEquals( 1, getTotalNumberOfLeavesQRTsIncluded() );
        cm.clearConnectionQRTStatus();
        waitForSeconds();
        
        //  Still no busy leaves
        forwardQueryRouteTablesCaller();
        assertEquals( 1, getTotalNumberOfLeavesQRTsIncluded() );
    }
    
    public void testBusyLastHop() throws Exception {
        //  No busy leaves
        forwardQueryRouteTablesCaller();
        assertEquals( 1, getTotalNumberOfLeavesQRTsIncluded() );
        cm.clearConnectionQRTStatus();
        
        leaf.setBusyEnoughToTriggerQRTRemoval(true);
        waitForSeconds();
        
        //  A busy leaf should have been noticed
        forwardQueryRouteTablesCaller();
        assertEquals( 0, getTotalNumberOfLeavesQRTsIncluded() );
    }
    
    public void forwardQueryRouteTablesCaller() throws Exception {
        PrivilegedAccessor.invokeMethod(mr, "forwardQueryRouteTables", null );
    }
    
    
    /**
     * Shortcut to waiting for awhile
     *
     */
    public void waitForSeconds() throws Exception {
            Thread.sleep(5000 + (ManagedConnectionCountQRT.TEST_MIN_BUSY_LEAF_TIME+5));
    }
    
    /**
     * Check to see how many leaves' tables would be included in the LastHop QRT
     * @return number of leaf tables which would be included in a LastHop QRT
     */
    private int getTotalNumberOfLeavesQRTsIncluded() {
        int leaves=0;
        
        Iterator it=cm.getInitializedClientConnections().iterator();
        
        while( it.hasNext() ){
            ManagedConnectionCountQRT mc=((ManagedConnectionCountQRT)it.next());
            
            if( mc._qrtIncluded )
                leaves++;
        }
        
        return leaves;
    }
    
    /**
     * Loops through all managed connections and checks them for whether they have 
     * been a busy leaf long enough to trigger a last-hop QRT update.
     * 
     * @return true iff the last-hop QRT tables should be updated early.
     */
     public boolean isAnyBusyLeafTriggeringQRTUpdate(){
        boolean busyLeaf=false;
        List list=cm.getInitializedClientConnections();
        Iterator it=list.iterator();
        
        while( !busyLeaf && it.hasNext() )
            //  NOTE: don't remove the leaf's BUSY flag, since some peers may not have
            //  been updated yet due to timing quirks.
            if( ((ManagedConnection)it.next()).isBusyEnoughToTriggerQRTRemoval() )
                busyLeaf=true;
        
        return busyLeaf;
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
            PrivilegedAccessor.invokeMethod( 
                    this, "connectionInitializing",  
                    new Object[] { mcso }, new Class[] { ManagedConnection.class } );

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
    }
    
    /**
     * The local stub for testing busy leaf connections
     */
    static class ManagedConnectionCountQRT extends ManagedConnectionStub {      
        
        private static final long TEST_MIN_BUSY_LEAF_TIME=1000*5;
        public ManagedConnectionCountQRT() {
            try {
                PrivilegedAccessor.setValue(ManagedConnection.class, "MIN_BUSY_LEAF_TIME", new Long(TEST_MIN_BUSY_LEAF_TIME));                
                PrivilegedAccessor.setValue(this, "hopsFlowMax", new Integer(-1));
            } catch (Exception e) {
                ErrorService.error( e );
            }
        }
        
        private boolean isLeaf = false;
        
        public long getNextQRPForwardTime() {
            return 0l;
        }        
        
        public void incrementNextQRPForwardTime(long curTime) {
        }

        public boolean isUltrapeerQueryRoutingConnection() {
            return !isLeaf;
        }
        
        /** 
         * If I'm not a leaf then I'm an Ultrapeer! See also 
         * isClientSupernodeConnection()
         */
        public boolean isSupernodeClientConnection() {
            return isLeaf;
        }
        
        /** 
         * If I'm not a leaf then I'm an Ultrapeer! See also 
         * isSupernodeClientConnection()
         */
        public boolean isClientSupernodeConnection() {
            return isLeaf;
        }
        
        public boolean isQueryRoutingEnabled() {
            return !isLeaf;
        }
        
        public void send(Message m) {}

        public void setBusyEnoughToTriggerQRTRemoval( boolean busy ) throws Exception{
            PrivilegedAccessor.setValue(this, "hopsFlowMax", new Integer( ((busy)?(0):(-1)) ) );
            
            setBusy(busy);
         }
        
        public void setLeafConnection( boolean isLeaf ){
            this.isLeaf = isLeaf;
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



