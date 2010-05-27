package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.messages.Message;

class MojitoMessage implements Message {

    private static final int GUILD_LENGTH = 16;
    
    private static final int HEADER_LENGTH = 23;
    
    /** Default TTL */
    private static final byte TTL = (byte)0x01;
    
    /** Default HOPS */
    private static final byte HOPS = (byte)0x00;
    
    private final long creationTime = System.currentTimeMillis();
    
    private final byte[] message;
    
    public MojitoMessage(byte[] header, byte[] payload) {
        this(concat(header, payload));
    }
    
    public MojitoMessage(byte[] message) {
        this.message = message;
    }
    
    public byte[] getMessage() {
        return message;
    }
    
    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public byte getFunc() {
        return org.limewire.mojito2.message.Message.F_DHT_MESSAGE;
    }

    @Override
    public byte[] getGUID() {
        byte[] guid = new byte[GUILD_LENGTH];
        System.arraycopy(message, 0, guid, 0, guid.length);
        return guid;
    }

    @Override
    public Class<? extends Message> getHandlerClass() {
        return getClass();
    }

    @Override
    public byte getHops() {
        return HOPS;
    }

    @Override
    public int getLength() {
        return message.length - HEADER_LENGTH;
    }

    @Override
    public Network getNetwork() {
        return Network.UNKNOWN;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public int getTotalLength() {
        return message.length;
    }

    @Override
    public byte getTTL() {
        return TTL;
    }

    @Override
    public byte hop() {
        return 0;
    }

    @Override
    public boolean isMulticast() {
        return false;
    }

    @Override
    public boolean isTCP() {
        return false;
    }

    @Override
    public boolean isUDP() {
        return true;
    }

    @Override
    public boolean isUnknownNetwork() {
        return getNetwork() == Network.UNKNOWN;
    }

    @Override
    public void setHops(byte hops) {
    }

    @Override
    public void setPriority(int priority) {
    }

    @Override
    public void setTTL(byte ttl) {
    }

    @Override
    public void write(OutputStream out, byte[] buf) throws IOException {
        write(out);
    }
    
    @Override
    public void writeQuickly(OutputStream out) throws IOException {
        write(out);
    }

    @Override
    public void write(OutputStream out) throws IOException {
        out.write(message);
    }
    
    @Override
    public int compareTo(Message o) {
        return getPriority() - o.getPriority();
    }
    
    private static byte[] concat(byte[] header, byte[] payload) {
        byte[] message = new byte[header.length + payload.length];
        
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(payload, 0, message, 
                header.length, payload.length);
        
        return message;
    }
}
