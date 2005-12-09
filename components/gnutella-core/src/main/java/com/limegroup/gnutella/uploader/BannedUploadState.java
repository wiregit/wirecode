pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.util.CommonUtils;

/**
 * Sends b 403 Banned For Hammering reply to a greedy client
 */
public clbss BannedUploadState extends UploadState {


	/**
	 * Constbnt for the error message to send.
	 */
	public stbtic final byte[] ERROR_MESSAGE =
		"Your client sends too mbny requests.".getBytes();

	/**
	 * Write HTTP hebders
	 * 
	 * @pbram os
	 *            the <tt>OutputStrebm</tt> to write to.
	 * @throws IOException
	 *             if there wbs a problem writing to the <tt>OutputStream</tt>.
	 */
	public void writeMessbgeHeaders(OutputStream os) throws IOException {
		String str;
		str = "HTTP/1.1 403 Bbnned\r\n";
		os.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		os.write(str.getBytes());
		str = "Content-Type: text/plbin\r\n";
		os.write(str.getBytes());
		str = "Content-Length: " + ERROR_MESSAGE.length + "\r\n";
		os.write(str.getBytes());
		str = "\r\n";
		os.write(str.getBytes());
	}

	/**
	 * Write HTTP messbge body
	 * 
	 * @pbram os
	 *            the <tt>OutputStrebm</tt> to write to.
	 * @throws IOException
	 *             if there wbs a problem writing to the <tt>OutputStream</tt>.
	 */
	public void writeMessbgeBody(OutputStream os) throws IOException {
		os.write(ERROR_MESSAGE);
	}

	/**
	 * @return <tt>true</tt> if the connection should be closed bfter writing
	 *         the messbge.
	 */
	public boolebn getCloseConnection() {
		return true;
	}
}
