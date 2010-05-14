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

        return false;
    }

    @Override
    public boolean isIncomingTLSEnabled() {

        return false;
    }

    @Override
    public boolean isOutgoingTLSEnabled() {

        return false;
    }

    @Override
    public void portChanged() {
        
    }

    @Override
    public void setIncomingTLSEnabled(boolean value) {
        
    }

    @Override
    public void setListeningPort(int port) throws IOException {
        
    }

    @Override
    public void setOutgoingTLSEnabled(boolean value) {
        
    }

    @Override
    public void validateTLS() {
        
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
