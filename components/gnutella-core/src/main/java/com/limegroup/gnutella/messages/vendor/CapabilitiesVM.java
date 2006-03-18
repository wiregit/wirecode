
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
import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.version.UpdateHandler;

/**
 * A Capabilities vendor message lists capabilities that a computer has.
 * 
 * After 2 Gnutella computers finish the handshake, they exchange Capabilities vendor messages.
 * This lets them advertise advanced Gnutella features and vendor-specific features.
 * 
 * Connection.postInit() calls CapabilitiesVM.instance() to get our Capabilities vendor message.
 * It sends it to a remote computer we've just finished the handshake with.
 * Here's what it looks like:
 * 
 * bf f1 9c c5 2f aa 1e 94  ----/---  aaaaaaaa
 * 70 44 28 9b 71 8c cc 00  pD(-q---  aaaaaaaa
 * 31 01 00 1c 00 00 00 00  1-------  bcdeeeef
 * 00 00 00 0a 00 00 00 03  --------  fffffffg
 * 00 4c 4d 55 50 4d 00 57  -LMUPM-W  ghhhhhhh
 * 48 41 54 01 00 49 4d 50  HAT--IMP  hhhhhhhh
 * 50 2e 00                 P.-       hhh
 * 
 * a, b, c, d, and e make up the 23 byte Gnutella packet header.
 * a is the message GUID.
 * b is 0x31, the byte that marks this as a vendor message.
 * c is the TTL, 1.
 * d is the hops, 0.
 * e is the length of the payload, 0x1c 28 bytes.
 * 
 * f is the first 8 bytes of the payload, which name what kind of vendor message this is.
 * Ordinarly, these 8 bytes are like LIMEssvv, with the vendor that introduced the message, the number they assigned it, and the version it is.
 * For a Capabilties vendor message, the vendor is 4 0s, the ID is 10, and the version is 0.
 * 
 * g is the number of capabilities that follow.
 * Our Capabilities vendor message lists 3 capabilities, so g is 03 00, 3 in 2 bytes in little endian order.
 * 
 * h is the list of 3 capabilities.
 * Each capability is described in 6 bytes like NAMEvv, with the capability name and version number.
 * This part of the packet looks like this:
 * 
 * 4c 4d 55 50 4d 00  LMUP 77
 * 57 48 41 54 01 00  WHAT  1
 * 49 4d 50 50 2e 00  IMPP 46
 * 
 * LMUP is the LimeWire update message.
 * The current update message on the network right now that I've received is 77.
 * 
 * WHAT is feature search.
 * LimeWire supports the first feature, What's New search.
 * 
 * IMPP is SIMPP control.
 * I've received the current SIMPP control message, number 46.
 * 
 * The message that lets other know what capabilities you support.  Everytime
 * you add a capability you should modify this class.
 */
public final class CapabilitiesVM extends VendorMessage {

    /**
     * "WHAT", the name of the feature search capability.
     * 
     * The version number tells how many advanced Gnutella features we support.
     * Right now we only support the first feature, feature number 1, What's New search.
     * This capability gains its name from the name of this feature.
     */
    static final byte[] FEATURE_SEARCH_BYTES = {(byte)87, (byte)72, (byte)65, (byte)84};

    /**
     * "IMPP", the name of the SIMPP control capability.
     * 
     * SIMPP is the system that lets the LimeWire company remotely control LimeWire programs running on the Internet.
     * This capablity used to be named "SIMP", but that implementation was broken and abandoned.
     * This is why the name is "IMPP" instead.
     */
    private static final byte[] SIMPP_CAPABILITY_BYTES = {'I', 'M', 'P', 'P'};

    /**
     * "LMUP", the name of the Lime Update ability. (do)
     */
    private static final byte[] LIME_UPDATE_BYTES = {'L', 'M', 'U', 'P'};

    /**
     * 0, The Capabilities vendor message has a version number of 0.
     * Other vendor messages have a version number of 1, or 2 if they have been updated.
     */
    public static final int VERSION = 0;

    /**
     * A list of the names of the capabilities the computer that sent us this Capablities vendor message supports.
     * _capabilitiesSupported is a HashSet that contains CapabilitiesVM.SupportedMessageBlock objects.
     */
    private final Set _capabilitiesSupported = new HashSet();

    /**
     * The single CapabilitesVM object that lists the 4 capabilities we support.
     * This is the Capabilities vendor message that we send.
     */
    private static CapabilitiesVM _instance;

    /**
     * Make a new CapabilitiesVM with data we read from a remote computer.
     * This is the packet parser.
     * 
     * @param guid    The message GUID we read from the Gnutella packet header
     * @param ttl     The TTL number we read from the Gnutella packet header
     * @param hops    The hops count we read from the Gnutella packet header
     * @param version The vendor message type version number we read from the payload
     * @param payload The data of the payload beyond the Gnutella packet header and the start of the payload like LIMEssvv
     */
    CapabilitiesVM(byte[] guid, byte ttl, byte hops, int version, byte[] payload) throws BadPacketException {

        // Call the VendorMessage constructor
        super(
            guid,             // From the Gnutella packet header data, the message GUID
            ttl,              // From the Gnutella packet header data, the message TTL
            hops,             // From the Gnutella packet header data, the hops count
            F_NULL_VENDOR_ID, // The vendor ID that names the Capabilities vendor message is 4 0s instead of text like "LIME"
            F_CAPABILITIES,   // The vendor message code that names the Capabilities vendor message is 10
            version,          // From the payload, the version number, which should be 0
            payload);         // The payload after that, with a list of capabilities like nnWHATvvIMPPvvLMUPvv

        /*
         * populate the Set of supported capabilities....
         */

        try {

            // Get the payload data like nnWHATvvIMPPvvLMUPvv, and wrap a ByteArrayInputStream around it to keep our place as we read it
            ByteArrayInputStream bais = new ByteArrayInputStream(getPayload());

            // Read the first 2 bytes, which are little endian and contain the number of NAMEvv chunks that follow
            int vectorSize = ByteOrder.ushort2int(ByteOrder.leb2short(bais));

            /*
             * constructing the SMB will cause a BadPacketException if the
             * network data is invalid
             */

            // Loop that many times to turn 6 bytes like NAMEvv into a SupportedMessageBlock object, and add it to the _capabilitiesSupported HashSet
            for (int i = 0; i < vectorSize; i++) _capabilitiesSupported.add(new SupportedMessageBlock(bais));

        // There was an error reading the byte array
        } catch (IOException ioe) { ErrorService.error(ioe); }
    }

    /**
     * Make the CapabilitiesVM that lists the capabilities we support.
     * This is the packet maker.
     * 
     * This constructor is marked private, and only the instance() method below calls it.
     * The program makes one CapabilitiesVM object to use each time we send the message.
     */
    private CapabilitiesVM() {

        // Call the VendorMessage constructor
        super(
            F_NULL_VENDOR_ID, // The vendor code for the Capabilities vendor message is 4 0 bytes instead of text like "LIME"
            F_CAPABILITIES,   // 10, the Capabilities vendor message has a message ID of 10
            VERSION,          // 0, the version number of the Capabilities vendor message is 0, not 1 or 2
            derivePayload()); // Compose the payload of our Capabilities vendor message, and save it in _payload

        // Add SupportedMessageBlock objects that name the capabilities we support to the _capabilitiesSupported HashSet
        addSupportedMessages(_capabilitiesSupported);
    }

    /**
     * Compose payload data that lists the capabilities we support.
     * Only the CapabilitiesVM() constructor above calls this.
     * Returns a byte array of 20 bytes like this:
     * 
     * [3][0]
     * WHAT[1][0]
     * IMPP[46][0]
     * LMUP[77][0]
     * 
     * The first 2 bytes have the number of capabilities the rest of the data lists.
     * They hold the number 3, 0x03, in little endian order, 0x03 0x00.
     * After that are the capabilities, each of which take 6 bytes.
     * An 6-byte capability is composed of a name and version number.
     * 
     * @return A byte array with the payload data of our Capabilities vendor message
     */
    private static byte[] derivePayload() {

        // Make a new HashSet and add SupportedMessageBlock objects for WHAT 1, IMPP 46, and LMUP 77 to it
        Set hashSet = new HashSet();
        addSupportedMessages(hashSet);

        try {

            // Make a new ByteArrayOutputStream that will grow to hold the data we write to it
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Write the number of capabilities we'll describe next
            ByteOrder.short2leb((short)hashSet.size(), baos); // Writes the number in 2 bytes in little endian order

            // Loop through each SupportedMessageBlock object
            for (Iterator i = hashSet.iterator(); i.hasNext(); ) {
                SupportedMessageBlock currSMP = (SupportedMessageBlock)i.next();

                // Write 6 bytes like WHAT10, with the capability name and version number
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

    /*
     * ADD NEW CAPABILITIES HERE AS YOU BUILD THEM....
     */

    /**
     * Make a SupportedMessageBlock object for each kind of capability we support, and add them to the given HashSet.
     * Names 3 capabilities.
     * Here are their name codes, version numbers, and names:
     * 
     * WHAT  1 Features supported, we support the 1st feature, What's New search
     * IMPP 46 SIMPP commands, we've received command number 46
     * LMUP 77 LimeWire Update, we've received update number 77
     * 
     * @param hashSet A HashSet this method will add SupportedMessageBlock objects to
     */
    private static void addSupportedMessages(Set hashSet) {

        // Make a SupportedMessageBlock that names each capability and version, and add it to the given HashSet
        SupportedMessageBlock smp = null;
        smp = new SupportedMessageBlock(FEATURE_SEARCH_BYTES,   FeatureSearchData.FEATURE_SEARCH_MAX_SELECTOR); hashSet.add(smp); // WHAT  1
        smp = new SupportedMessageBlock(SIMPP_CAPABILITY_BYTES, SimppManager.instance().getVersion());          hashSet.add(smp); // IMPP 46
        smp = new SupportedMessageBlock(LIME_UPDATE_BYTES,      UpdateHandler.instance().getLatestId());        hashSet.add(smp); // LMUP 77
    }

    /**
     * Get the CapabilitiesVM object that lists the capabilities we support.
     * Connection.postInit() does this to send our Capabilities vendor message to a remote computer we just finished the handshake with.
     * Connection.sendUpdatedCapabilities() also calls this method.
     * 
     * @return A reference to the program's one CapabilitiesVM object
     */
    public static CapabilitiesVM instance() {

        // If the program hasn't needed our CapabilitiesVM object before, make it and save it now
        if (_instance == null) _instance = new CapabilitiesVM();

        // Return a reference to the CapabilitiesVM object we made with information about us
        return _instance;
    }

    /**
     * Determine if this Capabilities vendor message lists a given capability, and find out for what version it indicates support.
     * 
     * @param capabilityName The name of a capability, like "WHAT".
     * @return               If this Capabilities vendor message indicates support for that capability, the version number it supports.
     *                       -1 if not listed.
     */
    public int supportsCapability(byte[] capabilityName) {

        // Loop through the capabilities named in this packet
        Iterator iter = _capabilitiesSupported.iterator();
        while (iter.hasNext()) {
            SupportedMessageBlock currSMP = (SupportedMessageBlock)iter.next(); // Each capability is represented by a SupportedMessageBlock object

            // If this capability name matches the given one, return its version number in the packet
            int version = currSMP.matches(capabilityName); // The matches() method returns -1 if the capability names don't match
            if (version > -1) return version;              // We found it, return the version number this Capabilities vendor message indicates support for
        }

        // Not found
        return -1;
    }

    /**
     * Find out how many advanced features the computer that sent this Capabilities vendor message supports.
     * Searches this Capabilities vendor message for the "WHAT" capability, and gets the version number.
     * The return value is probably 1, indicating support for the first advanced feature, What's New search.
     * 
     * @return The number of advanced Gnutella features the computer supports, like 1.
     *         -1 if "WHAT" is not listed.
     */
    public int supportsFeatureQueries() {

        // Search the list of capabilities in this Capabilties vendor message for "WHAT" feature search
        return supportsCapability(FEATURE_SEARCH_BYTES); // Return the version number like 1, or -1 if not listed
    }

    /**
     * Determine if the computer that sent this Capabilities vendor message supports What's New search.
     * Searches this Capabilities vendor message for the "WHAT" capability, and gets the version number.
     * If the version number is 1 or more, the compuer supports What's New search, returns true.
     * 
     * @return True if the computer that made this Capabilities vendor message supports What's New search
     */
    public boolean supportsWhatIsNew() {

        // Search this Capabilities vendor message for "WHAT" to get its value like 1, and return true if it's 1 or more
        return FeatureSearchData.supportsWhatIsNew(supportsCapability(FEATURE_SEARCH_BYTES)); // Return false if not listed
    }

    /**
     * Get the current SIMPP message number.
     * Searches this Capabilities vendor message for the "IMPP" capability, and gets the version number.
     * 
     * @return The number of the most recent SIMPP command the computer that made this message has received, like 46
     */
    public int supportsSIMPP() {

        // Search the list of capabilities in this Capabilties vendor message for "IMPP" SIMP control
        return supportsCapability(SIMPP_CAPABILITY_BYTES); // Return the version number like 46, or -1 if not listed
    }

    /**
     * Get the current update number.
     * Searches the Capabilities vendor message for the "LMUP" capability, and gets the version number.
     * 
     * @return The number of the most recent update information the computer that made this message has received, like 77
     */
    public int supportsUpdate() {

        // Search the list of capabilities in this Capabilties vendor message for "LMUP" Lime update ability
        return supportsCapability(LIME_UPDATE_BYTES); // Return the version number like 77, or -1 if not listed
    }

    /**
     * Determine if a given CapabilitiesVM object lists the same capabilities as this one.
     * 
     * @param other The object to compare this one to
     * @return      True if they contain the same information, false if they are different
     */
    public boolean equals(Object other) {

        // If the caller gave us a reference to ourselves, same
        if (other == this) return true;

        /*
         * two of these messages are the same if they list the same capabilities
         */

        // Make sure the given object is also a CapabilitiesVM
        if (other instanceof CapabilitiesVM) {
            CapabilitiesVM vmp = (CapabilitiesVM)other;

            // Compare the HashSet lists of SupportedMessageBlock objects
            return _capabilitiesSupported.equals(vmp._capabilitiesSupported);
        }

        // Different
        return false;
    }

    /**
     * Remake the Capabilities vendor message we send remote computers after the handshake.
     * 
     * addSupportedMessages() calls SimppManager.instance().getVersion() and UpdateHandler.instance().getLatestId().
     * These numbers are the version numbers of the IMPP and LMUP capabilities listed in the packet.
     * When the numbers getVerion() and getLatestId() return change, the program calls reconstructInstance() to get the new numbers into our packet.
     * 
     * SimppManager.checkAndUpdate() has a thread call this.
     * UpdateHandler.storeAndUpdate() calls this.
     */
    public static void reconstructInstance() {

        /*
         * replace _instance with a newer one, which will be created with the
         * correct simppVersion, a new _capabilitiesSupported will be created
         */

        // Remake our Capabilities vendor message
        _instance = new CapabilitiesVM();
    }

    /**
     * Hash the information in this CapabilitiesVM object into a number.
     * 
     * @return The hash code number
     */
    public int hashCode() {

        // Get the hash code of the Java HashSet _capabilitiesSupported, and multiply that by 17
        return 17 * _capabilitiesSupported.hashCode();
    }

    /**
     * A SupportedMessageBlock object represents a capability that we support.
     * The CapabilitiesVM class puts SupportedMessageBlock objects in its _capabilitiesSupported HashSet.
     * This makes up the list of capabilities that the CapabilitiesVM object documents support for.
     */
    static class SupportedMessageBlock {

        /** The 4 byte capabilitiy name like "WHAT". */
        final byte[] _capabilityName;

        /** The version number of that capability. */
        final int _version;

        /** A hash code number made from the capability name and version number this object keeps cached. */
        final int _hashCode;

        /**
         * Express this SupportedMessageBlock object as a String.
         * Composes text with the capability name and version number like "WHAT/1".
         * 
         * @return A String
         */
        public String toString() {

            // Compose text with the capability name and version number separated by a slash
            return new String(_capabilityName) + "/" + _version;
        }

        /**
         * Make a new SupportedMessageBlock with the given capability name and version number.
         * 
         * @param capabilityName A 4 character long capability name, like "WHAT"
         * @param version        The version number of that capability
         */
        public SupportedMessageBlock(byte[] capabilityName, int version) {

            // Save the given name and number in this new object
            _capabilityName = capabilityName;
            _version = version;

            // Compute and save the hash code based on that information
            _hashCode = computeHashCode(_capabilityName, _version);
        }

        /**
         * Make a new SupportedMessageBlock with data we read from a remote computer.
         * Parses 6 bytes like "WHAT10" with the capability name and version number.
         * 
         * @param  encodedBlock       An InputStream object we can call read() on to get data.
         * @throws BadPacketException Not enough data has arrived yet
         */
        public SupportedMessageBlock(InputStream encodedBlock) throws BadPacketException, IOException {

            // Make sure there are at least 6 bytes for us to read
            if (encodedBlock.available() < 6) throw new BadPacketException("invalid block.");

            // The first 4 bytes are the capability name, like "WHAT"
            _capabilityName = new byte[4];
            encodedBlock.read(_capabilityName, 0, _capabilityName.length); // Copy them into the _capabilityName byte array

            // The 2 bytes after that are the version number
            _version = ByteOrder.ushort2int(ByteOrder.leb2short(encodedBlock)); // The bytes are in little endian order, like 01 00 for version number 1

            // Compute and save the hash code based on that information
            _hashCode = computeHashCode(_capabilityName, _version);
        }

        /**
         * Writes this capability name and version number to a remote computer.
         * Composes 6 bytes like "WHAT10" with the capability name and version number.
         * 
         * @param out An OutputStream object we can call write() on to give it data.
         */
        public void encode(OutputStream out) throws IOException {

            // Write the 4 byte capability name followed by the version number in 2 bytes
            out.write(_capabilityName);
            ByteOrder.short2leb((short)_version, out); // Little endian order, version 1 becomes the 2 bytes 01 00
        }

        /**
         * Determine if this SupportedMessageBlock names the given capability name.
         * 
         * @return The version number if this is a match.
         *         -1 if not.
         */
        public int matches(byte[] capabilityName) {

            // If the given capability name matches this one, return the version number we're keeping
            if (Arrays.equals(_capabilityName, capabilityName)) return _version;
            else return -1; // Not found
        }

        /**
         * Determine if this object is the same as a given one.
         * Compares the capability names and version numbers.
         * 
         * @param other The object to compare this one to
         * @return      True if they are the same, false if they're different
         */
        public boolean equals(Object other) {

            // Make sure the given object is a SupportedMessageBlock
            if (other instanceof SupportedMessageBlock) {
                SupportedMessageBlock vmp = (SupportedMessageBlock)other;

                // Return true if the version numbers and capability names match.
                return ((_version == vmp._version) && (Arrays.equals(_capabilityName, vmp._capabilityName)));
            }

            // Different
            return false;
        }

        /**
         * Get the hash code we computed for this SupportedMessageBlock object.
         * 
         * @return The hash code number
         */
        public int hashCode() {

            // Return the hash code we computed when we made the object
            return _hashCode;
        }

        /**
         * Compute a hash code from a given capability name and version number.
         * 
         * @param capabilityName A capability name, like "WHAT"
         * @param version        A version number, like 1
         * @return               The hash code number we computed
         */
        private static int computeHashCode(byte[] capabilityName, int version) {

            // Start the hash code with the version number
            int hashCode = 0;
            hashCode += 37 * version;

            // Loop for each of the 4 characters in the capability name, and adjust the hash code for it
            for (int i = 0; i < capabilityName.length; i++) hashCode += (int)37 * capabilityName[i];

            // Return it
            return hashCode;
        }
    }

    /**
     * Write the payload of this vendor message to the given OutputStream.
     * Writes the 8-byte LIMEssvv type identifer that begins the payload, and the type specific payload data after that.
     * This class overrides this method from VendorMessage to count the statistic that we're sending this message.
     * 
     * A Capabilities vendor message looks like this:
     * 
     * gnutella header 23 bytes
     * vendor message type 8 bytes
     * 
     *   0000ssvv  Tells what kind of vendor message this is
     * 
     * rest of the payload
     * 
     *   nn        The number of capabilities that follow
     *   IMPPvv    A list of capability names and versions
     *   WHATvv
     *   LMUPvv
     * 
     * This method writes the whole payload, the vendor message type and the rest of the payload.
     * 
     * CapabilitiesVM overrides this method from VendorMessage to count a statistic.
     * 
     * @param out An OutputStream we can call write() on to send data to the remote computer
     */
    protected void writePayload(OutputStream out) throws IOException {

        // Call VendorMessage.writePayload(out) to write the vendor message type bytes followed by _payload, which derivePayload() composed
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

    /**
     * Express this CapabilitiesVM object as a String.
     * Composes text like:
     * 
     * {CapabilitiesVM:{guid=BFF19CC52FAA1E947044289B718CCC00, ttl=1, hops=0, priority=0}; supporting: [LMUP/77, WHAT/1, IMPP/46]}
     * 
     * @return A String
     */
    public String toString() {

        // Compose and return the text with information from this object
        return "{CapabilitiesVM:" + super.toString() + "; supporting: " + _capabilitiesSupported + "}";
    }
}
