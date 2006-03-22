package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.NIODispatcher;

/** 
 *  Manage the assignment of connectionIDs and the routing of 
 *  UDPConnectionMessages. 
 */
public class UDPMultiplexor extends AbstractSelector {

    private static final Log LOG =
      LogFactory.getLog(UDPMultiplexor.class);

	/** Keep track of a singleton instance */
    private static UDPMultiplexor     _instance    = new UDPMultiplexor();

	/** The 0 slot is for incoming new connections so it is not assigned */
	public static final byte          UNASSIGNED_SLOT   = 0;

	/** Keep track of the assigned connections */
	private volatile UDPSocketChannel[] _channels;
    
    /** A list of overflowed channels when registering. */
    private final List channelsToRemove = new LinkedList();
    
    /** A set of the currently connected keys. */
    private Set selectedKeys = new HashSet(256);

	/** Keep track of the last assigned connection id so that we can use a 
		circular assignment algorithm.  This should cut down on message
		collisions after the connection is shut down. */
	private int                       _lastConnectionID;

    /**
     *  Return the UDPMultiplexor singleton.
     */
    public static UDPMultiplexor instance() {
		return _instance;
    }      

    /**
     *  Initialize the UDPMultiplexor.
     */
    private UDPMultiplexor() {
        super(null);
		_channels       = new UDPSocketChannel[256];
		_lastConnectionID  = 0;
        NIODispatcher.instance().registerSelector(this, UDPSocketChannel.class);
    }
    
    /**
     * Determines if we're connected to the given host.
     */
    public boolean isConnectedTo(InetAddress host) {
        UDPSocketChannel[] array = _channels;

        if (_lastConnectionID == 0)
            return false;
        for (int i = 0; i < array.length; i++) {
            UDPSocketChannel channel = array[i];
            if (channel != null && host.equals(channel.getRemoteSocketAddress().getAddress())) {
                return true;
            }
        }
        return false;
    }

    /**
     *  Route a message to the UDPConnectionProcessor identified in the messages
	 *  connectionID;
     */
	public void routeMessage(UDPConnectionMessage msg, InetSocketAddress addr) {
        UDPSocketChannel[] array = _channels;
		int connID = (int) msg.getConnectionID() & 0xff;

		// If connID equals 0 and SynMessage then associate with a connection
        // that appears to want it (connecting and with knowledge of it).
		if ( connID == 0 && msg instanceof SynMessage ) {
			for (int i = 1; i < array.length; i++) {
                UDPSocketChannel channel = (UDPSocketChannel)array[i];
                if(channel == null)
                    continue;
                
				if ( channel.isConnectionPending() && channel.getRemoteSocketAddress().equals(addr)) {
                    channel.getProcessor().handleMessage(msg);
					break;
				} 
			}
			// Note: eventually these messages should find a match
			// so it is safe to throw away premature ones

		} else if(array[connID] != null) {  // If valid connID then send on to connection
            UDPSocketChannel channel = (UDPSocketChannel)array[connID];
			if (channel.getRemoteSocketAddress().equals(addr) )
                channel.getProcessor().handleMessage(msg);
		}
	}

    protected void implCloseSelector() throws IOException {
        throw new IllegalStateException("should never be closed.");
    }

    /**
     * Registers a new channel with this Selector.
     * If we've already stored over the limit of channels, this will store
     * the channel in a temporary list to be cancelled on the next selection.
     */
    protected synchronized SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        int connID;

        UDPSocketChannel[] copy = new UDPSocketChannel[_channels.length];
        for (int i = 0; i < _channels.length; i++)
            copy[i] = _channels[i];

        for (int i = 1; i <= copy.length; i++) {
            connID = (_lastConnectionID + i) % 256;

            // We don't assign zero.
            if (connID == 0)
                continue;

            // If the slot is open, take it.
            if (copy[connID] == null) {
                _lastConnectionID = connID;
                UDPSocketChannel channel = (UDPSocketChannel)ch;
                copy[connID] = channel;
                channel.getProcessor().setConnectionId((byte)connID);
                _channels = copy;
                return new UDPSelectionKey(this, att, ch, ops);
            }
        }
        
        // We don't have enough space for this connection.  Add it to a temporary
        // list of bad connections which will be removed during selectNow.
        LOG.warn("Attempting to add over connection limit");
        channelsToRemove.add(ch);
        return new UDPSelectionKey(this, att, ch, ops);
    }

    public Set keys() {
        throw new UnsupportedOperationException("full keyset retrieval not supported");
    }

    public int select() throws IOException {
        throw new UnsupportedOperationException("blocking select not supported");
    }

    public int select(long timeout) throws IOException {
        throw new UnsupportedOperationException("blocking select not supported");
    }

    public Set selectedKeys() {
        return selectedKeys;
    }

    /** Polls through all available channels and returns those that are ready. */
    public int selectNow() throws IOException {
        UDPSocketChannel[] array = _channels;
        UDPSocketChannel[] removed = null;

        selectedKeys.clear();
        
        for (int i = 0; i < array.length; i++) {
            UDPSocketChannel channel = (UDPSocketChannel) array[i];
            if (channel == null)
                continue;

            UDPSelectionKey key = (UDPSelectionKey)channel.keyFor(this);
            if (key != null) {
                if (key.isValid()) {
                    int currentOps = channel.getProcessor().readyOps();
                    int readyOps = currentOps & key.interestOps();
                    if (readyOps != 0) {
                        key.setReadyOps(readyOps);
                        selectedKeys.add(key);
                    }
                } else {
                    if (removed == null)
                        removed = new UDPSocketChannel[array.length];
                    removed[i] = channel;
                }
            }
        }

        // Go through the removed list & remove them from _connections.
        // _connections may have changed (since we didn't lock while polling),
        // so we need to check and ensure the given UDPConnectionProcessor
        // is the same.
        synchronized (this) {
            if (removed != null) {
                UDPSocketChannel[] copy = new UDPSocketChannel[_channels.length];
                for (int i = 0; i < _channels.length; i++) {
                    if (_channels[i] == removed[i])
                        copy[i] = null;
                    else
                        copy[i] = _channels[i];
                }
                _channels = copy;
            }
            
            if(!channelsToRemove.isEmpty()) {
                for(Iterator i = channelsToRemove.iterator(); i.hasNext(); ) {
                    SelectableChannel next = (SelectableChannel)i.next();
                    UDPSelectionKey key = (UDPSelectionKey)next.keyFor(this);
                    key.cancel();
                    selectedKeys.add(key);
                }
                channelsToRemove.clear();
            }
        }
        
        return selectedKeys.size();
    }

    public Selector wakeup() {
        // Does nothing, since this never blocks.
        return this;
    }
}
