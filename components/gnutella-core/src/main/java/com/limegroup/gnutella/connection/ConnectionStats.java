package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.BandwidthTrackerImpl;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.HorizonCounter;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.messages.PingReply;

/**
 * This class keeps track of statistics for a single Gnutella connection.
 */
public final class ConnectionStats {

    /**
     * The number of messages received.  This messages that are eventually
     * dropped.  This stat is synchronized by _outputQueueLock;
     */
    private int _numMessagesSent;
        
    /**
     * The number of messages received.  This includes messages that are
     * eventually dropped.  This stat is not synchronized because receiving
     * is not thread-safe; callers are expected to make sure only one thread
     * at a time is calling receive on a given connection.
     */
    private int _numMessagesReceived;
        
    /**
     * The number of messages received on this connection either filtered out
     * or dropped because we didn't know how to route them.
     */
    private int _numReceivedMessagesDropped;
    
    /**
     * The number of messages I dropped because the
     * output queue overflowed.  This happens when the remote host
     * cannot receive packets as quickly as I am trying to send them.
     * No synchronization is necessary.
     */
    private int _numSentMessagesDropped;
    
    /**
     * _lastSent/_lastSentDropped and _lastReceived/_lastRecvDropped the values
     * of _numMessagesSent/_numSentMessagesDropped and
     * _numMessagesReceived/_numReceivedMessagesDropped at the last call to
     * getPercentDropped.  LOCKING: These are synchronized by this;
     * finer-grained schemes could be used. 
     */
    private int _lastReceived;
    private int _lastRecvDropped;
    private int _lastSent;
    private int _lastSentDropped;
    
    /**
     * The number of bytes sent to the output stream.
     */
    private volatile long _bytesSent;
    
    /**
     * The number of bytes recieved from the input stream.
     */
    private volatile long _bytesReceived;
    
    /**
     * The number of compressed bytes sent to the stream.
     * This is effectively the same as _deflater.getTotalOut(),
     * but must be cached because Deflater's behaviour is undefined
     * after end() has been called on it, which is done when this
     * connection is closed.
     */
    private volatile long _compressedBytesSent;
    
    /**
     * The number of compressed bytes read from the stream.
     * This is effectively the same as _inflater.getTotalIn(),
     * but must be cached because Inflater's behaviour is undefined
     * after end() has been called on it, which is done when this
     * connection is closed.
     */
    private volatile long _compressedBytesReceived;
    
    /**
     * Whether or not horizon counting is enabled from this connection.
     */
    private boolean _horizonEnabled = true;

    /** 
     * The bandwidth trackers for the up/downstream.
     * These are not synchronized and not guaranteed to be 100% accurate.
     */
    private BandwidthTrackerImpl _upBandwidthTracker=
        new BandwidthTrackerImpl();
    private BandwidthTrackerImpl _downBandwidthTracker=
        new BandwidthTrackerImpl();
            
    /**
     * Constant for the <tt>Connection</tt> instance that this stat handler
     * tracks stats for.
     */
    private final Connection CONNECTION;
    
    /**
     * Creates a new <tt>ConnectionStats</tt> instance associated with the
     * specified connection.
     * 
     * @param conn the <tt>Connection</tt> this <tt>ConnectionStats</tt> 
     *  instance will maintain stats for
     */
    public ConnectionStats(Connection conn) {
        CONNECTION = conn;
    }

    /**
     * Adds the specified number of bytes to the number of bytes sent.
     * 
     * @param bytes the number of bytes to add
     */
    public void addBytesSent(int bytes) {
        _bytesSent += bytes;    
    }  
    
    /**
     * Adds the specified number of bytes to the number of compressed bytes 
     * sent.
     * 
     * @param bytes the number of compressed bytes to add
     */
    public void addCompressedBytesSent(int bytes) {
        _compressedBytesSent += bytes;    
    } 
    
    /**
     * Adds the specified number of bytes to the number of bytes received.
     * 
     * @param bytes the number of bytes to add
     */
    public void addBytesReceived(int bytes) {
        _bytesReceived += bytes;    
    }
    
    /**
     * Adds the specified number of bytes to the number of compressed bytes 
     * received for this connection.
     * 
     * @param bytes the number of compressed bytes to add
     */
    public void addCompressedBytesReceived(int bytes) {
        _compressedBytesReceived += bytes;    
    }
    
    /**
     * Utility method for adding dropped message data.
     * 
     * @param dropped the number of dropped messages to add
     */
    public void addSentDropped(int dropped) {
        _numSentMessagesDropped += dropped;
    }
        
    /**
     * Increments the number of messages sent for this connection.
     */
    public void addSent() {
        _numMessagesSent++;    
    }
        
    /**
     * Increments the number of received messages that have been dropped.
     */
    public void addReceivedDropped() {
        _numReceivedMessagesDropped++;   
    }
        
    /**
     * Increments the stat for the number of messages received.
     */
    public void addReceived() {
        _numMessagesReceived++;
    }
    
    /**
     * A callback for the ConnectionManager to inform this connection that a
     * message was dropped.  This happens when a reply received from this
     * connection has no routing path.
     */
    public void countDroppedMessage() {
        _numReceivedMessagesDropped++;
    }
    
    /** 
     * Returns the number of messages sent on this connection. 
     */
    public int getNumMessagesSent() {
        return _numMessagesSent;
    }

    /** 
     * Returns the number of messages received on this connection. 
     */
    public int getNumMessagesReceived() {
        return _numMessagesReceived;
    }

    /** 
     * Returns the number of messages I dropped while trying to send
     * on this connection.  This happens when the remote host cannot
     * keep up with me. 
     */
    public int getNumSentMessagesDropped() {
        return _numSentMessagesDropped;
    }

    /**
     * The number of messages received on this connection either filtered out
     * or dropped because we didn't know how to route them.
     */
    public long getNumReceivedMessagesDropped() {
        return _numReceivedMessagesDropped;
    }
    
    
    /**
     * @modifies this
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentReceivedDropped that were
     *  dropped by this end of the connection.
     */
    public synchronized float getPercentReceivedDropped() {
        int rdiff = _numMessagesReceived - _lastReceived;
        int ddiff = _numReceivedMessagesDropped - _lastRecvDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);

        _lastReceived = _numMessagesReceived;
        _lastRecvDropped = _numReceivedMessagesDropped;
        return percent;
    }

    /**
     * @modifies this
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentSentDropped that were
     *  dropped by this end of the connection.  This value may be
     *  greater than 100%, e.g., if only one message is sent but
     *  four are dropped during a given time period.
     */
    public synchronized float getPercentSentDropped() {
        int rdiff = _numMessagesSent - _lastSent;
        int ddiff = _numSentMessagesDropped - _lastSentDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);

        _lastSent = _numMessagesSent;
        _lastSentDropped = _numSentMessagesDropped;
        return percent;
    }      
    
    /**
     * Returns the number of bytes sent on this connection.
     * If the outgoing stream is compressed, the return value indicates
     * the compressed number of bytes sent.
     */
    public long getBytesSent() {
        if(CONNECTION.isWriteDeflated())
            return _compressedBytesSent;
        else            
            return _bytesSent;
    }
    
    /**
     * Returns the number of uncompressed bytes sent on this connection.
     * If the outgoing stream is not compressed, this is effectively the same
     * as calling getBytesSent()
     */
    public long getUncompressedBytesSent() {
        return _bytesSent;
    }
    
    /** 
     * Returns the number of bytes received on this connection.
     * If the incoming stream is compressed, the return value indicates
     * the number of compressed bytes received.
     */
    public long getBytesReceived() {
        if(CONNECTION.isReadDeflated())
            return _compressedBytesReceived;
        else
            return _bytesReceived;
    }
    
    /**
     * Returns the number of uncompressed bytes read on this connection.
     * If the incoming stream is not compressed, this is effectively the same
     * as calling getBytesReceived()
     */
    public long getUncompressedBytesReceived() {
        return _bytesReceived;
    }
    
    /**
     * Returns the percentage saved through compressing the outgoing data.
     * The value may be slightly off until the output stream is flushed,
     * because the value of the compressed bytes is not calculated until
     * then.
     */
    public float getSentSavedFromCompression() {
        if (!CONNECTION.isWriteDeflated() || _bytesSent == 0 ) return 0;
        return 1-((float)_compressedBytesSent/(float)_bytesSent);
    }
    
    /**
     * Returns the percentage saved from having the incoming data compressed.
     */
    public float getReadSavedFromCompression() {
        if (!CONNECTION.isReadDeflated() || _bytesReceived == 0 ) return 0;
        return 1-((float)_compressedBytesReceived/(float)_bytesReceived);
    }
    
    /** 
     * @modifies this
     * @effects enables or disables updateHorizon. Typically this method
     *  is used to temporarily disable horizon statistics before sending a 
     *  ping with a small TTL to make sure a connection is up.
     */
    public synchronized void setHorizonEnabled(boolean enable) {
        _horizonEnabled=enable;
    }

    /**
     * This method is called when a reply is received by this connection for a
     * PingRequest that originated from LimeWire.
     * 
     * @modifies this 
     * @effects adds the statistics from pingReply to this' horizon statistics,
     *  unless horizon statistics have been disabled via setHorizonEnabled(false).
     *  It's possible that the horizon statistics will not actually be updated
     *  until refreshHorizonStats is called.
     */
    public synchronized void updateHorizonStats(PingReply pingReply) {
        if (! _horizonEnabled)
            return;
        
        HorizonCounter.instance().addPong(pingReply);
    }
    
    /**
     * Takes a snapshot of the upstream and downstream bandwidth since the last
     * call to measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public void measureBandwidth() {
        _upBandwidthTracker.measureBandwidth(
             ByteOrder.long2int(getBytesSent()));
        _downBandwidthTracker.measureBandwidth(
             ByteOrder.long2int(getBytesReceived()));
    }

    /**
     * Returns the upstream bandwidth between the last two calls to
     * measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public float getMeasuredUpstreamBandwidth() {
        try {
            return _upBandwidthTracker.getMeasuredBandwidth();
        } catch(InsufficientDataException ide) {
            return 0;
        }
    }

    /**
     * Returns the downstream bandwidth between the last two calls to
     * measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public float getMeasuredDownstreamBandwidth() {
        try {
            return _downBandwidthTracker.getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            return 0;
        }
    }
}
