package com.limegroup.gnutella.io;

/**
 * Denotes the class can handle delegating ReadObserver events to
 * other ReadObservers.
 */
public interface NIOMultiplexor extends ReadHandler, WriteHandler {
    
    /**
     * Sets the new ReadObserver.
     *
     * This requires a ChannelReadObserver, so that any data that was buffered
     * by the existing ReadObserver can be written to the new ReadObserver.
     * After buffered data is read, the ChannelReader's reading channel is set
     * to the socket's channel.
     * After the reader's channel is set to the SocketChannel, the interest in
     * reading from that channel is turned on.
     *
     * It is important that the reader be able to read all data from the underlying
     * channel upon its handleRead calls.
     *
     * This is performed with code similar to:
     *      public void setReadObserver(ChannelReadObserver reader) {
     *          NIODispatcher.instance().invokeLater(new Runnable() {
     *              public void run() {
     *                   try {
     *                       myReader = reader;
     *
     *                       ChannelReader channel = reader;
     *                       while(channel.getReadChannel() instanceof ChannelReader)
     *                           channel = (ChannelReader)channel.getReadChannel();
     *                       channel.setReadChannel(existingChannelWithBufferedData);
     *                       reader.handleRead();
     *                       existingChannelWithBufferedData.shutdown();
     *                       channel.setReadChannel(socket.getChannel());
     *                       NIODispatcher.instance().interestRead(socket.getChannel());
     *                   } catch(IOException iox) {
     *                       shutdown();
     *                   }
     *              }     
     *          });
     *      }
     */
    public void setReadObserver(ChannelReadObserver reader);
}