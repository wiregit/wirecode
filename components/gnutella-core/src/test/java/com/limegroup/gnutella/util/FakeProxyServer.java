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

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.ErrorService;

public class FakeProxyServer {

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

        Thread proxyThread = new ManagedThread() {
            public void managedRun() {
                proxyLoop();
            }
        };
        proxyThread.setDaemon(true);
        proxyThread.start();
        
        Thread destThread = new ManagedThread() {
            public void managedRun() {
                destLoop();
            }            
        };
        destThread.setDaemon(true);
        destThread.start();
    }
    
    private void proxyLoop() {
        try {
            while(true) {
                Socket incomingProxy = null;
                incomingProxy = _proxyServer.accept();
                if(!_proxyOn)
                    Assert.that(false,
                      "LimeWire connected to proxy server instead of directly");
                InputStream is = incomingProxy.getInputStream();
                OutputStream os = incomingProxy.getOutputStream();
                if(_proxyVersion == ProxyTest.SOCKS4)
                    checkSOCKS4(is, os);
                else if(_proxyVersion == ProxyTest.SOCKS5)
                    checkSOCKS5(is, os);
                else if(_proxyVersion == ProxyTest.HTTP)
                    checkHTTP(is, os);
                else
                    Assert.that(_isHTTPRequest, 
                         "test not set up correctly, incorrect proxy version");
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
        Assert.that((byte)4==currByte, "Wrong version sent by LW to proxy");
        Assert.that((byte)is.read()==(byte)1, "connect command not sent");
        //TODO: make sure port is correct
        is.read();
        is.read();
        //check IP
        Assert.that((byte)is.read()==(byte)127,"0th byte of ip wrong");
        Assert.that((byte)is.read()==(byte)0,"1st byte of ip wrong");
        Assert.that((byte)is.read()==(byte)0,"2nd byte of ip wrong");
        Assert.that((byte)is.read()==(byte)1,"3rd byte of ip wrong");

        if(_authentication) {
            byte[] u = new byte[USER.length()];
            is.read(u);
            Assert.that(USER.equals(new String(u)),"LW sent wrong user");
        }
        Assert.that((byte)is.read()==(byte)0,"LW did not send terminating 0");
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
        Assert.that((byte)5==currByte,"Wrong version sent by LW to proxy");
        currByte = (byte)is.read();
        if(_authentication)
            Assert.that(currByte == (byte)2, "should support 2 auth methods");
        else
            Assert.that(currByte == (byte)1, "should support 1 auth method");
        currByte = (byte)is.read();
        Assert.that(currByte == (byte)0, "we always support no auth");
        if(_authentication) {
            currByte = (byte)is.read();
            Assert.that(currByte == (byte)2, "should support user/passwd");
        }
        
        os.write((byte)5);//confirm that we are supporting version 5
        if(_authentication)
            os.write((byte)2);
        else
            os.write((byte)0);
        if(_authentication) {//do all the checking
            Assert.that((byte)is.read() == (byte)1,"wrong auth version");
            Assert.that((byte)is.read() == USER.length(), "wrong user len");
            byte[] u = new byte[USER.length()];
            is.read(u);
            Assert.that(USER.equals(new String(u)), "Wrong user sent");
            Assert.that((byte)is.read() == PASS.length(), "wrong pass len");
            byte[] p = new byte[PASS.length()];
            is.read(p);
            Assert.that(PASS.equals(new String(p)), "Wrong pass sent");
            os.write((byte)1);//send version
            os.write((byte)0); //send success.
        }
        Assert.that((byte)is.read() == (byte)5, "no version sent at end");
        Assert.that((byte)is.read() == (byte)1, "no connect command sent");
        Assert.that((byte)is.read() == (byte)0, "no reserved byte sent");
        Assert.that((byte)is.read() == (byte)1, "IPv4 marker not sent");

        Assert.that((byte)is.read() == (byte)127, "wrong 0th ip byte");
        Assert.that((byte)is.read() == (byte)0, "wrong 1st ip byte");
        Assert.that((byte)is.read() == (byte)0, "wrong 2nd ip byte");
        Assert.that((byte)is.read() == (byte)1, "wrong 3rd ip byte");
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
        Assert.that(tok.nextToken().equals("CONNECT"),
                                                  "connect string not sent");
        Assert.that(tok.nextToken().equals("localhost"), "LW sent wrong host");
        Assert.that(tok.nextToken().equals(""+ProxyTest.DEST_PORT),
                                                     "LW sent wrong port");
        Assert.that(tok.nextToken().equals("HTTP/1.0"), 
                                                 "LW didn't send http string");
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
                    Assert.that(false, 
                           "Limewire connected to desination instead of proxy");
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
