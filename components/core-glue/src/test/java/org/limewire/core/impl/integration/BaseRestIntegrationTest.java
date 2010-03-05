package org.limewire.core.impl.integration;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.limewire.core.impl.tests.CoreGlueTestUtils;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.rest.RestAuthority;
import org.limewire.rest.RestAuthorityFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryProvider;
import com.limegroup.gnutella.LifecycleManager;

public class BaseRestIntegrationTest extends LimeTestCase {

    private static String LOCAL_REST_URL = "http://localhost:45100";
    
    @Inject private Injector injector;
    @Inject private LimeHttpClient client;

    public BaseRestIntegrationTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.setValue(true);
        CoreGlueTestUtils.createInjectorAndStart(new MockRestModule(), LimeTestUtils.createModule(this));
    }
  
    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
    }
    
    // basic test
    public void testHelloWorld() throws Exception {       
        String response = getAPIResponse("/remote/hello");
        assertTrue( response.endsWith("Hello world!") );       
    }
      
    // return GET HttpRequest response
    private String getAPIResponse(String restURL) throws IOException {
        String responseStr = "";
        HttpResponse response = null;
        try {
            HttpGet method = new HttpGet(LOCAL_REST_URL + restURL);           
            response = client.execute(method);
            responseStr = EntityUtils.toString(response.getEntity());
        } finally {
            client.releaseConnection(response);
        }
        return responseStr;
    }
    
    private static class MockRestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(RestAuthorityFactory.class).toProvider(
                    FactoryProvider.newFactory(RestAuthorityFactory.class, MockRestAuthority.class));
        }
    }
    
    private static class MockRestAuthority implements RestAuthority {
        @Override
        public boolean isAuthorized(HttpRequest request) {
            return true;
        }
    }
}
