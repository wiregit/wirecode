
// Commented for the Learning branch

package com.limegroup.gnutella.io;

import java.io.IOException;

/**
 * NIO can command you to read, handleRead().
 * 
 * An object implements ReadObserver so NIO can call its handleRead() method when it's time for it to read.
 * 
 * NIO calls handleRead() because of a SelectableChannel at the end of the read chain.
 * Use this line of code to get NIO to stop calling it:
 * NIODispatcher.instance().interestRead(channel, false);
 */
public interface ReadObserver extends IOErrorObserver {

    /**
     * NIO will call handleRead() when the SelectableChannel at the end of the read chain has data from the remote computer.
     * In this handleRead() method, you should have code that reads data from your source.
     */
    void handleRead() throws IOException;
}
