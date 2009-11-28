package org.limewire.http;

import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.limewire.common.LimeWireCommonModule;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.net.LimeWireNetTestModule;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class HttpClientTest extends BaseTestCase {
    
    private LimeHttpClient client;    

    public static Test suite() {
        return buildTestSuite(HttpClientTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = Guice.createInjector(new LimeWireHttpModule(), new LimeWireCommonModule(), new LimeWireNetTestModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(new SimpleTimer(true));
            }
        });
        client = injector.getInstance(LimeHttpClient.class);
    }
    
    public void testHttpGet() throws Exception {
        HttpResponse response = client.execute(new HttpGet("http://www.limewire.org/sam/ssltest.txt"));
        assertEquals("SSL TEST\n", EntityUtils.toString(response.getEntity()));
    }
    
    public void testHttpsGet() throws Exception {
        HttpResponse response = client.execute(new HttpGet("https://www.limewire.org/sam/ssltest.txt"));
        assertEquals("SSL TEST\n", EntityUtils.toString(response.getEntity()));
    }    
    
}
