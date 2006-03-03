
// Commented for the Learning branch

package com.limegroup.gnutella.connection;

import java.nio.ByteBuffer;
import java.io.IOException;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.util.BufferByteArrayOutputStream;

/**
 * Give a MessageWriter Gnutella packets and it will send them to the remote computer.
 * 
 * Here's how to send a Gnutella packet.
 * Make a MessageWriter object, and call the send(Message m) method on it.
 * The MessageWriter object puts the message in its message queue.
 * Then, it turns the message into data and writes it into a buffer named out.
 * Finally, it writes the contents of the out buffer to the channel, which is the next object in the write chain.
 * 
 * This MessageWriter is the start of a chain of writers that send data out to a remote computer.
 * As a member of the writer chain, a MessageWriter can't just write when it wants to.
 * Instead, it connects to express an interest in writing, and then the next object in the chain calls it when it wants some data.
 * 
 * A MessageWriter keeps the messages it sends in a message queue.
 * If there are too many messages in the queue or some messages are too old, the queue can decide which to drop.
 * Code in this class takes statistical information from the queue, and gives it to a statistics object.
 * 
 * This class uses non-blocking IO.
 * All methods will return quickly.
 * 
 * Extends and Implements
 * ChannelWriter: This object has a sink channel it writes to, setWriteChannel() and getWriteChannel().
 * WriteObserver: NIO can tell this object to get data and write now, handleWrite().
 * OutputRunner:  This is the only class that implements OutputRunner, which gives it a send(m) method.
 */
public class MessageWriter implements ChannelWriter, OutputRunner {

    /**
     * The queue that holds the messages we are going to write.
     * A MessageQueue object takes care of the messages it holds.
     * It deletes messages that grow too old, and kills the less important ones if it's holding too many.
     */
    private final MessageQueue queue;

    /**
     * The output stream that this MessageWriter object writes Gnutella packets into.
     * 
     * Call write(source) on a BufferByteArrayOutputStream to give it more data.
     * It has a ByteBuffer inside, and will trade it in for a bigger one when necessary.
     * Call writeTo(destination) to get all the data out.
     */
    private final BufferByteArrayOutputStream out;

    /**
     * A statistics object that counts how many Gnutella packets this MessageWriter sends, drops, and more.
     */
    private final ConnectionStats stats;

    /**
     * The ManagedConnection object that represents the remote computer we're sending packets to.
     * 
     * ManagedConnection is the only class in LimeWire that implements the SentMessageHandler interface.
     * This MessageWriter class is the only one that referes to a ManagedConnection as a SentMessageHandler.
     * 
     * The handleWrite() method gets messages from this MessageWriter's message queue and writes them to the out buffer.
     * After it puts on in the out buffer, it calls sendHandler.processSentMessage(m).
     * This lets the SentMessageHandler see all the messages we are sending out.
     */
    private final SentMessageHandler sendHandler;
    
    /**
     * The channel we write data into.
     * This is also called the sink channel because we sink data into it.
     * 
     * We can't just write data into channel, we have to be invited to write.
     * We do this by calling channel.interest(this, true), making the channel link back to us.
     * Some time later, it will call our handleWrite() method.
     * Code there writes to the channel by calling channel.write(b);
     * 
     * InterestWriteChannel isn't a type of object, it's an interface.
     * Any object that implements the InterestWriteChannel interface can be stored here.
     * It's probably not really a channel, but just some object that has a write method.
     * It will take the data, compress it or slow it down, and then write it to the next object in the write chain.
     */
    private InterestWriteChannel channel;

    /**
     * The BufferByteArrayOutputStream out contains a ByteBuffer you can get with out.buffer().
     * This boolean, flipped, keeps track of what the position and limit in that buffer identify.
     * At the start, flipped is false because the position and limit in the buffer clip out empty space.
     * The writeRemaining method will flip the buffer to make position and limit clip out the data before the empty space.
     * When it does this, it changes flipped to true.
     * When all the data has been emptied to the channel, the method clears the buffer and sets flipped back to false again.
     * 
     * TODO: A flipped boolean like this one should be refactored into the object that contains the buffer, BufferByteArrayOutputStream
     */
    private boolean flipped = false;

    /**
     * True when the Shutdown method has run once to shut this object down.
     * When this is true, we stop accepting incoming messages and stop writing them.
     */
    private boolean shutdown = false;

    /**
     * Make a new MessageWriter with the given statistics, message queue, and message handler objects.
     * The new MessageWriter won't have a channel to write to.
     * Make sure you give it one with setWriteChannel(channel) before code calls the handleWrite method here.
     * 
     * @param stats       A ConnectionStats object this one can us to count the packets it writes
     * @param queue       The MessageQueue that holds the messages we're going to write
     * @param sendHandler The ManagedConnection object that represents the remote computer we're sending this packet to
     */
    public MessageWriter(ConnectionStats stats, MessageQueue queue, SentMessageHandler sendHandler) {

    	// Pass all the objects along except for the write channel
        this(stats, queue, sendHandler, null);
    }

    /**
     * Make a new MessageWriter with the given statistics, message queue, and message handler objects.
     * Also give it the channel it will write to, the sink.
     * 
     * @param stats       A ConnectionStats object this one can us to count the packets it writes
     * @param queue       The MessageQueue that holds the messages we're going to write
     * @param sendHandler The ManagedConnection object that represents the remote computer we're sending this packet to
     * @param sink        The channel we write data into
     */
    public MessageWriter(ConnectionStats stats, MessageQueue queue, SentMessageHandler sendHandler, InterestWriteChannel sink) {

    	// Save references to the objects in this new one
        this.stats       = stats;
        this.queue       = queue;
        this.sendHandler = sendHandler;

        // Set the channel this object will write to
        this.channel = sink;

        // Make a new BufferByteArrayOutputStream that can grow to hold the data of the packets we will send
        out = new BufferByteArrayOutputStream();
    }

    /**
     * Get the channel this MessageWriter is writing to, the sink channel.
     * This is an object that implements the InterestWriteChannel interface.
     * 
     * @return The channel this MessageWriter writes to
     */
    public synchronized InterestWriteChannel getWriteChannel() {

    	// Return the reference we saved
        return channel;
    }
    
    /**
     * Set or change the channel this object writes to, the sink channel.
     * Takes an object that implements the InterestWriteChannel interface.
     * 
     * @param channel The channel you want this MessageWriter to write to.
     */
    public synchronized void setWriteChannel(InterestWriteChannel channel) {

    	// Link our channel reference forward to the channel object
        this.channel = channel;
        
        // Have the channel link its observer reference back to us
        // This will let it call our handleWrite() method when it wants us to write some data to it
        channel.interest(this, true); // This also creates backlinks all down the chain of writers
    }

    /**
     * Takes a Gnutella message.
     * Adds it to the queue of messages that this MessageWriter keeps.
     * From there, it will eventually be sent.
     * 
     * Communicates statistics from the queue object to the statistics object.
     * Has the channel we write to link back to us so it will call our handleWrite() method when it wants data from us.
     * 
     * @param m A Gnutella message to send
     */
    public synchronized void send(Message m) {

    	// If this MessageWriter object is being shut down, leave without doing anything
    	if (shutdown) return;

    	// Count one more packet sent
        stats.addSent();

        // Add the packet to the queue
        queue.add(m);

        // Ask the queue how many packets it dropped, and tell it to start counting up from 0 again
        int dropped = queue.resetDropped();
        stats.addSentDropped(dropped); // Give that information to the statistics object

        // If we have a write channel, have it link it's observer reference back to us
        if (channel != null) channel.interest(this, true); // When it wants data, it will call our handleWrite() method
    }

    /**
     * Send all the Gnutella packets that we're holding into the channel.
     * 
     * Other classes that implement handWrite() have it call observer.handleWrite() to get more data.
     * That's not done here, a MessageWriter doesn't ask for packets from the rest of the program.
     * It just turns the packets it already has into data, and writes them to the channel.
     * 
     * Writing has to stop when:
     * We run out of packets, Message m = queue.removeNext() returns null instead of a packet.
     * The channel runs out of space, writeRemaining() returns true because out still has data.
     * If handleWrite stops because it ran out of packets, it returns false.
     * If handleWrite stops because it filled the sink channel, it returns true.
     * 
     * The WriteObserver interface requires this method.
     * The channel we write to calls it to get us to send it data.
     * 
     * @return True if we still have Gnutella packets to send, false if we emptied all our data into the channel
     */
    public synchronized boolean handleWrite() throws IOException {

    	// Make sure we have a channel to write to
        if (channel == null) throw new IllegalStateException("writing with no source.");

        // Move data from our out buffer into the channel
        if (writeRemaining()) {

        	// The channel couldn't take everything we have for it, there is still data in the out buffer
        	return true; // This MessageWriter object is still holding data it needs to send
        }

        // We emptied our out buffer into the channel

        // then loop through and write to the channel till we can't anymore.
        while (true) {

        	// Get one message from the message queue
            Message m = queue.removeNext();

            // Get the dropped count from the queue to the statistics object
            int dropped = queue.resetDropped(); // Ask the queue how many mesages it dropped, and tell it to start counting up from 0 again
            stats.addSentDropped(dropped);      // Tell the statistics object this number

            // The queue gave us nothing, there are no more messages to send
            if (m == null) {

            	// Have our channel stop linking back to us
                channel.interest(this, false); // This means it won't call our handleWrite() method anymore

                // We wrote all the Gnutella packets the queue had, return false
                return false;
            }

            // Write the message into the output buffer
            m.writeQuickly(out);

            // Give the message to the ManagedConnection object that represents the remote computer we just sent it to, which will measure it for statistics
            sendHandler.processSentMessage(m);

            // Move data from our out buffer into the channel
            if (writeRemaining()) {

            	// The channel couldn't take everything we have for it, there is still data in the out buffer
            	return true; // This MessageWriter object is still holding data it needs to send
            }
        }
    }

    /**
     * Moves data from the out buffer into the channel this MessageWriter writes to.
     * Tries to move all the data from out to channel, but can only write as much as the channel accepts.
     * Returns true if there is still data in out to send, false if we sent everything we had.
     * 
     * This object has a ByteBufferArrayOutputStream, a type LimeWire defines.
     * This type holds data in a ByteBuffer.
     * The code here never calls compact on the ByteBuffer to shift the data back to the start.
     * Instead, it empties the buffer and then marks it blank to start over.
     * This is faster.
     * 
     * @return True if there is still data in out left to write, false if we emptied it into the channel
     */
    private boolean writeRemaining() throws IOException {

    	// If this MessageWriter is being shut down, don't write any more packets
        if (shutdown) throw new IOException("connection shut down.");

        // Get a reference to the ByteBuffer inside the BufferByteArrayOutputStream
        ByteBuffer buffer = out.buffer();

        // If we've already flipped the buffer so position and limit clip around the data, or
        // Position and length still clip around empty space, and we've written some data in the buffer that has moved position forward
        if (flipped || buffer.position() > 0) {

        	// If position and limit clip around the empty space at the end
            if (!flipped) {

            	// Flip the buffer to have them clip around the data at the start
                buffer.flip();
                flipped = true; // Record that position and length clip around the data now
            }

            // Move the data from the buffer into the channel
            channel.write(buffer); // Moves position forward past the data in the buffer

            // The write method didn't move position up to limit, it didn't take all the data
            if (buffer.hasRemaining()) {

            	// Report we still have data to write
            	return true;
            }

            // Reset the buffer
            flipped = false; // Position and limit will clip out empty space again
            buffer.clear();  // Move position to the start and limit to the end
        }

        // Report that we wrote all of the data in out into the channel
        return false;
    }

    /**
     * Not used, we'll shut down from reading.
     * 
     * It's important that this method doesn't close the connection.
     * 
     * The Shutdownable interface requires this method.
     * Frees the resources this object and its component objects were using.
     */
    public synchronized void shutdown() {

    	// Set shutdown to true to make this method stop accepting and writing Gnutella packets
        shutdown = true;
    }

    /**
     * This method is not used.
     * 
     * The IOErrorObserver interface requires this method.
     * It makes this object able to handle IO exceptions.
     */
    public void handleIOException(IOException e) {

    	// Wrap the given IOException as a RuntimeException named "Unsupported", and throw it again
    	throw new RuntimeException("Unsupported", e);
    }
}
