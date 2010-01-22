package com.limegroup.gnutella.filters;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;

import org.limewire.util.Visitor;
import org.limewire.core.settings.FilterSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.inject.AbstractModule;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.SpamServices;
import com.limegroup.gnutella.StubSpamServices;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.http.HTTPHeaderName;

import junit.framework.TestCase;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import com.google.inject.name.Names;
import com.google.inject.name.Named;
import com.google.inject.Singleton;
import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * functional test of the production blacklist server
 */
public class BlacklistServerTest extends LimeTestCase {
    private Injector injector;

    public BlacklistServerTest(String name){
        super(name);
    }

    protected void setUp(){}

    protected void tearDown(){
        FilterSettings.URN_BLACKLIST_UPDATE_URLS.revertToDefault();
        if(injector != null) {
            LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
            lifecycleManager.shutdown();
        }
    }

    // TODO LWC-5012
    public void xxtestAllSameTimestamps() throws DateParseException, IOException {
        // TODO programatically obtain these URLs in the test
        testAllSameTimestamps("http://10.0.0.58/list/1", "http://10.0.0.60/list/1");
        testAllSameTimestamps("http://10.0.0.58/list/2", "http://10.0.0.60/list/2");
    }

    public void testAllSameTimestamps(String... urls) throws IOException, DateParseException {
        Set<Date> times = new HashSet<Date>();
        for(String url : urls) {
            HttpHead request = new HttpHead(url);
            request.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close");
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(request);
            Date lastModified = DateUtils.parseDate(response.getFirstHeader("Last-Modified").getValue());
            times.add(lastModified);
        }
        assertEquals(1, times.size());
    }

    public void testIntegration10k() throws InterruptedException {
        testIntegration("http://static.list.limewire.com/list/1", 10000);
    }

    public void testIntegration30k() throws InterruptedException {
        testIntegration("http://static.list.limewire.com/list/2", 30000);
    }

    /**
     * Tests that the production blacklist server side is serving up valid
     * blacklists.
     *
     * @throws InterruptedException
     */
    public void testIntegration(String url, final int expectedMax) throws InterruptedException {
        // force an HTTP download
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.revertToDefault();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.revertToDefault();

        // this constant is used in the prod code to get the URL.
        // so even for getting the 10k list, which is on a different
        // url than the 30k list, we have to set this constant
        // to the appropirate url - 10k or 30k.
        FilterSettings.URN_BLACKLIST_UPDATE_URLS.set(
                new String[] {url});

        injector = LimeTestUtils.createInjectorAndStart(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Integer.class).annotatedWith(Names.named("testExpectedMax")).toInstance(expectedMax);
                bind(SpamServices.class).to(BlacklistValidatingSpamServicesStub.class);
            }
        });
        BlacklistValidatingSpamServicesStub spamServicesStub = (BlacklistValidatingSpamServicesStub)injector.getInstance(SpamServices.class);
        assertTrue(spamServicesStub.waitFor());
    }

    @Singleton
    static class BlacklistValidatingSpamServicesStub extends StubSpamServices {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final URNBlacklistManager manager;
        public int expectedMax;

        @Inject
        BlacklistValidatingSpamServicesStub(URNBlacklistManager manager,
                                            @Named("testExpectedMax") int expectedMax) {
            this.manager = manager;
            this.expectedMax = expectedMax;
        }

        @Override
        public void reloadSpamFilters() {
            final AtomicInteger count = new AtomicInteger();
            manager.loadURNs(new Visitor<String>() {
                @Override
                public boolean visit(String value) {
                    try {
                        URN.createSHA1Urn("urn:sha1:" + value);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    count.incrementAndGet();
                    return true;
                }
            });
            if(count.get() > 0 && count.get() <= expectedMax) {
                latch.countDown();
            }
        }

        boolean waitFor() throws InterruptedException {
            return latch.await(10, TimeUnit.SECONDS);
        }
    }
}
