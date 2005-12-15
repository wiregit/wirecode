
// Commented for the Learning branch

package com.limegroup.gnutella.io;

/**
 * NIO can command you to read, handleRead(), and NIO can command you to get data and write, handleWrite().
 * 
 * ReadWriteObserver is an interface that combines the ReadObserver and WriteObserver interfaces.
 * Instead of having your class implement ReadObserver, WriteObserver, it can just implement ReadWriteObserver.
 * 
 * The ReadWriteObserver allows an object to be passed around identified as supporting commands to read and and commands to write.
 * 
 * In NIODispatcher, the registerReadWrite(channel, ReadWriteObserver) method takes an object that implements this interface.
 * This means the NIODispatcher code will be able to call handleRead(), handleWrite() or handleIOException() on the object when necessary.
 */
public interface ReadWriteObserver extends ReadObserver, WriteObserver {}
