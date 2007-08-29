package org.limewire.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.Test;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.http.HttpStatus;
import org.limewire.concurrent.Providers;
import org.limewire.http.handler.BasicMimeTypeProvider;
import org.limewire.http.handler.FileRequestHandler;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.ConnectionDispatcherImpl;
import org.limewire.net.SocketAcceptor;
import org.limewire.util.BaseTestCase;

public class BasicHttpAcceptorTest extends BaseTestCase {

    private static final int PORT = 6668;

    private static final int TIMEOUT = 1000;

    private HttpClient client;

    private HostConfiguration hostConfig;

    private BasicHttpAcceptor httpAcceptor;

    private SocketAcceptor acceptor;

    private ConnectionDispatcher connectionDispatcher;

    public BasicHttpAcceptorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BasicHttpAcceptorTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        client = new HttpClient();
        hostConfig = new HostConfiguration();
        hostConfig.setHost("localhost", PORT);
        client.setHostConfiguration(hostConfig);
    }

    @Override
    protected void tearDown() throws Exception {
        stopAcceptor();
        
        client.getHttpConnectionManager().getConnection(hostConfig).close();
    }

    private void initializeAcceptor(int timeout, String... methods) throws Exception {
        acceptor = new SocketAcceptor(new ConnectionDispatcherImpl());
        acceptor.bind(PORT);

        connectionDispatcher = new ConnectionDispatcherImpl();
        httpAcceptor = new BasicHttpAcceptor(Providers.of(connectionDispatcher), true, BasicHttpAcceptor
                .createDefaultParams("agent", timeout), methods);
        httpAcceptor.start();
    }
    
    private void stopAcceptor() throws Exception {
        if (httpAcceptor != null) {
            httpAcceptor.stop();
            httpAcceptor = null;
        }
        if (acceptor != null) {
            acceptor.unbind();
            acceptor = null;
        }
        // FIXME wait for NIO?
    }
    
    public void testWatchdogTriggeredTimeout() throws IOException, Exception {
        initializeAcceptor(TIMEOUT, "GET");
        
        File file = File.createTempFile("lime", null);
        byte[] data = new byte[1 * 1000 * 1000];
        Arrays.fill(data, (byte) 'a');
        HttpTestUtils.writeData(file, data);
        
        FileRequestHandler handler = new FileRequestHandler(file.getParentFile(), new BasicMimeTypeProvider());
        handler.setTimeout(100);
        httpAcceptor.registerHandler("*", handler);
        
        GetMethod method = new GetMethod("/" + file.getName());
        try {
            int result = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, result);
            InputStream in = method.getResponseBodyAsStream();
            assertEquals('a', in.read());
            Thread.sleep(200);
            int i = 1;
            while (in.read() != -1) {
                i++;
            }
            assertFalse(client.getHttpConnectionManager().getConnection(hostConfig).isOpen());
            assertLessThan("Expected connection close", data.length, i);
        } finally {
            method.releaseConnection();
        }
    }
    
    public void testWatchdogDoesNotTimeout() throws Exception {
        initializeAcceptor(TIMEOUT, "GET");
        
        File file = File.createTempFile("lime", null);
        byte[] data = new byte[1 * 1000 * 1000];
        Arrays.fill(data, (byte) 'a');
        HttpTestUtils.writeData(file, data);
        
        FileRequestHandler handler = new FileRequestHandler(file.getParentFile(), new BasicMimeTypeProvider());
        handler.setTimeout(100);
        httpAcceptor.registerHandler("*", handler);

        // check that it doesn't timeout
        GetMethod method = new GetMethod("/" + file.getName());
        try {
            int result = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, result);
            InputStream in = method.getResponseBodyAsStream();
            assertEquals('a', in.read());
            int i = 1;
            byte[] buffer = new byte[1024];
            int l;
            while ((l = in.read(buffer)) != -1) {
                i += l;
            }
            assertEquals(data.length, i);
        } finally {
            method.releaseConnection();
        }        
    }
    
    public void testInvalidMethod() throws IOException, Exception {
        initializeAcceptor(TIMEOUT, "GET");
        
        HeadMethod method = new HeadMethod("/");
        try {
            int result = client.executeMethod(method);
            fail("Expected IOException, got: " + result);
        } catch (IOException expected) {
        } finally {
            method.releaseConnection();
        }
    }

    public void testDefaultHandler() throws IOException, Exception {
        initializeAcceptor(TIMEOUT, "HEAD");
        
        HeadMethod method = new HeadMethod("/");
        try {
            int result = client.executeMethod(method);
            assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, result);
        } finally {
            method.releaseConnection();
        }
    }

}
