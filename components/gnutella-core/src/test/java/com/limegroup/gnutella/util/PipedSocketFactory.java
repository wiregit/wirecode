package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/** 
 * Returns two sockets A and B, so that the input of A is connected to 
 * the output of B and vice versa.  Typical use:
 * <pre>
 *     PipedSocketFactory factory=new PipedSocketFactory("1.1.1.1", "2.2.2.2");
 *     Connection cA=new Connection(factory.getSocketA());
 *     Connection cB=new Connection(factory.getSocketB());
 *     cA.send(m1);
 *     cB.receive();
 *     cB.send(m2);
 *     cA.receive();
 * </pre>
 */
public class PipedSocketFactory {
    OutputStream _aOut;
    InputStream  _aIn;

    OutputStream _bOut;
    InputStream  _bIn;

    InetAddress  _hostA;
    InetAddress  _hostB;
    
    boolean aOutClosed;
    boolean bOutClosed;
    boolean aInClosed;
    boolean bInClosed;
        
    /**
     * @param hostA the address to use for socket A
     * @param hostB the address to use for socket B
     */
    public PipedSocketFactory(String hostA, String hostB) 
            throws IOException, UnknownHostException {
        PipedOutputStream aOut=new PipedOutputStream() {
            public void close() throws IOException {
                super.close();
                aOutClosed = true;
                PipedSocketFactory.this.close();
            }
        };
        PipedInputStream bIn=new PipedInputStream(aOut) {
            public void close() throws IOException {
                super.close();
                bInClosed = true;
                PipedSocketFactory.this.close();
            }
        };
        PipedOutputStream bOut=new PipedOutputStream() {
            public void close() throws IOException {
                super.close();
                bOutClosed = true;
                PipedSocketFactory.this.close();
            }
        };
        PipedInputStream aIn=new PipedInputStream(bOut) {
            public void close() throws IOException {
                super.close();
                aInClosed = true;
                PipedSocketFactory.this.close();
            }
        };
        
        _aIn=aIn;
        _bIn=bIn;
        _aOut = aOut;
        _bOut = bOut;
            
        _hostA=InetAddress.getByName(hostA);
        _hostB=InetAddress.getByName(hostB);            
    }
    
    private void close() throws IOException {
        if(!aInClosed)
            _aIn.close();
        if(!bInClosed)
            _bIn.close();
        if(!aOutClosed)
            _aOut.close();
        if(!bOutClosed)
            _bOut.close();
    }

    public Socket getSocketA() {
        return new Socket() {
            public InetAddress getInetAddress() { return _hostB; }
            public InetAddress getLocalAddress() { return _hostA; }
            public InputStream getInputStream() { return _aIn; }
            public OutputStream getOutputStream() { return _aOut; }
            public void close() throws IOException { 
                PipedSocketFactory.this.close();
            }
        };
    }

    public Socket getSocketB() {
        return new Socket() {
            public InetAddress getInetAddress() { return _hostA; }
            public InetAddress getLocalAddress() { return _hostB; }
            public InputStream getInputStream() { return _bIn; }
            public OutputStream getOutputStream() { return _bOut; }
            public void close() throws IOException {
                PipedSocketFactory.this.close();
            }
        };
    }
}
