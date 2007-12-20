package com.limegroup.gnutella.version;

import java.util.concurrent.ScheduledExecutorService;

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
    
    private Injector injector;
    
    private Mockery m;
    private ScheduledExecutorService ses;
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
        ses = new ScheduledExecutorServiceStub();
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
     * tests that we set up bindings correctly
     */
    public void testBindings() throws Exception {
        m.checking(new Expectations() {{
            ignoring(ac);
            ignoring(httpEx);
            ignoring(cvmf);
            ignoring(cm);
            ignoring(fm);
            ignoring(as);
            ignoring(ucf);
            ignoring(dm);
            
        }});
        injector.getInstance(UpdateHandler.class);
    }
}
