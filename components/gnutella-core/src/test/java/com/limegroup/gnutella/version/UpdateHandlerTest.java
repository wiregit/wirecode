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
    
    private Mockery m;
    private ImmediateExecutor ses;
    private ActivityCallback ac;
    private HttpExecutor httpEx;
    private CapabilitiesVMFactory cvmf;
    private ConnectionManager cm;
    private FileManager fm;
    private ApplicationServices as;
    private UpdateCollectionFactory ucf;
    private ClockStub clock;
    private DownloadManager dm;
    
    private SettingsProvider sp;
    public void setUp() {
        
        m = new Mockery();
        ac = m.mock(ActivityCallback.class);
        httpEx = m.mock(HttpExecutor.class);
        cvmf = m.mock(CapabilitiesVMFactory.class);
        cm = m.mock(ConnectionManager.class);
        fm = m.mock(FileManager.class);
        as = m.mock(ApplicationServices.class);
        ucf = m.mock(UpdateCollectionFactory.class);
        dm = m.mock(DownloadManager.class);

        
        // these are used from multiple threads so cannot be mocked.
        ses = new ImmediateExecutor();
        clock = new ClockStub();
        sp = new SettingsProvider() {
            public long getChangePeriod() {
                return Long.MAX_VALUE;
            }
            public long getGracePeriod() {
                return Long.MAX_VALUE - 1;
            }
        };
        
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            public void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(ses);
                bind(ScheduledExecutorService.class).toInstance(ses); // also unnamed
                bind(ActivityCallback.class).toInstance(ac);
                bind(HttpExecutor.class).toInstance(httpEx);
                bind(CapabilitiesVMFactory.class).toInstance(cvmf);
                bind(ConnectionManager.class).toInstance(cm);
                bind(FileManager.class).toInstance(fm);
                bind(ApplicationServices.class).toInstance(as);
                bind(UpdateCollectionFactory.class).toInstance(ucf);
                bind(Clock.class).toInstance(clock);
                bind(DownloadManager.class).toInstance(dm);
                bind(SettingsProvider.class).toInstance(sp);
            }
        });
    }
    
    /**
     * tests that we parse strings
     */
    public void testParses() throws Exception {
        final UpdateCollection uc = m.mock(UpdateCollection.class);
        m.checking(new Expectations() {{
            one(ucf).createUpdateCollection("asdf\n");
            will(returnValue(uc));
            
            ignoring(ac);
            ignoring(httpEx);
            ignoring(cvmf);
            ignoring(cm);
            ignoring(fm);
            ignoring(as);
            ignoring(dm);
            ignoring(uc);
            
        }});
        UpdateHandler h = injector.getInstance(UpdateHandler.class);
        h.handleNewData(SIGNED_ASDF.getBytes(), null);
        m.assertIsSatisfied();
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
