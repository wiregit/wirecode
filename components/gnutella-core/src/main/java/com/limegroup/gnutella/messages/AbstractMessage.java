package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;

public abstract class AbstractMessage implements Message {
    
    /** The GUID of this message */
    private byte[] guid;
    
    /** The function ID of this message */
    private byte func;

    /* We do not support TTLs > 2^7, nor do we support packets
     * of length > 2^31 */
    
    private byte ttl;
    
    private byte hops;

    /** 
     * Priority for flow-control. Lower numbers mean higher priority. 
     * NOT written to network.
     */
    private int priority = 0;
    
    /** Time this was created. Not written to network. */
    private long creationTime = System.currentTimeMillis();
    
    /**
     * The network that this was received on or is going to be sent to.
     */
    private int network;
    
    protected AbstractMessage(byte[] guid, byte func, byte ttl, byte hops, int network) {
        if (guid.length != 16) {
            throw new IllegalArgumentException("invalid guid length: " + guid.length);
        }
        
        this.guid = guid;
        this.func = func;
        this.ttl = ttl;
        this.hops = hops;
        this.network = network;
        //repOk();
    }
    
    protected abstract void repOk();
    
    public int getNetwork() {
        return network;
    }
    
    public boolean isMulticast() {
        return network == N_MULTICAST;
    }
    
    public boolean isUDP() {
        return network == N_UDP;
    }
    
    public boolean isTCP() {
        return network == N_TCP;
    }
    
    public boolean isUnknownNetwork() {
        return network == N_UNKNOWN;
    }

    public byte[] getGUID() {
        return guid;
    }

    public byte getFunc() {
        return func;
    }

    public byte getTTL() {
        return ttl;
    }

    public void setTTL(byte ttl) throws IllegalArgumentException {
        if (ttl < 0) {
            throw new IllegalArgumentException("invalid TTL: "+ttl);
        }
        this.ttl = ttl;
    }
    
    protected void setGUID(GUID guid) {
        this.guid = guid.bytes();
    }
    
    public void setHops(byte hops) throws IllegalArgumentException {
        if(hops < 0) {
            throw new IllegalArgumentException("invalid hops: " + hops);
        }
        this.hops = hops;
    }

    public byte getHops() {
        return hops;
    }
    
    public byte hop() {
        hops++;
        if (ttl>0) {
            return ttl--;
        } else {
            return ttl;
        }
    }

    public long getCreationTime() {
        return creationTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority=priority;
    }

    public int compareTo(Object message) {
        return ((Message)message).getPriority() - getPriority();
    }
    
    public abstract int getLength();
    
    public int getTotalLength() {
        //Header is 23 bytes.
        return 23 + getLength();
    }
    
    public void writeQuickly(OutputStream out) throws IOException {
        out.write(guid, 0, guid.length /* 16 */);
        out.write(func);
        out.write(ttl);
        out.write(hops);
        ByteOrder.int2leb(getLength(), out);
        writePayload(out);
    }
    
    public void write(OutputStream out, byte[] buf) throws IOException {
        System.arraycopy(guid, 0, buf, 0, guid.length /* 16 */);
        buf[16]=func;
        buf[17]=ttl;
        buf[18]=hops;
        ByteOrder.int2leb(getLength(), buf, 19);
        out.write(buf);
        writePayload(out);
    }

    public void write(OutputStream out) throws IOException {
        write(out, new byte[23]);
    }

    /** 
     * @modifies out
     * @effects writes the payload specific data to out (the stuff
     *  following the header).  Does NOT flush out.
     */
    protected abstract void writePayload(OutputStream out) throws IOException;
    
    public String toString() {
        return "{guid="+(new GUID(guid)).toString()
             +", ttl="+ttl
             +", hops="+hops
             +", priority="+getPriority()+"}";
    }
}
