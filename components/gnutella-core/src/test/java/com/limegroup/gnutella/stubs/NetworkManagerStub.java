package com.limegroup.gnutella.stubs;

import java.io.IOException;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.NetworkManager;

public class NetworkManagerStub implements NetworkManager {
    private boolean acceptedIncomingConnection;    

    public boolean acceptedIncomingConnection() {
        return acceptedIncomingConnection;
    }
    
    public void setAcceptedIncomingConnection(boolean accepted) {
        this.acceptedIncomingConnection = accepted;
    }

    public boolean addressChanged() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canDoFWT() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canReceiveSolicited() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canReceiveUnsolicited() {
        // TODO Auto-generated method stub
        return false;
    }

    public byte[] getAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    public byte[] getExternalAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    public byte[] getNonForcedAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getNonForcedPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    public GUID getUDPConnectBackGUID() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean incomingStatusChanged() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isGUESSCapable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isIpPortValid() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isOOBCapable() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setListeningPort(int port) throws IOException {
        // TODO Auto-generated method stub
        
    }

    public int getStableUDPPort() {
        return 0;
    }

    public GUID getSolicitedGUID() {
        return null;
    }

    public int supportsFWTVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

}
