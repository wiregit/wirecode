package com.limegroup.gnutella.http;

import java.io.*;

/**
 * This class defines the basic functionality of a class capable of writing
 * an HTTP message.
 */

public interface HTTPMessage {
	
	/**
	 * Writes out the HTTP headers for this message to the specified
	 * <tt>OutputStream</tt>.
	 *
	 * @param os the <tt>OutputStream</tt> to write to
	 */
	public void writeMessageHeaders(OutputStream os) throws IOException;

	/**
	 * Writes the message body for this message to the specified
	 * <tt>OutputStream</tt>.
	 *
	 * @param os the <tt>OutputStream</tt> to write to
	 */
	public void writeMessageBody(OutputStream os) throws IOException;

	/**
	 * Returns whether or not this HTTP message allows the connection to
	 * stay open, allowing another request on the same connection.
	 *
	 * @return <tt>true</tt> if this <tt>HTTPMessage</tt> supports multiple
	 *  requests, <tt>false</tt> otherwise
	 */
	public boolean getCloseConnection();
}
