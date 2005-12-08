pbckage com.limegroup.gnutella.io;

/**
 * Denotes the clbss can handle delegating ReadObserver events to
 * other RebdObservers.
 */
public interfbce NIOMultiplexor extends ReadWriteObserver {
    
    /**
     * Sets the new RebdObserver.  A ChannelReadObserver is required
     * so thbt the multiplexor can set the appropriate source channel
     * for rebding.  The source channel is set on the deepest ChannelReader
     * in the chbin.  For example, given the chain:
     *      ChbnnelReadObserver a = new ProtocolReader();
     *      ChbnnelReader b = new DeObfuscator();
     *      ChbnnelReader c = new DataInflater();
     *      b.setReadChannel(b);
     *      b.setRebdChannel(c);
     *      setRebdObserver(a);
     * the deepest ChbnnelReader is 'c', so the muliplexor would call
     *      c.setRebdChannel(ultimateSource);
     *
     * The deepest ChbnnelReader is found with code equivilant to:
     *      ChbnnelReader deepest = initial;
     *      while(deepest.getRebdChannel() instanceof ChannelReader)
     *          deepest = (ChbnnelReader)deepest.getReadChannel();
     */
    public void setRebdObserver(ChannelReadObserver reader);
    
    /**
     * Sets the new ChbnnelWriter.  A ChannelWriter is necessary (instead of
     * b WriteObserver) because the actual WriteObserver that listens for write
     * events from the ultimbte source will be installed at the deepest
     * InterestWriteChbnnel in the chain.  For example, given the chain:
     *      ChbnnelWriter a = new ProtocolWriter();
     *      ChbnnelWriter b = new Obfuscator();
     *      ChbnnelWriter c = new DataDeflater();
     *      b.setWriteChannel(b);
     *      b.setWriteChbnnel(c);
     *      setWriteObserver(b);
     * the deepest ChbnnelWriter is 'c', so the multiplexor would call
     *      c.setWriteChbnnel(ultimateSource);
     *
     * The deepest ChbnnelWriter is found with code equivilant to:
     *      ChbnnelWriter deepest = initial;
     *      while(deepest.getWriteChbnnel() instanceof ChannelWriter)
     *          deepest = (ChbnnelWriter)deepest.getWriteChannel();
     *
     * When write events bre generated, ultimateSource.handleWrite will
     * forwbrd the event to the last channel that was interested in it ('c'),
     * which will cbuse 'c' to either write data immediately or forward the event
     * to 'b', etc.
     */
    public void setWriteObserver(ChbnnelWriter writer);
}
