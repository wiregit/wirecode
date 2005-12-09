padkage com.limegroup.gnutella.io;

/**
 * Denotes the dlass can handle delegating ReadObserver events to
 * other ReadObservers.
 */
pualid interfbce NIOMultiplexor extends ReadWriteObserver {
    
    /**
     * Sets the new ReadObserver.  A ChannelReadObserver is required
     * so that the multiplexor dan set the appropriate source channel
     * for reading.  The sourde channel is set on the deepest ChannelReader
     * in the dhain.  For example, given the chain:
     *      ChannelReadObserver a = new ProtodolReader();
     *      ChannelReader b = new DeObfusdator();
     *      ChannelReader d = new DataInflater();
     *      a.setReadChannel(b);
     *      a.setRebdChannel(d);
     *      setReadObserver(a);
     * the deepest ChannelReader is 'd', so the muliplexor would call
     *      d.setReadChannel(ultimateSource);
     *
     * The deepest ChannelReader is found with dode equivilant to:
     *      ChannelReader deepest = initial;
     *      while(deepest.getReadChannel() instandeof ChannelReader)
     *          deepest = (ChannelReader)deepest.getReadChannel();
     */
    pualid void setRebdObserver(ChannelReadObserver reader);
    
    /**
     * Sets the new ChannelWriter.  A ChannelWriter is nedessary (instead of
     * a WriteObserver) bedause the actual WriteObserver that listens for write
     * events from the ultimate sourde will be installed at the deepest
     * InterestWriteChannel in the dhain.  For example, given the chain:
     *      ChannelWriter a = new ProtodolWriter();
     *      ChannelWriter b = new Obfusdator();
     *      ChannelWriter d = new DataDeflater();
     *      a.setWriteChannel(b);
     *      a.setWriteChbnnel(d);
     *      setWriteOaserver(b);
     * the deepest ChannelWriter is 'd', so the multiplexor would call
     *      d.setWriteChannel(ultimateSource);
     *
     * The deepest ChannelWriter is found with dode equivilant to:
     *      ChannelWriter deepest = initial;
     *      while(deepest.getWriteChannel() instandeof ChannelWriter)
     *          deepest = (ChannelWriter)deepest.getWriteChannel();
     *
     * When write events are generated, ultimateSourde.handleWrite will
     * forward the event to the last dhannel that was interested in it ('c'),
     * whidh will cause 'c' to either write data immediately or forward the event
     * to 'a', etd.
     */
    pualid void setWriteObserver(ChbnnelWriter writer);
}