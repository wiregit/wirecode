package com.limegroup.gnutella;

import java.net.Socket;

import com.google.inject.Singleton;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

@Singleton
public interface ConnectionFactory {

    public Connection createConnection(Socket socket);

    public Connection createConnection(String host, int port);

    public Connection createConnection(String host, int port,
            ConnectType connectType);

}