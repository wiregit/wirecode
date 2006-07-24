package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.LifecycleListener;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.settings.ContextSettings;

public interface DHTManager extends LifecycleListener{
    
    //TODO: this will change!
    public static int DHT_VERSION = ContextSettings.VERSION.getValue();
    
    public static enum DHTMode {
        NONE((byte)0x00),ACTIVE((byte)0x01),PASSIVE((byte)0x20);
        
        public static final byte DHT_MODE_MASK = 0x0F;
        
        private byte mode;
        
        DHTMode(byte mode){
            this.mode = mode;
        }
        
        public byte getByte() {
            return mode;
        }
    }
    
    public static final byte DHT_MODE_MASK = 0x0F;
    
    public void start(boolean activeMode);

    public void stop();
    
    public void switchMode(boolean toActiveMode);

    public void addBootstrapHost(SocketAddress hostAddress);

    public void addressChanged();

    public List<IpPort> getActiveDHTNodes(int maxNodes);

    public boolean isActiveNode();
    
    public boolean isRunning();
    
    public boolean isWaiting();

    public MojitoDHT getMojitoDHT();

}