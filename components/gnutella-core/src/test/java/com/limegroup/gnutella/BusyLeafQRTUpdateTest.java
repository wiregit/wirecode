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

    /**
     * The local stub for testing busy leaf QRT updating functionality
     */
    static class FileManagerStubOvr extends FileManagerStub{
        public synchronized QueryRouteTable getQRT() {
            QueryRouteTable qrt = new QueryRouteTable(2);
            return qrt;
        }

    }
    
    /**
     * The local stub for testing busy leaf QRT updating functionality
     */
    static class MessageRouterStubOvr extends MessageRouterStub {
        public MessageRouterStubOvr( ConnectionManagerStubOvr cm )throws Exception {
            _manager=cm;
            try {
                PrivilegedAccessor.setValue( MessageRouter.class, "_fileManager", new FileManagerStubOvr() );
            } catch (Exception e){
                ErrorService.error(e);
            }
        }
        
        public void forwardQueryRouteTablesStub() throws Exception {
            try {
                PrivilegedAccessor.invokeMethod(this, "forwardQueryRouteTables", null );
            }catch(Exception e){
                System.out.println("Exception: " + e.getCause() );
                ErrorService.error(e);
            }
        }
        
        
    }        
    
    /**
     * The local stub for testing busy leaf QRT updating functionality
     */
    static class ConnectionManagerStubOvr extends ConnectionManagerStub {
       
        public void addStubOvrConnection( ManagedConnectionStubOvr mcso ){
            mcso._managerStub=this;
            getConnections().add(mcso);
            
            if( !mcso.isClientSupernodeConnection() )
                super.getInitializedClientConnections().add(mcso);
        }
        
        public List getInitializedConnections() {
            return getConnections();
        }

        public void clearConnectionQRTStatus() {
            Iterator it=getConnections().iterator();
            while( it.hasNext() ) {
                ManagedConnectionStubOvr mc=((ManagedConnectionStubOvr)it.next());
                if( mc._qrtIncluded ) {
                    mc._qrtIncluded=false;
                }
            }
        }
        
        public List getInitializedClientConnections() {
            return super.getInitializedClientConnections();
        }
        
        public boolean isSupernode() {
            return true;    //  All tests in BusyLeafQRTTest class assume this...
        }
        
    }
    
    /**
     * The local stub for testing busy leaf connections
     */
    static class ManagedConnectionStubOvr extends ManagedConnectionStub {      
        
        private static final long TEST_MIN_BUSY_LEAF_TIME=1000*5;
        public ManagedConnectionStubOvr() {
            try {
                PrivilegedAccessor.setValue(ManagedConnection.class, "MIN_BUSY_LEAF_TIME", new Long(TEST_MIN_BUSY_LEAF_TIME));                
                PrivilegedAccessor.setValue(this, "softMaxHops", new Integer(-1));
            } catch (Exception e) {
                ErrorService.error( e );
            }
        }
        
//        private boolean _isBusy=false;
        private boolean _isPeer=false;
        
        public ConnectionManagerStubOvr _managerStub=null;
        
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
    
    
    private ManagedConnectionStubOvr peer; 
    private ManagedConnectionStubOvr leaf;
    private ConnectionManagerStubOvr cm;
    private MessageRouterStubOvr mr;
    
    public void setUp() throws Exception {
        cm=new ConnectionManagerStubOvr();
        
        peer=new ManagedConnectionStubOvr();
        leaf=new ManagedConnectionStubOvr();
        
        peer.setPeerConnection(true);
        leaf.setPeerConnection(false);
        
        cm.addStubOvrConnection(peer);
        cm.addStubOvrConnection(leaf);
        
        try {
            mr=new MessageRouterStubOvr(cm);
            PrivilegedAccessor.setValue( RouterService.class, "manager", cm);
        } catch (Exception e) {
            ErrorService.error(e);
        }
    }
    

    /**
     * Start of BusyLeafQRTUpdateTest suite of tests
     * 
     * @throws Exception
     */
	public void testBusyLeafNoticed() throws Exception {
        leaf.setBusyEnoughToTriggerQRTRemoval(true);
        
        //  Should't work for >20 seconds
        assertFalse( cm.isAnyBusyLeafTriggeringQRTUpdate() ); 

        waitForSeconds();
        
        //  Should work, since >20 seconds have elapsed
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
        mr.forwardQueryRouteTablesStub();
        assertTrue( 1==getTotalNumberOfLeavesQRTsIncluded() );
        cm.clearConnectionQRTStatus();
        waitForSeconds();
        
        //  Still no busy leaves
        mr.forwardQueryRouteTablesStub();
        assertTrue( 1==getTotalNumberOfLeavesQRTsIncluded() );
    }
    
    public void testBusyLastHop() throws Exception {
        //  No busy leaves
        mr.forwardQueryRouteTablesStub();
        assertTrue( 1==getTotalNumberOfLeavesQRTsIncluded() );
        cm.clearConnectionQRTStatus();
        
        leaf.setBusyEnoughToTriggerQRTRemoval(true);
        waitForSeconds();
        
        //  A busy leaf should have been noticed
        mr.forwardQueryRouteTablesStub();
        assertTrue( 0==getTotalNumberOfLeavesQRTsIncluded() );
    }
    
    /**
     * Shortcut to waiting for awhile
     *
     */
    public void waitForSeconds() throws Exception {
        waitForSeconds( (int)(ManagedConnectionStubOvr.TEST_MIN_BUSY_LEAF_TIME/1000) + 5, true );
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
            if( ((ManagedConnectionStubOvr)it.next())._qrtIncluded )
                leaves++;
        }
        
        return leaves;
    }
    
    /**
     * Check to see how many leaves are currently in a busy-signalling state, and
     * would cause peers' LastHop QRT tables to be updated early...
     * 
     * @return number of signalling busy leaves
     */
    private int getTotalNumberOfBusySignallingLeaves() {
        int leaves=0;
        //  TODO: finish this...
        return leaves;
    }
    
}



