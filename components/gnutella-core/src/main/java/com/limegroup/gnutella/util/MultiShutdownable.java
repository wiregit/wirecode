package com.limegroup.gnutella.util;

import com.limegroup.gnutella.io.Shutdownable;

/**
 * A shutdownable that can notify other shutdownables when it is shutdown.
 * The shutdownable should return true for isCancelled when shutdown.
 */
public interface MultiShutdownable extends Shutdownable, Cancellable {

    /** Associates a new shutdownable to be notified upon shutdown. */
    public void addShutdownable(Shutdownable shutdowner);
    
}
