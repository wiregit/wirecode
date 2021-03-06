package com.limegroup.gnutella.version;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.security.SettingsProvider;
import org.limewire.util.Clock;
import org.limewire.util.CommonUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ClockStub;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.security.Certificate;
import com.limegroup.gnutella.security.CertificateProvider;
import com.limegroup.gnutella.security.CertifiedMessageSourceType;
import com.limegroup.gnutella.security.CertifiedMessageVerifier;
import com.limegroup.gnutella.security.CertifiedMessageVerifier.CertifiedMessage;
import com.limegroup.gnutella.stubs.ScheduledExecutorServiceStub;

public class UpdateHandlerTest extends LimeTestCase {
    
    public UpdateHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UpdateHandlerTest.class);
    }

    private Injector injector;
    private Mockery mockery;
    private ImmediateExecutor backgroundExecutor;
    private ActivityCallback activityCallback;
    private HttpExecutor httpExecutor;
    private CapabilitiesVMFactory capabilitiesVmFactory;
    private ConnectionManager connectionManager;
    private ApplicationServices applicationServices;
    private UpdateCollectionFactory updateCollectionFactory;
    private ClockStub clock;
    private DownloadManager downloadManager;
    private SettingsProvider settingsProvider;
    private UpdateMessageVerifier updateMessageVerifier;
    private byte[] guid;

    private File saveFile;
    private CertifiedMessageVerifier certifiedMessageVerifier;
    private CertificateProvider certificateProvider;

    @Override
    public void setUp() {
        mockery = new Mockery();
        activityCallback = mockery.mock(ActivityCallback.class);
        httpExecutor = mockery.mock(HttpExecutor.class);
        capabilitiesVmFactory = mockery.mock(CapabilitiesVMFactory.class);
        connectionManager = mockery.mock(ConnectionManager.class);
        applicationServices = mockery.mock(ApplicationServices.class);
        updateCollectionFactory = mockery.mock(UpdateCollectionFactory.class);
        downloadManager = mockery.mock(DownloadManager.class);
        backgroundExecutor = new ImmediateExecutor();
        updateMessageVerifier = mockery.mock(UpdateMessageVerifier.class);
        certifiedMessageVerifier = mockery.mock(CertifiedMessageVerifier.class);
        certificateProvider = mockery.mock(CertificateProvider.class);
        guid = new byte[16];
        clock = new ClockStub();
        settingsProvider = new SettingsProvider() {
            public long getChangePeriod() {
                return Long.MAX_VALUE;
            }

            public long getGracePeriod() {
                return Long.MAX_VALUE - 1;
            }
        };

        injector = LimeTestUtils.createInjectorNonEagerly(new AbstractModule() {
            @Override
            public void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(
                        Names.named("backgroundExecutor")).toInstance(backgroundExecutor);
                bind(ActivityCallback.class).toInstance(activityCallback);
                bind(HttpExecutor.class).toInstance(httpExecutor);
                bind(CapabilitiesVMFactory.class).toInstance(capabilitiesVmFactory);
                bind(ConnectionManager.class).toInstance(connectionManager);
                bind(ApplicationServices.class).toInstance(applicationServices);
                bind(UpdateCollectionFactory.class).toInstance(updateCollectionFactory);
                bind(Clock.class).toInstance(clock);
                bind(DownloadManager.class).toInstance(downloadManager);
                bind(SettingsProvider.class).toInstance(settingsProvider);
                bind(UpdateMessageVerifier.class).toInstance(updateMessageVerifier);
                bind(CertifiedMessageVerifier.class).annotatedWith(Update.class).toInstance(certifiedMessageVerifier);
                bind(CertificateProvider.class).annotatedWith(Update.class).toInstance(certificateProvider);
            }
        });

        saveFile = new File(CommonUtils.getUserSettingsDir(), "version.xml");
        saveFile.delete();
        assertFalse(saveFile.exists());
    }

    /** tests that we set up bindings correctly */
    public void testBindings() throws Exception {
        mockery.checking(new Expectations() {
            {
                ignoring(activityCallback);
                ignoring(httpExecutor);
                ignoring(capabilitiesVmFactory);
                ignoring(connectionManager);
                ignoring(applicationServices);
                ignoring(updateCollectionFactory);
                ignoring(downloadManager);
            }
        });
        UpdateHandlerImpl updateHandler = (UpdateHandlerImpl) injector
                .getInstance(UpdateHandler.class);
        assertEquals("http://update0.limewire.com/v3/update.def", updateHandler.getTimeoutUrl());
        List<String> maxUrls = updateHandler.getMaxUrls();
        for (int i = 0; i < 10; i++)
            assertEquals("http://update" + (i + 1) + ".limewire.com/v3/update.def", maxUrls.get(i));
        assertEquals(10, maxUrls.size());
    }
    
    
    public void testMaxTriggersHttpAfterSmallDelay() throws Exception {
        final AtomicReference<HttpClientListener> httpClientListenerRef = new AtomicReference<HttpClientListener>();
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final CertifiedMessage certifiedMessage = mockery.mock(CertifiedMessage.class);
        final HttpGet method = new HttpGet();
        final HttpResponse response = mockery.mock(HttpResponse.class);
        final StatusLine statusLine = mockery.mock(StatusLine.class);
        final Sequence requestSequence = mockery.sequence("Request Sequence");
        final Certificate certificate = mockery.mock(Certificate.class);
        final Certificate providerCertificate = mockery.mock(Certificate.class);
        
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));
                inSequence(requestSequence);

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(updateCollection));
                inSequence(requestSequence);
                
                one(updateCollection).getCertifiedMessage();
                will(returnValue(certifiedMessage));
                inSequence(requestSequence);
                
                one(updateCollection).getNewVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(certifiedMessageVerifier).verify(certifiedMessage, null);
                will(returnValue(certificate));
                inSequence(requestSequence);
                
                one(certificate).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                               
                allowing(certificateProvider).get();
                will(returnValue(providerCertificate));
                
                allowing(providerCertificate).getKeyVersion();
                will(returnValue(5));
                
                allowing(applicationServices).getMyGUID();
                will(returnValue(guid));
            
            }
        });

        UpdateHandlerImpl h = (UpdateHandlerImpl) injector.getInstance(UpdateHandler.class);
        assertEquals(0, h.getLatestId());
        h.setMaxUrls(Arrays.asList("http://127.0.0.1:9999/update.def"));
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(new byte[0], null);
        
        assertGreaterThanOrEquals(1000 * 60, backgroundExecutor.getInitialDelay());
        assertLessThanOrEquals(1000 * 60 * 31, backgroundExecutor.getInitialDelay());
        assertEquals(-1, backgroundExecutor.getPeriod());
        assertNotNull(backgroundExecutor.scheduled);

        mockery.checking(new Expectations() {
            {
                one(httpExecutor).execute(with(new TypeSafeMatcher<HttpGet>() {
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("httpMethod");
                    }

                    @Override
                    public boolean matchesSafely(HttpGet item) {
                        assertEquals("GET", item.getMethod());
                        assertTrue(item.getURI().toString(), item.getURI().toString()
                                    .startsWith("http://127.0.0.1:9999/update.def?"));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpParams>() {
                    public void describeTo(org.hamcrest.Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpParams item) {
                        assertEquals(10000, HttpConnectionParams.getConnectionTimeout(item));
                        assertEquals(10000, HttpConnectionParams.getSoTimeout(item));
                        return true;
                    }
                }),
                    with(new TypeSafeMatcher<HttpClientListener>() {
                    public void describeTo(Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpClientListener item) {
                        httpClientListenerRef.set(item);
                        return true;
                    }
                }));
              inSequence(requestSequence);
            }
        });

        backgroundExecutor.scheduled.run();

        assertNotNull(httpClientListenerRef.get());

        mockery.checking(new Expectations() {
            {
                atLeast(1).of(response).getStatusLine();
                will(returnValue(statusLine));
                inSequence(requestSequence);
                
                atLeast(1).of(statusLine).getStatusCode();
                will(returnValue(100));
                inSequence(requestSequence);

                one(httpExecutor).releaseResources(with(same(response)));
                inSequence(requestSequence);
            }
        });

        httpClientListenerRef.get().requestComplete(method, response);
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(0, h.getLatestId());
        assertFalse(saveFile.exists());

        mockery.assertIsSatisfied();
    }

    public void testNetworkMaxIsNotSavedToDiskAndNotForwarded() throws Exception {
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final CertifiedMessage certifiedMessage = mockery.mock(CertifiedMessage.class);
        final Certificate cert = mockery.mock(Certificate.class);
        final Certificate providerCertificate = mockery.mock(Certificate.class);
        
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(updateCollection));

                one(updateCollection).getCertifiedMessage();
                will(returnValue(certifiedMessage));
                
                one(updateCollection).getNewVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));

                one(certifiedMessageVerifier).verify(certifiedMessage, null);
                will(returnValue(cert));
                
                one(cert).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                               
                one(certificateProvider).get();
                will(returnValue(providerCertificate));

                one(providerCertificate).getKeyVersion();
                will(returnValue(7));
                
            }
        });

        UpdateHandlerImpl h = (UpdateHandlerImpl) injector.getInstance(UpdateHandler.class);
        assertEquals(0, h.getLatestId());
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(new byte[0], null);
        assertNotNull(backgroundExecutor.scheduled);
        assertEquals(0, h.getLatestId());
        assertFalse(saveFile.exists());

        Thread.sleep(5000); // sleep a bit just to make sure it didn't take a
        // while to save.
        assertEquals(0, h.getLatestId());
        assertFalse(saveFile.exists());

        mockery.assertIsSatisfied();

    }

    public void testHttpMaxIsSavedToDiskAndForwards() throws Exception {
        final AtomicReference<HttpClientListener> httpClientListenerRef = new AtomicReference<HttpClientListener>();
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final Sequence requestSequence = mockery.sequence("Request Sequence");
        
        final CertifiedMessage certifiedMessage = mockery.mock(CertifiedMessage.class);
        final Certificate cert = mockery.mock(Certificate.class);
        final Certificate providerCertificate = mockery.mock(Certificate.class);
        
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));
                inSequence(requestSequence);

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(updateCollection));
                inSequence(requestSequence);
                
                one(updateCollection).getCertifiedMessage();
                will(returnValue(certifiedMessage));
                
                one(updateCollection).getNewVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));

                one(certifiedMessageVerifier).verify(certifiedMessage, null);
                will(returnValue(cert));
                
                one(cert).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(certificateProvider).get();
                will(returnValue(providerCertificate));

                one(providerCertificate).getKeyVersion();
                will(returnValue(7));
                
                allowing(applicationServices).getMyGUID();
                will(returnValue(guid));
                inSequence(requestSequence);
            }
        });

        UpdateHandlerImpl h = (UpdateHandlerImpl) injector.getInstance(UpdateHandler.class);
        assertEquals(0, h.getLatestId());
        h.setMaxUrls(Arrays.asList("http://127.0.0.1:9999/update.def"));
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(new byte[0], null);
        assertNotNull(backgroundExecutor.scheduled);

        mockery.checking(new Expectations() {
            {
                one(httpExecutor).execute(with(new TypeSafeMatcher<HttpGet>() {
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("httpMethod");
                    }

                    @Override
                    public boolean matchesSafely(HttpGet item) {
                        assertEquals("GET", item.getMethod());
                        assertTrue(item.getURI().toString(), item.getURI().toString()
                                .startsWith("http://127.0.0.1:9999/update.def?"));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpParams>() {
                    public void describeTo(org.hamcrest.Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpParams item) {
                        assertEquals(10000, HttpConnectionParams.getConnectionTimeout(item));
                        assertEquals(10000, HttpConnectionParams.getSoTimeout(item));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpClientListener>() {
                    public void describeTo(Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpClientListener item) {
                        httpClientListenerRef.set(item);
                        return true;
                    }
                }));
                inSequence(requestSequence);
            }
        });

        backgroundExecutor.scheduled.run();
        assertNotNull(httpClientListenerRef.get());

        final HttpGet method = new HttpGet();
        final HttpResponse response = mockery.mock(HttpResponse.class);
        final StatusLine statusLine = mockery.mock(StatusLine.class);
        final HttpEntity httpEntity = mockery.mock(HttpEntity.class);
        final UpdateCollection httpCollection = mockery.mock(UpdateCollection.class);

        final CertifiedMessage httpCertifiedMessage = mockery.mock(CertifiedMessage.class);
        final Certificate httpCert = mockery.mock(Certificate.class);        
        final Certificate providerCert = mockery.mock(Certificate.class);

        mockery.checking(new Expectations() {
            {
                atLeast(1).of(response).getStatusLine();
                will(returnValue(statusLine));
                inSequence(requestSequence);
                
                atLeast(1).of(statusLine).getStatusCode();
                will(returnValue(200));
                inSequence(requestSequence);                

                atLeast(1).of(response).getEntity();
                will(returnValue(httpEntity));
                inSequence(requestSequence);
                
                one(httpEntity).getContent();
                byte [] b = new byte[1];
                ByteArrayInputStream bis = new ByteArrayInputStream(b);
                will(returnValue(bis));
                inSequence(requestSequence);

                one(updateMessageVerifier).inflateNetworkData(with(LimeTestUtils.createByteMatcher(b)));
                byte[] inflated = new byte[2];
                inSequence(requestSequence);
                will(returnValue(inflated));

                one(httpExecutor).releaseResources(with(same(response)));
                inSequence(requestSequence);

                one(updateMessageVerifier).getVerifiedData(with(same(inflated)));
                inSequence(requestSequence);
                will(returnValue("http response"));

                one(updateCollectionFactory).createUpdateCollection("http response");
                inSequence(requestSequence);
                will(returnValue(httpCollection));
                
                one(httpCollection).getCertifiedMessage();
                will(returnValue(httpCertifiedMessage));
                
                one(certifiedMessageVerifier).verify(httpCertifiedMessage, null);
                will(returnValue(httpCert));
                
                one(httpCert).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                atLeast(1).of(httpCollection).getNewVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                atLeast(1).of(httpCollection).getId();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(certificateProvider).get();
                will(returnValue(providerCert));

                one(providerCert).getKeyVersion();
                will(returnValue(7));
                
                one(httpCollection).getTimestamp();
                inSequence(requestSequence);
                will(returnValue(54321L));

                one(capabilitiesVmFactory).updateCapabilities();
                inSequence(requestSequence);

                one(connectionManager).sendUpdatedCapabilities();
                inSequence(requestSequence);
                
                one(certificateProvider).set(httpCert);               
            }
        });

        httpClientListenerRef.get().requestComplete(method, response);
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(54321L, UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue());
        assertEquals(UpdateHandlerImpl.IGNORE_ID, h.getLatestId());
        assertTrue(saveFile.exists());

        mockery.assertIsSatisfied();
    }


    public void testHttpRequestOverridesLocalIfEqual() throws Exception {
        final AtomicReference<HttpClientListener> httpClientListenerRef = new AtomicReference<HttpClientListener>();
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final Sequence requestSequence = mockery.sequence("Request Sequence");
        
        final CertifiedMessage certifiedMessage = mockery.mock(CertifiedMessage.class);
        final Certificate cert = mockery.mock(Certificate.class);
        final Certificate providerCertificate = mockery.mock(Certificate.class);
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));
                inSequence(requestSequence);

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(updateCollection));
                inSequence(requestSequence);

                one(updateCollection).getCertifiedMessage();
                will(returnValue(certifiedMessage));
                
                one(updateCollection).getNewVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
 
//                one(certifiedMessage).getKeyVersion();
//                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
//                
                one(certifiedMessageVerifier).verify(certifiedMessage, null);
                will(returnValue(cert));
                
                one(cert).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
              
                one(certificateProvider).get();
                will(returnValue(providerCertificate));

                one(providerCertificate).getKeyVersion();
                will(returnValue(7));
                
                allowing(applicationServices).getMyGUID();
                will(returnValue(guid));
            }
        });

        UpdateHandlerImpl h = (UpdateHandlerImpl) injector.getInstance(UpdateHandler.class);
        assertEquals(0, h.getLatestId());
        h.setMaxUrls(Arrays.asList("http://127.0.0.1:9999/update.def"));
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(new byte[0], null);
        assertNotNull(backgroundExecutor.scheduled);

        mockery.checking(new Expectations() {
            {
                one(httpExecutor).execute(with(new TypeSafeMatcher<HttpGet>() {
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("httpMethod");
                    }

                    @Override
                    public boolean matchesSafely(HttpGet item) {
                        assertEquals("GET", item.getMethod());
                        assertTrue(item.getURI().toString(), item.getURI().toString()
                                .startsWith("http://127.0.0.1:9999/update.def?"));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpParams>() {
                    public void describeTo(org.hamcrest.Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpParams item) {
                        assertEquals(10000, HttpConnectionParams.getConnectionTimeout(item));
                        assertEquals(10000, HttpConnectionParams.getSoTimeout(item));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpClientListener>() {
                    public void describeTo(Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpClientListener item) {
                        httpClientListenerRef.set(item);
                        return true;
                    }
                }));
                inSequence(requestSequence);
            }
        });

        backgroundExecutor.scheduled.run();
        assertNotNull(httpClientListenerRef.get());

        final HttpGet method = new HttpGet();
        final HttpResponse response = mockery.mock(HttpResponse.class);
        final StatusLine statusLine = mockery.mock(StatusLine.class);
        final HttpEntity httpEntity = mockery.mock(HttpEntity.class);
        final UpdateCollection httpCollection = mockery.mock(UpdateCollection.class);
        
        final CertifiedMessage httpCertifiedMessage = mockery.mock(CertifiedMessage.class);
        final Certificate httpCert = mockery.mock(Certificate.class);        
        final Certificate providerCert = mockery.mock(Certificate.class);

        mockery.checking(new Expectations() {
            {
                atLeast(1).of(response).getStatusLine();
                will(returnValue(statusLine));
                inSequence(requestSequence);
                
                atLeast(1).of(statusLine).getStatusCode();
                will(returnValue(200));
                inSequence(requestSequence);                

                atLeast(1).of(response).getEntity();
                will(returnValue(httpEntity));
                inSequence(requestSequence);
                
                one(httpEntity).getContent();
                byte [] b = new byte[1];
                ByteArrayInputStream bis = new ByteArrayInputStream(b);
                will(returnValue(bis));
                inSequence(requestSequence);

                one(updateMessageVerifier).inflateNetworkData(with(LimeTestUtils.createByteMatcher(b)));
                byte[] inflated = new byte[2];
                inSequence(requestSequence);
                will(returnValue(inflated));

                one(httpExecutor).releaseResources(with(same(response)));
                inSequence(requestSequence);

                one(updateMessageVerifier).getVerifiedData(with(same(inflated)));
                inSequence(requestSequence);
                will(returnValue("http response"));

                one(updateCollectionFactory).createUpdateCollection("http response");
                inSequence(requestSequence);
                will(returnValue(httpCollection));

                one(httpCollection).getCertifiedMessage();
                will(returnValue(httpCertifiedMessage));
                
                one(certifiedMessageVerifier).verify(httpCertifiedMessage, null);
                will(returnValue(httpCert));
                
                one(httpCert).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));

                one(certificateProvider).get();
                will(returnValue(providerCert));

                one(providerCert).getKeyVersion();
                will(returnValue(7));
                
                atLeast(1).of(httpCollection).getNewVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                atLeast(1).of(httpCollection).getId();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(httpCollection).getTimestamp();
                inSequence(requestSequence);
                will(returnValue(54321L));

                one(capabilitiesVmFactory).updateCapabilities();

                one(connectionManager).sendUpdatedCapabilities();
                inSequence(requestSequence);
                
                one(certificateProvider).set(httpCert);       
                
            }
        });

        httpClientListenerRef.get().requestComplete(method, response);
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(54321L, UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue());
        assertEquals(UpdateHandlerImpl.IGNORE_ID, h.getLatestId());        
        assertEquals(UpdateHandlerImpl.IGNORE_ID, h.getNewVersion());
        assertEquals(2, h.getLatestBytes().length);
        
        assertTrue(saveFile.exists());

        mockery.assertIsSatisfied();
    }

    public void testAfterMaxFromHttpNewMaxFromNetworkDoesntRetrigger() throws Exception {
        final AtomicReference<HttpClientListener> httpClientListenerRef = new AtomicReference<HttpClientListener>();
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final Sequence requestSequence = mockery.sequence("Request Sequence");
        
        final CertifiedMessage certifiedMessage = mockery.mock(CertifiedMessage.class);
        final Certificate cert = mockery.mock(Certificate.class);
        final Certificate providerCertificate = mockery.mock(Certificate.class);
        
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));
                inSequence(requestSequence);

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(updateCollection));
                inSequence(requestSequence);
                
                one(updateCollection).getCertifiedMessage();
                will(returnValue(certifiedMessage));
                
                one(updateCollection).getNewVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(certifiedMessageVerifier).verify(certifiedMessage, null);
                will(returnValue(cert));
                
                one(cert).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(certificateProvider).get();
                will(returnValue(providerCertificate));

                one(providerCertificate).getKeyVersion();
                will(returnValue(7));
                
                allowing(applicationServices).getMyGUID();
                will(returnValue(guid));
                inSequence(requestSequence);
            }
        });

        UpdateHandlerImpl h = (UpdateHandlerImpl) injector.getInstance(UpdateHandler.class);
        assertEquals(0, h.getLatestId());
        h.setMaxUrls(Arrays.asList("http://127.0.0.1:9999/update.def"));
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(new byte[0], null);
        assertNotNull(backgroundExecutor.scheduled);

        mockery.checking(new Expectations() {
            {
                one(httpExecutor).execute(with(new TypeSafeMatcher<HttpGet>() {
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("httpMethod");
                    }

                    @Override
                    public boolean matchesSafely(HttpGet item) {
                        assertEquals("GET", item.getMethod());
                        assertTrue(item.getURI().toString(), item.getURI().toString()
                                .startsWith("http://127.0.0.1:9999/update.def?"));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpParams>() {
                    public void describeTo(org.hamcrest.Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpParams item) {
                        assertEquals(10000, HttpConnectionParams.getConnectionTimeout(item));
                        assertEquals(10000, HttpConnectionParams.getSoTimeout(item));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpClientListener>() {
                    public void describeTo(Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpClientListener item) {
                        httpClientListenerRef.set(item);
                        return true;
                    }
                }));
                inSequence(requestSequence);
            }
        });

        backgroundExecutor.scheduled.run();
        assertNotNull(httpClientListenerRef.get());

        final HttpGet method = new HttpGet();
        final HttpResponse response = mockery.mock(HttpResponse.class);
        final StatusLine statusLine = mockery.mock(StatusLine.class);
        final HttpEntity httpEntity = mockery.mock(HttpEntity.class);
        final UpdateCollection httpCollection = mockery.mock(UpdateCollection.class);

        final CertifiedMessage httpCertifiedMessage = mockery.mock(CertifiedMessage.class);
        final Certificate httpCert = mockery.mock(Certificate.class);        
        final Certificate providerCert = mockery.mock(Certificate.class);

        mockery.checking(new Expectations() {
            {
                atLeast(1).of(response).getStatusLine();
                will(returnValue(statusLine));
                inSequence(requestSequence);
                
                atLeast(1).of(statusLine).getStatusCode();
                will(returnValue(200));
                inSequence(requestSequence);                

                atLeast(1).of(response).getEntity();
                will(returnValue(httpEntity));
                inSequence(requestSequence);
                
                one(httpEntity).getContent();
                byte [] b = new byte[1];
                ByteArrayInputStream bis = new ByteArrayInputStream(b);
                will(returnValue(bis));
                inSequence(requestSequence);

                one(updateMessageVerifier).inflateNetworkData(with(LimeTestUtils.createByteMatcher(b)));
                byte[] inflated = new byte[2];
                inSequence(requestSequence);
                will(returnValue(inflated));

                one(httpExecutor).releaseResources(with(same(response)));
                inSequence(requestSequence);

                one(updateMessageVerifier).getVerifiedData(with(same(inflated)));
                inSequence(requestSequence);
                will(returnValue("http response"));

                one(updateCollectionFactory).createUpdateCollection("http response");
                inSequence(requestSequence);
                will(returnValue(httpCollection));
                
                one(httpCollection).getCertifiedMessage();
                will(returnValue(httpCertifiedMessage));
                
                one(certifiedMessageVerifier).verify(httpCertifiedMessage, null);
                will(returnValue(httpCert));
                
                one(httpCert).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));

                one(certificateProvider).get();
                will(returnValue(providerCert));

                one(providerCert).getKeyVersion();
                will(returnValue(7));
                
                atLeast(1).of(httpCollection).getNewVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                atLeast(1).of(httpCollection).getId();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));

                one(httpCollection).getTimestamp();
                inSequence(requestSequence);
                will(returnValue(54321L));

                one(capabilitiesVmFactory).updateCapabilities();
                inSequence(requestSequence);

                one(connectionManager).sendUpdatedCapabilities();
                inSequence(requestSequence);
                
                one(certificateProvider).set(httpCert);               
            }
        });

        httpClientListenerRef.get().requestComplete(method, response);
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(54321L, UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue());
        assertEquals(UpdateHandlerImpl.IGNORE_ID, h.getLatestId());
        assertTrue(saveFile.exists());

        clock.setNow(999999);        
        saveFile.delete();
        
        backgroundExecutor.clear();
        final UpdateCollection secondUpdateCollection = mockery.mock(UpdateCollection.class);
        final CertifiedMessage secondCertifiedMessage = mockery.mock(CertifiedMessage.class);
        final Certificate secondCert = mockery.mock(Certificate.class);         
        final Certificate secondProviderCertificate = mockery.mock(Certificate.class);
        
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));
                inSequence(requestSequence);

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(secondUpdateCollection));
                inSequence(requestSequence);

                one(secondUpdateCollection).getCertifiedMessage();
                will(returnValue(secondCertifiedMessage));
                               
                one(secondUpdateCollection).getNewVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(certifiedMessageVerifier).verify(secondCertifiedMessage, null);
                will(returnValue(secondCert));
                
                one(secondCert).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(certificateProvider).get();
                will(returnValue(secondProviderCertificate));

                one(secondProviderCertificate).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
            }
        });
        h.handleNewData(new byte[0], null);
        
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(54321L, UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue());
        assertEquals(UpdateHandlerImpl.IGNORE_ID, h.getLatestId());
        assertFalse(saveFile.exists());
        assertNull(backgroundExecutor.getRunnable());

        mockery.assertIsSatisfied();
    }
    
    public void testFailedHttpRequestUpdatesFailoverTime() throws Exception {
        final AtomicReference<HttpClientListener> httpClientListenerRef = new AtomicReference<HttpClientListener>();
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final byte[] data = new byte[0];
        final String verified = "";
        
        final CertifiedMessage certifiedMessage = mockery.mock(CertifiedMessage.class);
        final Certificate cert = mockery.mock(Certificate.class);
        final Certificate providerCertificate = mockery.mock(Certificate.class);
        
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(data);
                will(returnValue(verified));
                one(updateCollectionFactory).createUpdateCollection(verified);
                will(returnValue(updateCollection));

                one(updateCollection).getNewVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(updateCollection).getCertifiedMessage();
                will(returnValue(certifiedMessage));
                
                one(certifiedMessageVerifier).verify(certifiedMessage, null);
                will(returnValue(cert));
                
                one(cert).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(certificateProvider).get();
                will(returnValue(providerCertificate));

                one(providerCertificate).getKeyVersion();
                will(returnValue(7));
                                        
                allowing(applicationServices).getMyGUID();
                will(returnValue(guid));
            }
        });

        UpdateHandlerImpl h = injector.getInstance(UpdateHandlerImpl.class);
        assertEquals(0, h.getLatestId());
        h.setMaxUrls(Arrays.asList("http://127.0.0.1:9999/update.def"));
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(data, null);
        assertNotNull(backgroundExecutor.scheduled);

        mockery.checking(new Expectations() {
            {
                one(httpExecutor).execute(with(new TypeSafeMatcher<HttpGet>() {
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("httpMethod");
                    }

                    @Override
                    public boolean matchesSafely(HttpGet item) {
                        assertEquals("GET", item.getMethod());
                        assertTrue(item.getURI().toString(), item.getURI().toString()
                                .startsWith("http://127.0.0.1:9999/update.def?"));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpParams>() {
                    public void describeTo(org.hamcrest.Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpParams item) {
                        assertEquals(10000, HttpConnectionParams.getConnectionTimeout(item));
                        assertEquals(10000, HttpConnectionParams.getSoTimeout(item));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpClientListener>() {
                    public void describeTo(Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpClientListener item) {
                        httpClientListenerRef.set(item);
                        return true;
                    }
                }));
            }
        });

        backgroundExecutor.scheduled.run();
        assertNotNull(httpClientListenerRef.get());

        final HttpGet method = new HttpGet();
        final HttpResponse response = mockery.mock(HttpResponse.class);
        mockery.checking(new Expectations() {
            {
                one(httpExecutor).releaseResources(response);
            }
        });
        httpClientListenerRef.get().requestFailed(method, response, null);
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(0, h.getLatestId());
        assertFalse(saveFile.exists());
        mockery.assertIsSatisfied();
    }
    
    public void testBadHttpBodyUpdatesFailoverTime() throws Exception {
        final AtomicReference<HttpClientListener> httpClientListenerRef = new AtomicReference<HttpClientListener>();
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final byte[] data = new byte[0];
        final String verified = "";

        final CertifiedMessage certifiedMessage = mockery.mock(CertifiedMessage.class);
        final Certificate cert = mockery.mock(Certificate.class);
        final Certificate providerCertificate = mockery.mock(Certificate.class);
        
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(data);
                will(returnValue(verified));
                one(updateCollectionFactory).createUpdateCollection(verified);
                will(returnValue(updateCollection));
                
                one(updateCollection).getCertifiedMessage();
                will(returnValue(certifiedMessage));
                
                one(updateCollection).getNewVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(certifiedMessageVerifier).verify(certifiedMessage, null);
                will(returnValue(cert));
                
                one(cert).getKeyVersion();
                will(returnValue(UpdateHandlerImpl.IGNORE_ID));
                
                one(certificateProvider).get();
                will(returnValue(providerCertificate));

                one(providerCertificate).getKeyVersion();
                will(returnValue(7));

                allowing(applicationServices).getMyGUID();
                will(returnValue(guid));
            }
        });

        UpdateHandlerImpl h = injector.getInstance(UpdateHandlerImpl.class);
        assertEquals(0, h.getLatestId());
        h.setMaxUrls(Arrays.asList("http://127.0.0.1:9999/update.def"));
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(data, null);
        assertNotNull(backgroundExecutor.scheduled);

        mockery.checking(new Expectations() {
            {
                one(httpExecutor).execute(with(new TypeSafeMatcher<HttpGet>() {
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("httpMethod");
                    }

                    @Override
                    public boolean matchesSafely(HttpGet item) {
                        assertEquals("GET", item.getMethod());
                        assertTrue(item.getURI().toString(), item.getURI().toString()
                                .startsWith("http://127.0.0.1:9999/update.def?"));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpParams>() {
                    public void describeTo(org.hamcrest.Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpParams item) {
                        assertEquals(10000, HttpConnectionParams.getConnectionTimeout(item));
                        assertEquals(10000, HttpConnectionParams.getSoTimeout(item));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpClientListener>() {
                    public void describeTo(Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpClientListener item) {
                        httpClientListenerRef.set(item);
                        return true;
                    }
                }));
            }
        });

        backgroundExecutor.scheduled.run();
        assertNotNull(httpClientListenerRef.get());

        final HttpGet method = new HttpGet();
        final HttpResponse response = mockery.mock(HttpResponse.class);
        final StatusLine statusLine = mockery.mock(StatusLine.class);
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(response).getStatusLine();
                will(returnValue(statusLine));
                atLeast(1).of(statusLine).getStatusCode();
                will(returnValue(200));
                atLeast(1).of(response).getEntity();
                will(returnValue(null));
                one(httpExecutor).releaseResources(response);
            }
        });
        httpClientListenerRef.get().requestComplete(method, response);
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(0, h.getLatestId());
        assertFalse(saveFile.exists());
        mockery.assertIsSatisfied();
    }
        
    public void testUnverifiableDataFromNetworkIsInvalid() {
        final byte[] data = new byte[0];
        final ReplyHandler handler = mockery.mock(ReplyHandler.class);
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(data);
                will(returnValue(null));
            }
        });
        UpdateHandlerImpl h = injector.getInstance(UpdateHandlerImpl.class);
        h.handleDataInternal(data, CertifiedMessageSourceType.FROM_NETWORK, handler);
        mockery.assertIsSatisfied();        
    }
    
    public void testVerifiedDataFromNetworkIsValid() throws Exception {
        final byte[] data = new byte[0];
        final String verified = "";
        final ReplyHandler handler = mockery.mock(ReplyHandler.class);
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final int id = 12345;
        
        final CertifiedMessage certifiedMessage = mockery.mock(CertifiedMessage.class);
        final Certificate cert = mockery.mock(Certificate.class);
        final Certificate providerCertificate = mockery.mock(Certificate.class);
        
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(data);
                will(returnValue(verified));
                one(updateCollectionFactory).createUpdateCollection(verified);
                will(returnValue(updateCollection));
                
                allowing(updateCollection).getNewVersion();
                will(returnValue(7));
                       
                one(updateCollection).getCertifiedMessage();
                will(returnValue(certifiedMessage));
                        
               /* one(certifiedMessage).getKeyVersion();
                will(returnValue(id));*/
                        
                one(certifiedMessageVerifier).verify(certifiedMessage, handler);
                will(returnValue(cert));                     
                
                one(cert).getKeyVersion();
                will(returnValue(id));
                
                atLeast(1).of(updateCollection).getId();
                will(returnValue(id));
                
                one(certificateProvider).get();
                will(returnValue(providerCertificate));
                one(providerCertificate).getKeyVersion();
                will(returnValue(5));                
                
                // The ID is not IGNORE_ID and the update type is FROM_NETWORK,
                // so the update will be stored and propagated
                one(updateCollection).getTimestamp();
                will(returnValue(System.currentTimeMillis()));
                one(capabilitiesVmFactory).updateCapabilities();
                one(connectionManager).sendUpdatedCapabilities();
                
                one(certificateProvider).set(cert);
            }
        });
        UpdateHandlerImpl h = injector.getInstance(UpdateHandlerImpl.class);
        assertLessThan(id, h.getLatestId());
        h.handleDataInternal(data, CertifiedMessageSourceType.FROM_NETWORK, handler);
        assertEquals(id, h.getLatestId());
        mockery.assertIsSatisfied();
    }
    
    
    private class ImmediateExecutor extends ScheduledExecutorServiceStub {

        private volatile Runnable scheduled;
        private volatile long initialDelay = -1;
        private volatile long period = -1;
//        private volatile TimeUnit timeUnit;
        
        Runnable getRunnable() {
            return scheduled;
        }
        
        long getInitialDelay() {
            return initialDelay;
        }
        
        long getPeriod() {
            return period;
        }
        
//        TimeUnit getTimeUnit() {
//            return timeUnit;
//        }
        
        void clear() {
            scheduled = null;
            initialDelay = -1;
            period = -1;
//            timeUnit = null;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            scheduled = command;
            this.initialDelay = delay;
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                long period, TimeUnit unit) {
            scheduled = command;
            this.initialDelay = -1;
            this.period = period;
//            this.timeUnit = unit;
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                long delay, TimeUnit unit) {
            scheduled = command;
            this.initialDelay = -1;
            this.period = delay;
//            this.timeUnit = unit;
            return null;
        }

    }
}
