package com.limegroup.gnutella;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;

public class BusyLeafQRTUpdateTest extends com.limegroup.gnutella.util.BaseTestCase {


    /**
     * The local stub for testing busy leaf QRT updating functionality
     */
    static class ConnectionManagerStubOvr extends ConnectionManagerStub {
       
        public void addStubOvrConnection( ManagedConnectionStubOvr mcso ){
            mcso._managerStub=this;
            getConnections().add(mcso);
        }
    }
    
    /**
     * The local stub for testing busy leaf connections
     */
    static class ManagedConnectionStubOvr extends ManagedConnectionStub {      
        
//        private boolean _isBusy=false;
        private boolean _isPeer=false;
        private volatile int softMaxHops = -1;
        
        public ConnectionManagerStubOvr _managerStub=null;
        
        public boolean isSupernodeClientConnection() {
            return !_isPeer;
        }
        
        public boolean isClientSupernodeConnection() {
            return _isPeer;
        }

/*        
        public boolean isBusyEnoughToTriggerQRTRemoval(){
            return _isBusy;
        }
*/
        public void setBusyEnoughToTriggerQRTRemoval( boolean busy ){
//            _isBusy = busy;
            softMaxHops=0;            
            
            setBusyTime(busy);
            if( _managerStub!=null && busy )
                _managerStub.setAnyLeafHasBecomeBusy(busy);
        }
        
        public void setPeerConnection( boolean peer ){
            _isPeer = peer;
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

    /**
     * Start of BusyLeafQRTUpdateTest suite of tests
     * 
     * @throws Exception
     */
	public void testBusyLeafNoticed() throws Exception {
        
        ConnectionManagerStubOvr cm=new ConnectionManagerStubOvr();
        
        ManagedConnectionStubOvr peer=new ManagedConnectionStubOvr(); 
        ManagedConnectionStubOvr leaf=new ManagedConnectionStubOvr();
        
        peer.setPeerConnection(true);
        leaf.setPeerConnection(false);
        
        cm.addStubOvrConnection(peer);
        cm.addStubOvrConnection(leaf);
        
        leaf.setBusyEnoughToTriggerQRTRemoval(true);
        
        //  Should't work for >20 seconds
        assertFalse( cm.isAnyBusyLeafTriggeringQRTUpdate() ); 

        int sleepSecs=(int)(ManagedConnection.MIN_BUSY_LEAF_TIME/1000) + 5;
        
        System.out.println("Starting to sleep for " + sleepSecs + " seconds...");
        
        for (int i = 0; i < sleepSecs; i++) {
            if( (sleepSecs<=5) || (i%5)==0 )
                System.out.println( (sleepSecs-i) + " secs..." );
            Thread.sleep(1000);
        }
        System.out.print("Done sleeping...");
        
        //  Should work, since >20 seconds have elapsed
        assertTrue( cm.isAnyBusyLeafTriggeringQRTUpdate() ); 
        
        //  Shouldn't work, prior one should have cleared the busy flag.
        assertFalse( cm.isAnyBusyLeafTriggeringQRTUpdate() );
    }
    
    public void testBusyPeerNotNoticed() throws Exception {
        
        ConnectionManagerStubOvr cm=new ConnectionManagerStubOvr();
        
        ManagedConnectionStubOvr peer=new ManagedConnectionStubOvr(); 
        ManagedConnectionStubOvr leaf=new ManagedConnectionStubOvr();
        
        peer.setPeerConnection(true);
        leaf.setPeerConnection(false);
        
        cm.addStubOvrConnection(peer);
        cm.addStubOvrConnection(leaf);
        
        peer.setBusyEnoughToTriggerQRTRemoval(true);
        
        //  Should't work at all...
        assertFalse( cm.isAnyBusyLeafTriggeringQRTUpdate() ); 

        int sleepSecs=(int)(ManagedConnection.MIN_BUSY_LEAF_TIME/1000) + 5;
        
        System.out.println("Starting to sleep for " + sleepSecs + " seconds...");
        
        for (int i = 0; i < sleepSecs; i++) {
            if( (sleepSecs<=5) || (i%5)==0 )
                System.out.println( (sleepSecs-i) + " secs..." );
            Thread.sleep(1000);
        }
        System.out.print("Done sleeping...");
        
        //  Shouldn't work still
        assertFalse( cm.isAnyBusyLeafTriggeringQRTUpdate() );
    }
    
    public void testNonBusyLastHop() throws Exception {
        
        ConnectionManagerStubOvr cm=new ConnectionManagerStubOvr();
        
        ManagedConnectionStubOvr peer=new ManagedConnectionStubOvr(); 
        ManagedConnectionStubOvr leaf=new ManagedConnectionStubOvr();
        
        peer.setPeerConnection(true);
        leaf.setPeerConnection(false);
        
        cm.addStubOvrConnection(peer);
        cm.addStubOvrConnection(leaf);
        
        
    }
    
    public void testBusyLastHop() throws Exception {
        
    }
    
    
    
}



