
// Commented for the Learning branch

package com.limegroup.gnutella.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Make a BufferOutputStream, write data to it, and have NIOOutputStream.handleWrite move data from the buffer to the channel.
 * 
 * Only one object makes a new BufferOutputStream, NIOOutputStream.
 * The two classes share a lot of objects and are very closely related.
 * 
 * The constructor is BufferOutputStream(buffer, handler, channel).
 * buffer is a ByteBuffer from the stack of 8 KB buffers that NIOOutputStream made.
 * The Socket and SocketChannel we're talking to the remote computer through are handler and channel.
 * 
 * Once NIOOutputStream makes a BufferOutputStream, here's how it uses it.
 * It calls write(b) on it to give it data.
 * Then, NIO calls NIOOutputStream.handleWrite(), which empties the buffer into the channel.
 */
class BufferOutputStream extends OutputStream implements Shutdownable {

    /** Apache debugging log, not used. */
    private static final Log LOG = LogFactory.getLog(BufferOutputStream.class);

    /** Lock on this object when writing to the ByteBuffer you gave to the BufferOutputStream constructor. */
    private final Object LOCK = new Object();

    /**
     * The NIOSocket object that goes with the NIOOutputStream that made this BufferOutputStream.
     * If you have a BufferOutputStream object and call close() on it, it calls handler.close().
     */
    private final NIOSocket handler;

    /**
     * When you write data to this BufferOutputStream object, it puts it in this buffer.
     * Later, when NIO tells this object it wants data, the data moves from the buffer into the channel.
     */
    private final ByteBuffer buffer;

    /**
     * The SocketChannel object that goes with the NIOSocket, NIOOutputStream, and this BufferOutputStream.
     * When the buffer has data to write, we'll call NIODispatcher.interestWrite(channel, true) to let the channel know we have data for it.
     */
    private final SelectableChannel channel;

    /** True after shutdown() has been called. */
    private boolean shutdown = false;

    /**
     * Make a BufferOutputStream for a NIOOutputStream.
     * Only one place in the code makes a new BufferOutputStream, the NIOOutputStream init() method.
     * 
     * @param buffer  A new empty 8 KB ByteBuffer from the NIOInputStream cache
     * @param handler The LimeWire NIOSocket object this NIOOutputStream and BufferOutputStream are for
     * @param channel The SocketChannel that goes with the connection socket inside the NIOSocket object
     */
    BufferOutputStream(ByteBuffer buffer, NIOSocket handler, SelectableChannel channel) {

        // Save the given objects in this object also
        this.handler = handler; // In close(), we'll call handler.shutdown()
        this.buffer  = buffer;  // The write(b) methods here will move data from b into this buffer
        this.channel = channel; // NIOOutputStream.handleWrite() moves the data from buffer to channel
    }

    /**
     * Get the lock you have to synchronize on before touching the buffer you gave to this BufferOutputStream.
     * 
     * Lock on this object when writing this BufferOutputStream.
     * We synchronize on this lock when touching the buffer.
     * The buffer is shared, so other objects need to synchronize on the same lock when they touch the buffer also.
     * 
     * Only NIOOutputStream.init() calls this method.
     * 
     * @return The Object this BufferOutputStream made as a thread synchronization lock
     */
    Object getBufferLock() {

        // Return the lock that protects the buffer
        return LOCK;
    }

    /**
     * Takes a single byte, and writes it into the buffer.
     * From there, NIOOutputStream.handleWrite() will move it from the buffer out to the channel.
     * 
     * @param x A number this method will read as a byte and add to the buffer
     */
    public void write(int x) throws IOException {

        // Only let one thread at a time access the buffer, which is also used in the NIOOutputStream object that made this one
        synchronized (LOCK) { // This is the object we made to protect the ByteBuffer buffer

            // Wait until there is some free space in the buffer
            waitImpl(); // When NIO calls NIOOutputStream.handleWrite(), the buffer will get some free space 

            // Add the byte to the buffer
            buffer.put((byte)(x & 0xFF)); // Only look at the part of the int that is 1 byte big

            // There's data in the buffer now, tell the channel we're interested in writing to it
            NIODispatcher.instance().interestWrite(channel, true);
        }
    }

    /**
     * Takes an array of bytes, and writes them into the buffer.
     * From there, NIOOutputStream.handleWrite() will move them from the buffer out to the channel.
     * 
     * @param buf An array of bytes
     * @param off The distance into the array to start taking data
     * @param len The number of bytes from there to take
     */
    public void write(byte[] buf, int off, int len) throws IOException {

        // Only let one thread at a time access the buffer, which is also used in the NIOOutputStream object that made this one
        synchronized (LOCK) {

            // Loop until there are no more bytes to write
            while (len > 0) {

                // Wait until there is some free space in the buffer
                waitImpl(); // When NIO calls NIOOutputStream.handleWrite(), the buffer will get some free space

                // Calculate how much data to move across
                int available = Math.min( // Whichever is smaller
                    buffer.remaining(),   // The amount of free space in the buffer, or
                    len);                 // The amount of data we have left to write

                // Copy the data from the given array into the buffer
                buffer.put(     // buffer is the destination
                    buf,        // buf is the source
                    off,        // Start from here in the array
                    available); // Take this many bytes

                // Adjust our copy of the offset and length to clip around the data in the array we still need to write
                off += available; // Move our record of the offset in the given array past the data we just wrote
                len -= available; // Make the length less by the same distance

                // There's data in the buffer now, tell the channel we're interested in writing to it
                NIODispatcher.instance().interestWrite(channel, true);
            }
        }
    }

    /**
     * Forces all the data we wrote into the buffer to be sent away into the channel.
     * 
     * This call still blocks, but it doesn't actually force any data into the channel.
     * In NIO, you can't force a write.
     * All you can do is wait until NIO asks for the data, which is what this method does.
     */
    public void flush() throws IOException {

        // Only let one thread at a time access the buffer, which is also used in the NIOOutputStream object that made this one
        synchronized (LOCK) {

            /*
             * Since that adds no data to the buffer, we do not need to interest a write.
             * This simply waits until the existing buffer is empties into the TCP stack,
             * via whatever mechanism normally clears the buffer (via writes).
             */

            // Loop until the buffer is empty
            while (buffer.position() > 0) { // When the buffer is empty, it's position will be 0, at the start

                // After this BufferOutputStream object has been shut down, calling flush() will cause an IOException
                if (shutdown) throw new IOException("socket closed");

                try {

                    /*
                     * Wait on the buffer lock.
                     * The thread that makes it here will block on wait().
                     * Here's some code from NIOOutputStream.handleWrite().
                     * 
                     *   // If there's room in the buffer, we're interested in reading.
                     *   if(buffer.hasRemaining()) bufferLock.notify();
                     * 
                     * When another thread runs this code, the notify() call wakes up this thread.
                     * LOCK and bufferLock are the same object.
                     */

                    // Wait here until NIOOutputStream.handleWrite() makes some free space in the buffer
                    LOCK.wait();

                // The program called thread.interrupt()
                } catch (InterruptedException ix) {

                    // Wrap the exception and throw it
                    throw new InterruptedIOException(ix);
                }
            }
        }
    }

    /**
     * Waits until there is space in the buffer to write to.
     * When NIO calls NIOOutputStream.handleWrite(), the buffer will get some free space.
     */
    private void waitImpl() throws IOException {

        // Loop until the buffer has some free space (do)
        while (!buffer.hasRemaining()) {

            /*
             * The hasRemaining() method returns false when position and limit meet in the buffer.
             * Before the buffer is flipped, this means it has no more space.
             * After the buffer is flipped, this means it has no more data.
             * NIOOutputStream.handleWrite() flips the buffer.
             */

            // After this BufferOutputStream object has been shut down, calling waitImpl() will cause an IOException
            if (shutdown) throw new IOException("socket closed");
                
            try {

                // Wait here until NIOOutputStream.handleWrite() makes some free space in the buffer
                LOCK.wait();

            // The program called thread.interrupt()
            } catch (InterruptedException ix) {

                // Wrap the exception and throw it
                throw new InterruptedIOException(ix);
            }
        }

        // Before returning, make sure the program hasn't shut this object down
        if (shutdown) throw new IOException("socket closed");
    }

    /**
     * Call the shutdown() method on the NIOSocket the program made this BufferOutputStream for.
     * 
     * This overrides java.io.OutputStream.close().
     */
    public void close() throws IOException {

        // Call the shutdown() method on the NIOSocket the program made this BufferOutputStream for
        NIODispatcher.instance().shutdown(handler);
    }

    /**
     * Wake up any threads that called LOCK.wait().
     * 
     * The Shutdownable interface requires this method.
     */
    public void shutdown() {

        // Only let one thread at a time access the buffer, which is also used in the NIOOutputStream object that made this one
        synchronized(LOCK) {

            // Mark this object as shut down
            shutdown = true;

            // Wake up any threads that called LOCK.wait();
            LOCK.notify();
        }
    }
}
