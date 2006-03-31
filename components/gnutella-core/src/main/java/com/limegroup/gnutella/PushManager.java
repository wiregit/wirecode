
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Sockets;

/**
 * Call pushManager.acceptPushUpload() when we get a push packet.
 * It will make a thread that pushes open a connection to the remote computer, upload the file it requests, and exit.
 * 
 * The remote computer wants a file from us.
 * There are 3 ways it can get it:
 * 
 * (1) Connect and download.
 * We're receiving a push packet because we're not externally contactable on the Internet.
 * If we could accept an incoming TCP socket connection, the remote computer that wants our file would just connect to us.
 * 
 * (2) Push open a TCP socket connection.
 * There are 2 ways acceptPushUpload can push open the connection to the remote computer that wants it.
 * If the remote computer is externally contactable, the push packet will tell us to just open a new TCP socket connection to its listening socket.
 * acceptPushUpload() does this with a call to Sockets.connect().
 * 
 * (3) Open a firewall-to-firewall UDP connection.
 * If both the remote computer and we are behind network address translation devices, we can still connect for the file transfer.
 * The push packet has a file ID of 2147483645, marking it as requesting a firewall-to-firewall transfer.
 * acceptPushUpload() calls the UDPConnection() constructor, which starts sending UDP packets at the remote computer.
 * It's doing exactly the same thing, and soon, they start going through.
 * We then use the stream of UDP packets just like a regular TCP socket connection.
 * 
 * The first thing we tell the remote computer when we connect to it is:
 * 
 * GIV 0:219A7298C72905B60534EDA54AD3D500/file\n\n
 * 
 * According to the original Gnutella specification, this is supposed to be:
 * GIV <File Index>:<Servent Identifier>/<File Name>\n\n
 * The file index and file name should identify the file the remote computer requested, and we will now upload to it.
 * 
 * Gnutella programs don't use the file index or file name any longer.
 * We provide a file index of 0 and a file name of the text "file".
 * The important piece of information is our client ID GUID, which tells the remote computer it's us.
 * 
 * The first thing the remote computer says is "GET" or "HEAD".
 * This lets it request and download the file the normal HTTP way, or just request header information.
 */
public final class PushManager {

    /** A debugging log we can write lines of text to as the program runs. */
    private static final Log LOG = LogFactory.getLog(PushManager.class);

    /**
     * 10000, 10 seconds in milliseconds.
     * If it takes longer than 10 seconds to connect a TCP socket connection to the remote computer, we'll give up.
     */
    private static final int CONNECT_TIMEOUT = 10000;

	/**
     * Make a thread that pushes open a connection to a remote computer, uploads a file it requests, and exits.
     * 
     * ForMeReplyHandler.handlePushRequest() calls this when we've received a push packet addressed to our client ID GUID.
     * Makes a new thread named "PushUploadThread" which performs the necessary steps, and exits.
     * The thread makes a new UDPConnection object, or calls Sockets.connect() to open a connection to the remote computer.
     * It greets the remote computer with "GIV 0:219A7298C72905B60534EDA54AD3D500/file\n\n", telling it our client ID GUID.
     * The remote computer requests a file with a HTTP "GET".
     * Our UploadManager serves the file.
     * When it's done, control returns here, where acceptPushUpload() closes the connection, and exits, ending the thread.
     * 
	 * Accepts a new push upload.
     * NON-BLOCKING: creates a new thread to transfer the file.
     * 
     * The thread connects to the other side, waits for a GET/HEAD,
     * and delegates to the UploaderManager.acceptUpload method with the
     * socket it created.
     * Essentially, this is a reverse-Acceptor.
     * 
     * No file and index are needed since the GET/HEAD will include that
     * information.  Just put in our first file and filename to create a
     * well-formed.
     * 
     * @param host         The IP address to push a new connection open to, like "72.139.164.14"
     * @param port         The port number to push a new connection open to
     * @param guid         The client ID GUID that adressed the push to us, our client ID GUID, as base 16 text
     * @param forceAllow   True if the computer we'll push open the connection to is on the same LAN as us, so we'll upload the file without waiting for an upload slot
     * @param isFWTransfer The file ID is the special code indicating we should open a firewall-to-firewall UDP connection
	 */
	public void acceptPushUpload(final String host, final int port, final String guid, final boolean forceAllow, final boolean isFWTransfer) {

        // Make a note in the debugging log, and make sure the caller gave us everything we need
        if (LOG.isDebugEnabled()) LOG.debug("acceptPushUp ip:" + host + " port:" + port + " FW:" + isFWTransfer);
        if (host == null) throw new NullPointerException("null host");
        if (!NetworkUtils.isValidPort(port)) throw new IllegalArgumentException("invalid port: " + port);
        if (guid == null) throw new NullPointerException("null guid");

        // Access the program's FileManager object, which keeps a list of our shared files and their XML metadata
        FileManager fm = RouterService.getFileManager();

        /*
         * TODO: why is this check here?  it's a tiny optimization,
         * but could potentially kill any sharing of files that aren't
         * counted in the library.
         */

        // If we're not sharing any files, we don't have any reason to push open a connection to give a remote computer a file it wants, leave now
        if (fm.getNumFiles() < 1 && fm.getNumIncompleteFiles() < 1) return;

        /*
         * We used to have code here that tested if the guy we are pushing to is
         * 1) hammering us, or 2) is actually firewalled.  1) is done above us
         * now, and 2) isn't as much an issue with the advent of connectback
         */

        // Define a new unnamed inner class that extends ManagedThread and overrides its managedRun() method
        Thread runner = new ManagedThread("PushUploadThread") { // Name the thread "PushUploadThread", and make a new object of this type called runner

            // The "PushUploadThread" thread will run this managedRun() method, and then exit
            public void managedRun() {

                // We'll point s at a new UDPConnection or NIOSocket object, both of which extend Socket to use its methods
                Socket s = null;

                try {

                    // Open a new connection to the given IP address and port number
                    if (isFWTransfer) s = new UDPConnection(host, port);                // The push packet requested a LimeWire firewall-to-firewall connection
                    else              s = Sockets.connect(host, port, CONNECT_TIMEOUT); // Open a traditional TCP socket connection

                    /*
                     * Both UDPConnection() and Sockets.connect() will block until they can make the connection.
                     * If nothing happens for too long, the one we called will throw an IOException.
                     * So, if control stays here, the connection is open.
                     */

                    /*
                     * Once we push the connection open to the remote computer, we tell it a single HTTP header, like this:
                     * 
                     * GIV 0:219A7298C72905B60534EDA54AD3D500/file\n\n
                     * 
                     * "219A7298C72905B60534EDA54AD3D500" is our client ID GUID.
                     * This tells the remote computer it's us pushing this connection open to them.
                     * "file" is just the text "file" in place of an actual file name.
                     * 0 is the number 0 in place of an actual file ID.
                     * We don't use the file ID and file name in the GIV greeting any longer.
                     * The line is terminated by two \n bytes, not \r\n.
                     * 
                     * Originally, a Gnutella program would push open a new connection to upload the file the remote computer requested.
                     * Now, push just means push open a new connection.
                     * Once the connection is open, the remote computer requests the file.
                     */

                    // Send the GIV greeting to the remote computer, telling it our client ID GUID
        			OutputStream ostream = s.getOutputStream(); // Get the OutputStream object we can call ostream.write(b) on to send data to the remote computer
        			String giv = "GIV 0:" + guid + "/file\n\n"; // Compose the HTTP greeting like "GIV 0:219A7298C72905B60534EDA54AD3D500/file"
        			ostream.write(giv.getBytes());              // Convert the String into ASCII characters, and send those bytes to the remote computer
        			ostream.flush();                            // Make sure we actually send the data to the remote computer now

                    // Tell the UDPConnection or NIOSocket to give the remote computer 30 seconds to reply
        			s.setSoTimeout(30 * 1000); // 30 seconds in milliseconds

                    /*
                     * read GET or HEAD and delegate appropriately.
                     */

                    // Read the first 4 bytes the remote computer tells us, and take the characters before the first space as a word
                    String word = IOUtils.readWord(s.getInputStream(), 4);

                    // If we successfully made a UDP connection, record it in statistics
                    if (isFWTransfer) UploadStat.FW_FW_SUCCESS.incrementStat();

                    // The remote computer said "GET", it wants to download a file from us
                    if (word.equals("GET")) {

                        // Give the connection to the UploadManager, which will upload the file and then return
                        UploadStat.PUSHED_GET.incrementStat();
                        RouterService.getUploadManager().acceptUpload(HTTPRequestMethod.GET, s, forceAllow);

                    // The remote computer said "HEAD", it wants to know if we have a file
                    } else if (word.equals("HEAD")) {

                        // Give the connection to the UploadManager, which will send the headers for our file and then return
                        UploadStat.PUSHED_HEAD.incrementStat();
                        RouterService.getUploadManager().acceptUpload(HTTPRequestMethod.HEAD, s, forceAllow);

                    // The remote computer said something else
                    } else {

                        // We can only do "GET" and "HEAD", throw an exception
                        UploadStat.PUSHED_UNKNOWN.incrementStat();
                        throw new IOException();
                    }

                // We couldn't open the connection, get the output stream, or get the first data the remote computer told us
                } catch (IOException ioe) {

                    // Record this in statistics
                    if (isFWTransfer) UploadStat.FW_FW_FAILURE.incrementStat();
                    UploadStat.PUSH_FAILED.incrementStat();

                // Run this code after the try block above, even if we got and caught an IOException
                } finally {

                    /*
                     * At this point, the UploadManager has finished serving the file the remote computer requested with "GET".
                     */

                    // If we made a UDPConnection or NIOSocket object which opened the connection
                    if (s != null) {

                        // Call all its close() methods
                        try { s.getInputStream().close(); }  catch (IOException ioe) {}
                        try { s.getOutputStream().close(); } catch (IOException ioe) {}
                        try { s.close(); }                   catch (IOException ioe) {}
                    }
                }
            }
        };

        // Have the "PushUploadThread" call the managedRun() method above, and then exit
        runner.setDaemon(true); // Let Java shut down even if the thread is still running
        runner.start();         // Have the thread start and run managedRun() now
	}
}
