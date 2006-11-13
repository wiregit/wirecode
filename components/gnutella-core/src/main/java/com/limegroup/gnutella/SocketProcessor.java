package com.limegroup.gnutella;

import java.net.Socket;

public interface SocketProcessor {

	/** Accepts the given socket. */
	public void processSocket(Socket client);

	/**
	 * Accepts the given incoming socket, allowing only the given protocol.
	 * If allowedProtocol is null, all are allowed.
	 */
	public void processSocket(Socket client, String allowedProtocol);

}