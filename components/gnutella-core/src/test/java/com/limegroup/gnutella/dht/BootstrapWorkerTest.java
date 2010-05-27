package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.LimeWireIOTestModule;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.util.ArrayUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;

public class BootstrapWorkerTest extends DHTTestCase {
    
    private Injector injector;

    public BootstrapWorkerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BootstrapWorkerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws IOException {
        
        injector = LimeTestUtils.createInjectorNonEagerly(
                new LimeWireIOTestModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(MojitoDHT.class).to(MojitoDHTStub.class);
            }
        });
    }
    
    public void testSimppHosts() throws InterruptedException {
        
        // Set up SIMPP hosts
        final String[] hosts = new String[] {
            "www.limewire.org:1234",
            "www.limewire.org:4321",
            "www.limewire.org:1243",
        };
        
        DHTSettings.DHT_BOOTSTRAP_HOSTS.set(hosts);
        
        final CountDownLatch latch 
            = new CountDownLatch(hosts.length);
        
        final MojitoDHTStub stub = new MojitoDHTStub() {
            @Override
            public DHTFuture<PingEntity> ping(SocketAddress dst, 
                    long timeout, TimeUnit unit) {
                
                if (contains(hosts, dst)) {
                    latch.countDown();
                }
                
                return super.ping(dst, timeout, unit);
            }
        };
        
        injector = LimeTestUtils.createInjectorNonEagerly(
                new LimeWireIOTestModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(BootstrapWorker.class);
                bind(MojitoDHT.class).toInstance(stub);
            }
        });
        
        BootstrapWorker worker 
            = injector.getInstance(BootstrapWorker.class);
        
        try {
            worker.start();
            
            if (!latch.await(20, TimeUnit.SECONDS)) {
                fail("Shouldn't have failed!");
            }
            
        } finally {
            worker.close();
        }
    }
    
    private static boolean contains(String[] addresses, 
            SocketAddress address) {
        
        InetSocketAddress isa = (InetSocketAddress)address;
        String element = isa.getHostName() + ":" + isa.getPort();
        
        return ArrayUtils.contains(addresses, element);
    }
}
