package com.limegroup.gnutella;

import java.util.Arrays;

/**
 * Simple representation of a remote host.
 */
public class RemoteHostData {

    /**
     * The host's address.
     */
    private final String _host;
    
    /**
     * The host's port.
     */
    private final int _port;

    /**
     * The host's clientGUID.
     */
    private final byte[] _clientGUID;

    /**
     * The cached hashCode.
     */
    private volatile int _hashcode = 0;

    /**
     * Constructs a new RemoteHostData with the specified host, port & guid.
     */
    public RemoteHostData(String host, int port, byte[] guid) {
        _host = host;
        _port = port;
        _clientGUID = guid.clone();
    }
    
    
    //////accessors
    public String getHost() {
        return _host;
    }
    
    public int getPort() {
        return _port;
    }

    public byte[] getClientGUID() {
        return _clientGUID;
    }
    

    //////////////////hashtable  methods///////////////

    @Override
    public boolean equals(Object o) {
        if(this == o)
            return true;
        
        RemoteHostData other = (RemoteHostData)o;//dont catch ClassCastException
        return (_host.equals(other._host) &&
                _port==other._port &&
                Arrays.equals(_clientGUID, other._clientGUID) );
    }

    @Override
    public int hashCode() {
        if(_hashcode == 0) {
            int result = 17;
            result = (37* result)+_host.hashCode();
            result = (37* result)+_port;
            for(int i=0; i < _clientGUID.length; i++) 
                result = (37* result)+_clientGUID[i];
            _hashcode = result;
        }
        return _hashcode;
    }

}
