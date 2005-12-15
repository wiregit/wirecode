
// Commented for the Learning branch

package com.limegroup.gnutella.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Give your NIOSocket a NIOOutputStream, and use it to send the remote computer data.
 * 
 * Here's how to send data to the remote computer.
 * Make a NIOOutputStream object, which will make a BufferOutputStream object.
 * On the NIOOutputStream object, call getOutputStream(), and it will return a reference to the BufferOutputStream object.
 * On that BufferOutputStream object, call write(b), and it will move data from b into the 8 KB buffer the NIOOutputStream and BufferOutputStream objects share.
 * NIO will call handleWrite() when the channel wants some data, and handleWrite() will move data from the buffer to the channel.
 * 
 * As the program runs, it makes a NIOSocket for each remote computer it's connected to.
 * A NIOSocket creates a NIOOutputStream object, which creates a BufferOutputStream object.
 * These 3 objects are among the small group of interconnected objects the program makes for each remote computer.
 * 
 * NIOOutputStream is closely related to BufferOutputStream.
 * The classes both keep references to a 8 KB ByteBuffer, and an object to synchronize on when accessing it.
 * To send data to the remote computer, call write(b) on the BufferOutputStream, and it will put it in the buffer.
 * Then, the NIODispatcher will call handleWrite() here, and this method will move data from the buffer into the channel.
 * 
 * NIOOutputStream implements the WriteObserver interface.
 * This means that it has a handleWrite() method.
 * Here, handleWrite() moves data from the buffer to the channel.
 * 
 * Extends and Implements
 * WriteObserver: NIO can tell this object to get data and write now, handleWrite().
 */
class NIOOutputStream implements WriteObserver {

    /*
     * When the program connects to a remote computer, it makes an NIOSocket object.
     * This object contains the Socket and SocketChannel we communicate with the remote computer through.
     * 
     * The program also makes NIOInputStream and NIOOutputStream objects to help read and write data.
     * 
     * This NIOOutputStream object keeps a reference that points back to the NIOSocket that made it, handler.
     * It also keeps channel, the SocketChannel related to the connection Socket object inside NIOSocket.
     */

    /** The NIOSocket object that made this NIOOutputStream object. */
    private final NIOSocket handler;
    /** The SocketChannel associated with the connection socket in the NIOSocket object. */
    private final SocketChannel channel;

    /*
     * NIOOutputStream is the only class in the program that makes a BufferOutputStream object.
     * The two classes are closely related, and both keep references to some of the same objects.
     * They share a buffer, for instance, and also an object to lock on before touching the buffer.
     * 
     * Here's how BufferOutputStream works.
     * Make one, giving it the buffer and the channel to write to.
     * Then, call write(b) on it to have it take data and store it in its buffer.
     * It will use NIO to express interest in writing to the channel.
     * When NIO says it's time to write, NIODispatcher calls the handleWrite() method here.
     * The handleWrite() method moves data from the buffer into the channel.
     * 
     * The BufferOutputStream is named sink.
     * The Object bufferLock and the ByteBuffer buffer we and the BufferOutputStream will share.
     * Hold the data in buffer and make sure only one thread touches it with bufferLock.
     */

    /** A LimeWire BufferOutputStream object we'll write to, and then move data from it to the channel. */
    private BufferOutputStream sink; // This is named sink because it's the object this one writes to

    /**
     * Lock on this object before touching the buffer here and in the BufferOutputStream object.
     * The BufferOutputStream object will make an object to lock on, and then we'll get a reference to it.
     */
    private Object bufferLock;

    /** A buffer we keep here and in the BufferOutputStream object */
    private ByteBuffer buffer;
    
    /** True when this object has been shut down. */
    private boolean shutdown;

    /**
     * NIOSocket makes a NIOOutputStream object to help it send data to the remote computer.
     * This is the only way NIOOutputStream is made and used.
     * 
     * This constructor just saves the given NIOSocket and SocketChannel in the new object.
     * Call init() later to actually get things set up.
     * 
     * @param handler The NIOSocket object that is making this one
     * @param channel The SocketChannel that goes with the Socket inside NIOSocket, and that we'll use to write to the remote computer
     */
    NIOOutputStream(NIOSocket handler, SocketChannel channel) throws IOException {
        
        // (ask) why are there no keywords at all before the constructor name

        // Save both objects here, the init() method will actually set things up
        this.handler = handler;
        this.channel = channel;
    }
    
    /**
     * Make a BufferOutputStream object this NIOOutputStream will work with to send data to the remote computer.
     */
    synchronized void init() throws IOException {

        // Make sure we only initialize this object once, and before this object is shut down
        if (buffer != null) throw new IllegalStateException("already init'd!");
        if (shutdown) throw new IOException("already closed!");

        // Get a new empty 8 KB ByteBuffer from the stack of them NIOInputStream keeps
        this.buffer = NIOInputStream.getBuffer();

        // Make a new BufferOutputStream object that can hold data before NIO takes it
        sink = new BufferOutputStream(
            buffer,   // The data we write to our BufferOutputStream will go here
            handler,  // Give it a reference to the NIOSocket object that all this is for
            channel); // The handleWrite() method here will move the data from buffer into this channel

        // The BufferOutputStream made the object we'll both lock on to access the buffer
        bufferLock = sink.getBufferLock(); // Save a reference to it here also
    }

    /**
     * Get the BufferOutputStream object you can call write(b) on to send data to the remote computer.
     * 
     * @return The BufferOutputStream object we created to work closely with this one
     */
    synchronized OutputStream getOutputStream() throws IOException {

        // If we haven't setup this object yet, run init()
        if (buffer == null) init(); // Once done, we'll have a buffer and BufferOutputStream sink

        // Return the BufferOutputStream closely related to this NIOOutputStream object
        return sink;
    }

    /**
     * NIODispatcher.process() calls this method when NIO wants us to write data to the SocketChannel named channel.
     * When this happens, this method's job is to write everything it can into the channel.
     * 
     * BufferOutputStream has write(b) methods that give it data.
     * It puts the data in the buffer.
     * Then, NIO calls this handleWrite() method when our channel wants data.
     * This method moves data from the buffer into the channel.
     * 
     * @return False if we moved all the data from our buffer into the channel, and told it we're not interested in writing anymore.
     *         True if the buffer filled the channel and we still have more data in our buffer to go out.
     *         In this case, we didn't cancel our writing interest.
     */
    public boolean handleWrite() throws IOException {

        // Only let one thread at a time access the buffer, which is also used in the BufferOutputStream this object made
        synchronized (bufferLock) {
            
            /*
             * Before, the position and limit in the 8 KB buffer clipped out empty space.
             * Calling flip() moves them to clip out the data in the buffer before the empty space.
             */

            // Move position and limit to clip out the data in the buffer before the empty space
            buffer.flip();

            // Loop until the buffer doesn't have any data left
            while (buffer.hasRemaining()) {

                /*
                 * Tour Point
                 * 
                 * This is where LimeWire actually writes data to the remote computer.
                 * channel is a java.nio.channels.SocketChannel object.
                 */

                // Move data from the buffer into the channel
                int written = channel.write(buffer);

                /*
                 * The write method moved the position in the buffer forward past the data it took from it.
                 * It returned the number of bytes it wrote, which is also the distance it moved the position.
                 * 
                 * If write can't write any data, it returns 0.
                 * If the channel has reached the end somehow, write returns -1.
                 */

                // Unable to write, leave the loop
                if (written <= 0) break;
            }

            /*
             * Now we un-flip the buffer.
             * There should probably be a method in buffer that does this, it takes quite a bit of code.
             * Any data left in the buffer is slid to the start, and position and limit again clip out the free space after that.
             */

            // The buffer's position isn't at the start
            if (buffer.position() > 0) {

                // The buffer's position isn't at the start, and it clips out some data
                if (buffer.hasRemaining()) {

                    // Shift the data in the buffer to the start, this actually moves memory as well as changing position and limit
                    buffer.compact(); // Now position is 0

                // The position and limit in the buffer are squeezed together, but not at the start
                } else {

                    // The whole thing is free space
                    buffer.clear();
                }

            // The buffer's position is at the start
            } else {

                // Un-flip the buffer, making the position and limit clip around the space after the data again
                buffer.position(buffer.limit()); // Set the position where the limit is, at the end of the data
                buffer.limit(buffer.capacity()); // Set the limit to the end of the buffer
            }

            /*
             * There is very likely free space in the buffer now.
             * If so, we're interested in getting more data written to us.
             * 
             * A thread that called write(b) on our BufferOutputStream may be stuck on LOCK.wait() there.
             * Call bufferLock.notify() to wake it up.
             * Then, it will write more data into our buffer.
             * LOCK in BufferOutputStream and bufferLock here are the same object.
             */

            // Wake up a thread sleeping in BufferOutputStream.write(b) so it will put more data into the buffer
            if (buffer.hasRemaining()) bufferLock.notify();
            
            /*
             * The other thread won't be able to write right away, though, because we still are still synchronized on bufferLock
             */
            
            /*
             * If we were able to write everything, we're not interested in more writing.
             * Otherwise, we are interested.
             */

            // We emptied the entire buffer into the channel
            if (buffer.position() == 0) {

                // Tell the channel we are no longer interested in writing to it, and return false
                NIODispatcher.instance().interestWrite(channel, false);
                return false;

            // We filled the channel, and still have some more data in our buffer to give it
            } else {

                // Return true to indicate that we haven't cancelled our interest as a writer to the channel
                return true;
            }
        }
    }

    /**
     * Has the BufferOutputStream shut itself down, and puts the 8 KB ByteBuffer back on NIOInputStream's static stack of them.
     * This shutdown() method doesn't close the SocketChannel, because NIOSocket will take care of that.
     */
    public synchronized void shutdown() {

        // Make sure this method only runs once
        if (shutdown) return;

        // If we have a BufferOutputStream, tell it to shut itself down
        if (sink != null) sink.shutdown();

        // Mark this object as shut down
        shutdown = true;

        // We got a 8 KB ByteBuffer from the stack NIOInputStream keeps
        if (buffer != null) {

            // Mark it clear and put it back on the stack of empty ones
            buffer.clear();                    // Moves position to the start and limit to the end
            NIOInputStream.CACHE.push(buffer); // Put the buffer back on CACHE, the static stack of 8 KB ByteBuffers the program keeps
        }
    }

    /** IOErrorObserver requires this method, but the program doesn't use it. */
    public void handleIOException(IOException iox) {

        // Conver it into a RuntimeException
        throw new RuntimeException("unsupported operation", iox);
    }
}
