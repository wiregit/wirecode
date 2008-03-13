package com.limegroup.bittorrent.dht;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;

import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.db.DHTValueType;

import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * An utility class for encoding and decoding network information. Also contains
 * constants used by both <code>DHTPeerLocator</code> and
 * <code>DHTPeerPublisher</code>.
 */
public class DHTPeerLocatorUtils {

    /**
     * Value type associated with DHT lookup for a peer seeding a torrent.
     */
    public static final DHTValueType BT_PEER_TRIPLE = DHTValueType.valueOf("BT Alternate Location",
            "BTAL");

    static final String BT_PEER_IP_PORT_KEY = "IP_PORT";

    static final String BT_PEER_ID_KEY = "ID";

    /**
     * Given an instance of TorrentLocation, it encodes the network information
     * in GGEP format.
     * 
     * @param torLoc an instance of TorrentLocation we want to encode.
     * @return an instance of GGEP as an array of bytes, an empty array if GGEP
     *         is empty.
     * @throws IllegalArgumentException if any of the network information were
     *         invalid.
     */
    public static byte[] encode(TorrentLocation torLoc) throws IllegalArgumentException {
        if (torLoc == null) {
            throw new IllegalArgumentException();
        }
        try {
            GGEP encoding = new GGEP();

            encoding.put(BT_PEER_IP_PORT_KEY, NetworkUtils
                    .getBytes(torLoc, ByteOrder.LITTLE_ENDIAN));
            encoding.put(BT_PEER_ID_KEY, torLoc.getPeerID());

            return encoding.toByteArray();
        } catch (IllegalArgumentException iae) {
            throw iae;
        }
    }

    /**
     * Decodes a given GGEP encoded network information and creates an instance
     * of TorrentLocation from it.
     * 
     * @param payload GGEP Encoded network information.
     * @throws IOException Thrown when network information are invalid.
     */
    public static TorrentLocation decode(byte[] payload) throws IllegalArgumentException,
            InvalidDataException {
        try {
            GGEP encoding = new GGEP(payload, 0);
            byte[] ipPort = encoding.getBytes(BT_PEER_IP_PORT_KEY);
            byte[] peerID = encoding.getBytes(BT_PEER_ID_KEY);

            List<IpPort> ipPortList = NetworkUtils.unpackIps(ipPort);
            
            
            IpPort ipPortInstance = ipPortList.get(0);

            return new TorrentLocation(ipPortInstance.getInetAddress(), ipPortInstance.getPort(),
                    peerID);

        } catch (BadGGEPBlockException e) {
            throw new IllegalArgumentException();
        } catch (BadGGEPPropertyException e) {
            throw new IllegalArgumentException();
        } catch (InvalidDataException e) {
            throw new InvalidDataException();
        }
    }
}
