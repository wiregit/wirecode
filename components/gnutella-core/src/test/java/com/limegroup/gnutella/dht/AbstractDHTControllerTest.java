package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.mojito2.AbstractAddressPinger;
import org.limewire.mojito2.AddressPinger;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.DHTValueFuture;
import org.limewire.mojito2.entity.PingEntity;

import com.limegroup.gnutella.dht2.ContactPinger;

public class AbstractDHTControllerTest extends DHTTestCase {

    public AbstractDHTControllerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AbstractDHTControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testPing() throws InterruptedException {
        
        final int count = 20;
        final CountDownLatch latch = new CountDownLatch(count);
        
        AddressPinger foo = new AbstractAddressPinger() {
            @Override
            public DHTFuture<PingEntity> ping(SocketAddress address, 
                    long timeout, TimeUnit unit) {
                latch.countDown();
                return new DHTValueFuture<PingEntity>(
                        new UnsupportedOperationException());
            }
        };
        
        ContactPinger pinger = new ContactPinger(foo, 1, TimeUnit.MILLISECONDS);
        try {
            for (int i = 0; i < count; i++) {
                pinger.addActiveNode(new InetSocketAddress("localhost", 2000+i));
            }
            
            if (!latch.await(1L, TimeUnit.SECONDS)) {
                fail("Shouldn't have failed!");
            }
            
        } finally {
            pinger.close();
        }
    }
}
