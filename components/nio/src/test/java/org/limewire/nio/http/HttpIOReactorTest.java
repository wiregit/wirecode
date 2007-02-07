package org.limewire.nio.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import junit.framework.Test;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.impl.params.DefaultHttpParams;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExecutionContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class HttpIOReactorTest extends BaseTestCase {

    private static final int ACCEPTOR_PORT = 9999;
    private static Acceptor acceptThread;
    
    public HttpIOReactorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HttpIOReactorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        new RouterService(new ActivityCallbackStub());
        
        acceptThread = new Acceptor();
        acceptThread.start();
        acceptThread.setListeningPort(ACCEPTOR_PORT);
        
        // Give thread time to find and open it's sockets.   This race condition
        // is tolerated in order to speed up LimeWire startup
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {}                
    }

    public static void globealTearDown() throws Exception {
        acceptThread.setListeningPort(0);
    }

    public void testGet() throws Exception {
        HttpParams params = new DefaultHttpParams();
        params
            .setIntParameter(HttpConnectionParams.SO_TIMEOUT, 5000)
            .setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 10000)
            .setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK, false)
            .setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true)
            .setParameter(HttpProtocolParams.USER_AGENT, "LimeWire");

        ConnectingIOReactor ioReactor = new HttpIOReactor(params);
        MyNHttpClientHandler handler = new MyNHttpClientHandler(params);
        IOEventDispatch ioEventDispatch = new DefaultClientIOEventDispatch(handler, params);
        ioReactor.execute(ioEventDispatch);
        
        SessionRequest request = ioReactor.connect(
                new InetSocketAddress("www.limewire.com", 80), 
                null, 
                new HttpHost("www.limewire.com"));
        
        synchronized (handler) {
            handler.wait();
        }
        if (handler.exception != null) {
            fail(handler.exception);
        }
    }
    
    static class MyNHttpClientHandler implements NHttpClientHandler {

        private final HttpParams params;
        private final HttpRequestFactory requestFactory; 
        private final HttpProcessor httpProcessor;
        private final ByteBuffer inbuf;
        private final ConnectionReuseStrategy connStrategy;
        
        private Throwable exception;
        
        public MyNHttpClientHandler(final HttpParams params) {
            super();
            this.params = params;
            this.requestFactory = new DefaultHttpRequestFactory();
            BasicHttpProcessor httpproc = new BasicHttpProcessor();
            httpproc.addInterceptor(new RequestContent());
            httpproc.addInterceptor(new RequestTargetHost());
            httpproc.addInterceptor(new RequestConnControl());
            httpproc.addInterceptor(new RequestUserAgent());
            httpproc.addInterceptor(new RequestExpectContinue());
            this.httpProcessor = httpproc;
            this.inbuf = ByteBuffer.allocateDirect(2048);
            this.connStrategy = new DefaultConnectionReuseStrategy();
        }
        
//        public void waitFor(SessionRequest request) throws InterruptedException {
//            synchronized (request) {
//                request.wait();                
//            }
//        }

        private synchronized void shutdownConnection(final HttpConnection conn) {
            try {
                conn.shutdown();
            } catch (IOException ignore) {
            }
            notify();
        }
        
        public void connected(final NHttpClientConnection conn, final Object attachment) {
            try {
                HttpContext context = conn.getContext();
                
                HttpHost targetHost = (HttpHost) attachment;
                
                context.setAttribute(HttpExecutionContext.HTTP_CONNECTION, conn);
                context.setAttribute(HttpExecutionContext.HTTP_TARGET_HOST, targetHost);
                
                HttpRequest request = this.requestFactory.newHttpRequest("GET", "/");
                request.getParams().setDefaults(this.params);
                
                this.httpProcessor.process(request, context);
                
                conn.submitRequest(request);

                context.setAttribute(HttpExecutionContext.HTTP_REQUEST, request);
            
            } catch (IOException ex) {
                shutdownConnection(conn);
                System.err.println("I/O error: " + ex.getMessage());
            } catch (HttpException ex) {
                shutdownConnection(conn);
                System.err.println("Unexpected HTTP protocol error: " + ex.getMessage());
            }
        }

        public synchronized void closed(final NHttpClientConnection conn) {
            this.notify();
        }

        public void exception(final NHttpClientConnection conn, final HttpException ex) {
            this.exception = ex;
            shutdownConnection(conn);
        }

        public void exception(final NHttpClientConnection conn, final IOException ex) {
            this.exception =  ex;
            shutdownConnection(conn);
        }

        public void inputReady(final NHttpClientConnection conn, final ContentDecoder decoder) {
            HttpResponse response = conn.getHttpResponse();
            HttpContext context = conn.getContext();
            HttpHost targetHost = (HttpHost) context
                .getAttribute(HttpExecutionContext.HTTP_TARGET_HOST);
            WritableByteChannel channel = (WritableByteChannel) context
                .getAttribute("in-channel");

            try {
                while (decoder.read(this.inbuf) > 0) {
                    this.inbuf.flip();
                    channel.write(this.inbuf);
                    this.inbuf.compact();
                }
                if (decoder.isCompleted()) {
                    HttpEntity entity = response.getEntity();
                    
                    ByteArrayOutputStream bytestream = (ByteArrayOutputStream) context
                        .getAttribute("in-buffer");
                    byte[] content = bytestream.toByteArray();
                    
                    String charset = EntityUtils.getContentCharSet(entity);
                    if (charset == null) {
                        charset = HTTP.DEFAULT_CONTENT_CHARSET;
                    }
                    
                    System.out.println("--------------");
                    System.out.println("Target: " + targetHost);
                    System.out.println("--------------");
                    System.out.println(response.getStatusLine());
                    System.out.println("--------------");
                    System.out.println(new String(content, charset));
                    System.out.println("--------------");

                    if (!this.connStrategy.keepAlive(response, context)) {
                        conn.close();
                    }
                }
                
            } catch (IOException ex) {
                shutdownConnection(conn);
                System.err.println("I/O error: " + ex.getMessage());
            }
        }

        public void outputReady(final NHttpClientConnection conn, final ContentEncoder encoder) {
        }

        public void responseReceived(final NHttpClientConnection conn) {
            HttpResponse response = conn.getHttpResponse();
            
            if (response.getStatusLine().getStatusCode() >= 200) {
                ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
                WritableByteChannel channel = Channels.newChannel(bytestream);
                
                HttpContext context = conn.getContext();
                context.setAttribute("in-buffer", bytestream);
                context.setAttribute("in-channel", channel);
            }
        }

        public void timeout(final NHttpClientConnection conn) {
            System.err.println("Timeout");
            shutdownConnection(conn);
        }

        public void requestReady(NHttpClientConnection conn) {
            // TODO Auto-generated method stub
            
        }
        
    } 
   
}
