
// Edited for the Learning branch

package com.limegroup.gnutella.io;

/**
 * When NIO tells you to read or write, you have objects to call to do it for you, setReadObserver(), setWriteObserver().
 * NIO can command you to read, handleRead(), and NIO can command you to get data and write, handleWrite().
 * 
 * 
 * 
 * 
 * Denotes the class can handle delegating ReadObserver events to
 * other ReadObservers.
 * 
 * 
 * Your read observer is the object that you tell to read when NIO tells you to read.
 * 
 * For instance, you're an NIOSocket, at the end of the read chain.
 * NIO tells you to read.
 * You don't read directly, instead you tell your read observer to read.
 * Your read observer is MessageReader, the first object in the chain of readers.
 * It reads from its source, which reads from you.
 * 
 * Your write observer is the object that you tell to write when NIO tells you to write.
 * 
 * 
 */
public interface NIOMultiplexor extends ReadWriteObserver {

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

    /**
     * Sets the new ChannelWriter.  A ChannelWriter is necessary (instead of
     * a WriteObserver) because the actual WriteObserver that listens for write
     * events from the ultimate source will be installed at the deepest
     * InterestWriteChannel in the chain.  For example, given the chain:
     *      ChannelWriter a = new ProtocolWriter();
     *      ChannelWriter b = new Obfuscator();
     *      ChannelWriter c = new DataDeflater();
     *      a.setWriteChannel(b);
     *      b.setWriteChannel(c);
     *      setWriteObserver(a);
     * the deepest ChannelWriter is 'c', so the multiplexor would call
     *      c.setWriteChannel(ultimateSource);
     *
     * The deepest ChannelWriter is found with code equivilant to:
     *      ChannelWriter deepest = initial;
     *      while(deepest.getWriteChannel() instanceof ChannelWriter)
     *          deepest = (ChannelWriter)deepest.getWriteChannel();
     *
     * When write events are generated, ultimateSource.handleWrite will
     * forward the event to the last channel that was interested in it ('c'),
     * which will cause 'c' to either write data immediately or forward the event
     * to 'b', etc.
     */
    public void setWriteObserver(ChannelWriter writer);
}
