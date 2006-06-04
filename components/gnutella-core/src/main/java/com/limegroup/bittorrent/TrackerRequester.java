
// Commented for the Learning branch

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

/**
 * This TrackerRequester class contains a static method request() that lets you contact a BitTorrent tracker on the Web.
 * 
 * The Web address it will hit is like:
 * 
 * http://www.site.com:6969/announce?info_hash=H3%239s%F...
 * 
 * The createQueryString() method composes the "?info_hash=H3%239s%F..." part.
 * The parameters it composes are:
 * 
 * info_hash   The SHA1 hash of the bencoded "info" dictionary in the .torrent file, URL-encoded like %20.
 * peer_id     Our 20-byte peer ID that starts "LIME", URL-encoded.
 * key         A unique number that identifies our session to the tracker, we use the peer ID and info hash.
 * ip          Our Internet IP address, like "&ip=216.27.158.74".
 * port        Our Internet port number, like "&port=6375".
 * downloaded  The number of bytes of this torrent we've saved, including complete and verified pieces and partial pieces.
 * uploaded    The number of bytes of this torrent we've given peers, can be more than the whole size of the file.
 * left        The number of bytes of this torrent we still have to get, size = downloaded + left.
 * compact     Include with a value of 1 to tell the tracker we want addresses in 6 bytes, not a bencoded list.
 * event       Say "event=started" the first time we contact a tracker.
 *             Say "event=stopped" the last time we contact a tracker.
 *             Say "event=completed" if we finish downloading a torrent while in contact with a tracker.
 * numwant     The number of peer addresses we want.
 * 
 * The tracker sends back a bencoded message.
 * Token.parse() parses it into Java objects, and the TrackerResponse constructor turns it into a new TrackerResponse object.
 */
public class TrackerRequester {

	/** A debugging log we can write lines of text to as the program runs. */
	private static final Log LOG = LogFactory.getLog(TrackerRequester.class);

	/*
	 * if a tracker request takes too long it may cause other delays, e.g. in
	 * our choking because we did not offload tracker requests to their own
	 * thread yet
	 */

	/** 25 seconds in milliseconds, we'll wait this long for a BitTorrent tracker on the Web to respond. */
	private static final int HTTP_TRACKER_TIMEOUT = 25 * 1000;

	/**
	 * 1, our enumeration code for the start event.
	 * Include "event=start" in your first request to a tracker.
	 */
	public static final int EVENT_START = 1;

	/**
	 * 2, our enumeration code for the stop event.
	 * Include "event=stop" in your last request to a tracker.
	 */
	public static final int EVENT_STOP = 2;

	/**
	 * 3, our enumeration code for the complete event.
	 * Include "event=complete" if you get the whole file while talking to a tracker.
	 * If you already have the whole file and connect to a tracker, don't tell it "event=complete".
	 */
	public static final int EVENT_COMPLETE = 3;

	/**
	 * 4, our enumeration code for tracker communications that don't include an event.
	 * If you're not greeting or leaving the tracker, and you didn't just finish the file, don't include "event=" in the request.
	 */
	public static final int EVENT_NONE = 4;

	/** "?", used in a Web query string like "http://www.site.com/file?querystring". */
	private static final String QUESTION_MARK = "?";

	/** "=", used in a Web query string like "http://www.site.com/file?name=value". */
	private static final String EQUALS = "=";

	/** "&", used in a Web query string like "http://www.site.com/file?name=value&name2=value2". */
	private static final String AND = "&";

	/**
	 * Contact a tracker.
	 * 
	 * @param url     The Web address of the tracker, like "http://www.site.com:6969/announce".
	 * @param info    A BTMetaInfo object we made from a .torrent file.
	 * @param torrent The ManagedTorrent object that represents the torrent.
	 * @return        A TrackerResponse object that represents the parsed bencoded data the tracker sent back to us in response.
	 *                null if the Web tracker replied with nothing, or we couldn't contact it.
	 */
	public static TrackerResponse request(URL url, BTMetaInfo info, ManagedTorrent torrent) {

		// Call the next request() method
		return request(url, info, torrent, EVENT_NONE); // Don't include "event=" in our request
	}

	/**
	 * Contact a BitTorent tracker on the Web.
	 * This method blocks while we're navigating to the tracker's address, only giving up after 25 seconds.
	 * 
	 * @param url     The Web address of the tracker, like "http://www.site.com:6969/announce".
	 * @param info    A BTMetaInfo object we made from a .torrent file.
	 * @param torrent The ManagedTorrent object that represents the torrent.
	 * @param event   A BitTorrent event to tell the tracker, like EVENT_START, EVENT_STOP, or EVENT_COMPLETE.
	 * @return        A TrackerResponse object that represents the parsed bencoded data the tracker sent back to us in response.
	 *                null if the Web tracker replied with nothing, or we couldn't contact it.
	 */
	public static TrackerResponse request(URL url, BTMetaInfo info, ManagedTorrent torrent, int event) {

		// The given address is like "http://www..." or "https://www...", it's on the Web
		if (url.getProtocol().startsWith("http")) {

			// Compose the query string, like "?info_hash=H3%239s%F..."
			String queryStr = createQueryString(info, torrent, event);

			// Send a message to a BitTorrent tracker on the Web, and get its bencoded response
			return connectHTTP(url, queryStr); // This method blocks while we're navigating to the tracker's address, only giving up after 25 seconds

		// The given address is like "udp://...", this is a high performance UDP tracker we'll have to contact with a UDP packet
		} else {

			// The program can't do this yet, return null
			return null;
		}
	}

	/**
	 * Compose the query string we'll pass to a BitTorrent tracker on the Web.
	 * The Web address we'll hit is like:
	 * 
	 * http://www.site.com:6969/announce?info_hash=H3%239s%F...
	 * 
	 * This method composes the "?info_hash=H3%239s%F..." part.
	 * The parameters it composes are:
	 * 
	 * info_hash   The SHA1 hash of the bencoded "info" dictionary in the .torrent file, URL-encoded like %20.
	 * peer_id     Our 20-byte peer ID that starts "LIME", URL-encoded.
	 * key         A unique number that identifies our session to the tracker, we use the peer ID and info hash.
	 * ip          Our Internet IP address, like "&ip=216.27.158.74".
	 * port        Our Internet port number, like "&port=6375".
	 * downloaded  The number of bytes of this torrent we've saved, including complete and verified pieces and partial pieces.
	 * uploaded    The number of bytes of this torrent we've given peers, can be more than the whole size of the file.
	 * left        The number of bytes of this torrent we still have to get, size = downloaded + left.
	 * compact     Include with a value of 1 to tell the tracker we want addresses in 6 bytes, not a bencoded list.
	 * event       Say "event=started" the first time we contact a tracker.
	 *             Say "event=stopped" the last time we contact a tracker.
	 *             Say "event=completed" if we finish downloading a torrent while in contact with a tracker.
	 * numwant     The number of peer addresses we want.
	 * 
	 * @param info    A BTMetaInfo object we made from a .torrent file
	 * @param torrent The ManagedTorrent object that represents the torrent
	 * @param event   A BitTorrent event to tell the tracker, like EVENT_START, EVENT_STOP, or EVENT_COMPLETE
	 * @return        The String to put after the address to tell the tracker what we're doing and what we want
	 */
	private static String createQueryString(BTMetaInfo info, ManagedTorrent torrent, int event) {

		// Make a StringBuffer in which we'll build the query string, like "?name=value&name2=value2"
		StringBuffer buf = new StringBuffer();

		try {

			// Start the query text like "?info_hash=H3%239s%F..."
			String infoHash = URLEncoder.encode( // (3) Encode unsafe bytes for the Web, creating a bunch of percents like %20
				new String(                      // (2) Convert that into a String using normal ASCII encoding
					info.getInfoHash(),          // (1) get the SHA1 hash of the "info" section of the .torrent file in a 20-byte array
					Constants.ASCII_ENCODING),
				Constants.ASCII_ENCODING);
			addGetField(buf, "info_hash", infoHash);

			// To that, add the peer ID like "&peer_id=LIME%CA%D7%B7%7D%EF*%A7%94%F5%D8%FEe%C8.%F6%00"
			String peerId = URLEncoder.encode(new String(RouterService.getTorrentManager().getPeerId(), Constants.ASCII_ENCODING), Constants.ASCII_ENCODING);
			addGetField(buf, "peer_id", peerId);

			/*
			 * the "key" parameter is one of the most stupid parts of the
			 * tracker protocol. It is used by the tracker to identify a session
			 * even if the ip changes. - peerId and infoHash are more than
			 * enough information to do so.
			 */

			// To that, add the key like "&key=..."
			addGetField(buf, "key", peerId + infoHash);

		// The computer we're running on can't encode to ASCII
		} catch (UnsupportedEncodingException uee) { ErrorService.error(uee); }

		// Include our IP address and port number in the information we'll tell the tracker
		addGetField(buf, "ip",   NetworkUtils.ip2string(RouterService.getAddress())); // Add "&ip=216.27.158.74"
		addGetField(buf, "port", String.valueOf(RouterService.getPort()));            // Add "&port=6375"

		// Add information about how much of the file we have and have shared
		addGetField(buf, "downloaded", String.valueOf(torrent.getDownloader().getAmountRead()));                        // Add "&downloaded=0", bytes saved
		addGetField(buf, "uploaded",   String.valueOf(torrent.getUploader().getTotalAmountUploaded()));                 // Add "&uploaded=0", bytes uploaded
		addGetField(buf, "left",       String.valueOf(info.getTotalSize() - info.getVerifyingFolder().getBlockSize())); // Add "&left=10049270", bytes missing

		// Tell the tracker we want IP addresses and port numbers in 6 bytes, not in a bencoded list
		addGetField(buf, "compact", "1"); // Add "&compact=1"

		// Add the requested event
		switch (event) {

		// The first time a BitTorrent program contacts a tracker, it must include "&event=started"
		case EVENT_START:

			// Add "event" and "numwant" for our first contact with the tracker
			addGetField(buf, "event", "started");
			addGetField(buf, "numwant", "100"); // Also tell the tracker we want 100 IP addresses and port numbers of peer programs sharing the same torrent
			break;

		// The last time a BitTorrent program contacts a tracker, it should include "&event=stopped"
		case EVENT_STOP:

			// Add "event" and "numwant" for our last contact with the tracker
			addGetField(buf, "event", "stopped");
			addGetField(buf, "numwant", "0"); // Set "numwant" to 0, we don't want any more peer addresses
			break;

		// When we get the whole torrent, tell the tracker "&event=completed"
		case EVENT_COMPLETE: // Don't do this unless you actually complete the torrent while in contact with the tracker

			// Add "event" and "numwant" now that we're done with the torrent
			addGetField(buf, "event", "completed");
			addGetField(buf, "numwant", "20"); // Set "numwant" to 20, we want 20 peer addresses at a time now (do)
			break;

		// No specific event to declare
		default:

			// Just add "numwant", telling the tracker we want 50 peer addresses at a time
			addGetField(buf, "numwant", "50");
		}

		// Log and return the string of query text we built
		if (LOG.isDebugEnabled()) LOG.debug("tracker query " + buf.toString());
		return buf.toString();
	}

	/**
	 * Send a message to a BitTorrent tracker on the Web, and get its bencoded response.
	 * This method blocks while we're navigating to the tracker's address, only giving up after 25 seconds.
	 * 
	 * @param url   The Web address of the tracker, like "http://www.site.com:6969/announce".
	 * @param query The query text to put after that, like "?info_hash=H3%239s%F5%1...".
	 * @return      A TrackerResponse object that represents the parsed bencoded data the tracker sent back to us in response.
	 *              null if the Web tracker replied with nothing, or we couldn't contact it.
	 */
	private static TrackerResponse connectHTTP(URL url, String query) {

		// Make a GetMethod object which will hold our HTTP GET request to the BitTorrent tracker on the Web
		HttpMethod get = new GetMethod(url.toExternalForm() + query); // Give it the Web address to connect to

		// Add HTTP headers to send after the GetMethod makes the connection
		get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());            // "User-Agent: LimeWire/4.10.9"
		get.addRequestHeader("Cache-Control", "no-cache");                          // "Cache-Control: no-cache"
		get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close"); // "Connection: close", have the Web server not keep the TCP socket connection open

		// Have the GetMethod object follow HTTP redirects for us
		get.setFollowRedirects(true);

		// Make a HttpClient object which will connect to a Web site, send our request, and get the response
		HttpClient client = HttpClientManager.getNewClient(HTTP_TRACKER_TIMEOUT, HTTP_TRACKER_TIMEOUT);

		try {

			// Connect to the Web server, send the GET request we composed, and get the response
			client.executeMethod(get); // Control waits here until the server responds, or 25 seconds expire

			// Get the tracker's response
			if (get.getResponseContentLength() > 32768) return null; // Make sure it's 32 KB or less
			byte[] response = get.getResponseBody();
			if (response == null) return null; // No response, return null instead of a TrackerResponse object
			if (LOG.isDebugEnabled()) LOG.debug(new String(response));

			// Parse the bencoded data into a Java HashMap of other Java objects, and make a new TrackerResponse object from them
			return new TrackerResponse(Token.parse(response));

		// The HttpClient object couldn't connect to the Web address we gave it
		} catch (IOException e) {

			// Return null instead of a TrackerResponse object
			return null;

		// Do this last, if there was an exception or not
		} finally {

			// Release our end of the HTTP socket connection the GetMethod made
			if (get != null) get.releaseConnection();
		}
	}

	/**
	 * Add a name and value pair to a Web query string.
	 * Composes text like "?name=value", or adds another one like "&name2=value2".
	 * 
	 * @param buf   A StringBuffer that contains the query text so far.
	 *              We'll add more text to it.
	 * @param key   The text of a key, like "name".
	 * @param value The text of the key's value, like "value".
	 * @return      The same StringBuffer buf.
	 *              Returning buf isn't really necessary, because the caller already has it.
	 */
	private static StringBuffer addGetField(StringBuffer buf, String key, String value) {

		// Write the character that will separate this parameter from what's already there
		if (buf.length() == 0) buf.append(QUESTION_MARK); // If this is the start of the query string, being with a "?"
		else buf.append(AND);                             // Otherwise, separate it from those already there with "&"

		// Write the parameter, like "key=value"
		buf.append(key);
		buf.append(EQUALS);
		buf.append(value);

		// Return the StringBuffer we added text to
		return buf;
	}
}
