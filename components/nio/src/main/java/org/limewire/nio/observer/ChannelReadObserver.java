package org.limewire.nio.observer;

import org.limewire.nio.channel.ChannelReader;


/**
 * Combines the ReadObserver & ChannelReader interface.
 */
public interface ChannelReadObserver extends ReadObserver, ChannelReader {
}
