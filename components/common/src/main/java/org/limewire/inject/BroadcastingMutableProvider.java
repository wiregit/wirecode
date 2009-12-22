package org.limewire.inject;

import org.limewire.listener.ListenerSupport;

/**
 * A <code>MutableProvider</code> that broadcasts when its
 * value changes
 * @param <T>
 */
public interface BroadcastingMutableProvider<T> extends MutableProvider<T>, ListenerSupport<T> {
}
