package org.limewire.mojito.manager;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestSuite;

import org.limewire.io.IOUtils;
import org.limewire.mojito.DHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.settings.PingSettings;

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
        
        DHT dht1 = null;
        DHT dht2 = null;
        
        try {
            dht1 = MojitoFactory.createDHT("DHT1", 2000);
            dht2 = MojitoFactory.createDHT("DHT2", 3000);
            
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
                assertEquals(dht1.getLocalhost().getContactId(), 
                        entity.getContact().getContactId());
            } catch (ExecutionException e) {
                fail(e);
            }
            
        } finally {
            IOUtils.close(dht1, dht2);
        }
    }
}
