package org.limewire.listener;

import java.util.concurrent.Executor;

import org.limewire.logging.Log;

/**
 * A <code>CachingEventMulticasterImpl</code> that dispatches events to listeners asynchronously.
 * @param <E>
 */
public class AsynchronousCachingEventMulticasterImpl<E> extends CachingEventMulticasterImpl<E> implements AsynchronousBroadcaster<E> {

    public AsynchronousCachingEventMulticasterImpl(Executor executor) {
        this(executor, BroadcastPolicy.ALWAYS);
    }

    public AsynchronousCachingEventMulticasterImpl(Executor executor, Log log) {
        this(executor, BroadcastPolicy.ALWAYS, log);
    }

    public AsynchronousCachingEventMulticasterImpl(Executor executor, BroadcastPolicy broadcastPolicy) {
        super(broadcastPolicy, new AsynchronousMulticaster<E>(executor));
    }

    public AsynchronousCachingEventMulticasterImpl(Executor executor, BroadcastPolicy broadcastPolicy, Log log) {
        super(broadcastPolicy, new AsynchronousMulticaster<E>(executor, log));
    }
}
