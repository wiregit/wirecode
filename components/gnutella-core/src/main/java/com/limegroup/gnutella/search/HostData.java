
// Commented for the Learning branch

package com.limegroup.gnutella.search;

import java.util.Set;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * A HostData object holds information about a computer sharing files on the Gnutella network.
 * When we get a query hit packet, we make a HostData object to hold information about the sharing computer that made and sent it.
 * All of the information in a HostData object is read from the query hit packet.
 * 
 * LimeWire uses 3 classes to organize the information in a query hit packet:
 * A QueryReply object represents the query hit packet.
 * Response objects represent the shared files described in the query hit packet.
 * A HostData object holds information about the computer that made the packet and is sharing the files.
 * 
 * Only QueryReply.parseResults2() makes a HostData object.
 * The program accesses it by calling queryReply.getHostData().
 */
public final class HostData {

	/** This sharing computer's client ID GUID. */
	private final byte[] CLIENT_GUID;

	/** The message GUID of the query hit packet we got information about this sharing computer from. */
	private final byte[] MESSAGE_GUID;

	/** This sharing computer's upload speed. */
	private final int SPEED;

	/** True if this sharing computer is firewalled and can't receive an incoming TCP socket connection. */
	private final boolean FIREWALLED;

	/** True if all this sharing computer's upload slots are full. */
	private final boolean BUSY;

	/** True if this sharing computer is on the same LAN as we are, and we searched it over the multicast protocol. */
	private final boolean MULTICAST;

	/** True if this sharing computer supports the chat feature, and has it enabled. */
	private final boolean CHAT_ENABLED;

	/** True if we can browse this sharing computer's files. */
	private final boolean BROWSE_HOST_ENABLED;

	/** True if SPEED is from data this sharing computer measured, not just a setting its user entered. */
	private final boolean MEASURED_SPEED;

	/** This sharing computer's port number. */
	private final int PORT;

	/** This sharing computer's IP address. */
	private final String IP;

	/** A number from -1 to 4 that is our estimation of how likely we'll be able to get files from this sharing computer. */
	private final int QUALITY;

    /** The vendor code like "LIME" of the Gnutella software this sharing computer is running. */
    private final String VENDOR_CODE;

    /** The addresses of this sharing computer's push proxies, its ultrapeers that can forward a push request packet to it. */
    private final Set PROXIES;

    /** True if this sharing computer can do a firewall-to-firewall transfer. */
    private final boolean CAN_DO_FWTRANSFER;

    /** The version number of this sharing computer's firewall-to-firewall transfer support, like 0 for not supported, or 1 for the current version. */
    private final int FWT_VERSION;

	/**
     * Make a new HostData object to hold information about a computer that's sharing files.
     * The HostData object keeps information about the sharing computer, like its client ID GUID and its upload speed.
     * 
     * This constructor takes a query hit packet the sharing computer made.
     * It reads information about the sharing computer from it, and keeps it in this new HostData object.
     * 
     * @param reply A query hit packet the sharing computer made
	 */
	public HostData(QueryReply reply) {

        // Copy information from the query hit packet
		CLIENT_GUID  = reply.getClientGUID(); // The sharing computer's client ID GUID, from the very end of the query hit packet
		MESSAGE_GUID = reply.getGUID();       // The message GUID of the query hit packet, from the gnutella packet header
		IP           = reply.getIP();         // The sharing computer's IP address, from the start of the payload
		PORT         = reply.getPort();       // The sharing computer's port number, from the start of the payload

        // Set defaults for the values we'll parse for
		boolean firewalled        = true;  // Assume the sharing computer is firewalled by default
		boolean busy              = true;  // Assume the sharing computer doesn't have any free upload slots
		boolean browseHostEnabled = false; // Assume we can't browse the sharing computer's files
		boolean chatEnabled       = false; // Assume the sharing computer has chat turned off
		boolean measuredSpeed     = false; // Assume the upload speed the sharing computer told us is based on user input, not measured data
		boolean multicast         = false; // Assume the sharing computer isn't on the same LAN as we are
        String  vendor = "";               // This is where we'll keep the vendor code of the software the sharing computer is running, like "LIME"

		try {

            // Only set firewalled to false if the sharing computer says it's externally contactable, and gave us an Internet IP address
			firewalled =
                reply.getNeedsPush() ||            // The query hit says the sharing computer can't accept a TCP connection, or
                NetworkUtils.isPrivateAddress(IP); // The IP address the sharing computer gave as its own is a LAN IP address

        // Unable to read packet, assume the sharing computer is firewalled
		} catch (BadPacketException e) { firewalled = true; }

		try {

            // Set measuredSpeed if the query hit has a flag that says the upload speed is from measured data, not a user setting
			measuredSpeed = reply.getIsMeasuredSpeed();

        // Unable to read packet, assume the speed is from a setting the user entered
        } catch (BadPacketException e) { measuredSpeed = false; }

		try {

            // Set busy from the flag in the query hit packet
			busy = reply.getIsBusy();

        // Unable to read packet, assume all the sharing computer's upload slots are full
        } catch (BadPacketException bad) { busy = true; }

		try {

            // Get the vendor code of the Gnutella software the remote computer is running, like "LIME" for LimeWire
            vendor = reply.getVendor();

        // Unable to read packet, leave the vendor String blank
        } catch (BadPacketException bad) {}

        // If the query hit's GGEP block has "BH", set browseHostEnabled to true
    	browseHostEnabled = reply.getSupportsBrowseHost();

        // Only set chatEnabled true if the query hit has a chat byte of 1 in the private area, and isn't firewalled for TCP
		chatEnabled = reply.getSupportsChat() && !firewalled;

        // Set multicast true if the query hit's GGEP block has "MCAST"
		multicast = reply.isReplyToMulticastQuery();

        // Save information in this HostData object
		FIREWALLED          = firewalled && !multicast;   // The sharing computer is firewalled if it said so or sent a LAN IP, and didn't mention "MCAST"
		BUSY                = busy;                       // True if all the sharing computer's upload slots are full
		BROWSE_HOST_ENABLED = browseHostEnabled;          // True if the GGEP block has "BH"
		CHAT_ENABLED        = chatEnabled;                // True if the private area has a 1 for the chat byte
		MEASURED_SPEED      = measuredSpeed || multicast; // True if the query hit says the speed is measured, or GGEP has "MCAST"
		MULTICAST           = multicast;                  // True if GGEP has "MCAST"
        VENDOR_CODE         = vendor;                     // The vendor code of the Gnutella software the sharing computer is running

        // Estimate how likely it is we'll be able to get files from this sharing computer
		boolean ifirewalled = !RouterService.acceptedIncomingConnection(); // Determine if we're firewalled or not, which affects our chances
        QUALITY = reply.calculateQualityOfService(ifirewalled);            // Get a number from -1 it will never work to 4 it will definitely work

        // Get a list of the sharing computer's push proxies, its ultrapeers that can forward a push request packet to it
        PROXIES = reply.getPushProxies(); // From the GGEP "PUSH" extension, a IpPortSet of IPPortCombo objects with the addresses of the sharing computer's push proxies

        // Find out if the sharing computer can do firewall-to-firewall file transfers
        CAN_DO_FWTRANSFER = reply.getSupportsFWTransfer(); // In the query hit's GGEP block, "FW" indicates the sharing computer can do firewall-to-firewall transfers
        FWT_VERSION       = reply.getFWTransferVersion();  // The value of the "FW" extension, 1 indicates version 1 support, 0 if "FW" not found

        // Find out how fast the sharing computer can upload a file to us
        if (multicast) SPEED = Integer.MAX_VALUE;                    // We're on the same LAN, so we can get a file really fast
        else           SPEED = ByteOrder.long2int(reply.getSpeed()); // The sharing computer is remote on the Internet, read the speed from the query hit packet
	}

    /**
     * This sharing computer's client ID GUID.
     * From the last 16 bytes of the query hit packet.
     * 
     * @return The client ID GUID as a 16-byte array
     */
    public byte[] getClientGUID() {

        // Return the value we parsed and saved
		return CLIENT_GUID;
	}

    /**
     * The vendor code like "LIME" of the Gnutella software this sharing computer is running.
     * From the start of the QHD part of the query hit packet.
     * 
     * @return The vendor code as a String like "LIME"
     */
	public String getVendorCode() {

        // Return the value we parsed and saved
		return VENDOR_CODE;
	}

    /**
     * The message GUID of the query hit packet we got information about this sharing computer from.
     * From the query hit packet this sharing computer sent us, in the Gnutella packet header.
     * 
     * This is the GUID that identifies the search.
     * The query packet, the query hit this sharing computer sent in response, and all the vendor messages involved in this search have this as their message GUID.
     * 
     * @return The message GUID as a 16-byte array
     */
	public byte[] getMessageGUID() {

        // Return the value we parsed and saved
		return MESSAGE_GUID;
	}

    /**
     * This sharing computer's upload speed.
     * From the query hit packet this sharing computer sent us, in the 11-byte start of the payload.
     * 
     * If the query hit packet's GGEP block has "MCAST", returns the largest number that fits in an integer, Integer.MAX_VALUE.
     * This means we're on the same LAN as this sharing computer, so it will be able to upload a file to us very quickly.
     * 
     * @return The speed
     */
	public int getSpeed() {

        // Return the value we parsed and saved
		return SPEED;
	}

    /**
     * A number from -1 to 4 that is our estimation of how likely we'll be able to get files from this sharing computer.
     * QueryReply.calculateQualityOfService() computed this number.
     * 
     * @return The quality number
     */
	public int getQuality() {

        // Return the value we parsed and saved
		return QUALITY;
	}

    /**
     * This sharing computer's IP address.
     * From the query hit packet this sharing computer sent us, in the 11-byte start of the payload.
     * 
     * @return The IP address as a String like "1.2.3.4"
     */
	public String getIP() {

        // Return the value we parsed and saved
		return IP;
	}

    /**
     * This sharing computer's port number.
     * From the query hit packet this sharing computer sent us, in the 11-byte start of the payload.
     * 
     * @return The port number
     */
	public int getPort() {

        // Return the value we parsed and saved
		return PORT;
	}

    /**
     * Determine if we can't connect to this sharing computer because it's firewalled.
     * This means it can't accept an incoming TCP socket connection.
     * 
     * Looks at the query hit packet this sharing computer sent us.
     * Examines 0x01 in the mask and value bytes, which this sharing computer set if it's firewalled.
     * 
     * The sharing computer may have said it's not firewalled, but then returned a LAN IP address as its IP address.
     * If this is the case, this method will return true.
     * 
     * If this sharing computer's query hit packet had a GGEP block with "MCAST", returns false.
     * This means the sharing computer is on the same LAN as us, so even if it's firewalled for remote Internet computers, we'll be able to connect to it.
     * 
     * @return True if this sharing computer is firewalled
     */
	public boolean isFirewalled() {

        // Return the value we parsed and saved
		return FIREWALLED;
	}

    /**
     * Determine if all this sharing computer's upload slots are full.
     * In the sharing computer's query hit packet, looks for 0x04 in the mask and values bytes.
     * 
     * @return True if this sharing computer is busy uploading files to other computers
     */
	public boolean isBusy() {

        // Return the value we parsed and saved
		return BUSY;
	}

    /**
     * Determine if we can browse this sharing computer's files.
     * In the sharing computer's query hit packet, looks for the GGEP extension "BH".
     * 
     * @return True if this sharing computer supports browse host
     */
	public boolean isBrowseHostEnabled() {

        // Return the value we parsed and saved
		return BROWSE_HOST_ENABLED;
	}

    /**
     * Determine if we can chat with this sharing computer.
     * In the sharing computer's query hit packet, looks for a chat byte of 1 in the private area.
     * 
     * @return True if this sharing computer supports chat
     */
	public boolean isChatEnabled() {

        // Return the value we parsed and saved
		return CHAT_ENABLED;
	}

    /**
     * Determine if our record of this sharing computer's speed comes from real network data it measured, or just a setting its user entered.
     * In the sharing computer's query hit packet, looks for 0x10 in the mask and values bytes.
     * 
     * If the query hit GGEP has "MCAST", returns true.
     * This sharing computer is on the same LAN as us, so it will be able to upload a file to us very quickly.
     * 
     * @return True if getSpeed() returns data the sharing computer measured
     */
	public boolean isMeasuredSpeed() {

        // Return the value we parsed and saved
		return MEASURED_SPEED;
	}

    /**
     * Determine if this sharing computer is on the same LAN as us, and replied to a multicast query from us.
     * In the sharing computer's query hit packet, looks for the GGEP extension "MCAST".
     * 
     * @return True if this sharing computer replied to our multicast query on the LAN
     */
	public boolean isReplyToMulticastQuery() {

        // Return the value we parsed and saved
	    return MULTICAST;
	}

    /**
     * Get the addresses of this sharing computer's push proxies, its ultrapeers that can forward a push request packet to it.
     * In the sharing computer's query hit packet, looks for the GGEP extension "PUSH".
     * 
     * @return A IpPortSet of IPPortCombo objects with the IP addresses and port numbers of this sharing computer's push proxies.
     *         If this sharing computer's query hit's GGEP block doesn't have "PUSH", returns an empty Set.
     */
    public Set getPushProxies() {

        // Return the value we parsed and saved
        return PROXIES;
    }

    /**
     * Determine if this sharing compuer can do a firewall-to-firewall file transfer.
     * In the sharing computer's query hit packet, looks for the GGEP extension "FW".
     * 
     * @return True if this sharing computer can do a firewall-to-firewall transfer
     */
    public boolean supportsFWTransfer() {

        // Return the value we parsed and saved
        return CAN_DO_FWTRANSFER;
    }

    /**
     * Get the version number of firewall-to-firewall transfer support this sharing computer has, like 0 not supported or 1 current version.
     * In the sharing computer's query hit packet, looks for the GGEP extension "FW".
     * If the extension isn't there, returns 0.
     * Otherwise returns the number value of "FW", which is the version of firewall-to-firewall transfers the sharing computer supports.
     * The current version is 1
     * 
     * @return The version of firewall-to-firewall transfers the sharing computer supports, like 1.
     *         0 if the sharing computer can't do firewall-to-firewall transfers.
     */
    public int getFWTVersionSupported() {

        // Return the value we parsed and saved
    	return FWT_VERSION;
    }
}
