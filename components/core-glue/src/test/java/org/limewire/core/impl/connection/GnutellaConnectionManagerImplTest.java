package org.limewire.core.impl.connection;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;

public class GnutellaConnectionManagerImplTest extends BaseTestCase {

    private Mockery context = null;

    private ConnectionManager connectionManager = null;

    private ConnectionServices connectionServices = null;

    private RemoteLibraryManager remoteLibraryManager = null;

    public GnutellaConnectionManagerImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        connectionManager = context.mock(ConnectionManager.class);
        connectionServices = context.mock(ConnectionServices.class);
        remoteLibraryManager = context.mock(RemoteLibraryManager.class);
    }

    public void testIsConnected() {

        context.checking(new Expectations() {
            {
                one(connectionManager).addEventListener(
                        with(any(GnutellaConnectionManagerImpl.class)));
            }
        });

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
        context.checking(new Expectations() {
            {
                one(connectionManager).addEventListener(
                        with(any(GnutellaConnectionManagerImpl.class)));
            }
        });
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
