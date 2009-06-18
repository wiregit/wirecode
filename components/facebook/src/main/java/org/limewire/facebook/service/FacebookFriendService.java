package org.limewire.facebook.service;

import java.util.Arrays;
import java.util.concurrent.Callable;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ThreadPoolListeningExecutor;
import org.limewire.facebook.service.livemessage.PresenceHandlerFactory;
import org.limewire.facebook.service.settings.FacebookAuthServerUrls;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.friend.api.FriendConnectionFactoryRegistry;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.impl.feature.LimewireFeatureInitializer;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.http.httpclient.SimpleLimeHttpClient;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class FacebookFriendService implements FriendConnectionFactory, Service {
    
    private static Log LOG = LogFactory.getLog(FacebookFriendService.class);
    
    private final ThreadPoolListeningExecutor executorService;
    private final FacebookFriendConnectionFactory connectionFactory;

    private final PresenceHandlerFactory presenceHandlerFactory;
    private final FeatureRegistry featureRegistry;
    private volatile FacebookFriendConnection connection;

    private final Provider<String[]> authServerUrls;

    @Inject FacebookFriendService(FacebookFriendConnectionFactory connectionFactory,
                                  PresenceHandlerFactory presenceHandlerFactory,
                                  FeatureRegistry featureRegistry,
                                  @FacebookAuthServerUrls Provider<String[]> authServerUrls) {
        this.connectionFactory = connectionFactory;
        this.presenceHandlerFactory = presenceHandlerFactory;
        this.featureRegistry = featureRegistry;
        this.authServerUrls = authServerUrls;
        executorService = ExecutorsHelper.newSingleThreadExecutor(ExecutorsHelper.daemonThreadFactory(getClass().getSimpleName()));    
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }

    @Inject
    public void register(ListenerSupport<FriendConnectionEvent> listenerSupport) {
        listenerSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                switch (event.getType()) {
                    case DISCONNECTED:
                    case CONNECT_FAILED:
                        synchronized (FacebookFriendService.this) {
                            if(connection != null && connection == event.getSource()) {
                                connection = null;
                            }
                        }
                        break;
                    default:
                }
            }
        });
    }

    @Override
    public void start() {
        
    }

    @Override
    @Asynchronous
    public void stop() {
        logoutImpl();
    }

    @Override
    public void initialize() {
        
    }

    @Override
    public String getServiceName() {
        return getClass().getSimpleName();
    }
    
    private void logoutImpl() {
        synchronized (this) {
            if(connection != null) {
                connection.logoutImpl();
                connection = null;
            }
        }
    }

    @Override
    public ListeningFuture<FriendConnection> login(final FriendConnectionConfiguration configuration) {
        return executorService.submit(new Callable<FriendConnection>() {
            @Override
            public FriendConnection call() throws Exception {
                return loginImpl(configuration);
            }
        });
    }

    @Override
    @Inject
    public void register(FriendConnectionFactoryRegistry registry) {
        registry.register(Network.Type.FACEBOOK, this);
    }
    
    FacebookFriendConnection loginImpl(FriendConnectionConfiguration configuration) throws FriendException {
        LOG.debug("creating connection");
        connection = connectionFactory.create(configuration);
        presenceHandlerFactory.create(connection);
        new LimewireFeatureInitializer().register(featureRegistry);
        LOG.debug("logging in to facebook...");
        connection.loginImpl();
        LOG.debug("logged in.");
        return connection;
    }

    @Override
    public ListeningFuture<String> requestLoginUrl(final FriendConnectionConfiguration configuration) {
        return executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                HttpParams params = new BasicHttpParams();
                HttpClientParams.setRedirecting(params, false);
                LimeHttpClient httpClient = new SimpleLimeHttpClient();
                httpClient.setParams(params);   
                String[] authUrls = authServerUrls.get();
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("auth urls to choose from {0}", Arrays.asList(authUrls));
                }
                String authUrl = authUrls[(int)Math.floor(Math.random()*authUrls.length)];
                LOG.debugf("picked auth url: {0}", authUrl);
                HttpGet getMethod = new HttpGet(authUrl);
                HttpResponse response = httpClient.execute(getMethod);
                assert response.getStatusLine().getStatusCode() == 302;
                String url = response.getFirstHeader("Location").getValue();
                LOG.debugf("login url: {0}", url);
                configuration.setAttribute("auth-token", parseAuthToken(url));
                return url;
            }
        });
    }

    private static String parseAuthToken(String url) {
        int authTokenIndex = url.indexOf("auth_token=");
        if(authTokenIndex > -1) {
            String authTokenPart = url.substring(authTokenIndex + "auth_token=".length());
            int nextParamIndex = authTokenPart.indexOf('&');
            if (nextParamIndex > -1) {
                return authTokenPart.substring(0, nextParamIndex);
            } else {
                // last param return it all
                return authTokenPart;
            }
        }
        throw new IllegalArgumentException(url);
    }
    
        
}
