package com.limegroup.gnutella.io;

interface ConnectHandler extends NIOHandler {
    void handleConnect() throws java.io.IOException;
}