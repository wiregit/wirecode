package org.limewire.lws.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MockLWSNetworkAdapterImpl implements LWSNetworkAdapter {
    
    @Override
    public byte[] getExternalAddress() {
        try {
            return MockLWSNetworkAdapterImpl.getLocalhostAddress().getAddress();
        } catch(UnknownHostException ex) {
            return null;
        }
    }
    
    // Utility method for Test
    public static String getIPAddress(){
        try {
            return getLocalhostAddress().getHostAddress();
        } catch(UnknownHostException ex) {
            return "127.0.0.1";
        }          
    }
    
    private static InetAddress getLocalhostAddress() throws UnknownHostException {
        return InetAddress.getLocalHost();
    }
}
