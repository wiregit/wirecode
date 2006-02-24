package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;

import com.limegroup.gnutella.Connection.ConnectionObserver;

public class StubConnectionObserver implements ConnectionObserver {
    private boolean noGOK;
    private int code;
    private String msg;
    private boolean badHandshake;
    private boolean connect;
    private Socket socket;
    private boolean iox;
    private IOException ioexception;
    private boolean shutdown;
    
    public synchronized void handleNoGnutellaOk(int code, String msg) {
        this.noGOK = true;
        this.code = code;
        this.msg = msg;
        notify();
    }

    public synchronized void handleBadHandshake() {
        this.badHandshake = true;
        notify();
    }

    public synchronized void handleConnect(Socket socket) throws IOException {
        this.connect = true;
        this.socket = socket;
        notify();
    }

    public synchronized void handleIOException(IOException iox) {
        this.iox = true;
        this.ioexception = iox;
        notify();
    }

    public synchronized void shutdown() {
        this.shutdown = true;
        notify();
    }
    
    public synchronized void waitForResponse(long time) throws Exception {
        wait(time);
    }

    public boolean isBadHandshake() {
        return badHandshake;
    }

    public int getCode() {
        return code;
    }

    public boolean isConnect() {
        return connect;
    }

    public IOException getIoexception() {
        return ioexception;
    }

    public boolean isIox() {
        return iox;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isNoGOK() {
        return noGOK;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public Socket getSocket() {
        return socket;
    }

}
