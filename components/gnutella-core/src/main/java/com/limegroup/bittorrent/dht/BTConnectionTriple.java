package com.limegroup.bittorrent.dht;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * Serializes and deserializes bit torrent peer connection data
 */
public class BTConnectionTriple {
    
    private static final Log LOG = LogFactory.getLog(BTConnectionTriple.class);
    
    private final byte[] ip;
    private final int    port;
    private final byte[] peerID;    
    
    private static final String IP_KEY      = "IP";
    private static final String PORT_KEY    = "PORT";
    private static final String PEER_ID_KEY = "PEER_ID";
    
    /**
     * Creates a BTConnection instance given an GGEP encoded network information.
     * @param payload GGEP Encoded network information.
     * @throws IOException
     */
    public BTConnectionTriple(byte[] payload) throws IOException{

        byte[]  ip      = null;
        int     port    = -1;
        byte[]  peerID  = null;
        
        try {
            
            GGEP encoding = new GGEP(payload, 0);

            ip      = encoding.getBytes(IP_KEY);
            port    = encoding.getInt(PORT_KEY);
            peerID  = encoding.getBytes(PEER_ID_KEY);        
            
        } catch (BadGGEPBlockException e) {
            throw new IOException();
        } catch (BadGGEPPropertyException e) {            
            LOG.error("BadGGEPPropertyException", e);
            throw new IOException();
        }
        
        this.ip      = ip;
        this.port    = port;
        this.peerID  = peerID;
    }
    
    /**
     * Creates a BTConnection instance for the given network information
     * @param ip ip address
     * @param port port number
     * @param peerID peer id
     */
    public BTConnectionTriple(byte[] ip, int port, byte[] peerID) {
        this.ip      = ip;
        this.port    = port;
        this.peerID  = peerID;
    }
    
    public byte[] getIP() {
        return this.ip;
    }
    
    public int getPort() {
        return this.port;
    }
    
    public byte[] getPeerID() {
        return this.peerID;
    }
    
    public byte[] getEncoded() {
        GGEP encoding = new GGEP();
        
        encoding.put(IP_KEY,      this.ip);
        encoding.put(PORT_KEY,    this.port);
        encoding.put(PEER_ID_KEY, this.peerID);
        
        return encoding.toByteArray();
    }   
}
