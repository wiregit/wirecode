package com.limegroup.gnutella.messages;

/**
 * Contains utility methods for messages.
 */
public final class MessageUtils {

	/**
	 * Returns whether or not the specified port is within the valid range of
	 * ports.
	 *
	 * @param port the port number to check
	 */
	public static boolean isValidPort(int port) {
		if((port & 0xFFFF0000) != 0) return false;
		return true;
	}
}
