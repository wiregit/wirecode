package com.limegroup.gnutella.io;

/**
 * Denotes the class can handle delegating ReadObserver events to
 * other ReadObservers.
 */
public interface NIOMultiplexor extends ReadHandler, WriteHandler {
    
    /**
     * Sets the new ReadObserver.  A ChannelReadObserver is required
     * so that the multiplexor can set the appropriate source channel
     * for reading.  The source channel is set on the deepest ChannelReader
     * in the chain.  For example, given the chain:
     *      ChannelReadObserver a = new ProtocolReader();
     *      ChannelReader b = new DeObfuscator();
     *      ChannelReader c = new DataInflater();
     *      a.setReadChannel(b);
     *      b.setReadChannel(c);
     *      setReadObserver(a);
     * the deepest ChannelReader is 'c', so the muliplexor would call
     *      c.setReadChannel(ultimateSource);
     *
     * The deepest ChannelReader is found with code equivilant to:
     *      ChannelReader deepest = initial;
     *      while(deepest.getReadChannel() instanceof ChannelReader)
     *          deepest = (ChannelReader)deepest.getReadChannel();
     */
    public void setReadObserver(ChannelReadObserver reader);
}