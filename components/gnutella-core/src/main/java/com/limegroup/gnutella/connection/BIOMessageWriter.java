package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.statistics.CompressionStat;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * Blocking message writer.  Blocks writing messages to the network.  This 
 * class MUST only be used if the user is not running Java 1.4.x, or the 
 * properties specify not to use NIO.
 */
public final class BIOMessageWriter implements MessageWriter, Runnable {

    /**
     * The <tt>Connection</tt> instance for this writer.
     */
    private final Connection CONNECTION;
    
    /** 
     * A lock to protect changes to the message queue for this connection. 
     */
    private final Object OUTPUT_QUEUE_LOCK = new Object();
    

    /**
     * Handle to the message queue that keeps track of priorities for messages
     * to be sent.
     */
    private CompositeQueue QUEUE;
    
    /**
     * Creates a new <tt>MessageWriter</tt> instance for the specified 
     * connection.
     * 
     * @param conn the <tt>ManagedConnection</tt> containing this writer
     * @return a new <tt>MessageWriter</tt> instance
     */
    public static MessageWriter createWriter(Connection conn) {
        return new BIOMessageWriter(conn);    
    }
    
    /**
     * Constructor for creating a new blocking message writer instance.
     * 
     * @param conn the <tt>ManagedConnection</tt> for this writer
     */
    private BIOMessageWriter(Connection conn) {
        CONNECTION = conn;  
        QUEUE = CompositeQueue.createQueue(CONNECTION, OUTPUT_QUEUE_LOCK);     
        Thread blockingWriter = new Thread(this, "blocking message writer");
        blockingWriter.setDaemon(true);
        blockingWriter.start(); 
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#write()
     */
    public boolean write() throws IOException {
        // TODO Auto-generated method stub
        return false;
    }
    
    public void simpleWrite(Message msg) throws IOException {
        // in order to analyze the savings of compression,
        // we must add the 'new' data to a stat.
        long priorCompressed = 0, priorUncompressed = 0;
        
        // The try/catch block is necessary for two reasons...
        // See the notes in Connection.close above the calls
        // to end() on the Inflater/Deflater and close()
        // on the Input/OutputStreams for the details.        
        try {
            if ( CONNECTION.isWriteDeflated() ) {
                priorUncompressed = _deflater.getTotalIn();
                priorCompressed = _deflater.getTotalOut();
            }
            
            try {
                msg.write(_out);
            } catch(IOException ioe) {
                close(); // make sure we close.
                throw ioe;
            }

            updateWriteStatistics(msg, priorUncompressed, priorCompressed);
        } catch(NullPointerException e) {
            throw CONNECTION_CLOSED;
        }
    }
    
    /**
     * Updates the write statistics.
     * @param m the possibly null message to add to the bytes sent
     * @param pUn the prior uncompressed traffic, used for adding to stats
     * @param pComp the prior compressed traffic, used for adding to stats
     */
    private void updateWriteStatistics(Message m, long pUn, long pComp) {
        if( m != null )
            CONNECTION.stats().addBytesSent(m.getTotalLength());
        if(CONNECTION.isWriteDeflated()) {
            CONNECTION.stats().addCompressedBytesSent(_deflater.getTotalOut());
            if(!CommonUtils.isJava118()) {
                CompressionStat.GNUTELLA_UNCOMPRESSED_UPSTREAM.addData(
                    (int)(_deflater.getTotalIn() - pUn));
                CompressionStat.GNUTELLA_COMPRESSED_UPSTREAM.addData(
                    (int)(_deflater.getTotalOut() - pComp));
            }
        }
    }     

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#write(com.limegroup.gnutella.messages.Message)
     */
    public boolean write(Message msg) {
        synchronized (OUTPUT_QUEUE_LOCK) {
            QUEUE.add(msg);
            OUTPUT_QUEUE_LOCK.notify();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#hasPendingMessage()
     */
    public boolean hasPendingMessage() {
        // TODO Auto-generated method stub
        return false;
    }
    
    /**
     * Accessor for the size of the queue.
     * 
     * @return the size of the queue
     */
    public int size() {
        return QUEUE.size();
    }

    /** While the connection is not closed, sends all data delay. */
    public void run() {
        //Exceptions are only caught to set the _runnerDied variable
        //to make testing easier.  For non-IOExceptions, Throwable
        //is caught to notify ErrorService.
        try {
            while (true) {
                repOk();
                waitForQueued();
                sendQueued();
                repOk();
            }                
        } catch (IOException e) {
            CONNECTION.setSenderDied(true);
        } catch(Throwable t) {
            CONNECTION.setSenderDied(true);     
            ErrorService.error(t);
        }
    }

    /** 
     * Wait until the queue is (probably) non-empty or closed. 
     * @exception IOException this was closed while waiting
     */
    private final void waitForQueued() throws IOException {
        //The synchronized statement is outside the while loop to
        //protect _queued.
        synchronized (OUTPUT_QUEUE_LOCK) {
            while (CONNECTION.isOpen() && 
                   QUEUE.size()==0) {           
                try {
                    OUTPUT_QUEUE_LOCK.wait();
                } catch (InterruptedException e) {
                    Assert.that(false, "OutputRunner Interrupted");
                }
            }
        }
        
        if (! CONNECTION.isOpen()) {
            throw new IOException("connection closed");
        }
    }
    
    /**
     * Send several queued message of each type. The number of each type of
     * message sent depends on the message type.
     * 
     * @throws IOException if there is an IO error sending or flushing the
     *  data
     */
    private final void sendQueued() throws IOException {    
        QUEUE.write();
    }

    /**
     * Closes the writer.
     */
    public void close() {
        synchronized(OUTPUT_QUEUE_LOCK) {
            OUTPUT_QUEUE_LOCK.notify();
        }
    }
    
    /**
     * Used in tests to restart processing messages from the queue.
     */
    public void start() {
        Thread blockingWriter = new Thread(this, "blocking message writer");
        blockingWriter.setDaemon(true);
        blockingWriter.start(); 
    }
    
    /**
     * Accessor for the queue lock, used in testing.
     * 
     * @return the queue lock object
     */
    public Object getLock() {
        return OUTPUT_QUEUE_LOCK;
    }
    
    /** 
     * Tests representation invariants.  For performance reasons, this is
     * private and final.  Make protected if ManagedConnection is subclassed.
     */
    private void repOk() {
        /*
        //Check _queued invariant.
        synchronized (_outputQueueLock) {
            int sum=0;
            for (int i=0; i<_outputQueue.length; i++) 
                sum+=_outputQueue[i].size();
            Assert.that(sum==_queued, "Expected "+sum+", got "+_queued);
        }
        */
    }
    
    /**
     * DO NOT REMOVE -- used in ManagedConnectionBufferTest
     *
     */
    public void resetPriority() {
        QUEUE.resetPriority();
    }
}
