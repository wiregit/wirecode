package com.limegroup.bittorrent.dht;


/**
 * Serializes and deserializes bit torrent peer connection data
 */
public class BTConnectionTriple {

    private final byte[] ip;

    private final int port;

    private final byte[] peerID;

    /**
     * Creates a BTConnection instance for the given network information.<\br>
     * Make sure you do not to modify the arrays that are being passed in since they are not copied.
     * 
     * @param ip ip address
     * @param port port number
     * @param peerID peer id
     */
    public BTConnectionTriple(byte[] ip, int port, byte[] peerID) {
        this.ip = ip;
        this.port = port;
        this.peerID = peerID;
    }

    /**
     * @return ip address of the connection
     */
    public byte[] getIP() {
        return this.ip;
    }

    /**
     * @return port number of the connection
     */
    public int getPort() {
        return this.port;
    }

    /**
     * @return peer id of the connection
     */
    public byte[] getPeerID() {
        return this.peerID;
    }

}
