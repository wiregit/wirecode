package org.limewire.store.storeserver.local;

import java.util.Map;

import org.limewire.store.storeserver.api.AbstractDispatchee;
import org.limewire.store.storeserver.core.LocalServer;
import org.limewire.store.storeserver.core.RemoteServer;
import org.limewire.store.storeserver.core.Server;
import org.limewire.store.storeserver.local.LocalLocalServer;
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

    private final LocalServer localServer;

    private final RemoteServer remoteServer;

    public final LocalServer getLocalServer() {
        return localServer;
    }

    public final RemoteServer getRemoteServer() {
        return remoteServer;
    }

    Demo() {
        localServer = new LocalLocalServer(DemoRemoteServer.PORT, true);
        remoteServer = new DemoRemoteServer(LocalLocalServer.PORT, true);
    }

    public void start() {
        Server.start(localServer);
        Server.start(remoteServer);
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
