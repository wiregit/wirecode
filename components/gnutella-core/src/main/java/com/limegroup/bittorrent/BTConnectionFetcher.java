package com.limegroup.bittorrent;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.InterestScatteringByteChannel;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.Sockets;

public class BTConnectionFetcher implements Runnable {
	
	private static final Log LOG = LogFactory.getLog(BTConnectionFetcher.class);
	
	/*
	 * final String we send as a header for bittorrent connections
	 */
	private static final String BITTORRENT_PROTOCOL = "BitTorrent protocol";

	/*
	 * same as above as byte array
	 */
	private static byte[] BITTORRENT_PROTOCOL_BYTES;
	static {
		try {
			BITTORRENT_PROTOCOL_BYTES = BITTORRENT_PROTOCOL
					.getBytes(Constants.ASCII_ENCODING);
		} catch (UnsupportedEncodingException e) {
			ErrorService.error(e);
		}
	}
	
	/*
	 * extension bytes
	 */
	static final byte[] EXTENSION_BYTES = new byte[] { 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x02 };

	/*
	 * Max concurrent connection threads
	 */
	private static final int MAX_CONNECTORS = 5;
	
	/*
	 * time in milliseconds between connection attempts
	 */
	private static final int TIME_BETWEEN_CONNECTIONS = 20 * 1000;
	
	/*
	 * the List of concurrent connector threads
	 */
	private final Set _connectors = 
		Collections.synchronizedSet(new HashSet());
	
	private volatile boolean _isScheduled;
	
	private final ManagedTorrent _torrent;

	/**
	 * The fixed outgoing handshake for this torrent.
	 */
	private final ByteBuffer _handshake;
	
	BTConnectionFetcher(ManagedTorrent torrent, byte[]peerId) {
		_isScheduled = false;
		_torrent = torrent;
		ByteBuffer handshake = ByteBuffer.allocate(68);
		handshake.put((byte) BITTORRENT_PROTOCOL.length());
		handshake.put(BITTORRENT_PROTOCOL_BYTES);
		handshake.put(EXTENSION_BYTES);
		handshake.put(_torrent.getInfoHash());
		handshake.put(peerId);
		handshake.flip();
		
		_handshake = handshake.asReadOnlyBuffer(); // this actually does nothing :(
	}

	/**
	 * schedule a new connection attempt if non is scheduled so far
	 * 
	 * @param waitTime
	 *            the number of milliseconds to wait before starting the
	 *            BTConnectionFetcher
	 */
	public void schedule(long waitTime) {
		if (!_isScheduled) {
			_isScheduled = true;
			if (_torrent.hasStopped()) 
				return;
			if (LOG.isDebugEnabled())
				LOG.debug("rescheduling connection fetcher in " + waitTime);
			RouterService.schedule(this, waitTime, 0);
		}
	}

	/**
	 * main method
	 */
	public void run() {
		_isScheduled = false;
		
		if (_torrent.shouldStop()) {
			_torrent.stop();
			return;
		}

		while (!_torrent.hasStopped() && 
				_connectors.size() < MAX_CONNECTORS &&
				_torrent.needsMoreConnections() && 
				_torrent.hasNonBusyLocations()) {
			fetchConnection();
			if (LOG.isDebugEnabled())
				LOG.debug("started connection fetcher: "
						+ _connectors.size());
		}
	}


	private void reschedule() {
		if (_torrent.needsMoreConnections()) {
			long waitTime = 
				Math.max(_torrent.calculateWaitTime(), TIME_BETWEEN_CONNECTIONS);
			
			schedule(waitTime);
		}
	}

	/**
	 * Starts a connector thread if possible
	 */
	private void fetchConnection() {

		TorrentLocation ep = _torrent.getTorrentLocation();

		if (ep == null) {
			if (LOG.isDebugEnabled())
				LOG.debug("no hosts to connect to");
			return;
		}
		
		BTConnectObserver connector = new BTConnectObserver(ep);
		_connectors.add(connector);
		try {
			Sockets.connect(ep.getAddress(),
					ep.getPort(), Constants.TIMEOUT, connector);
		} catch (IOException impossible) {
			ErrorService.error(impossible);
		}
	}

	private class BTConnectObserver implements ConnectObserver, 
	ChannelWriter, ChannelReadObserver {

		private final TorrentLocation loc;
		
		private ByteBuffer outgoingHandshake;
		private ByteBuffer [] incomingHandshake;
		private int currentBufIndex;
		
		private InterestWriteChannel writeChannel;
		private InterestScatteringByteChannel readChannel;
		private NIOSocket sock;
		
		private boolean incomingDone, finishingHandshakes;
		
		// remote host info
		byte [] extBytes;
		byte [] peerId;
		
		BTConnectObserver(TorrentLocation loc) {
			this.loc = loc;
		}
		
		public void handleConnect(Socket socket) throws IOException {
			if (LOG.isDebugEnabled())
				LOG.debug("established connection to "+socket.getInetAddress());
			
			sock = (NIOSocket)socket;
			
			// connection was established - init the buffers.
			outgoingHandshake = _handshake.duplicate();
			incomingHandshake = new ByteBuffer[5];
			incomingHandshake[0] = ByteBuffer.allocate(1); // 19
			byte []tmp = new byte[19];
			incomingHandshake[1] = ByteBuffer.wrap(tmp); // protocol identifier
			extBytes = new byte[8];
			incomingHandshake[2] = ByteBuffer.wrap(extBytes); // extention bytes
			tmp = new byte[20];
			incomingHandshake[3] = ByteBuffer.wrap(tmp); // infoHash
			peerId = new byte[20];
			incomingHandshake[4] = ByteBuffer.wrap(peerId); // peerID
			
			sock.setReadObserver(this);
			sock.setWriteObserver(this);
			readChannel.interest(true);
			writeChannel.interest(this, true);
		}
		
		public void handleRead() throws IOException {
			long read = 0;
			while((read = readChannel.read(incomingHandshake)) > 0 && 
					incomingHandshake[4].hasRemaining());
			
			if (read == -1 || !verifyIncoming()) { // bad incoming handshake, drop.
				if (LOG.isDebugEnabled())
					LOG.debug("bad incoming handshake on element "+currentBufIndex+
							" or channel closed "+read);
				shutdown();
				return;
			}
			
			if (!incomingHandshake[4].hasRemaining()) { // done with incoming handshake
				if (LOG.isDebugEnabled())
					LOG.debug("incoming handshake finished "+sock.getInetAddress());
				incomingDone = true;
			}
			
			tryToFinishHandshakes();
		}
		
		private boolean verifyIncoming() {
			for(; 
			currentBufIndex < incomingHandshake.length &&
			!incomingHandshake[currentBufIndex].hasRemaining();
			currentBufIndex++) {
				ByteBuffer current = incomingHandshake[currentBufIndex];
				switch(currentBufIndex) {
				case 0 : // 0x19
					current.flip();
					if (current.get() != (byte)19) 
						return false;
					current.position(1); // mark it as full
					break;
				case 1 : // bittorrent protocol
					if (!Arrays.equals(current.array(), BITTORRENT_PROTOCOL_BYTES))
						return false;
					break;
				case 2 : // extention bytes
					break;
				case 3 : // infoHash
					if (!Arrays.equals(current.array(), _torrent.getInfoHash()))
						return false;
					break;
				case 4 : // peerId
					break;
				}
			}
			
			return true;
		}
		
		public boolean handleWrite() throws IOException {
			// write out our handshake
			int wrote = 0;
			while ((wrote = writeChannel.write(outgoingHandshake)) > 0 &&
				outgoingHandshake.hasRemaining());
			
			if (!outgoingHandshake.hasRemaining()) 
				writeChannel.interest(this, false);
			
			tryToFinishHandshakes();
			
			return true; //this falls through to SocketAdapter which ignores it.
		}
		
		private void tryToFinishHandshakes() {
			if (finishingHandshakes)
				return;

			if (incomingDone && !outgoingHandshake.hasRemaining()) {
				finishingHandshakes = true;
				
				TorrentLocation p = new TorrentLocation(sock.getInetAddress(), sock
						.getPort(), new String(peerId),
						extBytes);
				
				BTConnection btc = new BTConnection(sock, _torrent.getMetaInfo(), p,
						_torrent, true);
				
				if (LOG.isDebugEnabled())
					LOG.debug("added outgoing connection "
							+ sock.getInetAddress().getHostAddress());

				// add the connection and re-schedule fetching.
				_torrent.addConnection(btc);
				_connectors.remove(this);
				reschedule();
			}
		}

		public void handleIOException(IOException iox) {
			if (LOG.isDebugEnabled())
				LOG.debug("Connection failed: " + loc);
			loc.strike();
			if (!loc.isOut())
				_torrent.addEndpoint(loc);
			else
				_torrent.addBadEndpoint(loc);
		}

		public void shutdown() {
			_connectors.remove(this);
			if (sock != null)
				try {sock.close();} catch(IOException impossible){}
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
	}
	

	/**
	 * Accept a bittorrent connection
	 * 
	 * @param sock
	 *            the <tt>Socket</tt> for which to accept the connection
	 */
	public void acceptConnection(Socket sock, byte[] extensionBytes) {
		TorrentLocation ep;

		LOG.debug("got incoming connection "
				+ sock.getInetAddress().getHostAddress());

		try {
			InputStream in = sock.getInputStream();
			// read peer ID, everything else has already been consumed
			byte[] peerId = new byte[20];
			for (int i = 0; i < peerId.length; i += in.read(peerId))
				;

			ep = new TorrentLocation(sock.getInetAddress(), sock.getPort(),
					new String(peerId, Constants.ASCII_ENCODING),
					extensionBytes);
		} catch (IOException ioe) {
			IOUtils.close(sock);
			return;
		}

		if (!_torrent.allowIncomingConnection(ep)) {
			LOG.debug("no more connection slots");
			IOUtils.close(sock);
			return;
		}

		// send our part of the handshake
		try {
			ByteBuffer handshake = _handshake.duplicate();
			sock.getChannel().write(handshake);

			BTConnection btc = new BTConnection((NIOSocket) sock, 
					_torrent.getMetaInfo(), 
					ep,
					_torrent);

			// now add the connection to the Choker and to this:
			_torrent.addConnection(btc);
			if (LOG.isDebugEnabled())
				LOG.debug("added incoming connection "
						+ sock.getInetAddress().getHostAddress());
		} catch (IOException ioe) {
			IOUtils.close(sock);
		}
	}
}
