package com.limegroup.gnutella.connection;

import java.net.Socket;

import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

public interface ManagedConnectionFactory {

    public ManagedConnection createManagedConnection(String host, int port);

    public ManagedConnection createManagedConnection(String host, int port,
            ConnectType type);

    public ManagedConnection createManagedConnection(Socket socket);

}