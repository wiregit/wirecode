
// Commented for the Learning branch

package com.limegroup.gnutella.io;

/**
 * NIO can command you to get data and write, handleWrite().
 * 
 * An object implements WriteObserver so NIO can call its handleWrite() method when it's time for it to write.
 * 
 * NIO calls handleWrite() because of a SelectableChannel at the end of the write chain.
 * Use this line of code to get NIO to stop calling it:
 * NIODispatcher.instance().interestWrite(channel, false);
 */
public interface WriteObserver extends IOErrorObserver {

    /**
     * NIO will call handleWrite() when the SelectableChannel at the end of the write chain is ready for us to send some data to the remote computer.
     * In this handleWrite() method, you should have code that reads data from your source, processes it somehow, and writes it to your sink.
     * 
     * @return True if this object filled its sink, and is still holding more data it needs to send.
     *         False if it wrote everything it had and is empty.
     */
    boolean handleWrite() throws java.io.IOException;
}
