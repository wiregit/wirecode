
// Commented for the Learning branch

package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * A Messages Supported vendor message lists the other vendor messages that a computer understands.
 * 
 * After 2 Gnutella computers finish the handshake, they exchange Messages Supported vendor messages.
 * This lets them communicate which other vendor messages they support.
 * 
 * It takes 8 bytes to name a kind of vendor message, like this:
 * 
 * LIMEssvv
 * 
 * LIME is the vendor code of the vendor that introduced the message.
 * ss is the ID number the vendor assigned their message.
 * vv is the version number, allowing the vendor to change the message and give it the next version number.
 * 
 * The first 8 bytes of a vendor message payload identify what type of message it is.
 * In a Messages Supported vendor message, the vendor code, ID number, and version number are all 0.
 * Beyond that is a number, and then a list of 8-byte chunks that name the vendor messages the computer supports.
 * 
 * Connection.postInit() calls MessagesSupportedVendorMessage.instance() to get our Messages Supported vendor message.
 * It sends it to a remote computer we've just finished the handshake with.
 * Here's what it looks like:
 * 
 * 6b ab 05 01 af 85 bf f5  k-------  aaaaaaaa
 * 8e b5 e4 15 c0 44 c7 00  -----D--  aaaaaaaa
 * 31 01 00 6a 00 00 00 00  1--j----  bcdeeeef
 * 00 00 00 00 00 00 00 0c  --------  fffffffg
 * 00 4c 49 4d 45 10 00 01  -LIME---  ghhhhhhh LIME 16 1 SIMPP Request
 * 00 4c 49 4d 45 08 00 01  -LIME---  hhhhhhhh LIME  8 1 UDP Connect Back Redirect
 * 00 4c 49 4d 45 19 00 01  -LIME---  hhhhhhhh LIME 25 1 Header Update
 * 00 4c 49 4d 45 06 00 01  -LIME---  hhhhhhhh LIME  6 1 UDP Crawler Pong
 * 00 42 45 41 52 0b 00 01  -BEAR---  hhhhhhhh BEAR 11 1 Query Status Request
 * 00 42 45 41 52 04 00 01  -BEAR---  hhhhhhhh BEAR  4 1 Hops Flow
 * 00 4c 49 4d 45 0e 00 01  -LIME---  hhhhhhhh LIME 14 1 Give Statistics
 * 00 47 54 4b 47 07 00 02  -GTKG---  hhhhhhhh GTKG  7 2 UDP Connect Back
 * 00 4c 49 4d 45 11 00 01  -LIME---  hhhhhhhh LIME 17 1 SIMPP
 * 00 4c 49 4d 45 07 00 01  -LIME---  hhhhhhhh LIME  7 1 TCP Connect Back Redirect
 * 00 4c 49 4d 45 15 00 01  -LIME---  hhhhhhhh LIME 21 1 Push Proxy Request
 * 00 42 45 41 52 07 00 01  -BEAR---  hhhhhhhh BEAR  7 1 TCP Connect Back
 * 00                       -         h
 * 
 * a, b, c, d, and e make up the 23 byte Gnutella packet header.
 * a is the message GUID.
 * b is 0x31, the byte that marks this as a vendor message.
 * c is the TTL, 1.
 * d is the hops, 0.
 * e is the length of the payload, 0x6a 106 bytes.
 * 
 * f is the first 8 bytes of the payload, which name what kind of vendor message this is.
 * Ordinarly, these 8 bytes are like LIMEssvv, with the vendor that introduced the message, the number they assigned it, and the version it is.
 * For a Messages Supported vendor message, all 8 bytes are 0s.
 * 
 * g is the number of vendor message names that follow.
 * Our Messages Supported vendor message lists 12 messages, so g is 0c 00, 12 in 2 bytes in little endian order.
 * 
 * h is the list of 12 messages.
 * Each message is described in 8 bytes like LIMEssvv, with the vendor, ID number, and version.
 * The addSupportedMessages() method composes the list.
 * 
 * When a remote computer sends us its Messages Supported vendor message, VendorMessage.deriveVendorMessage() calls code in this class.
 * It calls the MessagesSupportedVendorMessage(byte[], byte, byte, int, byte[]) constructor to parse the data we read into a new object.
 * 
 * The message that lets other know what messages you support.  Everytime you
 * add a subclass of VendorMessage you should modify this class (assuming your
 * message is delivered over TCP).
 */
public final class MessagesSupportedVendorMessage extends VendorMessage {

    /**
     * 0, The Messages Supported vendor message has a version number of 0.
     * Other vendor messages have a version number of 1, or 2 if they have been updated.
     */
    public static final int VERSION = 0;

    /**
     * A list of the names of the vendor messages the computer that sent us this Messages Supported vendor message supports.
     * _messagesSupported is a HashSet that contains MessagesSupportedVendorMessage.SupportedMessageBlock objects.
     */
    private final Set _messagesSupported = new HashSet();

    /**
     * The single MessagesSupportedVendorMessage object that lists the 12 kinds of vendor messages we support.
     * This is the Messages Supported vendor message that we send.
     */
    private static MessagesSupportedVendorMessage _instance;

    /**
     * Make a new MessagesSupportedVendorMessage with data we read from a remote computer.
     * This is the packet parser.
     * 
     * @param guid    The message GUID we read from the Gnutella packet header
     * @param ttl     The TTL number we read from the Gnutella packet header
     * @param hops    The hops count we read from the Gnutella packet header
     * @param version The vendor message type version number we read from the payload
     * @param payload The data of the payload beyond the Gnutella packet header and the start of the payload like LIMEssvv
     */
    MessagesSupportedVendorMessage(byte[] guid, byte ttl, byte hops, int version, byte[] payload) throws BadPacketException {

        // Call the VendorMessage constructor
        super(
            guid,                 // From the Gnutella packet header data, the message GUID
            ttl,                  // From the Gnutella packet header data, the message TTL
            hops,                 // From the Gnutella packet header data, the hops count
            F_NULL_VENDOR_ID,     // The vendor ID that names the Messages Supported vendor message is 4 0s instead of text like "LIME"
            F_MESSAGES_SUPPORTED, // The vendor message code that names the Messages Supported vendor message is 0
            version,              // From the payload, the version number, which should be 0
            payload);             // The payload after that, with a list of vendor message names like nnLIMEssvvLIMEssvvLIMEssvv

        // Make sure the version number is 0 and not something higher from the future we don't understand
        if (getVersion() > VERSION) throw new BadPacketException("UNSUPPORTED VERSION");

        /*
         * populate the Set of supported messages....
         */

        try {

            // Get the payload data like nnLIMEssvvLIMEssvvLIMEssvv, and wrap a ByteArrayInputStream around it to keep our place as we read it
            ByteArrayInputStream bais = new ByteArrayInputStream(getPayload());

            // Read the first 2 bytes, which are little endian and contain the number of LIMEssvv chunks that follow
            int vectorSize = ByteOrder.ushort2int(ByteOrder.leb2short(bais));

            // Loop that many times to turn 8 bytes like LIMEssvv into a SupportedMessageBlock object, and add it to the _messagesSupported HashSet
            for (int i = 0; i < vectorSize; i++) _messagesSupported.add(new SupportedMessageBlock(bais));

        // There was an error reading the byte array
        } catch (IOException ioe) { ErrorService.error(ioe); }
    }

    /**
     * Make the MessagesSupportedVendorMessage that lists the kinds of vendor messages we support.
     * This is the packet maker.
     * 
     * This constructor is marked private, and only the instance() method below calls it.
     * The program makes one MessagesSupportedVendorMessage object to use each time we send the message.
     */
    private MessagesSupportedVendorMessage() {

        // Call the VendorMessage constructor
        super(
            F_NULL_VENDOR_ID,     // The vendor code for the Messages Supported vendor message is 4 0 bytes instead of text like "LIME"
            F_MESSAGES_SUPPORTED, // 0, the Messages Supported vendor message has a message ID of 0
            VERSION,              // 0, the version number of the Messages Supported vendor message is 0, not 1 or 2
            derivePayload());     // Compose the payload of our Messages Supported vendor message, with the number 12 followed by 12 blocks that name the vendor messages we support

        // Add 12 SupportedMessageBlock objects that name the vendor messages we support to the _messagesSupported HashSet
        addSupportedMessages(_messagesSupported);
    }

    /**
     * Compose payload data that lists the vendor messages we support.
     * Only the MessagesSupportedVendorMessage() constructor above calls this.
     * Returns a byte array like this:
     * 
     * c0
     * LIMEssvv
     * LIMEssvv
     * LIMEssvv
     * LIMEssvv
     * 
     * The first 2 bytes have the number of vendor messages the rest of the data names.
     * They hold the number 12, 0x0c, in little endian order, c0.
     * After that are the names, each of which take 8 bytes.
     * An 8-byte name is composed of a vendor code, message ID number, and message version number.
     * 
     * @return A byte array with the payload data of our Messages Supported vendor message
     */
    private static byte[] derivePayload() {

        // Make a new HashSet, and fill it with 12 SupportedMessageBlock objects that name the vendor messages we support
        Set hashSet = new HashSet();
        addSupportedMessages(hashSet);

        try {

            // Make a new ByteArrayOutputStream that will grow to hold the data we write to it
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Write the number of vendor messages we'll describe next
            ByteOrder.short2leb((short)hashSet.size(), baos); // Writes the number in 2 bytes in little endian order

            // Loop through each SupportedMessageBlock object
            Iterator iter = hashSet.iterator();
            while (iter.hasNext()) {
                SupportedMessageBlock currSMP = (SupportedMessageBlock)iter.next();

                // Write 8 bytes like LIMEssvv, with the vendor code, type number, and version number that identifies the kind of vendor message
                currSMP.encode(baos);
            }

            // Return the data we composed
            return baos.toByteArray();

        // There was an error writing to our ByteArrayOutputStream
        } catch (IOException ioe) {

            // Have the ErrorService log it, and return null
            ErrorService.error(ioe);
            return null;
        }
    }

    /**
     * Make a SupportedMessageBlock object for each kind of vendor message we support, and add them to the given HashSet.
     * Names 12 vendor messages.
     * Here are their vendor codes, identification numbers, version numbers, and names:
     * 
     * BEAR  7 1 TCP Connect Back
     * LIME  7 1 TCP Connect Back Redirect
     * GTKG  7 2 UDP Connect Back
     * LIME  8 1 UDP Connect Back Redirect
     * 
     * LIME 25 1 Header Update
     * BEAR  4 1 Hops Flow
     * BEAR 11 1 Query Status Request
     * LIME 21 1 Push Proxy Request
     * 
     * LIME  6 1 UDP Crawler Pong
     * LIME 14 1 Give Statistics
     * LIME 16 1 SIMPP Request
     * LIME 17 1 SIMPP
     * 
     * @param hashSet A HashSet this method will add SupportedMessageBlock objects to
     */
    private static void addSupportedMessages(Set hashSet) {

        /*
         * ADD NEW MESSAGES HERE AS YOU BUILD THEM....
         * you should only add messages supported over TCP
         */

        // Make a SupportedMessageBlock that names each vendor message's vendor code, ID number, and version, and add it to the given HashSet
        SupportedMessageBlock smp = null;
        smp = new SupportedMessageBlock(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK,       TCPConnectBackVendorMessage.VERSION); hashSet.add(smp); // BEAR  7 1 TCP Connect Back
        smp = new SupportedMessageBlock(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK,       UDPConnectBackVendorMessage.VERSION); hashSet.add(smp); // GTKG  7 2 UDP Connect Back
        smp = new SupportedMessageBlock(F_BEAR_VENDOR_ID, F_HOPS_FLOW,              HopsFlowVendorMessage.VERSION);       hashSet.add(smp); // BEAR  4 1 Hops Flow
        smp = new SupportedMessageBlock(F_LIME_VENDOR_ID, F_GIVE_STATS,             GiveStatsVendorMessage.VERSION);      hashSet.add(smp); // LIME 14 1 Give Statistics
        smp = new SupportedMessageBlock(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ,         PushProxyRequest.VERSION);            hashSet.add(smp); // LIME 21 1 Push Proxy Request
        smp = new SupportedMessageBlock(F_BEAR_VENDOR_ID, F_LIME_ACK,               QueryStatusRequest.VERSION);          hashSet.add(smp); // BEAR 11 1 Query Status Request, not used
        smp = new SupportedMessageBlock(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK,       TCPConnectBackRedirect.VERSION);      hashSet.add(smp); // LIME  7 1 TCP Connect Back Redirect
        smp = new SupportedMessageBlock(F_LIME_VENDOR_ID, F_UDP_CONNECT_BACK_REDIR, UDPConnectBackRedirect.VERSION);      hashSet.add(smp); // LIME  8 1 UDP Connect Back Redirect
        smp = new SupportedMessageBlock(F_LIME_VENDOR_ID, F_ULTRAPEER_LIST,         UDPCrawlerPong.VERSION);              hashSet.add(smp); // LIME  6 1 UDP Crawler Pong
        smp = new SupportedMessageBlock(F_LIME_VENDOR_ID, F_SIMPP_REQ,              SimppRequestVM.VERSION);              hashSet.add(smp); // LIME 16 1 SIMPP Request
        smp = new SupportedMessageBlock(F_LIME_VENDOR_ID, F_SIMPP,                  SimppVM.VERSION);                     hashSet.add(smp); // LIME 17 1 SIMPP Message
        smp = new SupportedMessageBlock(F_LIME_VENDOR_ID, F_HEADER_UPDATE,          HeaderUpdateVendorMessage.VERSION);   hashSet.add(smp); // LIME 25 1 Header Update

        /*
         * TODO:kfaaborg Remove BEAR 11 1 QueryStatusRequest, which is not used.
         */
    }

    /**
     * Get the MessagesSupportedVendorMessage object that lists the kinds of vendor messages we support.
     * Connection.postInit() does this to send our Messages Supported vendor message to a remote computer we just finished the handshake with.
     * 
     * @return A reference to the program's one MessagesSupportedVendorMessage object
     */
    public static MessagesSupportedVendorMessage instance() {

        // If the program hasn't needed our MessagesSupportedVendorMessage object before, make it and save it now
        if (_instance == null) _instance = new MessagesSupportedVendorMessage();

        // Return a reference to the MessagesSupportedVendorMessage we made with information about us
        return _instance;
    }

    /**
     * Determine if this Messages Supported vendor message lists a given kind of vendor message, and find out for what version it indicates support.
     * 
     * @param vendorID The vendor ID of a vendor message, like "LIME".
     * @param selector The vendor message type number, like 4.
     * @return         If this Messages Supported vendor message indicates support for that kind of vendor message, the version number it supports.
     *                 -1 if not listed.
     */
    public int supportsMessage(byte[] vendorID, int selector) {

        // Loop through the vendor messages named in this packet
        Iterator iter = _messagesSupported.iterator();
        while (iter.hasNext()) {
            SupportedMessageBlock currSMP = (SupportedMessageBlock)iter.next(); // Each type of vendor message is represented by a SupportedMessageBlock object

            // If this vendor message name matches the given vendor ID and message type, return its version number
            int version = currSMP.matches(vendorID, selector); // The matches() method returns -1 if the vendor ID and message type number don't match
            if (version > -1) return version;                  // We found it, return the version number this Messages Supported vendor message indicates support for
        }

        // Not found
        return -1;
    }

    /**
     * Determine if this Messages Supported vendor message indicates support for the TCP Connect Back vendor message.
     * Looks for BEAR 7 in the list of message names to get a version number like 1.
     * 
     * @return The version number supported, or -1 if it's not listed
     */
    public int supportsTCPConnectBack() {

        // Search this Messages Supported message for BEAR 7
        return supportsMessage(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK);
    }

    /**
     * Determine if this Messages Supported vendor message indicates support for the UDP Connect Back vendor message.
     * Looks for GTKG 7 in the list of message names to get a version number like 2.
     * 
     * @return The version number supported, or -1 if it's not listed
     */
    public int supportsUDPConnectBack() {

        // Search this Messages Supported message for GTKG 7
        return supportsMessage(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK);
    }

    /**
     * Determine if this Messages Supported vendor message indicates support for the TCP Connect Back Redirect vendor message.
     * Looks for LIME 7 in the list of message names to get a version number like 1.
     * 
     * @return The version number supported, or -1 if it's not listed
     */
    public int supportsTCPConnectBackRedirect() {

        // Search this Messages Supported message for LIME 7
        return supportsMessage(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK);
    }

    /**
     * Determine if this Messages Supported vendor message indicates support for the UDP Connect Back Redirect vendor message.
     * Looks for LIME 8 in the list of message names to get a version number like 1.
     * 
     * @return The version number supported, or -1 if it's not listed
     */
    public int supportsUDPConnectBackRedirect() {

        // Search this Messages Supported message for LIME 8
        return supportsMessage(F_LIME_VENDOR_ID, F_UDP_CONNECT_BACK_REDIR);
    }

    /**
     * Determine if this Messages Supported vendor message indicates support for the Hops Flow vendor message.
     * Looks for BEAR 4 in the list of message names to get a version number like 1.
     * 
     * @return The version number supported, or -1 if it's not listed
     */
    public int supportsHopsFlow() {

        // Search this Messages Supported message for BEAR 4
        return supportsMessage(F_BEAR_VENDOR_ID, F_HOPS_FLOW);
    }

    /**
     * Determine if this Messages Supported vendor message indicates support for the Push Proxy Request vendor message.
     * Looks for LIME 21 in the list of message names to get a version number like 1.
     * 
     * @return The version number supported, or -1 if it's not listed
     */
    public int supportsPushProxy() {

        // Search this Messages Supported message for LIME 21
        return supportsMessage(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ);
    }

    /**
     * Determine if this Messages Supported vendor message indicates support for the Give Statistics vendor message.
     * Looks for LIME 14 in the list of message names to get a version number like 1.
     * 
     * @return The version number supported, or -1 if it's not listed
     */
    public int supportsGiveStatsVM() {

        // Search this Messages Supported message for LIME 14
        return supportsMessage(F_LIME_VENDOR_ID, F_GIVE_STATS);
    }

    /**
     * Determine if this Messages Supported vendor message indicates support for the Query Status Request vendor message.
     * Looks for BEAR 11 in the list of message names to get a version number like 1.
     * 
     * @return The version number supported, or -1 if it's not listed
     */
    public int supportsLeafGuidance() {

        // Search this Messages Supported message for BEAR 11
        return supportsMessage(F_BEAR_VENDOR_ID, F_LIME_ACK);
    }

    /**
     * Determine if this Messages Supported vendor message indicates support for the UDP Crawler Pong vendor message.
     * Looks for LIME 6 in the list of message names to get a version number like 1.
     * 
     * @return The version number supported, or -1 if it's not listed
     */
    public int supportsUDPCrawling() {

        // Search this Messages Supported message for LIME 6
    	return supportsMessage(F_LIME_VENDOR_ID, F_ULTRAPEER_LIST);
    }

    /**
     * Determine if this Messages Supported vendor message indicates support for the Header Update vendor message.
     * Looks for LIME 25 in the list of message names to get a version number like 1.
     * 
     * @return The version number supported, or -1 if it's not listed
     */
    public int supportsHeaderUpdate() {

        // Search this Messages Supported message for LIME 25
        return supportsMessage(F_LIME_VENDOR_ID, F_HEADER_UPDATE);
    }

    /**
     * Determine if a given MessagesSupportedVendorMessage object lists the same vendor messages as this one.
     * 
     * @param other The object to compare this one to
     * @return      True if they contain the same information, false if they are different
     */
    public boolean equals(Object other) {

        /*
         * basically two of these messages are the same if the support the same
         * messages
         */

        // Make sure the given object is also a MessagesSupportedVendorMessage
        if (other instanceof MessagesSupportedVendorMessage) {
            MessagesSupportedVendorMessage vmp = (MessagesSupportedVendorMessage)other;

            // Compare the HashSet lists of SupportedMessageBlock objects
            return (_messagesSupported.equals(vmp._messagesSupported)); // Calls SupportedMessageBlock.equals()
        }

        // Different
        return false;
    }

    /**
     * Hash the information in this MessagesSupportedVendorMessage object into a number.
     * 
     * @return The hash code number
     */
    public int hashCode() {

        // Get the hash code of the Java HashSet _messagesSupported, and multiply that by 17
        return 17 * _messagesSupported.hashCode();
    }

    /**
     * A SupportedMessageBlock object represents a single vendor message type that we support.
     * The MessagesSupportedVendorMessage class puts SupportedMessageBlock objects in its _messagesSupported HashSet.
     * This makes up the list of vendor messages that the MessagesSupportedVendorMessage object documents support for.
     */
    static class SupportedMessageBlock {

        /** The 4 byte vendor ID like "LIME" of the vendor that introduced the vendor message this SupportedMessageBlock names. */
        final byte[] _vendorID;

        /** The vendor message type number, like 4 for Hops Flow. */
        final int _selector;

        /** The vendor message version. */
        final int _version;

        /** A number made from the contents of this object. */
        final int _hashCode;

        /**
         * Make a new SupportedMessageBlock from information that identifies a type of vendor message.
         * 
         * @param vendorID The vendor ID like "LIME" of the vendor that introduced the kind of vendor message
         * @param selector The vendor messate type number, like 4 for Hops Flow
         * @param version  The vendor message version
         */
        public SupportedMessageBlock(byte[] vendorID, int selector, int version) {

            // Save the information that identifies a kind of vendor message
            _vendorID = vendorID;
            _selector = selector;
            _version  = version;

            // Use this information to compute this new object's hash code
            _hashCode = computeHashCode(_vendorID, _selector, _version);
        }

        /**
         * Make a new SupportedMessageBlock from the 8 bytes that start a vendor message payload, like LIMEssvv.
         * "LIME" identifies the vendor that introduced the message, ss is a number that tells the type, and vv is a version number that allows changes.
         * 
         * @param encodedBlock An InputStream object we can call read() on to read the 8 bytes
         */
        public SupportedMessageBlock(InputStream encodedBlock) throws BadPacketException, IOException {

            // Make sure we'll be able to read the 8 bytes we need
            if (encodedBlock.available() < 8) throw new BadPacketException("invalid data.");

            // Read the vendor ID like "LIME", the first 4 bytes
            _vendorID = new byte[4];
            encodedBlock.read(_vendorID, 0, _vendorID.length);

            // Read the type number and version number, which are 2 bytes each
            _selector = ByteOrder.ushort2int(ByteOrder.leb2short(encodedBlock));
            _version = ByteOrder.ushort2int(ByteOrder.leb2short(encodedBlock));

            // Use this information to compute this new object's hash code
            _hashCode = computeHashCode(_vendorID, _selector, _version);
        }

        /**
         * Write out this SupportedMessageBlock as data.
         * Writes 8 bytes like LIMEssvv, with the vendor ID, type number, and version number.
         * 
         * @param out An OutputStream object we can call out.write(data) on to give it data
         */
        public void encode(OutputStream out) throws IOException {

            // Write 8 bytes like LIMEssvv with the vendor ID, selector, and version
            out.write(_vendorID);
            ByteOrder.short2leb((short)_selector, out); // Writes the number in 2 bytes
            ByteOrder.short2leb((short)_version, out);  // Writes the number in 2 bytes
        }

        /**
         * Determine if this SupportedMessageBlock names the kind of vendor message you give the vendor ID and type number for, and get its version number.
         * 
         * A vendor message is identified by a vendor ID, type number, and version number, like "LIME, 11, 1".
         * Give this method "LIME, 11".
         * If this SupportedMessageBlock was made to name the "LIME, 11" vendor message, it will return the version number, 1.
         * If this SupportedMessageBlock names a different vendor message, like "BEAR, 11" or "LIME, 23", returns -1.
         * 
         * @param vendorID The vendor ID of a vendor message, like "LIME"
         * @param selector The vendor message type number, like 4
         * @return         The version number from this SupportedMessageBlock, or -1 if the vendor ID and message type number don't match
         */
        public int matches(byte[] vendorID, int selector) {

            // If the given vendor ID and message type number match, return the version number
            if ((Arrays.equals(_vendorID, vendorID)) && (_selector == selector)) return _version;
            else return -1; // This SupportedMessageBlock isn't a "vendorID, selector" vendor message
        }

        /**
         * Determine if a given object names the same vendor message as this one.
         * 
         * @param other Another SupportedMessageBlock object
         * @return      True if the objects are the same, false if they are different
         */
        public boolean equals(Object other) {

            // Make sure the given object is a SupportedMessageBlock like this one
            if (other instanceof SupportedMessageBlock) {

                // Return true if the type number, version, and introducing vendor match
                SupportedMessageBlock vmp = (SupportedMessageBlock) other;
                return ((_selector == vmp._selector) && (_version == vmp._version) && (Arrays.equals(_vendorID, vmp._vendorID)));
            }

            // Different
            return false;
        }

        /**
         * The hash code made from the information in this object.
         * 
         * @return The hash code number
         */
        public int hashCode() {

            // Return the hash code we computed and saved
            return _hashCode;
        }

        /**
         * Compute a hash code from the information that identifies a kind of vendor message.
         * 
         * @param vendorID A vendor ID like "LIME"
         * @param selector A number that identifies a kind of vendor packet, like 4
         * @param version  A version number of a vendor packet, like 1
         * @return         A hash code number
         */
        private static int computeHashCode(byte[] vendorID, int selector, int version) {

            // Compute and return the hash code
            int hashCode = 0;
            hashCode += 37 * version;
            hashCode += 37 * selector;
            for (int i = 0; i < vendorID.length; i++) hashCode += (int)37 * vendorID[i]; // Loop 4 times
            return hashCode;
        }
    }

    /**
     * Write the payload of this vendor message to the given OutputStream.
     * Writes the 8-byte LIMEssvv type identifer that begins the payload, and the type specific payload data after that.
     * This class overrides this method from VendorMessage to count the statistic that we're sending this message.
     * 
     * A Messages Supported vendor message looks like this:
     * 
     * gnutella header 23 bytes
     * vendor message type 8 bytes
     * 
     *   LIMEssvv  Tells what kind of vendor message this is
     * 
     * rest of the payload
     * 
     *   nn        The number of vendor message names that follow
     *   LIMEssvv  A list of vendor message names
     *   LIMEssvv
     *   LIMEssvv
     * 
     * The vendor message type is like LIMEssvv, but for a Messages Supported vendor message, it's all 0s.
     * This method writes the whole payload, the vendor message type and the rest of the payload.
     * 
     * MessagesSupportedVendorMessage overrides this method from VendorMessage to count a statistic.
     * 
     * @param out An OutputStream we can call write() on to send data to the remote computer
     */
    protected void writePayload(OutputStream out) throws IOException {

        // Call VendorMessage.writePayload(out) to write the vendor message type bytes followed by _payload
        super.writePayload(out);

        // If we're writing the payload, we must be sending this message to a remote computer
        SentMessageStatHandler.TCP_MESSAGES_SUPPORTED.addMessage(this);
    }

    /**
     * Does nothing.
     * Call recordDrop() to record that we're dropping this Gnutella packet in the program's statistics.
     */
    public void recordDrop() {

        // Does nothing
        super.recordDrop();
    }
}
