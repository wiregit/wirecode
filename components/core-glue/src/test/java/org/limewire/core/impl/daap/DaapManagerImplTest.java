package org.limewire.core.impl.daap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.hamcrest.core.IsAnything;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.DaapSettings;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.util.BaseTestCase;

import com.google.inject.Provider;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.daap.DaapManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.library.FileManager;

public class DaapManagerImplTest extends BaseTestCase {

    public DaapManagerImplTest(String name) {
        super(name);
    }

    /**
     * This is a basic test for the daap server. It starts the server, checks to
     * make sure that the server can be connected to, then the server is
     * stopped and checked to make sure that no more connections can be made.
     */
    @SuppressWarnings("unchecked")
    public void testStartingDaapServerConnectingToItAndStopping() throws Exception {
        Mockery context = new Mockery();
        final FileManager fileManager = context.mock(FileManager.class);
        final Provider<FileManager> fileManagerProvider = context.mock(Provider.class);
        final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
        final IPFilter ipFilter = context.mock(IPFilter.class);
        final Provider<IPFilter> ipFilterProvider = context.mock(Provider.class);
        final NetworkInstanceUtils networkInstanceUtils = context.mock(NetworkInstanceUtils.class);
        final Provider<NetworkInstanceUtils> networkInstanceUtilsProvider = context
                .mock(Provider.class);
        final ActivityCallback activityCallback = context.mock(ActivityCallback.class);
        final Provider<ActivityCallback> activityCallbackProvider = context.mock(Provider.class);
        DaapManager daapManager = new DaapManager(executorService, fileManagerProvider,
                ipFilterProvider, networkInstanceUtilsProvider, activityCallbackProvider);
        DaapManagerImpl daapServer = new DaapManagerImpl(daapManager);

        context.checking(new Expectations() {
            {
                allowing(fileManagerProvider).get();
                will(returnValue(fileManager));
                allowing(ipFilterProvider).get();
                will(returnValue(ipFilter));
                allowing(networkInstanceUtilsProvider).get();
                will(returnValue(networkInstanceUtils));
                allowing(activityCallbackProvider).get();
                will(returnValue(activityCallback));
                one(activityCallback).translate(with(new IsAnything<String>()));
                will(returnValue("What's New"));
                one(activityCallback).translate(with(new IsAnything<String>()));
                will(returnValue("Creative Commons"));
                one(activityCallback).translate(with(new IsAnything<String>()));
                will(returnValue("Video"));
            }
        });

        Socket socket = null;
        BufferedWriter writer = null;
        try {
            daapServer.start();

            assertTrue(daapServer.isServerRunning());

            int port = DaapSettings.DAAP_PORT.getValue();

            InetAddress address = NetworkUtils.getLocalAddress();

            socket = new Socket();
            socket.connect(new InetSocketAddress(address, port));
            assertTrue(socket.isConnected());

            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write("test");
            writer.flush();
            writer.close();
            socket.close();

            daapServer.stop();

            socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(address, port));
                fail("Should not be able to connect, server should be stopped.");
            } catch (ConnectException e) {
                // expected
            }
            assertFalse(daapServer.isServerRunning());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
            
            daapServer.stop();
        }

        context.assertIsSatisfied();
    }

}
