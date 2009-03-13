package org.limewire.core.impl.connection;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.connection.ConnectionLifecycleEventType;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;

public class GnutellaConnectionManagerImplTest extends BaseTestCase {

    private Mockery context;
    private ConnectionManager connectionManager;

    public GnutellaConnectionManagerImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        connectionManager = context.mock(ConnectionManager.class);
    }

    public void testRegisterConnectionListener() {
        final GnutellaConnectionManagerImpl gnutellaConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, null, null);
        
        context.checking(new Expectations() {{
            exactly(1).of(connectionManager).addEventListener(gnutellaConnectionManager);
        }});
        
        gnutellaConnectionManager.registerListener();
        
        context.assertIsSatisfied();
    }
    
    /**
     * An integration test based around registering the service that goes all the way from
     *  the initial register to running the service once.
     */
    public void testRegisterService() {
        
        final GnutellaConnectionManagerImpl gnutellaConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, null, null);

        final ServiceRegistry registry = context.mock(ServiceRegistry.class);
        final ScheduledExecutorService backgroundExecutor = context.mock(ScheduledExecutorService.class);
        
        final MatchAndCopy<Service> serviceCollector = new MatchAndCopy<Service>(Service.class);
        final MatchAndCopy<ConnectionLifecycleListener> listenerCollector 
            = new MatchAndCopy<ConnectionLifecycleListener>(ConnectionLifecycleListener.class);
        final MatchAndCopy<Runnable> runnableCollector = new MatchAndCopy<Runnable>(Runnable.class);
        
        context.checking(new Expectations() {{
            exactly(1).of(registry).register(with(serviceCollector));
            exactly(1).of(connectionManager).addEventListener(with(listenerCollector));
            exactly(1).of(backgroundExecutor).scheduleWithFixedDelay(with(runnableCollector),
                    with(any(Long.class)), with(any(Long.class)), with(any(TimeUnit.class)));
            
            // Ensure that the calculation actually takes place by expecting a connection info
            //  query when the periodic service is run.
            exactly(1).of(connectionManager).countConnectionsWithNMessages(with(any(Integer.class)));
        }});
        
        // Kick off the register
        gnutellaConnectionManager.registerService(registry, backgroundExecutor);

        // Test stop without initialising
        serviceCollector.getLastMatch().stop();
        
        // Manually Initialise the service
        serviceCollector.getLastMatch().initialize();
        
        // Make sure the connection listener was properly linked
        listenerCollector.getLastMatch().handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, ConnectionLifecycleEventType.CONNECTED));
        assertEquals(ConnectionLifecycleEventType.CONNECTED, 
                gnutellaConnectionManager.lastStrengthRelatedEvent);
        
        // Test start
        serviceCollector.getLastMatch().start();
        
            
        // Test stop
        context.checking(new Expectations() {{
            exactly(1).of(connectionManager).removeEventListener(listenerCollector.getLastMatch());
            
            // We don't care about any other calls on connection manager that could happen while
            //  calculating the connection strength.
            ignoring(connectionManager);
        }});
        
        serviceCollector.getLastMatch().stop();
        
        // Test service once
        runnableCollector.getLastMatch().run();
        
        context.assertIsSatisfied();
    }
    
    public void testIsConnected() {
        final ConnectionServices connectionServices = context.mock(ConnectionServices.class);
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        
        GnutellaConnectionManagerImpl gConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, connectionServices, remoteLibraryManager);

        context.checking(new Expectations() {
            {
                one(connectionServices).isConnected();
                will(returnValue(true));
            }
        });
        assertTrue(gConnectionManager.isConnected());

        context.checking(new Expectations() {
            {
                one(connectionServices).isConnected();
                will(returnValue(false));
            }
        });
        assertFalse(gConnectionManager.isConnected());
        context.assertIsSatisfied();
    }

    public void testIsUltraPeer() {
        final ConnectionServices connectionServices = context.mock(ConnectionServices.class);
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
                
        GnutellaConnectionManagerImpl gConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, connectionServices, remoteLibraryManager);
        context.checking(new Expectations() {
            {
                one(connectionManager).isSupernode();
                will(returnValue(true));
            }
        });
        assertTrue(gConnectionManager.isUltrapeer());

        context.checking(new Expectations() {
            {
                one(connectionManager).isSupernode();
                will(returnValue(false));
            }
        });
        assertFalse(gConnectionManager.isUltrapeer());

        context.assertIsSatisfied();
    }
}
