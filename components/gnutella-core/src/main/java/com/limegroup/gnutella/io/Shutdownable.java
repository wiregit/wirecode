package com.limegroup.gnutella.io;

// Edited for the Learning branch

/**
 * Marks the class as being able to be shut down.
 * Classes that implement this interface need to write one method, named shutdown, which code can call to shut the object down.
 * When you write this method, make sure to never throw an exception.
 * 
 * Shutting a class down should have it release any resources it was keeping.
 * The action of shutting down should propegate down to any contained objects that also need to be shut down.
 */
public interface Shutdownable 
{
    
    /** Tell this object to release its resources. This will never throw an exception. */
    void shutdown();
}
