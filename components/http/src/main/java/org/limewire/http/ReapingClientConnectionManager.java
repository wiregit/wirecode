package org.limewire.http;

import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.HttpRoute;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.Scheme;
import org.apache.http.conn.SchemeRegistry;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.limewire.service.ErrorService;

import com.google.inject.Provider;

public class ReapingClientConnectionManager extends ThreadSafeClientConnManager {
    protected final ScheduledFuture connectionCloserTask;
    protected IdleConnectionCloser connectionCloser;

    public ReapingClientConnectionManager(Provider<SchemeRegistry> schemeRegistry, Provider<ScheduledExecutorService> scheduler) {
        super(new DefaultHttpParams(), schemeRegistry.get());
        connectionCloser = new IdleConnectionCloser();
        connectionCloserTask = scheduler.get().scheduleAtFixedRate(connectionCloser, 0L, 10L, TimeUnit.SECONDS);
    }

    public ManagedClientConnection getConnection(HttpRoute httpRoute) throws InterruptedException {
        connectionCloser.setManagerOnce(this);
        return super.getConnection(httpRoute);
    }

    public ManagedClientConnection getConnection(HttpRoute httpRoute, long l) throws ConnectionPoolTimeoutException, InterruptedException {
        connectionCloser.setManagerOnce(this);
        return super.getConnection(httpRoute, l);
    }
    
    public void shutdown() {
        connectionCloserTask.cancel(true);
        super.shutdown();
    }
    
    void setSocket(Socket s) {
        SchemeRegistry registry = getSchemeRegistry();
        for (Object o : registry.getSchemeNames()) {
            String name = (String) o;
            Scheme scheme = registry.getScheme(name);
            ((SocketWrapperProtocolSocketFactory) scheme.getSocketFactory()).setSocket(s);
        }
    }
    
    static class IdleConnectionCloser implements Runnable {

        private static final long IDLE_TIME = 30 * 1000; // 30 seconds.
    
        private final AtomicReference<ClientConnectionManager> managerHolder;
    
        IdleConnectionCloser(){
            managerHolder = new AtomicReference<ClientConnectionManager>();
        }
        
        void setManagerOnce(ClientConnectionManager manager) {
            managerHolder.compareAndSet(null, manager);
        }
        
        public void run() {
            try {
                ClientConnectionManager manager = managerHolder.get();
                if(manager != null) {
                    manager.closeIdleConnections(IDLE_TIME);
                }
            } catch (Throwable t) {
                ErrorService.error(t);
            }
        }
    }
}
