/**
 * 
 */
package com.limegroup.bittorrent.handshaking;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.BTConnection;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.InterestScatteringByteChannel;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.util.IpPort;

public abstract class BTHandshaker implements  
ChannelWriter, ChannelReadObserver, IpPort {

	private static final Log LOG = LogFactory.getLog(BTHandshaker.class);
	
	
	protected ManagedTorrent torrent;
	
	protected ByteBuffer outgoingHandshake;
	protected ByteBuffer [] incomingHandshake;
	protected int currentBufIndex;
	
	protected InterestWriteChannel writeChannel;
	protected InterestScatteringByteChannel readChannel;
	protected volatile NIOSocket sock;
	
	protected boolean incomingDone, finishingHandshakes;
	private volatile boolean shutdown;
	
	protected final TorrentLocation loc;
	
	protected BTHandshaker(TorrentLocation loc) {
		this.loc = loc;
	}
	
	public void handleRead() throws IOException {
		if (shutdown)
			return;
		
		long read = 0;
		while((read = readChannel.read(incomingHandshake)) > 0 && 
				incomingHandshake[incomingHandshake.length - 1].hasRemaining());
		
		if (read == -1 || !verifyIncoming()) { // bad incoming handshake, drop.
			if (LOG.isDebugEnabled())
				LOG.debug("bad incoming handshake on element "+currentBufIndex+
						" or channel closed "+read);
			shutdown();
			return;
		}
		
		if (!incomingHandshake[incomingHandshake.length - 1].hasRemaining()) { 
			// done with incoming handshake
			if (LOG.isDebugEnabled())
				LOG.debug("incoming handshake finished "+sock.getInetAddress());
			incomingDone = true;
		}
		
		tryToFinishHandshakes();
	}
	
	protected abstract boolean verifyIncoming();
	
	public boolean handleWrite() throws IOException {
		if (shutdown)
			return false;
		
		// write out our handshake
		while (writeChannel.write(outgoingHandshake) > 0 &&
			outgoingHandshake.hasRemaining());
		
		if (!outgoingHandshake.hasRemaining()) 
			writeChannel.interest(this, false);
		
		tryToFinishHandshakes();
		
		return true; //this falls through to SocketAdapter which ignores it.
	}

	protected abstract void initIncomingHandshake();
	
	protected void initOutgoingHandshake() {
		outgoingHandshake = torrent.getFetcher().getOutgoingHandshake();
	}
	
	protected final void setReadInterest() {
		sock.setReadObserver(this);
		readChannel.interest(true);
	}
	
	protected final void setWriteInterest() {
		sock.setWriteObserver(this);
		writeChannel.interest(this, true);
	}
	
	private void tryToFinishHandshakes() {
		if (finishingHandshakes || shutdown)
			return;

		if (incomingDone && !outgoingHandshake.hasRemaining()) {
			finishingHandshakes = true;
			
			if (torrent.shouldAddConnection(loc)) {
				BTConnection btc = new BTConnection(sock, 
						torrent.getMetaInfo(), 
						loc,
						torrent, true);

				if (LOG.isDebugEnabled())
					LOG.debug("created connection "
							+ sock.getInetAddress().getHostAddress());

				// add the connection and re-schedule fetching.
				torrent.addConnection(btc);
				torrent.getFetcher().handshakerDone(this);
			}
			else {
				if (LOG.isDebugEnabled())
					LOG.debug("have enough connections, remembering loc "+loc);
				
				torrent.addEndpoint(loc);
				shutdown();
			}
		}
	}
	
	public void handleIOException(IOException iox) {
		shutdown();
	}

	public void shutdown() {
		synchronized(this) {
			if (shutdown)
				return;
			shutdown = true;
		}
		
		if (torrent != null)
			torrent.getFetcher().handshakerDone(this);
		
		sock.close();
	}

	public void setWriteChannel(InterestWriteChannel newChannel) {
		writeChannel = newChannel;
	}

	public InterestWriteChannel getWriteChannel() {
		return writeChannel;
	}

	public void setReadChannel(InterestReadChannel newChannel) {
		// if this throws we got problemos.
		readChannel = (InterestScatteringByteChannel) newChannel;
	}

	public InterestReadChannel getReadChannel() {
		return readChannel;
	}

	public String getAddress() {
		return loc.getAddress();
	}

	public InetAddress getInetAddress() {
		return loc.getInetAddress();
	}

	public int getPort() {
		return loc.getPort();
	}
}