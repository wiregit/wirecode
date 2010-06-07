package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;

import com.google.inject.Injector;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.dht.DHTEvent.Type;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;

public class DHTManagerTest extends DHTTestCase {
    
    private Injector injector;

    public DHTManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DHTManagerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        
        injector = LimeTestUtils.createInjectorAndStart();
        
        // Stop the NodeAssigner which tries to change the 
        // DHTMode during our testing.
        NodeAssigner assigner = injector.getInstance(NodeAssigner.class);
        assigner.stop();
    }
    
    public void testInactive() throws IOException {
        startMode(DHTMode.INACTIVE);
    }
    
    public void testActive() throws IOException {
        startMode(DHTMode.ACTIVE);
    }
    
    public void testPassive() throws IOException {
        startMode(DHTMode.PASSIVE);
    }
    
    public void testPassiveLeaf() throws IOException {
        startMode(DHTMode.PASSIVE_LEAF);
    }
    
    private void startMode(DHTMode mode) throws IOException {
        DHTManager manager = injector.getInstance(DHTManager.class);
        try {
            assertFalse(manager.isRunning());
            assertFalse(manager.isReady());
            
            boolean success = manager.start(mode);
            assertTrue(success);
            
            assertEquals(mode, manager.getMode());
            assertTrue(manager.isMode(mode));
            
            if (mode != DHTMode.INACTIVE) {
                assertTrue(manager.isRunning());
            }
            
            manager.stop();
            
            assertFalse(manager.isRunning());
            assertFalse(manager.isReady());
            assertEquals(DHTMode.INACTIVE, manager.getMode());
            assertTrue(manager.isMode(DHTMode.INACTIVE));
            
        } finally {
            manager.close();
        }
    }
    
    public void testSwitchMode() throws IOException {
        DHTManager manager = injector.getInstance(DHTManager.class);
        try {
            
            boolean success = false;
            
            success = manager.start(DHTMode.INACTIVE);
            assertTrue(success);
            assertTrue(manager.isMode(DHTMode.INACTIVE));
            
            success = manager.start(DHTMode.ACTIVE);
            assertTrue(success);
            assertTrue(manager.isMode(DHTMode.ACTIVE));
            
            success = manager.start(DHTMode.PASSIVE);
            assertTrue(success);
            assertTrue(manager.isMode(DHTMode.PASSIVE));
            
            success = manager.start(DHTMode.PASSIVE_LEAF);
            assertTrue(success);
            assertTrue(manager.isMode(DHTMode.PASSIVE_LEAF));
            
        } finally {
            manager.close();
        }
    }
    
    public void testEvents() throws IOException, 
            InterruptedException, ExecutionException {
        
        DHTManager manager = injector.getInstance(DHTManager.class);
        
        try {
            
            final CountDownLatch starting = new CountDownLatch(1);
            manager.addEventListener(new DHTEventListener() {
                @Override
                public void handleDHTEvent(DHTEvent evt) {
                    if (evt.getType() == Type.STARTING) {
                        starting.countDown();
                    }
                }
            });
            
            manager.start(DHTMode.ACTIVE);
            if (!starting.await(1, TimeUnit.SECONDS)) {
                fail("Shouldn't have failed!");
            }
            
            MojitoDHT dht = MojitoFactory.createDHT("DHT", 5000);
            
            try {
                final CountDownLatch connected = new CountDownLatch(1);
                manager.addEventListener(new DHTEventListener() {
                    @Override
                    public void handleDHTEvent(DHTEvent evt) {
                        if (evt.getType() == Type.CONNECTED) {
                            connected.countDown();
                        }
                    }
                });
                
                manager.addActiveNode(new InetSocketAddress("localhost", 5000));
                
                if (!connected.await(10, TimeUnit.SECONDS)) {
                    fail("Shouldn't have failed!");
                }
                
            } finally {
                dht.close();
            }
            
            final CountDownLatch stopped = new CountDownLatch(1);
            manager.addEventListener(new DHTEventListener() {
                @Override
                public void handleDHTEvent(DHTEvent evt) {
                    if (evt.getType() == Type.STOPPED) {
                        stopped.countDown();
                    }
                }
            });
            
            manager.stop();
            
            if (!stopped.await(1, TimeUnit.SECONDS)) {
                fail("Shouldn't have failed!");
            }
            
        } finally {
            manager.close();
        }
    }
}
