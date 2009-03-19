package org.limewire.core.impl.connection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.core.api.connection.ConnectionLifecycleEventType;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;

import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.connection.RoutedConnection;

/**
 * Various tests for GnutellaConnectionManagerImpl.  Tests event listeners,
 *  delegate functions, and ConnectionStrength calculation. 
 */
public class GnutellaConnectionManagerImplTest extends BaseTestCase {

    // TODO: Test connection strength calculation
    
    private Mockery context;
    private ConnectionManager connectionManager;

    public GnutellaConnectionManagerImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        connectionManager = context.mock(ConnectionManager.class);
    }
    
    /**
     * Confirms calling registerListener() on an instance of gnutellaConnectionManager
     *  will register the instance as an event listener on ConnectionManager
     */
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
        
        final PropertyChangeListener changeListener = context.mock(PropertyChangeListener.class); 
        
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
        
        // Should start disconnected
        assertEquals(ConnectionStrength.DISCONNECTED, gnutellaConnectionManager.getConnectionStrength());
        
        // Kick off the register
        gnutellaConnectionManager.registerService(registry, backgroundExecutor);

        // Test stop without initialising
        serviceCollector.getLastMatch().stop();
        
        // Manually Initialise the service
        serviceCollector.getLastMatch().initialize();
        
        // Make sure the service has a valid name
        assertNotNull(serviceCollector.getLastMatch().getServiceName());
        assertNotEquals("", serviceCollector.getLastMatch().getServiceName());
        
        // Make sure the connection listener was properly linked
        listenerCollector.getLastMatch().handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, ConnectionLifecycleEventType.CONNECTED));
        assertEquals(ConnectionLifecycleEventType.CONNECTED, 
                gnutellaConnectionManager.lastStrengthRelatedEvent);
        
        listenerCollector.getLastMatch().handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, ConnectionLifecycleEventType.NO_INTERNET));
        assertEquals(ConnectionLifecycleEventType.NO_INTERNET, 
                gnutellaConnectionManager.lastStrengthRelatedEvent);
        
        listenerCollector.getLastMatch().handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, ConnectionLifecycleEventType.CONNECTION_INITIALIZED));
        assertEquals(ConnectionLifecycleEventType.CONNECTION_INITIALIZED, 
                gnutellaConnectionManager.lastStrengthRelatedEvent);
        
        listenerCollector.getLastMatch().handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, ConnectionLifecycleEventType.DISCONNECTED));
        assertEquals(ConnectionLifecycleEventType.CONNECTION_INITIALIZED, 
                gnutellaConnectionManager.lastStrengthRelatedEvent);
        
        // Test start
        serviceCollector.getLastMatch().start();

        // External listener and stop assertions        
        context.checking(new Expectations() {{
            exactly(1).of(connectionManager).removeEventListener(listenerCollector.getLastMatch());
            
            // When service is run the new state should alternate to test listeners
            one(connectionManager).isConnecting();
            will(returnValue(true));
            one(connectionManager).isConnecting();
            will(returnValue(true));
            one(connectionManager).isConnecting();
            will(returnValue(false));
            // Should only be fired the once, the rest are either caches or when the listener is removed
            exactly(1).of(changeListener).propertyChange(with(any(PropertyChangeEvent.class)));
            
            // We don't care about any other calls on connection manager that could happen while
            //  calculating the connection strength.
            ignoring(connectionManager);
        }});
        
        // Add a listener to ensure changes are sent out
        gnutellaConnectionManager.addPropertyChangeListener(changeListener);
        // Test the service - change event
        runnableCollector.getLastMatch().run();
        // Test the service - no change event because caching
        runnableCollector.getLastMatch().run();
        // Remove the listener
        gnutellaConnectionManager.removePropertyChangeListener(changeListener);
        // Test the service - no change event because no listeners conencted
        runnableCollector.getLastMatch().run();
        
        // Test stop
        serviceCollector.getLastMatch().stop();
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test that delegate to ConnectionManager.isConnected() is correctly connected and returns
     *  accordingly in true and false state.
     */
    public void testIsConnected() {
        final ConnectionServices connectionServices = context.mock(ConnectionServices.class);
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        
        GnutellaConnectionManagerImpl gnutellaConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, connectionServices, remoteLibraryManager);

        context.checking(new Expectations() {{
                one(connectionServices).isConnected();
                will(returnValue(true));
        }});
        assertTrue(gnutellaConnectionManager.isConnected());

        context.checking(new Expectations() {{
                one(connectionServices).isConnected();
                will(returnValue(false));
        }});
        assertFalse(gnutellaConnectionManager.isConnected());
        
        context.assertIsSatisfied();
    }

    /**
     * Test that delegate to ConnectionManager.isSupernode() is correctly connected and returns
     *  accordingly in true and false state.
     */
    public void testIsUltraPeer() {
        final ConnectionServices connectionServices = context.mock(ConnectionServices.class);
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
                
        GnutellaConnectionManagerImpl gnutellaConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, connectionServices, remoteLibraryManager);
        context.checking(new Expectations() {{
                one(connectionManager).isSupernode();
                will(returnValue(true));
        }});
        assertTrue(gnutellaConnectionManager.isUltrapeer());

        context.checking(new Expectations() {{
                one(connectionManager).isSupernode();
                will(returnValue(false));
        }});
        assertFalse(gnutellaConnectionManager.isUltrapeer());

        context.assertIsSatisfied();
    }
    
    /**
     * Ensures that the various delegate functions to ConnectionServices, LibraryManager,
     *  and core ConnectionManager are correctly linked and pass execution on properly.
     */
    public void testConnectionDelegates() {
        final ConnectionServices connectionServices = context.mock(ConnectionServices.class);
        final RemoteLibraryManager libraryManager = context.mock(RemoteLibraryManager.class);

        final int portnum = 111111;
        final String hostname = "hostekepucally";
        final ConnectionItem connectionItemToNotRemove = context.mock(ConnectionItem.class);
        final ConnectionItem connectionItemToRemove = context.mock(CoreConnectionItem.class);
        final ConnectionItem connectionItemToBrowse = context.mock(ConnectionItem.class);
        
        GnutellaConnectionManagerImpl gConnectionManager = new GnutellaConnectionManagerImpl(
                connectionManager, connectionServices, libraryManager);
        
        context.checking(new Expectations() {{
            allowing(connectionItemToRemove);
            
            exactly(1).of(connectionServices).connect();
            exactly(1).of(connectionServices).disconnect();
            exactly(1).of(connectionManager).disconnect(true);
            exactly(1).of(connectionManager).connect();
            exactly(1).of(connectionServices).removeConnection(with(any(RoutedConnection.class)));
            exactly(1).of(connectionServices).connectToHostAsynchronously(hostname, portnum, ConnectType.TLS);
            exactly(1).of(connectionServices).connectToHostAsynchronously(hostname, portnum, ConnectType.PLAIN);
            
            FriendPresence presence = context.mock(FriendPresence.class);
            allowing(connectionItemToBrowse).getFriendPresence();
            will(returnValue(presence));
            exactly(1).of(libraryManager).addPresenceLibrary(presence);
        }});
        
        gConnectionManager.connect();
        gConnectionManager.disconnect();
        gConnectionManager.restart();
        
        gConnectionManager.removeConnection(connectionItemToNotRemove);
        gConnectionManager.removeConnection(connectionItemToRemove);
        gConnectionManager.browseHost(connectionItemToBrowse);
        
        gConnectionManager.tryConnection(hostname, portnum, true);
        gConnectionManager.tryConnection(hostname, portnum, false);
        
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests the ConnectionLifecycleEvent code under various conditions.  Ensure
     *  ConnectionItems are created, added, updated, and removed according according to
     *  the differnt event types.
     */
    public void testHandleConnectionLifecycleEvent() {
        
        GnutellaConnectionManagerImpl gnutellaConnectionManager
            = new GnutellaConnectionManagerImpl(connectionManager, null, null);
        
        EventList<ConnectionItem> list = gnutellaConnectionManager.getConnectionList();
        
        final RoutedConnection routedConnection = context.mock(RoutedConnection.class);
        
        context.checking(new Expectations() {{
            // Key to ensure the RoutedConnection and ConnectionItem match
            allowing(routedConnection).getPort();
            will(returnValue(444));
            
            // Marker to identify with the ConnectionItem is updated
            allowing(routedConnection).getConnectionTime();
            will(returnValue(555L));
            
            allowing(routedConnection);
        }});
        
        assertEmpty(list);
        
        // An event with no routed connection -- should be ignored
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_INITIALIZING));        
        assertEmpty(list);
                
        // Spurious initialised event -- should be ignored
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_INITIALIZED,
                        routedConnection));
        assertEmpty(list);
        
        // Non relevant event -- should be ignored
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_CAPABILITIES,
                        routedConnection));
        assertEmpty(list);
        
        // Initialising -- should result in a new list item
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_INITIALIZING,
                        routedConnection));
        assertEquals(1, list.size());
        assertEquals(444, list.get(0).getPort());

        // Duplicate initialising -- should be ignored
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_INITIALIZING,
                        routedConnection));
        assertEquals(1, list.size());
        assertEquals(444, list.get(0).getPort());
        
        // Make sure the update marker is not set before the update is made
        assertNotEquals(555, list.get(0).getTime());
        
        // Initialised -- Should call update, check that time marker is updated
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_INITIALIZED,
                        routedConnection));
        assertEquals(1, list.size());
        assertEquals(444, list.get(0).getPort());
        // This marker will be propagated into the ConnectionItem when update is called
        assertEquals(555, list.get(0).getTime());
        
        // Closed -- Should remove the item from the list
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_CLOSED,
                        routedConnection));
        assertEmpty(list);

        // Duplicate closed -- Should have no effect
        gnutellaConnectionManager.handleConnectionLifecycleEvent(
                new ConnectionLifecycleEvent(this, 
                        ConnectionLifecycleEventType.CONNECTION_CLOSED,
                        routedConnection));
        assertEmpty(list);
        
        context.assertIsSatisfied();
    }
}
