package org.limewire.lws.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.limewire.core.api.network.NetworkManager;

public class TestNetworkManagerImpl implements NetworkManager{
    
    @Override
    public byte[] getExternalAddress() {
        try{
            return TestNetworkManagerImpl.getLocalhostAddress().getAddress();
        }catch(UnknownHostException ex){
            return null;
        }
    }

    @Override
    public boolean addressChanged() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isIncomingTLSEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isOutgoingTLSEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void portChanged() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setIncomingTLSEnabled(boolean value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setListeningPort(int port) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setOutgoingTLSEnabled(boolean value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void validateTLS() {
        // TODO Auto-generated method stub
        
    }
    
    // Utility method for Test
    public static String getIPAddress(){
        try{
            return getLocalhostAddress().getHostAddress();
        }catch(UnknownHostException ex){
            return "127.0.0.1";
        }          
    }
    
    private static InetAddress getLocalhostAddress() throws UnknownHostException{
        return InetAddress.getLocalHost();
    }
    
}
