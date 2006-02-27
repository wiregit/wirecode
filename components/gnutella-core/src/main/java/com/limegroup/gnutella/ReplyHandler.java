
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.io.IOException;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.messages.vendor.StatisticVendorMessage;
import com.limegroup.gnutella.util.IpPort;

/**
 * ManagedConnection, UDPReplyHandler, and ForMeReplyHandler implement the ReplyHandler interface to be listed in a RouteTable and get pong and query hit packets.
 * 
 * A reply packet is a packet a computer prepared as a reply to a request packet.
 * A ping is a request packet, and a pong is its reply.
 * A query is a request packet, and a query hit is its reply.
 * The two kinds of reply packets are pongs and query hits. (ask)
 * 
 * All Gnutella packets have message GUIDs that identify them on the network.
 * Reply packets have the same GUID as the request packet they're replying to. (ask)
 * 
 * On the Gnutella network, reply packets need to find their way back to the computer that sent the initial request.
 * Gnutella computers accomplish this by performing a process called routing.
 * Before a computer broadcasts a request packet forward, it makes a note of its GUID and which connection it came from.
 * This list of notes is called the route table.
 * Later, a response packet with the same GUID might arrive.
 * The computer finds the GUID in the route table, and remembers which computer wanted the response.
 * 
 * In LimeWire, the ManagedConnection, UDPReplyHandler, and ForMeReplyHandler classes implement the ReplyHandler interface.
 * All 3 represent computers that we can send reply packets to.
 * A ManagedConnection object represents a remote computer we have a TCP socket Gnutella connection with that began with the Gnutella handshake.
 * A UDPReplyHandler object represents a remote computer we've been exchanging UDP packets with.
 * The ForMeReplyHandler object represents us, and gets the reply packets addressed to us.
 * 
 * A ReplyHandler is usually a remote computer on the Internet running Gnutella software.
 * If a given ReplyHandler is a ManagedConnection, it's a remote computer we have a Gnutella TCP socket connection with.
 * If a given ReplyHandler is a UDPReplyHandler object, it's a remote computer that sent us a Gnutella message in a UDP packet.
 * Or, the given ReplyHandler can be the ForMeReplyHandler object.
 * In this case, the ReplyHandler isn't a remote computer, it's us.
 * 
 * The ReplyHandler interface outlines the methods your object will have to have if it wants to take and deliver reply packets.
 * For instance, it has to have a handlePingReply() method that can take a ping we've already determined is meant for it.
 * 
 * ReplyHandler extends IpPort, the interface that means your object has IP address and port number information.
 * ReplyHandler objects that accept packets represent computers on the Internet, and have IP addresses.
 */
public interface ReplyHandler extends IpPort {

    /**
     * Send the given pong to this remote computer.
     * MangedConnection.handlePingReply() sends the pong to this remote computer through our TCP Gnutella connection with it.
     * UDPReplyHandler.handlePingReply() wraps the pong in a UDP packet and sends it to the remote computer's IP address and port number.
     * 
     * If the ReplyHandler is us, process the packet.
     * ForMeReplyHandler.handlePingReply() does nothing.
     * 
     * @param pingReply A pong we've received, and should send to the computer this ReplyHandler represents
     * @param handler   The computer that sent it to us, only ForMeReplyHandler uses this
     */
    void handlePingReply(PingReply pingReply, ReplyHandler handler);

    /**
     * Send the given query hit to this remote computer.
     * MangedConnection.handleQueryReply() sends the query hit to this remote computer through our TCP Gnutella connection with it.
     * We may have gotten the query hit by UDP, but ManagedConnection will send it through TCP.
     * UDPReplyHandler.handleQueryReply() wraps the query hit in a UDP packet and sends it to the remote computer's IP address and port number.
     * 
     * If the ReplyHandler is us, process the packet.
     * ForMeReplyHandler.handlePingReply() passes the packet to SearchResultHandler.handleQueryReply(reply) and then DownloadManager.handleQueryReply(reply).
     * 
     * @param queryReply A query hit we've received, and should send to the computer this ReplyHandler represents
     * @param handler    The computer that sent it to us, only ForMeReplyHandler uses this
     */
    void handleQueryReply(QueryReply queryReply, ReplyHandler handler);

    /**
     * Send the given push packet to this remote computer.
     * MangedConnection.handlePushRequest() sends the push to this remote computer through our TCP Gnutella connection with it.
     * UDPReplyHandler.handlePushRequest() wraps the push in a UDP packet and sends it to the remote computer's IP address and port number.
     * 
     * If the ReplyHandler is us, process the packet.
     * ForMeReplyHandler.handlePushRequest() calls PushManager.acceptPushUpload() to have us push open the connection and give the file.
     * 
     * @param pushRequest A push we've received, and should send to the computer this ReplyHandler represents
     * @param handler     The computer that sent it to us, only ForMeReplyHandler uses this
     */
    void handlePushRequest(PushRequest pushRequest, ReplyHandler handler);

    /**
     * The number of Gnutella packets this remote computer has sent us.
     * ManagedConnection.getNumMessagesReceived() returns the count.
     * UDPReplyHandler and ForMeReplyHandler don't keep track, and return 0.
     * 
     * @return The number of Gnutella packets this remote computer has sent us.
     *         0 if we don't know because it's UDP or it's us.
     */
	int getNumMessagesReceived();

    /**
     * Have the ConnectionStats object count another message we received from this remote computer, and then dropped.
     * ManagedConnection.countDroppedMessage() do this.
     * UDPReplyHandler and ForMeReplyHandler do nothing.
     */
	void countDroppedMessage();

    /**
     * Determine if the given Gnutella packet passes through our personal SpamFilter, letting us show its information to the user.
     * ManagedConnection.isPersonalSpam() and UDPReplyHandler.isPersonalSpam() pass the message through our personal SpamFilter.
     * ForMeReplyHandler.isPersonalSpam() always returns false, letting the message pass through.
     * 
     * @param m A gnutella packet we've received
     * @return  True to hide it from the user, false if it's fine
     */
	boolean isPersonalSpam(Message m);

    /**
     * Determine if we initiated this connection to the remote computer.
     * ManagedConnection inherits isOutgoing() from Connection, which returns the answer.
     * UDPReplyHandler and ForMeReplyHandler always return false because they have no connections.
     * 
     * @return True if we initiated a TCP socket Gnutella connection to this remote computer.
     *         False if the remote computer connected to us.
     *         False if there is no connection at all.
     */
	boolean isOutgoing();

	/**
     * Determine if the program has permission to close this connection.
     * ManagedConnection.isKillable() always returns true to let the ConnectionWatchdog close TCP socket Gnutella connections.
     * UDPReplyHandler and ForMeReplyHandler always return false because there is no connection to close.
     * 
     * @return True if this is a TCP socket Gnutella connection.
     *         False if there is no connection at all.
	 */
	boolean isKillable();

	/**
     * Determine if we're an ultrapeer and the remote computer is a leaf connected to us with a TCP socket Gnutella connection.
     * Returns true if the computer receiving these replies is one of our leaves.
     * Connection.isSupernodeClientConnection() returns true if we're an ultrapeer and the remote computer is a leaf.
     * UDPReplyHandler and ForMeReplyHandler always return false.
     * 
     * @return True if we're an ultrapeer, and the remote computer is a leaf.
     *         False if we have some other relationship with the remote computer through a TCP socket connection.
     *         False if this ReplyHandler represents ourselves.
     *         False if we're communicating with this ReplyHandler with UDP packets.
	 */
	boolean isSupernodeClientConnection();

    /**
     * Determine if we can still send a packet with this connection.
     * Connection.isOpen() returns true or false depending on if the TCP socket connection is open or closed.
     * UDPReplyHandler.isOpen() always returns true because UDP doesn't have a connection that can be closed.
     * ForMeReplyHandler.isOpen() always returns true because we can always send a message to ourselves.
     * 
     * @return True if the TCP socket connection is open.
     *         True if we're using UDP, which doesn't need a connection.
     *         True if we're sending information to ourselves.
     *         False if the TCP socket connection is closed.
     */
    boolean isOpen();

	/**
     * Determine if this is a connection to a leaf.
     * Connection.isLeafConnection() returns true if the remote computer told us "X-Ultrapeer: false" in the handshake.
     * UDPReplyHandler.isLeafConnection() always returns false.
     * ForMeReplyHandler.isLeafConnection() returns true if we are a leaf.
     * 
     * @return True if the remote computer said "X-Ultrapeer: false", false if the remote computer is an ultrapeer.
     *         If this ReplyHandler is the ForMeReplyHandler representing us, true if we're a leaf, false if we're an ultrapeer.
     *         False if this ReplyHandler is UDP.
	 */
	boolean isLeafConnection();

	/**
     * Determine if the remote computer, as an ultrapeer, will try to keep connections to 15 or more other ultrapeers.
     * Connection.isHighDegreeConnection() returns true if the remote computer said "X-Degree: 15" or higher.
     * UDPReplyHandler and ForMeReplyHandler return false.
     * 
     * @return True if the remote computer said "X-Degree: 15" or higher.
     *         False if it said less, this ReplyHandler is ourselves, or it's using UDP.
	 */
	boolean isHighDegreeConnection();

    /**
     * Determine if this connection is to an ultrapeer that can exchange query routing tables with other ultrapeers.
     * Connection.isUltrapeerQueryRoutingConnection() returns true if the remote computer said "X-Ultrapeer-Query-Routing: 0.1".
     * UDPReplyHandler and ForMeReplyHandler return false.
     * 
     * @return True if the remote computer said "X-Ultrapeer-Query-Routing: 0.1" or higher.
     *         False if it didn't, this ReplyHandler is ourselves, or it's using UDP.
     */
    boolean isUltrapeerQueryRoutingConnection();

    /**
     * Determine if this connection is to a computer that has the advanced Gnutella features good ultrapeers need.
     * This doesn't mean the remote computer is an ultrapeer.
     * Connection.isGoodUltrapeer() returns true if the Gnutella handshake headers the remote computer told us advertise these features.
     * UDPReplyHandler and ForMeReplyHandler return false.
     * 
     * @return True if this is a TCP Gnutella connection to a computer that advertised good ultrapeer features in the Gnutella handshake.
     *         False if it didn't, or this isn't a TCP Gnutella connection to a remote computer.
     */
    boolean isGoodUltrapeer();

    /**
     * Determine if this connection is to a computer that has the advanced Gnutella features good leaves need.
     * This doesn't mean the remote computer is a leaf.
     * Connection.isGoodLeaf() returns true if the Gnutella handshake headers the remote computer told us advertise these features.
     * UDPReplyHandler and ForMeReplyHandler return false.
     * 
     * @return True if this is a TCP Gnutella connection to a computer that advertised good leaf features in the Gnutella handshake.
     *         False if it didn't, or this isn't a TCP Gnutella connection to a remote computer.
     */
    boolean isGoodLeaf();

    /**
     * Determine if this connection is to a computer that supports pong caching.
     * Connection.supportsPongCaching() returns true if the remote computer said "Pong-Caching: 0.1" or higher in the handshake.
     * UDPReplyHandler.supportsPongCaching() returns false because there is no handshake with UDP and we don't know.
     * ForMeReplyHandler.supportsPongCaching() returns true, because we do.
     * 
     * @return True if the remote computer said "Pong-Caching: 0.1" or higher, or this ReplyHandler is us.
     *         False if it didn't, or this ReplyHandler is using UDP.
     */
    boolean supportsPongCaching();

    /**
     * Determine if we'll let this remote computer ping us right now.
     * This makes sure a computer doesn't ping us too frequently.
     * Connection.allowNewPings() first returns true 5 seconds into a new connection, and only once every 2.5 seconds after that.
     * UDPReplyHandler and ForMeReplyHandler both always return true, not applying this restriction.
     * 
     * @return True if the TCP Gnutella connection hasn't pinged us in a few seconds.
     *         False if it has.
     *         True to allow pings from ourself.
     *         True if this is a UDP computer, which we don't control this way.
     */
    boolean allowNewPings();

    /**
     * Determine if our connection with this remote computer seems stable.
     * Considers TCP Gnutella connections unstable for the first 5 seconds, and stable after that.
     * Considers UDP unstable.
     * 
     * Connection.isStable() returns true after 5 seconds of Gnutella packet exchange through it.
     * UDPReplyHandler.isStable() returns false.
     * ForMeReplyHandler.isStable() returns true.
     * 
     * @return True if we've been exchanging packets over a TCP socket connection for more than 5 seconds.
     *         False if it hasn't been 5 seconds yet.
     *         True if this ReplyHandler is us.
     *         False if this ReplyHandler is using UDP.
     */
    boolean isStable();

    /**
     * The packet handling computer's language preference, like "en" for English.
     * Connection.getLocalePref() returns what the remote computer said in the Gnutella handshake, like "X-Locale-Pref: en".
     * UDPReplyHandler.getLocalePref() always returns "en", because most Gnutella computers prefer English and we don't find out through UDP.
     * ForMeReplyHandler.getLocalePref() returns our language preference from ApplicationSettings.
     * 
     * @return The language preference the remote computer told us in the Gnutella handshake.
     *         Our language preference if this ReplyHandler is us.
     *         "en" for English if this ReplyHandler is UDP, meaning we don't know.
     */
    public String getLocalePref();

    /**
     * Send the given StatisticVendorMessage to this remote computer.
     * Connection.handleStatisticVM() sends the message.
     * UDPReplyHandler.handleStatisticVM() sends the message.
     * ForMeReplyHandler.handleStatisticVM() logs an error.
     * 
     * @param m The StatisticVendorMessage to send
     */
    public void handleStatisticVM(StatisticVendorMessage m) throws IOException;

    /**
     * Send the given Gnutella packet to this remote computer.
     * ManagedConnection.reply() sends the packet through its TCP Gnutella connection with the remote computer.
     * UDPReplyHandler.reply() sends the packet in a UDP packet to the IP address and port number its setup with.
     * ForMeReplyHandler.reply() does nothing.
     * 
     * @param m A Gnutella packet to send to the remote computer
     */
    public void reply(Message m);

    /**
     * Send the given SIMPP vendor message to this remote computer.
     * SIMPP is the system of signed messages that lets the company LimeWire communicate directly with LimeWire programs running on the Internet.
     * Connection.handleSimppVM() sends the message.
     * UDPReplyHandler.handleSimppVM() does nothing because SIMPP vendor messages only travel over TCP Gnutella connections, not UDP.
     * ForMeReplyHandler.handleSimppVM() logs an error.
     * 
     * @param simppVM The SIMPP vendor message to send
     */
    public void handleSimppVM(SimppVM simppVM) throws IOException;

    /**
     * Get the remote computer's client ID GUID that uniquely identifies it on the Gnutella network.
     * ManagedConnection.getClientID() returns what we read from the end of a QueryReply packet the remote computer generated.
     * UDPReplyHandler.getClientID() returns 16 bytes of 0s.
     * ForMeReplyHandler.getClientID() calls RouterService.getMyGUID() to return our own Gnutella client ID.
     * 
     * @return A byte array of 16 bytes with the remote computer's client ID GUID, ours, or all 0s.
     */
    public byte[] getClientGUID();
}
