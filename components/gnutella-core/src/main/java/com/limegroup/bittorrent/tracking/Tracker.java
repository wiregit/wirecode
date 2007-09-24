package com.limegroup.bittorrent.tracking;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.StringUtils;

import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.util.LimeWireUtils;

class Tracker {
	private static final Log LOG = LogFactory.getLog(Tracker.class);

	/* 25 seconds */
	private static final int HTTP_TRACKER_TIMEOUT = 25 * 1000;

	/*
	 * possible EVENT codes for the tracker protocol
	 */
	public enum Event {
		START (100, "started"),
		STOP (0, "stopped"), 
		COMPLETE (20, "completed"), 
		NONE (50, null);
		
		private final String numWant;
		private final String description;
		Event(int numWant, String description) {
			this.numWant = numWant > 0 ? Integer.toString(numWant) : null;
			this.description = description;
		}
		
		public void addEventFields(StringBuilder buf) {
			if (description != null)
				addGetField(buf,"event",description);
			if (numWant != null)
				addGetField(buf,"numwant",numWant);
		}
	}


	private static final String QUESTION_MARK = "?";

	private static final String EQUALS = "=";

	private static final String AND = "&";
	
	private final URI uri;
	
	private final TorrentContext context;
	
	private final ManagedTorrent torrent;
	
	private int failures;
    
    /** The key, as required by some trackers */
    private final String key;
    
    private final NetworkManager networkManager;
    private final ApplicationServices applicationServices;
	
	Tracker(URI uri, TorrentContext context, ManagedTorrent torrent,
            NetworkManager networkManager,
            ApplicationServices applicationServices) {
	    this.networkManager = networkManager;
        this.applicationServices = applicationServices;
		this.uri = uri;
		this.context = context;
		this.torrent = torrent;
        String k = Integer.toHexString((int)(Math.random() * Integer.MAX_VALUE));
        while(k.length() < 8) // make sure length is 8 bytes
            k = k+"0";
        key = k;
            
	}

	/**
	 * Notifies the tracker that a request to it failed.
	 * @return how many times it had failed previously.
	 */
	public void recordFailure() {
		failures++;
	}
	
	/**
	 * Notifies the tracker that a request completed successfully.
	 */
	public void recordSuccess() {
		failures = 0;
	}
	
	/**
	 * @return how many consecutive failures we have for this
	 * tracker.
	 */
	public int getFailures() {
		return failures;
	}
	
	/**
	 * Does a tracker request for a certain event code
	 * 
	 * @param event
	 *            the event code to send to the tracker
	 * @return TrackerResponse holding the data the tracker sent or null if the
	 *         tracker did not send any data
	 */
	public TrackerResponse request(Event event) {
		String queryStr = createQueryString(event);
		return connectHTTP(uri, queryStr);
	}

	/**
	 * helper method creating the query string for a HTTP tracker request
	 * 
	 * @param info
	 *            BTMetaInfo for the torrent
	 * @param torrent
	 *            ManagedTorrent for the torrent
	 * @param event
	 *            the event code to send
	 * @return string, the HTTP GET query string we send to the tracker
	 */
	private String createQueryString(Event event) {
        StringBuilder buf = new StringBuilder();
		try {
			String infoHash = URLEncoder.encode(
					StringUtils.getASCIIString(context.getMetaInfo().getInfoHash()),
					Constants.ASCII_ENCODING);
			addGetField(buf, "info_hash", infoHash);

			String peerId = URLEncoder
					.encode(StringUtils.getASCIIString(applicationServices.getMyBTGUID()),
							Constants.ASCII_ENCODING);
			addGetField(buf, "peer_id", peerId);

			addGetField(buf, "key", key);

		} catch (UnsupportedEncodingException uee) {
            ErrorService.error(uee);
        }

        addGetField(buf, "ip", NetworkUtils.ip2string(networkManager.getAddress()));

        addGetField(buf, "port", String.valueOf(networkManager.getPort()));

		addGetField(buf, "downloaded", 
				String.valueOf(torrent.getTotalDownloaded()));

		addGetField(buf, "uploaded", 
				String.valueOf(torrent.getTotalUploaded()));

		addGetField(buf, "left", String.valueOf(context.getFileSystem().getTotalSize()
				- context.getDiskManager().getBlockSize()));

		addGetField(buf, "compact", "1");

		event.addEventFields(buf);

		if (LOG.isDebugEnabled())
			LOG.debug("tracker query " + buf.toString());
		return buf.toString();
	}

	/**
	 * connects to a tracker via HTTP
	 * 
	 * @param url
	 *            the URL for the tracker
	 * @param query
	 *            the HTTP GET query string, sent to the tracker
	 * @return InputStream
	 */
	private static TrackerResponse connectHTTP(URI uri, String query) {
		HttpMethod get = new GetMethod(uri + query);
		get.addRequestHeader("User-Agent", LimeWireUtils.getHttpServer());
		get.addRequestHeader("Cache-Control", "no-cache");
		get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
				"close");
		get.setFollowRedirects(true);
		HttpClient client = HttpClientManager.getNewClient(
				HTTP_TRACKER_TIMEOUT, HTTP_TRACKER_TIMEOUT);
		try {
			HttpClientManager.executeMethodRedirecting(client, get);

			// response too long
			if (get.getResponseContentLength() > 32768)
				return null;

			byte[] response = get.getResponseBody();
			
			if (response == null)
				return null;

			if (LOG.isDebugEnabled())
				LOG.debug(new String(response));
			return new TrackerResponse(Token.parse(response));
		} catch (IOException e) {
			return null;
		} finally {
			get.releaseConnection();
		}
	}

	/**
	 * helper method adding a field to the HTTP GET query string
	 * 
	 * @param buf
	 *            the StringBuffer containing the query string so far
	 * @param key
	 *            the key for the entry in the query string
	 * @param value
	 *            the value of the entry in the query string
	 * @return StringBuffer containing the modified query string (not really
	 *         necessary because the StringBuffer given as argument will be
	 *         modified)
	 */
	private static StringBuilder addGetField(StringBuilder buf, String key,
			String value) {
		if (buf.length() == 0)
			buf.append(QUESTION_MARK);
		else
			buf.append(AND);
		buf.append(key);
		buf.append(EQUALS);
		buf.append(value);
		return buf;
	}

	public String toString() {
		return "Tracker " + uri +" failures "+failures;
	}
}
