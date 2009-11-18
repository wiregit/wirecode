package com.limegroup.gnutella.simpp;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.limegroup.gnutella.BlockingConnectionUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.ServerSideTestCase;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMStubHelper;
import com.limegroup.gnutella.messages.vendor.SimppRequestVM;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.settings.SimppSettingsManager;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;

public class SimppSenderIntegrationTest extends ServerSideTestCase {

    private SimppManagerStub simppManager;
    
    @Inject MessageRouter messageRouter;
    
    public SimppSenderIntegrationTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SimppSenderIntegrationTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        simppManager = new SimppManagerStub();
        Injector injector = LimeTestUtils.createInjector(TestUtils.bind(SimppManager.class).toInstances(simppManager));
        super.setUp(injector);
    }
    
    @Override
    public int getNumberOfUltrapeers() {
        return 2;
    }

    @Override
    public int getNumberOfLeafpeers() {
        return 2;
    }
    
    /**
     * Ensures that a second simpp request from the same connection is ignored
     * as long as the first request is being serviced. 
     */
    public void testDuplicateSimppRequestWhileServicingIsIgnored() throws Exception {
        drainAll();
        
        simppManager.version = 5;
        simppManager.data = new byte[32 * 1024];
        
        ULTRAPEER[0].send(new SimppRequestVM());
        ULTRAPEER[0].send(new SimppRequestVM());
        ULTRAPEER[0].flush();
        
        SimppVM simppVM = BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER[0], SimppVM.class);
        assertNotNull(simppVM);
        BlockingConnectionUtils.failIfAnyArrive(ULTRAPEER, SimppVM.class);
        
        // another request from the same connection should be handled now
        ULTRAPEER[0].send(new SimppRequestVM());
        ULTRAPEER[0].flush();
        
        simppVM = BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER[0], SimppVM.class);
        assertNotNull(simppVM);
        
        BlockingConnectionUtils.failIfAnyArrive(ULTRAPEER, SimppVM.class);
    }
    
    /**
     * Ensures a duplicate request from a connection that is already queued
     * is ignored. 
     */
    public void testDuplicateSimppRequestForQueuedConnectionIsIgnored() throws Exception {
        drainAll();
        
        simppManager.version = 5;
        simppManager.data = new byte[32 * 1024];
        
        ULTRAPEER[0].send(new SimppRequestVM());
        ULTRAPEER[0].flush();
        ULTRAPEER[1].send(new SimppRequestVM());
        // request from same connection is already queued when this one comes in
        ULTRAPEER[1].send(new SimppRequestVM());
        ULTRAPEER[1].flush();
        
        SimppVM simppVM = BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER[0], SimppVM.class);
        assertNotNull(simppVM);

        simppVM = BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER[1], SimppVM.class);
        assertNotNull(simppVM);
     
        // ensure no request is handled twice
        BlockingConnectionUtils.failIfAnyArrive(ULTRAPEER, SimppVM.class);
        
        // new request for second connection should be handled now
        // another request from the same connection should be handled now
        ULTRAPEER[1].send(new SimppRequestVM());
        ULTRAPEER[1].flush();
        
        simppVM = BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER[1], SimppVM.class);
        assertNotNull(simppVM);
        
        BlockingConnectionUtils.failIfAnyArrive(ULTRAPEER, SimppVM.class);
    }
    
    /**
     * Ensures a simpp requestor is removed from the queue if we receive a 
     * capabilties update that shows it already has the current simpp version. 
     */
    public void testRequestorIsRemovedFromQueueOnCapabilitiesUpdate() throws Exception {
        drainAll();
        
        simppManager.version = 5;
        simppManager.data = new byte[32 * 1024];
        
        ULTRAPEER[0].send(new SimppRequestVM());
        ULTRAPEER[0].flush();
        LEAF[0].send(new SimppRequestVM());
        LEAF[0].flush();
        ULTRAPEER[1].send(new SimppRequestVM());
        // send capabilties update which should remove it from the queue
        ULTRAPEER[1].send(CapabilitiesVMStubHelper.makeCapibilitesWithSimpp(5));
        ULTRAPEER[1].flush();
        
        SimppVM simppVM = BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER[0], SimppVM.class);
        assertNotNull(simppVM);
        simppVM = BlockingConnectionUtils.getFirstInstanceOfMessageType(LEAF[0], SimppVM.class);
        assertNotNull(simppVM);

        // ensure utrapeer 1 is not serviced
        BlockingConnectionUtils.failIfAnyArrive(ULTRAPEER, SimppVM.class);
        
        // a new request should ben handled again
        ULTRAPEER[1].send(new SimppRequestVM());
        ULTRAPEER[1].flush();
        
        simppVM = BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER[1], SimppVM.class);
        assertNotNull(simppVM);
        
        BlockingConnectionUtils.failIfAnyArrive(ULTRAPEER, SimppVM.class);
    }
    
    /**
     * Ensures that simpp updates are propagated to peers in a {@link CapabilitiesVM}. 
     */
    public void testCapabilitiesAreSentOnSimppVersionUpdate() throws Exception {
        drainAll();
        
        // trigger simpp version update
        simppManager.version = 5;
        for (SimppListener listener : simppManager.listeners) {
            listener.simppUpdated(5);
        }
        
        for (BlockingConnection connection : ULTRAPEER) {
            CapabilitiesVM capabilitiesVM = BlockingConnectionUtils.getFirstInstanceOfMessageType(connection, CapabilitiesVM.class);
            assertNotNull(capabilitiesVM);
            assertEquals(5, capabilitiesVM.supportsSIMPP());
        }
        for (BlockingConnection connection : LEAF) {
            CapabilitiesVM capabilitiesVM = BlockingConnectionUtils.getFirstInstanceOfMessageType(connection, CapabilitiesVM.class);
            assertNotNull(capabilitiesVM);
            assertEquals(5, capabilitiesVM.supportsSIMPP());
        }
        // no more capabilities should come in
        BlockingConnectionUtils.failIfAnyArrive(ULTRAPEER, CapabilitiesVM.class);
        BlockingConnectionUtils.failIfAnyArrive(LEAF, CapabilitiesVM.class);
    }
    
    /**
     * Ensures that a stale reply handler doesn't starve the simpp sender
     * and that the next requestor is being serviced after the timeout.
     */
    public void testTimeoutSchedulerTriggersSimppToBeSentToNextInQueue() throws Exception {
        drainAll();
        
        simppManager.version = 5;
        simppManager.data = new byte[32 * 1024];

        ConnectionSettings.SIMPP_SEND_TIMEOUT.setValue(2 * 1000);
        final CountDownLatch latch = new CountDownLatch(1);
        
        // stalling reply handler is a mocked one, so the message sent event
        // is not triggered for it
        ReplyHandler stallingReplyHandler = new ReplyHandlerStub() {
            @Override
            public void handleSimppVM(SimppVM svm) {
                latch.countDown();
            }  
        };
        
        messageRouter.handleMessage(new SimppRequestVM(), stallingReplyHandler);
        
        ULTRAPEER[0].send(new SimppRequestVM());
        ULTRAPEER[0].flush();
        
        // message has been sent to stalling reply handler
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        
        // after a short timeout the other reply handler should also receive
        // the simpp message
        SimppVM simppVM = BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER[0], SimppVM.class);
        assertNotNull(simppVM);
        BlockingConnectionUtils.failIfAnyArrive(ULTRAPEER, SimppVM.class);
    }
    
    private static class SimppManagerStub implements SimppManager {

        public volatile int version;
        public volatile byte[] data = new byte[] { 1 };
        
        public final List<SimppListener> listeners = new CopyOnWriteArrayList<SimppListener>(); 
        
        @Override
        public void addListener(SimppListener listener) {
            listeners.add(listener);
        }

        @Override
        public void addSimppSettingsManager(SimppSettingsManager simppSettingsManager) {
        }

        @Override
        public void checkAndUpdate(ReplyHandler handler, byte[] data) {
        }

        @Override
        public byte[] getOldUpdateResponse() {
            return null;
        }

        @Override
        public byte[] getSimppBytes() {
            return data;
        }

        @Override
        public List<SimppSettingsManager> getSimppSettingsManagers() {
            return null;
        }

        @Override
        public int getVersion() {
            return version;
        }

        @Override
        public void initialize() {
        }

        @Override
        public void removeListener(SimppListener listener) {
            listeners.remove(listener);
        }
        
    }
    
}
