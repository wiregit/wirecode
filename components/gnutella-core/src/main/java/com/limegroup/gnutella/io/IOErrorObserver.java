
// Commented for the Learning branch

package com.limegroup.gnutella.io;

import java.io.IOException;

/**
 * An object that implements IOErrorObserver has a handleIOException(e) method NIODispatcher will call with an exception.
 * 
 * IOErrorObserver requires the method handleIOException().
 * AcceptObserver, ConnectObserver, ReadObserver, and WriteObserver extend IOErrorObserver.
 * They add methods like handleAccept(), handleConnect(), handleRead(), and handleWrite().
 * So, an object that implements ReadObserver has handleRead() and handleIOException() methods.
 * 
 * Call NIODispatcher.registerRead(channel, ReadObserver) to register a channel with the NIO selector object.
 * Later, NIO will tell NIODispatcher.process() that the channel has data for us to read.
 * Code in NIODispatcher will call handleRead() on the ReadObserver we gave when we registered the channel.
 * 
 * If a Java NIO method throws an exception when we're making a call on the channel, this object is also used.
 * ReadObserver extends IOErrorObserver, so the object also has a handleIOException(e) method.
 * Code in NIODispatcher calls handleIOException(e) on the object, giving it the exception.
 */
public interface IOErrorObserver extends Shutdownable {

    /**
     * Code in the NIODispatcher class will call handleIOException(e) to give this object an exception.
     * 
     * An object that implements IOErrorObserver is passed along with a channel so both get registered with the NIO selector.
     * When NIO says the channel is ready for some operation, code in NIODispatcher calls a method on the object.
     * If a NIO call throws an exception, code in NIODispatcher calls handleIOException(e) on the object, giving it the exception.
     * 
     * @param iox The IOException that the an Java NIO method threw us
     */
    void handleIOException(IOException iox);
}
