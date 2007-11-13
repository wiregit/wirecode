package com.limegroup.gnutella.privategroups;

import junit.framework.TestCase;

public class ServerSocketClassTest extends TestCase {

    
    public void testBuffer(){
        ServerSocketClass socketTest = new ServerSocketClass();
        StringBuffer buffer = socketTest.getBuffer();
        System.out.println("helo");
        //socketTest.initializeServerSocket(9999);

        System.out.println("buffer before append: " + buffer);
        System.out.println("buffer capacity before append: " + buffer.capacity());
        System.out.println("buffer length before append: " + buffer.length());
        socketTest.appendBuffer("TEST");
        System.out.println("buffer after append: " + buffer);
        System.out.println("buffer capacity after append: " + buffer.capacity());
        System.out.println("buffer length after append: " + buffer.length());
        socketTest.emptyBuffer();
        System.out.println("buffer after empty: " + buffer);
        System.out.println("buffer capacity after empty: " + buffer.capacity());
        System.out.println("buffer length after empty: " + buffer.length());
    }
    
    
}
