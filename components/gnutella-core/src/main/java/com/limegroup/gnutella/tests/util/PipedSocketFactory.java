package com.limegroup.gnutella.tests.util;

import java.io.*;
import java.net.*;

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
    
    public PipedSocketFactory(String hostA, String hostB) throws IOException {
        this(hostA, hostB, -1, -1);
    }
        
    /**
     * @param hostA the address to use for socket A
     * @param hostB the address to use for socket B
     * @param limitAB the number of bytes to allow A to send to B,
     *  or unlimited if less than zero
     * @param limitBA the number of bytes to allow B to send to A,
     *  or unlimited if less than zero
     */
    public PipedSocketFactory(String hostA, String hostB,
                              int limitAB, int limitBA) 
            throws IOException, UnknownHostException {
        PipedOutputStream aOut=new PipedOutputStream();
        PipedInputStream bIn=new PipedInputStream(aOut);
        PipedOutputStream bOut=new PipedOutputStream();
        PipedInputStream aIn=new PipedInputStream(bOut);
        
        _aIn=aIn;
        _bIn=bIn;
        _aOut=limitAB<0 ? 
            (OutputStream)aOut : 
            new BlockingOutputStream(aOut, limitAB);
        _bOut=limitBA<0 ? 
            (OutputStream)bOut : 
            new BlockingOutputStream(bOut, limitBA);
            
        _hostA=InetAddress.getByName(hostA);
        _hostB=InetAddress.getByName(hostB);            
    }

    public Socket getSocketA() {
        return new Socket() {
            public InetAddress getInetAddress() { return _hostB; }
            public InetAddress getLocalAddress() { return _hostA; }
            public InputStream getInputStream() { return _aIn; }
            public OutputStream getOutputStream() { return _aOut; }
            public void close() throws IOException { 
                _aOut.close();
                _bOut.close();
                _aIn.close();
                _bIn.close();
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
                _aOut.close();
                _bOut.close();
                _aIn.close();
                _bIn.close();
            }
        };
    }
}
