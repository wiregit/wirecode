package com.limegroup.gnutella.dht.db;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.db.DHTValue;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTControllerStub;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.NullDHTController;
import com.limegroup.gnutella.dht.DHTEvent.Type;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.SSLSettings;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class PushProxiesPublisherTest extends LimeTestCase {

    private NetworkManagerStub networkManagerStub;
    private PushProxiesPublisher pushProxiesPublisher;
    private Mockery context;

    private Runnable publishingRunnable;
    
    public PushProxiesPublisherTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PushProxiesPublisherTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
            }
        });
        pushProxiesPublisher = injector.getInstance(PushProxiesPublisher.class);
    }
    
    /**
     * An integration test that makes sure we create a storable for a 
     * non-firewalled peer.
     */
    public void testValueToPublishForNonFirewalledPeer() {
        networkManagerStub.setAcceptedIncomingConnection(true);
        
        PushProxiesValue value = pushProxiesPublisher.getValueToPublish();
        assertNull("First value should be null since not stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        Connectable expected = new ConnectableImpl(new IpPortImpl(networkManagerStub.getAddress(), networkManagerStub.getPort()), SSLSettings.isOutgoingTLSEnabled());
        assertEquals(0, IpPort.IP_COMPARATOR.compare(expected, value.getPushProxies().iterator().next()));
    }
    
    /**
     * When the set of push proxies or the fwt capability of this peer change it 
     * should publish its updated info into the DHT the next time the StorableModel
     * is asked for them.
     */
    public void testValueToPublishWhenProxiesValueHasChanged() throws Exception {
        
        networkManagerStub.setAcceptedIncomingConnection(true);
        networkManagerStub.setSupportsFWTVersion(1);

        PushProxiesValue value = pushProxiesPublisher.getValueToPublish();
        assertNull("First value should be null, since not considered stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        
        Connectable expected = new ConnectableImpl(new IpPortImpl(networkManagerStub.getAddress(), networkManagerStub.getPort()), SSLSettings.isOutgoingTLSEnabled());
        assertEquals(0, IpPort.IP_COMPARATOR.compare(expected, value.getPushProxies().iterator().next()));
        assertEquals(1, value.getFwtVersion());
        
        // change fwt support status so we should get a different pushproxy value 
        // for ourselves that needs to be republished
        networkManagerStub.setSupportsFWTVersion(2);
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("first value after change should be null, since not stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertEquals(2, value.getFwtVersion());
    }
    
    public void testValueChangingInBetweenConsecutiveCallsToGetValueToPublish() {
        networkManagerStub.setAcceptedIncomingConnection(true);
        
        PushProxiesValue value = pushProxiesPublisher.getValueToPublish();
        assertNull("value should be null, since not stable as first value", value);
        
        networkManagerStub.setPort(54545);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("value should be null, since push proxies should have changed and not stable", value);
        
        networkManagerStub.setCanDoFWT(true);
        networkManagerStub.setSupportsFWTVersion(2);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNull("value should be null, since push proxies should have changed and not stable", value);
        
        value = pushProxiesPublisher.getValueToPublish();
        assertNotNull("value should not be null, since stable now", value);
    }
    
    public void testDHTEventHandlerIsInstalledAndTriggersRunnable() {
        networkManagerStub.setAcceptedIncomingConnection(true);
        networkManagerStub.setSupportsFWTVersion(1);
        
        final MojitoDHT mojitoDHT = context.mock(MojitoDHT.class);
        final ScheduledExecutorService executorService = context.mock(ScheduledExecutorService.class);
        final ScheduledFuture future = context.mock(ScheduledFuture.class);
        
        
        context.checking(new Expectations() {{
            one(executorService).scheduleAtFixedRate(with(any(Runnable.class)), 
                    with(any(Long.class)),
                    with(equal(DHTSettings.PUSH_PROXY_STABLE_PUBLISHING_INTERVAL.getValue())),
                    with(equal(TimeUnit.MILLISECONDS)));
            will(new CustomAction("save runnable and return future action") {
                public Object invoke(Invocation invocation) throws Throwable {
                    publishingRunnable = (Runnable)invocation.getParameter(0);
                    return future;
                }
            });
        }});
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
                // not binding mocked executor, since it is used in all kinds of places
            } 
        });
        
        DHTSettings.PUBLISH_PUSH_PROXIES.setValue(true);
        
        pushProxiesPublisher = new PushProxiesPublisher(injector.getInstance(PushProxiesValueFactory.class), executorService);

        pushProxiesPublisher.handleDHTEvent(new DHTEvent(new DHTControllerStub(mojitoDHT, DHTMode.PASSIVE), Type.CONNECTED));
        
        assertNotNull(publishingRunnable);

        context.assertIsSatisfied();
        
        // test publishing 
        context.checking(new Expectations() {{
            one(mojitoDHT).put(with(any(KUID.class)), with(any(DHTValue.class)));
        }});

        publishingRunnable.run();

        // publishing only takes place the second time around
        publishingRunnable.run();
        
        context.assertIsSatisfied();
        
        context.checking(new Expectations() {{
            one(future).cancel(false);
        }});
        
        pushProxiesPublisher.handleDHTEvent(new DHTEvent(new NullDHTController(), Type.STOPPED));
        
        context.assertIsSatisfied();
    }
    
}
