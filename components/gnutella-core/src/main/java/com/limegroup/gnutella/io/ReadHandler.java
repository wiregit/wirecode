package com.limegroup.gnutella.io;

interface ReadHandler extends NIOHandler {
    boolean handleRead() throws java.io.IOException;
}