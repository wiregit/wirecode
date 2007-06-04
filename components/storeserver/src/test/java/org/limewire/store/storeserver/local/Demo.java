package org.limewire.store.storeserver.local;

import java.util.Map;

import org.limewire.store.storeserver.api.AbstractDispatchee;
import org.limewire.store.storeserver.core.ServerImpl;
import org.limewire.store.storeserver.core.RemoteServer;
import org.limewire.store.storeserver.core.AbstractServer;
import org.limewire.store.storeserver.local.DemoRemoteServer;


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
        getLocalServer().setDispatchee(
                new AbstractDispatchee(getLocalServer()) {
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
