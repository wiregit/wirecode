package org.limewire.lws.server.demos;

import java.util.Map;

import org.limewire.lws.server.AbstractDispatchee;
import org.limewire.lws.server.AbstractRemoteServer;
import org.limewire.lws.server.AbstractServer;
import org.limewire.lws.server.LocalServerForTesting;
import org.limewire.lws.server.RemoteServerForTesting;
import org.limewire.lws.server.ServerImpl;

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
    private final AbstractRemoteServer remoteServer;

    public final ServerImpl getLocalServer() {
        return localServer;
    }

    public final AbstractRemoteServer getRemoteServer() {
        return remoteServer;
    }

    Demo() {
        localServer = new LocalServerForTesting("localhost", RemoteServerForTesting.PORT);
        remoteServer = new RemoteServerForTesting(LocalServerForTesting.PORT);
    }

    public void start() {
        AbstractServer.start(localServer);
        AbstractServer.start(remoteServer);
    }

    public void realMain(final String[] args) {
        start();
        getLocalServer().getDispatcher().setDispatchee(
                new AbstractDispatchee() {
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
