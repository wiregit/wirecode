package org.limewire.http;

import java.nio.ByteBuffer;

import junit.framework.Test;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpProcessor;
import org.limewire.util.BaseTestCase;

public class HttpChannelTest extends BaseTestCase {

    private HttpParams parms;

    private ConnectionReuseStrategy connStrategy;

    private HttpResponseFactory responseFactory;

    private HttpProcessor httpProcessor;

    private HttpServiceHandler serviceHandler;

    private DefaultServerIOEventDispatch eventDispatch;

    public HttpChannelTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HttpChannelTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        parms = new BasicHttpParams();
        connStrategy = new DefaultConnectionReuseStrategy();
        responseFactory = new DefaultHttpResponseFactory();
        httpProcessor = new BasicHttpProcessor();
        serviceHandler = new HttpServiceHandler(httpProcessor, responseFactory,
                connStrategy, parms);
        eventDispatch = new DefaultServerIOEventDispatch(serviceHandler, parms);
    }

    public void testPushBackReadAllAtOnce() throws Exception {
        MockSocket socket = new MockSocket();
        HttpIOSession session = new HttpIOSession(socket);
        MockChannel srcChannel = new MockChannel("abc");

        HttpChannel channel = new HttpChannel(session, eventDispatch, "GET");
        channel.setReadChannel(srcChannel);
        ByteBuffer dst = ByteBuffer.allocate(8);
        int read = channel.read(dst);
        assertEquals(6, read);
        assertEquals(6, dst.position());
        assertEquals("GETabc", new String(dst.array(), 0, 6));

        read = channel.read(dst);
        assertEquals(0, read);
        assertEquals(6, dst.position());
    }

    public void testPushBackReadSlowly() throws Exception {
        MockSocket socket = new MockSocket();
        HttpIOSession session = new HttpIOSession(socket);
        MockChannel srcChannel = new MockChannel("abc");

        HttpChannel channel = new HttpChannel(session, eventDispatch, "GET");
        channel.setReadChannel(srcChannel);
        ByteBuffer dst = ByteBuffer.allocate(2);
        int read = channel.read(dst);
        assertEquals(2, read);
        dst.flip();
        assertEquals("GE", new String(dst.array(), 0, 2));

        dst.clear();
        read = channel.read(dst);
        assertEquals(2, read);
        assertEquals("Ta", new String(dst.array(), 0, 2));

        dst.clear();
        read = channel.read(dst);
        assertEquals(2, read);
        assertEquals("bc", new String(dst.array(), 0, 2));

        dst.clear();
        read = channel.read(dst);
        assertEquals(0, read);
        assertEquals(0, dst.position());
    }

    public void testNoPushBack() throws Exception {
        MockSocket socket = new MockSocket();
        HttpIOSession session = new HttpIOSession(socket);
        MockChannel srcChannel = new MockChannel("abc");

        HttpChannel channel = new HttpChannel(session, eventDispatch, null);
        channel.setReadChannel(srcChannel);
        ByteBuffer dst = ByteBuffer.allocate(4);
        int read = channel.read(dst);
        assertEquals(3, read);
        dst.flip();
        assertEquals("abc", new String(dst.array(), 0, 3));
    }

}
