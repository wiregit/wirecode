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
    
    private final byte[] IP;
    private final int    PORT;
    private final byte[] PEER_ID;
    private final boolean SUCCESS; 
    
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
        
        this.IP      = ip;
        this.PORT    = port;
        this.PEER_ID  = peerID;
        this.SUCCESS = success;
    }
    
    public BTConnectionTriple(byte[] ip, int port, byte[] peerID) {
        this.IP      = ip;
        this.PORT    = port;
        this.PEER_ID  = peerID;
        this.SUCCESS = true;
    }
    
    public byte[] getIP() {
        return this.IP;
    }
    
    public int getPort() {
        return this.PORT;
    }
    
    public byte[] getPeerID() {
        return this.PEER_ID;
    }
    
    public byte[] getEncoded() {
        if (!this.SUCCESS) {
            return null;
        }
        
        GGEP encoding = new GGEP();
        
        encoding.put(IP_KEY,      this.IP);
        encoding.put(PORT_KEY,    this.PORT);
        encoding.put(PEER_ID_KEY, this.PEER_ID);
        
        return encoding.toByteArray();
    }
    
    public boolean getSuccess() {
        return this.SUCCESS;
    }
    
    public String toString() {
        return "(" + this.IP + ":" + this.PORT + " - " + this.PEER_ID + ")";
    }
}
