package org.limewire.swarm.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Test;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
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
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.limewire.collection.Range;
import org.limewire.http.reactor.LimeConnectingIOReactor;
import org.limewire.net.SocketsManagerImpl;
import org.limewire.nio.NIODispatcher;
import org.limewire.swarm.file.ContiguousSelectionStrategy;
import org.limewire.swarm.file.FileCoordinator;
import org.limewire.swarm.file.FileCoordinatorImpl;
import org.limewire.swarm.file.NoOpFileVerifier;
import org.limewire.swarm.file.SwarmFile;
import org.limewire.swarm.file.SwarmFileCompletionListener;
import org.limewire.swarm.http.handler.SwarmFileExecutionHandler;
import org.limewire.util.BaseTestCase;

public class SwarmFileExecutionHandlerTest extends BaseTestCase {

    private TestHttpServer server;

    public SwarmFileExecutionHandlerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SwarmFileExecutionHandlerTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
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
    
    public void testSimpleSingleSourceSwarm() throws Exception {
        final ByteBuffer wholeFile = ByteBuffer.allocate(26 * 1024);
        for(int i = 0; i < wholeFile.capacity(); i++) {
            wholeFile.put((byte)i);
        }
        
        final AtomicInteger openedConnections = new AtomicInteger(0);
        final AtomicInteger incomingRequests = new AtomicInteger(0);
        server.start(createHttpServiceHandler(new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                incomingRequests.incrementAndGet();
                if(request.getRequestLine().getUri().equals("/get/a")) {
                    String rangeRequestHeader = request.getFirstHeader("Range").getValue();
                    Range range = SwarmHttpUtils.rangeForRequest(rangeRequestHeader);
                    response.addHeader("Content-Range", rangeRequestHeader + "/*");
                    response.setStatusCode(206);
                    ByteBuffer duplicate = wholeFile.duplicate();
                    duplicate.position((int)range.getLow());
                    duplicate.limit((int)range.getHigh()+1);
                    response.setEntity(new ByteBufferEntity(duplicate));
                } else {
                    response.setStatusCode(404);
                }
            }
            
        }, null, new EventListenerAdapter() {
            @Override
            public void connectionOpen(NHttpConnection conn) {
                openedConnections.incrementAndGet();
            }
        }));
        
        HttpParams params = getBasicParams();
        ConnectingIOReactor ioReactor = getIoReactor(params);
        int size = 26 * 1024;
        ByteBufferSwarmFile swarmFile = new ByteBufferSwarmFile(size);
        FileCoordinator fileCoordinator = new FileCoordinatorImpl(size, swarmFile,
                new NoOpFileVerifier(), Executors.newSingleThreadExecutor(),
                new ContiguousSelectionStrategy(size), 
                1024);
        SwarmFileExecutionHandler swarmFileExecutionHandler = new SwarmFileExecutionHandler(fileCoordinator);
        
        final Swarmer swarmer = new SwarmerImpl(swarmFileExecutionHandler,
                new DefaultConnectionReuseStrategy(),
                ioReactor,
                params,
                null);
        
        swarmer.start();
        
        server.getListenerEndpoint().waitFor();
        int port = ((InetSocketAddress)server.getListenerEndpoint().getAddress()).getPort();
        swarmer.addSource(new SourceImpl(new InetSocketAddress("localhost", port), "/get/a", true), null);
        
        final CountDownLatch latch = new CountDownLatch(1);
        fileCoordinator.addCompletionListener(new SwarmFileCompletionListener() {
            public void fileCompleted(FileCoordinator fileCoordinator, SwarmFile swarmFile) {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(wholeFile.array(), swarmFile.getBuffer().array());
        assertEquals(0, fileCoordinator.getAmountVerified());
        assertEquals(1, openedConnections.get());
        assertEquals(26, incomingRequests.get());
    }
    
    public void testSimpleDoubleSourceSwarm() throws Exception {
        final ByteBuffer wholeFile = ByteBuffer.allocate(26 * 1024);
        for(int i = 0; i < wholeFile.capacity(); i++) {
            wholeFile.put((byte)i);
        }
        
        final AtomicInteger openedConnections = new AtomicInteger(0);
        final AtomicInteger incomingRequests = new AtomicInteger(0);
        server.start(createHttpServiceHandler(new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                incomingRequests.incrementAndGet();
                if(request.getRequestLine().getUri().equals("/get/a")) {
                    String rangeRequestHeader = request.getFirstHeader("Range").getValue();
                    Range range = SwarmHttpUtils.rangeForRequest(rangeRequestHeader);
                    response.addHeader("Content-Range", rangeRequestHeader + "/*");
                    response.setStatusCode(206);
                    ByteBuffer duplicate = wholeFile.duplicate();
                    duplicate.position((int)range.getLow());
                    duplicate.limit((int)range.getHigh()+1);
                    response.setEntity(new ByteBufferEntity(duplicate));
                } else {
                    response.setStatusCode(404);
                }
            }
            
        }, null, new EventListenerAdapter() {
            @Override
            public void connectionOpen(NHttpConnection conn) {
                openedConnections.incrementAndGet();
            }
        }));
        
        HttpParams params = getBasicParams();
        ConnectingIOReactor ioReactor = getIoReactor(params);
        int size = 26 * 1024;
        ByteBufferSwarmFile swarmFile = new ByteBufferSwarmFile(size);
        FileCoordinator fileCoordinator = new FileCoordinatorImpl(size, swarmFile,
                new NoOpFileVerifier(), Executors.newSingleThreadExecutor(),
                new ContiguousSelectionStrategy(size), 
                1024);
        SwarmFileExecutionHandler swarmFileExecutionHandler = new SwarmFileExecutionHandler(fileCoordinator);
        
        final Swarmer swarmer = new SwarmerImpl(swarmFileExecutionHandler,
                new DefaultConnectionReuseStrategy(),
                ioReactor,
                params,
                null);
        
        swarmer.start();
        
        server.getListenerEndpoint().waitFor();
        int port = ((InetSocketAddress)server.getListenerEndpoint().getAddress()).getPort();
        
        final AtomicInteger responsesOne = new AtomicInteger(0);
        final AtomicInteger responsesTwo = new AtomicInteger(0);
        swarmer.addSource(new SourceImpl(new InetSocketAddress("localhost", port), "/get/a", true), new SourceEventListenerAdapter() {
            @Override
            public void responseProcessed(Swarmer swarmer, SwarmSource source, int statusCode) {
                responsesOne.incrementAndGet();
            }
        });
        swarmer.addSource(new SourceImpl(new InetSocketAddress("localhost", port), "/get/a", true), new SourceEventListenerAdapter() {
            @Override
            public void responseProcessed(Swarmer swarmer, SwarmSource source, int statusCode) {
                responsesTwo.incrementAndGet();
            }
        });
        final CountDownLatch latch = new CountDownLatch(1);
        fileCoordinator.addCompletionListener(new SwarmFileCompletionListener() {
            public void fileCompleted(FileCoordinator fileCoordinator, SwarmFile swarmFile) {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(wholeFile.array(), swarmFile.getBuffer().array());
        assertEquals(0, fileCoordinator.getAmountVerified());
        assertEquals(2, openedConnections.get());
        assertEquals(26, incomingRequests.get());
        
        // Make sure they split it up about equally.
        assertGreaterThan(10, responsesOne.get());
        assertLessThan(16, responsesOne.get());
        assertEquals(26-responsesOne.get(), responsesTwo.get());
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
        httpproc.addInterceptor(new ResponseConnControl());

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
