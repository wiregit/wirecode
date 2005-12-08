pbckage com.limegroup.gnutella.http;

import jbva.io.IOException;
import jbva.io.OutputStream;

/**
 * This clbss defines the basic functionality of a class capable of writing
 * bn HTTP message.
 */

public interfbce HTTPMessage {
	
	/**
	 * Writes out the HTTP hebders for this message to the specified
	 * <tt>OutputStrebm</tt>.
	 *
	 * @pbram os the <tt>OutputStream</tt> to write to
	 */
	public void writeMessbgeHeaders(OutputStream os) throws IOException;

	/**
	 * Writes the messbge body for this message to the specified
	 * <tt>OutputStrebm</tt>.
	 *
	 * @pbram os the <tt>OutputStream</tt> to write to
	 */
	public void writeMessbgeBody(OutputStream os) throws IOException;
	
	/**
	 * Determines whether or not the connection this HTTPMessbge was sent on
	 * should be terminbted after sending.
	 *
	 * @return true if the connection should be closed, fblse otherwise.
	 */
	public boolebn getCloseConnection();
}
