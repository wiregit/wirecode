package com.limegroup.gnutella.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.service.ErrorService;
import org.limewire.util.AssertComparisons;

import com.limegroup.gnutella.ByteReader;

// extends AssertCmparisons just to get useful methods.
public class FakeProxyServer extends AssertComparisons {

    /**
     * The server which we tell limewire is the proxy
     */ 
    private ServerSocket _proxyServer;

    /** 
     * The socket limewire wants to connect to
     */ 
    private ServerSocket _destinationServer;

    private boolean _proxyOn;

    private int _proxyVersion;
    
    private boolean _authentication;

    private boolean _makeError = false;
    
    private boolean _isHTTPRequest = false;
    
    final static String USER = "Sumeet";
    
    final static String PASS = "Thadani";
    

    public FakeProxyServer(int proxyPort, int destinationPort) {
        super("fake");
        _proxyOn = false;
        _proxyVersion = ProxyTest.NONE;
        _authentication = false;
        startServers(proxyPort, destinationPort);        
    }

    private void startServers(int proxyPort, int destPort) {
        try {
            _proxyServer = new ServerSocket();
            _proxyServer.bind(new InetSocketAddress(proxyPort));
            _destinationServer = new ServerSocket();
            _destinationServer.bind(new InetSocketAddress(destPort));
        } catch(IOException iox) {
            ErrorService.error(iox);
        }

        Thread proxyThread = ThreadExecutor.newManagedThread(new Runnable() {
            public void run() {
                proxyLoop();
            }
        });
        proxyThread.setDaemon(true);
        proxyThread.start();
        
        Thread destThread = ThreadExecutor.newManagedThread(new Runnable() {
            public void run() {
                destLoop();
            }
        });
        destThread.setDaemon(true);
        destThread.start();
    }
    
    private void proxyLoop() {
        try {
            while(true) {
                Socket incomingProxy = null;
                incomingProxy = _proxyServer.accept();
                if(!_proxyOn)
                    fail("LimeWire connected to proxy server instead of directly");
                InputStream is = incomingProxy.getInputStream();
                OutputStream os = incomingProxy.getOutputStream();
                if(_proxyVersion == ProxyTest.SOCKS4)
                    checkSOCKS4(is, os);
                else if(_proxyVersion == ProxyTest.SOCKS5)
                    checkSOCKS5(is, os);
                else if(_proxyVersion == ProxyTest.HTTP)
                    checkHTTP(is, os);
                else
                   assertTrue("test not set up correctly, incorrect proxy version",
                           _isHTTPRequest);
                int a = 0;
                if(_isHTTPRequest) {
                    consumeHttpHeaders(is);
                    writeHTTPBack(os);
                    try {
                        Thread.sleep(1000); 
                    } catch (InterruptedException x) { }
                }
                else {
                    //os.write('x');
                    while(a != -1) 
                        a = is.read();
                }
                if(!incomingProxy.isClosed())
                    incomingProxy.close();
            }
        } catch(IOException iox) {
            ErrorService.error(iox);
        }
    }

    private void checkSOCKS4(InputStream is, OutputStream os) 
                                                       throws IOException {
        byte currByte = (byte)is.read();
        assertEquals("Wrong version sent by LW to proxy", 4, currByte);
        assertEquals( "connect command not sent", 1, is.read());
        //TODO: make sure port is correct
        is.read();
        is.read();
        //check IP
        assertEquals("0th byte of ip wrong", 127, is.read());
        assertEquals("1st byte of ip wrong", 0, is.read());
        assertEquals("2nd byte of ip wrong", 0, is.read());
        assertEquals("3rd byte of ip wrong", 1, is.read());

        if(_authentication) {
            byte[] u = new byte[USER.length()];
            is.read(u);
            assertEquals("LW sent wrong user", USER, new String(u));
        }
        assertEquals("LW did not send terminating 0", 0, is.read());
        os.write((byte)4);//send version

        if(_makeError)
            os.write(0x33);//write wrong status code
        else
            os.write(0x5A);//write correct status code
        //write out random ip port 
        byte[] ip = {(byte)1,(byte)1,(byte)1,(byte)1,(byte)1, (byte)1};
        os.write(ip);
        os.flush();
    }
    
    private void checkSOCKS5(InputStream is, OutputStream os) 
                                                           throws IOException {
        byte currByte = (byte)is.read();
        assertEquals("Wrong version sent by LW to proxy", 5, currByte);
        currByte = (byte)is.read();
        if(_authentication)
            assertEquals("should support 2 auth methods", 2, currByte);
        else
            assertEquals("should support 1 auth method", 1, currByte);
        currByte = (byte)is.read();
        assertEquals("we always support no auth", 0, currByte);
        if(_authentication) {
            currByte = (byte)is.read();
            assertEquals("should support user/passwd", 2, currByte);
        }
        
        os.write((byte)5);//confirm that we are supporting version 5
        if(_authentication)
            os.write((byte)2);
        else
            os.write((byte)0);
        if(_authentication) {//do all the checking
            assertEquals("wrong auth version", 1, is.read());
            assertEquals("wrong user len", USER.length(), is.read());
            byte[] u = new byte[USER.length()];
            is.read(u);
            assertEquals("Wrong user sent", USER, new String(u));
            assertEquals("wrong pass len", PASS.length(), is.read());
            byte[] p = new byte[PASS.length()];
            is.read(p);
            assertEquals("Wrong pass sent", PASS, new String(p));
            os.write((byte)1);//send version
            os.write((byte)0); //send success.
        }
        assertEquals("no version sent at end", 5, is.read());
        assertEquals("no connect command sent", 1, is.read()); 
        assertEquals("no reserved byte sent", 0, is.read());
        assertEquals("IPv4 marker not sent", 1, is.read()); 

        assertEquals("wrong 0th ip byte", 127, is.read());
        assertEquals("wrong 1st ip byte", 0, is.read());
        assertEquals("wrong 2nd ip byte", 0, is.read());
        assertEquals("wrong 3rd ip byte", 1, is.read());
        //TODO2: check if port bytes are correct
        is.read();
        is.read();
        
        os.write((byte)5);//write version again
        if(_makeError)
            os.write((byte)8); //make error send bad status
        else
            os.write((byte)0);//status
        os.write((byte)1);//write reserved byte
        os.write((byte)1);//ip v4
        byte[] ip = {(byte)1,(byte)1,(byte)1,(byte)1,(byte)1, (byte)1};
        os.write(ip);
        os.flush();
    }

    
    private void checkHTTP(InputStream is, OutputStream os) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = reader.readLine();
        StringTokenizer tok = new StringTokenizer(line, " :");
        assertEquals("connect string not sent", "CONNECT", tok.nextToken());
        assertEquals("LW sent wrong host", "127.0.0.1", tok.nextToken());
        assertEquals("LW sent wrong port", "" + ProxyTest.DEST_PORT, tok.nextToken());
        assertEquals("LW didn't send http string", "HTTP/1.0", tok.nextToken());
        if(_makeError)
            os.write("503 Busy\r\n\r\n".getBytes());
        else
            os.write("200 OK\r\n\r\n".getBytes());
        
    }

    private void destLoop() {
        try {
            while(true) {
                Socket incomingDest = null;
                incomingDest = _destinationServer.accept();
                if(_proxyOn)
                    fail("Limewire connected to desination instead of proxy");
                if(_isHTTPRequest) {
                    writeHTTPBack(incomingDest.getOutputStream());
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException e) {}
                }
                if(!incomingDest.isClosed())
                    incomingDest.close();
            }
        } catch(IOException iox) {
            ErrorService.error(iox);
        }
    }


    private void writeHTTPBack(OutputStream os) throws IOException {        
        os.write("HTTP/1.1 200 OK\r\n".getBytes());
        os.write("Server: limewire \r\n".getBytes());
        os.write("Content-Type: txt/html \r\n".getBytes());
        os.write("Content-Length: 5 \r\n".getBytes());
        os.write("\r\n".getBytes());
        os.write("hello".getBytes());
        os.flush();
    }
    
    private void consumeHttpHeaders(InputStream is) throws IOException {
        ByteReader reader = new ByteReader(is);
        String line = " ";
        while(!line.equals("")) {      
            line = reader.readLine();  
            //System.out.println("\t\t"+line);
        }
    }


    void killServers() {
        try {
            if(_proxyServer != null)
                _proxyServer.close();
            if(_destinationServer != null)
                _destinationServer.close();
        } catch(IOException iox) {}
    }

    void setProxyOn(boolean proxyOn) {
        _proxyOn = proxyOn;
    }

    void setAuthentication(boolean auth) {
        _authentication = auth;
    }

    void setProxyVersion(int ver) {
        _proxyVersion = ver;
    }
    
    void setMakeError(boolean b) {
        _makeError = b;
    }

    void setHttpRequest(boolean b) {
        _isHTTPRequest = b;
    }
}
