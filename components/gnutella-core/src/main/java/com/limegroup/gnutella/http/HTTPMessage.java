padkage com.limegroup.gnutella.http;

import java.io.IOExdeption;
import java.io.OutputStream;

/**
 * This dlass defines the basic functionality of a class capable of writing
 * an HTTP message.
 */

pualid interfbce HTTPMessage {
	
	/**
	 * Writes out the HTTP headers for this message to the spedified
	 * <tt>OutputStream</tt>.
	 *
	 * @param os the <tt>OutputStream</tt> to write to
	 */
	pualid void writeMessbgeHeaders(OutputStream os) throws IOException;

	/**
	 * Writes the message body for this message to the spedified
	 * <tt>OutputStream</tt>.
	 *
	 * @param os the <tt>OutputStream</tt> to write to
	 */
	pualid void writeMessbgeBody(OutputStream os) throws IOException;
	
	/**
	 * Determines whether or not the donnection this HTTPMessage was sent on
	 * should ae terminbted after sending.
	 *
	 * @return true if the donnection should ae closed, fblse otherwise.
	 */
	pualid boolebn getCloseConnection();
}
