package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.gnutella.RouterService;

/**
 * indicates that we will not upload anything to the remote host
 */
public class BTAltLocRequest extends BTMessage {
	/**
	 * sender's listening port
	 */
	private int _port;

	/**
	 * caching message payload
	 */
	private ByteBuffer _payload = null;

	/**
	 * factory method
	 * 
	 * @return new instance of BTAltLocRequest
	 */
	public static BTAltLocRequest createMessage() {
		int port = RouterService.acceptedIncomingConnection() ? RouterService
				.getPort() : 0;
		return new BTAltLocRequest(port);
	}

	/**
	 * Create BTAltLocRequest from network
	 * 
	 * @param payload
	 *            ByteBuffer with data from network
	 * @return new instance of BTAltLocRequest
	 * @throws BadBTMessageException
	 *             if data from network was bad.
	 */
	public static BTAltLocRequest readMessage(ByteBuffer payload)
			throws BadBTMessageException {
		// check message.
		// the payload is a 2 byte big endian port, - we don't care for it,
		// though. XBT who invented this message just wanted us to send it
		if (payload.remaining() != 2) {
			byte[] msg = new byte[payload.remaining()];
			payload.get(msg);
			throw new BadBTMessageException(
					"unexpected payload in altlocrequest message: "
							+ new String(msg));
		}
		payload.order(ByteOrder.BIG_ENDIAN);
		return new BTAltLocRequest(0 | payload.getShort());
	}

	/**
	 * Constructor called by BTMessage.parseMessage()
	 */
	private BTAltLocRequest(int port) {
		super(ALT_LOC_REQUEST);
		_port = port;
	}

	/**
	 * @return bittorrent listening port of message sender
	 */
	public int getPort() {
		return _port;
	}

	public ByteBuffer getPayload() {
		if (_payload == null) {
			_payload = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
					.putShort((short) _port).asReadOnlyBuffer();
		}
		_payload.clear();
		return _payload;
	}
	
	public String toString() {
		return "BTAltLocRequest";
	}
}
