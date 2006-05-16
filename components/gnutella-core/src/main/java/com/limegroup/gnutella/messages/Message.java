package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.OutputStream;

public interface Message extends Comparable {
    
    public static final int N_UNKNOWN = -1;
    public static final int N_TCP = 1;
    public static final int N_UDP = 2;
    public static final int N_MULTICAST = 3;
    
    public int getNetwork();
    
    public boolean isMulticast();
    
    public boolean isUDP();
    
    public boolean isTCP();
    
    public boolean isUnknownNetwork();

    public byte[] getGUID();

    public byte getFunc();

    public byte getTTL();
    
    /**
     * If ttl is less than zero, throws IllegalArgumentException.  Otherwise sets
     * this TTL to the given value.  This is useful when you want certain messages
     * to travel less than others.
     *    @modifies this' TTL
     */
    public void setTTL(byte ttl) throws IllegalArgumentException;
    
    /**
     * If the hops is less than zero, throws IllegalArgumentException.
     * Otherwise sets this hops to the given value.  This is useful when you
     * want certain messages to look as if they've travelled further.
     *   @modifies this' hops
     */
    public void setHops(byte hops) throws IllegalArgumentException;

    public byte getHops();

    /** Returns the length of this' payload, in bytes. */
    public int getLength();
    
    /** Returns the total length of this, in bytes */
    public int getTotalLength();
    
    /** @modifies this
     *  @effects increments hops, decrements TTL if > 0, and returns the
     *   OLD value of TTL.
     */
    public byte hop();

    /** 
     * Returns the system time (i.e., the result of System.currentTimeMillis())
     * this was instantiated.
     */
    public long getCreationTime();

    /** Returns this user-defined priority.  Lower values are higher priority. */
    public int getPriority();

    /** Set this user-defined priority for flow-control purposes.  Lower values
     *  are higher priority. */
    public void setPriority(int priority);
    
    /**
     * @modifies out
     * @effects Writes an encoding of this to out.  Does NOT flush out.
     */
    public void write(OutputStream out) throws IOException;
    
    /**
     * Writes a message out, using the buffer as the temporary header.
     */
    public void write(OutputStream out, byte[] buf) throws IOException;
    
    /**
     * Writes a message quickly, without using temporary buffers or crap.
     */
    public void writeQuickly(OutputStream out) throws IOException;
    
    /**
     * Records the dropping of this message in statistics.
     */
    public void recordDrop();
    
    /** 
     * Returns a message identical to this but without any extended (typically
     * GGEP) data.  Since Message's are mostly immutable, the returned message
     * may alias parts of this; in fact the returned message could even be this.
     * The caveat is that the hops and TTL field of Message can be mutated for
     * efficiency reasons.  Hence you must not call hop() on either this or the
     * returned value.  Typically this is not a problem, as hop() is called
     * before forwarding/broadcasting a message.  
     *
     * @return an instance of this without any dangerous extended payload
     */
    public Message stripExtendedPayload();
}
