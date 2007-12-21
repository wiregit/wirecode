package com.limegroup.gnutella.version;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.security.SettingsProvider;
import org.limewire.util.Clock;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ClockStub;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.stubs.ScheduledExecutorServiceStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class UpdateHandlerTest extends LimeTestCase {

    public UpdateHandlerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UpdateHandlerTest.class);
    }

    /**
     * used to bypass the signer - customize the UpdateCollectionFactory to actually test.
     */
    private final String SIGNED_ASDF = "GAWAEFDW7Q73ILI2N5FSNPS7ASVYZ646BFYLZLQCCQ6G3VLJD4EE7KNGHVUDPLCALWTH2R4BLQ||asdf\n";
    
    private Injector injector;

    private Mockery mockery;

    private ScheduledExecutorService backgroundExecutor;

    private ActivityCallback activityCallback;

    private HttpExecutor httpExexecutor;

    private CapabilitiesVMFactory capabilitiesVmFactory;

    private ConnectionManager connectionManager;

    private FileManager fileManager;

    private ApplicationServices applicationServices;

    private UpdateCollectionFactory updateCollectionFactory;

    private ClockStub clock;

    private DownloadManager downloadManager;

    private SettingsProvider settingsProvider;
    
    public void setUp() {
        createInjector(null);
    }

    private void createInjector(final String failoverUrl) {
        mockery = new Mockery();
        activityCallback = mockery.mock(ActivityCallback.class);
        httpExexecutor = mockery.mock(HttpExecutor.class);
        capabilitiesVmFactory = mockery.mock(CapabilitiesVMFactory.class);
        connectionManager = mockery.mock(ConnectionManager.class);
        fileManager = mockery.mock(FileManager.class);
        applicationServices = mockery.mock(ApplicationServices.class);
        updateCollectionFactory = mockery.mock(UpdateCollectionFactory.class);
        downloadManager = mockery.mock(DownloadManager.class);        
        backgroundExecutor = new ImmediateExecutor();
        clock = new ClockStub();
        settingsProvider = new SettingsProvider() {
            public long getChangePeriod() {
                return Long.MAX_VALUE;
            }
            public long getGracePeriod() {
                return Long.MAX_VALUE - 1;
            }
        };
        
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            public void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(backgroundExecutor);
                bind(ScheduledExecutorService.class).toInstance(backgroundExecutor); // also unnamed
                bind(ActivityCallback.class).toInstance(activityCallback);
                bind(HttpExecutor.class).toInstance(httpExexecutor);
                bind(CapabilitiesVMFactory.class).toInstance(capabilitiesVmFactory);
                bind(ConnectionManager.class).toInstance(connectionManager);
                bind(FileManager.class).toInstance(fileManager);
                bind(ApplicationServices.class).toInstance(applicationServices);
                bind(UpdateCollectionFactory.class).toInstance(updateCollectionFactory);
                bind(Clock.class).toInstance(clock);
                bind(DownloadManager.class).toInstance(downloadManager);
                bind(SettingsProvider.class).toInstance(settingsProvider);
                if(failoverUrl != null)
                    bindConstant().annotatedWith(Names.named("failoverUpdateLocation")).to("http://127.0.0.1:9999/update.def");
            }
        });
    }
    
    /** tests that we set up bindings correctly */
    public void testBindings() throws Exception {
        mockery.checking(new Expectations() {{
            ignoring(activityCallback);
            ignoring(httpExexecutor);
            ignoring(capabilitiesVmFactory);
            ignoring(connectionManager);
            ignoring(fileManager);
            ignoring(applicationServices);
            ignoring(updateCollectionFactory);
            ignoring(downloadManager);            
        }});
        UpdateHandlerImpl updateHandler = (UpdateHandlerImpl)injector.getInstance(UpdateHandler.class);
        assertEquals("http://update.limewire.com/version.def", updateHandler.getFailoverLocation());
    }
    
    /** tests that we set up bindings correctly */
    public void testUpdateFailoverConstant() throws Exception {
        createInjector("http://127.0.0.1:9999/update.def");
        mockery.checking(new Expectations() {{
            ignoring(activityCallback);
            ignoring(httpExexecutor);
            ignoring(capabilitiesVmFactory);
            ignoring(connectionManager);
            ignoring(fileManager);
            ignoring(applicationServices);
            ignoring(updateCollectionFactory);
            ignoring(downloadManager);         
        }});

        UpdateHandlerImpl updateHandler = (UpdateHandlerImpl)injector.getInstance(UpdateHandler.class);
        assertEquals("http://127.0.0.1:9999/update.def", updateHandler.getFailoverLocation());
    }
            
    /**
     * tests that we parse strings
     */
    public void testParses() throws Exception {
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        mockery.checking(new Expectations() {{
            one(updateCollectionFactory).createUpdateCollection("asdf\n");
            will(returnValue(updateCollection));
            ignoring(activityCallback);
            ignoring(httpExexecutor);
            ignoring(capabilitiesVmFactory);
            ignoring(connectionManager);
            ignoring(fileManager);
            ignoring(applicationServices);
            ignoring(updateCollectionFactory);
            ignoring(downloadManager);      
            ignoring(updateCollection);
        }});

        UpdateHandler h = injector.getInstance(UpdateHandler.class);
        h.handleNewData(SIGNED_ASDF.getBytes(), null);
        mockery.assertIsSatisfied();
    }
    
    private class ImmediateExecutor extends ScheduledExecutorServiceStub {

        volatile Runnable scheduled;
        
        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            scheduled = command;
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                long period, TimeUnit unit) {
            scheduled = command;
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                long delay, TimeUnit unit) {
            scheduled = command;
            return null;
        }
        
    }
}
