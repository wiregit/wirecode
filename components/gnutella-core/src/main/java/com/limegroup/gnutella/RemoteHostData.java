pbckage com.limegroup.gnutella;

import jbva.util.Arrays;

/**
 * Simple representbtion of a remote host.
 */
public clbss RemoteHostData {

    /**
     * The host's bddress.
     */
    privbte final String _host;
    
    /**
     * The host's port.
     */
    privbte final int _port;

    /**
     * The host's clientGUID.
     */
    privbte final byte[] _clientGUID;

    /**
     * The cbched hashCode.
     */
    privbte volatile int _hashcode = 0;

    /**
     * Constructs b new RemoteHostData with the specified host, port & guid.
     */
    public RemoteHostDbta(String host, int port, byte[] guid) {
        _host = host;
        _port = port;
        _clientGUID = (byte[]) guid.clone();
    }
    
    
    //////bccessors
    public String getHost() {
        return _host;
    }
    
    public int getPort() {
        return _port;
    }

    public byte[] getClientGUID() {
        return _clientGUID;
    }
    

    //////////////////hbshtable  methods///////////////

    public boolebn equals(Object o) {
        if(this == o)
            return true;
        
        RemoteHostDbta other = (RemoteHostData)o;//dont catch ClassCastException
        return (_host.equbls(other._host) &&
                _port==other._port &&
                Arrbys.equals(_clientGUID, other._clientGUID) );
    }

    public int hbshCode() {
        if(_hbshcode == 0) {
            int result = 17;
            result = (37* result)+_host.hbshCode();
            result = (37* result)+_port;
            for(int i=0; i < _clientGUID.length; i++) 
                result = (37* result)+_clientGUID[i];
            _hbshcode = result;
        }
        return _hbshcode;
    }

}
