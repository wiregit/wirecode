package com.limegroup.gnutella.util;

import java.net.*;
import com.limegroup.gnutella.*;
import java.io.*;

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

        Thread proxyThread = new Thread() {
            public void run() {
                proxyLoop();
            }
        };
        proxyThread.setDaemon(true);
        proxyThread.start();
        
        Thread destThread = new Thread() {
            public void run() {
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
                if(_proxyVersion == ProxyTest.SOCKS4)
                    checkSOCKS4(is);
                else if(_proxyVersion == ProxyTest.SOCKS5)
                    checkSOCKS5(is, incomingProxy.getOutputStream());
                else if(_proxyVersion == ProxyTest.HTTP)
                    checkHTTP(is);
                else
                    Assert.that(false, 
                         "test not set up correctly, incorrect proxy version");
                int a = 0;
                while(a != -1) 
                    a = is.read();
                if(!incomingProxy.isClosed())
                    incomingProxy.close();
            }
        } catch(IOException iox) {
            ErrorService.error(iox);
        }
    }

    private void checkSOCKS4(InputStream is) {
        //TODO1: implement
    }
    
    private void checkSOCKS5(InputStream is, OutputStream os) 
                                                           throws IOException{
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
        os.write((byte)1);//ip v4
        byte[] ip = {(byte)1,(byte)1,(byte)1,(byte)1,(byte)1, (byte)1, (byte)1};
        os.write(ip);
        os.flush();
    }

    
    private void checkHTTP(InputStream is) {
        //TODO1: implement
    }

    private void destLoop() {
        try {
            while(true) {
                Socket incomingDest = null;
                incomingDest = _destinationServer.accept();
                if(_proxyOn)
                    Assert.that(false, 
                           "Limewire connected to desination instead of proxy");
                if(!incomingDest.isClosed())
                    incomingDest.close();
            }
        } catch(IOException iox) {
            ErrorService.error(iox);
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
}
