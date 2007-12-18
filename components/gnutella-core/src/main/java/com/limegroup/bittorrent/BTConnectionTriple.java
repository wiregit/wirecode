package com.limegroup.bittorrent;

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
    private final boolean success; 
    
    private static final String IP_KEY      = "IP";
    private static final String PORT_KEY    = "PORT";
    private static final String PEER_ID_KEY = "PEER_ID";
    
    public BTConnectionTriple(byte[] stream) {

        byte[]  ip      = null;
        int     port    = -1;
        byte[]  peerID  = null;
        boolean success = false;
        
        try {
            
            GGEP encoding = new GGEP(stream, 0);

            ip      = encoding.getBytes(IP_KEY);
            port    = encoding.getInt(PORT_KEY);
            peerID  = encoding.getBytes(PEER_ID_KEY);
            success = true;
            
        } catch (BadGGEPBlockException e) {
            LOG.error("BadGGEPBlockException", e);
        } catch (BadGGEPPropertyException e) {
            LOG.error("BadGGEPPropertyException", e);
        }
        
        this.ip      = ip;
        this.port    = port;
        this.peerID  = peerID;
        this.success = success;
    }
    
    public BTConnectionTriple(byte[] ip, int port, byte[] peerID) {
        this.ip      = ip;
        this.port    = port;
        this.peerID  = peerID;
        this.success = true;
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
        if (!this.success) {
            return null;
        }
        
        GGEP encoding = new GGEP();
        
        encoding.put(IP_KEY,      this.ip);
        encoding.put(PORT_KEY,    this.port);
        encoding.put(PEER_ID_KEY, this.peerID);
        
        return encoding.toByteArray();
    }
    
    public boolean getSuccess() {
        return this.success;
    }
}
