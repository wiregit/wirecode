package org.limewire.nio.ssl;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class SSLNIOSocketTest extends BaseTestCase {
    
    public SSLNIOSocketTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SSLNIOSocketTest.class);
    }
    
    public void testConnectToSSLSiteBlocking() throws Exception {
        SSLNIOSocket socket = new SSLNIOSocket();
        socket.connect(new InetSocketAddress("www.limewire.org", 443));
        
        OutputStream out = socket.getOutputStream();
        out.write("GET /sam/ssltest.txt HTTP 1.0\r\n\r\n".getBytes());
        out.flush();
        
        InputStream in = socket.getInputStream();
        byte[] buffer = new byte[10240];
        int totalRead = 0;
        int read;
        while((read = in.read(buffer)) > 0) {
            totalRead += read;
        }
        String data = new String(buffer, 0, totalRead);
        assertTrue(data, data.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(data, data.contains("Content-Length: 9\r\n"));
        assertTrue(data, data.contains("Content-Type: text/plain; charset=UTF-8"));
        assertTrue(data, data.endsWith("\r\n\r\nSSL TEST\n"));
    }
    
}
