
// Commented for the Learning branch

package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.LinkedList;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.NameValue;

/**
 * A Gnutella ping message.
 * 
 * Traditionally, ping packets don't have payloads, and consist only of a 23-byte Gnutella packet header.
 * LimeWire uses pings that have payloads, however.
 * These are called big pings.
 * 
 * The payload is a GGEP block.
 * In LimeWire, a GGEP block is represented by a GGEP object.
 * Here, it's the _ggep member.
 * The serialized data of the GGEP block is the byte array member named payload.
 * 
 * We can send pings in a TCP socket connection, or in UDP packets.
 * UDP pings can be intended for remote PCs like us, UDP host caches, or the multicast network.
 * 
 * Pinger.run() makes a ping we send to all our connections.
 * Here's what one looks like:
 * 
 * 84 11 e8 94 14 4a 4a 9c  -----JJ-  aaaaaaaa
 * c2 b0 25 19 51 ac 67 00  --%-Q-g-  aaaaaaaa
 * 00 03 00 09 00 00 00 c3  --------  bcdeeeef
 * 83 4c 4f 43 42 65 6e 00  -LOCBen-  gggggggg
 * 
 * a is the 16 byte message GUID.
 * b is 0x00, the byte code for a ping.
 * c is the TTL, 3.
 * d is the hops, 0.
 * 
 * e is the length of the payload, 0x09, which is 9 bytes.
 * The length is 4 bytes in little endian order, like 09 00 00 00.
 * 
 * f is 0xC3, the byte that begins a GGEP block.
 * The remaining bytes contain a single GGEP extension, "LOC" Locale preference.
 * 
 * 83        4c  4f  43  42        65  6e  00
 * 1000 0011 'L' 'O' 'C' 0100 0010 'e' 'n' 0
 * hij  kkkk             llmm mmmm         n
 * 
 * h is 1 to indicate this is the last extension in the GGEP block.
 * i and j are 0 because the value isn't COBS encoded or defalte compressed.
 * k is 3, the length of the extension name "LOC".
 * l is 01 to mark that this byte is the last needed to hold the value length.
 * m is 2, the length of the extension value "en".
 * n is a 0 that addGGEPs() adds to terminate the GGEP block, and may not be necessary.
 */
public class PingRequest extends Message {

    /*
     * Here's how the "SCP" and "IPP" GGEP extensions work.
     * 
     * "IPP" is used in UDP pongs.
     * It tells the IP addresses and port numbers of computers running Gnutella software that you can try to connect to.
     * It's like the "X-Try-Ultrapeers" handshake header.
     * "IPP" has a value a multiple of 6 bytes long, with each 6 bytes containing an IP address and port number.
     * 
     * "SCP" is used in UDP pings.
     * It requests "IPP" in the responding pong.
     * It has a 1 byte value.
     * If the byte value is 0x00, it wants the addresses of ultrapeers with free leaf slots.
     * If the byte value is 0x01, it wants the addresses of ultrapeers with free ultrapeer slots.
     */

    /** 0x01, look in the lowest bit in "SCP" byte value to see what the ping wants. */
    public static final byte SCP_ULTRAPEER_OR_LEAF_MASK = 0x1;

    /** 0x00, the "SCP" ping wants "IPP" with the addresses of ultrapeers with free leaf slots. */
    public static final byte SCP_LEAF = 0x0;

    /** 0x01, the "SCP" ping wants "IPP" with the addresses of ultrapeers with free ultrapeer slots. */
    public static final byte SCP_ULTRAPEER = 0x1;

    /**
     * The payload of this ping if it is a big ping.
     * 
     * Gnutella ping packets are just a header.
     * The payload length in the header is set to 0, and there is no payload.
     * But, newer extensions define big pings and big pongs.
     * These ping and pong packets do have a payload.
     * If this PingRequest object describes a big ping, it will have a payload.
     */
    private byte[] payload = null;

    /**
     * The GGEP block this ping carries.
     * If this is a big ping, it's payload will be a GGEP block.
     * Code in this class parses and edits the GGEP block, and serializes its data when we send the packet.
     */
    private GGEP _ggep;

    /*
     * /////////////////Constructors for incoming messages/////////////////
     */

    /**
     * Make a new PingRequest object to represent the Gnutella ping packet we just received.
     * Use this constructor when reading packets from the network.
     * 
     * @param guid The GUID that uniquely identifies this ping packet
     * @param ttl  The message TTL, the number of times it will travel across the Internet
     * @param hops The number of times this Gnutella message has traveled across the Internet
     */
    public PingRequest(byte[] guid, byte ttl, byte hops) {

        // Call the Message constructor, adding the 0x00 ping byte and a payload length of 0 to the information we were given
        super(guid, Message.F_PING, ttl, hops, 0);
    }

    /**
     * Not used.
     * 
     * Creates an incoming group ping. Used only by boot-strap server.
     */
    protected PingRequest(byte[] guid, byte ttl, byte hops, byte length) {
        super(guid, Message.F_PING, ttl, hops, length);
    }

    /**
     * Make a new PingRequest object to represent the Gnutella ping packet we just received that has a payload.
     * Message.createMessage() calls this when it gets a ping that has a payload.
     * 
     * @param guid    The GUID that uniquely identifies this ping packet
     * @param ttl     The message TTL, the number of times it will travel across the Internet
     * @param hops    The number of times this Gnutella message has traveled across the Internet
     * @param payload A byte array that contains the data of the payload
     * @param length  The payload length, the number of bytes in the message beyond the Gnutella message header
     */
    public PingRequest(byte[] guid, byte ttl, byte hops, byte[] payload) {

        // Call the Message constructor, adding the 0x00 ping byte to the information we were given
        super(guid, Message.F_PING, ttl, hops, payload.length); // Get the length from the given byte array

        // Save the given payload in this new PingRequest object
        this.payload = payload;
    }

    /*
     * ////////////////////// Constructors for outgoing Pings /////////////
     */

    /**
     * Make a new Gnutella ping packet for us to send with a new unique GUID.
     * It comes with a GGEP block with the key "LOC" and a value like "en", indicating our language preference.
     * 
     * @param ttl The TTL for the packet, the number of times it should hop across the Internet before dying
     */
    public PingRequest(byte ttl) {

        // Call the Message constructor to set values in the packet header
        super(
            (byte)0x0, // 0x00 identifies this as a ping packet
            ttl,       // The number of times this packet will travel across the Internet
            (byte)0);  // Payload length 0, no payload yet

        // Add a GGEP block with the key "LOC" and value like "en", indicating our language preference
        addBasicGGEPs();
    }

    /**
     * Make a new Gnutella ping packet for us to send with the specified GUID.
     * It comes with a GGEP block with the key "LOC" and a value like "en", indicating our language preference.
     * 
     * @param guid The GUID for this new packet
     * @param ttl  The TTL for the packet, the number of times it should hop across the Internet before dying
     */
    public PingRequest(byte[] guid, byte ttl) {

        // Call the Message constructor to set values in the packet header
        super(
            guid,      // GUID, use the given GUID
            (byte)0x0, // Function byte, 0x00 identifies this as a ping packet
            ttl,       // TTL, use the given number of times this packet can travel across the Internet
            (byte)0,   // Hops, 0 because this packet hasn't traveled across the Internet yet
            0);        // Length, 0 because we haven't added a payload yet

        // Add a GGEP block with the key "LOC" and value like "en", indicating our language preference
        addBasicGGEPs();
    }

    /**
     * Make a new Gnutella ping packet for us to send with the given GUID, TTL, and GGEP fields.
     * 
     * @param guid  The GUID for this new packet
     * @param ttl   The TTL for this packet, the number of times it should hop across the Internet before dying
     * @param ggeps A List of NameValue objects with GGEP tags and values to add
     */
    private PingRequest(byte[] guid, byte ttl, List ggeps) {

        // Call the Message constructor to set values in the packet header
        super(
            guid,      // GUID, use the given GUID
            (byte)0x0, // Function byte, 0x00 identifies this as a ping packet
            ttl,       // TTL, use the given number of times this packet can travel across the Internet
            (byte)0,   // Hops, 0 because this packet hasn't traveled across the Internet yet
            0);        // Length, 0 because we haven't added a payload yet

        // Add the given GGEP block
        addGGEPs(ggeps);
    }

    /**
     * Make a new QueryKey ping for us to send.
     * It will have hops 0, TTL 1, and a GGEP block with the key "QT" with no value.
     * QueryKey is a part of GUESS, which LimeWire no longer uses.
     * 
     * @return A new PingRequest object
     */
    public static PingRequest createQueryKeyRequest() {

        // Make a new LinkedList of NameValue objects with just one, the key "QK" and no value
        List l = new LinkedList();
        l.add(new NameValue(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT));

        // Make and return a new PingRequest object
        return new PingRequest(
            GUID.makeGuid(), // Make a new unique GUID for the packet
            (byte)1,         // Set a TTL of 1
            l);              // Add the GGEP block with the "QT" key and no value
    }

    /**
     * Make a TTL 1 ping we'll send to remote computers over UDP.
     * 
     * If we're externally contactable, it will have a new unique GUID.
     * If we're behind a NAT, it will have the GUID we chose for "SCP" solicited ping attempts.
     * It will have the GGEP field "SCP" with a value of 0x01 if we're an ultrapeer, and 0x00 if we're a leaf.
     * 
     * @return A new PingRequest object
     */
    public static PingRequest createUDPPing() {

        // Make a new ping packet with a TTL of 1 and either a random GUID or our special "SCP" GUID
        List l = new LinkedList();
        return new PingRequest(
            populateUDPGGEPList(l).bytes(), // Choose a GUID and GGEP fields for the new UDP ping packet
            (byte)1,                        // TTL of 1
            l);                             // The GGEP tags that populateUDPGGEPList(l) set
    }

    /**
     * Make a TTL 1 ping we'll send to UDP host caches.
     * 
     * If we're externally contactable, it will have a new unique GUID.
     * If we're behind a NAT, it will have the GUID we chose for "SCP" solicited ping attempts.
     * It will have the GGEP field "SCP" with a value of 0x01 if we're an ultrapeer, and 0x00 if we're a leaf.
     * It will have the GGEP field "UDPHC" with no value, indicating this ping is for a UDP host cache.
     * 
     * @return A new PingRequest object
     */
    public static PingRequest createUHCPing() {

        // Choose a GUID and GGEP fields for the new UDP ping packet
        List ggeps = new LinkedList();
        GUID guid = populateUDPGGEPList(ggeps);

        // Add the key "UDPHC" with no value, indicating this ping is for a UDP host cache
        ggeps.add(new NameValue(GGEP.GGEP_HEADER_UDP_HOST_CACHE));

        // Make a new ping packet with a TTL of 1 and either a random GUID or our special "SCP" GUID
        return new PingRequest(
            guid.bytes(), // Use the random or special "SCP" GUID
            (byte)1,      // TTL of 1
            ggeps);       // The GGEP tags that populateUDPGGEPList(l) set, and the "UDPHC" one
    }

    /**
     * Choose a GUID and GGEP fields for a new UDP ping packet we'll send.
     * 
     * If we're externally contactable, makes a new unique GUID.
     * If we're behind a NAT device, adds "IP" to the GGEP block and uses the GUID we chose for solicited ping attempts.
     * Adds the GGEP field "SCP" with a value of 0x01 if we're an ultrapeer, or 0x00 if we're a leaf.
     * 
     * @param l A List this method adds NameValue objects to for the fields like "IP" and "SCP"
     * @return  The GUID the new ping should have
     */
    private static GUID populateUDPGGEPList(List l) {

        // Remote computers can contact us on our TCP listening socket
        GUID guid;
        if (ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue()) {

            // Make a new unique GUID for the ping packet we're making
            guid = new GUID();

        // We may be behind a NAT device
        } else {

            // Prepare the key "IP" with no value to the list we'll add to the ping's GGEP block
            l.add(new NameValue(GGEP.GGEP_HEADER_IPPORT)); // This identifies it as a request for IP address and port number information

            // Ask the UDPService for the GUID we've been using for our solicited ping attempts
            guid = UDPService.instance().getSolicitedGUID();
        }

        // Add the GGEP field "SCP" with a value of 0x01 if we're an ultrapeer, or 0x00 if we're a leaf
        byte[] data = new byte[1];
        if (RouterService.isSupernode()) data[0] = SCP_ULTRAPEER;
        else                             data[0] = SCP_LEAF;
        l.add(new NameValue(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS, data));

        // Return the GUID we chose for this new ping packet
        return guid;
    }

    /**
     * Make a TTL 1 ping we'll send to the multicast network over UDP. (do)
     * 
     * It will have a new unique GUID.
     * It will have the GGEP field "SCP" with a value of 0x01 if we're an ultrapeer, or 0x00 if we're a leaf.
     * 
     * @return A new PingRequest object
     */
    public static PingRequest createMulticastPing() {

        // Make a new unique GUID for this ping
        GUID guid = new GUID();

        // Add the GGEP field "SCP" with a value of 0x01 if we're an ultrapeer, or 0x00 if we're a leaf
        byte[] data = new byte[1];
        if (RouterService.isSupernode()) data[0] = 0x1;
        else                             data[0] = 0x0;
        List l = new LinkedList();
        l.add(new NameValue(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS, data));

        // Make a new ping packet with a TTL of 1
        return new PingRequest(
            guid.bytes(), // Use the new random GUID we made
            (byte)1,      // TTL of 1
            l);           // The GGEP "SCP" tag with the value 0x01 ultrapeer or 0x00 leaf
    }

    /*
     * ///////////////////////////// methods ///////////////////////////
     */

    /**
     * Write this ping's GGEP block to the given OutputStream.
     * 
     * @param out The OutputStream we can call write(b) on to send data to the remote computer
     */
    protected void writePayload(OutputStream out) throws IOException {

        // If this ping packet has a GGEP block payload, write it out
        if (payload != null) out.write(payload);

        // Give this message to the SentMessageHandler as a ping we sent over TCP
        SentMessageStatHandler.TCP_PING_REQUESTS.addMessage(this);
    }

    /**
     * Make a copy of this PingRequest, but without the GGEP block payload.
     * If this ping doesn't have a payload, returns a reference to this same object.
     * 
     * @return A new PingRequest with no GGEP block, or a reference to this same one
     */
    public Message stripExtendedPayload() {

        // This ping packet has no GGEP block payload
        if (payload == null) {

            // Return a reference to this object
            return this;

        // This ping packet has a GGEP block payload
        } else {

            // Make a new PingRequest with this one's GUID, TTL, an hops count, but no GGEP block
            return new PingRequest(this.getGUID(), this.getTTL(), this.getHops());
        }
    }

    /**
     * Record that we're dropping this ping packet.
     */
	public void recordDrop() {

        // Give the whole packet to the DroppedSentMessageStatHandler for TCP ping requests
		DroppedSentMessageStatHandler.TCP_PING_REQUESTS.addMessage(this);
	}

    /**
     * Express this ping packet as text.
     * Composes text like "PingRequest({guid=00FF00FF00FF00FF00FF00FF00FF00FF, ttl=1, hops=1, priority=1})".
     * 
     * This isn't the same thing as turning it into bytes to send to a computer.
     * 
     * @return A String with the ping's GUID, TTL, hops count, and priority
     */
    public String toString() {

        // Compose text like "PingRequest({guid=00FF00FF00FF00FF00FF00FF00FF00FF, ttl=1, hops=1, priority=1})"
        return "PingRequest(" + super.toString() + ")";
    }

    /**
     * Determine if this ping is a heartbeat ping.
     * Heartbeat pings have hops 1 and TTL 0.
     * Computers send them to keep a TCP socket open that would close if it was silent for too long.
     * 
     * @return True if this ping has hops 1 and TTL 0
     */
    public boolean isHeartbeat() {

        // Return true if this ping has a hops count of 1, and a TTL of 0
        return (getHops() == 1 && getTTL() == 0);
    }

    /**
     * Adds the key "IP" with no value to this ping's GGEP block.
     * This makes this ping request a pong with IP address and port number information.
     */
    public void addIPRequest() {

        // Add the key "IP" and no value to this ping packet's GGEP block
        List l = new LinkedList();
        l.add(new NameValue(GGEP.GGEP_HEADER_IPPORT));
        addGGEPs(l);
    }

    /**
     * Adds the key "LOC" with a value like "en" to this ping's GGEP block.
     */
    private void addBasicGGEPs() {

        // Add the key "LOC" and a value like "en" for English to this ping packet's GGEP block
        List l = new LinkedList();
        l.add(new NameValue(GGEP.GGEP_HEADER_CLIENT_LOCALE, ApplicationSettings.LANGUAGE.getValue()));
        addGGEPs(l);
    }

    /**
     * Add a given list of name and value text to this packet's GGEP block and payload.
     * 
     * @param ggeps A LinkedList of NameValue objects, each of which has a key and value
     */
    private void addGGEPs(List ggeps) {

        // Make a new ByteArrayOutputStream we'll use to compose the payload
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {

            // Format the name value pairs in the given list into a GGEP block, and save it as the payload of this ping
            if (_ggep == null) _ggep = new GGEP(true); // If this PingRequest doesn't have a GGEP block, make it a new empty one
            _ggep.putAll(ggeps);                       // Add all the NameValue objects in the given LinkedList into the GGEP object
            _ggep.write(baos);                         // Have the GGEP object write all its data into the ByteArrayOutputStream
            baos.write(0);                             // Write a 0 byte that terminates the GGEP block
            payload = baos.toByteArray();              // Have the ByteArrayOutputStream release everything we wrote as a byte array, and point payload at it
            updateLength(payload.length);              // Set the paylaod length in the Gnutella packet header to the length of the GGEP block

            /*
             * TODO:kfaaborg Terminating the GGEP block with a 0 isn't necessary.
             */

        // Pass an exception to the ErrorService
        } catch (IOException e) { ErrorService.error(e); }
    }

    /**
     * Get the language preference stored under "LOC" in this big ping's GGEP block.
     * If this isn't a big ping, returns our default language preference.
     * 
     * @return A language preference, like "en" for English
     */
    public String getLocale() {

        // If this is a big ping with a payload, we can look in the GGEP block for the language preference
        if (payload != null) {

            try {

                // If this ping doesn't have a GGEP object, make it a new blank one
                parseGGEP();

                // If our GGEP block contains the key "LOC", parse and return the value, like "en" for English
                if (_ggep.hasKey(GGEP.GGEP_HEADER_CLIENT_LOCALE)) return _ggep.getString(GGEP.GGEP_HEADER_CLIENT_LOCALE);

            // Catch and ignore exceptions
            } catch (BadGGEPBlockException ignored) {
            } catch (BadGGEPPropertyException ignoredToo) {}
        }

        // This ping doesn't have a GGEP block, or a "LOC" key, or there was an error reading it
        return ApplicationSettings.DEFAULT_LOCALE.getValue(); // Return our language preference, like "en" for English
    }

    /**
     * Determine if this ping has the "supports cached pongs" marking.
     * This is the key "SCP" in the GGEP block.
     * 
     * @return True if this ping has a GGEP block with the key "SCP", false if it doesn't
     */
    public boolean supportsCachedPongs() {

        // If this is a big ping with a payload, we can look in the GGEP block for the supports cached pongs marking
        if (payload != null) {
            
            try {
                
                // If this ping doesn't have a GGEP object, make it a new blank one
                parseGGEP();
                
                // If our GGEP block has the key "SCP", return true
                return _ggep.hasKey(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS);
                
            // There was an error reading the GGEP block, return false below
            } catch (BadGGEPBlockException ignored) {}
        }

        // This ping doesn't have a GGEP block, or there was an error reading it, return false
        return false;
    }

    /**
     * Get the value of the "supports cached pongs" marking in this big ping's GGEP block.
     * This is the value stored under the "SCP" key.
     * 
     * @return A byte array with the value of the "SCP" key.
     *         If we don't have a GGEP block or a "SCP" key, returns an empty byte array or null. (do)
     */
    public byte[] getSupportsCachedPongData() {

        // The byte array that we'll return
        byte[] ret = null;

        // If this is a big ping with a payload, we can look in the GGEP block for the supports cached pongs marking
        if (payload != null) {

            try {

                // If this ping doesn't have a GGEP object, make it a new blank one
                parseGGEP();

                // Our GGEP block has the key "SCP", indicating it supports cached pongs
                if (_ggep.hasKey(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS)) {

                    // Point ret at an empty byte array
                    ret = DataUtils.EMPTY_BYTE_ARRAY;

                    /*
                     * The next line may throw an exception.
                     * This is why we first set ret to an empty value.
                     */

                    // Get the value of the "SCP" key as a byte array, and return it
                    return _ggep.getBytes(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS);
                }

            // There was an error reading the GGEP block, return ret pointed to the empty byte array below
            } catch(BadGGEPBlockException ignored) {
            } catch(BadGGEPPropertyException ignored) {}
        }

        // The ping doesn't have a GGEP block, or there was an error reading it, return an empty byte array
        return ret;
    }

    /**
     * Determine if this big ping has "QK" in its GGEP block, indicating QueryKey support.
     * Before we even look in the GGEP block, this ping must have hops 1 and TTL 0.
     * QueryKey is a part of GUESS, which LimeWire no longer uses.
     * 
     * @return True if this ping has hops 1, TTL 0, and a GGEP block with the key "QK"
     */
    public boolean isQueryKeyRequest() {

        // Make sure this ping has TTL 0 and hops 1
        if (!(getTTL() == 0) || !(getHops() == 1)) return false; // It doesn't, return false

        // If this is a big ping with a payload, we can look in the GGEP block for the supports cached pongs marking
        if (payload != null) {

            try {

                // If this ping doesn't have a GGEP object, make it a new blank one
                parseGGEP();

                // Our GGEP block has the key "QK", indicating it supports QueryKey
                return _ggep.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);

            // There was an error reading the GGEP block, return false below
            } catch (BadGGEPBlockException ignored) {}
        }

        // The ping doesn't have a GGEP block, or there was an error reading it, return false
        return false;
    }

    /**
     * Determine if this is a big ping with "IP" in its GGEP block, indicating it wants IP address and port number information.
     * 
     * @return True if this ping has a GGEP block with the key "IP"
     */
    public boolean requestsIP() {

        // If this is a big ping with a payload, we can look in the GGEP block for the supports cached pongs marking
        if (payload != null) {

            try {

                // If this ping doesn't have a GGEP object, make it a new blank one
                parseGGEP();

                // Our GGEP block has the key "IP", indicating it wants IP address and port number information
                return _ggep.hasKey(GGEP.GGEP_HEADER_IPPORT);

            // There was an error reding the GGEP block, return false below
            } catch (BadGGEPBlockException ignored) {}
        }

        // This ping doesn't have a GGEP block, or there was an error reading it, return false
        return false;
    }

    /**
     * If this ping doesn't have a GGEP object, make it a new blank one.
     * The GGEP object is named _ggep.
     * 
     * @throws BadGGEPBlockException
     */
    private void parseGGEP() throws BadGGEPBlockException {

        // If this ping doesn't have a GGEP object, make it a new blank one
        if(_ggep == null) _ggep = new GGEP(payload, 0, null);
    }
}
