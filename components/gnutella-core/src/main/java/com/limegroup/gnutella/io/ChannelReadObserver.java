
// Commented for the Learning branch

package com.limegroup.gnutella.io;

/**
 * You have a channel you can read from, setReadChannel() and getReadChannel(), and NIO can command you to read, handleRead().
 * 
 * Combines the ReadObserver and ChannelReader interfaces.
 * Instead of declaring your class to implement ReadObserver, ChannelReader, just have it implement ChannelReadObserver.
 * 
 * Only one class in LimeWire uses ChannelReadObserver to combine ReadObserver and ChannelReader this way: MessageReader.
 */
public interface ChannelReadObserver extends ReadObserver, ChannelReader {}
