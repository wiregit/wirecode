package com.limegroup.gnutella.util;

import java.util.Stack;

/**
 * A cache of byte[].  Intended to allow a limited number of byte[]s to be created,
 * with methods for retrieving & returning them.  When the maximum number is created,
 * further attempts to retrieve will block until others have been returned.
 */
public class ByteArrayCache {
    
    /** Default # of byte[]'s to hold. */
    private static final int DEFAULT_SIZE = 512;
    
    /** Default length of returned bytes. */
    private static final int DEFAULT_LENGTH = 1024;

    /** Holds cached byte[]'s for future re-use. */
    private final Stack CACHE = new Stack();
    
    /** The number of byte[]'s this cache has created that haven't been erased. */
    private int _numCreated;

    /** The number of byte[]'s to create before blocking when the next one is gotten. */
    private int _maxSize;
    
    /** The length of buffers to create. */
    private int _length;

    /** Constructs a new ByteArraCache using the default size of 512 & length of 1024 */
    public ByteArrayCache() {
        this(DEFAULT_SIZE, DEFAULT_LENGTH);
    }
    
    /** Constructs a new ByteArrayCache using the given maxSize & length. */
    public ByteArrayCache(int maxSize, int length) {
        _maxSize = maxSize;
        _length = length;
        CACHE.ensureCapacity(maxSize);
    }
    
    /**
     * Attempts to retrieve a new byte[].
     * If the cache is empty and getCreated() is >= getMaxSize(), this will block until
     * a byte[] is returned to the cache.
     */
    public synchronized byte[] get() throws InterruptedException {
        while(true) {
            if (!CACHE.isEmpty()) {
                return (byte[]) CACHE.pop();
            } else if (_numCreated < _maxSize) {
                _numCreated++;
                return new byte[_length];
            } else {
                wait();
            }
        }
    }
    
    /**
     * Returns a byte[] to this cache.
     * The byte[] MUST HAVE BEEN A byte[] RETURNED FROM get().
     */
    public synchronized void release(byte[] data) {
        CACHE.push(data);
        notifyAll();
    }
    
    /** Clears all items in the cache. */
    public synchronized void clear() {
        _numCreated -= CACHE.size();
        CACHE.clear();
        notifyAll();
    }
    
    /** Returns the number of byte[]'s this cache has created. */
    public synchronized int getCreated() {
        return _numCreated;
    }
    
    /** Returns the length of the byte[]'s this creates. */
    public int getLength() {
        return _length;
    }
    
    /** Returns the maximum number of byte[]'s this will create. */
    public int getMaxSize() {
        return _maxSize;
    }
    
}
