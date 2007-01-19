package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.BufferUtils;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadChannel;
import org.limewire.nio.channel.InterestWriteChannel;

/**
 * State machine for reading & writing.
 */
public class IOStateMachine implements ChannelReadObserver, ChannelWriter, InterestReadChannel {
    
    private static final Log LOG = LogFactory.getLog(IOStateMachine.class);
    
   
    /** Observer to notify when this finishes or fails. */
    private IOStateObserver observer;
    /** The states this will use while handshaking.*/
    private List<IOState> states;
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
    
    public IOStateMachine(IOStateObserver observer, List<IOState> states) {
        this(observer, states, 2048);
    }

    public IOStateMachine(IOStateObserver observer, List<IOState> states, int bufferSize) {
        this.observer = observer;
        this.states = states;
        this.readBuffer = NIODispatcher.instance().getBufferCache().getHeap(bufferSize);
        if(!states.isEmpty())
            this.currentState = states.remove(0);
    }
    
    /**
     * Adds a new state to process.
     * 
     * @param state
     */
    public void addState(final IOState newState) {
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                if(LOG.isDebugEnabled())
                    LOG.debug("Adding single state: " + newState);
                states.add(newState);
                if(states.size() == 1) {
                    nextState(false, false);
                }
            }
        });
    }
    
    /**
     * Adds a collection of new states to process.
     */
    public void addStates(final List<? extends IOState> newStates) {
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                if(LOG.isDebugEnabled())
                    LOG.debug("Adding multiple states: " + newStates);
                states.addAll(newStates);
                if(states.size() == newStates.size())
                    nextState(false, false);
            }
        });        
    }
    
    /**
     * Adds an array of new states to process.
     */
    public void addStates(final IOState... newStates) {
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                if(LOG.isDebugEnabled())
                    LOG.debug("Adding multiple states...");
                for(int i = 0; i < newStates.length; i++) {
                    if(LOG.isDebugEnabled())
                        LOG.debug(" state[" + i + "]: " + newStates[i]);
                    states.add(newStates[i]);
                }
                if(states.size() == newStates.length)
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
        if(currentState != null) {
            if(currentState.isWriting()) {
                LOG.warn("Got a read notification while writing.");
                processCurrentState(null, true); // read up the data into the buffer
                readSink.interest(false);
            } else {
                processCurrentState(currentState, true);
            }
        } else {
            LOG.warn("Got a read notification with no current state");
            processCurrentState(null, true);
            readSink.interest(false);
        }
    }
    
    /**
     * Notification that a write can be performed.  If our current state is for reading,
     * we'll turn off future interest events.  Otherwise we'll tell the current state to process.
     */
    public boolean handleWrite() {
        if(currentState != null) {
            if(currentState.isReading()) {
                LOG.warn("Got a write notification while reading");
                writeSink.interest(this, false);
                return false;
            } else {
                return processCurrentState(currentState, false);        
            }
        } else {
            LOG.warn("Got a write notification with no current state");
            writeSink.interest(this, false);
            return false;
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
    private boolean processCurrentState(IOState state, boolean reading) {
        if(!shutdown) {
            try {
               // if(LOG.isDebugEnabled())
              //      LOG.debug("Processing (" + (reading ? "R" : "W") + ") state: " + currentState);
                if (reading) {
                    if(state == null) {
                        if(LOG.isDebugEnabled())
                            LOG.debug("Processing a read with no state");
                        // We must read up data otherwise it could be lost.
                        // (it would be lost if we were transfering observers
                        //  and the prior observer had already read data)
                        while(readBuffer.hasRemaining() && readSink.read(readBuffer) > 0);
                    } else if (!state.process(readSink, readBuffer))
                        nextState(true, false);
                } else {
                    if (!state.process(writeSink, null))
                        nextState(false, true);
                    else
                        return true;
                }
            } catch (IOException iox) {
                if(LOG.isWarnEnabled())
                    LOG.warn("IOX while processing state: " + state, iox);
                synchronized(this) {
                    shutdown = true;
                }
                try {
                    close();
                } catch(IOException ignored) {}
                NIODispatcher.instance().getBufferCache().release(readBuffer);
                observer.handleIOException(iox);
            }
        } else {
            if(LOG.isDebugEnabled())
                LOG.debug("Ignoring processing because machine is shutdown");
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
            LOG.debug("No more states, processing finished.");
            readSink.interest(false);
            writeSink.interest(this, false);
            observer.handleStatesFinished();
        } else {
            currentState = states.remove(0);
            if(LOG.isDebugEnabled())
                LOG.debug("Incrementing state to: " + currentState);
            
            if(currentState.isReading() && !reading) {
                writeSink.interest(this, false);
                if(readSink != null)
                    readSink.interest(true);
            }
            
            if(currentState.isWriting() && !writing) {
                readSink.interest(false);
                if(writeSink != null)
                    writeSink.interest(this, true);
            }
            
            // Process reading immediately, else
            // data already in the buffer may be
            // ignored while waiting on more data
            // in the socket.
            if(currentState.isReading()) 
                processCurrentState(currentState, true);
        }
    }

    public InterestWriteChannel getWriteChannel() {
        return writeSink;
    }

    public void setWriteChannel(InterestWriteChannel newChannel) {
        this.writeSink = newChannel;
        if(currentState != null)
            writeSink.interest(this, true);
    }

    public InterestReadChannel getReadChannel() {
        return readSink;
    }

    public void setReadChannel(InterestReadChannel newChannel) {
        this.readSink = newChannel;
        if(currentState != null)
            readSink.interest(true); 
    }

    public boolean isOpen() {
        return readSink != null && readSink.isOpen() &&
               writeSink != null && writeSink.isOpen();
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
        
        // This must be done on the NIO thread, else the NIO thread could
        // currently be processing this buffer, and things may continue to
        // process it after we release it.
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                NIODispatcher.instance().getBufferCache().release(readBuffer);
            }
        });
    }

    public void interest(boolean status) {
        if(currentState != null)
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
