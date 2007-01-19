package org.limewire.nio.channel;

import org.limewire.nio.observer.ReadObserver;


/**
 * Combines the ReadObserver & ChannelReader interface.
 */
public interface ChannelReadObserver extends ReadObserver, ChannelReader {
}
