
// Commented for the Learning branch

package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.limegroup.gnutella.messages.BadPacketException;

/**
 * A Header Update vendor message lets a computer tell another when information it gave in the Gnutella handshake has changed.
 * 
 * When we connect to a remote computer running Gnutella software, we start by exchanging information in the Gnutella handshake.
 * After the handshake is over, we switch to exchanging a binary stream of Gnutella packets.
 * If a piece of information we told the remote computer in the handshake changes, we need a way to send the updated information.
 * The Header Update vendor message is the solution.
 * 
 * LimeWire uses the Header Update vendor message to report a change in our IP address.
 * In the handshake, we tell a remote computer our IP address with a header like this:
 * 
 * Listen-IP: 192.168.0.101:6346
 * 
 * If our IP address changes, we need to send an updated "Listen-IP" header.
 * RouterService.addressChanged() makes a new Properties hash table of strings with a key "Listen-IP" and value "216.27.158.74:6346".
 * It gives it to the HeaderUpdateVendorMessage(Properties) constructor.
 * 
 * The packet data looks like this:
 * 
 * e0 d8 b1 f5 19 d8 06 ff  --------  aaaaaaaa
 * 49 7d 26 c0 ab 9d 0e 00  I}&-----  aaaaaaaa
 * 31 01 00 46 00 00 00 4c  1--F---L  bcdeeeef
 * 49 4d 45 19 00 01 00 23  IME----#  fffffffg
 * 54 68 75 20 4d 61 72 20  Thu-Mar-  gggggggg
 * 31 36 20 31 35 3a 33 36  16-15:36  gggggggg
 * 3a 30 33 20 45 53 54 20  :03-EST-  gggggggg
 * 32 30 30 36 0d 0a 4c 69  2006--Li  gggggggg
 * 73 74 65 6e 2d 49 50 3d  sten-IP=  gggggggg
 * 32 31 36 2e 32 37 2e 31  216.27.1  gggggggg
 * 35 38 2e 37 34 5c 3a 36  58.74\:6  gggggggg
 * 33 37 35 0d 0a           375--     ggggg
 * 
 * a, b, c, d, and e make up the 23 byte Gnutella packet header.
 * a is the message GUID.
 * b is 0x31, the byte that marks this as a vendor message.
 * c is the TTL, 1.
 * d is the hops, 0.
 * e is the length of the payload, 0x46, 70 bytes, the size of the payload, f and g.
 * 
 * f is the 8 bytes like LIMEssvv that indentify what kind of vendor message this is.
 * The vendor code is LIME, the type number is 0x19, 25, and the version number is 1.
 * 
 * g is a Java Properties object written out as a program would to a properties file.
 * derivePayload() writes it out with the code props.save(baos, null).
 * The HeaderUpdateVendorMessage() constructor here reads it with the code _headers.load(bais).
 * 
 * Written out, the information from the Properties object looks like this:
 * 
 * #Thu Mar 16 15:36:03 EST 2006
 * Listen-IP=216.27.158.74\:6375
 * 
 * The first line is a comment that begins with a # symbol.
 * Each line is termianted with "\r\n", the two bytes 0x0d 0x0a.
 * The : has a \ before it, for some reason.
 */
public class HeaderUpdateVendorMessage extends VendorMessage {

    /** 1, LimeWire understands the initial version of the Header Update vendor message. */
    public static final int VERSION = 1;

    /**
     * The headers with updated information this Header Update vendor message is carrying.
     * A Properties object is a hash table of strings.
     * It has keys like "Listen-IP" and values like "216.27.158.74:6346".
     */
    private Properties _headers;

    /**
     * Make a new HeaderUpdateVendorMessage with data we read from a remote computer.
     * This is the packet parser.
     * 
     * @param guid    The message GUID we read from the Gnutella packet header
     * @param ttl     The TTL number we read from the Gnutella packet header
     * @param hops    The hops count we read from the Gnutella packet header
     * @param version The vendor message type version number we read from the payload
     * @param payload The data of the payload beyond the Gnutella packet header and the start of the payload like LIMEssvv
     */
    HeaderUpdateVendorMessage(byte[] guid, byte ttl, byte hops, int version, byte[] payload) throws BadPacketException {

        // Call the VendorMessage constructor
		super(
            guid,             // From the gnutella packet header, the message GUID
            ttl,              // From the gnutella packet header, the message TTL
            hops,             // From the gnutella packet header, the hops count
            F_LIME_VENDOR_ID, // "LIME", LimeWire introduced this vendor message
            F_HEADER_UPDATE,  // 25, LimeWire assigned this kind of vendor message number 25
            version,          // From the payload, the version number, which should be 1
            payload);         // The payload after that

        // If we were given the version number 1 but no payload after that, throw an exception
		if (getVersion() == VERSION && (payload == null || payload.length == 0)) throw new BadPacketException();

        // Make a new empty Properties object
        _headers = new Properties();

        try {

            // Wrap a ByteArrayInputStream around the payload data
		    InputStream bais = new ByteArrayInputStream(payload);

            // Read the data from the packet payload into keys and values in the Properties hash table
		    _headers.load(bais);

        // The load() method didn't like the data
		} catch (IOException bad) {

            // Convert this into a BadPacketException
		    throw new BadPacketException(bad.getMessage());
		}
	}

    /**
     * Make a new Header Update vendor message with information that's changed since the handshake.
     * 
     * Only RouterService.addressChanged() calls this.
     * It makes a new HeaderUpdateVendorMessage with this constructor.
     * The Properties hash table it gives us has one entry with our new IP address.
     * The key is like "Listen-IP", and our IP address and port number in its value like "216.27.158.74:6346".
     * It sends the Header Update vendor message to the remote computers we're connected to.
     * 
     * @param props A Properties hash table with our handshake headers that have new values
     */
    public HeaderUpdateVendorMessage(Properties props) {

        // Call the VendorMessage constructor
        super(
            F_LIME_VENDOR_ID,      // "LIME" 25 1, the vendor code, message type number, and version number that identify the Header Update vendor message
            F_HEADER_UPDATE,
            VERSION,
            derivePayload(props)); // Write the given Properties object as we would to a file, and save it as VendorMessage._payload

        // Save the given Properties object in this new one
        _headers = props;
    }

    /**
     * Compose the payload beyond LIMEssvv for this Header Update vendor message.
     * Calls props.save(b, null) to write the given Properties object into lines of text.
     * 
     * @param props A Properties hash table of strings with handshake headers like key "Listen-IP" and value "216.27.158.74:6346"
     * @return      The Properties object written into a byte array
     */
    private static byte[] derivePayload(Properties props) {

        // Make a new ByteArrayOutputStream that will grow to hold the data we write to it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Call save() to write the contens of the Properties hash table into the ByteArrayOutputStream
        props.save(baos, null); // Use save() to ignore the IOException

        // Get the byte array inside the ByteArrayOutputStream with the data we composed, and return it
        return baos.toByteArray();
    }

    /**
     * Get the hash table of updated handshake headers in this Header Update vendor message.
     * Returns a Properties hash table of strings with a key "Listen-IP" and a value like "216.27.158.74:6346".
     * 
     * @return A Java Properties hash table of strings
     */
    public Properties getProperties() {

        // Return the Properties object we saved or read
        return _headers;
    }
}
