package com.limegroup.gnutella.uploader;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import com.limegroup.gnutella.BandwidthTrackerImpl;
import com.limegroup.gnutella.HTTPUploadManager;
import com.limegroup.gnutella.Uploader;

/**
 * A class encapsulating an HTTP session on the uploader side. 
 */
public class HTTPSession extends BandwidthTrackerImpl implements UploadSlotUser {
	
	/** The socket used for this session */
	private final Socket socket;
	
	private final HTTPUploadManager manager;
	
	/** The current HTTPUploader servicing an upload */
	private HTTPUploader uploader;
	
    /** The min and max allowed times (in milliseconds) between requests by
     *  queued hosts. */
    public static final int MIN_POLL_TIME = 45000; //45 sec
	public static final int MAX_POLL_TIME = 120000; //120 sec
	
	/** The last time this session was polled if queued */
	private volatile long lastPollTime;
	
	
	public HTTPSession(Socket socket, HTTPUploadManager manager) {
		this.socket = socket;
		this.manager = manager;
	}
	
	public void setUploader(HTTPUploader newUploader) {
		uploader = newUploader;
	}
	
	public HTTPUploader getUploader() {
		return uploader;
	}
	
	public void releaseSlot() {
		uploader.stop();
	}
	
	public void measureBandwidth() {
		// delegate to the uploader which will eventually call us back.
		uploader.measureBandwidth();
	}
	
	Socket getSocket() {
		return socket;
	}
	
	public String getHost() {
		return socket.getInetAddress().getHostAddress();
	}
	
	public void handleQueued() throws SocketException {
		uploader.setState(Uploader.QUEUED);
		socket.setSoTimeout(MAX_POLL_TIME);
		lastPollTime = System.currentTimeMillis();
	}
	
	public void handleNotQueued() {
		lastPollTime = 0;
	}
	
	public boolean isConnectedTo(InetAddress addr) {
		return socket.getInetAddress().equals(addr);
	}
	
	int positionInQueue() {
		return manager.getPositionInQueue(this);
	}
	
	/**
	 * Notifies the session of a queue poll.
	 * @return true if the poll was too soon.
	 */
	public boolean poll() {
		long now = System.currentTimeMillis();
		boolean tooSoon = lastPollTime + MIN_POLL_TIME > now;
		lastPollTime = now;
		return tooSoon;
	}
	
	public boolean equals(Object other) {
		if (! (other instanceof HTTPSession))
			return false;

		// one session per socket and vice versa.
		HTTPSession otherSession = (HTTPSession)other;
		return getSocket().equals(otherSession.getSocket());
	}
}
