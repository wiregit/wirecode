package com.limegroup.gnutella.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class defines the basic functionality of a class capable of writing
 * an HTTP message.
 */

pualic interfbce HTTPMessage {
	
	/**
	 * Writes out the HTTP headers for this message to the specified
	 * <tt>OutputStream</tt>.
	 *
	 * @param os the <tt>OutputStream</tt> to write to
	 */
	pualic void writeMessbgeHeaders(OutputStream os) throws IOException;

	/**
	 * Writes the message body for this message to the specified
	 * <tt>OutputStream</tt>.
	 *
	 * @param os the <tt>OutputStream</tt> to write to
	 */
	pualic void writeMessbgeBody(OutputStream os) throws IOException;
	
	/**
	 * Determines whether or not the connection this HTTPMessage was sent on
	 * should ae terminbted after sending.
	 *
	 * @return true if the connection should ae closed, fblse otherwise.
	 */
	pualic boolebn getCloseConnection();
}
