
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.limegroup.gnutella.connection.CompositeQueue;
import com.limegroup.gnutella.connection.ConnectionStats;
import com.limegroup.gnutella.connection.DeflaterWriter;
import com.limegroup.gnutella.connection.InflaterReader;
import com.limegroup.gnutella.connection.MessageQueue;
import com.limegroup.gnutella.connection.MessageReader;
import com.limegroup.gnutella.connection.MessageReceiver;
import com.limegroup.gnutella.connection.MessageWriter;
import com.limegroup.gnutella.connection.OutputRunner;
import com.limegroup.gnutella.connection.SentMessageHandler;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.LeafHandshakeResponder;
import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.handshaking.UltrapeerHandshakeResponder;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.DelayedBufferWriter;
import com.limegroup.gnutella.io.NBThrottle;
import com.limegroup.gnutella.io.NIOMultiplexor;
import com.limegroup.gnutella.io.Throttle;
import com.limegroup.gnutella.io.ThrottleWriter;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.HopsFlowVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.messages.vendor.SimppRequestVM;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UpdateRequest;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.updates.UpdateManager;
import com.limegroup.gnutella.util.BandwidthThrottle;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.ThrottledOutputStream;
import com.limegroup.gnutella.version.UpdateHandler;

/**
 * A ManagedConnection object represents our connection to a remote computer running Gnutella software.
 * As LimeWire runs, it has one ManagedConnection object for each computer we're connected to.
 * 
 * The ConnectionManager object keeps the list of ManagedConnection objects.
 * This is the list of computers we're connected to.
 * The ConnectionManager manages the ManagedConnection objects in its list.
 * 
 * ManagedConnection extends Connection.
 * The program never makes a Connection object, just ManagedConnection objects.
 * Connection is never used by itself, only as a part of ManagedConnection.
 * 
 * A thread runs in loopForMessages() forever, or at least until an IOException is thrown.
 * It gets Gnutella packets from this remote computer, and replies to them.
 * 
 * ManagedConnection provides a sophisticated message buffering mechanism.
 * When you call send(m), the message is not actually delivered to the socket.
 * Instead, it buffered in an application-level buffer.
 * Periodically, a thread reads messages from the buffer, writes them to the network, and flushes the socket buffers.
 * This means that there is no need to manually call flush().
 * 
 * ManagedConnection provides a simple form of flow control.
 * If messages are queued faster than they can be written to the network, they are dropped in the following order:
 * PingRequest, PingReply, QueryRequest, QueryReply, and PushRequest.
 * 
 * All ManagedConnection's have two underlying spam filters.
 * A personal filter controls what the user sees.
 * A route filter controls what the program passes on to others.
 * These filters are configured by the properties in the SettingsManager.
 * You can change them with setPersonalFilter and setRouteFilter.
 * 
 * A ManagedConnection object maintains a large number of statistics, such as the current bandwidth for upstream and downstream.
 * ManagedConnection doesn't quite fit the BandwidthTracker interface, unfortunately.
 * On the query-routing3-branch and pong-caching CVS branches, these statistics have been bundled into a single object, reducing the complexity of ManagedConnection.
 * 
 * ManagedConnection also handles various kinds of vendor messages.
 * The handleVendorMessage() works with advanced Gnutella features like hops flow, UDP connect back, and TCP connect back.
 * 
 * This class implements ReplyHandler to route pongs and query replies that originated from it.
 * 
 * Here is a summary of the contents of this class, grouped together by Gnutella feature.
 * 
 * === Use the ConnectionManager Object ===
 * 
 * _manager is a reference to the ConnectionManager object.
 * 
 * === Accepting a Connection ===
 * 
 * Use the ManagedConnection(Socket) constructor to make a new object for a remote computer that has connected to us.
 * 
 * === Connecting to an IP Address and Port Number ===
 * 
 * Use the ManagedConnection(String, int) constructor to make a new object we'll try to connect to an IP address and port number.
 * ManagedConnection(String, int, Properties, HandshakeResponder) actually makes the object.
 * initialize() connects to the remote computer, does the Gnutella handshake, and sets up compression.
 * CONNECT_TIMEOUT is 6 seconds, the amount of time we'll wait for a remote computer to accept our connection.
 * 
 * === Doing the Handshake ===
 * 
 * ConnectionFetcher.managedRun calls setLocalePreferencing(false) to have the HandshakeResponder not refuse foreign language computers.
 * 
 * === Making the Read and Write Chains ===
 * 
 * ManagedConnection inherits _socket from Connection.
 * _socket looks like a java.net.Socket object, but it's actually a LimeWire NIOSocket.
 * A NIOSocket makes four objects to use behind it, like this:
 * 
 *           - NIOInputStream  - BufferInputStream
 * NIOSocket -
 *           - NIOOutputStream - BufferOutputStream
 * 
 * NIOSocket uses this configuration and pretends to block for connecting and the handshake.
 * Downloads and uploads also use NIOSocket in this default configuration.
 * 
 * getOutputStream() puts a ThrottledOutputStream infront of the BufferOutputStream and returns it.
 * It uses _throttle to make the ThrottledOutputStream.
 * 
 * ManagedConnection, however, changes NIOSocket to use a different configuration.
 * Code here causes NIOSocket to throw away the 4 objects shown above.
 * ManagedConnection builds the chains of readers and writers, which look like this:
 * 
 *   MessageReader - InflaterReader - NIOSocket.channel
 * 
 *   MessageWriter - DeflaterWriter - DelayedBufferWriter - ThrottleWriter - SocketInterestWriteAdapter - NIOSocket.channel
 * 
 * loopForMessages() builds the chain of readers, and startOutput() builds the chain of writers.
 * 
 * === The Ultrapeer System ===
 * 
 * supernodeClientAtLooping is true if we're an ultrapeer and the remote computer is a leaf.
 * loopForMessages() sets this right after the handshake and before it builds the chain of readers.
 * 
 * === Bandwidth Throttling ===
 * 
 * TOTAL_OUTGOING_MESSAGING_BANDWIDTH is 8000 bytes/second, the limit on total upstream messaging bandwidth for all connections.
 * Both the _throttle and _nbThrottle objects use this value.
 * getOutputStream() uses _throttle to make the ThrottledOutputStream.
 * startOutput() uses _nbThrottle to make the ThrottleWriter.
 * 
 * === The Gnutella Client ID GUID ===
 * 
 * clientGUID is the remote computer's client ID, its GUID on the Gnutella network.
 * Read this value with getClientGUID().
 * 
 * === Gnutella Packets ===
 * 
 * To send a Gnutella packet to this remote computer, call send(m).
 * reply(m) just calls send(m).
 * _outputRunner is the MessageWriter we'll give the packet to.
 * Use originateQuery(QueryRequest) to send a QueryRequest packet to this remote computer that we made ourselves, or that we're sending for one of our leaves.
 * MessageReader calls processReadMessage(m) to give us a freshly sliced Gnutella packet.
 * It calls handleMessageInternal(m) to check the packet with the spam filter and hand it off to the message dispatcher.
 * The MessageReader calls getNetwork(), and we tell it we're using TCP and not UDP.
 * 
 * === Spam Filtering ===
 * 
 * A MangedConnection objects keeps two SpamFilter objects.
 * These objects look at Gnutella packets from this remote computer, and determine if they are good or if they are spam.
 * _routeFilter tells us if we shouldn't forward a Gnutella packet to other remote computers.
 * _personalFilter tells us if we shouldn't show a Gnutella packet to our user.
 * isSpam(m) uses the route filter, and isPersonalSpam(m) uses the personal filter.
 * Both of these objects are of type SpamFilter.
 * Call setRouteFilter(filter) and setPersonalFilter(filter) to set them.
 * 
 * === Query Routing Tables ===
 * 
 * Synchronize on QRP_LOCK before calling a query route table method.
 * Call getQRPLock() to access this method.
 * 
 * _lastQRPTableReceived is the remote computer's query route table.
 * _lastQRPTableSent is our query route table we sent the remote computer.
 * Access these with getQueryRouteTableReceived() and getQueryRouteTableSent().
 * Save the query route table we sent the remote computer with setQueryRouteTableSent(qrt).
 * When the remote computer sends its query route table, save it in _lastTableReceived with resetQueryRouteTable().
 * The remote computer may just send a query route table patch, patch our copy here with patchQueryRouteTable().
 * 
 * shouldForwardQuery() tests if a given query request packet will pass through or hit this remote computer's query route table.
 * hitsQueryRouteTable() makes the call to perform this test.
 * 
 * There are more methods here that give details about this remote computer's query route table.
 * getQueryRouteTablePercentFull() tells how full the remote computer's query route table is.
 * getQueryRouteTableSize() tells how big the remote computer's query route table is.
 * getQueryRouteTableEmptyUnits() tells how many units in the remote computer's query route table are empty.
 * getQueryRouteTableUnitsInUse() tells how many units in the remote computer's query route table are in use.
 * 
 * LEAF_QUERY_ROUTE_UPDATE_TIME if the remote computer is a leaf, we'll send it our query route table every 5 minutes.
 * ULTRAPEER_QUERY_ROUTE_UPDATE_TIME if the remote computer is an ultrapeer, we'll send it our query route table every minute.
 * incrementNextQRPForwardTime() uses these to set the times we should next forward a query route table to the remote computer.
 * Find out when this is with getNextQRPForwardTime(), which reads _nextQRPForwardTime.
 * 
 * MIN_BUSY_LEAF_TIME is 20 seconds, the minimum time a leaf needs to be in busy mode before we will consider it truly busy for the purpose of QRT updates.
 * isBusyEnoughToTriggerQRTRemoval() uses this busy time.
 * 
 * === Connect Back Requests ===
 * 
 * MAX_UDP_CONNECT_BACK_ATTEMPTS is 15, the maximum number of times we should send UDP connect back requests.
 * MAX_TCP_CONNECT_BACK_ATTEMPTS is 10, the maximum number of times we should send tcp connect back requests.
 * _numUDPConnectBackRequests and _numTCPConnectBackRequests are used for connect back vendor messages.
 * 
 * === Statistics ===
 * 
 * ManagedConnection keeps statistics about bandwidth and packets.
 * 
 * _upBandwidthTracker and _downBandwidthTracker are BandwidthTrackerImpl objects.
 * A thread from SupernodeAssigner calls measureBandwidth() repeatedly to keep these numbers up to date.
 * Call getMeasuredUpstreamBandwidth() and getMeasuredDownstreamBandwidth() to get the current speeds.
 * 
 * _connectionStats is a ConnectionStats object that counts how many gnutella packets we've exchanged with the remote computer, and how many we've dropped.
 * countDroppedMessage() counts another message we received and dropped.
 * getNumMessagesSent() returns the number of Gnutella packets we've sent this remote computer.
 * getNumMessagesReceived() returns the number of gnutella packets this remote computer has sent us.
 * getNumSentMessagesDropped() returns the number of packets we've sacrificed before being able to send them to keep bandwidth low.
 * getNumReceivedMessagesDropped() returns the number of packets this remote computer sent us that we then dropped or filtered out.
 * If the remote computer sent us 100 packets and then we dropped 20, getPercentReceivedDropped() returns 20%.
 * If we prepared 100 packets for this remote computer and then sacrificed 20, getPercentSentDropped() returns 20%.
 * 
 * processSentMessage(m) measures a packet to update outgoing bandwidth statistics.
 * 
 * === Hops Flow ===
 * 
 * ManagedConnection has code that performs the hops flow feature. (do)
 * hopsFlowMax is the hops flow maximum value from a HopsFlowVendorMessage this remote computer sent us.
 * Get this value with getHopsFlowMax().
 * _busyTime and setBusy() are used for hops flow.
 * isBusyLeaf() returns true if we're an ultrapeer and this remote computer is a leaf, and it told us its hops flow maximum right now is 0.
 * 
 * === Ping Reply, Push Request, and Push Proxy ===
 * 
 * _pushProxy is used for a push proxy vendor message.
 * handlePingReply() sends the given PingReply packet to this remote computer.
 * handlePushRequest() is used for QueryReply and PushRequest.
 * isPushProxy() is true if the remote computer sent a PushProxyAcknowledgement vendor message.
 * 
 * === Out of Band UDP Query Hits ===
 * 
 * A recent advanced Gnutella feature is out-of-band query hits. (do)
 * Instead of sending a query hit back through the Gnutella network, we just sent it directly with a UDP packet.
 * 
 * _guidMap maps GUIDs for this feature.
 * TIMED_GUID_LIFETIME is 10 minutes.
 * tryToProxy() and morphToStopQuery() perform the mapping.
 * handleQueryReply() has code related to this feature.
 * The nested class GuidMapExpirer was created for it.
 * 
 * === Vendor Messges ===
 * 
 * Like Connection, ManagedConnection has code related to vendor-specific Gnutella packets.
 * handleVendorMessage() sorts a vendor specific packet.
 * 
 * === Closing the Connection ===
 * 
 * Call close() to disconnect from this remote computer.
 * The messagingClosed() method lets MessageReader have ConnectionManager remove us from the list of connected computers.
 * 
 * The ConnectionWatchdog's DudChecker is allowed to kill this connection.
 * _isKillable, accessed by isKillable(), is true.
 * 
 * === Not Used With NIO ===
 * 
 * LimeWire now uses Java's new I/O, NIO.
 * NIO is non-blocking, but LimeWire's NIOSocket class uses Object.wait() and Object.notify() to create blocking behavior.
 * LimeWire classes that connect, accept connections, do the handshake, upload, and download expect this blocking behavior.
 * Gnutella connections with Gnutella packets use non-blocking NIO without simulated blocking.
 * So, LimeWire uses NIO exclusively, but created blocking behavior for many legacy classes.
 * 
 * Earlier, however, NIO was new enough that it didn't work well on Macintosh and other computers.
 * Then, LimeWire was coded so it could be released two ways: using java.net, and using java.nio.
 * There is still code here from that design.
 * Now that Connection._socket is always a NIOSocket, it is never called.
 * 
 * createDeflatedOutputStream() and createInflatedInputStream() return the stream they are given.
 * receive() and receive(int) are not used at all anymore.
 * flush() does nothing, the chain of writers will take care of moving data out.
 * The nested class BlockingRunner is also not used.
 * 
 * === Interfaces ===
 * 
 * ManagedConnection implements the ReplyHandler, MessageReceiver, and SentMessageHandler interfaces.
 * As a ReplyHandler, LimeWire lists a ManagedConnection object in a RouteTable to send pongs back where a ping came from.
 * UDPReplyHandler and ForMeReplyHandler are the two other classes that implement ReplyHandler and can do this.
 * 
 * ManagedConnection is the only class in LimeWire that implements the MessageReceiver and SentMessageHandler interfaces.
 * MessageReader is the only class that referes to the ManagedConnection it keeps as a MessageReceiver.
 * MessageWriter is the only class that referes to the ManagedConnection it keeps as a SentMessageHandler.
 */
public class ManagedConnection extends Connection implements ReplyHandler, MessageReceiver, SentMessageHandler {

    /** If the remote computer is a leaf, we'll send it our query route table every 5 minutes. (do) */
    private long LEAF_QUERY_ROUTE_UPDATE_TIME = 5 * 60 * 1000;

    /** If the remote computer is an ultrapeer, we'll send it our query route table every minute. (do) */
    private long ULTRAPEER_QUERY_ROUTE_UPDATE_TIME = 60 * 1000;

    /**
     * 6 seconds in milliseconds, the timeout to use when connecting.
     * This is not used for bootstrap servers. (do)
     */
    private static final int CONNECT_TIMEOUT = 6000;

    /**
     * 8000 bytes per second, the limit on the total upstream messaging bandwidth for all connections.
     * This is in bytes per second, not bits per second.
     */
    private static final int TOTAL_OUTGOING_MESSAGING_BANDWIDTH = 8000;

    /** 15, the maximum number of times we should send UDP connect back requests. (do) to this computer? */
    private static final int MAX_UDP_CONNECT_BACK_ATTEMPTS = 15;

    /** 10, the maximum number of times we should send TCP connect back requests. (do) to this computer? */
    private static final int MAX_TCP_CONNECT_BACK_ATTEMPTS = 10;

	/** A reference to the connection manager object. */
    private ConnectionManager _manager;

    /*
     * A ManagedConnection object has two spam filters that filter Gnutella packets from this remote computer.
     * The _personalFilter controls what the user sees, and the _routeFilter controls what the program passes along to other computers.
     * 
     * These filters are configured by the properties in the SettingsManager.
     * You can change them with setPersonalFilter() and setRouteFilter().
     */

    /** A SpamFilter object that will identify which Gnutella packets from this remote computer that we don't want to route to other computers. */
    private volatile SpamFilter _routeFilter = SpamFilter.newRouteFilter();
    /** A SpamFilter object that will identify which Gnutella packets from this remote computer that have information we don't want the user to see. */
    private volatile SpamFilter _personalFilter = SpamFilter.newPersonalFilter();

    /*
     * Implementation Note:
     * This class uses the SACHRIFC algorithm described at http://www.limewire.com/developer/sachrifc.txt.
     * The basic idea is to use one queue for each message type.
     * Messages are removed from the queue in a biased round-robin fashion.
     * This prioritizes some message types while preventing any one message type from dominating traffic.
     * Query replies are further prioritized by "GUID volume", i.e., the number of bytes already routed for that GUID.
     * Other messages are sorted by time and removed in a LIFO [sic] policy.
     * This, coupled with timeouts, reduces latency.
     */

    /**
     * Synchronize on QRP_LOCK before calling a method that deals with this remote computer's query route table.
     * 
     * Code in MessageRouter gets this lock and synchronizes on it.
     * Then, it calls methods like patchQueryRouteTable(), resetQueryRouteTable(), isBusyLeaf(), and getQueryRouteTableReceived().
     */
    private final Object QRP_LOCK = new Object();

    /**
     * The startOutput() method will use this NBThrottle to make a ThrottleWriter for the chain of writers.
     * NBThrottle is a non-blocking throttle for outgoing Gnutella packets.
     */
    private final static Throttle _nbThrottle = new NBThrottle(
    	true,                                          // True, for writing (do)
        TOTAL_OUTGOING_MESSAGING_BANDWIDTH,            // 8000 bytes/second
        ConnectionSettings.NUM_CONNECTIONS.getValue(), // 32, maximum number of requestors
        CompositeQueue.QUEUE_TIME);                    // 5 seconds, maximum latency

    /**
     * A BandwidthThrottle we'll use to make sure we don't do the handshake too quickly.
     * 
     * getOutputStream() returns the object you can write to and it will send the data to the remote computer.
     * The getOutputStream() method creates a new ThrottledOutputStream from the BufferOutputStream behind the NIOSocket.
     * When the handshake is over, all those objects will be thrown away and replaced with the chain of writers.
     */
    private final static BandwidthThrottle _throttle = new BandwidthThrottle(TOTAL_OUTGOING_MESSAGING_BANDWIDTH);

    /**
     * To send a Gnutella packet to the remote computer, call _outputRunner.send(m).
     * This is a MessageWriter object, and it implements the OutputRunner interface.
     * The MessageWriter is the start of the chain of writers that startOutput() builds.
     */
    private OutputRunner _outputRunner;

    /** Counts how many Gnutella packets we've exchanged with this remote computer, and how many we've dropped. */
    private final ConnectionStats _connectionStats = new ConnectionStats();

    /** 20 seconds, the minimum time a leaf needs to be in busy mode before we will consider it truly busy for the purposes of QRT updates. (do) */
    private static long MIN_BUSY_LEAF_TIME = 20 * 1000; // 20 seconds

    /** The next time we should send our (do) query route table to this remote computer. */
    private long _nextQRPForwardTime;

    /*
     * The bandwidth trackers for the upstream and downstream.
     * These are not synchronized and not guaranteed to be completely accurate.
     */

    /** We'll tell this BandwidthTrackerImpl when we send the remote computer data, and then ask it the current speed. */
    private BandwidthTrackerImpl _upBandwidthTracker   = new BandwidthTrackerImpl();
    /** We'll tell this BandwidthTrackerImpl when the remote computer sends us data, and then ask it the current speed. */
    private BandwidthTrackerImpl _downBandwidthTracker = new BandwidthTrackerImpl();

    /**
     * True, the ConnectionWatchdog's DudChecker is allowed to kill this connection.
     */
    private boolean _isKillable = true;

    /**
     * The hops flow max value from the HopsFlowVendorMessage this remote computer sent us. (do)
     * 
     * Use this if a HopsFlowVM instructs us to stop sending queries below
     *  this certain hops value....
     */
    private volatile int hopsFlowMax = -1;

    /**
     * Used for hops flow. (do)
     * 
     * This member contains the time beyond which, if this host is still busy (hops flow==0),
     * that we should consider him as "truly idle" and should then remove his contributions
     * last-hop QRTs.  A value of -1 means that either the leaf isn't busy, or he is busy,
     * and his busy-ness was already noticed by the MessageRouter, so we shouldn't 're-notice'
     * him on the next QRT update iteration.
     */
    private volatile long _busyTime = -1;

    /**
     * Used for the push proxy vendor message. (do)
     * 
     * whether this connection is a push proxy for somebody
     */
    private volatile boolean _pushProxy;

    /**
     * Used for connect-back vendor messages. (do)
     * 
     * The class wide static counter for the number of udp connect back request sent.
     */
    private static int _numUDPConnectBackRequests = 0;

    /**
     * Used for connect-back vendor messages. (do)
     * 
     * The class wide static counter for the number of tcp connect back request sent.
     */
    private static int _numTCPConnectBackRequests = 0;

    /** The remote computer's query route table. */
    private QueryRouteTable _lastQRPTableReceived;

    /** Our query route table we sent the remote computer. */
    private QueryRouteTable _lastQRPTableSent;

    /**
     * Maps the search GUIDs this leaf sent us with its address in them to the GUIDs we made with our IP address in them instead.
     * 
     * ManagedConnection uses _guidMap when this remote computer, one of our leaves, sends us a query that we'll run on its behalf.
     * We replace the leaf's IP address and port number hidden in the query packet's message GUID with our own.
     * _guidMap is our record of the GUID before and after this change.
     * 
     * _guidMap is a Java Hashtable, which maps keys to values.
     * The keys are TimedGUID objects, and the values are GUID objects.
     * A key is a TimedGUID object with the GUID with our IP address, and an expriation time of 10 minutes from when we added it.
     * A value is the GUID with the leaf's IP address, as the leaf sent it to us.
     * 
     * _guidMap isn't static.
     * Each ManagedConnection object has it's own _guidMap.
     * 
     * Here's how the code in ManagedConnection uses _guidMap:
     * close() removes this ManagedConnection's _guidMap from the list of them the GuidMapExpirer keeps.
     * tryToProxy() adds a new entry to _guidMap with the GUID before and after address replacement, and the time the entry should expire 10 minutes from now.
     * morphToStopQuery() changes the message guid of a BEAR 12 1 Query Status Response vendor message the same way, so our QueryHandler object will recognize it.
     * handleQueryReply() looks up the possibly morphed GUID of a query hit packet in _guidMap, and changes it back to the GUID this leaf will understand.
     * Every 10 minutes, the GuidMapExpirer loops through every entry in every ManagedConnection's _guidMap, removing those older than 10 minutes.
     * 
     * _guidMap lets us see that we've morphed a GUID so we can morph it again the same way, or un-morph it back.
     * tryToProxy() and morphToStopQuery() morph the GUID.
     * handleQueryReply() morphs it back.
     */
    private Map _guidMap = null;

    /**
     * 10 minutes in milliseconds.
     * After 10 minutes, we'll forget about a search this leaf asked us to run on its behalf.
     */
    private static long TIMED_GUID_LIFETIME = 10 * 60 * 1000;

    /**
     * True if we are an ultrapeer and the remote computer is a leaf.
     * After the handshake, loopForMessages() sets this before starting to get and process Gnutella packets.
     * 
     * Whether or not this was a supernode <-> client connection when message
     * looping started. (ask) why is that important, can it change?
     */
    private boolean supernodeClientAtLooping = false;

    /**
     * The remote computer's GUID on the Gnutella network.
     * 
     * Each computer on the Gnutella network has a GUID that identifies it.
     * This GUID is also sometimes called the Client ID.
     * Since they are GUIDs, they are all unique and each computer can make a new unique one for itself seprately.
     * 
     * QueryReply packets contain the GUID of the computer that generated them.
     * handleMessageInternal(m) looks for a QueryReply the remote computer generated.
     * When it finds one, it reads the GUID from it and sets clintGUID to it.
     * 
     * The remote computer might change its GUID while we are connected to it. (ask)
     * 
     * The last clientGUID a Hops=0 QueryReply had. (do)
     */
    private byte[] clientGUID = DataUtils.EMPTY_GUID; // Start by pointing at an array of 16 bytes that are all 0

    /**
     * Make a new ManagedConnection object that we'll use to try to connect to a new remote computer.
     * This is for an outgoing connection that we initiated.
     * This constructor takes the IP address and port number of the remote computer.
     * When it runs, we haven't tried connecting to the remote computer yet.
     * ManagedConnection objects should only be made by the connection manager.
	 * 
     * @param host The IP address of the remote computer we're going to try to connect to, in a string like "24.183.177.68"
     * @param port The port number of the remote computer we're going to try to connect to, like 6346
     */
    public ManagedConnection(String host, int port) {

    	// Call the next ManagedConnection constructor with the given IP address and port number, and new blank objects
        this(host, port,

            // Make a new empty UltrapeerHeaders or LeafHeaders object, depending on if we're an ultrapeer or not
            // Cast it as a Properties object and pass it to the other constructor
            (RouterService.isSupernode() ? (Properties)(new UltrapeerHeaders(host)) : (Properties)(new LeafHeaders(host))),

            // Make a new empty UltrapeerHandshakeResponder or LeafHandshakeResponder object, depending on if we're an ultrapeer or not
            // Cast it as a HandshakeResponder object and pass it to the other constructor
            (RouterService.isSupernode() ? (HandshakeResponder)new UltrapeerHandshakeResponder(host) : (HandshakeResponder)new LeafHandshakeResponder(host)));
    }

	/**
     * Make a new ManagedConnection object that we'll use to try to connect to a new remote computer.
     * This is for an outgoing connection that we initiated.
     * This constructor takes the IP address and port number of the remote computer.
     * When it runs, we haven't tried connecting to the remote computer yet.
     * ManagedConnection objects should only be made by the connection manager.
	 * 
     * @param host            The IP address of the remote computer we're going to try to connect to, in a string like "24.183.177.68"
     * @param port            The port number of the remote computer we're going to try to connect to, like 6346
     * @param requestHeaders  A new empty UltrapeerHeaders or LeafHeaders object cast to a Properties, depending on which role we're in.
     *                        This will be saved as REQUEST_HEADERS, our stage 1 headers we'll send with "GNUTELLA CONNECT/0.6".
     * @param responseHeaders A new empty UltrapeerHandshakeResponder or LeafHandshakeResponder object cast to a HandshakeResponder, depending on which role we're in.
     *                        This is the HandshakeResponder object that will read stage 2 and compose stage 3.
	 */
	private ManagedConnection(String host, int port, Properties props, HandshakeResponder responder) {

		// Call the Connection constructor with the same parameters
        super(host, port, props, responder);

        // Save a reference to the connection manager in this ManagedConnection object
        _manager = RouterService.getConnectionManager();
	}

    /**
     * Make a new ManagedConnection object for a new remote computer that just connected to us.
     * This is for an incoming connection the remote computer initiated.
     * ManagedConnection objects should only be made by the connection manager.
     * 
     * @param socket The connection socket that accept() returned.
     *               The Acceptor already read "GNUTELLA " from the socket, so the next thing in it will probably be "CONNECT/0.6".
     */
    ManagedConnection(Socket socket) {

    	// Call the Connection constructor with the connection socket and a new empty HandshakeResponder object we'll make here
    	super(socket,

    		// If we're an ultrapeer
            RouterService.isSupernode() ?

            // Make this new ManagedConnection object with an UltrapeerHandshakeResponder
            (HandshakeResponder)(new UltrapeerHandshakeResponder(socket.getInetAddress().getHostAddress())) :

            // Otherwise, make it with a LeafHandshakeResponder
            (HandshakeResponder)(new LeafHandshakeResponder(socket.getInetAddress().getHostAddress())));

        // Save a reference to the connection manager in this ManagedConnection object
        _manager = RouterService.getConnectionManager();
    }

    /**
     * Connect to the remote computer, do the Gnutella handshake, and setup compression on the connection.
     * 
     * Calls Connection.initialize() to do 3 important jobs:
     * Connect to the remote computer, going from an IP address and port number to a connection socket.
     * Negotiate the Gnutella handshake with the remote computer.
     * Setup compression on the connection.
     * 
     * Then, does 2 more things:
     * Start our OutputRunner. (do)
     * Show the handshake to the UpdateManager, which will look for "X-Version" and an update message for us.
     * 
     * @exception IOException           We were unable to connect to the host.
     * @exception NoGnutellaOkException We or the remote computer started a group of handshake headers with a rejection code instead of "200 OK".
     * @exception BadHandshakeException There was some other problem establishing the connection.
     *                                  For instance, the remote computer might close the connection during the handshake.
     *                                  Or, it might respond with HTTP headers instead of Gnutella headers.
     */
    public void initialize() throws IOException, NoGnutellaOkException, BadHandshakeException {

    	// Call Connection.initialize to connect to the remote computer, do the Gnutella handshake, and setup compression
		super.initialize(CONNECT_TIMEOUT); // Timeouts may not work with NIO underneath

        // Make the chain of objects we'll use to send Gnutella packets to the remote computer
        startOutput();

        /*
         * LimeWire the company can update the LimeWire programs running on the Internet with a feature called SIMPP.
         * It uses the "X-Version" header and cryptographically signed XML messages to announce, download, and verify the updated version.
         */

        // Show the handshake headers this remote computer told us to the UpdateManager object
        UpdateManager updater = UpdateManager.instance(); // Get an instance to the program's one UpdateManager object
        updater.checkAndUpdate(this);                     // See if the remote computer told us the "X-Version" header, meaning it has a signed message for us
    }

    /**
     * The remote computer sent us a query route table, save it in _lastQRPTableReceived.
     * 
     * Called when the remote computer sends us a ResetTableMessage.
     * Resets the query route table for this remote computer.
     * The new table will be the size specified in the ResetTableMessage.
     * It will start out empty.
     * If there isn't a QueryRouteTable object for this remote computer yet, this method will make one.
     * 
     * @param m The ResetTableMessage the remote computer sent us
     */
    public void resetQueryRouteTable(ResetTableMessage m) {

    	// We don't have a query route table from the remote computer yet
        if (_lastQRPTableReceived == null) {

        	// Make a new QueryRouteTable object
            _lastQRPTableReceived = new QueryRouteTable(
                m.getTableSize(), // The size of the query route table
                m.getInfinity()); // The infinity the QRP table uses

        // The remote computer is replacing a previous query route table with a new one
        } else {

        	// Make our QueryRouteTable for this remote computer block everything
            _lastQRPTableReceived.reset(m);
        }
    }

    /**
     * The remote computer sent us a QRP patch, patch _lastQRPTableReceived with it.
     * 
     * Called when the remote computer sends us a PatchTableMessage.
     * Patches the query route table the remote computer sent us.
     * If the remote computer sends us a patch before it sends us a table, this method makes a new table.
     * 
     * @param m The PatchTableMessage the remote computer sent us
     */
    public void patchQueryRouteTable(PatchTableMessage m) {

    	// If the remote computer hasn't sent us a query route table at all yet
        if (_lastQRPTableReceived == null) {

        	/*
        	 * This is unusual.
        	 * The remote computer should have sent a query route table before sending a patch for it.
        	 * But, we can deal with this situation.
        	 * We'll make a new QueryRouteTable object with all the defaults, and then patch it.
        	 */

        	// Make one with all the defaults
            _lastQRPTableReceived = new QueryRouteTable();
        }

        try {

        	// Give the PatchTableMessage to the QueryRouteTable the remote computer sent us
            _lastQRPTableReceived.patch(m);

        } catch (BadPacketException e) {}
    }

    /**
     * Set the busy timer for this remote leaf to now, or clear the timer.
     * handleVendorMessage() calls this when the remote computer sends us a HopsFlowVendorMessage.
     * 
     * @param bSet True to set the busy timer for this remote computer.
     *             False to clear the busy timer for this remote computer.
     */
    public void setBusy(boolean bSet) {

    	// Set the busy timer for this remote computer
        if (bSet) {

        	// If it hasn't been set yet, set it to now
            if (_busyTime == -1) _busyTime = System.currentTimeMillis();

        // Clear the busy timer for this remote computer
        } else {

        	// Clear the busy timer so it never goes off
        	_busyTime = -1;
        }
    }

    /**
     * The current hops flow maximum for this remote computer.
     * It told us its limit in a HopsFlowVendorMessage.
     * If this remote computer hasn't sent us a HopsFlowVendorMessage yet, this method will return -1
     * 
     * @return The current hops flow maximum value for this remote computer.
     *         -1 if it hasn't sent us a HopsFlowVendorMessage yet.
     */
    public byte getHopsFlowMax() {

        // The hops flow max value from the HopsFlowVendorMessage this remote computer sent us (do)
        return (byte)hopsFlowMax;
    }

    /**
     * Returns true if we're an ultrapeer and this remote computer is a leaf, and it told us its hops flow maximum right now is 0.
     * 
     * (do)
     * Returns true iff this connection is a shielded leaf connection, and has 
     * signalled that he is currently busy (full on upload slots).  If so, we will 
     * not include his QRT table in last hop QRT tables we send out (if we are an 
     * Ultrapeer)
     * return true iff this connection is a busy leaf (don't include his QRT table)
     * 
     * @return True if this remote computer is a leaf without any open connection slots (do)
     */
    public boolean isBusyLeaf() {

        // If we are an ultrapeer and the remote computer is a leaf, and it told us its hops flow maximum right now is 0, it's busy
        boolean busy =
        	isSupernodeClientConnection() && // We are an ultrapeer and the remote computer is a leaf, and
        	(getHopsFlowMax() == 0);         // This remote computer sent us a HopsFlowVendorMessage with a maximum value of 0

        // Return true if the leaf is busy
        return busy;
    }

    /**
     * Determine if the remote leaf has been busy long enough to remove its QRT from the combined last-hop QRT. (ask)
     * 
     * Determine whether or not the leaf has been busy long enough to remove his QRT tables
     * from the combined last-hop QRTs, and should trigger an earlier update (ask)
     * 
     * @return true iff this leaf is busy and should trigger an update to the last-hop QRTs 
     */
    public boolean isBusyEnoughToTriggerQRTRemoval() {

    	/*
    	 * A _busyTime of -1 means one of the following things are true:
    	 * The remote leaf isn't busy.
    	 * Or, it is busy, but the MessageRouter already noticed its busy status. 
    	 */

    	// The timer is not set, don't trigger a QRT removal (do)
        if (_busyTime == -1) return false;

        // If more than 20 seconds have passed since the program called setBusy(true)
        if (System.currentTimeMillis() > (_busyTime + MIN_BUSY_LEAF_TIME)) return true; // Trigger QRT removal (do)

        // Not enough time has passed yet to warrant a QRT removal (do)
        return false;
    }

    /**
     * Tests if a given QueryRequest packet will pass through or hit this remote computer's query route table.
     * 
     * @param query A QueryRequest packet from some other computer that is searching for something.
     * @return      True if this remote computer might have a result for this search.
     *              We'll route the QueryRequest packet to this remote computer so it can see it.
     *              False if this remote computer's query route table proves that it won't have a result.
     *              We won't route the QueryRequest packet to this remote computer at all.
     */
    public boolean shouldForwardQuery(QueryRequest query) {

        /*
         * special what is queries have version numbers attached to them - make
         * sure that the remote host can answer the query....
         */

        // This is a feature query (ask)
        if (query.isFeatureQuery()) {

            // We are an ultrapeer and the remote computer is a leaf
            if (isSupernodeClientConnection()) {

                // Return true if the remote computer sent a capabilities vendor message with a later or matching version of the given query request packet
                return (getRemoteHostFeatureQuerySelector() >= query.getFeatureSelector());

            // We and the remote computer are both ultrapeers
            } else if (isSupernodeSupernodeConnection()) {

                // Return true if the remote computer sent us a capabilities vendor message that says it supports feature queries (do)
                return getRemoteHostSupportsFeatureQueries();

            // We must be a leaf and the remote computer must be an ultrapeer
            } else {

                // No, a leaf shouldn't forward a QueryRequest packet at all
                return false;
            }

        // This is just a regular Gnutella query request packet
        } else {

            // Return true if the query passes through the remote computer's query route table, false if it hits it
            return hitsQueryRouteTable(query);
        }
    }

    /**
     * Determines if a given query passes through or hits this remote computer's query route table.
     * Only shouldForwardQuery() above calls this.
     * 
     * @param query A QueryRequest we might send to this remote computer
     * @return      True if the remote computer might have a result for this search.
     *              False if this remote computer's query route table proves that it won't have a result.
     */
    protected boolean hitsQueryRouteTable(QueryRequest query) {

        // If we don't have a query route table from this remote computer, there is nothing to block the query
        if(_lastQRPTableReceived == null) return false;

        // Return true if the remote computer might have a result, false if the query route table proves it won't
        return _lastQRPTableReceived.contains(query);
	}

    /**
     * Get this remote computer's query route table.
     * This is the most recent query route table the remote computer has sent us.
     * We've been keeping it up to date with resetQueryRouteTable() and patchQueryRouteTable().
     * 
     * @return The current query route table from this remote computer.
     *         Returns null if this remote computer hasn't sent us a query route table.
     */
    public QueryRouteTable getQueryRouteTableReceived() {

        // Return the remote computer's query route table
        return _lastQRPTableReceived;
    }

    /**
     * How full the remote computer's query route table is.
     * 
     * @return The percentage of slots used in the BitTable in the remote computer's QueryRouteTable. (do)
     *         -1 if the remote computer hasn't sent its query route table.
     */
    public double getQueryRouteTablePercentFull() {

        // If we have no query route table from this remote computer, return 0
        if (_lastQRPTableReceived == null) return 0;

        // Return the percentage of slots used in the BitTable in the remote computer's QueryRouteTable (do)
        return _lastQRPTableReceived.getPercentFull();
    }

    /**
     * How big the remote computer's query route table is.
     * 
     * @return The size of the remote computer's query route table. (do)
     *         0 if the remote computer hasn't sent its query route table.
     */
    public int getQueryRouteTableSize() {

        // If we have no query route table from this remote computer, return 0
        if (_lastQRPTableReceived == null) return 0;

        // Return the size of the remote computer's query route table (do)
        return _lastQRPTableReceived.getSize();
    }

    /**
     * How many units in the remote computer's query route table are empty.
     * 
     * @return The number of empty elements in the remote computer's query route table. (do)
     *         -1 if the remote computer hasn't sent its query route table.
     */
    public int getQueryRouteTableEmptyUnits() {

        // If we have no query route table from this remote computer, return -1
        if (_lastQRPTableReceived == null) return -1;

        // Return the number of empty elements in the remote computer's query route table (do)
        return _lastQRPTableReceived.getEmptyUnits();
    }

    /**
     * How many units in the remote computer's query route table are in use. (do) or is it how many units there are total
     * 
     * @return The total number of units the remote computer's query route table has allocated for storage. (do)
     *         -1 if the remote computer hasn't sent its query route table.
     */
    public int getQueryRouteTableUnitsInUse() {

        // If we have no query route table from this remote computer, return -1
        if (_lastQRPTableReceived == null) return -1;

        // Return the total number of units the remote computer's query route table has allocated for storage (do)
        return _lastQRPTableReceived.getUnitsInUse();
    }

    /**
     * Does nothing now that LimeWire has switched to NIO.
     * Make the output stream compress data before it sends it.
     * If LimeWire is using NIO, this method returns the stream you give it without changing it or adding anything.
     * 
     * @param out The OutputStream to call write() on to send the remote computer data
     * @return    An OutputStream to write to instead that will compress the data before writing it to the given OutputStream
     */
    protected OutputStream createDeflatedOutputStream(OutputStream out) {

    	// The connection socket, _socket, is actually a LimeWire NIOSocket object
        if (isAsynchronous()) {
        	
        	/*
        	 * We already installed an asynchronous writer that doesn't use streams.
        	 * We don't need to do anything here.
        	 */

        	// Return the given output stream without putting another object between us and it
        	return out;

        // Not used now that LimeWire has switched to NIO
        } else {

        	// Make a new CompressingOutputStream that uses _deflater and then writes to out
        	return super.createDeflatedOutputStream(out); // Return it to write to it instead
        }
    }

    /**
     * Does nothing now that LimeWire has switched to NIO.
     * Make the input stream decompress the data it reads.
     * If LimeWire is using NIO, this method returns the stream you give it without changing it or adding anything.
     * 
     * @param in The InputStream to call read() on to get data from the remote computer
     * @return   An InputStream to read from instead that will read from in and then decompress the data
     */
    protected InputStream createInflatedInputStream(InputStream in) {

    	// The connection socket, _socket, is actually a LimeWire NIOSocket object
    	if (isAsynchronous()) {

    		/*
    		 * We're going to install a reader when we start looping for messages.
    		 * So, we don't need to do anything here.
    		 * 
    		 * Note, however, that if we use the Connection.receive() method instead of loopForMessages,
    		 * it will setup an UncompressingInputStream automatically.
    		 */

    		// Return the given input stream without putting another object between us and it
    		return in;

        // Not used now that LimeWire has switched to NIO
    	} else {

        	// Make a new UncompressingInputStream that reads from in and then uses _inflater
    		return super.createInflatedInputStream(in); // Return it to read from it instead
        }
    }

    /**
     * Get the object you can use to write data to the remote computer.
     * 
     * This method overrides Connection.getOutputStream().
     * When Connection.initialize() calls getOutputStream(), the call goes here.
     * From here, the call super.getOutputStream() goes back down to Connection.getOutputStream().
     * 
     * Connection.initialize() calls this right after we establish the connection.
     * We haven't done the handshake yet.
     * 
     * When this is called, the objects behind this ManagedConnection look like this:
     * 
     *   NIOSocket - NIOOutputStream - BufferOutputStream
     * 
     * Our NIOSocket still has NIOOutputStream and BufferOutputStream objects.
     * It hasn't traded them in and switched to the chain of writers to send Gnutella packets yet.
     * 
     * This method, getOutputStream(), finds the BufferOutputStream behind NIOSocket.
     * Then, it places a ThrottledOutputStream before it, and returns it.
     * 
     *   ThrottledOutputStream - BufferOutputStream
     *         
     * Throttles the super's OutputStream.  This works quite well with
     * compressed streams, because the chaining mechanism writes the
     * compressed bytes, ensuring that we do not attempt to request
     * more data (and thus sleep while throttling) than we will actually write.
     * 
     * @return A new ThrottledOutputStream placed before the BufferOutputStream behind the NIOSocket object
     */
    protected OutputStream getOutputStream() throws IOException {

        /*
         * super.getOutputStream() calls Connection.getOutputStream().
         * _socket is an NIOSocket.
         * super.getOutputStream() resolves to the BufferOutputStream object the NIOSocket made to help it send data.
         * 
         * _throttle is a BandwidthThrottle object.
         * The line of code here makes a new ThrottledOutputStream from the BufferOutputStream and BandwidthThrottle objects.
         */

        // Make a new ThrottledOutputStream from the BufferOutputStream and BandwidthThrottle objects this ManagedConnection has
        return new ThrottledOutputStream(super.getOutputStream(), _throttle);
    }

    /**
     * Not used now that LimeWire has switched to NIO.
     * 
     * Override of receive to do ConnectionManager stats and to properly shut
     * down the connection on IOException
     */
    public Message receive() throws IOException, BadPacketException {
        Message m = null;
        try {
            m = super.receive();
        } catch (IOException e) {
            if (_manager != null) _manager.remove(this);
            throw e;
        }
        // record received message in stats
        _connectionStats.addReceived();
        return m;
    }

    /**
     * Not used now that LimeWire has switched to NIO.
     * 
     * Override of receive to do MessageRouter stats and to properly shut
     * down the connection on IOException
     */
    public Message receive(int timeout) throws IOException, BadPacketException, InterruptedIOException {
        Message m = null;
        try {
            m = super.receive(timeout);
        } catch (InterruptedIOException ioe) {
            //we read nothing in this timeframe,
            //do not remove, just rethrow.
            throw ioe;
        } catch (IOException e) {
            if (_manager != null) _manager.remove(this);
            throw e;
        }
        // record received message in stats
        _connectionStats.addReceived();
        return m;
    }

    /*
     * ////////////////////// Sending, Outgoing Flow Control //////////////////////
     */

    /**
     * Makes the chain of objects the program will use to write data to the remote computer.
     * The 4 objects in the base of the chain of writers look like this:
     * 
     *   MessageWriter -> DeflaterWriter -> DelayedBufferWriter -> ThrottleWriter
     * 
     * Points _outputRunner at MessageWriter.
     * Later, we'll call _outputRunner.send(m) to give MessageWriter a packet and put it into the chain.
     * 
     * MessageWriter holds Gnutella packets, and drops the unimportant ones according to the SACHRIFC algorithm.
     * DeflaterWriter compresses data.
     * DelayedBufferWriter groups data into discreet blobs, which will move across the Internet more efficiently than a constant trickle.
     * ThrottleWriter makes sure we're not sending the remote computer too much data too quickly.
     * 
     * At the end, calls NIOSocket.setWriteObserver(MessageWriter).
     * This adds a SocketInterestWriteAdapter above the chain.
     * When NIODispatcher calls NIOSocket.handleWrite(), it will call SocketInterestWriteAdapter.handleWrite() instead.
     * 
     * Data will go from the MessageWriter, DeflaterWriter, DelayedBufferWriter, ThrottleWriter, SocketInterestWriteAdapter, channel.
     * The SocketInterestWriteAdapter contains the line of code that actually writes data to the Java channel object.
     */
    private void startOutput() {

        /*
        // Taking this change out until we can safely handle attacks and overflow
        // TODO: make a cheaper Queue that still prevents flooding of ultrapeer
        //       and ensures that clogged leaf doesn't drop QRP messages.
		//if(isSupernodeSupernodeConnection())
		    queue = new CompositeQueue();
		//else
		    //queue = new BasicQueue();
        */

        // Make a queue that holds Gnutella packets
        MessageQueue queue = new CompositeQueue(); // A CompositeQueue keeps queues of each type of message to do SACHRIFC flow control

        // True because LimeWire has switched to NIO
		if (isAsynchronous()) {

            // Make the MessageWriter object that will start the chain of writers
		    MessageWriter messager = new MessageWriter(_connectionStats, queue, this);
		    _outputRunner = messager; // The MessageWriter is our _outputRunner, the object we'll give packets to
		    ChannelWriter writer = messager; // We'll point writer at the last object in the chain as we make it in this method

            // We told the remote computer "Content-Encoding: deflate" and will be sending compressed data
		    if (isWriteDeflated()) {

                // Make a DeflaterWriter object, and have the MessageWriter write to it
		        DeflaterWriter deflater = new DeflaterWriter(_deflater);
		        messager.setWriteChannel(deflater);
                writer = deflater; // Now the last object in the chain is the DeflaterWriter
            }

            // Make a DelayedBufferWriter object, and have the DeflaterWriter write to it
            DelayedBufferWriter delayer = new DelayedBufferWriter(1400);
            writer.setWriteChannel(delayer);
            writer = delayer; // Now the last object in the chain is the DelayedBufferWriter
            
            // Make a ThrottleWriter object, and have the DelayedBufferWriter write to it
            writer.setWriteChannel(new ThrottleWriter(_nbThrottle));

            // When NIODispatcher calls NIOSocket.handleWrite(), have it call MessageWriter.handleWrite()
		    ((NIOMultiplexor)_socket).setWriteObserver(messager); // This calls NIOSocket.setWriteObserver(MessageWriter)

        // Not used now that LimeWire has switched to NIO
		} else {
		    _outputRunner = new BlockingRunner(queue);
        }
    }

    /**
     * To send this remote computer a Gnutella packet, call send(m).
     * 
     * Removes the GGEP extension block if the remote computer won't be able to understand it.
     * Drops the packet instead of sending it if a vendor message gave us an unacceptable hops count for QueryRequest packets.
     * Calls MessageWriter.send(m) to release the packet into the write chain.
     * 
     * @param m A Gnutella packet you want to send this remote computer
     */
    public void send(Message m) {

        // If the remote computer didn't say "GGEP:", it can't understand GGEP extension blocks in Gnutella packets
        if (!supportsGGEP()) m = m.stripExtendedPayload(); // Remove the GGEP extension block from this packet

        /*
         * if Hops Flow is in effect, and this is a QueryRequest, and the
         * hoppage is too biggage, discardage time...
         */

        // The remote computer may have sent us a vendor message that told us to stop sending queries that had hopped a certain number of times or more
        int smh = hopsFlowMax;
        if (smh > -1 &&                    // We got the vendor message
            (m instanceof QueryRequest) && // This is a QueryRequest packet
            m.getHops() >= smh)            // It's hopped the limit or more
            return;                        // Leave this method without sending the packet

        // Call MessageWriter.send(m) to release the Gnutella packet into the chain of writers
        _outputRunner.send(m); // From here it will move through the chain and out to the remote computer
    }

    /**
     * Send a QueryRequest packet to this remote computer that we made ourselves, or that we're sending on behalf of one of our leaves.
     * 
     * (do)
     * This is a specialized send method for queries that we originate,
     * either from ourselves directly, or on behalf of one of our leaves
     * when we're an Ultrapeer.  These queries have a special sending
     * queue of their own and are treated with a higher priority.
     * 
     * @param query The QueryRequest packet to send to this remote computer
     */
    public void originateQuery(QueryRequest query) {

        // Make the QueryRequest packet look like we created it, even though we actually got it from the network (do)
        query.originate(); // Marks our object that represents the query packet with originated = true

        // Send the modified QueryRequest packet to this remote computer
        send(query);
    }

    /**
     * Does nothing.
     * The chain of writers takes care of moving data out to the remote computer.
     */
    public void flush() throws IOException {

        /*
         * Note that flush() does NOT block for TCP buffers to be emptied.
         */
    }

    /**
     * Close our connection to this remote computer, and release all the objects we've been using to communicate with it.
     */
    public void close() {

        // Call MessageWriter.shutdown(), which will shut down all the objects in the chain of writers
        if (_outputRunner != null) _outputRunner.shutdown();

        // Call Connection.close(), which will close the NIOSocket object and put away the Java Deflater and Inflater
        super.close();

        // Remove this ManagedConnection's _guidMap from the list of them the GuidMapExpirer keeps
        if (_guidMap != null) GuidMapExpirer.removeMap(_guidMap);
    }

    /*
     * //////////////////////////////////////////////////////////////////////////
     */

    /**
     * Makes the chain of objects the program will use to read data from the remote computer.
     * The chain of readers looks like this:
     * 
     *   (us) -> MessageReader -> InflaterReader -> NIOSocket.channel -> (remote computer)
     * 
     * Here's how it works:
     * NIODispatcher calls NIOSocket, which forwards the call to MessageReader.
     * MessageReader gets data from InflaterReader, which gets it from channel in NIOSocket.
     * MessageReader slices the data into Gnutella packets, and hands each one back to us by calling processMessage(m).
     * 
     * ConnectionManager.startConnection() calls this.
     * 
     * This returns immediately.
     * A separate thread will call processMessage(m) to give us Gnutella packets from this remote computer.
     */
    void loopForMessages() throws IOException {

        // True if we are an ultrapeer and the remote computer is a leaf
        supernodeClientAtLooping = isSupernodeClientConnection();

        // Not used now that LimeWire has switched to NIO
        if (!isAsynchronous()) {

            while (true) {
                Message m = null;
                try {
                    m = receive();
                    if (m == null) continue;
                    handleMessageInternal(m);
                } catch (BadPacketException ignored) {}
            }

        // _socket is actually an NIOSocket object that we can use without blocking
        } else {

            /*
             * Make a MessageReader object for this connection.
             * This is the object that will slice already decompressed data into Gnutella packets.
             * 
             * Give it ManagedConnection.this, a reference to this class.
             * ManagedConnection implements the MessageReceiver interface, which MessageReader takes.
             * MessageReader.handleRead() will call the getSoftMax(), getNetwork(), and processReadMessage(m) methods here.
             */

            // Make a MessageReader object that will slice data from the remote computer into Gnutella packets
            MessageReader reader = new MessageReader(ManagedConnection.this);

            // If the remote computer said "Content-Encoding: deflate"
            if (isReadDeflated()) {

                /*
                 * Make a new InflaterReader, and give it the _inflater object to use to actually decompress data.
                 * We still need to tell it what channel to read from, it's not ready for anyone to call read() on it yet.
                 * 
                 * Don't save a reference to it here, but make it the read channel of the MessageReader.
                 * We'll read from the MessageReader, and the MessageReader will read from the InflaterReader.
                 */

                // Make a new InflaterReader, and have the MessageReader packet slicer read from it
                reader.setReadChannel(new InflaterReader(_inflater));

                /*
                 * We're not ready to read yet.
                 * We told the InflaterReader the _inflater object to use, but didn't tell it where to read from.
                 * The next line of code, setReadObserver(reader), will do this.
                 */
            }

            /*
             * The read chain looks like this:
             * 
             *   (us) -> MessageReader -> InflaterReader -> NIOSocket -> (remote computer)
             * 
             * NIOSocket contains channel, the SelectableChannel connected to the remote computer.
             * NIODispatcher will tell NIOSocket when there is data to read.
             * It will do this by calling NIOSocket.handleRead().
             * 
             * NIOSocket.handleRead() just calls reader.handleRead(), forwarding the call to the reader NIOSocket has saved.
             * At the start, this reader is the NIOInputStream that NIOSocket made for itself.
             * 
             * Now, we've built the chain of readers.
             * We want NIO's command to read to hit the first object in the chain, MessageReader.
             * MessageReader will be told to read, it will read from InflaterReader, and InflaterReader will read from NIOSocket.
             * 
             * This is how a push becomes a pull.
             * NIO's notification that the channel can read doesn't push data onto NIOSocket.
             * Rather, it causes MessageReader to read, pulling data in through InflaterReader from NIOSocket.
             */

            // Make it so that when NIO tells _socket to read, it's MessageReader.handleRead() that gets called
            ((NIOMultiplexor)_socket).setReadObserver(reader); // Also have InflaterReader read from NIOSocket
            
            /*
             * _socket is an NIOSocket object.
             * NIOMultiplexor is an interface that requires a setReadObserver(reader) method.
             * The line of code above calls NIOSocket.setReadObserver(reader).
             * 
             * setReadObserver(reader) also tells the InflaterReader to read from the NIOSocket object.
             */
        }
    }

    /**
     * MessageReader.shutdown() calls messagingClosed() to tell us there will be no more packets.
     * This method has the ConnectionManager object remove us from its list of connected remote computers.
     * 
     * The MessageReceiver interface requires this method.
     */
    public void messagingClosed() {

        // Have the ConnectionManager remove this ManagedConnection object from its list, closing the connection and deleting this object
        if (_manager != null) _manager.remove(this);   
    }

    /**
     * MessageReader.handleRead() calls processReadMessage(m) to give us a freshly sliced Gnutella packet.
     * The MessageReceiver interface requires this method.
     * 
     * @param m A Gnutella packet the MessageReader sliced and is giving to us to process
     */
    public void processReadMessage(Message m) throws IOException {

        // Update statistics to reflect that we've received this packet
        updateReadStatistics(m);        // Update _compressedBytesReceived and _bytesReceived, measures the packet if we're not using compression
        _connectionStats.addReceived(); // Record that this remote computer has sent us one more Gnutella packet in the connection statistics object

        // Run it through the spam filter, and give it to the message dispatcher
        handleMessageInternal(m);
    }

    /**
     * Updates _bytesSent and _compressedBytesSent, two member variables inherited from Connection.
     * Only MessageWriter.handleWrite() calls this.
     * 
     * @param m The Gnutella packet MessageWriter just sent to this remote computer
     */
    public void processSentMessage(Message m) {

        // Updates _bytesSent and _compressedBytesSent
        updateWriteStatistics(m); // Measures the packet if we're not using compression
    }

    /**
     * Checks the packet with the SpamFilter, morphs its GUID for proxying, and hands it off to the MessageDispatcher.
     * 
     * This only gets called from processReadMessage() above.
     * processReadMessage() already updated statistics, so we don't do that here.
     * Runs the message through the SpamFilter for routing.
     * Morphs its GUID to try to proxy it. (do)
     * Then, hands it off to the MessageDispatcher.
     * 
     * @param m A Gnutella packet the MessageReader sliced and gave to processReadMessage(), which passed it here
     */
    private void handleMessageInternal(Message m) {

        // Run the message through the spam filter for routing
        if (isSpam(m)) {

            // The message is spam, record statistics about it and drop it
			ReceivedMessageStatHandler.TCP_FILTERED_MESSAGES.addMessage(m);
            _connectionStats.addReceivedDropped();

            /*
             * To drop a Gnutella packet, we just don't do anything else with it
             */

        // The Gnutella packet made it through the spam filter, we'll route it to other computers we're connected to
        } else {

            // This is a QueryReply packet the remote computer made
            if (m instanceof QueryReply && m.getHops() == 0) {

                /*
                 * A packet's hops count tells how many times the packet has travelled across the Internet.
                 * A hops count of 0 means that the remote computer made this packet and sent it to us.
                 * 
                 * Each computer running Gnutella software makes a GUID for itself on the Gnutella network.
                 * QueryReply packets contain the GUID of the computer that made them.
                 * 
                 * So, we can look inside this QueryPacket and find out the remote computer's Gnutella GUID.
                 */

                // Read the remote computer's GUID from the QueryReply packet, and save it in the clientGUID member variable
                clientGUID = ((QueryReply)m).getClientGUID();
            }

            // If we are an ultrapeer and the remote computer is a leaf
            if (supernodeClientAtLooping) {

                /*
                 * special handling for proxying. (ask)
                 */

                // The leaf sent us a QueryRequest or QueryStatusResponse packet, morph its guid to try to proxy it (do)
                if      (m instanceof QueryRequest)        m = tryToProxy((QueryRequest)m);              // Our leaf sent us a query packet
                else if (m instanceof QueryStatusResponse) m = morphToStopQuery((QueryStatusResponse)m); // Our leaf sent us a BEAR 12 1 Query Status Response vendor message
            }

            // Have the "MessageDispatch" thread call MessageRouter.handleMessage(m, this)
            MessageDispatcher.instance().dispatchTCP(m, this); // We got it over TCP, and it's from this ManagedConnection object
        }
    }

    /**
     * Returns the Internet protocol ManagedConnection uses in its role as as a MessageReceiver, 1 Message.N_TCP for TCP.
     * The two networks are TCP and UDP, represented by 1 Message.N_TCP and 2 Message.N_UDP.
     * 
     * MessageReader.handleRead() calls ManagedConnection.getNetwork().
     * ManagedConnection.getNetwork() returns 1 Message.N_TCP, telling MessageReader the packet came in from TCP and not UDP.
     * MessageReader.handleRead() puts this number in the new Message object it's making to hold the Gnutella packet.
     * 
     * The MessageReceiver interface requires this method.
     */
    public int getNetwork() {

        // Have MessageReader make the new Message with 1 Message.N_TCP because ManagedConnection uses TCP
        return Message.N_TCP;
    }

    /**
     * Takes a query packet this leaf sent us, replaces the leaf's IP address in the GUID with our own, and returns it.
     * 
     * We are an ultrapeer and this remote computer is a leaf.
     * Our leaf sent us a QueryRequest packet.
     * It wants us to perform this search for it.
     * 
     * Here's what's happened up to this point:
     * MessageReader.handleRead() sliced some data from the remote computer, and parsed it into a new QueryRequest packet.
     * ManagedConnection.processReadMessage() updated statistics and called the next method.
     * ManagedConnection.handleMessageInternal() determined that we're an ultrapeer and this remote computer is a leaf, and the packet is a QueryRequest packet.
     * 
     * tryToProxy() only does something if we can get UDP packets.
     * If we can't, it returns the packet unchanged.
     * 
     * When the leaf made the query packet, it hid its IP address and port number in the message GUID.
     * It's using this message GUID to identify its search.
     * tryToProxy() replaces the leaf's address in the GUID with our own.
     * This will make query hit packets come back to us, not the leaf.
     * 
     * tryToProxy() adds an entry to this ManagedConnection's _guidMap with 3 pieces of information:
     * The GUID the leaf gave this search, with the leaf's IP address and port number in it.
     * The GUID we gave this search, with our IP address and port number in it instead.
     * The time 10 minutes from now when we'll remove the entry.
     * 
     * Here's what will happen next:
     * The "MessageDispatch" thread calls MessageRouter.handleMessage(m), which leads to handleQueryRequestPossibleDuplicate().
     * handleQueryRequest() starts dynamic querying, searches our shared files and respons with query hit packets, and forwards the query to our ultrapeers.
     * 
     * @param query The query packet the leaf sent us.
     * @return      The query packet with our IP address and port number hidden in the GUID instead of the leaf's.
     *              If we can't run this search for the leaf, returns the query packet unchanged.
     */
    private QueryRequest tryToProxy(QueryRequest query) {

        /*
         * we must have the following qualifications:
         * 1) Leaf must be sending SuperNode a query (checked in loopForMessages)
         * 2) Leaf must support Leaf Guidance
         * 3) Query must not be OOB.
         * 3.5) The query originator should not disallow proxying.
         * 4) We must be able to OOB and have great success rate.
         */

        // Make sure the leaf understands dynamic querying, looks in its Messages Supported vendor message for BEAR 11
        if (remoteHostSupportsLeafGuidance() < 1) return query; // If it doesn't, return the query packet without changing it

        // Make sure the leaf wants query hits sent directly to it in UDP, looks for 0x04 in the speed flags bytes
        if (query.desiresOutOfBandReplies()) return query; // If it doesn't, return the query packet without changing it

        // Make sure the leaf wants us to search for it, make sure the query packet doesn't have the GGEP "NP" No Proxy extension
        if (query.doNotProxy()) return query; // If it does, return the query packet without changing it

        // Make sure we can get UDP packets
        if (!RouterService.isOOBCapable()                 ||   // Make sure we can receive UDP packets
            !OutOfBandThroughputStat.isSuccessRateGreat() ||   // Make sure we're getting most of the UDP packets we expect to get
            !OutOfBandThroughputStat.isOOBEffectiveForProxy()) // Make sure we've gotten results when we've tried this before
            return query;                                      // If not, return the query packet without changing it

        /*
         * everything is a go - we need to do the following:
         * 1) mutate the GUID of the query - you should maintain every param of
         * the query except the new GUID and the OOB minspeed flag
         * 2) set up mappings between the old guid and the new guid.
         * after that, everything is set.  all you need to do is map the guids
         * of the replies back to the original guid.  also, see if a you get a
         * QueryStatusResponse message and morph it...
         * THIS IS SOME MAJOR HOKERY-POKERY!!!
         */

        /*
         * 1) mutate the GUID of the query
         */

        // Get the query packet's message GUID, origGUID, and make a copy we'll change, oobGUID
        byte[] origGUID = query.getGUID();                          // Get the query packet's message GUID, this is the GUID the leaf chose for the search
        byte[] oobGUID = new byte[origGUID.length];                 // Make a 16-byte array for the morphed GUID we'll make
        System.arraycopy(origGUID, 0, oobGUID, 0, origGUID.length); // Copy origGUID into oobGUID

        // Replace the leaf's IP address and port number in the GUID with our own
        GUID.addressEncodeGuid(
            oobGUID,                    // The GUID to edit
            RouterService.getAddress(), // Write our IP address at the start
            RouterService.getPort());   // Write our port number at byte 13

        // Replace our reference to the query packet with one to a copy that has our IP address and port number in the GUID instead of the leaf's
        query = QueryRequest.createProxyQuery(query, oobGUID); // In the speed flags bytes, set 0x04 to say we can receive UDP packets and want hits that way

        /*
         * Before, the query packet had:
         * The leaf's IP address and port number hidden in the GUID.
         * 0x04 set to request hits to it directly in UDP packets.
         * 
         * Now, the query packet has:
         * Our IP address and port number hidden in the GUID.
         * 0x04 set to request hits to us directly in UDP packets.
         * 
         * When we send copies of this new query packet, we'll get the hits, not the leaf. (ask)
         */

        /*
         * 2) set up mappings between the guids
         */

        // We haven't done any searches for this leaf yet
        if (_guidMap == null) {

            // Make _guidMap, a list of 
            _guidMap = new Hashtable();
            GuidMapExpirer.addMapToExpire(_guidMap);
        }

        // Make a TimedGUID object with the GUID with our IP address that will expire in 10 minutes
        GUID.TimedGUID tGuid = new GUID.TimedGUID(new GUID(oobGUID), TIMED_GUID_LIFETIME);

        // Add the GUID with our IP address, the GUID with the leaf's IP address, and the time 10 minutes from now to this leaf's _guidMap
        _guidMap.put(
            tGuid,               // The key, the TimedGUID with the GUID with our IP address and an expiration time 10 minutes from now
            new GUID(origGUID)); // The value, a copy of the GUID with the leaf's IP address

        // Record a statistic
        OutOfBandThroughputStat.OOB_QUERIES_SENT.incrementStat();

        // Return the query packet with our IP address and port number in the message GUID, overwriting the leaf's address
        return query;
    }

    /**
     * Takes a BEAR 12 1 Query Status Response vendor message this leaf sent us, replaces the leaf's IP address in the GUID with our own, and returns it.
     * Only does this if we previously got a query packet from this leaf, and replaced the address in the GUID to search on the leaf's behalf.
     * 
     * Both tryToProxy() and morphToStopQuery() replace the leaf's IP address in the GUID with our own.
     * They are not morph and un-morph, they are both morph the same way.
     * morphToStopQuery() looks up how tryToProxy() morphed the GUID in _guidMap.
     * If there isn't a listing, we didn't proxy the search, and this method returns the packet unchanged.
     * 
     * We are an ultrapeer and this remote computer is a leaf.
     * Our leaf sent us a BEAR 12 1 Query Status Response vendor message.
     * It's telling us how many hits its gotten for the search we're performing for it.
     * 
     * Here's what's happened up to this point:
     * MessageReader.handleRead() sliced some data from the remote computer, and parsed it into a new BEAR 12 1 Query Status Response vendor message.
     * ManagedConnection.processReadMessage() updated statistics and called the next method.
     * ManagedConnection.handleMessageInternal() determined that we're an ultrapeer and this remote computer is a leaf, and the packet is a BEAR 12 1 vendor message.
     * 
     * morphToStopQuery() takes the Query Status Response vendor message, morphs its GUID the same way tryToProxy() did, and returns it.
     * 
     * Here's what will happen next:
     * The "MessageDispatch" thread calls MessageRouter.handleMessage(m), which leads to handleQueryStatus().
     * handleQueryStatus() has the QueryDispatcher find the QueryHandler that represents the search, and saves the updated number in it.
     * We replaced the GUID the leaf assigned this search to the GUID we assigned it.
     * With our GUID in it, the QueryHandler object will recognize it.
     * 
     * @param resp The BEAR 12 1 Query Status Response vendor message the leaf sent us.
     * @return     The vendor message with our IP address and port number hidden in the GUID instead of the leaf's.
     *             If tryToProxy() didn't perform this same replacement to run this search on behalf of the leaf, rturns the vendor message unchanged.
     */
    private QueryStatusResponse morphToStopQuery(QueryStatusResponse resp) {

        /*
         * if the _guidMap is null, we aren't proxying anything....
         */

        // If we haven't run any searches for this leaf, return the Query Status Response vendor message unchanged
        if (_guidMap == null) return resp;

        /*
         * if we are proxying this query, we should modify the GUID so as
         * to shut off the correct query
         */

        // Get the GUID from the message the leaf sent us
        final GUID origGUID = resp.getQueryGUID(); // origGUID has the leaf's IP address and port number in it

        // Make a GUID object for the corresponding GUID we'll look up in _guidMap
        GUID oobGUID = null; // oobGUID will be the GUID with our IP address and port number in it instead

        // Only let one thread access this leaf's _guidMap at a time
        synchronized (_guidMap) {

            // Loop for each key and value pair in _guidMap
            Iterator entrySetIter = _guidMap.entrySet().iterator();
            while (entrySetIter.hasNext()) {

                // Get this key and value pair as a Map.Entry object
                Map.Entry entry = (Map.Entry)entrySetIter.next();

                // If this entry has the leaf's GUID with the leaf's IP address and port number in it
                if (origGUID.equals(entry.getValue())) { // getValue() returns the GUID with the leaf's IP address, as the leaf sent it to us and before we changed it

                    // Get the GUID we made by replacing the leaf's IP address with our own, oobGUID
                    oobGUID = ((GUID.TimedGUID)entry.getKey()).getGUID();
                    break;
                }
            }
        }

        /*
         * if we had a match, then just construct a new one....
         */

        // We found the GUID the leaf chose for this search in _guidMap
        if (oobGUID != null) {

            // Replace the leaf's GUID for this search with our own, morphing it the same way tryToProxy() did so our QueryDispatcher will recognize it
            return new QueryStatusResponse(oobGUID, resp.getNumResults());

        // Not found
        } else {

            // Return the vendor message unchanged
            return resp;
        }
    }

    /**
     * Returns !_routeFilter.allow(m) to have the SpamFilter for routing tell us if we should drop the packet.
     * Only handleMessageInternal() calls this.
     * 
     * @param m The Gnutella packet to check
     * @return  True if we should drop the packet, false if we should route it, sending it to other computers we're connected to
     */
    public boolean isSpam(Message m) {

        // Have the SpamFilter for routing look at the Gnutella packet and tell us to drop it or route it
        return !_routeFilter.allow(m);
    }

    /*
     * Begin Message dropping and filtering calls
     */

    /**
     * Have the ConnectionStats object count another message we received from this remote computer, and then dropped.
     * MessageRouter calls this when it drops a message.
     * The ReplyHandler interface requires this method.
     * 
     * A callback for the ConnectionManager to inform this connection that a message was dropped.
     * This happens when a reply received from this connection has no routing path.
     */
    public void countDroppedMessage() {

        // Count another message we received, didn't like, and dropped
        _connectionStats.addReceivedDropped();
    }

    /**
     * Determine if the given Gnutella packet passes through our personal SpamFilter, letting us show its information to the user.
     * The ReplyHandler interface requires this method.
     * 
     * @param m A gnutella packet we've received
     * @return  True to hide it from the user, false if it's fine
     */
    public boolean isPersonalSpam(Message m) {

        // See if the Gnutella packet passes through our SpamFilter for messages to show the user
        return !_personalFilter.allow(m);
    }

    /**
     * Set the spam filter this object will use to drop Gnutella packets from (do) or to? this remote computer instead of routing them.
     * RouterService.adjustSpamFilters() calls this.
     * 
     * Note that most filters are not thread safe, so they should not be shared among multiple connections.
     * 
     * @param filter The new SpamFilter object this ManagedConnection should use as its _routeFilter
     */
    public void setRouteFilter(SpamFilter filter) {

        // Save the given object in this one
        _routeFilter = filter;
    }

    /**
     * Set the spam filter this object will use to not show the user a Gnutella packet from (do) or to? this remote computer.
     * RouterService.adjustSpamFilters() calls this.
     * 
     * Note that most filters are not thread safe, so they should not be shared among multiple connections.
     * 
     * @param filter The new SpamFilter object this ManagedConnection should use as its _personalFilter
     */
    public void setPersonalFilter(SpamFilter filter) {

        // Save the given object in this one
        _personalFilter = filter;
    }

    /**
     * Send the given pong to this remote computer.
     * 
     * Previously, this remote computer gave us a ping, which we broadcasted forward.
     * Now, we're getting pongs in response to that ping.
     * This method sends the pongs back to the computer that wanted them.
     * 
     * The ReplyHandler interface requires this method.
     * 
     * @param pingReply           A pong addressed to this remote computer
     * @param receivingConnection Not used, may be null
     */
    public void handlePingReply(PingReply pingReply, ReplyHandler receivingConnection) {

        // Send the given packet to this remote computer
        send(pingReply);
    }

    /**
     * Send the given query hit to this remote computer.
     * 
     * Previously, this remote computer gave us a query, which we broadcasted forward.
     * Now, we're getting query hits in response to that query.
     * This method sends them back to the computer that wanted them.
     * 
     * Or, this leaf of ours sent us a query packet.
     * We decided to search on behalf of the leaf, and switched the IP address hidden in the GUID from the leaf's to our own.
     * Now, we're getting query hits in response to that query.
     * This method restores the GUID so the leaf will recognize the search, and sends the packet to the leaf.
     * 
     * The ReplyHandler interface requires this method.
     * 
     * @param queryReply          A query hit packet addressed to this remote computer
     * @param receivingConnection Not used, may be null
     */
    public void handleQueryReply(QueryReply queryReply, ReplyHandler receivingConnection) {

        /*
         * If we are proxying for a query, map back the guid of the reply
         */

        // We took a query packet from this leaf, replaced the address information in its GUID with our own, and ran the search for it
        if (_guidMap != null) {

            /*
             * In the _guidMap:
             * A key is a TimedGUID object with the morphed GUID with our IP address, and an expiration time of 10 minutes from when we added it.
             * A value is a GUID object with the original GUID with the leaf's IP address.
             * 
             * The lookup below works because TimedGUID.equals() only compares GUIDs, and doesn't look at expiration times.
             */

            // Wrap the morphed GUID with our IP address from the query hit packet we just got into a new TimedGUID object
            GUID.TimedGUID tGuid = new GUID.TimedGUID(new GUID(queryReply.getGUID()), TIMED_GUID_LIFETIME);

            // Use the new TimedGUID object to look up the morphed GUID in the _guidMap
            GUID origGUID = (GUID) _guidMap.get(tGuid);

            // If we found it
            if (origGUID != null) {

                /*
                 * we ttl 1 more than necessary
                 */

                // Change the GUID in the packet from having our IP address to having the IP address of the leaf we've been searching on behalf of
                byte prevHops = queryReply.getHops();                      // Get the hops from the query hit
                queryReply = new QueryReply(origGUID.bytes(), queryReply); // Copy the query hit packet, putting in the GUID the leaf chose to identify this search
                queryReply.setTTL((byte)2);                                // Set the TTL to 2, one more than necessary
                queryReply.setHops(prevHops);                              // Keep the hops count the same
            }
        }

        // Send the given packet to this remote computer
        send(queryReply);
    }

    /**
     * The remote computer's Client ID, the GUID that identifies it on the Gnutella network.
     * The handleMessageInternal() method reads it from the end of a QueryReply packet the remote computer generated and sent to us.
     * The ReplyHandler interface requires this method.
     * 
     * @return The remote computer's client ID GUID on the Gnutella network.
     *         If the remote computer hasn't sent us one of its own QueryReply packets yet, returns a byte array of 16 0s.
     */
    public byte[] getClientGUID() {

        // Return the GUID handleMessageInternal() read from a QueryReply packet
        return clientGUID; // Or, DataUtils.EMPTY_GUID, 16 bytes of zeroes
    }

    /**
     * Send the given push packet to this remote computer.
     * 
     * The message routing system got a push packet with a GUID that addresses it to this remote computer.
     * This method sends it there.
     * 
     * The ReplyHandler interface requires this method.
     * 
     * @param pushRequest         A push packet addressed to this remote computer
     * @param receivingConnection Not used, may be null
     */
    public void handlePushRequest(PushRequest pushRequest, ReplyHandler receivingConnection) {

        // Send the given packet to this remote computer
        send(pushRequest);
    }

    /**
     * This remote computer sent us a vendor message through our TCP socket connection with it.
     * Only MessageRouter.handleMessage() calls this.
     * 
     * @param vm The vendor message
     */
    protected void handleVendorMessage(VendorMessage vm) {

        // Save this remote computer's Messages Supported, Capabilities, or Header Update vendor messages
        super.handleVendorMessage(vm);

        // BEAR 4 Hops Flow vendor message (do)
        if (vm instanceof HopsFlowVendorMessage) {
            HopsFlowVendorMessage hops = (HopsFlowVendorMessage)vm;

            // We're an ultrapeer and this remote computer is a leaf
            if (isSupernodeClientConnection()) {

                /*
                 * If the connection is to a leaf, and it is busy (HF == 0)
                 * then set the global busy leaf flag appropriately
                 */

                // If the hop value in the message is 0, set the busy timer, otherwise clear it (do)
                setBusy(hops.getHopValue() == 0);
            }

            // Save the remote computer's hops flow value in hopsFlowMax
            hopsFlowMax = hops.getHopValue();

        // BEAR 7 Push Proxy Acknowledgement vendor message (do)
        } else if (vm instanceof PushProxyAcknowledgement) {
            PushProxyAcknowledgement ack = (PushProxyAcknowledgement)vm;

            // this connection can serve as a PushProxy, so note this....
            if (Arrays.equals(ack.getGUID(), RouterService.getMessageRouter()._clientGUID)) _pushProxy = true;
            // else mistake on the server side - the guid should be my client
            // guid - not really necessary but whatever

        // Capabilities vendor message, the remote computer is telling us what capabilities it supports
        } else if (vm instanceof CapabilitiesVM) {
            CapabilitiesVM capVM = (CapabilitiesVM)vm;

            /*
             * we need to see if there is a new simpp version out there.
             */

            // The remote compuer has a later numbered SIMPP command message than we do
            if (capVM.supportsSIMPP() >                 // The value of the "SIMPP" capability is the number of the signed SIMPP command message the remote computer has
                SimppManager.instance().getVersion()) { // If this is bigger than the number of the SIMPP command message we have received and followed

                // Ask the remote computer for it
                SimppRequestVM simppReq = new SimppRequestVM(); // Make a new SIMPP Request vendor message
                send(simppReq);                                 // Send it to the remote computer
            }

            /*
             * see if there's a new update message.
             */

            // Get the current software update number from the remote computer, and from us
            int latestId = UpdateHandler.instance().getLatestId(); // The latest update we're aware of
            int currentId = capVM.supportsUpdate();                // The update number the remote computer knows about

            // The remote computer knows about a new update
            if (currentId > latestId) {

                // Make a new Update Request vendor message and send it to this remote computer
                send(new UpdateRequest());

            // We and the remote computer know about the same update
            } else if (currentId == latestId) {

                // Add this remote comptuer as a source for the same LimeWire setup file we have
                UpdateHandler.instance().handleUpdateAvailable(this, currentId);
            }

        // Messages Supported vendor message, the remote computer is telling us what kinds of vendor messages it understands
        } else if (vm instanceof MessagesSupportedVendorMessage) {

            /*
             * If this is a ClientSupernodeConnection and the host supports
             * leaf guidance (because we have to tell them when to stop)
             * then see if there are any old queries that we can re-originate
             * on this connection.
             */

            // If we're a leaf and we just connected to this remote ultrapeer, and we searched for something less than 30 seconds ago, have this new ultrapeer search for us too
            if (isClientSupernodeConnection() &&           // This remote computer is an ultrapeer and we are just a leaf, and
                (remoteHostSupportsLeafGuidance() >= 0)) { // This remote computer can do dynamic querying, in which it will run our searches for us

                // Access the SearchResultHandler
                SearchResultHandler srh = RouterService.getSearchResultHandler();

                // Get a list of the query packets we've been searching with for the last 30 seconds that don't have enough hits yet
                List queries = srh.getQueriesToReSend();

                // Send them all to our new ultrapeer
                for (Iterator i = queries.iterator(); i.hasNext(); ) send((Message)i.next());
            }

            //do

            /*
             * see if you need a PushProxy - the remoteHostSupportsPushProxy
             * test incorporates my leaf status in it.....
             */

            if (remoteHostSupportsPushProxy() > -1) {

                // get the client GUID and send off a PushProxyRequest
                GUID clientGUID = new GUID(RouterService.getMessageRouter()._clientGUID);
                PushProxyRequest req = new PushProxyRequest(clientGUID);
                send(req);
            }

            // do i need to send any ConnectBack messages????
            if (!UDPService.instance().canReceiveUnsolicited() &&
                (_numUDPConnectBackRequests < MAX_UDP_CONNECT_BACK_ATTEMPTS) &&
                (remoteHostSupportsUDPRedirect() > -1)) {

                GUID connectBackGUID = RouterService.getUDPConnectBackGUID();
                Message udp = new UDPConnectBackVendorMessage(RouterService.getPort(), connectBackGUID);
                send(udp);
                _numUDPConnectBackRequests++;
            }

            if (!RouterService.acceptedIncomingConnection() &&
                (_numTCPConnectBackRequests < MAX_TCP_CONNECT_BACK_ATTEMPTS) &&
                (remoteHostSupportsTCPRedirect() > -1)) {

                Message tcp = new TCPConnectBackVendorMessage(RouterService.getPort());
                send(tcp);
                _numTCPConnectBackRequests++;
            }

            //done
        }
    }

    /*
     * End reply forwarding calls.
     * Begin statistics accessors.
     */

    /**
     * The number of Gnutella packets we've sent this remote computer.
     * 
     * @return The value from ConnectionStats.getSent()
     */
    public int getNumMessagesSent() {

        // Ask the ConnectionStats object for this number
        return _connectionStats.getSent();
    }

    /**
     * The number of Gnutella packets this remote computer has sent us.
     * The ReplyHandler interface requires this method.
     * 
     * @return The value from ConnectionStats.getReceived()
     */
    public int getNumMessagesReceived() {

        // Ask the ConnectionStats object for this number
        return _connectionStats.getReceived();
    }

    /**
     * The number of Gnutella packets we've sacrificed before being able to send them to this remote computer.
     * This happens when the remote computer can't keep up with us.
     * 
     * @return The value from ConnectionStats.getSentDropped()
     */
    public int getNumSentMessagesDropped() {

        // Ask the ConnectionStats object for this number
        return _connectionStats.getSentDropped();
    }

    /**
     * The number of Gnutella packets this remote computer sent us that we then dropped or filtered out.
     * We did this because we didn't like them or know how to route them.
     * 
     * @return The value from ConnectionStats.getReceivedDropped()
     */
    public long getNumReceivedMessagesDropped() {

        // Ask the ConnectionStats object for this number
        return _connectionStats.getReceivedDropped();
    }

    /**
     * If the remote computer sent us 100 packets and then we dropped 20, returns 20%.
     * Uses counts since the last time you called this method.
     * 
     * @return The received dropped percent from ConnectionStats.getPercentReceivedDropped(), like 20
     */
    public float getPercentReceivedDropped() {

        // Ask the ConnectionStats object for this number
        return _connectionStats.getPercentReceivedDropped();
    }

    /**
     * If we prepared 100 packets for this remote computer and then sacrificed 20 to stay within a bandwidth limit, returns 20%.
     * Uses counts since the last time you called this method.
     * 
     * @return The sent dropped percent from ConnectionStats.getPercentSentDropped(), like 20
     */
    public float getPercentSentDropped() {

        /*
         * TODO:kfaaborg The Javadoc here may be wrong:
         * 
         * @effects Returns the percentage of messages sent on this
         *  since the last call to getPercentSentDropped that were
         *  dropped by this end of the connection.  This value may be
         *  greater than 100%, e.g., if only one message is sent but
         *  four are dropped during a given time period.
         * 
         * It actually looks like the value can never go over 100%.
         */

        // Ask the ConnectionStats object for this number
        return _connectionStats.getPercentSentDropped();
    }

    /**
     * Watch getBytesSent() and getBytesReceived() grow to keep our records of our transfer speed up to date.
     * A thread from SupernodeAssigner calls this repeatedly.
     * Takes a snapshot of the upstream and downstream bandwidth since the last time the thread called measureBandwidth().
     */
    public void measureBandwidth() {

        // Have the BandwidthTrackerImpl objects look at the total sizes of compressed data we've exchanged with this remote computer
        _upBandwidthTracker.measureBandwidth(ByteOrder.long2int(getBytesSent()));
        _downBandwidthTracker.measureBandwidth(ByteOrder.long2int(getBytesReceived()));
    }

    /**
     * The speed we're sending data to this remote computer.
     * Computed from recent transfers, not averaged across all the time we've been connected.
     * 
     * @return The speed as a floating point number in bytes/millisecond.
     *         0 if we don't have enough data yet.
     */
    public float getMeasuredUpstreamBandwidth() {

        // Initialize no speed
        float retValue = 0;

        try {

            // Get the average of the 10 speeds that the BandwidthTrackerImpl has recorded most recently
            retValue = _upBandwidthTracker.getMeasuredBandwidth();

        // If the BandwidthTrackerImpl hasn't recorded 3 speeds yet, return no speed
        } catch (InsufficientDataException ide) { return 0; }

        // Return the average speed the BandwidthTrackerImpl object calculated
        return retValue;
    }

    /**
     * The speed we're receiving data from this remote computer.
     * Computed from recent transfers, not averaged across all the time we've been connected.
     * 
     * @return The speed as a floating point number in bytes/millisecond.
     *         0 if we don't have enough data yet.
     */
    public float getMeasuredDownstreamBandwidth() {

        // Initialize to no speed
        float retValue = 0;

        try {

            // Get the average of the 10 speeds that the BandwidthTrackerImpl has recorded most recently
            retValue = _downBandwidthTracker.getMeasuredBandwidth();

        // If the BandwidthTrackerImpl hasn't recorded 3 speeds yet, return no speed
        } catch (InsufficientDataException ide) { return 0; }

        // Return the average speed the BandwidthTrackerImpl object calculated
        return retValue;
    }

    /*
     * End statistics accessors
     */

    /**
     * Returns the time we should next forward our query route table to this remote computer.
     * incrementNextQRPForwardTime() set it to 1 minute from now if the remote computer is an ultrapeer, 5 minutes if it is a leaf.
     * 
     * This time is only valid if isClientSupernodeConnection() is true. (do)
     */
    public long getNextQRPForwardTime() {

        // Return the time 1 or 5 minutes from now that incrementNextQRPForwardTime() below set
        return _nextQRPForwardTime;
    }

	/**
     * Sets the times we should next forward a query route table to this remote computer.
     * If the remote computer is a leaf, we'll send it our QRT 5 minutes from now.
     * If the remote computer is an ultrapeer, we'll send it our QRT 1 minute from now.
     * 
     * MessageRouter.forwardQueryRouteTables() calls this with the current time.
	 * 
     * @param curTime The time right now, the value from System.currentTimeMillis()
	 */
	public void incrementNextQRPForwardTime(long curTime) {

        // The remote computer is a leaf
		if (isLeafConnection()) {

            // Set _nextQRPForardTime to 5 minutes from now
			_nextQRPForwardTime = curTime + LEAF_QUERY_ROUTE_UPDATE_TIME;

        // The remote computer is an ultrapeer
		} else {

            // Set _nextQRPForwardTime to 1 minute from now
			_nextQRPForwardTime = curTime + ULTRAPEER_QUERY_ROUTE_UPDATE_TIME;
		}
	}

    /**
     * Determine if you are allowed to close this connection.
     * Returns true, you are.
     * 
     * The ConnectionWatchdog's DudChecker calls isKillable().
     * The ReplyHandler interface requires this method.
     * 
     * @return true
     */
	public boolean isKillable() {

        /*
         * Returns true if this should not be policed by the ConnectionWatchdog,
         * e.g., because this is a connection to a Clip2 reflector. Default value:
         * true.
         * 
         * TODO:kfaaborg Clip2 reflectors are completely extinct, and this always returns true.
         */

        // Always returns true
		return _isKillable;
	}

    /**
     * Our query route table that we most recently sent to this remote computer.
     * 
     * Accessor for the query route table associated with this.  This is
     * guaranteed to be non-null, but it may not yet contain any data.
     * TODO:kfaaborg Actually, it looks like this method can return null.
     * 
     * @return Our query route table that we most recently sent to this remote computer.
     *         Returns null if we haven't sent our query route table to this remote computer yet.
     */
    public QueryRouteTable getQueryRouteTableSent() {

        // Return our query route table that we most recently sent the remote computer
        return _lastQRPTableSent;
    }

    /**
     * MessageRouter.queryRouteTable(qrt) calls this with our query route table we most recently sent to this remote computer.
     * 
     * @param qrt Our query route table that we most recently sent to this remote computer
     */
    public void setQueryRouteTableSent(QueryRouteTable qrt) {

        // Save our query route table that we most recently sent to this remote computer
        _lastQRPTableSent = qrt;
    }

    /**
     * True if the remote computer sent a PushProxyAcknowledgement vendor message that makes us a push proxy for somebody. (do)
     * 
     * The remote computer sent us a PushProxyAcknowledgement vendor specific packet.
     * handleVendorMessage() identified it, and set _pushProxy to true.
     * This means we are a push proxy for somebody (do).
     * 
     * @return True if the remote computer sent a PushProxyAcknowledgement vendor message.
     */
    public boolean isPushProxy() {

        // We set this value in handleVendorMessage()
        return _pushProxy;
    }

    /**
     * Get the object to lock on before you call the query route table methods in the ManagedConnection class.
     * 
     * Code in MessageRouter gets this lock and synchronizes on it.
     * Then, it calls methods like patchQueryRouteTable(), resetQueryRouteTable(), isBusyLeaf(), and getQueryRouteTableReceived().
     * 
     * @return The QRP_LOCK object to synchronize on
     */
	public Object getQRPLock() {

        // Return the object this class keeps to synchronize on for query route table methods
		return QRP_LOCK;
	}

    /**
     * Configure the HandshakeResponder to refuse or not refuse foreign language computers.
     * ConnectionFetcher.managedRun() calls this and always passes it false.
     * 
     * @param b False to not have the HandshakeResponder refuse foreign language remote computers
     */
    public void setLocalePreferencing(boolean b) {

        // Configure the HandshakeResponder to not refuse foreign language remote computer
        RESPONSE_HEADERS.setLocalePreferencing(b);
    }

    /**
     * Call this method to send this remote computer a Gnutella packet.
     * This is the same as calling send(m).
     * The ReplyHandler interface requires this method.
     * 
     * @param m The Gnutella packet to send this remote computer
     */
    public void reply(Message m) {

        // Just call the send method
    	send(m);
    }

    /**
     * Not used now that LimeWire has switched to NIO.
     * 
     * Repeatedly sends all the queued data using a thread.
     */
    private class BlockingRunner implements Runnable, OutputRunner {
        private final Object LOCK = new Object();
        private final MessageQueue queue;
        private boolean shutdown = false;
        public BlockingRunner(MessageQueue queue) {
            this.queue = queue;
            Thread output = new ManagedThread(this, "OutputRunner");
            output.setDaemon(true);
            output.start();
        }
        public void send(Message m) {
            synchronized (LOCK) {
                _connectionStats.addSent();
                queue.add(m);
                int dropped = queue.resetDropped();
                _connectionStats.addSentDropped(dropped);
                LOCK.notify();
            }
        }
        public void shutdown() {
            synchronized(LOCK) {
                shutdown = true;
                LOCK.notify();
            }
        }
        /** While the connection is not closed, sends all data delay. */
        public void run() {
            //For non-IOExceptions, Throwable is caught to notify ErrorService.
            try {
                while (true) {
                    waitForQueued();
                    sendQueued();
                }                
            } catch (IOException e) {
                if(_manager != null)
                    _manager.remove(ManagedConnection.this);
            } catch(Throwable t) {
                if(_manager != null)
                    _manager.remove(ManagedConnection.this);
                ErrorService.error(t);
            }
        }
        /** 
         * Wait until the queue is (probably) non-empty or closed. 
         * @exception IOException this was closed while waiting
         */
        private final void waitForQueued() throws IOException {
            // Lock outside of the loop so that the MessageQueue is synchronized.
            synchronized (LOCK) {
                while (!shutdown && isOpen() && queue.isEmpty()) {           
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if (! isOpen() || shutdown)
                throw CONNECTION_CLOSED;
        }
        /** Send several queued message of each type. */
        private final void sendQueued() throws IOException {
            // Send as many messages as we can, until we run out.
            while(true) {
                Message m = null;
                synchronized(LOCK) {
                    m = queue.removeNext();
                    int dropped = queue.resetDropped();
                    _connectionStats.addSentDropped(dropped);
                }
                if(m == null)
                    break;
                //Note that if the ougoing stream is compressed
                //(isWriteDeflated()), this call may not actually
                //do anything.  This is because the Deflater waits
                //until an optimal time to start deflating, buffering
                //up incoming data until that time is reached, or the
                //data is explicitly flushed.
                ManagedConnection.super.send(m);
            }
            //Note that if the outgoing stream is compressed 
            //(isWriteDeflated()), then this call may block while the
            //Deflater deflates the data.
            ManagedConnection.super.flush();
        }
    }

    /**
     * The GuidMapExpirer keeps a list of all the _guidMap objects in the program, and removes entries in them that grow older than 10 minutes.
     * 
     * As the program runs, there is one GuidMapExpirer object and many ManagedConnection objects.
     * Each ManagedConnection has a _guidMap object.
     * The GuidMapExpirer keeps a list of all the _guidMap objects.
     * 
     * The GuidMapExpirer schedules itself with the RouterService.
     * The RouterService has a thread that will call GuidMapExpirer.run() once every 10 minutes.
     * run() loops through every entry in every _guidMap list.
     * When it finds an entry that was added more than 10 minutes ago, it removes it.
     */
    private static class GuidMapExpirer implements Runnable {

        /**
         * A list of all the _guidMap objects the ManagedConnection objects make as the program runs.
         * 
         * This list is static.
         * As the program runs, there will be a lot of ManagedConnection objects.
         * Each ManagedConnection object will have a _guidMap member variable.
         * But, there will be only one GuidMapExpirer object for the whole program.
         * And it will have only one toEpire list.
         */
        private static List toExpire = new LinkedList();

        /**
         * True when we've scheduled the GuidMapExpirer object with the RouterService.
         * When true, the RouterService will have a thread call the run() method below once every 10 minutes.
         */
        private static boolean scheduled = false;

        /** Only the addMapToExpire() method below makes a new GuidMapExpirer() object. */
        public GuidMapExpirer() {};

        /**
         * Add the _guidMap you just made to the list of them the program keeps.
         * 
         * ManagedConnection.tryToProxy() calls this.
         * It's just made this ManagedConnection object's _guidMap, and needs to add it to the program's list.
         * 
         * @param expiree A new _guidMap we just made
         */
        public static synchronized void addMapToExpire(Map expiree) {

            // If we haven't scheduled the GuidMapExpirer with the RouterService yet, set that up now
            if (!scheduled) {

                // Have the RouterService call GuidMapExpirer.run() every 10 minutes
                RouterService.schedule(new GuidMapExpirer(), 0, TIMED_GUID_LIFETIME);

                // Record that it's scheduled
                scheduled = true;
            }

            // Add the new _guidMap to the program's list of them
            toExpire.add(expiree);
        }

        /**
         * Remove a _guidMap that belongs to a ManagedConnection object that we're disconnecting.
         * ManagedConnection.close() calls this.
         * 
         * @param expiree A _guidMap that we won't have access to anymore
         */
        public static synchronized void removeMap(Map expiree) {

            // Remove it from the program's list of all of them
            toExpire.remove(expiree);
        }

        /**
         * Loop through all the entres in all the _guidMap objects in the program, removing those that are more than 10 minutes old.
         * The RouterService has a thread call this run() method once every 10 minutes.
         */
        public void run() {

            // Only let one thread do this at a time
            synchronized (GuidMapExpirer.class) {

                // Loop for each _guidMap that a ManagedConnection object has made as the program has run
                Iterator iter = toExpire.iterator();
                while (iter.hasNext()) {
                    Map currMap = (Map)iter.next();

                    // Wait here until other threads are done with this _guidMap
                    synchronized (currMap) {

                        // Loop for each entry in the _guidMap
                        Iterator keyIter = currMap.keySet().iterator();
                        while (keyIter.hasNext()) {

                            // If it's been 10 minutes since we added this entry, remove it
                            if (((GUID.TimedGUID)keyIter.next()).shouldExpire()) keyIter.remove();
                        }
                    }
                }
            }
        }
    }
}
