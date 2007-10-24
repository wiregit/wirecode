package com.limegroup.gnutella.connection;

import java.net.Socket;

import org.limewire.net.SocketsManager.ConnectType;

import com.google.inject.Singleton;

@Singleton
public interface BlockingConnectionFactory {

    public BlockingConnection createConnection(Socket socket);

    public BlockingConnection createConnection(String host, int port);

    public BlockingConnection createConnection(String host, int port,
            ConnectType connectType);

}