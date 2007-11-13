package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;

class PrivateGroupsValueForSelf extends AbstractPrivateGroupsValue{

    private final NetworkManager networkManager; 
    private final ApplicationServices applicationServices;
    private final byte[] ipaddress;
    
    
    public PrivateGroupsValueForSelf(NetworkManager networkManager, ApplicationServices applicationServices){
        super(AbstractPrivateGroupsValue.VERSION);
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
        ipaddress = networkManager.getAddress();
    }
    
    @Override
    public byte[] getGUID() {
        return applicationServices.getMyGUID();
    }

    @Override
    public int getPort() {
        return networkManager.getPort();
    }

    public byte[] getValue() {
        return AbstractPrivateGroupsValue.serialize(this);
    }

    public void write(OutputStream out) throws IOException {
        out.write(getValue());
    }

    @Override
    public int getPublicKey() {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public byte[] getIPAddress() {
        return ipaddress;
    }
}
