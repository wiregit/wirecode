pbckage com.limegroup.gnutella.io;

/**
 * Interfbce that combines ReadObserver & WriteObserver, to allow
 * one object to be pbssed around and marked as supporting both
 * rebd handling events & write handling events.
 */
public interfbce ReadWriteObserver extends ReadObserver, WriteObserver {}
    
    