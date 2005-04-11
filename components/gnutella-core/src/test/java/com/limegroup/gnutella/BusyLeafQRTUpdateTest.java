package com.limegroup.gnutella;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;

public class BusyLeafQRTUpdateTest extends com.limegroup.gnutella.util.BaseTestCase {


    /**
     * The local stub for testing busy leaf QRT updating functionality
     */
    static class ConnectionManagerStubOvr extends ConnectionManagerStub {
        
        private boolean _isBusy=false;
        
        public boolean isAnyBusyLeafTriggeringQRTUpdate(){
            //  TODO: finish this
            //return _isBusy;
            return super.isAnyBusyLeafTriggeringQRTUpdate();
        }
        
        public void setIsAnyBusyLeafTriggeringQRTUpdate( boolean busy ){
            _isBusy = busy;
        }
        
        public void addStubOvrConnection( ManagedConnectionStubOvr mcso ){
            //  TODO: finish this
        }
    }
    
    /**
     * The local stub for testing busy leaf connections
     */
    static class ManagedConnectionStubOvr extends ManagedConnectionStub {      
        
        private boolean _isBusy=false;
        private boolean _isPeer=false;
        
        public boolean isBusyEnoughToTriggerQRTRemoval(){
            return _isBusy;
        }
        
        public void setBusyEnoughToTriggerQRTRemoval( boolean busy ){
            _isBusy = busy;
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
	public void testSomething() throws Exception {
        
        ConnectionManagerStubOvr cm=new ConnectionManagerStubOvr();
        cm.setIsAnyBusyLeafTriggeringQRTUpdate(false);
        
        ManagedConnectionStubOvr peer=new ManagedConnectionStubOvr(); 
        ManagedConnectionStubOvr leaf=new ManagedConnectionStubOvr();
        
        peer.setPeerConnection(true);
        leaf.setPeerConnection(false);
        
        cm.addStubOvrConnection(peer);
        cm.addStubOvrConnection(leaf);
                        
        
        
        assertTrue(true);
    }

}
