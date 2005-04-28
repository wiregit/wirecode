package com.limegroup.gnutella.io;

import java.io.IOException;

/**
 * Interface that combines ReadObserver & WriteObserver, to allow
 * one object to be passed around and marked as supporting both
 * read handling events & write handling events.
 */
public interface ReadWriteObserver extends ReadObserver, WriteObserver {}
    
    