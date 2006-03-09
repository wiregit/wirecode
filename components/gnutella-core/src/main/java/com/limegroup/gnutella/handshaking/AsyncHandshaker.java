package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.InterestWriteChannel;

/**
 * Handshaking class that iterates through the available states and sets the
 * appropriate interest on the channel depending on the state.
 */
class AsyncHandshaker implements ChannelReadObserver, ChannelWriter, InterestReadChannel {
   
    /** The Handshaker controlling this. */
    private Handshaker shaker;
    /** Observer to notify when this finishes or fails. */
    private HandshakeObserver handshakeObserver;
    /** The states this will use while handshaking.*/
    private List /* of HandshakeState */ states;
    /** The current state. */
    private HandshakeState currentState;
    /** The sink we write to. */
    private InterestWriteChannel writeSink;
    /** The sink we read from. */
    private InterestReadChannel readSink;
    /** The ByteBuffer to use for reading. */
    private ByteBuffer readBuffer;
    /** Whether or not we've shutdown this handshaker. */
    private volatile boolean shutdown;

    /** Constructs a new AsyncHandker using the given Handshaker, HandshakeObserver, and List of states. */
    AsyncHandshaker(Handshaker shaker, HandshakeObserver observer, List states) {
        this.shaker = shaker;
        this.handshakeObserver = observer;
        this.states = states;
        this.readBuffer = ByteBuffer.allocate(2048);
        this.currentState = (HandshakeState)states.remove(0);
    }    
    
    /**
     * Notification that a read can be performed.  If our current state is for writing,
     * we'll turn off future interest events.  Otherwise we'll tell the current state
     * to process.
     */
    public void handleRead() {
        if(currentState.isWriting()) {
            readSink.interest(false);
        } else {
            processCurrentState(true);
        }
    }
    
    /**
     * Notification that a write can be performed.  If our current state is for reading,
     * we'll turn off future interest events.  Otherwise we'll tell the current state to process.
     */
    public boolean handleWrite() {
        if(currentState.isReading()) {
            writeSink.interest(this, false);
            return false;
        } else {
            return processCurrentState(false);        
        }
    }
    
    /**
     * Process the current state.  If any exceptions occur while procesing,
     * we'll notify the observer of them.  If the state indicated it needs to be
     * processed again, we do not move to the next state.  Otherwise, if the state
     * indicated that it's done, we move to the next state.
     * 
     * This will return true if we're writing and we have more to write.
     * @param reading
     * @return
     */
    private boolean processCurrentState(boolean reading) {
        if(!shutdown) {
            try {
                if (reading) {
                    if (!currentState.process(readSink, readBuffer))
                        nextState(true);
                } else {
                    if (!currentState.process(writeSink, null))
                        nextState(false);
                    else
                        return true;
                }
            } catch (NoGnutellaOkException ex) {
                shutdown = true;
                handshakeObserver.handleNoGnutellaOk(ex.getCode(), ex.getMessage());
            } catch (IOException iox) {
                shutdown = true;
                handshakeObserver.handleBadHandshake();
            }
        }
        
        return false;
    }    
    
    /**
     * Moves to the next state.
     * If there are no states left, we notify the observer that we're finished.
     * Otherwise, we'll move to the next state and change interest on our channels
     * depending on what we're currently doing and what's needed next.
     * 
     * @param reading
     */
    private void nextState(boolean reading) {
        if(states.isEmpty()) {
            readSink.interest(false);
            writeSink.interest(this, false);
            handshakeObserver.handleHandshakeFinished(shaker);
        } else {
            currentState = (HandshakeState)states.remove(0);
            if(currentState.isReading() && !reading) {
                readSink.interest(true);
                writeSink.interest(this, false);
            } else if(currentState.isWriting() && reading) {
                readSink.interest(false);
                writeSink.interest(this, true);
            }
        }
    }

    public InterestWriteChannel getWriteChannel() {
        return writeSink;
    }

    public void setWriteChannel(InterestWriteChannel newChannel) {
        this.writeSink = newChannel;
        writeSink.interest(this, true);
    }

    public InterestReadChannel getReadChannel() {
        return readSink;
    }

    public void setReadChannel(InterestReadChannel newChannel) {
        this.readSink = newChannel;
        readSink.interest(true); 
    }

    public boolean isOpen() {
        return readSink.isOpen() && writeSink.isOpen();
    }

    public void close() throws IOException {
        readSink.close();
        writeSink.close();
    }

    /**
     * Notification that this Handshaker is being shut down.
     * This may be because message looping is taking over and is notifying us that
     * we're done.  If that's the case, we do not notify the observer that we were shutdown.
     * Otherwise, we notify the observer that we were shut down.
     */
    public void shutdown() {
        synchronized(this) {
            if(shutdown)
                return;
            shutdown = true;
        }
        
        // this may be called when the message reading is installed
        if(!isOpen()) {
            handshakeObserver.shutdown();
        }
    }

    public void interest(boolean status) {
        readSink.interest(status);
    }

    /**
     * Allows another channel to read from this, passing any unread bytes to that channel.
     * This is typically used for the MessageReader to read any bytes that this AsyncHandshaker
     * read from the network but did not process during handshaking.
     */
    public int read(ByteBuffer toBuffer) throws IOException {
        int read = 0;

        if(readBuffer.position() > 0) {
            readBuffer.flip();
            int remaining = readBuffer.remaining();
            int toRemaining = toBuffer.remaining();
            if(toRemaining >= remaining) {
                toBuffer.put(readBuffer);
                read += remaining;
            } else {
                int limit = readBuffer.limit();
                int position = readBuffer.position();
                readBuffer.limit(position + toRemaining);
                toBuffer.put(readBuffer);
                read += toRemaining;
                readBuffer.limit(limit);
            }
            readBuffer.compact();
        }
        
        return read;
    }

    // unused.
    public void handleIOException(IOException iox) {}
}
