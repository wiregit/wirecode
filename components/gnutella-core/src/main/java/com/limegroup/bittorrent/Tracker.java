package com.limegroup.bittorrent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NetworkUtils;

public class Tracker {
	private static final Log LOG = LogFactory.getLog(Tracker.class);

	/* 25 seconds */
	private static final int HTTP_TRACKER_TIMEOUT = 25 * 1000;

	/*
	 * possible EVENT codes for the tracker protocol
	 */
	/**
	 * event: the client started the download
	 */
	public static final int EVENT_START = 1;

	/**
	 * event: the client stops the download, inform the tracker so it can unlist
	 * you
	 */
	public static final int EVENT_STOP = 2;

	/**
	 * event: the download has just been completed
	 */
	public static final int EVENT_COMPLETE = 3;

	/**
	 * no event: normal tracker update
	 */
	public static final int EVENT_NONE = 4;

	private static final String QUESTION_MARK = "?";

	private static final String EQUALS = "=";

	private static final String AND = "&";
	
	private final URL url;
	
	private final BTMetaInfo info;
	
	private final ManagedTorrent torrent;
	
	private int failures;
	
	public Tracker(URL url, BTMetaInfo info, ManagedTorrent torrent) {
		this.url = url;
		this.info = info;
		this.torrent = torrent;
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
	public TrackerResponse request(int event) {
		if (url.getProtocol().startsWith("http")) {
			String queryStr = createQueryString(event);
			return connectHTTP(url, queryStr);
		} else
			return null;
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
	private String createQueryString(int event) {
		StringBuffer buf = new StringBuffer();
		try {
			String infoHash = URLEncoder.encode(new String(info.getInfoHash(),
					Constants.ASCII_ENCODING), Constants.ASCII_ENCODING);
			addGetField(buf, "info_hash", infoHash);

			String peerId = URLEncoder
					.encode(new String(RouterService.getTorrentManager()
							.getPeerId(), Constants.ASCII_ENCODING),
							Constants.ASCII_ENCODING);
			addGetField(buf, "peer_id", peerId);

			// the "key" parameter is one of the most stupid parts of the
			// tracker protocol. It is used by the tracker to identify a session
			// even if the ip changes. - peerId and infoHash are more than
			// enough information to do so.
			addGetField(buf, "key", peerId + infoHash);

		} catch (UnsupportedEncodingException uee) {
			ErrorService.error(uee);
		}

		addGetField(buf, "ip", NetworkUtils.ip2string(RouterService
				.getAddress()));

		addGetField(buf, "port", String.valueOf(RouterService.getPort()));

		addGetField(buf, "downloaded", String.valueOf(torrent.getDownloader()
				.getAmountRead()));

		addGetField(buf, "uploaded", String.valueOf(torrent.getUploader()
				.getTotalAmountUploaded()));

		addGetField(buf, "left", String.valueOf(info.getTotalSize()
				- info.getVerifyingFolder().getBlockSize()));

		addGetField(buf, "compact", "1");

		switch (event) {
		case EVENT_START:
			addGetField(buf, "event", "started");
			addGetField(buf, "numwant", "100");
			break;
		case EVENT_STOP:
			addGetField(buf, "event", "stopped");
			addGetField(buf, "numwant", "0");
			break;
		case EVENT_COMPLETE:
			addGetField(buf, "event", "completed");
			addGetField(buf, "numwant", "20");
			break;
		default:
			addGetField(buf, "numwant", "50");
		}

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
	private static TrackerResponse connectHTTP(URL url, String query) {
		HttpMethod get = new GetMethod(url.toExternalForm() + query);
		get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
		get.addRequestHeader("Cache-Control", "no-cache");
		get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
				"close");
		get.setFollowRedirects(true);
		HttpClient client = HttpClientManager.getNewClient(
				HTTP_TRACKER_TIMEOUT, HTTP_TRACKER_TIMEOUT);
		try {
			client.executeMethod(get);

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
			if (get != null)
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
	private static StringBuffer addGetField(StringBuffer buf, String key,
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

}
