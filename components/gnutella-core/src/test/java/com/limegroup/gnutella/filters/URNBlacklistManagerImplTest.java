package com.limegroup.gnutella.filters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.SimpleTimeZone;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpParams;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.core.settings.FilterSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.util.Base32;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import com.limegroup.gnutella.SpamServices;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;

public class URNBlacklistManagerImplTest extends LimeTestCase {

    private URNBlacklistManagerImpl urnBlacklistManager;
    private Mockery context;
    private HttpExecutor httpExecutor;
    private HttpParams defaultParams;
    private SpamServices spamServices;
    private ServiceRegistry serviceRegistry;

    public URNBlacklistManagerImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(URNBlacklistManagerImplTest.class);
    }

    @Override
    public void setUp() {
        // This URL won't actually be hit, but we need to supply one
        FilterSettings.URN_BLACKLIST_UPDATE_URLS.set(
                new String[] {"http://127.0.0.1/"});
        context = new Mockery();
        httpExecutor = context.mock(HttpExecutor.class);
        defaultParams = context.mock(HttpParams.class);
        spamServices = context.mock(SpamServices.class);
        serviceRegistry = context.mock(ServiceRegistry.class);
        context.checking(new Expectations() {{
            one(serviceRegistry).register(with(any(
                    URNBlacklistManagerImpl.class)));
        }});
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(HttpExecutor.class).toInstance(httpExecutor);
                bind(HttpParams.class).annotatedWith(Names.named(
                "defaults")).toInstance(defaultParams);
                bind(SpamServices.class).toInstance(spamServices);
                bind(ServiceRegistry.class).toInstance(serviceRegistry);
            }
        };
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, m);
        urnBlacklistManager =
            injector.getInstance(URNBlacklistManagerImpl.class);
        context.assertIsSatisfied();
    }

    @Override
    public void tearDown() {
        FilterSettings.URN_BLACKLIST_UPDATE_URLS.revertToDefault();
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.revertToDefault();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.revertToDefault();
        urnBlacklistManager.getFile().delete();
    }

    public void testChecksForUpdatesAtStartupIfNeeded() {
        long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();
    }

    public void testDoesNotCheckForUpdatesAtStartupIfNotNeeded() {
        long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now + 1000);
        urnBlacklistManager.start();
        context.assertIsSatisfied();        
    }

    public void testReadingFileTriggersCheckIfFileIsMissing() {
        File f = urnBlacklistManager.getFile();
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
        }});
        assertFalse(f.exists());
        assertFalse(urnBlacklistManager.iterator().hasNext());
        context.assertIsSatisfied();
    }

    public void testReadingFileTriggersCheckIfFileIsEmpty() throws Exception {
        File f = urnBlacklistManager.getFile();
        FileOutputStream out = new FileOutputStream(f);
        out.write(new byte[0]);
        out.flush();
        out.close();
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
        }});
        assertTrue(f.exists());
        assertFalse(urnBlacklistManager.iterator().hasNext());
        context.assertIsSatisfied();        
    }

    public void testReadingFileTriggersCheckIfFileIsCorrupt() throws Exception {
        byte[] data = new byte[19];
        for(int i = 0; i < data.length; i++) {
            data[i] = (byte)i;
        }
        File f = urnBlacklistManager.getFile();
        FileOutputStream out = new FileOutputStream(f);
        out.write(data);
        out.flush();
        out.close();
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
        }});
        assertTrue(f.exists());
        assertFalse(urnBlacklistManager.iterator().hasNext());
        context.assertIsSatisfied();        
    }

    public void testReadingFileReturnsDataIfFileIsNotCorrupt() throws Exception {
        byte[] data = new byte[20];
        for(int i = 0; i < data.length; i++) {
            data[i] = (byte)i;
        }
        String urn = Base32.encode(data);
        File f = urnBlacklistManager.getFile();
        FileOutputStream out = new FileOutputStream(f);
        out.write(data);
        out.flush();
        out.close();
        assertTrue(f.exists());
        Iterator<String> i = urnBlacklistManager.iterator();
        assertTrue(i.hasNext());
        assertEquals(urn, i.next());
        assertFalse(i.hasNext());
        context.assertIsSatisfied();
    }

    public void testEmptyURLSettingSetsNextUpdateTime() throws Exception {
        FilterSettings.URN_BLACKLIST_UPDATE_URLS.set(new String[0]);
        long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        urnBlacklistManager.start();
        context.assertIsSatisfied();
        assertNextUpdateTimeIsSet(now);
    }

    public void testFailedHeadRequestSetsNextUpdateTime() {
        long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        final HttpResponse response = context.mock(HttpResponse.class);
        final StatusLine statusLine = context.mock(StatusLine.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new FailedRequestAction(response));
            allowing(response).getStatusLine();
            will(returnValue(statusLine));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();
        assertNextUpdateTimeIsSet(now);
    }

    public void testMissingLastModifiedHeaderSetsNextUpdateTime() {
        long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        final HttpResponse response = context.mock(HttpResponse.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(null));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();
        assertNextUpdateTimeIsSet(now);
    }

    public void testStaleLastModifiedHeaderSetsNextUpdateTime() {
        final long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.set(now - 2000);
        final HttpResponse response = context.mock(HttpResponse.class);
        final Header header = context.mock(Header.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(header));
            allowing(header).getValue();
            will(returnValue(dateString(now - 3000))); // Stale
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();
        assertNextUpdateTimeIsSet(now);
    }

    public void testFreshLastModifiedHeaderTriggersGetRequest() {
        final long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.set(now - 2000);
        final HttpResponse response = context.mock(HttpResponse.class);
        final Header header = context.mock(Header.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(header));
            allowing(header).getValue();
            will(returnValue(dateString(now))); // Fresh
            one(httpExecutor).execute(with(any(HttpGet.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();
    }

    public void testFailedGetRequestSetsNextUpdateTime() {
        final long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.set(now - 2000);
        final HttpResponse response = context.mock(HttpResponse.class);
        final Header header = context.mock(Header.class);
        final StatusLine statusLine = context.mock(StatusLine.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(header));
            allowing(header).getValue();
            will(returnValue(dateString(now))); // Fresh
            one(httpExecutor).execute(with(any(HttpGet.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new FailedRequestAction(response));
            allowing(response).getStatusLine();
            will(returnValue(statusLine));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();        
        assertNextUpdateTimeIsSet(now);
    }

    public void testMissingBodySetsNextUpdateTime() {
        final long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.set(now - 2000);
        final HttpResponse response = context.mock(HttpResponse.class);
        final Header header = context.mock(Header.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(header));
            allowing(header).getValue();
            will(returnValue(dateString(now))); // Fresh
            one(httpExecutor).execute(with(any(HttpGet.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getEntity();
            will(returnValue(null));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();        
        assertNextUpdateTimeIsSet(now);        
    }

    public void testBodyIsWrittenToFileAndSpamFiltersAreReloaded()
    throws IOException {
        final long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.set(now - 2000);
        File f = urnBlacklistManager.getFile();
        f.delete();
        final HttpResponse response = context.mock(HttpResponse.class);
        final Header header = context.mock(Header.class);
        final HttpEntity entity = context.mock(HttpEntity.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(header));
            allowing(header).getValue();
            will(returnValue(dateString(now))); // Fresh
            one(httpExecutor).execute(with(any(HttpGet.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getEntity();
            will(returnValue(entity));
            one(entity).writeTo(with(any(FileOutputStream.class)));
            one(spamServices).reloadSpamFilters();
        }});
        assertFalse(f.exists());
        urnBlacklistManager.start();
        assertTrue(f.exists());
        context.assertIsSatisfied();
        assertNextUpdateTimeIsSet(now);
    }

    private void assertNextUpdateTimeIsSet(long now) {
        long last = FilterSettings.LAST_URN_BLACKLIST_UPDATE.getValue();
        long next = FilterSettings.NEXT_URN_BLACKLIST_UPDATE.getValue();
        assertGreaterThanOrEquals(now, last);
        assertGreaterThan(now, next);
    }

    private String dateString(long time) {
        // Never trust a class with 'simple' in its name
        SimpleDateFormat sdf =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return sdf.format(new Date(time));
    }

    private static class FailedRequestAction extends CustomAction {

        private final HttpResponse response;

        FailedRequestAction(HttpResponse response) {
            super("Failed HTTP request");
            this.response = response;
        }

        @Override
        public Object invoke(Invocation invocation) {
            HttpUriRequest request =
                (HttpUriRequest)invocation.getParameter(0);
            HttpClientListener listener =
                (HttpClientListener)invocation.getParameter(2);
            listener.requestFailed(request, response, new IOException("Mock"));
            return null;
        }
    }

    private static class SuccessfulRequestAction extends CustomAction {

        private final HttpResponse response;

        SuccessfulRequestAction(HttpResponse response) {
            super("Successful HTTP request");
            this.response = response;
        }

        @Override
        public Object invoke(Invocation invocation) {
            HttpUriRequest request =
                (HttpUriRequest)invocation.getParameter(0);
            HttpClientListener listener =
                (HttpClientListener)invocation.getParameter(2);
            listener.requestComplete(request, response);
            return null;
        }
    }
}
