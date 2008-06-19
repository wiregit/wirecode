package org.limewire.swarm.http.gnutella;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Test;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.mockup.SimpleHttpRequestHandlerResolver;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExpectationVerifier;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.limewire.http.reactor.LimeConnectingIOReactor;
import org.limewire.net.SocketsManagerImpl;
import org.limewire.nio.NIODispatcher;
import org.limewire.swarm.http.EventListenerAdapter;
import org.limewire.swarm.http.ExecutionHandlerAdapter;
import org.limewire.swarm.http.SourceImpl;
import org.limewire.swarm.http.SwarmExecutionContext;
import org.limewire.swarm.http.SwarmSource;
import org.limewire.swarm.http.Swarmer;
import org.limewire.swarm.http.SwarmerImpl;
import org.limewire.swarm.http.TestHttpServer;
import org.limewire.swarm.http.handler.ExecutionHandler;
import org.limewire.util.BaseTestCase;

public class QueueExecutionHandlerTest extends BaseTestCase {
        
    private TestHttpServer server;
    
    private QueueController queueController;

    public QueueExecutionHandlerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(QueueExecutionHandlerTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        queueController = new QueueControllerImpl(new ScheduledThreadPoolExecutor(1));
        queueController.setMaxQueueCapacity(2);
        
        HttpParams serverParams = new BasicHttpParams();
        serverParams
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
            .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "TEST-SERVER/1.1");

        this.server = new TestHttpServer(serverParams);
        this.server.setExceptionHandler(new IOReactorExceptionHandler() {

            public boolean handle(IOException ex) {
                ex.printStackTrace();
                return false;
            }

            public boolean handle(RuntimeException ex) {
                ex.printStackTrace();
                return false;
            }

        });
    }
    
    @Override
    protected void tearDown() throws Exception {
        this.server.shutdown();
    }
    
    public void testQueueIntegrationTest() throws Exception {
        final AtomicInteger openedConnections = new AtomicInteger(0);
        final AtomicInteger incomingRequests = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(9);
        server.start(createHttpServiceHandler(new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                incomingRequests.incrementAndGet();
                
                Integer count = (Integer)context.getAttribute("COUNT");
                if(count == null)
                    count = 0;
                context.setAttribute("COUNT", count+1);
                if(count == 3) {
                    response.addHeader(new BasicHeader("Connection", "Close"));
                    response.setStatusCode(503);
                } else {
                    response.addHeader(new BasicHeader("X-Queue", "position=0,pollMin=0,pollMax=2"));
                    response.setStatusCode(503);
                }
                
                latch.countDown();
            }
        }, null, new EventListenerAdapter() {
            @Override
            public void connectionOpen(NHttpConnection conn) {
                openedConnections.incrementAndGet();
            }
        }));
        
        final Map<SwarmSource, List<Long>> requestTimes = new ConcurrentHashMap<SwarmSource, List<Long>>();
        ExecutionHandler handler =  new ExecutionHandlerAdapter() {
            @Override
            public HttpRequest submitRequest(HttpContext context) {
                SwarmSource source = (SwarmSource)context.getAttribute(SwarmExecutionContext.HTTP_SWARM_SOURCE);
                List<Long> times = requestTimes.get(source);
                if(times == null) {
                    times = Collections.synchronizedList(new ArrayList<Long>());
                    requestTimes.put(source, times);
                }
                times.add(System.currentTimeMillis());
                HttpRequest request = new BasicHttpRequest("GET", "/a");
                return request;
            }
        };
        
        final Swarmer swarmer = new SwarmerImpl(new QueuableExecutionHandler(handler, queueController),
                new DefaultConnectionReuseStrategy(),
                getIoReactor(getBasicParams()),
                getBasicParams(),
                null);        
        swarmer.start();
        
        server.getListenerEndpoint().waitFor();
        int port = ((InetSocketAddress)server.getListenerEndpoint().getAddress()).getPort();
        swarmer.addSource(new SourceImpl(new InetSocketAddress("localhost", port), null, true), null);
        swarmer.addSource(new SourceImpl(new InetSocketAddress("localhost", port), null, true), null);
        swarmer.addSource(new SourceImpl(new InetSocketAddress("localhost", port), null, true), null);
        
        assertTrue(latch.await(15, TimeUnit.SECONDS));
        assertEquals(9, incomingRequests.get());
        assertEquals(3, openedConnections.get());
        
        // Make sure the requests were scattered over time, with the different connections sending
        // different ones at about the same time.
        assertEquals(3, requestTimes.size());
        Iterator<List<Long>> iterator = requestTimes.values().iterator();
        List<Long> s1 = iterator.next();
        List<Long> s2 = iterator.next();
        List<Long> s3 = iterator.next();
        assertEquals(9, s1.size() + s2.size() + s3.size());
        
        if(s1.size() == 1) {
            assertEquals(4, s2.size());
        } else if(s2.size() == 1 || s3.size() == 1) {
            assertEquals(4, s1.size());
            // Re-arrange just to make things below a little simpler.
            // Assume s1 is the 1, s2 & s3 are the 4.
            List<Long> tmp = s1;
            if(s2.size() == 1) {
                s1 = s2;
                s2 = tmp;
            } else {
                s1 = s3;
                s3 = tmp;
            }
        }
        
        // Validate that the request times are all roughly equal per
        // each request, and that subsequent request times are roughly
        // one second apart.
        assertClose(s1.get(0), s2.get(0), s3.get(0));
        for(int i = 1; i < 4; i++) {
            assertGreaterThan((Long)(s2.get(i-1)+950L), s2.get(i));
            assertLessThan((Long)(s2.get(i-1)+1050L), s2.get(i));
            assertClose(s2.get(i), s3.get(i));
        }
    }
    
    private void assertClose(long... times) {
        long a = times[0];
        for(int i = 1; i < times.length; i++) {
            assertLessThan(a+50, times[i]);
            assertGreaterThan(a-50, times[i]);
        }
    }

    private ConnectingIOReactor getIoReactor(HttpParams params) {
        return new LimeConnectingIOReactor(params, NIODispatcher.instance().getScheduledExecutorService(), new SocketsManagerImpl());
    }

    private HttpParams getBasicParams() {
        HttpParams params = new BasicHttpParams();
        params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
            .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 2000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setParameter(CoreProtocolPNames.USER_AGENT, "LimeTest/1.1");        
        return params;
    }
    
    private NHttpServiceHandler createHttpServiceHandler(
            final HttpRequestHandler requestHandler,
            final HttpExpectationVerifier expectationVerifier,
            final EventListener eventListener) {
        
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());

        BufferingHttpServiceHandler serviceHandler = new BufferingHttpServiceHandler(
                httpproc,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(),
                this.server.getParams());

        serviceHandler.setHandlerResolver(
                new SimpleHttpRequestHandlerResolver(requestHandler));
        serviceHandler.setExpectationVerifier(expectationVerifier);
        serviceHandler.setEventListener(eventListener);
        
        return serviceHandler;
    }
    
}
