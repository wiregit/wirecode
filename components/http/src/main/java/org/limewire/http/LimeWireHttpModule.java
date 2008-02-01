package org.limewire.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.conn.ConnectTimeoutException;
//import org.apache.http.conn.LayeredSocketFactory;
import org.apache.http.conn.PlainSocketFactory;
import org.apache.http.conn.Scheme;
import org.apache.http.conn.SchemeRegistry;
import org.apache.http.conn.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.inject.AbstractModule;
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
        bind(ReapingClientConnectionManager.class).annotatedWith(Names.named("nonBlockingConnectionManager")).toProvider(LimeClientConnectionManagerProvider.class).in(Scopes.SINGLETON);
        bind(ReapingClientConnectionManager.class).annotatedWith(Names.named("blockingConnectionManager")).toProvider(DefaultClientConnectionManagerProvider.class).in(Scopes.SINGLETON);
        bind(ReapingClientConnectionManager.class).annotatedWith(Names.named("socketWrappingConnectionManager")).toProvider(SocketWrappingClientConnectionManagerProvider.class).in(Scopes.SINGLETON);
        bind(LimeHttpClient.class).toProvider(NonBlockingLimeHttpClientProvider.class);
        bind(LimeHttpClient.class).annotatedWith(Names.named("blockingClient")).toProvider(BlockingLimeHttpClientProvider.class);
        bind(SocketWrappingHttpClient.class).toProvider(SocketWrappingLimeHttpClientProvider.class);
        bind(SchemeRegistry.class).toProvider(DefaultSchemeRegistryProvider.class);
        bind(SchemeRegistry.class).annotatedWith(Names.named("limeSchemeRegistry")).toProvider(LimeSchemeRegistryProvider.class);
        bind(SchemeRegistry.class).annotatedWith(Names.named("socketWrappingSchemeRegistry")).toProvider(SocketWrappingSchemeRegistryProvider.class);
        bind(SocketWrapperProtocolSocketFactory.class);
    }
    
    private abstract static class AbstractLimeHttpClientProvider implements Provider<SocketWrappingHttpClient> {
        private ReapingClientConnectionManager manager;

        public AbstractLimeHttpClientProvider(ReapingClientConnectionManager manager) {
            this.manager = manager;
        }

        public SocketWrappingHttpClient get() {
            return new LimeHttpClientImpl(manager);
        }
    }
    
    @Singleton
    private static class BlockingLimeHttpClientProvider extends AbstractLimeHttpClientProvider {
        @Inject
        public BlockingLimeHttpClientProvider(@Named("blockingConnectionManager") ReapingClientConnectionManager manager) {
            super(manager);
        }        
    }
    
    @Singleton
    private static class NonBlockingLimeHttpClientProvider extends AbstractLimeHttpClientProvider {
        @Inject
        public NonBlockingLimeHttpClientProvider(@Named("nonBlockingConnectionManager") ReapingClientConnectionManager manager) {
            super(manager);
        }        
    }
    
    @Singleton
    private static class SocketWrappingLimeHttpClientProvider extends AbstractLimeHttpClientProvider {
        @Inject
        public SocketWrappingLimeHttpClientProvider(@Named("socketWrappingConnectionManager") ReapingClientConnectionManager manager) {
            super(manager);
        }        
    }
    
    @Singleton
    private static class DefaultSchemeRegistryProvider extends AbstractLazySingletonProvider<SchemeRegistry> {

        protected SchemeRegistry createObject() {
            SchemeRegistry registry = new SchemeRegistry();
            // TODO more injection?
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("tls", SSLSocketFactory.getSocketFactory(),80)); // TODO no ssl?
            registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(),80)); // TODO no ssl?      
            return registry;
        }
    }
    
    @Singleton
    private static class LimeSchemeRegistryProvider extends AbstractLazySingletonProvider<SchemeRegistry> {
        private final Provider<SocketsManager> socketsManager;
        
        @Inject
        public LimeSchemeRegistryProvider(Provider<SocketsManager> socketsManager) {
            this.socketsManager = socketsManager;
        }

        protected SchemeRegistry createObject() {
            SchemeRegistry registry = new SchemeRegistry();
            // TODO more injection?
            registry.register(new Scheme("http", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.PLAIN), 80));
            registry.register(new Scheme("tls", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.TLS),80));
            registry.register(new Scheme("https", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.TLS),80));
            return registry;
        }
    }
    
    @Singleton
    private static class SocketWrappingSchemeRegistryProvider extends AbstractLazySingletonProvider<SchemeRegistry> {

        protected SchemeRegistry createObject() {
            SchemeRegistry registry = new SchemeRegistry();
            // TODO more injection?
            registry.register(new Scheme("http", new SocketWrapperProtocolSocketFactory(), 80));
            registry.register(new Scheme("tls", new SocketWrapperProtocolSocketFactory(),80));
            registry.register(new Scheme("https", new SocketWrapperProtocolSocketFactory(),80));
            return registry;
        }
    }   
    
    
    private abstract static class AbstractClientConnectionManagerProvider extends AbstractLazySingletonProvider<ReapingClientConnectionManager> {
        private final Provider<SchemeRegistry> registry;
        private final Provider<ScheduledExecutorService> scheduler;

        public AbstractClientConnectionManagerProvider(Provider<SchemeRegistry> registry, Provider<ScheduledExecutorService> scheduler) {
            this.registry = registry;
            this.scheduler = scheduler;
        }

        public ReapingClientConnectionManager createObject() {
            return new ReapingClientConnectionManager(registry, scheduler);
        }
    }
    
    @Singleton
    private static class DefaultClientConnectionManagerProvider extends AbstractClientConnectionManagerProvider {

        @Inject
        public DefaultClientConnectionManagerProvider(Provider<SchemeRegistry> registry, @Named("backgroundExecutor") Provider<ScheduledExecutorService> scheduler) {
            super(registry, scheduler);
        }
    }
    
    @Singleton
    private static class LimeClientConnectionManagerProvider extends AbstractClientConnectionManagerProvider {

        @Inject
        public LimeClientConnectionManagerProvider(@Named("limeSchemeRegistry")Provider<SchemeRegistry> registry, @Named("backgroundExecutor") Provider<ScheduledExecutorService> scheduler) {
            super(registry, scheduler);
        }
    }
    
    @Singleton
    private static class SocketWrappingClientConnectionManagerProvider extends AbstractClientConnectionManagerProvider {

        @Inject
        public SocketWrappingClientConnectionManagerProvider(@Named("socketWrappingSchemeRegistry")Provider<SchemeRegistry> registry, @Named("backgroundExecutor") Provider<ScheduledExecutorService> scheduler) {
            super(registry, scheduler);
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
            InetSocketAddress localSocketAddr = null;
            if((localAddress != null && !localAddress.isAnyLocalAddress()) || localPort > 0) {
                localSocketAddr = new InetSocketAddress(localAddress, localPort);
            }
            return socketsManager.get().connect((NBSocket)socket, localSocketAddr, new InetSocketAddress(targetHost,targetPort), HttpConnectionParams.getConnectionTimeout(httpParams), type);
        }

        public boolean isSecure(Socket socket) throws IllegalArgumentException {
            return false; // TODO type.equals(SocketsManager.ConnectType.TLS);  // TODO use socket instead?
        }        
    }
    
// TODO    
//    private static class SecureLimeSocketFactory extends LimeSocketFactory implements LayeredSocketFactory {
//        public SecureLimeSocketFactory(Provider<SocketsManager> socketsManager, SocketsManager.ConnectType type) {
//            super(socketsManager, type);
//        }
//
//        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
//            return socket;
//        }
//    }
}
