package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

public class RemoteHostData {

    private String _host;

    private int _port;

    private byte[] _clientGUID;

    private int _hashcode = 0;

    public RemoteHostData(String host, int port, byte[] guid) {
        _host = host;
        _port = port;
        _clientGUID = (byte[]) guid.clone();
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

    public boolean equals(Object o) {
        RemoteHostData other = (RemoteHostData)o;//dont catch ClassCastException
        return (_host.equals(other._host) &&
                _port==other._port &&
                Arrays.equals(_clientGUID, other._clientGUID) );
    }

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
