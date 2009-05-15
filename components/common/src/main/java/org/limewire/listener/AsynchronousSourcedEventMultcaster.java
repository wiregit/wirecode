package org.limewire.listener;

public interface AsynchronousSourcedEventMultcaster<E extends SourcedEvent<S>, S> extends
        SourcedEventMulticaster<E, S> {

}
