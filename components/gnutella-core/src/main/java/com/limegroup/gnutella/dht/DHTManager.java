package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.LifecycleListener;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

public interface DHTManager extends LifecycleListener{

    public void startDHT(boolean activeMode);

    public void switchMode(boolean toActiveMode);

    public void addBootstrapHost(SocketAddress hostAddress);

    public void addressChanged();

    public List<IpPort> getActiveDHTNodes(int maxNodes);

    public boolean isActiveNode();

    public void shutdown();

    public boolean isRunning();

    public int getDHTVersion();

    public MojitoDHT getMojitoDHT();

}