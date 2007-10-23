package com.limegroup.gnutella.connection;

import java.net.Socket;

import com.google.inject.Singleton;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

@Singleton
public interface BlockingConnectionFactory {

    public BlockingConnection createConnection(Socket socket);

    public BlockingConnection createConnection(String host, int port);

    public BlockingConnection createConnection(String host, int port,
            ConnectType connectType);

}