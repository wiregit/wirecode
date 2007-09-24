package com.limegroup.gnutella.stubs;

import java.io.IOException;

import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.NetworkManager;

@Singleton
public class NetworkManagerStub implements NetworkManager {
    private boolean acceptedIncomingConnection;
    private byte[] address;
    private int port;
    private boolean ipPortValid;
    private GUID solicitedGUID;    
    private boolean canReceiveSolicited;
    private boolean canReceiveUnsolicited;
    private boolean guessCapable;
    private byte[] externalAddress;

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
        return canReceiveSolicited;
    }

    public boolean canReceiveUnsolicited() {
        return canReceiveUnsolicited;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }
    
    public byte[] getAddress() {
        return address;
    }

    public byte[] getExternalAddress() {
        return externalAddress;
    }

    public byte[] getNonForcedAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getNonForcedPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public int getPort() {
        return port;
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
        return guessCapable;
    }

    public boolean isIpPortValid() {
        return ipPortValid;
    }
    
    public void setIpPortValid(boolean ipPortValid) {
        this.ipPortValid = ipPortValid;
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
        return solicitedGUID;
    }
    
    public void setSolicitedGUID(GUID solicitedGUID) {
        this.solicitedGUID = solicitedGUID;
    }

    public int supportsFWTVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setCanReceiveSolicited(boolean canReceiveSolicited) {
        this.canReceiveSolicited = canReceiveSolicited;
    }


    public void setCanReceiveUnsolicited(boolean canReceiveUnsolicited) {
        this.canReceiveUnsolicited = canReceiveUnsolicited;
    }

    public void setGuessCapable(boolean guessCapable) {
        this.guessCapable = guessCapable;
    }

    public void setExternalAddress(byte[] externalAddress) {
        this.externalAddress = externalAddress;
    }

}
