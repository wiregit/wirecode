package com.limegroup.gnutella.stubs;

import java.io.IOException;

import org.limewire.io.NetworkUtils;

import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.NetworkManager;

@Singleton
public class NetworkManagerStub implements NetworkManager {
    private boolean acceptedIncomingConnection;
    private byte[] address = new byte[] { 127, 0, 0, 1 };
    private int port = 5555;
    private GUID solicitedGUID = new GUID();
    private boolean canReceiveSolicited;
    private boolean canReceiveUnsolicited;
    private boolean guessCapable;
    private byte[] externalAddress;
    private boolean canDoFWT;
    private GUID udpConnectBackGUI = new GUID();
    private boolean oobCapable;

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
        return canDoFWT;
    }
    
    public void setCanDoFWT(boolean canDoFWT) {
        this.canDoFWT = canDoFWT;
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
    
    /**
     * Returns whatever is set in {@link #setAddress(byte[])} by default
     * 127.0.0.1.
     */
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
    
    /**
     * Returns whatever is set by {@link #setPort(int)}, by default 5555.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns a random but the same GUID across calls.
     */
    public GUID getUDPConnectBackGUID() {
        return udpConnectBackGUI;
    }
    
    public void setUDPConnectBackGUID(GUID guid) {
        udpConnectBackGUI = guid;
    }

    public boolean incomingStatusChanged() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isGUESSCapable() {
        return guessCapable;
    }

    public boolean isIpPortValid() {
        return (NetworkUtils.isValidAddress(getAddress()) &&
                NetworkUtils.isValidPort(getPort()));
    }
    
    public boolean isOOBCapable() {
        return oobCapable;
    }
    
    public void setOOBCapable(boolean capable) {
        oobCapable = capable;
    }

    public void setListeningPort(int port) throws IOException {
        // TODO Auto-generated method stub
    }

    public int getStableUDPPort() {
        return 0;
    }

    /**
     * Returns whatever is set by {@link #setSolicitedGUID(GUID)}, by default
     * a random GUID that does not change.
     */
    public GUID getSolicitedGUID() {
        return solicitedGUID;
    }
    
    public void setSolicitedGUID(GUID solicitedGUID) {
        this.solicitedGUID = solicitedGUID;
    }

    public int supportsFWTVersion() {
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
