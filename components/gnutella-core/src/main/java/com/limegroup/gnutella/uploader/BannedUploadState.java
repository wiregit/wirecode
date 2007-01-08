package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Sends a 403 Banned For Hammering reply to a greedy client
 */
public class BannedUploadState extends UploadState {


	/**
	 * Constant for the error message to send.
	 */
	public static final byte[] ERROR_MESSAGE =
		"Your client sends too many requests.".getBytes();

	/**
	 * Write HTTP headers
	 * 
	 * @param os
	 *            the <tt>OutputStream</tt> to write to.
	 * @throws IOException
	 *             if there was a problem writing to the <tt>OutputStream</tt>.
	 */
	public void writeMessageHeaders(OutputStream os) throws IOException {
		String str;
		str = "HTTP/1.1 403 Banned\r\n";
		os.write(str.getBytes());
		str = "Server: " + LimeWireUtils.getHttpServer() + "\r\n";
		os.write(str.getBytes());
		str = "Content-Type: text/plain\r\n";
		os.write(str.getBytes());
		str = "Content-Length: " + ERROR_MESSAGE.length + "\r\n";
		os.write(str.getBytes());
		str = "\r\n";
		os.write(str.getBytes());
	}

	/**
	 * Write HTTP message body
	 * 
	 * @param os
	 *            the <tt>OutputStream</tt> to write to.
	 * @throws IOException
	 *             if there was a problem writing to the <tt>OutputStream</tt>.
	 */
	public void writeMessageBody(OutputStream os) throws IOException {
		os.write(ERROR_MESSAGE);
	}

	/**
	 * @return <tt>true</tt> if the connection should be closed after writing
	 *         the message.
	 */
	public boolean getCloseConnection() {
		return true;
	}
}
