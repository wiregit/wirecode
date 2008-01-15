package org.limewire.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.LayeredSocketFactory;
import org.apache.http.conn.PlainSocketFactory;
import org.apache.http.conn.Scheme;
import org.apache.http.conn.SchemeRegistry;
import org.apache.http.conn.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.collection.Periodic;
import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.inject.AbstractModule;
import org.limewire.net.SocketBindingSettingsImpl;
import org.limewire.net.SocketsManager;
import org.limewire.nio.NBSocket;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * <code>Guice</code> module to provide bindings for <code>http</code> component classes.
 */
public class LimeWireHttpModule extends AbstractModule {
    
    protected void configure() {
        //bindAll(Names.named("httpExecutor"), ScheduledExecutorService.class, BackgroundTimerProvider.class, ExecutorService.class, Executor.class);
        requestStaticInjection(HttpClientManager.class);
        bind(ClientConnectionManager.class).annotatedWith(Names.named("nonBlockingConnectionManager")).toProvider(LimeClientConnectionManagerProvider.class).in(Scopes.SINGLETON);
        bind(ClientConnectionManager.class).annotatedWith(Names.named("blockingConnectionManager")).toProvider(DefaultClientConnectionManagerProvider.class).in(Scopes.SINGLETON);
        bind(LimeHttpClient.class).toProvider(NonBlockingLimeHttpClientProvider.class);
        bind(LimeHttpClient.class).annotatedWith(Names.named("blockingClient")).toProvider(BlockingLimeHttpClientProvider.class);
        bind(SocketWrappingClient.class).toProvider(SocketWrappingClientImpl.class);
    }
    
    private abstract static class AbstractLimeHttpClientProvider implements Provider<LimeHttpClient> {
        private ClientConnectionManager manager;
        private final ScheduledExecutorService scheduler;

        public AbstractLimeHttpClientProvider(ClientConnectionManager manager, ScheduledExecutorService scheduler) {
            this.manager = manager;
            this.scheduler = scheduler;
        }

        public LimeHttpClient get() {
            return new LimeHttpClientImpl(manager, scheduler);
        }
    }
    
    @Singleton
    private static class BlockingLimeHttpClientProvider extends AbstractLimeHttpClientProvider {
        @Inject
        public BlockingLimeHttpClientProvider(@Named("blockingConnectionManager") ClientConnectionManager manager,
                                              @Named("backgroundExecutor") Provider<ScheduledExecutorService> scheduler) {
            super(manager, scheduler.get());
        }        
    }
    
    @Singleton
    private static class NonBlockingLimeHttpClientProvider extends AbstractLimeHttpClientProvider {
        @Inject
        public NonBlockingLimeHttpClientProvider(@Named("nonBlockingConnectionManager") ClientConnectionManager manager,
                                                 @Named("backgroundExecutor") Provider<ScheduledExecutorService> scheduler) {
            super(manager, scheduler.get());
        }        
    }
    
    @Singleton
    private static class SocketWrappingClientImpl implements Provider<SocketWrappingClient> {
        private final Provider<ScheduledExecutorService> scheduler;

        @Inject
        public SocketWrappingClientImpl(@Named("backgroundExecutor") Provider<ScheduledExecutorService> scheduler) {
            this.scheduler = scheduler;
        }

        public SocketWrappingClient get() {
            return new LimeHttpClientImpl(scheduler.get());
        }
    }
    
    @Singleton
    private static class DefaultClientConnectionManagerProvider extends AbstractLazySingletonProvider<ClientConnectionManager> {
        // TODO more Guice'ing and code reuse
        private static ClientConnectionManager manager;
        private Periodic connectionCloser;

        @Inject
        public DefaultClientConnectionManagerProvider(@Named("backgroundExecutor") Provider<ScheduledExecutorService> scheduler) {
            this.connectionCloser = new Periodic(new IdleConnectionCloser(manager), scheduler.get());
        }
        
        protected ClientConnectionManager createObject() {
            if(manager != null) {
                // TODO hack to handle lifecycle of connection managers
                // need to move this to something that is called by LifeCycleManager
                //manager.shutdown();
                connectionCloser.unschedule();
            }
            HttpParams defaultParams = new DefaultHttpParams();
        
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("tls", SSLSocketFactory.getSocketFactory(),80)); // TODO no ssl?
            registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(),80)); // TODO no ssl?      
            
            manager = new ThreadSafeClientConnManager(defaultParams, registry);
            
            connectionCloser.scheduleAtFixedRate(0L, 10L, TimeUnit.SECONDS);
            
            return manager; 
        }
    }
    
    @Singleton
    private static class LimeClientConnectionManagerProvider extends AbstractLazySingletonProvider<ClientConnectionManager> {
        // TODO more Guice'ing and code reuse
        private final Provider<SocketsManager> socketsManager;
        private static ClientConnectionManager manager;
        private Periodic connectionCloser;

        @Inject
        public LimeClientConnectionManagerProvider(Provider<SocketsManager> socketsManager, @Named("backgroundExecutor") Provider<ScheduledExecutorService> scheduler) {
            this.socketsManager = socketsManager;
            this.connectionCloser = new Periodic(new IdleConnectionCloser(manager), scheduler.get());
        }
        
        protected ClientConnectionManager createObject() {
            if(manager != null) {
                // TODO hack to handle lifecycle of connection managers
                // need to move this to something that is called by LifeCycleManager
                //manager.shutdown();
                connectionCloser.unschedule();
            }
            HttpParams defaultParams = new DefaultHttpParams();
        
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.PLAIN), 80));
            registry.register(new Scheme("tls", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.TLS),80));
            registry.register(new Scheme("https", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.TLS),80)); 
            
            manager = new ThreadSafeClientConnManager(defaultParams, registry);
            
            connectionCloser.scheduleAtFixedRate(0L, 10L, TimeUnit.SECONDS);
            
            return manager; 
        }
    }
    
    private static class LimeSocketFactory implements SocketFactory {
        final Provider<SocketsManager> socketsManager;
        final SocketsManager.ConnectType type;
        
        public LimeSocketFactory(Provider<SocketsManager> socketsManager, SocketsManager.ConnectType type) {
            this.socketsManager = socketsManager;
            this.type = type;
        }

        public Socket createSocket() throws IOException {
            return socketsManager.get().create(type);
        }

        public Socket connectSocket(Socket socket, String targetHost, int targetPort, InetAddress localAddress, int localPort, HttpParams httpParams) throws IOException, UnknownHostException, ConnectTimeoutException {
            if(socket == null) {
                socket = createSocket();
            }            
            return socketsManager.get().connect((NBSocket)socket, new SocketBindingSettingsImpl(localAddress,  localPort), new InetSocketAddress(targetHost,targetPort), HttpConnectionParams.getConnectionTimeout(httpParams), type);
        }

        public boolean isSecure(Socket socket) throws IllegalArgumentException {
            return false; // TODO type.equals(SocketsManager.ConnectType.TLS);  // TODO use socket instead?
        }        
    }
    
    private static class SecureLimeSocketFactory extends LimeSocketFactory implements LayeredSocketFactory {
        public SecureLimeSocketFactory(Provider<SocketsManager> socketsManager, SocketsManager.ConnectType type) {
            super(socketsManager, type);
        }

        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
            return socket;
        }
    }
    
    @Singleton
    private static class BackgroundTimerProvider extends AbstractLazySingletonProvider<ScheduledExecutorService> {
        protected ScheduledExecutorService createObject() {
            return new SimpleTimer(true);
        }
    }
}
