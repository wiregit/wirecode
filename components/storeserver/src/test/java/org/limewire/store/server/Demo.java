package org.limewire.store.server;

import java.util.Map;

import org.limewire.store.server.AbstractDispatchee;
import org.limewire.store.server.AbstractServer;
import org.limewire.store.server.LocalLocalServer;
import org.limewire.store.server.RemoteServer;
import org.limewire.store.server.ServerImpl;


/**
 * Starts up the two demo servers for communication.
 * 
 * @author jpalm
 */
public class Demo {

    public static void main(final String[] args) {
        new Demo().realMain(args);
    }

    private final ServerImpl localServer;
    private final RemoteServer remoteServer;

    public final ServerImpl getLocalServer() {
        return localServer;
    }

    public final RemoteServer getRemoteServer() {
        return remoteServer;
    }

    Demo() {
        localServer = new LocalLocalServer("localhost", DemoRemoteServer.PORT);
        remoteServer = new DemoRemoteServer(LocalLocalServer.PORT);
    }

    public void start() {
        AbstractServer.start(localServer);
        AbstractServer.start(remoteServer);
    }

    public void realMain(final String[] args) {
        start();
        getLocalServer().getDispatcher().setDispatchee(
                new AbstractDispatchee(getLocalServer().getDispatcher()) {
                    public String dispatch(final String cmd,
                            final Map<String, String> args) {
                        return "OK (" + cmd + ":" + args + ")";
                    }

                    @Override
                    protected void connectionChanged(boolean isConnected) {
                        System.out.println("connectionChanged: " + isConnected);
                    }
                });
    }

}
