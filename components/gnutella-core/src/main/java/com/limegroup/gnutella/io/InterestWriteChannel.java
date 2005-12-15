
// Edited for the Learning branch

package com.limegroup.gnutella.io;

import java.nio.channels.WritableByteChannel;

/**
 * The object that gives you data can tell you it has some, interest().
 * NIO can command you to get data and write, handleWrite().
 * 
 * 
 * 
 * 
 * A channel that can be written to, can receive write events of when writing
 * on this channel is capable, and can forward these events to other chained
 * WriteObservers.
 * 
 * A channel that you can write to.
 * A channel that can get write events when it wants data written to it.
 * A channel that can forward these write events to other chained WriteObservers
 * 
 * (do)
 * 
 * You can give data to an object that implements InterestWriteChannel, but only when it wants some.
 * First, call interest(this, true) on it. It will link back to you.
 * When it wants some data, it will call your handleWrite method.
 * In your handleWrite method, give it data by calling its write method.
 * 
 * If you want data for writing, implement the InterestWriteChannel interface.
 * An object that can give you data will call your interest method so you can link back to it.
 * When you want some data, call its handleWrite method.
 * It will call your write method to give you data.
 * 
 */
public interface InterestWriteChannel extends WritableByteChannel, WriteObserver {
    
    /**
     * Marks the given observer as interested (or not interested, if status is false)
     * in knowing when a write can be performed on this channel.
     * 
     * 
     * If you are an object and you want to write.
     * You've linked your channel to the object you want to write to, the sink.
     * In one of your methods, call channel.interest(this, true).
     * This will cause the sink to link its observer reference back to you.
     * 
     * Later, you don't want to be called to write anymore.
     * Call channel.interest(null, false);
     * 
     * If the second parameter is false, it doesn't matter what the first one is.
     * All false does is null the observer reference.
     * 
     * A true call runs down the chain of writers, creating all the observer backlinks.
     * A false call disconnects the first observer backlink, and then runs down the rest of the chain setting up new backlinks.
     * 
     * 
     * 
     */
    public void interest(WriteObserver observer, boolean status);
}