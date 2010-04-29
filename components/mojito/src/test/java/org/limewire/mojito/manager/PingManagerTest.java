package org.limewire.mojito.manager;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestSuite;

import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.settings.PingSettings;
import org.limewire.mojito2.DHT;
import org.limewire.mojito2.MojitoFactory2;
import org.limewire.mojito2.io.DatagramTransport;
import org.limewire.mojito2.message.DefaultMessageFactory;
import org.limewire.mojito2.message.MessageFactory;

public class PingManagerTest extends MojitoTestCase {
    
    public PingManagerTest(String name){
        super(name);
    }
    
    public static TestSuite suite() {
        return buildTestSuite(PingManagerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
    }

    public void testPing() throws IOException, InterruptedException {
        
        PingSettings.PARALLEL_PINGS.setValue(3);
        NetworkSettings.MAX_ERRORS.setValue(0);
        
        DatagramTransport transport1 = null, transport2 = null;
        DHT dht1 = null, dht2 = null;
        
        try {
            
            MessageFactory factory1 = new DefaultMessageFactory();
            transport1 = new DatagramTransport(2000, factory1);
            
            MessageFactory factory2 = new DefaultMessageFactory();
            transport2 = new DatagramTransport(3000, factory2);
            
            dht1 = MojitoFactory2.createDHT(transport1, factory1);
            dht1.start();
            
            dht2 = MojitoFactory2.createDHT(transport2, factory2);
            dht2.start();
            
            try {
                DHTFuture<PingEntity> future = dht2.ping(
                        "www.google.com", 80, 
                        1L, TimeUnit.SECONDS);
                future.get();
                fail("Ping should have failed");
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof TimeoutException);
            }
            
            try {
                DHTFuture<PingEntity> future = dht2.ping(
                        "localhost", 2000, 
                        1L, TimeUnit.SECONDS);
                PingEntity entity = future.get();
                assertEquals(dht1.getLocalNode().getNodeID(), 
                        entity.getContact().getNodeID());
            } catch (ExecutionException e) {
                fail(e);
            }
            
        } finally {
            transport1.close();
            transport2.close();
            
            dht1.close();
            dht2.close();
        }
    }
}
