package com.limegroup.gnutella.statistics;

import com.sun.java.util.collections.*;

public final class MessageStatistics {

	private final List MESSAGE_LIST = new LinkedList();

	/**
	 * Variable for the total number of pings received over TCP.
	 */
	private static int _totalTCPPingRequests = 0;

	/**
	 * Variable for the total number of pongs received over TCP.
	 */
	private static int _totalTCPPingReplies = 0;

	/**
	 * Variable for the total number of queries received over TCP.
	 */
	private static int _totalTCPQueryRequests = 0;

	/**
	 * Variable for the total number of replies received over TCP.
	 */
	private static int _totalTCPQueryReplies = 0;

	/**
	 * Variable for the total number of push requests received over TCP.
	 */
	private static int _totalTCPPushRequests = 0;

	/**
	 * Variable for the total number of route table messages received 
	 * over TCP.
	 */
	private static int _totalTCPRouteTableMessages = 0;

	/**
	 * Variable for the current number of pings received over TCP.
	 */
	private static int _currentTCPPingRequests = 0;

	/**
	 * Variable for the current number of pongs received over TCP.
	 */
	private static int _currentTCPPingReplies = 0;

	/**
	 * Variable for the current number of queries received over TCP.
	 */
	private static int _currentTCPQueryRequests = 0;

	/**
	 * Variable for the current number of replies received over TCP.
	 */
	private static int _currentTCPQueryReplies = 0;

	/**
	 * Variable for the current number of push requests received over TCP.
	 */
	private static int _currentTCPPushRequests = 0;

	/**
	 * Variable for the current number of route table messages received 
	 * over TCP.
	 */
	private static int _currentTCPRouteTableMessages = 0;

	/**
	 * Variable for the current number of messages filtered over TCP.
	 */
	private static int _currentFilteredTCPMessages = 0;

	/**
	 * Variable for the total number of messages received over TCP
	 * that have been filtered.
	 */
	private static int _totalFilteredTCPMessages = 0;

	/**
	 * Variable for the total number of TCP messages received.
	 */
	private static long _totalTCPMessages = 0;

	/**
	 * Variable for the total number of pings received over UDP.
	 */
	private static int _totalUDPPingRequests = 0;

	/**
	 * Variable for the total number of pongs received over UDP.
	 */
	private static int _totalUDPPingReplies = 0;

	/**
	 * Variable for the total number of queries received over UDP.
	 */
	private static int _totalUDPQueryRequests = 0;

	/**
	 * Variable for the total number of replies received over UDP.
	 */
	private static int _totalUDPQueryReplies = 0;

	/**
	 * Variable for the total number of push requests received over UDP.
	 */
	private static int _totalUDPPushRequests = 0;

	/**
	 * Variable for the total number of route table messages received 
	 * over UDP.
	 */
	private static int _totalUDPRouteTableMessages = 0;

	/**
	 * Variable for the total number of messages received over UDP
	 * that have been filtered.
	 */
	private static int _totalFilteredUDPMessages = 0;

	/**
	 * Variable for the current number of pings received over UDP.
	 */
	private static int _currentUDPPingRequests = 0;

	/**
	 * Variable for the current number of pongs received over UDP.
	 */
	private static int _currentUDPPingReplies = 0;

	/**
	 * Variable for the current number of queries received over UDP.
	 */
	private static int _currentUDPQueryRequests = 0;

	/**
	 * Variable for the current number of replies received over UDP.
	 */
	private static int _currentUDPQueryReplies = 0;

	/**
	 * Variable for the current number of push requests received over UDP.
	 */
	private static int _currentUDPPushRequests = 0;

	/**
	 * Variable for the current number of route table messages received 
	 * over UDP.
	 */
	private static int _currentUDPRouteTableMessages = 0;

	/**
	 * Variable for the current number of messages filtered over UDP.
	 */
	private static int _currentFilteredUDPMessages = 0;

	/**
	 * Variable for the total number of UDP messages received.
	 */
	private static long _totalUDPMessages = 0;
    
	/**
	 * Variable for the total number of messages received.
	 */
	private static long _totalMessages = 0;

	/**
	 * Variable for the total number of routing errors.
	 */
	private static int _totalRouteErrors = 0;

	private MessageStatistics() {}

	/**
	 * Adds a new TCP ping request to the message statistics.
	 */
	public static void addTCPPingRequest() {
		_totalTCPPingRequests++;
		_currentTCPPingRequests++;
		addTCPMessage();
	}

	/**
	 * Adds a new TCP ping reply to the message statistics.
	 */
	public static void addTCPPingReply() {
		_totalTCPPingReplies++;
		_currentTCPPingReplies++;
		addTCPMessage();
	}

	/**
	 * Adds a new TCP query request to the message statistics.
	 */
	public static void addTCPQueryRequest() {
		_totalTCPQueryRequests++;
		_currentTCPQueryRequests++;
		addTCPMessage();
	}

	/**
	 * Adds a new TCP query reply to the message statistics.
	 */
	public static void addTCPQueryReply() {
		_totalTCPQueryReplies++;
		_currentTCPQueryReplies++;
		addTCPMessage();
	}


	/**
	 * Adds a new TCP push request to the message statistics.
	 */
	public static void addTCPPushRequest() {
		_totalTCPPushRequests++;
		_currentTCPPushRequests++;
		addTCPMessage();
	}
	
	/**
	 * Adds a new TCP route table message to the message statistics.
	 */
	public static void addTCPRouteTableMessage() {
		_totalTCPRouteTableMessages++;
		_currentTCPRouteTableMessages++;
		addTCPMessage();
	}

	/**
	 * Adds a filtered TCP message to the message statistics.
	 */
	public static void addFilteredTCPMessage() {
		_totalFilteredTCPMessages++;
		_currentFilteredTCPMessages++;
		addTCPMessage();
	}

	/**
	 * Adds a TCP message to the data.
	 */
	private static void addTCPMessage() {
		_totalTCPMessages++;
		_totalMessages++;
	}

	/**
	 * Adds a new UDP ping request to the message statistics.
	 */
	public static void addUDPPingRequest() {
		_totalUDPPingRequests++;
		_currentUDPPingRequests++;
		addUDPMessage();
	}

	/**
	 * Adds a new UDP ping reply to the message statistics.
	 */
	public static void addUDPPingReply() {
		_totalUDPPingReplies++;
		_currentUDPPingReplies++;
		addUDPMessage();
	}

	/**
	 * Adds a new UDP query request to the message statistics.
	 */
	public static void addUDPQueryRequest() {
		_totalUDPQueryRequests++;
		_currentUDPQueryRequests++;
		addUDPMessage();
	}

	/**
	 * Adds a new UDP query reply to the message statistics.
	 */
	public static void addUDPQueryReply() {
		_totalUDPQueryReplies++;
		_currentUDPQueryReplies++;
		addUDPMessage();
	}


	/**
	 * Adds a new UDP push request to the message statistics.
	 */
	public static void addUDPPushRequest() {
		_totalUDPPushRequests++;
		_currentUDPPushRequests++;
		addUDPMessage();
	}
	
	/**
	 * Adds a new UDP route table message to the message statistics.
	 */
	public static void addUDPRouteTableMessage() {
		_totalUDPRouteTableMessages++;
		_currentUDPRouteTableMessages++;
		addUDPMessage();
	}

	/**
	 * Adds a filtered UDP message to the message statistics.
	 */
	public static void addFilteredUDPMessage() {
		_totalFilteredUDPMessages++;
		_currentFilteredUDPMessages++;
		addUDPMessage();
	}

	/**
	 * Adds a UDP message to the data.
	 */
	private static void addUDPMessage() {
		_totalUDPMessages++;
		_totalMessages++;
	}

	/**
	 * Adds a routing error to the number of routing errors.a
	 */
	public static void addRouteError() {
		_totalRouteErrors++;
	}

	/**
	 * Accessor for the total number of dropped messages.
	 *
	 * @return the total number of dropped messages
	 */
	public static int getTotalDroppedMessages() {
		return (_totalRouteErrors + _totalFilteredUDPMessages +
				_totalFilteredTCPMessages);		
	}

	/**
	 * Accessor for the total number of routing errors.
	 *
	 * @return the total number of routing errors
	 */
	public static int getTotalRouteErrors() {
		return _totalRouteErrors;
	}

	/**
	 * Accessor for the total number of query requests.
	 *
	 * @return the total number of query requests.
	 */
	public static int getTotalQueryRequests() {
		return _totalTCPQueryRequests + _totalUDPQueryRequests;
	}

	/**
	 * Accessor for the total number of TCP ping requests.
	 *
	 * @return the total number of TCP ping requests
	 */
	public static int getTotalTCPPingRequests() {
		return _totalTCPPingRequests;
	}

	/**
	 * Accessor for the total number of TCP ping replies.
	 *
	 * @return the total number of TCP ping replies
	 */
	public static int getTotalTCPPingReplies() {
		return _totalTCPPingReplies;
	}

	/**
	 * Accessor for the total number of TCP query requests.
	 *
	 * @return the total number of TCP query requests
	 */
	public static int getTotalTCPQueryRequests() {
		return _totalTCPQueryRequests;
	}

	/**
	 * Accessor for the total number of TCP query replies.
	 *
	 * @return the total number of TCP query replies
	 */
	public static int getTotalTCPQueryReplies() {
		return _totalTCPQueryReplies;
	}

	/**
	 * Accessor for the total number of TCP push requests.
	 *
	 * @return the total number of TCP push requests
	 */
	public static int getTotalTCPPushRequests() {
		return _totalTCPPushRequests;
	}

	/**
	 * Accessor for the total number of TCP route table messages.
	 *
	 * @return the total number of TCP route table messages
	 */
	public static int getTotalTCPRouteTableMessages() {
		return _totalTCPRouteTableMessages;
	}

	/**
	 * Accessor for the current number of TCP ping requests.
	 *
	 * @return the current number of TCP ping requests
	 */
	public static int getCurrentTCPPingRequests() {
		return _currentTCPPingRequests;
	}

	/**
	 * Accessor for the current number of TCP ping replies.
	 *
	 * @return the current number of TCP ping replies
	 */
	public static int getCurrentTCPPingReplies() {
		return _currentTCPPingReplies;
	}

	/**
	 * Accessor for the current number of TCP query requests.
	 *
	 * @return the current number of TCP query requests
	 */
	public static int getCurrentTCPQueryRequests() {
		return _currentTCPQueryRequests;
	}

	/**
	 * Accessor for the current number of TCP query replies.
	 *
	 * @return the current number of TCP query replies
	 */
	public static int getCurrentTCPQueryReplies() {
		return _currentTCPQueryReplies;
	}

	/**
	 * Accessor for the current number of TCP push requests.
	 *
	 * @return the current number of TCP push requests
	 */
	public static int getCurrentTCPPushRequests() {
		return _currentTCPPushRequests;
	}

	/**
	 * Accessor for the current number of TCP route table messages.
	 *
	 * @return the current number of TCP route table messages
	 */
	public static int getCurrentTCPRouteTableMessages() {
		return _currentTCPRouteTableMessages;
	}

	/**
	 * Accessor for the total number of UDP ping requests.
	 *
	 * @return the total number of UDP ping requests
	 */
	public static int getTotalUDPPingRequests() {
		return _totalUDPPingRequests;
	}

	/**
	 * Accessor for the total number of UDP ping replies.
	 *
	 * @return the total number of UDP ping replies
	 */
	public static int getTotalUDPPingReplies() {
		return _totalUDPPingReplies;
	}

	/**
	 * Accessor for the total number of UDP query requests.
	 *
	 * @return the total number of UDP query requests
	 */
	public static int getTotalUDPQueryRequests() {
		return _totalUDPQueryRequests;
	}

	/**
	 * Accessor for the total number of UDP query replies.
	 *
	 * @return the total number of UDP query replies
	 */
	public static int getTotalUDPQueryReplies() {
		return _totalUDPQueryReplies;
	}

	/**
	 * Accessor for the total number of UDP push requests.
	 *
	 * @return the total number of UDP push requests
	 */
	public static int getTotalUDPPushRequests() {
		return _totalUDPPushRequests;
	}

	/**
	 * Accessor for the total number of UDP route table messages.
	 *
	 * @return the total number of UDP route table messages
	 */
	public static int getTotalUDPRouteTableMessages() {
		return _totalUDPRouteTableMessages;
	}

	/**
	 * Accessor for the current number of UDP ping requests.
	 *
	 * @return the current number of UDP ping requests
	 */
	public static int getCurrentUDPPingRequests() {
		return _currentUDPPingRequests;
	}

	/**
	 * Accessor for the current number of UDP ping replies.
	 *
	 * @return the current number of UDP ping replies
	 */
	public static int getCurrentUDPPingReplies() {
		return _currentUDPPingReplies;
	}

	/**
	 * Accessor for the current number of UDP query requests.
	 *
	 * @return the current number of UDP query requests
	 */
	public static int getCurrentUDPQueryRequests() {
		return _currentUDPQueryRequests;
	}

	/**
	 * Accessor for the current number of UDP query replies.
	 *
	 * @return the current number of UDP query replies
	 */
	public static int getCurrentUDPQueryReplies() {
		return _currentUDPQueryReplies;
	}

	/**
	 * Accessor for the current number of UDP push requests.
	 *
	 * @return the current number of UDP push requests
	 */
	public static int getCurrentUDPPushRequests() {
		return _currentUDPPushRequests;
	}

	/**
	 * Accessor for the current number of UDP route table messages.
	 *
	 * @return the current number of UDP route table messages
	 */
	public static int getCurrentUDPRouteTableMessages() {
		return _currentUDPRouteTableMessages;
	}
	
	/**
	 * Accessor for the total number of messages passed.
	 *
	 * @return the total number of messages passed
	 */
	public static long getTotalMessages() {
		return _totalMessages;
	}
}
