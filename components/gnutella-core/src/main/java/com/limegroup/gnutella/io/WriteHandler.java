package com.limegroup.gnutella.io;

interface WriteHandler extends NIOHandler {
    boolean handleWrite() throws java.io.IOException;
    
}