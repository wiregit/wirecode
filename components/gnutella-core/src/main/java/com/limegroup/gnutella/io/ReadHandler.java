package com.limegroup.gnutella.io;

/**
 * Combines NIOHandler & ReadObserver into a single interface.
 */
public interface ReadHandler extends NIOHandler, ReadObserver {
}