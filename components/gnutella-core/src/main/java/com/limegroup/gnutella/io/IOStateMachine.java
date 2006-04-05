package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.List;

import com.limegroup.gnutella.io.BufferUtils;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.InterestWriteChannel;

/**
 * State machine for reading & writing.
 */
public class IOStateMachine implements ChannelReadObserver, ChannelWriter, InterestReadChannel {
   
    /** Observer to notify when this finishes or fails. */
    private IOStateObserver observer;
    /** The states this will use while handshaking.*/
    private List /* of IOState */ states;
    /** The current state. */
    private IOState currentState;
    /** The sink we write to. */
    private InterestWriteChannel writeSink;
    /** The sink we read from. */
    private InterestReadChannel readSink;
    /** The ByteBuffer to use for reading. */
    private ByteBuffer readBuffer;
    /** Whether or not we've shutdown this handshaker. */
    private volatile boolean shutdown;

    public IOStateMachine(IOStateObserver observer, List states) {
        this.observer = observer;
        this.states = states;
        this.readBuffer = NIODispatcher.instance().getBufferCache().getHeap(2048);
        this.currentState = (IOState)states.remove(0);
    }
    
    /**
     * Adds a new state to process.
     * 
     * @param state
     */
    public void addState(final IOState newState) {
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                states.add(newState);
                if(states.size() == 1)
                    nextState(false, false);
            }
        });
    }
    
    /**
     * Adds a collection of new states to process.
     */
    public void addState(final List /* of IOState */ newStates) {
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                states.addAll(newStates);
                if(states.size() == newStates.size())
                    nextState(false, false);
            }
        });        
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
                        nextState(true, false);
                } else {
                    if (!currentState.process(writeSink, null))
                        nextState(false, true);
                    else
                        return true;
                }
            } catch (IOException iox) {
                shutdown = true;
                NIODispatcher.instance().getBufferCache().release(readBuffer);
                observer.handleIOException(iox);
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
    private void nextState(boolean reading, boolean writing) {
        if(states.isEmpty()) {
            readSink.interest(false);
            writeSink.interest(this, false);
            observer.handleStatesFinished();
        } else {
            currentState = (IOState)states.remove(0);
            if(currentState.isReading() && !reading) {
                if(readSink != null)
                    readSink.interest(true);
                writeSink.interest(this, false);
            } else if(currentState.isWriting() && !writing) {
                readSink.interest(false);
                if(writeSink != null)
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
        
        // this may be called when transfer is switched to another observer
        if(!isOpen()) {
            observer.shutdown();
        }
        
        NIODispatcher.instance().getBufferCache().release(readBuffer);
    }

    public void interest(boolean status) {
        readSink.interest(status);
    }

    /**
     * Allows another channel to read from this, passing any unread bytes to that channel.
     * This is typically used for the MessageReader to read any bytes that this AsyncHandshaker
     * read from the network but did not process during handshaking.
     */
    public int read(ByteBuffer toBuffer) throws ClosedChannelException {
        if(shutdown)
            throw new ClosedChannelException();
        
        return BufferUtils.transfer(readBuffer, toBuffer);
    }

    // unused.
    public void handleIOException(IOException iox) {}
}
