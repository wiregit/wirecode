package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

public class StubHandshakeState extends HandshakeState {
    
    private boolean writing;
    private boolean reading;
    private boolean throwNGOK;
    private int ngokCode;
    private boolean throwIOX;
    private boolean returnTrueOnProcess;
    private boolean processed;
    private byte[] dataToPutInBuffer;

    public void setDataToPutInBuffer(byte[] dataToPutInBuffer) {
        this.dataToPutInBuffer = dataToPutInBuffer;
    }

    public StubHandshakeState() {
        super(new HandshakeSupport("127.0.0.1"));
    }

    boolean isWriting() {
        return writing;
    }

    boolean isReading() {
        return reading;
    }

    boolean process(Channel channel, ByteBuffer buffer) throws IOException {
        processed = true;
        
        if(throwNGOK)
            throw NoGnutellaOkException.createClientUnknown(ngokCode);
        if(throwIOX)
            throw new IOException();
        
        if(dataToPutInBuffer != null)
            buffer.put(dataToPutInBuffer);
        
        return returnTrueOnProcess;
    }

    public void setReading(boolean reading) {
        this.reading = reading;
    }

    public void setReturnTrueOnProcess(boolean returnTrueOnProcess) {
        this.returnTrueOnProcess = returnTrueOnProcess;
    }

    public void setThrowIOX(boolean throwIOX) {
        this.throwIOX = throwIOX;
    }

    public void setThrowNGOK(boolean throwNGOK, int code) {
        this.throwNGOK = throwNGOK;
        this.ngokCode = code;
    }

    public void setWriting(boolean writing) {
        this.writing = writing;
    }
    
    public boolean isProcessed() {
        return processed;
    }
    
    public void clear() {
        processed = false;
    }

}
