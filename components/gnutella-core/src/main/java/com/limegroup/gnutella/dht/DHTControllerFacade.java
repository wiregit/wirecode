package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.StorableModel;
import org.limewire.mojito.io.MessageDispatcherFactory;

import com.limegroup.gnutella.ManagedConnection;

/* This really isn't the best name for the object, but I don't know what'd be right. */
public interface DHTControllerFacade {

    MessageDispatcherFactory getMessageDispatcherFactory();

    DHTValueFactory getAltLocValueFactory();

    DHTValueFactory getPushProxyValueFactory();

    StorableModel getAltLocModel();

    StorableModel getPushProxyModel();

    boolean isActiveSupernode();

    List<ManagedConnection> getInitializedClientConnections();

    boolean isConnected();

    byte[] getAddress();

    int getPort();

    void updateCapabilities();

    void sendUpdatedCapabilities();

    ScheduledFuture<?> scheduleWithFixedDelay(Runnable runner, long initialDelay, long delay, TimeUnit milliseconds);

    boolean allow(SocketAddress addr);

    void reloadIPFilter();

    DHTBootstrapper getDHTBootstrapper(DHTController dhtController);

}
